package client;

import java.util.Collection;
import java.util.LinkedHashMap;

public class TreeNode {

    private final String word;
    final String shortTip;
    final String longTip;
    private double prob;
    private TreeNode parent;
    private final LinkedHashMap<String, TreeNode> children = new LinkedHashMap<>();

    /**
     *
     */

    public TreeNode(String word, String shortTooltip, String longTooltip) {
        this.word = word;
        this.shortTip = shortTooltip;
        this.longTip = longTooltip;
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
     * Places this TreeNode into a tree hierarchy as a child of the the supplied TreeNode
     */

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    /**
     * @return The TreeNode of which this one is a child. Returns null if this is the root node.
     */

    public TreeNode getParent() {
        return parent;
    }

    /**
     *
     * @param key The letters which, when added to this word, form the alphagram of newChild's word
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
