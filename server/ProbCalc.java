package server;

import java.lang.Math;

/**
 * Tool for calculating the likelihood of possible steals
 */

public class ProbCalc {

    private final static int[] tileFrequencies = {27, 6, 6, 12, 36, 6, 9, 6, 27, 3, 3, 12, 6, 18, 24, 6, 3, 18, 12, 18, 12, 6, 6, 3, 6, 3};

    /**
     * Calculates the chance of drawing and random the given set of tiles from a bag containing three
     * standard English-language Scrabble sets
     *
     * @param tiles A set of letters which must be uppercase
     * @return      The probability that these tiles will be drawn expressed as a decimal
     */

    static double getProbability(String tiles) {
        int currChar;
        int prevChar = '&';
        int numer = 1;
        int count = 0;
        int repet = 1;
        int denom = 294;
        double probability = 1.0;
        for(int i = 0; i < tiles.length(); i++) {
            currChar = tiles.charAt(i) - 'A';
            count++;
            if(currChar != prevChar) {
                numer = tileFrequencies[currChar];
                repet = 1;
            }
            else {
                numer--;
                repet++;
            }

            //System.out.println(numer+"*"+count+"/"+denom+"/"+repet);
            probability *= (double)numer*count/denom--/repet;
            prevChar = currChar;
        }
        return probability;

    }

    /**
     *
     */

    static double getLogProbability(String word) {
        return -1*Math.log10(getProbability(word));
    }

    /**
     *
     */

    static double round (double value, double precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    /**
     *
     */

    public static void main(String[] args) {
        for(String word : args) {
            System.out.println(getProbability(word.toUpperCase()));
        }
    }
}