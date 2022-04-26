package server;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */

class WordFinder {

    private final int blankPenalty;
    private final int minLength;
    private String tilePool;
    private final AlphagramTrie dictionary;
    private int blanksAvailable = 0;

    private final HashSet<String> wordsInPool = new HashSet<>();
    HashMap<String, WordTree> trees = new HashMap<>();

    /**
     *
     */

    WordFinder(int minLength, int blankPenalty, AlphagramTrie dictionary) {

        this.minLength = minLength;
        this.blankPenalty = blankPenalty;
        this.dictionary = dictionary;

    }

    /**
     * Given a gameState, generates a list of all possible (up to 70) valid plays, e.g.
     * [FUMITORY] @ [BLEWARTS + I -> BRAWLIEST, BLEWARTS + I -> WARBLIEST, BLEWARTS + FO -> BATFOWLERS]
     *
     * @param gameState a description of the current position on the board,
     *                  including the pool and words already formed, e.g.
     *                 "257 YUIFOTMR GrubbTime [HAUYNES] Robot-Genius [BLEWARTS,POTJIES]"
     * @return          a list of all possible (up to 70) valid plays that can be made, e.g.
     *                  [FUMITORY] @ [BLEWARTS + I -> BRAWLIEST, BLEWARTS + I -> WARBLIEST, BLEWARTS + FO -> BATFOWLERS]
     */

    synchronized String findWords(String gameState) {

        wordsInPool.clear();
        blanksAvailable = 0;

        //Find words in pool
        String tiles = (gameState.split(" ")[1]).replace("#" ,"");
        tilePool = tiles.length() <= 20 ? tiles : tiles.substring(0, 20); //is this necessary?
        if(tilePool.length() >= minLength) {
            blanksAvailable = tilePool.length() - tilePool.replace("?","").length();
            findInPool(dictionary.rootNode, "", AlphagramTrie.alphabetize(tilePool.replace("?","")), 0);
        }

        //build string
        StringBuilder wordsFound = new StringBuilder("[");
        for(String word : wordsInPool) {
            wordsFound.append(word).append(",");
        }
        wordsFound.append("] @ [");

        //Find steals
        if(!tilePool.isEmpty()) {
            Matcher m = Pattern.compile("\\[([,A-z]+?)]").matcher(gameState);
            while (m.find()) {
                wordsFound.append(searchForSteals(m.group(1).split(",")));
            }
        }
        return wordsFound.append("]").toString();
    }

    /**
     * Recursively searches the pool for words and adds them to the wordsInPool Set
     *
     * @param node 				The node being searched
     * @param charsFound 		chars found so far (the 'address' of the current node)
     * @param poolRemaining 	chars left in the pool from which to form a word
     * @param blanksRequired 	Blanks needed to make this word
     */

    private void findInPool(Node node, String charsFound, String poolRemaining, int blanksRequired) {

        if(wordsInPool.size() >= 40) {
            return;
        }

        else if(charsFound.length() >= minLength + blanksRequired*(blankPenalty+1)) {
            if(blanksRequired == 0) {
                wordsInPool.addAll(node.anagrams);
            }
            else {
                for(String anagram : node.anagrams) {
                    String newWord = "";
                    String tiles = tilePool;
                    for (String s : anagram.split("")) {
                        //move a non-blank tile to the new word
                        if (tiles.contains(s)) {
                            tiles = tiles.replaceFirst(s, "");
                            newWord = newWord.concat(s);
                        }
                        //move a blank to the new word and designate it
                        else {
                            tiles = tiles.replaceFirst("\\?", "");
                            newWord = newWord.concat(s.toLowerCase());
                        }
                    }
                    wordsInPool.add(newWord);
                }
            }
        }

        for(int i = 0; i < poolRemaining.length(); i++) {
            char nextChar = poolRemaining.charAt(i);
            if(node.children.containsKey(nextChar)) {
                if(charsFound.length()+1 + poolRemaining.length()-(i+1) >= blanksRequired*(blankPenalty+1) + minLength) {
                    findInPool(node.children.get(nextChar), charsFound + nextChar, poolRemaining.substring(i+1), blanksRequired);
                }
            }

            if(blanksAvailable >= blanksRequired+1 && charsFound.length()+1 + poolRemaining.length()-i >= (blanksRequired+1)*(blankPenalty+1) + minLength) {
                for(Map.Entry<Character, Node> child : node.children.headMap(nextChar).entrySet()) {
                    findInPool(child.getValue(), charsFound + child.getKey(), poolRemaining.substring(i), blanksRequired + 1);
                }
            }
        }

        if (blanksAvailable >= (blanksRequired + 1) && charsFound.length()+1 >= (blanksRequired+1)*(blankPenalty+1) + minLength) {
            for(Map.Entry<Character, Node> child : node.children.entrySet()) {
                findInPool(child.getValue(), charsFound + child.getKey(), "", blanksRequired + 1);
            }
        }
    }


    /**
     * Finds all (up to 30) possible first-order steals (i.e. steals that are not steals of steals).
     *
     * @param   words   All the words currently on the board
     * @return          a comma-separated list of up to 30 steals of the form BLEWARTS + FO -> BATFOWLERS)
     */

    private synchronized String searchForSteals(String[] words) {
        StringBuilder possibleSteals = new StringBuilder();
        int numWordsFound = 0;

        outer: for(String shortWord : words) {
            trees.putIfAbsent(shortWord, new WordTree(shortWord.replaceAll("[a-z]", ""), dictionary));
            for (TreeNode child : trees.get(shortWord).rootNode.getChildren()) {
                String entry = child.toString();
                if (entry.length() <= shortWord.length()) continue;
                if (entry.length() > shortWord.length() + tilePool.length()) break;
                Play play = new Play(shortWord, entry, tilePool, minLength, blankPenalty);
                if (play.isValid()) {
                    possibleSteals.append(shortWord).append(" + ").append(child.getLongSteal()).append(" -> ").append(play.nextWord()).append(",");
                    if (++numWordsFound >= 30) {
                        break outer;
                    }
                }
            }
        }

        return possibleSteals.toString();
    }

}