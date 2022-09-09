package client;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashSet;


abstract class GameWindowBase extends BorderPane {

    protected final AnagramsClient client;
    protected final Label notificationArea = new Label("");

    protected final LinkedHashSet<GamePanel> gamePanels = new LinkedHashSet<>();
    protected final GridPane gameGrid = new GridPane();
    protected final GamePanel homePanel = new GamePanel(-1);
    protected final TilePanel tilePanel = new TilePanel();
    protected final Image blackRobot = new Image(getClass().getResourceAsStream("/images/black robot.png"));
    protected final Image whiteRobot = new Image(getClass().getResourceAsStream("/images/white robot.png"));
    protected final ImageView robotImage = new ImageView(blackRobot);

    protected final static double MIN_PANEL_WIDTH = 175;
    protected final String username;
    protected final String gameID;
    protected String tilePool = "";
    protected final ArrayList<String[]> gameLog;

    /**
     *
     */
    GameWindowBase(AnagramsClient client, String gameID, String username, String minLength, int blankPenalty, String numSets, String speed, String lexicon, ArrayList<String[]> gameLog) {

        this.client = client;
        this.username = username;
        this.gameID = gameID;
        this.gameLog = gameLog;

        //control panel
        HBox controlPanel = new HBox();
        controlPanel.getStyleClass().add("control-panel");
        controlPanel.getChildren().addAll(
                notificationArea,
                new Label(lexicon),
                new Label("Minimum length = " + minLength),
                new Label("Blank penalty = " + blankPenalty),
                new Label("Speed = " + speed)
        );

        //game panels
        gameGrid.setPadding(new Insets(3));
        gameGrid.setHgap(3);
        gameGrid.setVgap(3);

        ColumnConstraints col1 = new ColumnConstraints(175, 342, 735, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col2 = new ColumnConstraints(175, 342, 735, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col3 = new ColumnConstraints(175, 342, 735, Priority.ALWAYS, HPos.CENTER, true);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(36);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(36);
        RowConstraints row3 = new RowConstraints();
        row3.setPercentHeight(28);

        gameGrid.getColumnConstraints().addAll(col1, col2, col3);
        gameGrid.getRowConstraints().addAll(row1, row2, row3);

        gameGrid.add(tilePanel, 1, 1);
        gameGrid.add(new GameWindowBase.GamePanel(0), 0, 0);
        gameGrid.add(new GamePanel(1), 1, 0);
        gameGrid.add(new GamePanel(2), 2, 0);
        gameGrid.add(new GamePanel(0), 0, 1);
        gameGrid.add(new GamePanel(2), 2, 1);
        gameGrid.add(homePanel, 0, 2, 3, 1);

        //main layout
        setTop(controlPanel);
        setCenter(gameGrid);

        getStyleClass().add("game-background");
        getStylesheets().setAll("css/anagrams.css", "css/popup.css", "css/game-window.css");

        String newStyle = "";
        for (AnagramsClient.Colors color : client.colors.keySet()) {
            newStyle += color.css + ": " + client.colors.get(color) + "; ";
            newStyle += color.css + "-text: " + AnagramsClient.getTextColor(client.colors.get(color)) + "; ";
        }

        setStyle(newStyle);

        if (AnagramsClient.getTextColor(client.colors.get(AnagramsClient.Colors.GAME_FOREGROUND)).equals("white"))
            robotImage.setImage(whiteRobot);
        else
            robotImage.setImage(blackRobot);
    }


    /**
     * A panel for displaying the tilePool
     */
    protected class TilePanel extends Pane {

        protected TilePanel() {
            getStyleClass().add("tile-panel");
            setMinWidth(MIN_PANEL_WIDTH);

            widthProperty().addListener((obs, oldVal, newVal) -> setTiles(tilePool));
            heightProperty().addListener((obs, oldVal, newVal) -> setTiles(tilePool));
        }

        /**
         * Draws the tiles in a spiral pattern
         */
        protected void showTiles() {

            getChildren().clear();

            for (int i = 1; i <= tilePool.length(); i++) {
                double x = getWidth() / 2 + 16 * Math.sqrt(i) * Math.cos(Math.sqrt(i) * Math.PI * 4 / 3);
                double y = 3 + getHeight() / 2 + 16 * Math.sqrt(i) * Math.sin(Math.sqrt(i) * Math.PI * 4 / 3);

                Rectangle rect = new Rectangle(x - 1, y - 21, 20, 21);
                rect.setArcWidth(2);
                rect.setArcHeight(2);
                rect.setFill(Color.YELLOW);

                Text text = new Text(x, y - 2, String.valueOf(tilePool.charAt(i - 1)));
                text.setFont(Font.font("Courier New", FontWeight.BOLD, 28));

                getChildren().addAll(rect, text);
            }
            if (!getChildren().isEmpty()) {
                getChildren().get(0).setTranslateY(10);
                getChildren().get(1).setTranslateY(10);
            }
        }
    }

    /**
     * A panel for displaying the name, score, and words possessed by a player.
     */
    protected class GamePanel extends BorderPane {

        String playerName = null;
        protected int score;
        protected final HBox infoPane = new HBox();
        protected final FlowPane wordPane = new FlowPane();
        protected final Label playerNameLabel = new Label();
        protected final Label playerScoreLabel = new Label();

        protected final SimpleBooleanProperty savingSpace = new SimpleBooleanProperty(false);

        protected static final int PADDING = 4;
        final int column;
        protected int tileWidth;
        protected int tileHeight;
        protected int tileFontSize;

        boolean isAvailable = true;

        /**
         * An empty placeholder gamePanel
         */
        protected GamePanel(int column) {
            getStyleClass().add("game-panel");

            gamePanels.add(this);
            this.column = column;

            infoPane.getChildren().add(playerNameLabel);
            infoPane.getChildren().add(playerScoreLabel);

            wordPane.setAlignment(Pos.TOP_CENTER);
            wordPane.hgapProperty().bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 6 : 12, savingSpace));
            wordPane.vgapProperty().bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 2 : 6, savingSpace));

