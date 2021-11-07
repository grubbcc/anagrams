package server;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.io.*;

/**
* 
*/

class Analyzer {

	private String gameID = null;
	private int blankPenalty = 0;
	private int minLength = 0;
	private String tilePool = null;
	private int blanksAvailable = 0;
	private int numSets = 0;
	private String lexicon = null;
	private String speed = null;
	private AlphagramTrie dictionary;
	
	private HashSet<String> players = new HashSet<>();
	private Hashtable<String, WordTree> trees = new Hashtable<>();
	private Hashtable<String, Vector<String>> words = new Hashtable<>();
	private Hashtable<String, Integer> makes = new Hashtable<String, Integer>();
	private HashSet<String> foundWords = new HashSet<>();
	private Vector<String> misses = new Vector<>();


	public Analyzer(String gameID, int minLength, int blankPenalty, AlphagramTrie dictionary) {
		this.gameID = gameID;
		this.minLength = minLength;
		this.blankPenalty = blankPenalty;
		this.dictionary = dictionary;
		this.lexicon = dictionary.lexicon;
		
		analyze();
	}
	
	/**
	*
	*/
	
	public Analyzer(String gameID) {
		
		readPreamble(gameID);
		analyze();
	}
	
	/**
	*
	*/
	
	public void readPreamble(String gameID) {
		this.gameID = gameID;
		String[] tokens;
		String cmd;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader("game logs/log" + gameID + ".txt"));
			for(String newLine = reader.readLine(); newLine != null; newLine = reader.readLine()) {


				if(!newLine.trim().isEmpty()) {
					
					//read preamble
					tokens = newLine.split(" ");
					cmd = tokens[0];

					if(cmd.equalsIgnoreCase("numSets")) {
						numSets = Integer.parseInt(tokens[1]);
					}
					else if(cmd.equalsIgnoreCase("lexicon")) {
						lexicon = tokens[1];
						if(dictionary == null) {
							dictionary = new AlphagramTrie(lexicon);
						}
					}
					else if(cmd.equalsIgnoreCase("minLength")) {
						minLength = Integer.parseInt(tokens[1]);
					}
					else if(cmd.equalsIgnoreCase("blankPenalty")) {
						blankPenalty = Integer.parseInt(tokens[1]);
					}
					else if(cmd.equalsIgnoreCase("speed")) {
						speed = tokens[1];
					}
				}
			}
			reader.close();	
		}
		catch(IOException e) {
			e.printStackTrace();
		}

	}		
		
	/**
	*
	*/

	void analyze () {

		String[] tokens;
		String cmd;
		int currentTime = 0;
	
		try {
			BufferedReader reader = new BufferedReader(new FileReader("game logs/log" + gameID + ".txt"));
			BufferedWriter writer = new BufferedWriter(new FileWriter("game logs/anno" + gameID + ".txt"));
			for(String newLine = reader.readLine(); newLine != null; newLine = reader.readLine()) {
	
				//read game data
				if(!newLine.isBlank()) {

					tokens = newLine.split(" ");
					cmd = tokens[1];
					if(newLine.charAt(0) >= '0' && newLine.charAt(0) <= '9') {
						
						currentTime = Integer.parseInt(tokens[0]);
						cmd = tokens[1];

						if(cmd.equals("nexttiles")) {
							tilePool = tokens[2];

							if(tilePool.length() >= minLength) {
								searchForWords("", "", tilePool.replace("?", ""), minLength);
							}
							if(!words.isEmpty() && !tilePool.isEmpty()) {
								searchForSteals(words);
							}
						}
						else if(cmd.equals("addword")) {
							foundWords = new HashSet<>();
							words.get(tokens[2]).add(tokens[3]);
							trees.put(tokens[3], new WordTree(tokens[3].replaceAll("[a-z]",""), dictionary));
							
						}
						else if(cmd.equals("removeword")) {
							words.get(tokens[2]).remove(tokens[3]);
							trees.remove(tokens[3]);							
						}
						else if(cmd.equals("takeseat")) {
							players.add(tokens[2]);
							words.put(tokens[2], new Vector<String>());
						}
						else if(cmd.equals("removeplayer")) {
							players.remove(tokens[2]);
							if(words.get(tokens[2]).isEmpty()) {
								words.remove(tokens[2]);
							}
						}
					}
				}
				//specification: time tiles missedwords player playerwords
				writer.write(currentTime + " " + tilePool + " " + foundWords.toString().replace(", ",",") + " " + getFormattedWordList() + "\n");

			}
			reader.close();
			writer.flush();
			writer.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}

	/**
	*
	*/

	private String getFormattedWordList() {
		String wordList = new String();
		HashSet<String> union = new HashSet<String>();
		union.addAll(players);
		union.addAll(words.keySet());
		for(String playerName : union) {
			if(words.containsKey(playerName)) {
				wordList += playerName + " " + words.get(playerName).toString().replace(", ", ",") + " ";
				
			}
			else {
				wordList += playerName + " [] ";
			}
		}
		return wordList;
	}
	
	/**
	*
	*/

	void searchForWords(String key, String pool, String rest, int charsToTake) {
		
		if(rest.length() == charsToTake) {
			key += rest;

			if(dictionary.getNode(key) != null) {
				
				searchForWord(dictionary.getNode(key), pool, 0);
				return;
			}
		}
		
		for(int i = 0; i < rest.length() && i <= charsToTake; i++) {
	
			searchForWords(key + rest.substring(0,i), pool + rest.substring(i, i+1), rest.substring(i+1, rest.length()), charsToTake-i);
		}
	}
	
	/**
	* Given a node in the dictionary trie containing a known set of letters, recursively searches for words that can be constructed 
	* using the remaining letters in the pool.
	*/

	void searchForWord(Node node, String remainingPool, int blanksRequired) {
		for(String anagram : node.anagrams) {
			if(anagram.length() >= minLength + blanksRequired*blankPenalty) {
				foundWords.add(anagram);
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
		}
	}

	
	/**
	*
	*/
	
	void recordSteal(String newWord) {
		if(makes.containsKey(newWord)) {
			int newCount = makes.get(newWord).intValue() + 1;
			makes.put(newWord, Integer.valueOf(newCount));
		}
		else {
			makes.put(newWord, Integer.valueOf(1));
		}
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
	
	void searchForSteals(Hashtable<String, Vector<String>> words) {
		
		for(String shortWord : trees.keySet()) {

			for (TreeNode child : trees.get(shortWord).rootNode.getChildren()) {

				String longWord = child.toString();
				if(longWord.length() <= shortWord.length() + tilePool.length()) {
					if(checkSteal(shortWord, longWord)) {
						foundWords.add(longWord);

					}
				}
			}
		}

	}

	/**
	*
	*/

	boolean checkSteal(String shortWord, String longWord) {

		String charsToFind = longWord;

		int blanksAvailable = tilePool.length() - tilePool.replace("?", "").length();
		int blanksToChange = 0;
		int blanksToTakeFromPool = 0;
		
		for(String s : shortWord.split("")) {
			if(charsToFind.contains(s.toUpperCase()))
				charsToFind = charsToFind.replaceFirst(s.toUpperCase(), "");
			else {
				if(Character.isLowerCase(s.charAt(0))) {
					blanksToChange++;
					blanksAvailable++;
				}
				else
					return false;
			}
		}

		String tiles = tilePool;	
		for(String s : charsToFind.split("")) {
			if(tiles.contains(s))
				tiles = tiles.replaceFirst(s, "");
			else
				blanksToTakeFromPool++;
		}
		
		if(blanksAvailable - blanksToChange < blanksToTakeFromPool)
			return false; //not enough blanks in the pool
		
		int additionalTilesRequired = 1;
		if(blanksToTakeFromPool > 0 || blanksToChange > 0)
			additionalTilesRequired = blankPenalty*blanksToChange + (blankPenalty + 1)*blanksToTakeFromPool;

		if(longWord.length() - shortWord.length() < additionalTilesRequired)
			return false;
		
		return true;
	}
	
	/**
	*
	*/
	
	public static void main(String[] args) {
		
		new Analyzer(args[0]);
	}
}