package client;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;

/**
 *  A JPro-compatible alternative to the standard JavaFX Window with draggable functionality and title bar.
 */

public class PopWindow extends BorderPane {

    ObjectProperty<Point2D> mouseLocation = new SimpleObjectProperty<>();
    boolean resizable = false;
    private final Pane container;
    final HBox titleBar = new HBox();
    final Label title = new Label();
    final Button closeButton = new Button();

    public PopWindow(Pane container) {

        this.container = container;

        //close button
        Line line1 = new Line(6, 7, 14, 15);
        Line line2 = new Line(6, 15, 14, 7);
        Group closeIcon = new Group(line1, line2);
        closeButton.setGraphic(closeIcon);
        closeButton.setCancelButton(true);
        closeButton.setOnAction(e -> hide());

        //title bar
        titleBar.setId("title-bar");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        titleBar.getChildren().addAll(title, spacer, closeButton);
        setTop(titleBar);

        makeMovable();
        setResizable(true);
        setVisible(false);
        setId("popup");
        getStylesheets().add(getClass().getResource("/anagrams.css").toExternalForm());

    }

    /**
     *
     */

    public void setTitle(String title) {
        this.title.setText(title);
    }

    /**
     *
     */

    public void setContents(Region contents) {
        setCenter(contents);
    }

    /**
     *
     */
    
    public void show(boolean modal) {
        if(!isVisible()) {
            for (Node child : container.getChildren()) {
                if (modal) {
                    child.setDisable(true);
                }
            }
            container.getChildren().add(this);
            setVisible(true);
            this.setDisable(false);
        }

    }

    /**
     *
     */

    void bringToFront() {
         for(Node child : container.getChildren()) {
             if(!child.equals(this)) {
                 if (child instanceof GameWindow) {
                     child.setViewOrder(1);
                 }
             }
             setViewOrder(0);
         }
    }
    /**
     * 
     */

    public void hide() {
        setVisible(false);
        container.getChildren().remove(this);
        for(Node child : container.getChildren()) {
            child.setDisable(false);
        }
    }

    /**
     *
     */

    public void setResizable(boolean resizable) {
        this.resizable = resizable;
        DragResizer.makeResizable(this);
    }


    /**
     * Maybe need to change this to getBoundsInParent or getLayoutX something so that the GameWindows doesn't move also.
     */

    public void makeMovable() {
        setOnMousePressed(event ->
            mouseLocation.set(new Point2D(event.getScreenX(), event.getScreenY()))
        );

        setOnMouseDragged(event -> {
            event.consume();

            if (mouseLocation.get() != null) {
                double x = event.getScreenX();
                double deltaX = x - mouseLocation.get().getX();
                double y = event.getScreenY();
                double deltaY = y - mouseLocation.get().getY();

                setTranslateX(getTranslateX() + deltaX);
                setTranslateY(getTranslateY() + deltaY);

                mouseLocation.set(new Point2D(x, y));
            }
        });

        setOnMouseReleased(event -> mouseLocation.set(null));
    }

}
