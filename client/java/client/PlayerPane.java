package client;

import com.sandec.mdfx.MarkdownView;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 */

class PlayerPane extends PopWindow {

    private final String sampleMarkdown =
        "### Heading\n" +
        "\n" +
        "*italic*\n" +
        "\n" +
        "**bold**\n" +
        "\n" +
        "***bold and italic***\n" +
        "\n" +
        " [click here](https://google.com)\n" +
        "\n" +
        "![](https://www.seattlephysicstutor.com/thinker.jpg)\n" +
        "\n" +
        "[text-color](GREEN)\n" +
        "[link-color](BLUE)\n" +
        "[background-color](YELLOW)\n" +
        "\n" +
        "```\n" +
        "block quote\n" +
        "```\n" +
        "\n" +
        "the  | old | man\n" +
        " ---  | ---   | ---\n" +
        "and  | the  | sea\n" +
        "\n" +
        "* Bacon\n" +
        "* Lettuce\n" +
        "* Tomato";
    private final TextArea codePane = new TextArea(sampleMarkdown);
    private final AnagramsClient client;
    private final ScrollPane bioScrollPane = new ScrollPane();
    private final BorderPane contents = new BorderPane();
    private final PopWindow infoPane;
    private String textColor = "BLACK";
    private String linkColor = "BLUE";
    private String backgroundColor = "#DDD";
    private final HashSet<Node> nodesToRemove = new HashSet<>();
    private final MarkdownView mdfx = new MarkdownView() {
        @Override
        public void setLink(Node node, String link, String description) {
            switch (description) {
                case "text-color" -> {
                    textColor = link;
                    nodesToRemove.add(node);
                }
                case "link-color" -> {
                    linkColor = link;
                    nodesToRemove.add(node);
                }
                case "background-color" -> {
                    backgroundColor = link;
                    nodesToRemove.add(node);
                }
                default -> {
                    node.setCursor(Cursor.HAND);
                    node.setOnMouseClicked(e -> client.getWebAPI().openURLAsTab(link.trim()));
                }
            }
        }
    };

    /**
     *
     */

    PlayerPane(AnagramsClient client) {
        super(client.anchor);

        this.client = client;

        contents.getStyleClass().clear();
        contents.setStyle(null);
        contents.getStylesheets().add(getClass().getResource("/mdfx-default.css").toExternalForm());
        bioScrollPane.setFitToHeight(true);
        bioScrollPane.setFitToWidth(true);
        bioScrollPane.setContent(mdfx);
        bioScrollPane.addEventFilter(KeyEvent.ANY, Event::consume);

        contents.setCenter(bioScrollPane);

        mdfx.addEventFilter(MouseEvent.ANY, event -> Event.fireEvent(this, event));

        setContents(contents);
        makeResizable();

        infoPane = new PopWindow(client.anchor);
        infoPane.setTitle("Markdown Guide");
        codePane.setPrefWidth(300);
        ImageView markdownPane = new ImageView("/images/markdown.png");
        markdownPane.setFitHeight(550);
        markdownPane.setPreserveRatio(true);
        markdownPane.setSmooth(true);
        markdownPane.setMouseTransparent(true);
        infoPane.setContents(new HBox(codePane, markdownPane));
    }


    /**
     *
     */

    @Override
    public void hide() {
        super.hide();
        infoPane.hide();
    }
    /**
     *
     * @param playerName The player whose data is to be displayed
     */

    void displayPlayerInfo(String playerName) {
        if(client.getWebAPI().isMobile()) {
            setScaleX(1.35); setScaleY(1.35);
        }
        setTitle(playerName);
        textColor = "BLACK"; linkColor = "BLUE"; backgroundColor = "#DDD";
        mdfx.setMdString("");
        mdfx.setMdString(client.prefs.parent().node(playerName).get("bio", playerName));
        setColors();
        bioScrollPane.setContent(mdfx);
        contents.setBottom(null);
        if(playerName.equals(client.username) && !client.guest) {
            addButtonPanel();
        }
    }

    /**
     *
     */

