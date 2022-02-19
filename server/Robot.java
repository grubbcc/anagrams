package server;

import java.util.*;

/**
* An artificial intelligence that uses wordTrees to find words and make steals
*/

class Robot {

	final String robotName;
	final int skillLevel;

	private  Game game;
	private final int blankPenalty;
	private final int minLength;
	final HashMap<String, WordTree> trees = new HashMap<>();
	private final HashMap<String, WordTree> commonTrees = new HashMap<>();
	private final AlphagramTrie dictionary;
	final private Random rgen = new Random();

	private int blanksAvailable;

	/**
	*
	*/
	
	public Robot(int skillLevel, AlphagramTrie dictionary, int minLength, int blankPenalty) {
		
		this.dictionary = dictionary;

		this.minLength = minLength;
		this.blankPenalty = blankPenalty;
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
	 * Either attempt to steal a word or to make a word from the letters in the pool
	 */

	public void makePlay(Game game, String tilePool, Hashtable<String,Vector<String>> words) {

		this.game=game;
		blanksAvailable = tilePool.length() - tilePool.replace("?", "").length();

		if (tilePool.length() >= 2 * minLength || rgen.nextInt(2) == 0 && tilePool.length() >= minLength + 1) {

			//decide whether to search among all words or among common subset
			Node rootNode = rgen.nextInt(1,4) >= skillLevel ? dictionary.common.rootNode : dictionary.rootNode;

			searchInPool(rootNode, "", AlphagramTrie.alphabetize(tilePool.replace("?", "")), 0);
		}
		else {
			searchForSteal(game, words);
		}
	}

	/**
	 * Recursively searches for a word that can be formed from the letters in the pool.
	 * If a node is discovered containing one or more anagrams, one is chosen at random,
	 * and the method halts.
	 *
	 * @param node				The node being searched
	 * @param charsFound 		chars found so far (the 'address' of the current node)
	 * @param poolRemaining 	chars left in the pool from which to form a word
	 * @param blanksRequired 	Blanks needed to make this word
	 */

	private void searchInPool(Node node, String charsFound, String poolRemaining, int blanksRequired) {

		if(charsFound.length() >= minLength + blanksRequired*(blankPenalty+1)) {
			if(!node.anagrams.isEmpty()) {
				int num = rgen.nextInt(node.anagrams.size());
				for(String anagram: node.anagrams) {
					if (--num < 0) {
						game.doMakeWord(robotName, anagram);
						return;
					}
				}
			}
		}

		for (int i = 0; i < poolRemaining.length(); i++) {
			char nextChar = poolRemaining.charAt(i);
			if (node.children.containsKey(nextChar)) {
				if (charsFound.length() + 1 + poolRemaining.length() - (i + 1) >= blanksRequired * (blankPenalty + 1) + minLength) {
					searchInPool(node.children.get(nextChar), charsFound + nextChar, poolRemaining.substring(i + 1), blanksRequired);
				}
			}

			if (blanksAvailable >= blanksRequired + 1 && charsFound.length() + 1 + poolRemaining.length() - i >= (blanksRequired + 1) * (blankPenalty + 1) + minLength) {
				for (Map.Entry<Character, Node> child : node.children.headMap(nextChar).entrySet()) {
					searchInPool(child.getValue(), charsFound + child.getKey(), poolRemaining.substring(i), blanksRequired + 1);
				}
			}
		}

		if (blanksAvailable >= (blanksRequired + 1) && charsFound.length() + 1 >= (blanksRequired + 1) * (blankPenalty + 1) + minLength) {
			for (Map.Entry<Character, Node> child : node.children.entrySet()) {
				searchInPool(child.getValue(), charsFound + child.getKey(), "", blanksRequired + 1);
			}
		}
	}




	/**
	*
	*/
	
	void makeTree(String shortWord) {
		trees.put(shortWord, new WordTree(shortWord.replaceAll("[a-z]",""), dictionary));
		commonTrees.put(shortWord, new WordTree(shortWord.replaceAll("[a-z]",""), dictionary.common));
	}
	
	/**
	*
	*/
	
	void removeTree(String stolenWord) {
		trees.remove(stolenWord);
		commonTrees.remove(stolenWord);
	}

	/**
	 * Attempts to find a steal
	 *
	 * @param words All the words on the board grouped by player
	 */
	
	private void searchForSteal(Game game, Hashtable<String,Vector<String>> words) {

		ArrayList<String> players = new ArrayList<>(words.keySet());
		Collections.shuffle(players);
		for(String player : players) {
			for(String shortWord : words.get(player)) {

				//decide whether to search among all words or among common subset
				HashMap<String, WordTree> treeSet = rgen.nextInt(1,4) >= skillLevel ? commonTrees : trees;

				if(treeSet.containsKey(shortWord)) {
					for(TreeNode child : treeSet.get(shortWord).rootNode.getChildren()) {
						String longWord = child.toString();
						if(game.doSteal(player, shortWord, robotName, longWord)) {
							return;
						}
					}
				}
			}
		}
	}
}