package server;

import java.util.LinkedList;

/**
 *
 */

public class TreeNode {

    private final String word;
    private final String longSteal;
    private String shortSteal;
    private double prob = 0;
    private final LinkedList<TreeNode> children = new LinkedList<>();

    /**
     *
     */

    public TreeNode(String word, String steal) {
        this.word = word.toUpperCase();
        this.longSteal = steal;
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

    public String getLongSteal() {
        return longSteal;
    }

    /**
     *
     */

    public void setShortSteal(String shortSteal) {
        this.shortSteal = shortSteal;
    }

    /**
     *
     */

    public String getShortSteal() {
        return shortSteal;
    }


    /**
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

    /**
     *
     */

    public void setProb(double prob) {
        this.prob = prob;
    }

    /**
     *
     */

    public double getProb() {
        return prob;
    }
}
