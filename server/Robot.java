import javax.swing.tree.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Random;
import java.util.Collections;

/**
* An artificial intelligence that uses wordTrees to find words and make steals
*/

class Robot {

	private Game game;
	public final String robotName;
	public final int skillLevel;
	private final int blankPenalty;
	private final int minLength;
	int blanksAvailable;
	
	private HashMap<String, WordTree> trees = new HashMap<>();
	private AlphagramTrie dictionary;
	
	public boolean found = false;
	private Random rgen = new Random();	
	
	/**
	*
	*/
	
	public Robot(Game game, int skillLevel, AlphagramTrie dictionary) {
		
		this.dictionary = dictionary;

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
	}
	
	/**
	* Recursively generates all possible combinations of tiles from the tilePool whose length equals the minimum word length.
	* Each combination is checked to see if a valid word can be formed using the remaining letters in the pool. The loop halts
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
			}
		}
		
		for(Map.Entry<Character,Node> child : node.children.entrySet()) {
			if(remainingPool.contains(child.getKey() + "")) {
				searchForWord(child.getValue(), remainingPool.replaceFirst(child.getKey() + "", ""), 0);
			}
			else {
				blanksRequired++;
				if(blanksAvailable >= blanksRequired && remainingPool.length() >= blanksRequired*blankPenalty) {
					searchForWord(child.getValue(), remainingPool, blanksRequired);
				}
			}
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
	
	boolean makeSteal(Hashtable<String, Vector<String>> words) {
		ArrayList<String> players = new ArrayList<>(words.keySet());
		Collections.shuffle(players);
		for(String player : players) {
			for(String shortWord : words.get(player)) {
				if(trees.containsKey(shortWord)) {
					Enumeration e = trees.get(shortWord).root.children();

					while(e.hasMoreElements()) {
						DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
						String longWord = child.getUserObject().toString();
						
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
