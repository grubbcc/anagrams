package client;

import org.json.JSONObject;

import java.util.*;

class TreeNode {

    private final JSONObject data;
    private final LinkedList<String> address;
    private final String word;
    final String shortSteal;
    final String longSteal;
    private double prob;
    private final String definition;
    private TreeNode parent;
    private final LinkedHashMap<String, TreeNode> children = new LinkedHashMap<>();

    TreeNode(JSONObject data) {
        this.data = data;
        address = new LinkedList<>(Arrays.asList(data.getString("id").split("\\.")));
        word = address.removeLast();
        shortSteal = data.getString("shortsteal");
        longSteal = data.getString("longsteal");
        prob = data.getDouble("prob");
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

    String getWord() {
        return word;
    }

    /**
     *
     */

    LinkedList<String> getAddress() {
        return address;
    }

    /**
     *
     */

    String getDefinition() {
        return definition;
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

    /**
     * Places this TreeNode into a tree hierarchy as a child of the supplied TreeNode
     */

    void setParent(TreeNode parent) {
        this.parent = parent;
    }

    /**
     * @return The TreeNode of which this one is a child. Returns null if this is the root node.
     */

    TreeNode getParent() {
        return parent;
    }

    /**
     *
     * @param key The letters which, when added to this word, form the alphagram of newChild's word
     * @param newChild another TreeNode whose word is a steal of this node according to the rules of Anagrams.
     */

    void addChild(String key, TreeNode newChild) {
        children.put(key, newChild);
    }

    /**
     *
     */

    TreeNode getChild(String key) {
        return children.get(key);
    }

    /**
     *
     */

    Collection<TreeNode> getChildren() {
        return children.values();
    }
}
