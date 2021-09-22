package server;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */

class WordFinder {

    private int blankPenalty = 0;
    private int minLength = 0;
    private String tilePool;
    private final AlphagramTrie dictionary;
    private int blanksAvailable = 0;

    private final HashSet<String> wordsInPool = new HashSet<>();
    private final HashMap<String, WordTree> trees = new HashMap<>();

    /**
     *
     */

    public WordFinder(int minLength, int blankPenalty, AlphagramTrie dictionary) {

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
     * @return a list of all possible (up to 70) valid plays that can be made, e.g.
     *         [FUMITORY] @ [BLEWARTS + I -> BRAWLIEST, BLEWARTS + I -> WARBLIEST, BLEWARTS + FO -> BATFOWLERS]
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
     * @param words All the words currently on the board
     * @return a comma-separated list of up to 30 steals of the form BLEWARTS + FO -> BATFOWLERS)
     */

    synchronized private String searchForSteals(String[] words) {
        StringBuilder possibleSteals = new StringBuilder();
        int numWordsFound = 0;

        outer: for(String shortWord : words) {
            trees.putIfAbsent(shortWord, new WordTree(shortWord.replaceAll("[a-z]", ""), dictionary));
            for (TreeNode child : trees.get(shortWord).root.getChildren()) {
                String entry = child.toString();

                if (entry.length() <= shortWord.length() + tilePool.length() && entry.length() > shortWord.length()) {
                    String longWord = checkSteal(shortWord, entry);
                    if (longWord != null) {
                        possibleSteals.append(shortWord).append(" + ").append(child.getTooltip()).append(" -> ").append(longWord).append(",");
                        if(++numWordsFound >= 30) {
                            break outer;
                        }
                    }
                }
            }
        }

        return possibleSteals.toString();
    }

    /**
     * Given a word, determines whether the appropriate tiles can be found in the pool. If so,
     * the word is awarded to the player, the tiles are removed from the pool, and the players
     * and watchers are notified.
     *
     * @param 	longWord 		A new word the player is attempting to make.
     * @param   shortWord       The word the player is attempting to steal. (An empty shortWord
     *                          indicates that the word is to be taken directly from the pool
     *                          without stealing.)
     * @return 					Whether the word is taken successfully
     */

    String checkSteal(String shortWord, String longWord) {

        // charsToFind contains the letters that cannot be found in the existing word;
        // they must be taken from the pool or a blank must be redesignated.
        String charsToFind = longWord;
        int blanksToChange = 0;

        //lowercase is used to represent blanks
        //If the shortWord contains a tile not found in the longWord, it cannot be stolen unless that tile is a blank
        for(String s : shortWord.split("")) {
            if(charsToFind.contains(s.toUpperCase()))
                charsToFind = charsToFind.replaceFirst(s.toUpperCase(), "");
            else {
                if(Character.isLowerCase(s.charAt(0)))
                    blanksToChange++;
                else
                    return null;
            }
        }

        int missingTiles = 0;
        String tiles = tilePool;
        for(String s : charsToFind.split("")) {
            if(tiles.contains(s))
                tiles = tiles.replaceFirst(s, "");
            else
                missingTiles++;
        }
        int blanksToTakeFromPool = missingTiles - blanksToChange;

        int blanksInPool = tilePool.length() - tilePool.replace("?", "").length();
        if(blanksInPool < blanksToTakeFromPool) {
            return null; //not enough blanks in the pool
        }

        //Calculate if the word is long enough, accounting for the blankPenalty
        if(longWord.length() - shortWord.length() < blankPenalty*blanksToChange + (blankPenalty + 1)*blanksToTakeFromPool) {
            return null;
        }

        //steal is successful
        String oldWord = shortWord;
        String newWord = "";
        tiles = tilePool;

        for(String s : longWord.split("")) {
            //move a non-blank from the old word to the new word
            if (oldWord.contains(s)) {
                oldWord = oldWord.replaceFirst(s, "");
                newWord = newWord.concat(s);
            }
            //move a blank from the old word to the new word without redesignating it
            else if (oldWord.contains(s.toLowerCase())) {
                oldWord = oldWord.replaceFirst(s.toLowerCase(), "");
                newWord = newWord.concat(s.toLowerCase());
            }
            else if(charsToFind.length() - blanksToChange > 0 ) {
                //take a non-blank from the pool
                if(tiles.contains(s)) {
                    tiles = tiles.replaceFirst(s, "");
                    charsToFind = charsToFind.replaceFirst(s, "");
                    newWord = newWord.concat(s);
                }
                //take a blank from the pool and designate it
                else {
                    tiles = tiles.replaceFirst("\\?", "");
                    newWord = newWord.concat(s.toLowerCase());
                }
            }
            //move a blank from the old word to the new word and redesignate it
            else {
                oldWord = oldWord.replaceFirst("[a-z]","");
                newWord = newWord.concat(s.toLowerCase());
            }
        }
        return newWord;

    }

}
