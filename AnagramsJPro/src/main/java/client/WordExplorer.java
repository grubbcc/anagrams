package client;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;
import com.jpro.webapi.WebAPI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 *
 * A utility allowing the user to display WordTrees and analysis tools
 *
 */
class WordExplorer extends PopWindow {

    private final AnagramsClient client;
    private String lexicon;
    private final TextField textField = new TextField();
    private final Button goButton = new Button("Go");
    private final String[] lexicons = {"CSW21", "NWL23"};
    private final ComboBox<String> lexiconSelector = new ComboBox<>(FXCollections.observableArrayList(lexicons));

    private final TextArea messagePane = new TextArea();
    private final HBox controlPanel = new HBox();
    private final SplitPane splitPane = new SplitPane();
    private final StackPane treePanel = new StackPane();
    private final BorderPane messagePanel = new BorderPane();
    private final ScrollPane treeSummaryScrollPane = new ScrollPane();
    private final ContextMenu contextMenu = new ContextMenu();

    TreeItem<TreeNode> root;
    TreeNode rootNode;
    String wordList = "";
    JSONArray data;

    /**
     *
     */
    WordExplorer(String lexicon, AnagramsClient client) {
        super(client.popWindows, client.anchor);
        this.client = client;
        setViewOrder(Double.NEGATIVE_INFINITY);

        AnchorPane.setLeftAnchor(this, 50.0);
        AnchorPane.setTopAnchor(this, 200.0);

        setLexicon(lexicon);

        if(client.getWebAPI().isMobile()) {
            setScaleX(1.35); setScaleY(1.35);
        }

        //Top panel
        textField.setOnAction(e -> {
            goButton.arm();
            goButton.fire();
            PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
            pause.setOnFinished(ef -> goButton.disarm());
            pause.play();
        });

        goButton.setOnAction(e -> {
            String query = textField.getText().replaceAll("[^A-Za-z]","");
            if (query.length() < 4)
                messagePane.setText("You must enter a word of 4 or more letters.");
            else
                lookUp(query);
        });

        lexiconSelector.setPrefSize(75, 20);
        lexiconSelector.setValue(lexicon);
        lexiconSelector.setOnAction(e -> this.lexicon = lexiconSelector.getValue());
        lexiconSelector.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), client.getWebAPI().isMobile());
        controlPanel.setId("control-panel");
        controlPanel.setCursor(Cursor.DEFAULT);
        controlPanel.getChildren().addAll(textField, goButton, lexiconSelector);

        //Tree panel
        treePanel.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        treePanel.setMinHeight(70);
        treePanel.setCursor(Cursor.DEFAULT);
        MenuItem textOption = new MenuItem("Save List to File");
        textOption.setOnAction(e-> saveListToFile());
        MenuItem imageOption = new MenuItem("View List as Image");
        imageOption.setOnAction(e -> viewListAsImage());
        contextMenu.getItems().addAll(textOption, imageOption);
        contextMenu.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), client.getWebAPI().isMobile());

        //Prevent treeView from capturing drag events
        treePanel.addEventFilter(MouseEvent.ANY, event -> {
            if(event.getTarget() instanceof Text || event.getTarget() instanceof CustomTreeCell) {
                Event.fireEvent(splitPane, event);
            }
        });

        //Message panel
        messagePanel.setCenter(messagePane);
        messagePanel.setCursor(Cursor.DEFAULT);
        messagePanel.setId("message-area");
        messagePanel.setStyle("-fx-background-color: rgb(20,250,20)");
        messagePane.setStyle("-fx-background-color: rgb(20,250,20);" + "-fx-text-fill: black");
        treeSummaryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagePane.setEditable(false);
        messagePane.setWrapText(true);

        messagePane.addEventFilter(MouseEvent.ANY, event -> {
            if(!(event.getTarget() instanceof Text || event.getTarget() instanceof ScrollBar || event.getTarget() instanceof StackPane)) {
                Event.fireEvent(splitPane, event);
            }
        });
        KeyCombination copyKey = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
        messagePane.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            if (copyKey.match(keyEvent)) {
                client.getWebAPI().executeScript("navigator.clipboard.writeText('%s')".formatted(messagePane.getSelectedText()));
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

        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().addAll(mainPanel, messagePanel);
        splitPane.setDividerPosition(0, 0.75);

        getStylesheets().add("css/explorer.css");

        setContents(splitPane);

        setVisible(false);
        setAsDragZone(controlPanel, splitPane);
        makeResizable();
        setPrefSize(345, 415);
    }

    /**
     *
     */
    void setLexicon(String lexicon) {
        this.lexicon = lexicon;
        lexiconSelector.getSelectionModel().select(lexicon);
    }

    /**
     * Instructs the server to retrieve data for a new word
     *
     * @param query The contents of the textField (stripped of any non-alphabetic characters)
     */
    void lookUp(String query) {
        textField.clear();
        treePanel.getChildren().clear();
        messagePane.clear();
        client.send("lookup", new JSONObject()
                .put("lexicon", lexicon)
                .put("query", query.replaceAll("[^a-zA-z]",""))
        );

    }

    /**
     * Parses the JSON data from the server, constructs the TreeView, and displays it
     *
     * @param data A list of nodes containing words, definitions, probabilities, and
     *             instructions on how to place them in the hierarchy.
     */
    void setUpTree(JSONArray data) {
        this.data = data;

        rootNode = new TreeNode(data.getJSONObject(0));
        rootNode.setProb(1);

        TreeMap<Integer, Integer> counts = new TreeMap<>();

        for(int i = 1; i < data.length(); i++) {
            TreeNode child = new TreeNode(data.getJSONObject(i));
            child.getAddress().removeFirst();
            counts.merge(child.getWord().replaceFirst("[#$]", "").length(), 1, Integer::sum);
            addNode(rootNode, child, child.getAddress());
        }

        root = new TreeItem<>(rootNode);
        root.setExpanded(true);
        addChildren(root);

        TreeView<TreeNode> treeView = new TreeView<>(root);
        treePanel.getChildren().add(treeView);
        treeView.setContextMenu(contextMenu);
        treeView.setCellFactory(tv -> new CustomTreeCell());

        messagePane.setText(rootNode.getDefinition());

        if(!rootNode.getChildren().isEmpty()) {
            treeSummaryScrollPane.setContent(treeSummary(counts));
            treeSummaryScrollPane.addEventFilter(MouseEvent.ANY, event -> {
                if(event.getY() > treeSummaryScrollPane.getContent().getBoundsInLocal().getHeight()) {
                    Event.fireEvent(splitPane, event);
                }
            });
            messagePanel.setRight(treeSummaryScrollPane);
        }
        else
            messagePanel.setRight(null);
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
            parent.addChild(child.getWord(), child);
            child.setParent(parent);
        }
        else {
            parent = parent.getChild(address.removeFirst());
            addNode(parent, child, address);
        }
    }

    /**
     * Recursively builds the TreeView according to the TreeNode hierarchy.
     *
     * @param parentItem The TreeItem whose children will be added next.
     */
    private void addChildren(TreeItem<TreeNode> parentItem) {
        for(TreeNode child : parentItem.getValue().getChildren()) {
            TreeItem<TreeNode> childItem = new TreeItem<>(child);
            parentItem.getChildren().add(childItem);
            addChildren(childItem);
        }
    }

    /**
     * A TreeView cell that can be selected to show a definition, double-clicked to perform a lookup,
     * hovered over to display the steal, and whose background is colored to indicate probability.
     */
    private class CustomTreeCell extends TreeCell<TreeNode> {

        /**
         * On double-click looks up the selected node and triggers the goButton
         */
        private CustomTreeCell () {
            setPickOnBounds(false);

            setOnMouseClicked(e -> {
                if (!isEmpty() && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    goButton.arm();
                    PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                    pause.setOnFinished(ef -> goButton.disarm());
                    pause.play();
                    lookUp(getText());
                }
            });
        }

        /**
         * Sets the cell's tooltip and background color
         *
         * @param item The Java object to be rendered as a TreeCell
         * @param empty Whether the cell is part of the background
         */
        @Override
        public void updateItem(TreeNode item, boolean empty) {
            super.updateItem(item, empty);

            if(!empty) {
                if(client.prefs.getBoolean("highlight_words"))
                    setText(item.getWord());
                else
                    setText(item.getWord().replaceFirst("[#$]", ""));
                if(item.getParent() != null ) {
                    Tooltip tooltip = new Tooltip(item.longSteal + "   " + item.getProb() + "%");
                    setTooltip(tooltip);
                }
                if(isSelected()) {
                    messagePane.setText(getItem().getDefinition());
                }

                setStyle("-cell-background: hsb(0, " + item.getProb() + "%, 100%);");

            }
            else {
                setText(null);
                setStyle("-cell-background: white;");
            }
        }
    }

    /**
     * Creates a table showing the number of steals of the rootWord organized by word length
     *
     * @param counts The raw data as computed in the setUpTree method
     */
    private VBox treeSummary(TreeMap<Integer, Integer> counts) {

        VBox summaryPane = new VBox();

        if(!counts.isEmpty()) {
            GridPane treeSummary = new GridPane();
            treeSummary.getColumnConstraints().add(new ColumnConstraints(45));
            treeSummary.getColumnConstraints().add(new ColumnConstraints(55));
            treeSummary.addRow(0, new Label(" length"), new Label(" words"));

            for (Integer key : counts.keySet()) {
                treeSummary.addRow(key, new Label(" " + key), new Label(" " + counts.get(key)));
            }
            treeSummary.setGridLinesVisible(true);
            treeSummary.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
            summaryPane.getChildren().add(treeSummary);
        }

        summaryPane.addEventFilter(MouseEvent.ANY, event -> {
            setCursor(Cursor.DEFAULT);
            if(event.getTarget() instanceof GridPane || event.getTarget() instanceof Line || event.getTarget() instanceof Text) {
                Event.fireEvent(splitPane, event);
            }
        });
        return summaryPane;
    }

    /**
     * Recursively performs a depth-first search of the word tree, saving the
     * words visited to a String.
     *
     * @param prefix Indentations indicating the depth of the node
     * @param node The node currently being visited
     */
    private void generateWordList(String prefix, TreeNode node) {
        for(TreeNode child : node.getChildren()) {
            wordList = wordList.concat(prefix + child.getWord() + "\\n");
            generateWordList(prefix + "  ", child);
        }
    }

    /**
     * Executes some JavaScript to create the file, insert the word list, and open the download window
     */
    private void saveListToFile () {

        generateWordList("", rootNode);

        client.getWebAPI().executeScript("""
            var pom = document.createElement('a');
            pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent('%s'));
            pom.setAttribute('download', '%s.txt');
            if (document.createEvent) {
                var event = document.createEvent('MouseEvents');
                event.initEvent('click', true, true);
                pom.dispatchEvent(event);
            }
            else {
                pom.click();
            }
            """.formatted(wordList, rootNode.getWord())
        );

    }

    /**
     * Sends the tree JSON data to the webpage and opens it.
     */
    private void viewListAsImage() {
        WebAPI.getWebAPI(getScene()).executeScript("localStorage.setItem('lexicon', '" + lexicon + "')");
        WebAPI.getWebAPI(getScene()).executeScript("localStorage.setItem('json', '" + data.toString().replaceAll("'", "\\\\'") + "');" );
        WebAPI.getWebAPI(getScene()).openURLAsTab("/tree.html");
    }

}