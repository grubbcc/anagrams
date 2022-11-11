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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;

import java.util.Stack;

/**
 *  A JPro-compatible alternative to the standard JavaFX Window with
 *  draggable, maximizable, and resizable functionality and title bar.
 *  By default, the window is draggable, but not maximizable or resizable.
 */
class PopWindow extends BorderPane {

    private final ObjectProperty<Point2D> mouseLocation = new SimpleObjectProperty<>();
    private final ObjectProperty<Point2D> newLocation = new SimpleObjectProperty<>();
    private final Pane container;
    final HBox titleBar = new HBox();
    final Label title = new Label();
    final MaximizeButton maximizeButton = new MaximizeButton();
    boolean isMaximized;
    final Button closeButton = new Button();

    final static Stack<PopWindow> popWindows = new Stack<>();
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
        closeButton.setOnMouseClicked(e -> hide());

        //title bar
        titleBar.getStyleClass().add("title-bar");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        titleBar.getChildren().addAll(title, spacer, closeButton);
        setTop(titleBar);
        setVisible(false);
        setAsDragZone(titleBar);
        getStyleClass().add("popup");
        getStylesheets().add("css/popup.css");
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
         * Alternate PopWindow between maximized and unmaximized and update MaximizeButton accordingly
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
     * Adds this PopWindow to its container and makes it visible.
     * @param modal whether the PopWindow's siblings should be disabled until hidden.
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
            popWindows.push(this);
        }
    }

    /**
     * Hides this PopWindow and removes it from its parent. Ensures that its siblings
     * are now enabled (in case this PopWindow is modal).
     */
    void hide() {
        setVisible(false);
        container.getChildren().remove(this);
        for(Node child : container.getChildren()) {
            child.setDisable(false);
        }
        if(!popWindows.isEmpty()) {
            popWindows.remove(this);
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
     * Adds a button that allows this PopWindow to fill the screen
     */
    void makeMaximizable() {
        titleBar.getChildren().remove(closeButton);
        titleBar.getChildren().addAll(maximizeButton, closeButton);
    }

    /**
     * Allows the user to resize this PopWindow by dragging its borders
     */
    void makeResizable() {
        DragResizer.makeResizable(this);
    }

    /**
     * Allows the user to move this PopWindow by dragging one of the provided regions.
     *
     * @param handles Regions that should serve as handles for dragging
     */
    void setAsDragZone(Region... handles) {
        for(Region handle : handles) {
            handle.setOnMousePressed(this::mousePressed);
            handle.setOnMouseDragged(this::mouseDragged);
            handle.setOnMouseReleased(this::mouseReleased);
        }
    }

    /**
     *
     */
    void setAsMaximizeZone(Region... handles) {
        for(Region handle : handles) {
            handle.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
                    maximizeButton.fire();
            });
        }
    }

    /**
     *
     */
    private void mousePressed(MouseEvent event) {
        mouseLocation.set(new Point2D(event.getSceneX(), event.getSceneY()));
    }

    /**
     *
     */
    private void mouseDragged(MouseEvent event) {
        newLocation.set(new Point2D(event.getSceneX(), event.getSceneY()));
        if(mouseLocation.get() != null && newLocation.get() != null) {
            setTranslateX(getTranslateX() + newLocation.get().getX() - mouseLocation.get().getX());
            setTranslateY(getTranslateY() + newLocation.get().getY() - mouseLocation.get().getY());
            mouseLocation.set(newLocation.get());
        }
    }

    /**
     *
     */
    protected void mouseReleased(MouseEvent event) {
         mouseLocation.set(null);
         newLocation.set(null);
    }
}
