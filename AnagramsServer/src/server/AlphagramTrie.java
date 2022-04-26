package server;

import java.io.*;
import java.util.Arrays;

/**
 * Stores a list of words as a trie data structure for fast lookup.
 * See: https://en.wikipedia.org/wiki/Trie
 * Words are indexed according to a unique alphabetical ordering ("alphagram") so that
 * words which are anagrams of each other are stored in the same node.
 *
 */


class AlphagramTrie {

	public final Node rootNode = new Node();
	public final String lexicon;
	private String currentWord;
	private String currentDefinition;
	public AlphagramTrie common;

	/**
	 * Creates a root node to serve as the base of the trie.
	 * Reads the words from the file, formats them, and adds them to the trie.
	 *
	 * @param lexicon the name of the word list. This should be stored in a file called <lexicon>.txt
	 *                with one word per line and definition (optional) separated by white space.
	 */

	AlphagramTrie(String lexicon) {

		this.lexicon = lexicon;

		try {
			InputStream stream = getClass().getResourceAsStream("/wordlists/" + lexicon + ".txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			for(String stringRead = reader.readLine(); stringRead != null; stringRead = reader.readLine()) {
				if (!stringRead.isBlank() || stringRead.startsWith("#")) {
					String[] entry = stringRead.split("\\s+", 2);
					currentWord = entry[0].toUpperCase();
					if (entry.length > 1)
						currentDefinition = entry[1];
					insertWord(alphabetize(currentWord), rootNode);
				}
			}
			System.out.println("new trie created with lexicon = " + lexicon);
		}
		catch (FileNotFoundException exception) {
			System.out.println("Word list not found.");
		}
		catch (IOException ioexception) {
			System.out.println(ioexception.getMessage());
		}
	}

	/**
	 *  Generates the subset of this trie containing only common words
	 */

	void common() {
		common = new AlphagramTrie(lexicon + "-common");
	}


	/**
	 * Adds a new alphagram to the trie, one letter at a time, recursively. (An alphagram is a unique
	 * alphabetical ordering of the letters in a word.)
	 *
	 * @param subalphagram: the suffix letters of the current alphagram which have not been added to the trie
	 * @param node			the node whose path represents the prefix letters which have already been added
	 */

	private void insertWord(String subalphagram, Node node) {

		final Node nextChild;

		//The trie already contains an alphagram with the same prefix
		if (node.children.containsKey(subalphagram.charAt(0))) {
			nextChild = node.children.get(subalphagram.charAt(0));
		}
		else {
			//Add a new subtrie representing this alphagram's unique suffix
			nextChild = new Node();
			node.children.put(subalphagram.charAt(0), nextChild);
		}

		//This node is the end of a word. Store the original word and its definition here.
		if (subalphagram.length() == 1) {
			nextChild.anagrams.add(currentWord);
			nextChild.definitions.put(currentWord, currentDefinition);
		}
		else {
			//This node is not the end of a word. Remove the first letter and continue
			insertWord(subalphagram.substring(1), nextChild);
		}
	}

	/**
	 * Checks whether the given entry is a valid word in this trie's lexicon
	 *
	 * @param searchKey the String to be searched for
	 */

	boolean contains(String searchKey) {

		Node node = getNode(searchKey);

		if(getNode(searchKey) != null) {
			//A node has been found with the same alphagram as the searchKey
			for(String anagram : node.anagrams) {
				if(searchKey.toUpperCase().equals(anagram)) {
					return true; //the node contains the word
				}
			}
		}
		return false;
	}

	/**
	 * Given a series of letters, not necessarily ordered, returns the corresponding node in
	 * the trie. If the node does not exist, returns null.
	 *
	 * @param searchKey the sequence of letters corresponding to the address of the searched-for node
	 */

	Node getNode(String searchKey) {

		String alphagram = alphabetize(searchKey.toUpperCase());
		Node node = rootNode;

		//Recursively searches through the trie, one letter at a time
		while (alphagram.length() > 0) {
			if(node.children.containsKey(alphagram.charAt(0))) {
				node = node.children.get(alphagram.charAt(0));
				alphagram = alphagram.substring(1);
			}
			else //The trie does not contain any node matching the searchKey
				return null;
		}

		return node;
	}

	/**
	 *
	 */

	String getDefinition(String searchKey) {
		Node node = getNode(searchKey);
		if(node != null)
			return node.definitions.get(searchKey.toUpperCase());
		else
			return null;
	}

	/**
	 * Given a String, creates an "alphagram" consisting of its letters arranged in alphabetical order.
	 *
	 * @param entry: the letters to be rearranged
	 */

	static String alphabetize(String entry) {
		char[] chars = entry.toCharArray();
		Arrays.sort(chars);
		return new String(chars);
	}

}