import java.lang.Math;

public class ProbCalc {

	static int[] tileFrequencies = {27, 6, 6, 12, 36, 6, 9, 6, 27, 3, 3, 12, 6, 18, 24, 6, 3, 18, 12, 18, 12, 6, 6, 3, 6, 3};
	
	public static double getProbability(String word) {
		int currChar;
		int prevChar = '&';
		int numer = 1;
		int count = 0;
		int repet = 1;
		int denom = 294;
		double probability = 1.0;
		for(int i = 0; i < word.length(); i++) {
			currChar = word.charAt(i) - 'A';
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
	
	public static double getLogProbability(String word) {
		return -1*Math.log10(getProbability(word));
	}
	
	public static void main(String[] args) {
		for(String word : args) {
			System.out.println(getProbability(word));
		}
	}
}
