package server;

import java.util.TreeMap;
import java.util.HashSet;
import java.util.HashMap;

/**
 * A node representing a unique combination of letters and possibly one or more anagrams,
 * each of which is a valid dictionary word.
 *
 */

class Node {

	final HashSet<Word> words = new HashSet<>(2);

	final TreeMap<Character, Node> children = new TreeMap<>();

}