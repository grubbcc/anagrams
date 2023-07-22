package client;


import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;


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

    protected final JSONArray gameLog;

    /**
     *
     */
    GameWindowBase(AnagramsClient client, String gameID, String username, String minLength, int blankPenalty, String numSets, String speed, String lexicon, JSONArray gameLog) {

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

        ColumnConstraints col0 = new ColumnConstraints(175, 342, 735, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col1 = new ColumnConstraints(175, 342, 735, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col2 = new ColumnConstraints(175, 342, 735, Priority.ALWAYS, HPos.CENTER, true);

        RowConstraints row0 = new RowConstraints();
        row0.setPercentHeight(36);
        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(36);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(28);

        gameGrid.getColumnConstraints().addAll(col0, col1, col2);
        gameGrid.getRowConstraints().addAll(row0, row1, row2);

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

        tilePanel.widthProperty().addListener((obs, oldVal, newVal) -> setTiles(tilePool));
    }



    /**
     * A panel for displaying the name, score, and words possessed by a player.
     */
    protected class GamePanel extends BorderPane {

        String playerName = null;
        protected final HBox infoPane = new HBox();
        protected final FlowPane wordPane = new FlowPane();
        protected final ScrollPane scrollPane = new ScrollPane(wordPane);

        protected final Label playerNameLabel = new Label();
        protected final Label playerScoreLabel = new Label();

        private final SimpleIntegerProperty tileWidth = new SimpleIntegerProperty();
        private final SimpleIntegerProperty tileHeight = new SimpleIntegerProperty();
        private final SimpleIntegerProperty tileFontSize = new SimpleIntegerProperty();
        private final SimpleBooleanProperty savingSpace = new SimpleBooleanProperty(false);

        final int column;

        boolean isAvailable = true;
        private final LinkedHashMap<String, Word> words = new LinkedHashMap<>();
        protected int score;


        /**
         * An empty placeholder gamePanel
         */
        protected GamePanel(int column) {
            getStyleClass().add("game-panel");

            gamePanels.add(this);
            this.column = column;

            infoPane.getChildren().add(playerNameLabel);
            infoPane.getChildren().add(playerScoreLabel);

            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.prefViewportWidthProperty().bind(widthProperty().subtract(2));
            scrollPane.prefViewportHeightProperty().bind(heightProperty().subtract(2));

            wordPane.setMaxHeight(column >= 0 ? 185 : 137);

            GridPane.setHgrow(this, Priority.ALWAYS);
            GridPane.setVgrow(this, Priority.ALWAYS);
            wordPane.setAlignment(Pos.TOP_CENTER);
            wordPane.hgapProperty().bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 6 : 12, savingSpace));
            wordPane.vgapProperty().bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 2 : 6, savingSpace));

            tileWidth.bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 12 : 16, savingSpace));
            tileHeight.bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 16 : 20, savingSpace));
            tileFontSize.bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 18 : 24, savingSpace));

            setTop(infoPane);
            setCenter(scrollPane);

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
            words.clear();
            wordPane.getChildren().clear();
            playerName = null;
            playerNameLabel.setText("");
            playerScoreLabel.setText("");
            playerNameLabel.setGraphic(null);
            isAvailable = true;
            savingSpace.set(false);
        }


        /**
         * Adds a bunch of words all at once. Used during postgame analysis, when resizing the window,
         * and when joining or starting a game.
         */
        protected void addWords(Collection<Word> wordsToAdd) {

            if(wordsToAdd.isEmpty())
                return;

            setScore(wordsToAdd);

            double width = wordPane.getWidth();
            double height = wordPane.getMaxHeight();

            boolean widthChanged = false;

            if(!savingSpace.get()) {
                if (willOverflow(width, height - 4, false)) {
                    savingSpace.set(true);
                }
            }

            if(savingSpace.get() && column >= 0) {
                ColumnConstraints constraints = gameGrid.getColumnConstraints().get(column);
                double columnWidth = constraints.getPrefWidth();
                while (willOverflow(width, height - 4, true)) {
                    widthChanged = true;
                    width *= 1.15;
                    columnWidth *= 1.15;
                }
                if(widthChanged) {
                    constraints.setMinWidth(columnWidth);
                    allocateSpace();
                }
            }

            for(Word word : words.values())
                word.draw();

            wordPane.getChildren().addAll(words.values());
        }

        /**
         *
         */
        protected void setScore(Collection<Word> words) {
            score = 0;
            for (Word word : words) {

                score += word.length() * word.length();

                playerScoreLabel.setText(score + "");
            }
        }

        /**
         * Check if this panel of given width can fit the words of the given size without resizing.
         * <b>
         * Used during postgame in conjunction with the addWords method.
         * @param paneWidth How wide this pane should be.
         */
        private boolean willOverflow(double paneWidth, double paneHeight, boolean small) {
            double HGAP = small ? 6 : 12;
            double VGAP = small ? 7 : 14;
            double TILE_WIDTH = small ? 12 : 16;
            double TILE_HEIGHT = small ? 16 : 20;

            double x = -HGAP;
            double y = small ? 4.4 + TILE_HEIGHT : 7.2 + TILE_HEIGHT;

            for(Word word : words.values()) {
                x += HGAP + word.length()*TILE_WIDTH + (word.length()-1)* GamePanel.Word.TILE_GAP;
                if(x > paneWidth) {
                    x = -HGAP + word.length()*(TILE_WIDTH) + (word.length()-1)* GamePanel.Word.TILE_GAP + 2* GamePanel.Word.PADDING;
                    y += VGAP + TILE_HEIGHT;
                }
            }

            return y > paneHeight;

        }



        /**
         *
         */
        protected class Word extends Region {

            protected static final int PADDING = 4;
            protected final static double TILE_GAP = 2;
            protected final String letters;
            protected final boolean highlight;


            /**
             *
             */
            protected Word(String symbols) {
                if(symbols.endsWith("#") || symbols.endsWith("$")) {
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
                return length()*tileWidth.get() + (length() - 1)*TILE_GAP + 1 + PADDING;
            }

            /**
             *
             */
            void draw() {
                getChildren().clear();

                int x = 0;

                for (Character tile : letters.toCharArray()) {

                    Rectangle rect = new Rectangle(x, 0, tileWidth.get(), tileHeight.get());
                    rect.setArcWidth(2);
                    rect.setArcHeight(2);
                    rect.setFill(highlight ? Color.GOLD : Color.YELLOW);

                    Text text = new Text(x + 1, tileHeight.get() - 2, String.valueOf(Character.toUpperCase(tile)));
                    text.setFont(Font.font("Courier New", FontWeight.BOLD, tileFontSize.get()));
                    text.setFill(Character.isLowerCase(tile) ? Color.RED : Color.BLACK);

                    getChildren().addAll(rect, text);
                    x += tileWidth.get() + TILE_GAP;
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
        tilePool = nextTiles.equals("#") ? "" : nextTiles;
        tilePanel.showTiles(tilePool);
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
            ColumnConstraints constraints = gameGrid.getColumnConstraints().get(i);
            constraints.setPrefWidth(Math.max(constraints.getPrefWidth(), constraints.getMinWidth() + availableSpace / 3));
        }
    }

    /**
     * Displays the provided game position: time remaining, the tile pool, the players, and their words.
     * If the WordDisplay is visible retrieves possible plays from the server.
     *
     * @param gameState the current game state, an array e.g.
     *               ["257", "YUIFOTMR", "GrubbTime", "[HAUYNES]", "Robot-Genius", "[BLEWARTS,POTJIES]"]
     *               consisting of the remaining time, the tile pools, a list of players (even indices)
     *               and their words (odd indices)
     */
    void showPosition(JSONObject gameState) {

        for (GamePanel panel : gamePanels) {
            panel.reset();
        }

        for (int c = 0; c < 3; c++) {
            ColumnConstraints constraints = gameGrid.getColumnConstraints().get(c);
            constraints.setMinWidth(Math.max(constraints.getMinWidth(), MIN_PANEL_WIDTH));
            constraints.setPrefWidth(Math.max(constraints.getPrefWidth(), 326));
        }

        notificationArea.setText("Time remaining " + gameState.getInt("time"));

        JSONArray players = gameState.getJSONArray("players");
        for(int p = 0; p < players.length(); p++) {
            JSONObject player = players.getJSONObject(p);
            GamePanel panel = addPlayer(player.getString("name"));
            for(int s = 0; s < player.getJSONArray("words").length(); s++) {
                String symbols = player.getJSONArray("words").getString(s);
                GamePanel.Word newWord = panel.new Word(symbols);
                panel.words.put(symbols, newWord);
            }
            panel.addWords(panel.words.values());

        }

        setTiles(gameState.getString("tiles"));

    }

}
