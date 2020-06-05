/****
* Given a lexicon and a rootWord, creates a hierarchial tree structure graphically depicting
* how the word can be "stolen" according to the rules of Anagrams.
*
* For instance, LAUNCHPAD is a steal of CHALUPA, since LAUNCHPAD contains all the letters of CHALPUA
* with at least one rearrangement. ANDROCEPHALOUS is in turn a steal of LAUNCHPAD.
* However, PROMENADE is not a steal of POMADE because the latter can be formed from the former by
* insertion of letters without rearrangement.
*
* TO DO: add an asterisk next to words that are not in the dictionary.
*****/


import java.io.*;
import java.util.Scanner;
import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.*;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.util.Map;
import java.awt.Color;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Enumeration;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Iterator;

public class WordTree extends JTree {
	
	private TreeSet<DefaultMutableTreeNode> treeNodeList;
	private String rootWord;
	public AlphagramTrie trie;
	public DefaultMutableTreeNode root;

	/**
	* Creates an empty WordTree object which serves only as a container for a trie
	*/
	
	public WordTree(String lexicon) {
		this.trie = new AlphagramTrie(lexicon);
	}
	
    /**
     * Generates a tree from a newly created trie of the given lexicon
     */
	
	public WordTree(DefaultMutableTreeNode root, String lexicon) {
		this(root, new AlphagramTrie(lexicon));
	}

    /**
     * Generates a tree from a existing trie
     */
	
	public WordTree(DefaultMutableTreeNode root, AlphagramTrie trie) {

		super(root);		
		this.root = root;	
		this.trie = trie;
				
		treeNodeList = new TreeSet<DefaultMutableTreeNode>(new TreeNodeComparator());
		treeNodeList.add(this.root);

		rootWord = root.toString();		
		char[] rootChars = rootWord.toCharArray();
		Arrays.sort(rootChars);
		find(trie.rootNode, rootChars, "");
		
		while(treeNodeList.size() > 1)
			buildTree();

		sortChildren();

		if(!trie.contains(rootWord))
			root.setUserObject(rootWord.toLowerCase());
	}
	
	
	/**
	* A tool for sorting tree nodes (1) according to length, and in case of ties, (2) in alphabetical order.
	*/
	
	static class TreeNodeComparator implements Comparator<TreeNode> {
		@Override
		public int compare(TreeNode node1, TreeNode node2) {
			int result = (node2.toString()).length() - (node1.toString()).length();
			if(result == 0) {
				result = (node2.toString()).compareTo(node1.toString());
			}
			return result;
		}
	}


	/**
	* Recursively searches the children of a given node for words containing the given list of characters.
	*
	* @param Node node : a node in the trie
	* @param char[] charsToFind : characters that a descendent of the given node's path must contain to be considered a steal of the rootWord
	*/
	
	private void find(Node node, char[] charsToFind, String otherCharsFound) {
	
		if(charsToFind.length > 0) {
			Character firstChar = Character.valueOf(charsToFind[0]);
			
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
					treeNodeList.add(new DefaultMutableTreeNode(new UserObject(anagram, otherCharsFound)));
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

		DefaultMutableTreeNode currentNode = treeNodeList.pollFirst();
		String currentWord = currentNode.toString();

		Iterator<DefaultMutableTreeNode> iterator = treeNodeList.iterator();

		while(iterator.hasNext()) {
			DefaultMutableTreeNode nextNode = iterator.next();
			String nextWord = nextNode.toString();
			
			if(isSteal(nextWord, currentWord)) {
				nextNode.insert(currentNode, 0);
				return;
			}
		}
		
		//If a word is not a steal of any shorter word, that word is eliminated and its children are returned to the front of the queue
		Enumeration e = currentNode.children();
		while(e.hasMoreElements()) {
			DefaultMutableTreeNode orphanNode = (DefaultMutableTreeNode)e.nextElement();
			treeNodeList.add(orphanNode);
		}
	}

	/**
	* Make sure the children of the root node are listed in order from shortest to longest.
	*/

	private void sortChildren() {

		ArrayList<TreeNode> children = Collections.list(root.children());
	
		Collections.sort(children, new TreeNodeComparator());

		root.removeAllChildren();

		for (TreeNode child : children)
			root.insert((DefaultMutableTreeNode)child, 0);
	}

	
	/**
	* Creates a JTable showing the number of steals of the rootWord organized by word length
	*/

	public JTable treeSummary() {
		TreeMap<Integer, Integer> nodeSummary = new TreeMap<Integer, Integer>();
		Enumeration e = root.preorderEnumeration();
		while(e.hasMoreElements()) {
			DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode)e.nextElement();
		
			String nextWord = nextNode.toString();
			
			if(nodeSummary.containsKey(nextWord.length()))
				nodeSummary.replace(nextWord.length(), Integer.valueOf(nodeSummary.get(nextWord.length()).intValue() + 1));
			else
				nodeSummary.put(nextWord.length(), Integer.valueOf(1));
		}
		
		String[][] array = new String[nodeSummary.size()][2];
		int i = 0;
		for(Map.Entry<Integer,Integer> entry : nodeSummary.entrySet()) {
			array[i][0] = entry.getKey().toString();
			array[i][1] = entry.getValue().toString();
			i++;
		}
		array[0][0] = "length";
		array[0][1] = "num words";	
		JTable summary = new JTable(array, new String[] {"length", "num words"});	

		return summary;

		
	}
	
	/**
	* Given two words, determines whether one's letters are a strict subset of the other's.
	*
	* @param String shortWord
	* @param String longWord
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
				continue;
			}
			else if(shortString.charAt(0) == longString.charAt(0)) {
				shortString = shortString.substring(1);
				longString = longString.substring(1);
				continue;
			}
		}
		return true;
	}
	
	/**
	* Given two words, determines whether one's letters can be rearranged, with the addition of at least
	* one letter not found in the word, to form the second word.
	*
	* @param String shortWord
	* @param String longWord
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
					continue;
				}
				else {
					shortString = shortString.substring(1);
					longString = longString.substring(1);
					continue;
				}
			}
			
			if(shortString.length() > longString.length()) 
				return true;
			else 
				return false;
		}
	}


	
	/**
	* Given a String, returns an "alphagram" consisting of its letters arranged in alphabetical order.
	* 
	* @param String entry: the letters to be rearranged
	*/
	public static String alphabetize(String entry) {

	    char[] chars = entry.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
	
	}

	/**
	*
	*/
	
/*	public static void main(String[] args) {

		String lexicon = "CSW19";
		String baseWord = "RACHEL";
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(baseWord.toUpperCase());
		
        WordTree tree = new WordTree(root, lexicon);
		tree.setRootVisible(true);
		tree.expandRow(0);
	
		JFrame frame = new JFrame();
		frame.setSize(400, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		JPanel panel = new JPanel();  
		panel.setBackground(Color.WHITE);
		frame.add(new JScrollPane(panel), BorderLayout.CENTER);  
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(tree);
		frame.revalidate();		
	}*/
}