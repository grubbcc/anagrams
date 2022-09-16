package client;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * A panel for displaying the name, score, and words possessed by a player.
 */
abstract class GamePanelBase extends BorderPane {

    String playerName = null;
    protected int score;
    protected final HBox infoPane = new HBox();
    protected final FlowPane wordPane = new FlowPane();
    protected final Label playerNameLabel = new Label();
    protected final Label playerScoreLabel = new Label();

    protected final SimpleBooleanProperty savingSpace = new SimpleBooleanProperty(false);

    protected final Image blackRobot = new Image(getClass().getResourceAsStream("/images/black robot.png"));
    protected final Image whiteRobot = new Image(getClass().getResourceAsStream("/images/white robot.png"));
    protected final ImageView robotImage = new ImageView(blackRobot);

    protected double minPanelWidth;
    protected double minPanelHeight;
    protected static final int PADDING = 4;
    final int column;
    protected int tileWidth;
    protected int tileHeight;
    protected int tileFontSize;

    boolean isAvailable = true;

    /**
     * An empty placeholder gamePanel
     */
    protected GamePanelBase(int column) {
        getStyleClass().add("game-panel");
        this.column = column;

        infoPane.getChildren().add(playerNameLabel);
        infoPane.getChildren().add(playerScoreLabel);

        playerScoreLabel.setMinWidth(USE_PREF_SIZE);

        wordPane.setAlignment(Pos.TOP_CENTER);
        wordPane.hgapProperty().bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 6 : 12, savingSpace));
        wordPane.vgapProperty().bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 2 : 6, savingSpace));

        setTop(infoPane);

        makeBig();
    }


    /**
     * Puts the given player's name on this panel and displays their score.
     *
     * @param newPlayer The name of the player to be added.
     */
    protected GamePanelBase takeSeat(String newPlayer) {
        this.playerName = newPlayer;
        playerNameLabel.setText(playerName);

        if (playerName.startsWith("Robot")) {
            playerNameLabel.setGraphic(robotImage);
        }
        isAvailable = false;

        return this;
    }

    /**
     * Removes the occupant, as well as any words they may have, from this pane.
     * Used only during endgame analysis.
     */
    protected void reset() {
        wordPane.getChildren().clear();
        playerName = null;
        playerNameLabel.setText("");
        playerNameLabel.setGraphic(null);
        playerScoreLabel.setText("");
        isAvailable = true;
        makeBig();
    }


    /**
     * Adds a bunch of words all at once. Used during postgame analysis, when resizing the window,
     * and when joining or starting a game.
     */
    protected void addWords(String[] wordsToAdd) {

        wordPane.getChildren().clear();

        for (String word : wordsToAdd) {
            GamePanelBase.Word newWord = new GamePanelBase.Word(word);
            wordPane.getChildren().add(newWord);
        }

        layout();

        double paneWidth = getWidth();
        double paneHeight = getHeight() - infoPane.getHeight();
        double maxY = wordPane.getChildren().get(wordsToAdd.length - 1).getBoundsInParent().getMaxY();

        //check if we need to save space by switching to small tiles
        if (!savingSpace.get() && maxY > paneHeight) {
            makeSmall();
            addWords(wordsToAdd);
        }
        else if(column >= 0) {
            setMinWidth(Math.max(minPanelWidth, paneWidth * maxY / paneHeight));
        }
    }

    /**
     *
     */
    protected void setScore(String[] words) {
        score = 0;
        for (String word : words) {
            if(word.endsWith("#"))
                score += (word.length() - 1) * (word.length() - 1);
            else
                score += word.length() * word.length();
            playerScoreLabel.setText(score + "");
        }
    }

    /**
     *
     */
    protected void makeSmall() {
        savingSpace.set(true);
        tileWidth = 12;
        tileHeight = 16;
        tileFontSize = 18;
    }

    /**
     *
     */
    protected void makeBig() {
        savingSpace.set(false);
        tileWidth = 16;
        tileHeight = 20;
        tileFontSize = 24;
    }

    /**
     *
     */
    protected class Word extends Region {

        protected final static double TILE_GAP = 2;
        protected final String letters;
        protected final boolean highlight;

        /**
         *
         */
        protected Word(String symbols) {
            if(symbols.endsWith("#")) {
                letters = symbols.substring(0, symbols.length() - 1);
                highlight = true;
            }
            else {
                letters = symbols;
                highlight = false;
            }

            draw();
        }

        /**
         *
         */
        protected int length() {
            return letters.length();
        }

        /**
         *
         */
        protected double width() {
            return length()*tileWidth + (length() - 1)*TILE_GAP + 1 + PADDING;
        }

        /**
         * Sets the robot icon to be visible against the background.
         *
         * @param dark whether the game foreground has a luminance < 40.
         */
        void setDark(Boolean dark) {
            if (dark)
                robotImage.setImage(whiteRobot);
            else
                robotImage.setImage(blackRobot);
        }

        /**
         *
         */
        void draw() {
            getChildren().clear();

            int x = 0;

            for (Character tile : letters.toCharArray()) {

                Rectangle rect = new Rectangle(x, 0, tileWidth, tileHeight);
                rect.setArcWidth(2);
                rect.setArcHeight(2);
                rect.setFill(highlight ? Color.GOLD : Color.YELLOW);

                Text text = new Text(x + 1, tileHeight - 2, String.valueOf(Character.toUpperCase(tile)));
                text.setFont(Font.font("Courier New", FontWeight.BOLD, tileFontSize));
                text.setFill(Character.isLowerCase(tile) ? Color.RED : Color.BLACK);

                getChildren().addAll(rect, text);
                x += tileWidth + TILE_GAP;
            }
        }
    }

}