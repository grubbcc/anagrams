package client;

import org.json.JSONObject;

import java.util.*;

public class TreeNode {

    private final JSONObject data;
    private final LinkedList<String> address;
    private final String word;
    final String shortTip;
    final String longTip;
    private double prob;
    private final String definition;
    private TreeNode parent;
    private final LinkedHashMap<String, TreeNode> children = new LinkedHashMap<>();

    public TreeNode(JSONObject data) {
        this.data = data;
        address = new LinkedList<>(Arrays.asList(data.getString("id").split("\\.")));
        word = address.removeLast();
        shortTip = data.getString("shorttip");
        longTip = data.getString("longtip");
        definition = data.optString("def", "Definition not available");
    }

    /**
     *
     */

    @Override
    public String toString() {
        return data.toString();
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

    public LinkedList<String> getAddress() {
        return address;
    }
    /**
     *
     */

    public String getDefinition() {
        return definition;
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
