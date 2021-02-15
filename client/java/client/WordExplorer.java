package client;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import com.jpro.webapi.WebAPI;
import java.util.Objects;
import java.util.TreeMap;

/**
 *
 * A utility allowing the user to display WordTrees and analysis tools
 *
 */

public class WordExplorer extends PopWindow {

    private String lexicon;
    private final TextField textField = new TextField();
    private final Button goButton = new Button("Go");
    private final String[] lexicons = {"CSW19", "NWL20"};
    private final ComboBox<String> lexiconSelector = new ComboBox<>(FXCollections.observableArrayList(lexicons));

    private final TreeMap<String, AlphagramTrie> tries = new TreeMap<>();
    private WordTree tree;
    private final TextArea messagePane = new TextArea();
    private final HBox controlPanel = new HBox();
    private final StackPane treePanel = new StackPane();
    private final BorderPane messagePanel = new BorderPane();
    private final ScrollPane treeSummaryScrollPane = new ScrollPane();
    private final ContextMenu contextMenu = new ContextMenu();

    /**
     *
     */

    public WordExplorer(AlphagramTrie trie, AnchorPane anchor) {
        super(anchor);
        setViewOrder(Double.NEGATIVE_INFINITY);

        AnchorPane.setLeftAnchor(this, 50.0);
        AnchorPane.setTopAnchor(this, 200.0);

        this.lexicon = trie.lexicon;
        tries.put(lexicon, trie);

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
            if (textField.getText().replaceAll("\\s"," ").length() < 4)
                messagePane.setText("You must enter a word of 4 or more letters.");
            else
                lookUp(textField.getText());
        });

        lexiconSelector.setPrefSize(75, 20);
        lexiconSelector.setValue(lexicon);
        lexiconSelector.setOnAction(e -> {
            lexicon = lexiconSelector.getValue();
            if(!tries.containsKey(lexicon)) {
                WordTree newTree = new WordTree(lexicon);
                tries.put(lexicon, newTree.trie);
            }
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

        //Message panel
        messagePanel.setCenter(messagePane);
        messagePanel.setId("message-area");
        treeSummaryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        treeSummaryScrollPane.setPrefWidth(125);
        messagePanel.setPrefHeight(110);
        messagePanel.setStyle("-fx-background-color: rgb(20,250,20)");
        messagePane.setStyle("-fx-background-color: rgb(20,250,20);" +
                "-fx-text-fill: black");
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
        mainPanel.setCenter(treePanel);
        mainPanel.setBottom(messagePanel);
        String explorerStyle = getClass().getResource("/explorer.css").toExternalForm();
        getStylesheets().add(explorerStyle);
        setStyle(explorerStyle);
        setContents(mainPanel);
        setVisible(false);
        setResizable(true);
        setPrefSize(345, 415);

    }

    /**
     *
     */

    public void lookUp(String query) {
        textField.clear();
        treePanel.getChildren().clear();
        tree = new WordTree(query.toUpperCase(), tries.get(lexicon));
        TreeItem<TreeNode> root = new TreeItem<>(tree.root);
        TreeView<TreeNode> treeView = new TreeView<>(root);
        setUpTree(root);
        treePanel.getChildren().add(treeView);
        root.setExpanded(true);

        treeView.setContextMenu(contextMenu);
        treeView.setCellFactory(tv -> new CustomTreeCell());

        //Prevent treeView from capturing drag events
        treePanel.addEventFilter(MouseEvent.ANY, event -> {
            if(event.getTarget() instanceof CustomTreeCell) {
                Event.fireEvent(this, event);
            }
        });

        String definition = tree.trie.getDefinition(query);
        messagePane.setText(Objects.requireNonNullElse(definition, "Definition not available"));

        if(!tree.root.getChildren().isEmpty()) {
            treeSummaryScrollPane.setContent(tree.treeSummary());
            messagePanel.setRight(treeSummaryScrollPane);
        }
        else
            messagePanel.setRight(null);
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
                if(e.getButton() == MouseButton.PRIMARY) {
                    if (e.getClickCount() == 2 && !isEmpty()) {
                        goButton.arm();
                        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                        pause.setOnFinished(ef -> goButton.disarm());
                        pause.play();
                        lookUp(getText());
                    }
                    else if (!isEmpty()) {
                        String definition = tree.trie.getDefinition(getText());
                        messagePane.setText(Objects.requireNonNullElse(definition, "Definition not available"));
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
                Tooltip tooltip = new Tooltip(item.getTooltip() + "   " + round(100*item.getProb(), 1) + "%");
                tooltip.setShowDelay(Duration.seconds(0.5));
                setTooltip(tooltip);

                setStyle("-cell-background: hsb(0, " + round(100*item.getProb(), 0) + "%, 100%);");
                if(item.equals(tree.root)) {
                    setText(tree.rootWord);
                    setTooltip(null);
                }
            }
            else {
                setText(null);
                setTooltip(null);
                setStyle("-cell-background: white;");
            }
        }
    }

    /**
     *
     */

    public void setUpTree(TreeItem<TreeNode> parentItem) {
        for(TreeNode child : parentItem.getValue().getChildren()) {
            TreeItem<TreeNode> childItem = new TreeItem<>(child);
            parentItem.getChildren().add(childItem);
            setUpTree(childItem);
        }
    }

    /**
     * Assigns probabilities to possible steals assuming a full bag of three tile sets.
     *
     * @param parent the TreeNode containing the word whose steals are to be calculated
     */

    public void doProbabilities(TreeNode parent) {
        double norm = 0;

        double parentProb;
        if(parent.equals(tree.root)) {
            parentProb = 1.0;
        }
        else {
            parentProb = parent.getProb();
        }

        for(TreeNode child : parent.getChildren()) {
            norm += ProbCalc.getProbability(child.getTooltip());
        }
        for(TreeNode child : parent.getChildren()) {
            child.setProb(parentProb*ProbCalc.getProbability(child.getTooltip())/norm);
        }
    }

    /**
     *
     */

    private void saveListToFile() {

        tree.generateWordList("", tree.root);

        WebAPI.getWebAPI(getScene()).executeScript(
       "var pom = document.createElement('a'); " +
            "pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent('" + tree.wordList + "')); " +
            "pom.setAttribute('download', '" + tree.rootWord + ".txt'); " +
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
        tree.generateCSVList(tree.rootWord, "", tree.root);
        WebAPI.getWebAPI(getScene()).executeScript("localStorage.setItem('CSV', '[" + tree.CSV.replaceAll(",$","") + "]');");
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