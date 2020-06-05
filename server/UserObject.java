class UserObject {
	
	private String word;
	private String tooltip;
	
	public UserObject(String word, String tooltip) {
		this.word = word.toUpperCase();
		this.tooltip = tooltip;
	}
	
	public String toString() {
		return word;
	}
	
	public String getTooltip() {
		return tooltip;
	}
	
}
