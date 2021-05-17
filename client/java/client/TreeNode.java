package client;

import java.util.Collection;
import java.util.LinkedHashMap;

public class TreeNode {

    private final String word;
    private final String tooltip;
    private double prob;
    private final LinkedHashMap<String, TreeNode> children = new LinkedHashMap<>();

    /**
     *
     */

    public TreeNode(String word, String tooltip) {
        this.word = word;
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
     * @param key
     * @param newChild another TreeNode whose word is a steal of this node according to the rules of Anagrams.
     */

    public void addChild(String key, TreeNode newChild) {
        children.put(key, newChild);
    }

    /**
     *
     */

    public TreeNode getChild(String key) {
        return children.get(key);
    }

    /**
     *
     */

    public Collection<TreeNode> getChildren() {
        return children.values();
    }
}
