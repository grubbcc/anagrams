/***
* Stores a word and toolTip (the letters required for a steal) for use in the WordTree.
*/

class UserObject {
	
	private String word;
	private String toolTip;
	private double prob;
	
	/**
	*
	*/
	
	public UserObject(String word, String toolTip) {
		this.word = word.toUpperCase();
		this.toolTip = toolTip;
	}

	/**
	*
	*/
	
	public UserObject(String word, String toolTip, double prob) {
		this.word = word.toUpperCase();
		this.toolTip = toolTip;
		this.prob = prob;
	}
	
	/**
	*
	*/
	
	public String toString() {
		return word;
	}
	
	/**
	*
	*/
	
	public String getToolTip() {
		return toolTip;
	}
	
	/**
	*
	*/
	
	public double getProb() {
		return prob;
	}
	
	/**
	*
	*/
	
	public void setProb(double prob) {
		this.prob = prob;
	}
}
