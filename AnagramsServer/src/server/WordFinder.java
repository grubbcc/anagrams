package server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 *
 */
class WordFinder {

    private final int blankPenalty;
    private final int minLength;
    private final AlphagramTrie dictionary;

    private String tilePool;
    private int blanksAvailable = 0;

    private final HashSet<String> wordsInPool = new HashSet<>();
    private final JSONArray possibleSteals = new JSONArray();
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
     */
    synchronized JSONObject findWords(JSONObject gameState) {

        wordsInPool.clear();
        possibleSteals.clear();

        String tiles = gameState.getString("tiles").replace("#", "");
        tilePool = tiles.length() <= 20 ? tiles : tiles.substring(0, 20);
        blanksAvailable = tiles.length() - tiles.replaceAll("\\?","").length();

        if(tilePool.length() >= minLength)
            findInPool(dictionary.rootNode, "", Utils.alphabetize(tilePool.replaceAll("\\?","")), 0);

        JSONObject foundWords = new JSONObject().put("pool", new JSONArray(wordsInPool));

        JSONArray players = gameState.getJSONArray("players");
        players.forEach(player -> searchForSteals(((JSONObject)player).getJSONArray("words")));

        return foundWords.put("steals", possibleSteals);
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
                node.words.forEach(word -> wordsInPool.add(word.letters + word.suffix));
            }
            else {
                for(Word anagram : node.words) {
                    String newWord = "";
                    String tiles = tilePool;
                    for (String s : anagram.letters.split("")) {
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
                    wordsInPool.add(newWord + anagram.suffix);
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
     */
    private synchronized void searchForSteals(JSONArray words) {

        Iterator<Object> it = words.iterator();
        while(it.hasNext() && possibleSteals.length() < 30) {
            String shortWord = (String)it.next();

            trees.computeIfAbsent(shortWord, word -> new WordTree(word.replaceAll("[a-z]", ""), dictionary));
            ArrayDeque<TreeNode> wordQueue = new ArrayDeque<>(trees.get(shortWord).rootNode.getChildren());

            while(!wordQueue.isEmpty()) {
                TreeNode child = wordQueue.pollFirst();
                String entry = child.getWord().letters;
                if (entry.length() <= shortWord.length()) {
                    wordQueue.addAll(child.getChildren());
                    continue;
                }
                else if (entry.length() > shortWord.length() + tilePool.length()) {
                    continue;
                }
                Play play = new Play(shortWord, entry, tilePool, minLength, blankPenalty);
                if (play.isValid()) {
                    possibleSteals.put(new JSONObject()
                            .put("shortWord", shortWord)
                            .put("steal", child.getLongSteal())
                            .put("longWord", dictionary.annotate(play.nextWord())));
                }
            }
        }

    }

}