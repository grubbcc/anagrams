package server;

/*
 * Given a lexicon and a rootWord, creates a hierarchical tree structure graphically depicting
 * how the word can be "stolen" according to the rules of Anagrams.
 *
 * For instance, LAUNCHPAD is a steal of CHALUPA, since LAUNCHPAD contains all the letters of CHALUPA
 * with at least one rearrangement. ANDROCEPHALOUS is in turn a steal of LAUNCHPAD.
 *
 * On the other hand, PROMENADE is not a steal of POMADE because the latter can be formed from the
 * former by insertion of letters without rearrangement.
 *
 */

import java.util.*;

public class WordTree {

	public final AlphagramTrie trie;
	public TreeNode root;
	String rootWord;
	private final TreeSet<TreeNode> treeNodeList = new TreeSet<>(new TreeNodeComparator());
	String JSON = "";

	/**
	 * Generates a tree from a existing trie
	 */

	public WordTree(String rootWord, AlphagramTrie trie) {
		this.rootWord = rootWord.toUpperCase();
		root = new TreeNode(rootWord, "");
		this.trie = trie;

		treeNodeList.add(root);

		char[] rootChars = rootWord.toCharArray();
		Arrays.sort(rootChars);
		find(trie.rootNode, rootChars, "");

		while(treeNodeList.size() > 1)
			buildTree();

		root.getChildren().sort(new TreeNodeComparator().reversed());
		if(!trie.contains(rootWord)) {
			this.rootWord = rootWord.toLowerCase();
		}

	}


	/**
	 * Recursively searches the children of a given node for words containing the given list of characters.
	 *
	 * @param node : a node in the trie
	 * @param charsToFind : characters that a descendant of the given node's path must contain to be considered a steal of the rootWord
	 */

	private void find(Node node, char[] charsToFind, String otherCharsFound) {

		if(charsToFind.length > 0) {
			Character firstChar = charsToFind[0];

			for(Map.Entry<Character,Node> child : node.children.headMap(firstChar, true).entrySet()) {
				if(child.getKey().equals(firstChar))
					find(child.getValue(), Arrays.copyOfRange(charsToFind, 1, charsToFind.length), otherCharsFound);
				else
					find(child.getValue(), charsToFind, otherCharsFound + child.getKey());
			}
		}

		//If the node's path already contains all characters to be found, then automatically add its descendants to the node list
		else {
			for(String anagram : node.anagrams) {
				if(anagram.length() > rootWord.length()) {	 //This prevents the node list from including the node of the search key itself
					treeNodeList.add(new TreeNode(anagram, otherCharsFound));
				}
			}
			for(Map.Entry<Character,Node> child : node.children.entrySet())
				find(child.getValue(), charsToFind, otherCharsFound + child.getKey());
		}
	}

	/**
	 * Given the class's list of DefaultMutableTreeNodes, each representing a single word,
	 * 	 sort them into a hierarchical structure such that:
	 * 		(1) each word becomes a child of the longest word whose letters are a strict subset of that word's letters
	 *		(2) provided that at least two letters of the shorter word must be rearranged to form the longer word
	 *		(3) any word whose letters cannot be formed from a shorter word by rearrangement is eliminated
	 *		(4) the shortest word is the rootWord entered by the user.
	 */

	private void buildTree() {

		TreeNode currentNode = treeNodeList.pollFirst();
		String currentWord = currentNode.toString();

		for (TreeNode nextNode : treeNodeList) {
			String nextWord = nextNode.toString();

			if (isSteal(nextWord, currentWord)) {
				nextNode.addChild(currentNode);
				return;
			}
		}

		//If a word is not a steal of any shorter word, that word is eliminated and its children are returned to the front of the queue
		treeNodeList.addAll(currentNode.getChildren());
	}

	/**
	 * A tool for sorting tree nodes (1) according to length, and in case of ties, (2) in alphabetical order.
	 */

	static class TreeNodeComparator implements Comparator<TreeNode> {
		@Override
		public int compare(TreeNode node1, TreeNode node2) {
			int result = node2.toString().length() - node1.toString().length();
			if(result == 0) {
				result = (node2.toString()).compareTo(node1.toString());
			}
			return result;
		}
	}

	/**
	 *
	 *'[{"id":"grubb", "tooltip": ""},{"id":"grubb.BUGBEAR", tooltip: "AE"}]'
	 * Recursively generates a JSON-formatted list for making a "flare" diagram
	 */

	public void generateJSON(String prefix, String tooltip, TreeNode node) {

		JSON = JSON.concat("{\"id\": \"" + prefix + "\", \"shorttip\": \"" + tooltip + "\", \"longtip\": \"" + node.getTooltip() + "\"},");
		for(TreeNode child : node.getChildren()) {
			String nextTooltip = child.getTooltip();
			for(char c : node.getTooltip().toCharArray()) {
				nextTooltip = nextTooltip.replaceFirst(c + "", "");
			}
			generateJSON(prefix + "." + child.toString(), nextTooltip, child);
		}
	}


	/**
	 * Given two words, determines whether one's letters are a strict subset of the other's.
	 *
	 * @param shortWord a shorter word
	 * @param longWord a longer word
	 */

	public static boolean isSubset(String shortWord, String longWord) {

		if(shortWord.length() >= longWord.length()) {
			return false;
		}

		String shortString = alphabetize(shortWord);
		String longString = alphabetize(longWord);

		while(shortString.length() > 0) {

			if(longString.length() == 0 ) {
				return false;
			}

			else if(shortString.charAt(0) < longString.charAt(0)) {
				return false;
			}
			else if(shortString.charAt(0) > longString.charAt(0)) {
				longString = longString.substring(1);
			}
			else if(shortString.charAt(0) == longString.charAt(0)) {
				shortString = shortString.substring(1);
				longString = longString.substring(1);
			}
		}
		return true;
	}

	/**
	 * Given two words, determines whether one's letters can be rearranged, with the addition of at least
	 * one letter not found in the word, to form the second word.
	 *
	 * @param shortWord a shorter word
	 * @param longWord a longer word
	 */

	public static boolean isSteal(String shortWord, String longWord) {

		String shortString = shortWord;
		String longString = longWord;

		if(!isSubset(shortString, longString))
			return false;
		else {
			while(longString.length() >= shortString.length() && shortString.length() > 0) {

				if(shortString.charAt(0) != longString.charAt(0)) {
					longString = longString.substring(1);
				}
				else {
					shortString = shortString.substring(1);
					longString = longString.substring(1);
				}
			}

			return shortString.length() > longString.length();
		}
	}


	/**
	 * Given a String, returns an "alphagram" consisting of its letters arranged in alphabetical order.
	 *
	 * @param entry: the letters to be rearranged
	 */

	public static String alphabetize(String entry) {
		char[] chars = entry.toCharArray();
		Arrays.sort(chars);
		return new String(chars);
	}

}