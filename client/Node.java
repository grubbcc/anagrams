/****
* A node representing a unique combination of letters and possibly one or more anagrams,
* each representing a valid dictionary word.
*
* TO DO: replace the node HashSet with a HashTable representing an <anagram, definition> structure
*
****/



import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;

public class Node {
	
	public HashSet<String> anagrams = new HashSet<String>();
	public HashMap<String, String> definitions = new HashMap<String, String>();
	
	public TreeMap<Character, Node> children = new TreeMap<Character, Node>();

}
