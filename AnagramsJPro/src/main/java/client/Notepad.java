package client;

import javafx.scene.control.TextArea;
import javafx.scene.layout.*;

public class Notepad extends PopWindow {

    /**
     * @param container The Pane in which this window resides.
     */
    Notepad(Pane container) {
        super(container);

        setMinWidth(300);
        setMinHeight(400);
        makeResizable();
        AnchorPane.setLeftAnchor(this, container.getWidth() - 400);
        AnchorPane.setTopAnchor(this, 180.0);
        setTitle("Notepad");
        TextArea textArea = new TextArea();
        textArea.setPrefSize(300, 150);
        getStylesheets().add("css/notepad.css");
        setContents(textArea);

    }

}
