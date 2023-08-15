package client;

import javafx.event.Event;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.Stack;

/**
 * A widget for taking notes during a game.
 */
class Notepad extends PopWindow {

    /**
     * @param container The Pane in which this window resides.
     */
    Notepad(Stack<PopWindow> popWindows, Pane container) {
        super(popWindows, container);

        setMinWidth(300);
        setMinHeight(400);
        makeResizable();
        setAsDragZone(this);
        AnchorPane.setLeftAnchor(this, container.getWidth() - 400);
        AnchorPane.setTopAnchor(this, 180.0);
        setTitle("Notepad");

        TextArea textArea = new TextArea();
        textArea.setPrefSize(300, 150);
        textArea.addEventFilter(MouseEvent.ANY, event -> {
            if(!(event.getTarget() instanceof Text || event.getTarget() instanceof ScrollBar || event.getTarget() instanceof StackPane)) {
                Event.fireEvent(this, event);
            }
        });
        setContents(textArea);
        getStylesheets().add("css/notepad.css");

    }

}
