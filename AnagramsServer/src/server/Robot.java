package server;

import java.util.*;
import java.util.prefs.Preferences;

/**
* An artificial intelligence that uses wordTrees to find words and make steals
*/
class Robot extends Player {

	int thinkTime = 2;	//when thinkTime reaches 0, Robot will attempt a play

	final int skillLevel;
	final private Game game;

	private final static String[] NAMES = {"Robot-Novice", "Robot-Player", "Robot-Expert", "Robot-Genius"};
	private final int blankPenalty;
	private final int minLength;
	private final int rating;
	final HashMap<String, WordTree> trees = new HashMap<>();
	private final HashMap<String, WordTree> commonTrees = new HashMap<>();
	private final AlphagramTrie dictionary;
	final private Random rgen = new Random();
	private boolean wordFound = false;

	private int blanksAvailable;

	Preferences prefs;

	/**
	*
	*/
	Robot(Game game, int skillLevel, AlphagramTrie dictionary, int minLength, int blankPenalty) {
		super(game, NAMES[skillLevel]);
		this.name = NAMES[skillLevel];
		prefs = Preferences.userNodeForPackage(getClass()).node(name);
		this.rating = prefs.getInt("rating", 1500);
		this.game = game;

		this.dictionary = dictionary;

		this.minLength = minLength;
		this.blankPenalty = blankPenalty;
		this.skillLevel = skillLevel;
	}

	/**
	 *
	 */
	@Override
	int getRating() {
		return rating;
	}

	/**
	 *
	 */
	@Override
	void updateRating(int newRating) {
		prefs.putInt("rating", newRating);
	}

	/**
	 * Determines whether the Robot will attempt to make a play.
	 */
	boolean think(int tilesPlayed, int tilesInBag, int tilesInPool) {
		if (tilesPlayed < tilesInBag) {
			if (tilesInPool >= 29) {
				return true;
			}
			if (rgen.nextInt(100) <= 1 + 4*(skillLevel - 1) + game.delay + 6*(tilesInPool/minLength - 1)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Either attempt to steal a word or to make a word from the letters in the pool
	 */
	void makePlay() {

		String tilePool = game.tilePool;

		blanksAvailable = tilePool.length() - tilePool.replace("?", "").length();

		if (tilePool.length() >= 2 * minLength || rgen.nextInt(2) == 0 && tilePool.length() >= minLength + 1) {

			//decide whether to search among all words or among common subset
			Node rootNode = rgen.nextInt(3) + 1 >= skillLevel ? dictionary.common.rootNode : dictionary.rootNode;
			wordFound = false;
			searchInPool(rootNode, "", Utils.alphabetize(tilePool.replace("?", "")), 0);
		}
		else {
			searchForSteal();
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
		if(wordFound) return;
		if(charsFound.length() >= minLength + blanksRequired*(blankPenalty+1)) {
			if(!node.words.isEmpty()) {
				int num = rgen.nextInt(node.words.size());
				for(Word anagram : node.words) {
					if (--num < 0) {
						wordFound = true;
						game.doMakeWord(name, anagram.letters);
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
	private void searchForSteal() {

		List<Player> players = new ArrayList<>(game.players.values());
		Collections.shuffle(players);
		for(Player player : players) {
			for(String shortWord : player.words) {

				//decide whether to search among all words or among common subset
				HashMap<String, WordTree> treeSet = rgen.nextInt(3) + 1 >= skillLevel ? commonTrees : trees;

				if(treeSet.containsKey(shortWord)) {
					for(TreeNode child : treeSet.get(shortWord).rootNode.getChildren()) {
						String longWord = child.getWord().letters;
						if(game.doSteal(player.name, shortWord, name, longWord)) {
							return;
						}
					}
				}
			}
		}
	}
}