package client;

import com.jpro.webapi.HTMLView;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * A simple JPro-compatible window with HTML content and a button bar.
 */


class MessageDialog extends PopWindow {

    private final BorderPane mainPane = new BorderPane();
    private final HTMLView htmlView = new HTMLView();
    final HBox buttonPane = new HBox();

    final Button okayButton = new Button("Okay");
    final Button yesButton = new Button("Yes");
    final Button noButton = new Button("No");
    final Button backButton = new Button("Back");
    final Button nextButton = new Button("Next");

    /**
     *
     */

    MessageDialog(AnagramsClient client, String title) {
        super(client.stack);

        if(client.getWebAPI().isMobile()) {
            setScaleX(1.2);
            setScaleY(1.2);
        }

        mainPane.getStyleClass().add("dialog");
        mainPane.setCenter(htmlView);
        mainPane.setBottom(buttonPane);
        buttonPane.setAlignment(Pos.BASELINE_CENTER);
        buttonPane.setSpacing(8);
        setContents(mainPane);
        setMaxSize(410, 120);
        setTitle(title);
        setAsDragZone(mainPane, buttonPane);
    }

    /**
     * Sets the content of the htmlView.
     *
     * @param text some html for the dialog to display
     */

    void setText(String text) {
        htmlView.setContent(text);
    }

    /**
     *
     */

    void setImage(String url) {
        if(url != null) {
            ImageView img = new ImageView(new Image(url));
            img.setPreserveRatio(true);
            img.setFitWidth(200);
            mainPane.setRight(img);
        }
        else {
            mainPane.setRight(null);
        }
    }

    /**
     * Adds a button which hides the dialog
     */

    void addOkayButton() {
        buttonPane.getChildren().add(okayButton);
        okayButton.setOnAction(e -> hide());
    }

    /**
     * Adds two buttons, labeled "Yes" and "No", with configurable actions
     */

    void addYesNoButtons() {
        buttonPane.getChildren().addAll(yesButton, noButton);
    }

    /**
     *
     */

    void addBackNextButtons() {
        buttonPane.getChildren().addAll(backButton, nextButton);
    }
}