            setTop(infoPane);
            setCenter(wordPane);
            makeBig();
        }


        /**
         * Puts the given player's name on this panel and displays their score.
         *
         * @param newPlayer The name of the player to be added.
         */
        protected GamePanel takeSeat(String newPlayer) {
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
                GamePanel.Word newWord = new GamePanel.Word(word);
                wordPane.getChildren().add(newWord);
            }

            layout();

            double paneWidth = getWidth();
            double paneHeight = getHeight() - infoPane.getHeight();
            double maxY = wordPane.getChildren().get(wordsToAdd.length - 1).getBoundsInParent().getMaxY();

            //check if we need to save space by switching to small tiles
            double minWidth = Math.max(MIN_PANEL_WIDTH, paneWidth * maxY / paneHeight);
            if(maxY > paneHeight) {
                if (!savingSpace.get()) {
                    gameGrid.getColumnConstraints().get(column).setMinWidth(324);
                    makeSmall();
                    addWords(wordsToAdd);
                } else if (column >= 0) {
                    gameGrid.getColumnConstraints().get(column).setMinWidth(minWidth);
                    allocateSpace();
                }
            }
            else if (column >= 0) {
                gameGrid.getColumnConstraints().get(column).setMinWidth(minWidth);
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


    /**
     * Instructs the TilePanel to update its display. Used when the tilePool changes
     * or when the window is resized.
     *
     * @param nextTiles The letters that the TilePanel should display.
     */
    void setTiles(String nextTiles) {
        if (nextTiles.equals("#"))
            nextTiles = "";
        tilePool = nextTiles;
        tilePanel.showTiles();
    }

    /**
     * If the player already has words at this table, the player claims them.
     * Otherwise, the player takes the next available seat, if there is one.
     *
     * @param newPlayerName the name of the player to be added.
     * @return the GamePanel to which the player has been added or null if
     * none is available
     */
    GamePanel addPlayer(String newPlayerName) {

        //current player is assigned homePanel
        if (newPlayerName.equals(username)) {
            return homePanel.takeSeat(newPlayerName);
        }

        //new player is assigned the first available seat
        else {
            for (GamePanel panel : gamePanels) {
                if (panel.isAvailable && panel.column >= 0) {
                    return panel.takeSeat(newPlayerName);
                }
            }
        }
        return null;
    }

    /**
     *
     */
    private void allocateSpace() {

        double availableSpace = getPrefWidth() - 10;

        for (int i = 0; i < 3; i++) {
            availableSpace -= gameGrid.getColumnConstraints().get(i).getMinWidth();
        }
        for (int i = 0; i < 3; i++) {
            ColumnConstraints currentColumn = gameGrid.getColumnConstraints().get(i);
            currentColumn.setPrefWidth(currentColumn.getMinWidth() + availableSpace / 3);
        }
    }

    /**
     * Displays the provided game position: time remaining, the tile pool, the players, and their words.
     * If the WordDisplay is visible retrieves possible plays from the server.
     *
     * @param tokens the current game state, an array e.g.
     *               ["257", "YUIFOTMR", "GrubbTime", "[HAUYNES]", "Robot-Genius", "[BLEWARTS,POTJIES]"]
     *               consisting of the remaining time, the tile pools, a list of players (even indices)
     *               and their words (odd indices)
     */
    void showPosition(String[] tokens) {
        for (GamePanel panel : gamePanels)
            panel.reset();
        for (int i = 0; i < 3; i++) {
            gameGrid.getColumnConstraints().get(i).setMinWidth(MIN_PANEL_WIDTH);
            gameGrid.getColumnConstraints().get(i).setPrefWidth(326);
        }

        notificationArea.setText("Time remaining " + tokens[0]);

        if (tokens.length > 2) {
            for (int i = 2; i < tokens.length; i += 2) {
                String playerName = tokens[i];
                GamePanel panel = addPlayer(playerName);
                String[] words = tokens[i + 1].substring(1, tokens[i + 1].length() - 1).split(",");
                panel.addWords(words);
                panel.setScore(words);
            }
        }

        setTiles(tokens[1]);
    }

}
