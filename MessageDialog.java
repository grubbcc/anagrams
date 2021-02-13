package client;

import com.jpro.webapi.HTMLView;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class MessageDialog extends PopWindow {


    /**
     *
     */

    private final HTMLView htmlView = new HTMLView();
    private final HBox buttonPane = new HBox();

    /**
     *
     */

    public MessageDialog(AnagramsClient client, String title) {
        super(client.stack);
        htmlView.setPrefSize(500, 100);

        VBox mainPane = new VBox(htmlView, buttonPane);
        setContents(mainPane);
        setMaxSize(400, 120);
        setTitle(title);
    }

    /**
     *
     * @param text some html for the dialog to display
     */

    public void setText(String text) {
        htmlView.setContent(text);
    }


    /**
     *
     */

    public void addOkayButton() {
        Button okayButton = new Button("Okay");
        okayButton.setPrefWidth(50);
        buttonPane.getChildren().add(okayButton);
        okayButton.setOnAction(e -> hide());
    }

    /**
     *
     */

    public void addYesNoButtons() {
        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");
        yesButton.setPrefWidth(50);
        noButton.setPrefWidth(50);
        buttonPane.getChildren().addAll(yesButton, noButton);
        yesButton.setOnAction(e -> {});
        noButton.setOnAction(e -> {});
    }
}

