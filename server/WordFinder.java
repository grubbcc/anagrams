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
     *
     */

    public String findWords(String gameState) {

        wordsInPool.clear();
        blanksAvailable = 0;
        String tiles = (gameState.split(" ")[1]).replace("#" ,"");
        tilePool = tiles.length() <= 20 ? tiles : tiles.substring(0, 20);
        if(tilePool.length() >= minLength) {
            blanksAvailable = tilePool.length() - tilePool.replace("?","").length();
            searchPool("", "", tilePool.replace("?", ""), minLength);
        }

        StringBuilder wordsFound = new StringBuilder("[");
        for(String word : wordsInPool) {
            wordsFound.append(word).append(",");
        }
        wordsFound.append("] @ [");

        Matcher m = Pattern.compile("\\[([,A-z]+?)]").matcher(gameState);
        while(m.find()) {
            wordsFound.append(searchForSteals(m.group(1).split(",")));
        }

        return wordsFound.append("]").toString();
    }


    /**
     * Generates all combinations of letters in the pool whose length equals the
     * minLength. If there is at least one word containing that combination,
     * asks the examineNode method to find those words.
     *
     */

    void searchPool(String key, String pool, String rest, int charsToTake) {

        if(rest.length() == charsToTake) {
            key += rest;

            if(dictionary.getNode(key) != null) {
                examineNode(dictionary.getNode(key), pool, 0);
                return;
            }
        }

        for(int i = 0; i < rest.length() && i <= charsToTake; i++) {
            searchPool(key + rest.substring(0,i), pool + rest.charAt(i), rest.substring(i+1), charsToTake-i);
        }
    }

    /**
     * Given a node in the dictionary trie containing a known set of letters, recursively searches for words that can be constructed
     * using the remaining letters in the pool.
     */

    private void examineNode(Node node, String remainingPool, int blanksRequired) {
        if (wordsInPool.size() < 40) {
            for(String anagram : node.anagrams) {
                if(anagram.length() >= minLength + blanksRequired*blankPenalty) {
                    String newWord = doMakeWord(anagram);
                    if(newWord != null) {
                        wordsInPool.add(newWord);
                    }
                }
            }

            for(Map.Entry<Character,Node> child : node.children.entrySet()) {
                if(remainingPool.contains(child.getKey() + "")) {
                    examineNode(child.getValue(), remainingPool.replaceFirst(child.getKey() + "", ""), 0);
                }
                else {
                    blanksRequired++;
                    if(blanksAvailable >= blanksRequired && remainingPool.length() >= blanksRequired*blankPenalty) {
                        examineNode(child.getValue(), remainingPool, blanksRequired);
                    }
                }
            }
        }
    }

    /**
     * Given a word, determines whether the appropriate tiles can be found in the pool. If so,
     * the word is awarded to the player, the tiles are removed from the pool, and the players
     * and watchers are notified.
     *
     * @param 	entry 			The word the player is attempting to make.
     * @return 					Whether the word is taken successfully
     */

    private String doMakeWord(String entry) {

        int blanksAvailable = tilePool.length() - tilePool.replace("?", "").length();
        int blanksRequired = 0;

        //If the tilePool does not contain a letter from the entry, a blank must be used
        String tiles = tilePool;
        for(String s : entry.split("")) {
            if(tiles.contains(s))
                tiles = tiles.replaceFirst(s, "");
            else
                blanksRequired++;
        }

        if(blanksAvailable < blanksRequired)
            return null; //not enough blanks in pool

        if(blanksRequired > 0)
            if(entry.length() - minLength < blanksRequired*(blankPenalty + 1))
                return null; //word not long enough


        tiles = tilePool;
        String newWord = "";
        for(String s : entry.split("")) {
            //move a non-blank tile to the new word
            if(tiles.contains(s)) {
                tiles = tiles.replaceFirst(s, "");
                newWord = newWord.concat(s);
            }
            //move a blank to the new word and designate it
            else {
                tiles = tiles.replaceFirst("\\?", "");
                newWord = newWord.concat(s.toLowerCase());
            }
        }

        return newWord;
    }

    /**
     *
     */

    public String searchForSteals(String[] words) {
        StringBuilder possibleSteals = new StringBuilder();
        int wordsFound = 0;

    //    LinkedHashSet<String[]> possibleSteals = new LinkedHashSet<>();
        if(!tilePool.isEmpty()) {

            outer: for(String shortWord : words) {

                trees.putIfAbsent(shortWord, new WordTree(shortWord.replaceAll("[a-z]", ""), dictionary));
                for (TreeNode child : trees.get(shortWord).root.getChildren()) {

                    String entry = child.toString();

                    if (entry.length() <= shortWord.length() + tilePool.length()) {
                        String longWord = checkSteal(shortWord, entry);
                        if (longWord != null) {

                 //           String toolTip = child.getTooltip();
               //             String[] array = {shortWord, toolTip, longWord};

                            possibleSteals.append(shortWord).append(" + ").append(child.getTooltip()).append(" -> ").append(longWord).append(",");
                            if(++wordsFound >= 30) {
                                break outer;
                            }
                        }
                    }
                }
            }

        }
        return possibleSteals.toString();
    }

    /**
     *
     */

    private String checkSteal(String shortWord, String longWord) {

        String charsToFind = longWord;

        int blanksAvailable = tilePool.length() - tilePool.replace("?", "").length();
        int blanksToChange = 0;
        int blanksToTakeFromPool = 0;

        for(String s : shortWord.split("")) {
            if(charsToFind.contains(s.toUpperCase()))
                charsToFind = charsToFind.replaceFirst(s.toUpperCase(), "");
            else {
                if(Character.isLowerCase(s.charAt(0))) {
                    blanksToChange++;
                    blanksAvailable++;
                }
                else
                    return null;
            }
        }

        String tiles = tilePool;
        for(String s : charsToFind.split("")) {
            if(tiles.contains(s))
                tiles = tiles.replaceFirst(s, "");
            else
                blanksToTakeFromPool++;
        }

        if(blanksAvailable - blanksToChange < blanksToTakeFromPool)
            return null; //not enough blanks in the pool

        int additionalTilesRequired = 1;
        if(blanksToTakeFromPool > 0 || blanksToChange > 0)
            additionalTilesRequired = blankPenalty*blanksToChange + (blankPenalty + 1)*blanksToTakeFromPool;

        if(longWord.length() - shortWord.length() < additionalTilesRequired)
            return null;

        //steal is successful
        tiles = tilePool;
        String oldWord = shortWord;
        String newWord = "";
        for(String s : longWord.split("")) {
            //move a non-blank from the old word to the new word
            if(oldWord.contains(s)) {
                oldWord = oldWord.replaceFirst(s, "");
                newWord = newWord.concat(s);
            }
            //move a blank from the old word to the new word without redesignating it
            else if(oldWord.contains(s.toLowerCase())) {
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