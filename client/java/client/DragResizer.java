package client;

import javafx.geometry.Point2D;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * Adapted from https://gist.github.com/andytill/4369729
 * @author atill
 */

@SuppressWarnings("UnnecessaryReturnStatement")
public class DragResizer {

    /**
     * The margin (in pixels) around the control that a user can click to resize the region.
     */

    private static final int RESIZE_MARGIN = 6;

    private final Region region;
    private boolean initMinHeight;

    private boolean draggingNorth;
    private boolean draggingEast;
    private boolean draggingSouth;
    private boolean draggingWest;

    private final ObjectProperty<Point2D> mouseLocation = new SimpleObjectProperty<>();

    /**
     *
     */
    
    private DragResizer(Region aRegion) {
        region = aRegion;
    }

    /**
     *
     */

    public static void makeResizable(Region region) {
        final DragResizer resizer = new DragResizer(region);

        region.setOnMousePressed(resizer::mousePressed);
        region.setOnMouseDragged(resizer::mouseDragged);
        region.setOnMouseMoved(resizer::mouseOver);
        region.setOnMouseReleased(resizer::mouseReleased);
    }

    /**
     * Sets the cursor to the appropriate type.
     */

    protected void mouseOver(MouseEvent event) {
        if (isInDraggableZoneN(event) || draggingNorth) {
            if(isInDraggableZoneE(event) || draggingEast) {
                region.setCursor(Cursor.NE_RESIZE);
            }
            else if(isInDraggableZoneW(event) || draggingWest) {
                region.setCursor(Cursor.NW_RESIZE);
            }
            else {
                region.setCursor(Cursor.N_RESIZE);
            }
        }
        else if (isInDraggableZoneS(event) || draggingSouth) {
            if(isInDraggableZoneE(event) || draggingEast) {
                region.setCursor(Cursor.SE_RESIZE);
            }
            else if(isInDraggableZoneW(event) || draggingWest) {
                region.setCursor(Cursor.SW_RESIZE);
            }
            else {
                region.setCursor(Cursor.S_RESIZE);
            }
        }
        else if (isInDraggableZoneE(event) || draggingEast) {
            region.setCursor(Cursor.E_RESIZE);
        }
        else if (isInDraggableZoneW(event) || draggingWest) {
            region.setCursor(Cursor.W_RESIZE);
        }
        else {
            region.setCursor(Cursor.DEFAULT);
        }
    }

    /**
     *
     */

    private void mousePressed(MouseEvent event) {

        event.consume();

        //should bring clicked gameWindow to front if multiple gameWindows are open

        draggingNorth = isInDraggableZoneN(event);
        draggingEast = isInDraggableZoneE(event);
        draggingSouth = isInDraggableZoneS(event);
        draggingWest = isInDraggableZoneW(event);

        mouseLocation.set(new Point2D((float)event.getScreenX(), (float)event.getScreenY()));

        // Make sure that the minimum height is set to the current height once;
        // setting a min height that is smaller than the current height will have no effect.
        if (!initMinHeight) {
            region.setMinHeight(region.getHeight());
            region.setMinWidth(region.getWidth());
            initMinHeight = true;
        }
    }

    /**
     *
     */

    private boolean isInDraggableZoneN(MouseEvent event) {
        return event.getY() < RESIZE_MARGIN;
    }

    /**
     * 
     */

    private boolean isInDraggableZoneW(MouseEvent event) {
        return event.getX() < RESIZE_MARGIN;
    }

    /**
     * 
     */

    private boolean isInDraggableZoneS(MouseEvent event) {
        return event.getY() > (region.getHeight() - RESIZE_MARGIN);
    }

    /**
     * 
     */

    private boolean isInDraggableZoneE(MouseEvent event) {
        return event.getX() > (region.getWidth() - RESIZE_MARGIN);
    }

    /**
     * 
     */

    private void mouseDragged(MouseEvent event) {

        event.consume();

        if (draggingSouth) resizeSouth(event);
        if (draggingEast) resizeEast(event);
        if (draggingNorth) resizeNorth(event);
        if (draggingWest) resizeWest(event);

        if(draggingSouth || draggingEast || draggingNorth || draggingWest) {
            return;
        }
        else if(mouseLocation.get() != null) {
            double x = event.getScreenX();
            double y = event.getScreenY();

            region.setTranslateX(region.getTranslateX() + x - mouseLocation.get().getX());
            region.setTranslateY(region.getTranslateY() + y - mouseLocation.get().getY());
            mouseLocation.set(new javafx.geometry.Point2D(x, y));
        }
    }

    /**
     *
     */

    private void resizeNorth(MouseEvent event) {
        double prevMin = region.getMinHeight();
        region.setMinHeight(region.getMinHeight() - event.getY());
        if (region.getMinHeight() < region.getPrefHeight()) {
            region.setMinHeight(region.getPrefHeight());
            region.setTranslateY(region.getTranslateY() - (region.getPrefHeight() - prevMin));
            return;
        }
        if (region.getMinHeight() > region.getPrefHeight() || event.getY() < 0)
            region.setTranslateY(region.getTranslateY() + event.getY());
    }

    /**
     * 
     */

    private void resizeEast(MouseEvent event) {
        region.setMinWidth(event.getX());
    }

    /**
     *
     */

    private void resizeSouth(MouseEvent event) {
        region.setMinHeight(event.getY());
    }

    /**
     *
     */

    private void resizeWest(MouseEvent event) {
        double prevMin = region.getMinWidth();
        region.setMinWidth(region.getMinWidth() - event.getX());
        if (region.getMinWidth() < region.getPrefWidth()) {
            region.setMinWidth(region.getPrefWidth());
            region.setTranslateX(region.getTranslateX() - (region.getPrefWidth() - prevMin));
            return;
        }
        if (region.getMinWidth() > region.getPrefWidth() || event.getX() < 0)
            region.setTranslateX(region.getTranslateX() + event.getX());
    }

    /**
     *
     */

    protected void mouseReleased(MouseEvent event) {
        initMinHeight = false; //Reset each time
        draggingNorth = false; draggingEast = false; draggingSouth = false; draggingWest = false;
        region.setCursor(Cursor.DEFAULT);
        mouseLocation.set(null);
    }
}