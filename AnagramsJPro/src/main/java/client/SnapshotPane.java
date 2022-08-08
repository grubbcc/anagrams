package client;

import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A user interface for playing, watching, or analyzing a game.
 */
class SnapshotPane extends BorderPane {

    private final AnagramsClient client;
    private final Label notificationArea = new Label("");

    private final LinkedHashSet<GamePanel> gamePanels = new LinkedHashSet<>();
    private final GridPane gameGrid = new GridPane();
    private final GamePanel homePanel = new GamePanel(-1);
    private final TilePanel tilePanel = new TilePanel();
    private final Image blackRobot = new Image(getClass().getResourceAsStream("/images/black robot.png"));
    private final Image whiteRobot = new Image(getClass().getResourceAsStream("/images/white robot.png"));
    private final ImageView robotImage = new ImageView(blackRobot);

    private final double minPanelWidth = 175;
    private final double minPanelHeight = 100;
    private final String username;

    private final String gameID;

    private String tilePool = "";
    final SimpleDoubleProperty progress = new SimpleDoubleProperty(0);
    final SimpleBooleanProperty finished = new SimpleBooleanProperty(false);

    ArrayList<String[]> gameLog;
    GifSequenceWriter writer;

    /**
     *
     */

    SnapshotPane(AnagramsClient client, String gameID, String username, String minLength, int blankPenalty, String numSets, String speed, String lexicon, ArrayList<String[]> gameLog) {

        this.client = client;
        this.username = username;
        this.gameID = gameID;
        this.gameLog = gameLog;

        //control panel
        HBox controlPanel = new HBox();
        controlPanel.getStyleClass().add("control-panel");
        controlPanel.getChildren().addAll(notificationArea, new Label(lexicon), new Label("Minimum length = " + minLength), new Label("Blank penalty = " + blankPenalty), new Label("Speed = " + speed));

        //game panels
        gameGrid.setPadding(new Insets(3));
        gameGrid.setHgap(3);
        gameGrid.setVgap(3);

        ColumnConstraints col1 = new ColumnConstraints(minPanelWidth, 342, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col2 = new ColumnConstraints(minPanelWidth, 342, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col3 = new ColumnConstraints(minPanelWidth, 342, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(36);
        row1.setFillHeight(true);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(36);
        row2.setFillHeight(true);
        RowConstraints row3 = new RowConstraints();
        row3.setPercentHeight(28);
        row3.setFillHeight(true);

        gameGrid.getColumnConstraints().addAll(col1, col2, col3);
        gameGrid.getRowConstraints().addAll(row1, row2, row3);

        gameGrid.add(tilePanel, 1, 1);
        gameGrid.add(new GamePanel(0), 0, 0);
        gameGrid.add(new GamePanel(1), 1, 0);
        gameGrid.add(new GamePanel(2), 2, 0);
        gameGrid.add(new GamePanel(0), 0, 1);
        gameGrid.add(new GamePanel(2), 2, 1);
        gameGrid.add(homePanel, 0, 2, 3, 1);

        //main layout
        setTop(controlPanel);
        setCenter(gameGrid);
        final double HEIGHT = 630;
        final double WIDTH = 1050;
        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);

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


        Scene scene = new Scene(this, WIDTH, HEIGHT);
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setScene(scene);

        try {
            animate();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
     * A panel for displaying the tilePool
     */
    private class TilePanel extends Pane {

        private TilePanel() {
            getStyleClass().add("tile-panel");
            setMinSize(minPanelWidth, minPanelHeight);

            widthProperty().addListener((obs, oldVal, newVal) -> setTiles(tilePool));
            heightProperty().addListener((obs, oldVal, newVal) -> setTiles(tilePool));
        }

        /**
         * Draws the tiles in a spiral pattern
         */
        private void showTiles() {

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
    private class GamePanel extends BorderPane {

        String playerName = null;
        private final HBox infoPane = new HBox();
        private final FlowPane wordPane = new FlowPane();
        private final Label playerNameLabel = new Label();
        private final Label playerScoreLabel = new Label();

        private final SimpleBooleanProperty savingSpace = new SimpleBooleanProperty(false);

        final int column;
        private int tileWidth;
        private int tileHeight;
        private int tileFontSize;

        boolean isAvailable = true;

        /**
         * An empty placeholder gamePanel
         */
        private GamePanel(int column) {
            getStyleClass().add("game-panel");

            gamePanels.add(this);
            this.column = column;

            infoPane.getChildren().add(playerNameLabel);
            infoPane.getChildren().add(playerScoreLabel);

            playerScoreLabel.setMinWidth(USE_PREF_SIZE);

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
        private GamePanel takeSeat(String newPlayer) {
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
        private void reset() {
            wordPane.getChildren().clear();
            playerNameLabel.setGraphic(null);
            makeBig();
            isAvailable = true;
            playerNameLabel.setText("");
            playerScoreLabel.setText("");
            playerName = null;
        }


        /**
         * Adds a bunch of words all at once. Used during analysis, when resizing the window,
         * and when joining or starting a game.
         */
        private void addWords(String[] wordsToAdd) {

            WordLabel newWordLabel = new WordLabel(wordsToAdd[0]);

            for (String word : wordsToAdd) {
                newWordLabel = new WordLabel(word);
                wordPane.getChildren().add(newWordLabel);
            }

            applyCss();
            layout();

            double paneWidth = getWidth();
            double paneHeight = getHeight() - infoPane.getHeight();
            double maxY = newWordLabel.getLayoutY() + newWordLabel.getHeight();

            //check if we need to save space by switching to small tiles
            if (!savingSpace.get() && maxY > paneHeight) {
                makeSmall();
                wordPane.getChildren().clear();
                addWords(wordsToAdd);
                return;
            }

            if (column >= 0 && maxY > paneHeight) {
                gameGrid.getColumnConstraints().get(column).setMinWidth(Math.max(minPanelWidth, paneWidth * maxY / paneHeight));

                int score = 0;
                for (String word : wordsToAdd) {
                    score += word.length() * word.length();
                    playerScoreLabel.setText(score + "");
                }
                allocateSpace();
            }
        }


        /**
         *
         */
        private void makeSmall() {
            savingSpace.set(true);
            tileWidth = 12;
            tileHeight = 16;
            tileFontSize = 18;
        }

        /**
         *
         */
        private void makeBig() {
            savingSpace.set(false);
            tileWidth = 16;
            tileHeight = 20;
            tileFontSize = 24;
        }


        /**
         * An object that displays a word
         */
        private class WordLabel extends Region {

            private final String word;
            private final static double TILE_GAP = 2;

            /**
             *
             */
            WordLabel(String word) {
                this.word = word;
                drawWord();
            }

            /**
             * Draws a word, showing blanks in red and regular tiles in black.
             */
            private void drawWord() {
                getChildren().clear();
                int x = 0;

                for (Character tile : word.toCharArray()) {

                    Rectangle rect = new Rectangle(x, 0, tileWidth, tileHeight);
                    rect.setArcWidth(2);
                    rect.setArcHeight(2);
                    rect.setFill(Color.YELLOW);

                    Text text = new Text(x + 1, tileHeight - 3, String.valueOf(Character.toUpperCase(tile)));
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
     * Displays the provided game position: time remaining, the tile pool, the players, and their words.
     * If the WordDisplay is visible retrieves possible plays from the server.
     *
     * @param tokens the current game state, an array e.g.
     *               ["257", "YUIFOTMR", "GrubbTime", "[HAUYNES]", "Robot-Genius", "[BLEWARTS,POTJIES]"]
     *               consisting of the remaining time, the tile pools, a list of players (even indices)
     *               and their words (odd indices)
     */
    void showPosition(String[] tokens) {
        for (int i = 0; i < 3; i++) {
            gameGrid.getColumnConstraints().get(i).setMinWidth(minPanelWidth);
        }

        for (GamePanel panel : gamePanels)
            panel.reset();

        notificationArea.setText("Time remaining " + tokens[0]);

        if (tokens.length > 2) {
            for (int i = 2; i < tokens.length; i += 2) {
                String playerName = tokens[i];
                GamePanel panel = addPlayer(playerName);
                String[] words = tokens[i + 1].substring(1, tokens[i + 1].length() - 1).split(",");
                panel.addWords(words);
            }
        }

        setTiles(tokens[1]);
    }

    /**
     * Create an animation (offscreen) and play it. Screen capture each frame,
     * save the resulting images in gif format, and download it.
     */
    public void animate() throws IOException {
        final Duration frameDuration = Duration.millis(1000);
        Timeline animation = new Timeline(20);

        File tempFile = new File(gameID + ".gif");

        ImageOutputStream output = new FileImageOutputStream(tempFile);
        writer = new GifSequenceWriter(output, frameDuration, true);

        animation.getKeyFrames().add(new KeyFrame(Duration.ZERO, e -> {
            showPosition(gameLog.get(gameLog.size() - 1)); //show final frame first
            try {
                capture();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }));

        for (int i = 0; i < gameLog.size(); i++) {
            final int finalI = i;
            animation.getKeyFrames().add(new KeyFrame(frameDuration.multiply(i + 1), e -> {
                showPosition(gameLog.get(finalI));

                progress.set((double) finalI / gameLog.size());

                try {
                    capture();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }));
        }

        animation.playFromStart();

        animation.setOnFinished(event -> {
            try {
                finished.set(true);
                System.out.println("FINISHED!");
                writer.close();
                output.close();
                client.getWebAPI().downloadURL(tempFile.toURI().toURL());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    /**
     *
     */
    void capture() throws IOException {

        WritableImage fxImage = new WritableImage((int)getPrefWidth(), (int)getPrefHeight());

        snapshot(null, fxImage);
        BufferedImage image = SwingFXUtils.fromFXImage(fxImage, null);

        // Remove alpha-channel from buffered image:
        BufferedImage imageRGB = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.OPAQUE);
        Graphics2D graphics = imageRGB.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        writer.writeToSequence(imageRGB);
    }
}
