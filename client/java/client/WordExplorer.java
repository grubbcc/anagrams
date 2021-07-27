package client;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import com.jpro.webapi.WebAPI;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * A utility allowing the user to display WordTrees and analysis tools
 *
 */

public class WordExplorer extends PopWindow {

    private final AnagramsClient client;
    private String lexicon;
    private final TextField textField = new TextField();
    private final Button goButton = new Button("Go");
    private final String[] lexicons = {"CSW19", "NWL20"};
    private final ComboBox<String> lexiconSelector = new ComboBox<>(FXCollections.observableArrayList(lexicons));

    private final TextArea messagePane = new TextArea();
    private final HBox controlPanel = new HBox();
    private final StackPane treePanel = new StackPane();
    private final BorderPane messagePanel = new BorderPane();
    private final ScrollPane treeSummaryScrollPane = new ScrollPane();
    private final ContextMenu contextMenu = new ContextMenu();

    private final TreeMap<Integer, Integer> counts = new TreeMap<>();
    TreeItem<TreeNode> root;
    TreeNode rootNode;
    String wordList = "";
    String json = "";

    /**
     *
     */

    public WordExplorer(String lexicon, AnagramsClient client) {
        super(client.anchor);
        this.client = client;
        setViewOrder(Double.NEGATIVE_INFINITY);

        AnchorPane.setLeftAnchor(this, 50.0);
        AnchorPane.setTopAnchor(this, 200.0);

        setLexicon(lexicon);
        if(client.getWebAPI().isMobile()) getTransforms().add(new Scale(1.35, 1.35));

        //Top panel
        textField.setOnAction(e -> {
            goButton.arm();
            goButton.fire();
            PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
            pause.setOnFinished(ef -> goButton.disarm());
            pause.play();
        });
        textField.setPrefSize(170, 20);

        goButton.setPrefSize(55, 20);
        goButton.setOnAction(e -> {
            String query = textField.getText().replaceAll("[^A-Za-z]","");
            if (query.length() < 4)
                messagePane.setText("You must enter a word of 4 or more letters.");
            else
                lookUp(query);
        });

        lexiconSelector.setPrefSize(75, 20);
        lexiconSelector.setValue(lexicon);
        lexiconSelector.setOnAction(e -> {
            this.lexicon = lexiconSelector.getValue();
        });
        controlPanel.setId("control-panel");
        controlPanel.getChildren().addAll(textField, goButton, lexiconSelector);

        //Tree panel
        treePanel.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        treePanel.setMinHeight(70);
        MenuItem textOption = new MenuItem("Save List to File");
        textOption.setOnAction(e-> saveListToFile());
        MenuItem imageOption = new MenuItem("View List as Image");
        imageOption.setOnAction(e -> viewListAsImage());
        contextMenu.getItems().addAll(textOption, imageOption);

        //Prevent treeView from capturing drag events
        treePanel.addEventFilter(MouseEvent.ANY, event -> {
            if(event.getTarget() instanceof CustomTreeCell) {
                Event.fireEvent(this, event);
            }
        });

        //Message panel
        messagePanel.setCenter(messagePane);
        messagePanel.setId("message-area");
        treeSummaryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        treeSummaryScrollPane.setPrefWidth(125);
        messagePanel.setPrefHeight(110);
        messagePanel.setStyle("-fx-background-color: rgb(20,250,20)");
        messagePane.setStyle("-fx-background-color: rgb(20,250,20);" + "-fx-text-fill: black");
        messagePane.setEditable(false);
        messagePane.setWrapText(true);

        //Prevent messagePane from capturing drag events
        messagePanel.addEventFilter(MouseEvent.ANY, event -> {
            if(event.getTarget() instanceof GridPane) {
                Event.fireEvent(this, event);
                event.consume();
            }
        });

        //Window
        BorderPane mainPanel = new BorderPane();
        mainPanel.setTop(controlPanel);

        if(client.getWebAPI().isMobile()) {
            MenuButton saveListButton = new MenuButton("Save List");
            saveListButton.getItems().addAll(textOption, imageOption);
            saveListButton.setPopupSide(Side.TOP);
            AnchorPane anchor = new AnchorPane(treePanel, saveListButton);
            mainPanel.setCenter(anchor);

            anchor.setMinSize(Double.MIN_VALUE, Double.MIN_VALUE);
            anchor.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            AnchorPane.setTopAnchor(treePanel, 0.0);
            AnchorPane.setRightAnchor(treePanel, 0.0);
            AnchorPane.setBottomAnchor(treePanel, 0.0);
            AnchorPane.setLeftAnchor(treePanel, 0.0);

            AnchorPane.setRightAnchor(saveListButton, 15.0);
            AnchorPane.setBottomAnchor(saveListButton, 15.0);
        }
        else {
            mainPanel.setCenter(treePanel);
        }
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(mainPanel, messagePanel);
        splitPane.setDividerPosition(0, 0.75);
        String explorerStyle = getClass().getResource("/explorer.css").toExternalForm();
        getStylesheets().add(explorerStyle);
        setStyle(explorerStyle);
        setContents(splitPane);

        setVisible(false);
        makeResizable();
        setPrefSize(345, 415);
    }

