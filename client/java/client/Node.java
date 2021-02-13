package client;

/*
* A node representing a unique combination of letters and possibly one or more anagrams,
* each representing a valid dictionary word.
*
*/

import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;

public class Node {
	
	public HashSet<String> anagrams = new HashSet<>();
	public HashMap<String, String> definitions = new HashMap<>();
	
	public TreeMap<Character, Node> children = new TreeMap<>();

}