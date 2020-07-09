/**
* A node representing a unique alphagram, i.e. an alphabetized list of letters, containing:
*  • a number of anagram Strings coorresponding to this alphagram, each each of which 
*    represents a valid dictionary word
*  • a sorted mapping of characters onto child nodes each representing a 
*    node containing a single additional letter, that character
*
*/

import java.util.TreeMap;
import java.util.HashSet;

public class Node {
	
	public HashSet<String> anagrams = new HashSet<String>();
	public TreeMap<Character, Node> children = new TreeMap<Character, Node>();

}