    /**
     *
     */

    public void setLexicon(String lexicon) {
        this.lexicon = lexicon;
        lexiconSelector.getSelectionModel().select(lexicon);
    }

    /**
     *
     */

    public void lookUp(String query) {
        textField.clear();
        treePanel.getChildren().clear();
        messagePane.clear();
        client.send("lookup " + lexicon + " " + query.toUpperCase());
        client.send("def " + lexicon + " " + query.toUpperCase());
    }

    /**
     *
     */

    public void setUpTree(String json) {
        this.json = json;

        String[] nodes = json.split("},");

        Matcher m = Pattern.compile("\"id\": \"([A-z]+)").matcher(nodes[0]);
        m.find();
        String rootWord = m.group(1);
        rootNode = new TreeNode(rootWord, "", "");
        rootNode.setProb(1);

        for(int i = 1; i < nodes.length; i++) {
            String[] matches = Pattern.compile("[.A-Z]+")
                    .matcher(nodes[i])
                    .results()
                    .map(MatchResult::group)
                    .toArray(String[]::new);
            LinkedList<String> id = new LinkedList<>();

            Collections.addAll(id, matches[0].split("\\."));
            id.removeFirst();
            String shortTip = matches[1];
            String longTip = matches[2];

            TreeNode child = new TreeNode(id.removeLast(), shortTip, longTip);
            addNode(rootNode, child, id);
        }

        root = new TreeItem<>(rootNode);
        root.setExpanded(true);
        addChildren(root);

        TreeView<TreeNode> treeView = new TreeView<>(root);
        treePanel.getChildren().add(treeView);
        treeView.setContextMenu(contextMenu);
        treeView.setCellFactory(tv -> new CustomTreeCell());

        if(!rootNode.getChildren().isEmpty()) {
            treeSummaryScrollPane.setContent(treeSummary());
            messagePanel.setRight(treeSummaryScrollPane);
        }
        else
            messagePanel.setRight(null);
    }

    /**
     *
     */

    public void showDefinition(String definition) {
        messagePane.setText(definition);
    }

    /**
     * Recursively adds a TreeNode to a hierarchy according to its address
     *
     * @param parent A node of which the child is a descendant
     * @param child The node being added to the tree
     * @param address A list of Strings representing where in tree's hierarchy where the node should be added. If the
     *                address is empty, the node will be placed immediately; otherwise it will be added as a descendant
     *                of the first node referenced in the list.
     */

    private void addNode(TreeNode parent, TreeNode child, LinkedList<String>address) {
        if(address.isEmpty()) {
            parent.addChild(child.toString(), child);
            child.setParent(parent);
        }
        else {
            parent = parent.getChild(address.removeFirst());
            addNode(parent, child, address);
        }
    }

    /**
     * Recursively builds the TreeView according to to the hierarchy of TreeNodes.
     * @param parentItem The TreeItem whose children will be added next.
     */

    public void addChildren(TreeItem<TreeNode> parentItem) {
        for(TreeNode child : parentItem.getValue().getChildren()) {
            TreeItem<TreeNode> childItem = new TreeItem<>(child);
            parentItem.getChildren().add(childItem);
            addChildren(childItem);
        }
    }

    /**
     *
     */

    private class CustomTreeCell extends TreeCell<TreeNode> {

