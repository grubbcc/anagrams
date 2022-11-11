package client;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * A panel for displaying the tilePool
 */
class TilePanel extends Pane {

    protected TilePanel() {
        getStyleClass().add("tile-panel");
        setMinWidth(GameWindowBase.MIN_PANEL_WIDTH);

    }

    /**
     * Draws the tiles in a spiral pattern
     */
    protected void showTiles(String tilePool) {

        getChildren().clear();

        for (int i = 1; i <= tilePool.length(); i++) {
            double x = getWidth() / 2 + 16 * Math.sqrt(i) * Math.cos(Math.sqrt(i) * Math.PI * 4 / 3);
            double y = 3 + getHeight() / 2 + 16 * Math.sqrt(i) * Math.sin(Math.sqrt(i) * Math.PI * 4 / 3);

            Rectangle rect = new Rectangle(x - 1, y - 21, 20, 21);
            rect.setArcWidth(2);
            rect.setArcHeight(2);
            rect.setFill(Color.YELLOW);

            char c = tilePool.charAt(i - 1);
            Text text = new Text(x, c == 'Q' ? y - 3 : y - 2, c + "");
            text.setFont(Font.font("Courier New", FontWeight.BOLD, 28));

            getChildren().addAll(rect, text);
        }
        if (!getChildren().isEmpty()) {
            getChildren().get(0).setTranslateY(10);
            getChildren().get(1).setTranslateY(10);
        }
    }
}