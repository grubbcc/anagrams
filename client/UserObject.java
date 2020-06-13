/***
* Stores a word and tooltip (the letters required for a steal) for use in the WordTree.
*/

class UserObject {
	
	private String word;
	private String toolTip;
	
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
	
	public String toString() {
		return word;
	}
	
	/**
	*
	*/
	
	public String getToolTip() {
		return toolTip;
	}
	
}