    private void setColors() {

        //Remove non-display nodes
        for(Node node : nodesToRemove) {
            Parent parent = node.getParent();
            Parent grandparent = parent.getParent();
            if(grandparent instanceof Pane) {
                ((Pane)grandparent).getChildren().remove(parent);
            }
        }

        ArrayList<Node> nodesToColor = new ArrayList<>();
        mdfx.setStyle("-fx-background-color: " + backgroundColor);
        addAllDescendents(mdfx, nodesToColor);
        for(Node node : nodesToColor) {
            if (node instanceof Text) {
                if(node.getStyleClass().toString().contains("link")) {
                    node.setStyle("-fx-fill: " + linkColor);
                }
                else {
                    node.setStyle("-fx-fill: " + textColor);
                }
            }
        }
    }

    /**
     *
     */

    private static void addAllDescendents(Parent parent, ArrayList<Node> nodes) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            nodes.add(node);
            if (node instanceof Parent)
                addAllDescendents((Parent)node, nodes);
        }
    }


    /**
     * Turns this pane into an editable pane
     */

    void addButtonPanel() {
        HBox buttonPanel = new HBox();
        buttonPanel.setSpacing(10);
        buttonPanel.setAlignment(Pos.CENTER);

        TextArea editorPane = new TextArea();
        editorPane.setWrapText(true);
        editorPane.getProperties().put("vkType", "text");

        editorPane.lengthProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() > oldValue.intValue()) {
                if (editorPane.getText().length() >= 4000) {
                    editorPane.setText(editorPane.getText().substring(0, 4000));
                }
            }
        });

        Button editBioButton = new Button("Change bio");
        Button confirmButton = new Button("Confirm changes");
        Button cancelButton = new Button("Cancel");
        Button deleteAccountButton = new Button("Delete account");

        editBioButton.setPrefWidth(110);
        confirmButton.setPrefWidth(130);
        cancelButton.setPrefWidth(90);
        deleteAccountButton.setPrefWidth(110);

        ImageView infoIcon = new ImageView("/images/info.png");
        infoIcon.setFitWidth(17);
        infoIcon.setFitHeight(17);
        Tooltip tooltip = new Tooltip("Show markdown guide");
        tooltip.setShowDelay(Duration.seconds(0.5));
        Tooltip.install(infoIcon, tooltip);

        editBioButton.setOnAction(e -> {
            editorPane.setText(mdfx.getMdString());
            bioScrollPane.setContent(editorPane);
            buttonPanel.getChildren().clear();
            buttonPanel.getChildren().addAll(confirmButton, cancelButton, infoIcon);
            Platform.runLater(editorPane::requestFocus);
        });
        confirmButton.setOnAction(e -> {
            client.prefs.put("bio", editorPane.getText());
            textColor = "BLACK"; linkColor = "BLUE"; backgroundColor = "#DDD";
            mdfx.setMdString("");
            mdfx.setMdString(editorPane.getText());
            setColors();
            bioScrollPane.setContent(mdfx);
            buttonPanel.getChildren().clear();
            buttonPanel.getChildren().addAll(editBioButton, deleteAccountButton);

        });
        cancelButton.setOnAction(e -> {
            bioScrollPane.setContent(mdfx);
            buttonPanel.getChildren().clear();
            buttonPanel.getChildren().addAll(editBioButton, deleteAccountButton);
        });

        deleteAccountButton.setOnAction(e -> {
            MessageDialog confirmDialog = new MessageDialog(client, "Delete account");
            confirmDialog.setText("Are you sure you want to delete your account?");
            confirmDialog.addYesNoButtons();
            confirmDialog.yesButton.setOnAction(click -> {
                client.guest = true;
                confirmDialog.hide();
                MessageDialog infoDialog = new MessageDialog(client, "Account deleted");
                infoDialog.setText("Your account has been deleted. You will remain\n" +
                        "logged in as a guest until you close the application.");
                infoDialog.addOkayButton();
                Platform.runLater(() -> infoDialog.show(true));
            });
            confirmDialog.noButton.setOnAction(click -> confirmDialog.hide());
            Platform.runLater(() -> confirmDialog.show(true));
        });
        infoIcon.setOnMouseClicked(e -> {
            codePane.setText(sampleMarkdown);
            infoPane.show(false);
        });

        contents.setBottom(buttonPanel);
        buttonPanel.getChildren().addAll(editBioButton, deleteAccountButton);
    }
}
