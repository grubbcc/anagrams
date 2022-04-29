package client;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;

/**
 *  A JPro-compatible alternative to the standard JavaFX Window with
 *  draggable, maximizable, and resizable functionality and title bar.
 *  By default, the window is draggable, but not maximizable or resizable.
 */

class PopWindow extends BorderPane {

    private final ObjectProperty<Point2D> mouseLocation = new SimpleObjectProperty<>();
    private final Pane container;
    final HBox titleBar = new HBox();
    final Label title = new Label();
    final MaximizeButton maximizeButton = new MaximizeButton();
    private boolean isMaximized;
    final Button closeButton = new Button();

    /**
     *
     * @param container The Pane in which this window resides.
     */

    PopWindow(Pane container) {

        this.container = container;

        //close button
        Line line1 = new Line(6, 7, 14, 15);
        Line line2 = new Line(6, 15, 14, 7);
        Group closeIcon = new Group(line1, line2);
        closeButton.setGraphic(closeIcon);
        closeButton.setCancelButton(true);
        closeButton.setOnAction(e -> hide());

        //minimize button
        //final Button minimizeButton = new Button();
//        Rectangle minimizeIcon = new Rectangle(11,1);
//        minimizeButton.setGraphic(minimizeIcon);
//        minimizeButton.getGraphic().setTranslateY(5);


        //title bar
        titleBar.setId("title-bar");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        titleBar.getChildren().addAll(title, spacer, closeButton);
        setTop(titleBar);

        setMovable(true);
        setVisible(false);
        setId("popup");
        getStylesheets().add(getClass().getResource("/anagrams.css").toExternalForm());
    }

    /**
     *
     */

    class MaximizeButton extends Button {

        Polyline maximizeIcon = new Polyline(2, 2, 14, 2, 14, 14, 2, 14, 2, 2);
        Polyline unmaximizeIcon = new Polyline(10, 9, 12, 9, 12, 1, 4, 1, 4, 4, 10, 4, 10, 12, 2, 12, 2, 4, 4, 4);

        Point2D savedCoords;
        Point2D savedSize;

        /**
         *
         */

        MaximizeButton() {
            setGraphic(maximizeIcon);
//          setOnAction(maximizeAction);
        }

        /**
         *
         */

        void toggle() {
            if(isMaximized) {
                maximizeButton.setGraphic(maximizeButton.maximizeIcon);
                isMaximized = false;
            }
            else {
                maximizeButton.setGraphic(maximizeButton.unmaximizeIcon);
                isMaximized = true;
            }
        }

        /**
         *
         */

        EventHandler<ActionEvent> maximizeAction = event -> {
            if (isMaximized) {
                PopWindow.this.setMinWidth(savedSize.getX());
                PopWindow.this.setMinHeight(savedSize.getY());
                PopWindow.this.setTranslateX(savedCoords.getX());
                PopWindow.this.setTranslateY(savedCoords.getY());

                isMaximized = false;
                setGraphic(maximizeIcon);
            }
            else {
                isMaximized = true;
                savedCoords = new Point2D(PopWindow.this.getTranslateX(), PopWindow.this.getTranslateY());
                savedSize = new Point2D(PopWindow.this.getWidth(), PopWindow.this.getHeight());
                PopWindow.this.setTranslateX(0);
                PopWindow.this.setTranslateY(0);
                if (getScene().getWidth() > PopWindow.this.getMinWidth()) {
                    PopWindow.this.setMinWidth(getScene().getWidth());
                }
                if (getScene().getHeight() > PopWindow.this.getMinHeight()) {
                    PopWindow.this.setMinHeight(getScene().getHeight());
                }
                setGraphic(unmaximizeIcon);
            }
        };

    }

    /**
     *
     */

    void setTitle(String title) {
        this.title.setText(title);
    }

    /**
     *
     */

    void setContents(Region contents) {
        setCenter(contents);
    }

    /**
     *
     */
    
    void show(boolean modal) {
        if(!isVisible()) {
            if(modal) {
                for (Node child : container.getChildren()) {
                    child.setDisable(true);
                }
            }
            container.getChildren().add(this);
            setVisible(true);
            setDisable(false);
        }
    }

    /**
     *
     */

    void hide() {
        setVisible(false);
        container.getChildren().remove(this);
        for(Node child : container.getChildren()) {
            child.setDisable(false);
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

    void makeMaximizable() {
        titleBar.getChildren().remove(closeButton);
        titleBar.getChildren().addAll(maximizeButton, closeButton);
    }

    /**
     *
     */

    void makeResizable() {
        DragResizer.makeResizable(this);
    }

    /**
     * If set to true, allows user to reposition this window by dragging inside its area.
     *
     * @param movable whether this window can be dragged using the mouse. true by default.
     */

    void setMovable(boolean movable) {

        if (movable) {
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

        else {
            setOnMousePressed(null);
            setOnMouseDragged(null);
            setOnMouseReleased(null);
        }
    }

}
