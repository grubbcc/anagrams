import javax.swing.tree.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Arrays;
import java.io.IOException;
import java.util.Random;

/***
* An artificial intelligence that uses wordTrees to find words and make make steals
*/

class Robot extends Thread {

	private Game game;
	public final String robotName;
	public final int skillLevel;
	private final int blankPenalty;
	private final int minLength;
	private String tilePool;
	private int blanksAvailable;
	
	private HashMap<String, WordTree> trees = new HashMap<>();
	private HashMap<String, Vector<String>> words;
	private AlphagramTrie dictionary;
	
	public boolean found = false;
	public boolean ready = false;
	private Random rgen = new Random();	
	
	/**
	*
	*/
	
	public Robot(Game game, int skillLevel) {
		
		this.dictionary = new AlphagramTrie(game.lexicon);
		this.game = game;
		this.blankPenalty = game.blankPenalty;
		this.minLength = game.minLength;
		this.skillLevel = skillLevel;
		
		if(skillLevel == 1)
			robotName = "Robot-Novice";
		else if(skillLevel == 2)
			robotName = "Robot-Player";
		else if(skillLevel == 3)
			robotName = "Robot-Expert";
		else
			robotName = "Robot-Genius";
		
		ready = true;
	}
	
	/**
	* Recursively generates all possible combinations of tiles from the tilePool whose length equals the minimum word length.
	* Each combination is checked to see if a valid word can be formed using the remaining letters in the pool. The loop ends
	* if and when the first valid word is found.
	*
	* @param key: the character address of the node being searched
	* @param rest: the characters
	* @param charsToTake
	*/

	void makeWord(String key, String pool, String rest, int charsToTake) {
		
		if(found)
			return;
		
		if(rest.length() == charsToTake) {
			key += rest;
			//System.out.println("Done: key = " + key + ", pool = " + pool);	
			if(dictionary.getNode(key) != null) {
				
				searchForWord(dictionary.getNode(key), pool, 0);
				return;
			}
		}
		
		for(int i = 0; i < rest.length() && i <= charsToTake; i++) {
			if(found)
				return;
			makeWord(key + rest.substring(0,i), pool + rest.substring(i, i+1), rest.substring(i+1, rest.length()), charsToTake-i);
		}
	}
	
	/**
	* Given a node in the dictionary trie containing a known set of letters, recursively searches for words that can be constructed 
	* using the remaining letters in the pool.
	*/

	void searchForWord(Node node, String remainingPool, int blanksRequired) {
		
		if(found)
			return;
		
		for(String anagram : node.anagrams) {
			if(anagram.length() >= minLength + blanksRequired*blankPenalty) {
				found = true;
				game.doMakeWord(robotName, anagram);
				//long endTime = System.nanoTime();
				//System.out.println("found the word " + anagram + ". elapsed time: " + (endTime - game.startTime)/1000000 + "ms");
			}
		}
		
		for(Map.Entry<Character,Node> child : node.children.entrySet()) {
			if(remainingPool.contains(child.getKey() + "")) {
				searchForWord(child.getValue(), remainingPool.replaceFirst(child.getKey() + "", ""), 0);
			}
			else {
				if(blanksAvailable >= ++blanksRequired && remainingPool.length() >= ++blanksRequired*blankPenalty) {
					searchForWord(child.getValue(), remainingPool, blanksRequired);
				}
			}
			//long endTime = System.nanoTime();
			//System.out.println("no word found here. elapsed time: " + (endTime - game.startTime)/1000000 + "ms");
		}
	}
	


	/**
	*
	*/
	
	void makeTree(String shortWord) {
		trees.put(shortWord, new WordTree(new DefaultMutableTreeNode(new UserObject(shortWord.replaceAll("[a-z]",""), "")), dictionary));
	}
	
	/**
	*
	*/
	
	void removeTree(String stolenWord) {
		trees.remove(stolenWord);
	}
	
	/**
	*
	*/
	
	void setTilePool(String tilePool) {
		this.tilePool = tilePool;
	}
	
	/**
	*
	*/
	
	boolean makeSteal(HashMap<String, Vector<String>> words) {
		
		for(String player : words.keySet()) {
			for(String shortWord : words.get(player)) {
				//WordTree currentTree = trees.get(shortWord); //fto
				//System.out.println("shortWord = " + shortWord); //fto
				//DefaultMutableTreeNode currentRoot = currentTree.root; //fto
				//System.out.println("root word = " + currentRoot.getUserObject().toString()); //fto
				if(trees.containsKey(shortWord)) {
					Enumeration e = trees.get(shortWord).root.children();

					while(e.hasMoreElements()) {
						DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
						String longWord = child.getUserObject().toString();
						//System.out.println("attempting to steal " + longWord + " from " + shortWord);
						if(rgen.nextInt(4) < skillLevel) {
							if(game.doSteal(player, shortWord, robotName, longWord)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
}