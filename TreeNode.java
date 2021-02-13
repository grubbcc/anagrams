package client;

import java.util.LinkedList;

public class TreeNode {

    private final String word;
    private final String tooltip;
    private double prob;
    private final LinkedList<TreeNode> children = new LinkedList<>();

    /**
     *
     */

    public TreeNode(String word, String tooltip) {
        this.word = word.toUpperCase();
        this.tooltip = tooltip;
    }

    /**
     *
     */

    @Override
    public String toString() {
        return word;
    }

    /**
     *
     */

    public String getTooltip() {
        return tooltip;
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

    /**
     *
     * @param newChild another TreeNode whose word is a steal of this node according to the rules of Anagrams.
     */

    public void addChild(TreeNode newChild) {
        children.add(0, newChild);
    }

    /**
     *
     */

    public LinkedList<TreeNode> getChildren() {
        return children;
    }
}
