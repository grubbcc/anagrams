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
     * Note: this method does not check if steal is a rearrangement of a shortWord or
     * whether it is in the dictionary; that is assumed.
     *
     * @return whether the play is valid according to the rules of Anagrams
     */


     boolean isValid() {

         String entry = longWord;
         String blanksToKeep = "";
         int blanksToChange = 0;

        //Search for characters in the word to be stolen
        for (String s : shortWord.split("")) {
            if (entry.contains(s)) {
                //Transfer a tile from the shortWord to the longWord
                entry = entry.replaceFirst(s, "");
            }
            else if (Character.isLowerCase(s.charAt(0))) {
                if (entry.contains(s.toUpperCase())) {
                    //Transfer the blank without re-designating
                    blanksToKeep += s.toUpperCase();
                    entry = entry.replaceFirst(s.toUpperCase(), "");
                }
                else {
                    //Mark a blank for re-designation
                    blanksToChange++;
                }
            }
            else {
                //The shortWord contains a letter not found in the longWord
                return false;
            }
        }

        //Search pool for missing tiles
        for (String s : entry.split("")) {
            if(entry.length() > blanksToChange) {
                if (nextTiles.contains(s)) {
                    //Add a regular tile to the word
                    nextTiles = nextTiles.replaceFirst(s, "");
                    entry = entry.replaceFirst(s, "");
                }
                else if(!blanksToKeep.isEmpty()) {
                    for (String t : blanksToKeep.split("")) {
                        //Mark a retained blank for re-designation
                        if (nextTiles.contains(t)) {
                            blanksToKeep = blanksToKeep.replaceFirst(t, "");
                            nextTiles = nextTiles.replaceFirst(t, "");
                            blanksToChange++;
                            break;
                        }
                    }
                }
            }
        }

        //Designate blanks to missing letters
        int penalty = 0;
        if(!entry.isEmpty()) {
            for (String s : entry.split("")) {
                blanks += s;
                if (blanksToChange-- > 0) {
                    //Re-designate a blank
                    penalty += blankPenalty;
                } else if (nextTiles.contains("?")) {
                    //Take a blank from the pool and designate it
                    nextTiles = nextTiles.replaceFirst("\\?", "");
                    penalty += blankPenalty + 1;
                } else {
                    //Not enough blanks available
                    return false;
                }
            }
        }

        //Check if word is long enough, accounting for the blank penalty
        if(shortWord.isEmpty())
            return longWord.length() - minLength >= penalty;
        else
            return longWord.length() - shortWord.length() >= Math.max(penalty, 1);

    }

    /**
     * @return a formatted word with lowercase letters representing blanks
     */

    String nextWord() {
        String nextWord = longWord;
        for(String s : blanks.split(""))
            nextWord = nextWord.replaceFirst(s + "(?!.*?" + s + ")", s.toLowerCase()); //replace last occurrence
        return nextWord;
    }

}
