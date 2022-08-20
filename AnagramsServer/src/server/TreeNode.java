package server;

import java.util.LinkedList;

/**
 *
 */
class TreeNode {

    private final String word;
    private String suffix;
    private final String longSteal;
    private String shortSteal;
    private double prob = 0;
    private final LinkedList<TreeNode> children = new LinkedList<>();

    /**
     *
     */
    TreeNode(String word, String suffix, String steal) {
        this.word = word.toUpperCase();
        this.suffix = suffix;
        this.longSteal = steal;
    }

    /**
     *
     */
    public String getWord() {
        return word;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return word + suffix;
    }

    /**
     *
     */
    void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     *
     */
    String getLongSteal() {
        return longSteal;
    }

    /**
     *
     */
    void setShortSteal(String shortSteal) {
        this.shortSteal = shortSteal;
    }

    /**
     *
     */
    String getShortSteal() {
        return shortSteal;
    }


    /**
     * @param newChild another TreeNode whose word is a steal of this node according to the rules of Anagrams.
     */
    void addChild(TreeNode newChild) {
        children.add(0, newChild);
    }

    /**
     *
     */
    LinkedList<TreeNode> getChildren() {
        return children;
    }

    /**
     *
     */
    void setProb(double prob) {
        this.prob = prob;
    }

    /**
     *
     */
    double getProb() {
        return prob;
    }
}