        /**
         *
         */

        private CustomTreeCell () {
            setPickOnBounds(false);
            setOnMouseClicked(e -> {
                if (!isEmpty()) {
                    if(e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                        goButton.arm();
                        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                        pause.setOnFinished(ef -> goButton.disarm());
                        pause.play();
                        lookUp(getText());
                    }
                    else {
                        client.send("def " + lexicon + " " + getText());
                    }
                }
            });
        }

        /**
         *
         */

        @Override
        public void updateItem(TreeNode item, boolean empty) {
            super.updateItem(item, empty);

            if(!empty) {

                setText(item.toString());
                doProbabilities(item);
                if(item.getParent() != null ) {
                    Tooltip tooltip = new Tooltip(item.longTip + "   " + round(100 * item.getProb(), 1) + "%");
                    tooltip.setShowDelay(Duration.seconds(0.5));
                    setTooltip(tooltip);
                }

                setStyle("-cell-background: hsb(0, " + round(100*item.getProb(), 0) + "%, 100%);");

            }
            else {
                setText(null);
                setStyle("-cell-background: white;");
            }
        }
    }

    /**
     * Recursively visits all nodes in the wordTree and stores their lengths in a HashMap.
     */

    private void countNodes(TreeNode node) {
        for(TreeNode child: node.getChildren()) {
            counts.computeIfPresent(child.toString().length(), (key, val) -> val + 1);
            counts.putIfAbsent(child.toString().length(), 1);
            countNodes(child);
        }
    }

    /**
     * Creates a JTable showing the number of steals of the rootWord organized by word length
     */

    public VBox treeSummary() {

        VBox summaryPane = new VBox();

        counts.clear();
        countNodes(rootNode);
        if(!counts.isEmpty()) {
            GridPane treeSummary = new GridPane();
            treeSummary.getColumnConstraints().add(new ColumnConstraints(45));
            treeSummary.getColumnConstraints().add(new ColumnConstraints(74));
            treeSummary.addRow(0, new Label(" length"), new Label(" num words  "));

            for (Integer key : counts.keySet()) {
                treeSummary.addRow(key, new Label(" " + key), new Label(" " + counts.get(key)));
            }
            treeSummary.setGridLinesVisible(true);
            treeSummary.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
            summaryPane.getChildren().add(treeSummary);

        }
        return summaryPane;
    }

    /**
     * Assigns probabilities to possible steals assuming a full bag of three tile sets.
     *
     * @param parent the TreeNode containing the word whose steals are to be calculated
     */

    public void doProbabilities(TreeNode parent) {
        double norm = 0;

        for(TreeNode child : parent.getChildren()) {
            norm += ProbCalc.getProbability(child.shortTip);
        }
        for(TreeNode child : parent.getChildren()) {
            child.setProb(parent.getProb()*ProbCalc.getProbability(child.shortTip)/norm);
        }
    }

    /**
     * Recursively performs a depth-first search of the word tree, saving the
     * words visited to a String.
     *
     * @param prefix Indentations indicating the depth of the node
     * @param node The node currently being visited
     */

    public void generateWordList(String prefix, TreeNode node) {
        for(TreeNode child : node.getChildren()) {
            wordList = wordList.concat(prefix + child.toString() + "\\n");
            generateWordList(prefix + "  ", child);
        }
    }

    /**
     *
     */

    private void saveListToFile() {

        generateWordList("", rootNode);

        client.getWebAPI().executeScript(
          "var pom = document.createElement('a'); " +
            "pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent('" + wordList + "')); " +
            "pom.setAttribute('download', '" + rootNode.toString() + ".txt'); " +
            "if (document.createEvent) { " +
                "var event = document.createEvent('MouseEvents'); " +
                "event.initEvent('click', true, true); " +
                "pom.dispatchEvent(event); " +
            "}" +
            "else { " +
                "pom.click(); " +
            "}"
        );
    }

    /**
     *
     */

    private void viewListAsImage() {
        WebAPI.getWebAPI(getScene()).executeScript("localStorage.setItem('JSON', '[" + json.replaceAll(",$","") + "]');");
        WebAPI.getWebAPI(getScene()).openURLAsTab("/flare.html");
    }

    /**
     *
     */

    private static double round (double value, double precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

}