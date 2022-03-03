package server;

/**
 * An attempt to form a word from the pool or to steal a preexisting word
 */

class Play {

    private final String shortWord;
    private final String longWord;

    private final int minLength;
    private final int blankPenalty;

    String nextTiles;
    private String blanks = "";

    /**
     *
     * @param shortWord
     * @param longWord
     * @param tilePool
     * @param minLength
     * @param blankPenalty
     */

    Play(String shortWord, String longWord, String tilePool, int minLength, int blankPenalty) {
        this.shortWord = shortWord;
        this.longWord = longWord;
        this.nextTiles = tilePool;
        this.minLength = minLength;
        this.blankPenalty = blankPenalty;
    }

    /**
     * Checks whether the play can be formed from the pool and preexisting words and
     * whether it long enough, accounting for the number of blanks required.
     *
     * Note: this method does not check if steal is a rearrangement of a shortWord;
     * that is assumed.
     *
     * @return whether the play is valid according to the rules of Anagrams
     */

    boolean isValid() {

        String entry = longWord;
        int blanksToChange = 0;
        int penalty = 0;

        //search for characters in the word to be stolen
        for (String s : shortWord.split("")) {
            if (entry.contains(s)) {
                //Transfer a tile from the shortWord to the longWord
                entry = entry.replaceFirst(s, "");
            }
            else if (Character.isLowerCase(s.charAt(0))) {
                if (!entry.contains(s.toUpperCase())) {
                    //Redesignate a blank and transfer it to the longWord
                    blanksToChange++;
                    penalty += blankPenalty;
                }
                else {
                    //Transfer the blank without redesignating
                    entry = entry.replaceFirst(s.toUpperCase(), "");
                    blanks += s.toUpperCase();
                }
            }
            else {
                //The shortWord contains a letter not found in the longWord
                return false;
            }
        }

        //Search for the remaining tiles in the entry
        int charsToFind = entry.length();
        for (String s : entry.split("")) {
            if (charsToFind > blanksToChange) {
                charsToFind--;
                //Add a tile from the pool
                if (nextTiles.contains(s)) {
                    nextTiles = nextTiles.replaceFirst(s, "");
                }
                else if (nextTiles.contains("?")) {
                    //Take a tile from the pool and designate it
                    nextTiles = nextTiles.replaceFirst("\\?", "");
                    blanks += s;
                    penalty += blankPenalty + 1;
                }
                else {
                    //Not enough blanks in the pool
                    return false;
                }
            }
            else if (blanksToChange > 0) {
                nextTiles = nextTiles.replaceFirst(s, "");
                blanks += s;
                charsToFind--;
                blanksToChange--;
            }
            else {
                //The entry contains a letter found neither in the shortWord nor the pool
                return false;
            }
        }

        //Check if word is long enough, accounting for the blank penalty
        if(shortWord.isEmpty())
            return longWord.length() - minLength >= penalty;
        else
            return longWord.length() - shortWord.length() >= Math.max(penalty, 1);

    }

    /**
     *
     * @return a formatted word with lowercase letters representing blanks
     */

    String nextWord() {
        String nextWord = longWord;
        for(String s : blanks.split(""))
            nextWord = nextWord.replaceFirst(s, s.toLowerCase());
        return nextWord;
    }

}
