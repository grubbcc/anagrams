package client;

import com.jpro.webapi.HTMLView;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 *
 */


public class MessageDialog extends PopWindow {


    /**
     *
     */

    private final HTMLView htmlView = new HTMLView();
    private final HBox buttonPane = new HBox();

    Button yesButton = new Button("Yes");
    Button noButton = new Button("No");

    /**
     *
     */

    public MessageDialog(AnagramsClient client, String title) {
        super(client.stack);
        htmlView.setPrefSize(USE_PREF_SIZE, USE_PREF_SIZE);

        if(client.getWebAPI().isMobile()) {
            setScaleX(1.2);
            setScaleY(1.2);
        }
        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(htmlView);
        mainPane.setBottom(buttonPane);
        buttonPane.setAlignment(Pos.BASELINE_CENTER);
        buttonPane.setSpacing(5);
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
        yesButton.setPrefWidth(50);
        noButton.setPrefWidth(50);
        buttonPane.getChildren().addAll(yesButton, noButton);
    }
}

