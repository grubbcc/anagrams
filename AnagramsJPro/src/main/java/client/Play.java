package client;

/**
 * An attempt to form a word, either by building from the pool or by stealing an existing word
 */
class Play {

    static int minLength;
    static int blankPenalty;
    final String shortWord;
    final String longWord;
    final String tilePool;

    private String blanks = "";

    /**
     *
     */
    Play(String shortWord, String longWord, String tiles) {
        this.shortWord = shortWord;
        this.longWord = longWord;
        this.tilePool = tiles;
    }

    /**
     * @return a measure of how "playable" this play is. All valid plays will have a score >= 0.
     */
    int getScore() {

        String entry = longWord;
        String tiles = this.tilePool;

        String blanksToRetain = "";
        int unstolen = 0; //letters in the shortWord but not in the longWord
        int blanksToChange = 0;
        int missingFromPool = 0; //letters in the longWord that are not found in the shortWord or the pool

        //Search for characters in the word to be stolen
        for (String s : shortWord.split("")) {
            if (entry.contains(s)) {
                //Transfer a tile from the shortWord to the longWord
                entry = entry.replaceFirst(s, "");
            }
            else if (Character.isLowerCase(s.charAt(0))) {
                if (entry.contains(s.toUpperCase())) {
                    //Transfer the blank without re-designating
                    blanksToRetain += s.toUpperCase();
                    entry = entry.replaceFirst(s.toUpperCase(), "");
                }
                else {
                    //Mark a blank for re-designation
                    blanksToChange++;
                }
            }
            else {
                //The shortWord contains a letter not found in the longWord
                unstolen++;
            }
        }

        //Search pool for missing tiles
        for (String s : entry.split("")) {
            if(entry.length() > blanksToChange) {
                if (tiles.contains(s)) {
                    //Add a regular tile to the word
                    tiles = tiles.replaceFirst(s, "");
                    entry = entry.replaceFirst(s, "");
                }
                else if(!blanksToRetain.isEmpty()) {
                    for (String t : blanksToRetain.split("")) {
                        //Mark a retained blank for re-designation
                        if (tiles.contains(t)) {
                            blanksToRetain = blanksToRetain.replaceFirst(t, "");
                            tiles = tiles.replaceFirst(t, "");
                            blanksToChange++;
                            break;
                        }
                    }
                }
            }
        }

        //Designate blanks as missing letters
        int penalty = 0;

        for (int i = 0; i < entry.length(); i++) {
            if (blanksToChange-- > 0) {
                //Re-designate a blank
                penalty += blankPenalty;
            } else if (tiles.contains("?")) {
                //Take a blank from the pool and designate it
                tiles = tiles.replaceFirst("\\?", "");
                penalty += blankPenalty + 1;
            } else {
                //Not enough blanks available
                missingFromPool++;
            }
        }

        if (shortWord.isEmpty())
            return longWord.length() - minLength - penalty - 2*missingFromPool - 2*unstolen;

        else
            return longWord.length() - shortWord.length() - Math.max(penalty, 1) - 2*missingFromPool - 2*unstolen;
    }

    /**
     * Designate letters as blanks or as normal tiles
     */
    char[] buildWord() {
        String tiles = tilePool + shortWord.toUpperCase();
        String newWord = "";

        for (String s : longWord.split("")) {
            if (tiles.contains(s)) {
                tiles = tiles.replaceFirst(s, "");
                newWord = newWord.concat(s);
            }
            else {
                newWord = newWord.concat(s.toLowerCase());
            }
        }
        return newWord.toCharArray();
    }

    /**
     *
     */
    boolean isValid() {

        String entry = longWord;
        String blanksToKeep = "";
        String tiles = tilePool;
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
                if (tiles.contains(s)) {
                    //Add a regular tile to the word
                    tiles = tiles.replaceFirst(s, "");
                    entry = entry.replaceFirst(s, "");
                }
                else if(!blanksToKeep.isEmpty()) {
                    for (String t : blanksToKeep.split("")) {
                        //Mark a retained blank for re-designation
                        if (tiles.contains(t)) {
                            blanksToKeep = blanksToKeep.replaceFirst(t, "");
                            tiles = tiles.replaceFirst(t, "");
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
                } else if (tiles.contains("?")) {
                    //Take a blank from the pool and designate it
                    tiles = tiles.replaceFirst("\\?", "");
                    penalty += blankPenalty + 1;
                } else {
                    //Not enough blanks available
                    return false;
                }
            }
        }

        blanks += blanksToKeep;

        if(shortWord.isEmpty()) {
            return longWord.length() - minLength >= penalty;
        }
        else if(longWord.length() - shortWord.length() >= Math.max(penalty, 1))  {
            return isRearrangement(shortWord.replaceAll("[a-z]", "?"), nextWord().replaceAll("[a-z]","?"));
        }
        return false;
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

    /**
     * Given two words, determines whether a rearrangement/permutation
     * of letters is necessary to form the longer.
     *
     * @param shortWord     a short word (case must match longWord)
     * @param longWord      a longer word (case must match that of shortWord)
     */
    static boolean isRearrangement(String shortWord, String longWord) {

        while(longWord.length() >= shortWord.length() && shortWord.length() > 0) {
            if (shortWord.charAt(0) == longWord.charAt(0)) {
                shortWord = shortWord.substring(1);
            }
            longWord = longWord.substring(1);
        }

        return shortWord.length() > longWord.length();
    }

}