package server;

import java.util.Arrays;
import java.util.List;

/**
 * A collection of static methods for working with Strings
 */
class Utils {

    /**
     * Given two words, determines whether one's letters are a strict subset of the other's.
     *
     * @param shortWord a shorter word
     * @param longWord a longer word
     */
    static boolean isSubset(String shortWord, String longWord) {

        if(shortWord.length() >= longWord.length()) {
            return false;
        }

        String shortString = Utils.alphabetize(shortWord);
        String longString = Utils.alphabetize(longWord);

        while(shortString.length() > 0) {

            if(longString.length() == 0 ) {
                return false;
            }
            else if(shortString.charAt(0) < longString.charAt(0)) {
                return false;
            }
            else if(shortString.charAt(0) > longString.charAt(0)) {
                longString = longString.substring(1);
            }
            else {
                shortString = shortString.substring(1);
                longString = longString.substring(1);
            }
        }
        return true;
    }

    /**
     * Given two words, one longer than the other, determines whether one's letters can be rearranged,
     * with the addition of at least one letter not found in the shorter word, to form the longer one.
     *
     * @param shortWord a shorter word
     * @param longWord a longer word
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


    /**
     * Given a String, creates an "alphagram" consisting of its letters arranged in alphabetical order.
     *
     * @param entry: the letters to be rearranged
     */
    static String alphabetize(String entry) {
        char[] chars = entry.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    /**
     * Check whether the List contains any String with exactly the same letters in the same order
     * but possibly of different case.
     */
    static boolean containsCaseInsensitive(List<String> l, String s){
        return l.stream().anyMatch(x -> x.equalsIgnoreCase(s));
    }

}
