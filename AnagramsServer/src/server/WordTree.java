package server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Given a lexicon and a rootWord, creates a hierarchical tree structure graphically depicting
 * how the word can be "stolen" according to the rules of Anagrams.
 * <p>
 * For instance, LAUNCHPAD is a steal of CHALUPA, since LAUNCHPAD contains all the letters of CHALUPA
 * with at least one rearrangement. ANDROCEPHALOUS is in turn a steal of LAUNCHPAD.
 * <p>
 * On the other hand, PROMENADE is not a steal of POMADE because the latter can be formed from the
 * former by insertion of letters without rearrangement.
 *
 */
class WordTree {

	final AlphagramTrie trie;
	final TreeNode rootNode;
	String rootWord;
	private final TreeSet<TreeNode> treeNodeList = new TreeSet<>(new TreeNodeComparator());
	final JSONArray jsonArray = new JSONArray();

	/**
	 * Generates a tree from an existing trie
	 */
	WordTree(String rootWord, AlphagramTrie trie) {
		this.rootWord = rootWord.toUpperCase();
		rootNode = new TreeNode(rootWord, "", "");
		rootNode.setShortSteal("");
		rootNode.setProb(1);
		this.trie = trie;

		treeNodeList.add(rootNode);

		char[] rootChars = rootWord.toCharArray();
		Arrays.sort(rootChars);
		find(trie.rootNode, rootChars, "");

		while(treeNodeList.size() > 1)
			buildTree();

		sort(rootNode);

		if(trie.contains(rootWord)) {
			if(trie.getNode(rootWord).anagrams.containsKey(rootWord)) {
				String suffix = trie.getNode(rootWord).anagrams.get(rootWord);
				if (suffix != null) {
					rootNode.setSuffix(suffix);
					this.rootWord = rootWord + suffix;
				}
			}
			else {
				this.rootWord = rootWord.toLowerCase();
			}
		}
		else {
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
			for(Map.Entry<String, String> anagram : node.anagrams.entrySet()) {
				if(anagram.getKey().length() > rootWord.length()) {	 //This prevents the node list from including the node of the search key itself
					treeNodeList.add(new TreeNode(anagram.getKey(), anagram.getValue(), otherCharsFound));
				}
			}
			for(Map.Entry<Character,Node> child : node.children.entrySet())
				find(child.getValue(), charsToFind, otherCharsFound + child.getKey());
		}
	}

	/**
	 * Given the class's list of TreeNodes, each representing a single word,
	 * 	 sort them into a hierarchical structure such that:
	 * 		(1) each word becomes a child of the longest word whose letters are a strict subset of that word's letters
	 *		(2) provided that at least two letters of the shorter word are rearranged to form the longer word
	 *		(3) any word whose letters cannot be formed from a shorter word by rearrangement is eliminated
	 *		(4) the shortest word is the rootWord entered by the user.
	 */
	private void buildTree() {

		TreeNode currentNode = treeNodeList.pollFirst();
		String currentWord = currentNode.getWord();

		for (TreeNode nextNode : treeNodeList) {
			String nextWord = nextNode.getWord();

			if (Utils.isSteal(nextWord, currentWord)) {
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
			int result = node2.getWord().length() - node1.getWord().length();
			if(result == 0) {
				result = (node2.getWord()).compareTo(node1.getWord());
			}
			return result;
		}
	}

	/**
	 * Traverses the tree recursively and sorts the children of each node
	 *
	 * @param node The node whose children are to be sorted
	 */
	private void sort(TreeNode node) {
		node.getChildren().sort(new TreeNodeComparator().reversed());
		for(TreeNode child : node.getChildren()) {
			sort(child);
		}
	}

	/**
	 * Recursively generates a JSON-formatted list for displaying a word tree diagram
	 * 	 *'[{"id":"grubb", "longsteal": ""},{"id":"grubb.BUGBEAR", longsteal: "AE"}]'
	 */
	void generateJSON(String prefix, TreeNode node, double prob) {

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", prefix);
		jsonObject.put("shortsteal", node.getShortSteal());
		jsonObject.put("longsteal", node.getLongSteal());
		jsonObject.put("prob", ProbCalc.round(100 * prob, 1));
		jsonObject.put("def", trie.getDefinition(node.getWord()));
		jsonArray.put(jsonObject);

		double norm = 0;
		for (TreeNode child : node.getChildren()) {
			String nextSteal = child.getLongSteal();
			for (String s : node.getLongSteal().split("")) {
				nextSteal = nextSteal.replaceFirst(s, "");
			}
			child.setShortSteal(nextSteal);
			child.setProb(ProbCalc.getProbability(nextSteal));
			norm += child.getProb();
		}
		for (TreeNode child : node.getChildren()) {
			generateJSON(prefix + "." + child.toString(), child, prob*child.getProb()/norm);
		}
	}
}