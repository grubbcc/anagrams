package client;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.Event;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import one.jpro.sound.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A front end user interface for playing, watching, or analyzing a game.
 *
 */

public class GameWindow extends PopWindow {

    private final HBox controlPanel = new HBox();
    private final Label notificationArea = new Label("");
    private final Button exitGameButton = new Button("Exit Game");
    private final TextField textField = new TextField();
    private final StackPane textStack = new StackPane();
    private final Label infoPane = new Label();

    private final TextArea chatBox = new TextArea();
    private final Button backToStartButton = new Button("|<");
    private final Button backTenButton = new Button("<<");
    private final Button backButton = new Button("<");
    private final Button showPlaysButton = new Button("Show plays");
    private final Button forwardButton = new Button(">");
    private final Button forwardTenButton = new Button(">>");
    private final Button forwardToEndButton = new Button(">|");
    private final Button hideButton = new Button("show chat \u25B2"); // up

    private final LinkedHashSet<GamePanel> gamePanels = new LinkedHashSet<>();
    private final GridPane gameGrid = new GridPane();
    private final BorderPane borderPane = new BorderPane();
    private final SplitPane splitPane = new SplitPane();
    private final GamePanel homePanel = new GamePanel();
    private final WordExplorer explorer;
    private final TilePanel tilePanel = new TilePanel();
    private final String wordSound = getClass().getResource("/steal sound.wav").toExternalForm();
    private final AudioClip wordClip;
    private final Image blackRobot = new Image(getClass().getResourceAsStream("/black robot.png"));
    private final Image whiteRobot = new Image(getClass().getResourceAsStream("/white robot.png"));
    private final ImageView robotImage = new ImageView(blackRobot);
    final WordDisplay wordDisplay;

    private final boolean isMobile;
    private final double minPanelWidth;
    private final double minPanelHeight;
    private final AnagramsClient client;
    final String gameID;
    private final String username;
    public final boolean isWatcher;
    public final boolean allowsChat;
    private final int minLength;
    private final int blankPenalty;
    private String tilePool = "";
    private final HashMap<String, GamePanel> players = new HashMap<>();
    boolean gameOver = false;

    //fields for postgame analysis
    ArrayList<String[]> gameLog;
    private int position;
    private int maxPosition;

    /**
     *
     */

    GameWindow(AnagramsClient client, String gameID, String username, String minLength, String blankPenalty, boolean allowsChat, String lexicon, ArrayList<String[]> gameLog, boolean isWatcher) {
        super(client.anchor);

        this.client = client;
        explorer = client.explorer;
        explorer.setLexicon(lexicon);
        this.gameID = gameID;
        this.username = username;
        this.minLength = Integer.parseInt(minLength);
        this.blankPenalty = Integer.parseInt(blankPenalty);
        this.gameLog = gameLog;
        this.allowsChat = allowsChat;
        this.isWatcher = isWatcher;

        isMobile = client.getWebAPI().isMobile();
        minPanelWidth = isMobile ? 75 : 175;
        minPanelHeight = isMobile ? 100 : 175;

        wordClip = AudioClip.getAudioClip(wordSound, client.stage);
        client.gameWindows.put(gameID, this);
        wordDisplay = new WordDisplay();

        //title bar
        setTitle("Game " + gameID);
        makeMaximizable();

        //control panel
        controlPanel.setId("control-panel");
        if(isMobile) controlPanel.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
        controlPanel.setSpacing(isMobile ? 5 : 20);
        exitGameButton.setOnAction(e -> exitGame());
        notificationArea.setFont(Font.font("Arial", FontWeight.BOLD, isMobile ? 15 : 13));
        infoPane.setFont(Font.font("Arial", FontWeight.BOLD, isMobile ? 15 : 13));
        infoPane.setText(lexicon + (isMobile ? "" : "      Minimum length = " + minLength));
        controlPanel.getChildren().addAll(notificationArea, exitGameButton, infoPane);

        //game panels
        gameGrid.setPadding(new Insets(3));
        gameGrid.setHgap(3);
        gameGrid.setVgap(3);

        ColumnConstraints col1 = new ColumnConstraints(minPanelWidth, 326, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col2 = new ColumnConstraints(minPanelWidth, 326, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col3 = new ColumnConstraints(minPanelWidth, 326, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);

        RowConstraints row1 = new RowConstraints(); row1.setPercentHeight(36); row1.setFillHeight(true);
        RowConstraints row2 = new RowConstraints(); row2.setPercentHeight(36); row2.setFillHeight(true);
        RowConstraints row3 = new RowConstraints(); row3.setPercentHeight(28); row3.setFillHeight(true);

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
        borderPane.setTop(controlPanel);
        borderPane.setCenter(gameGrid);
        borderPane.setId("game-background");
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.getItems().add(borderPane);
        splitPane.addEventFilter(MouseEvent.ANY, event -> {
            if (event.getTarget() instanceof Styleable) {
                if (!((Styleable) event.getTarget()).getStyleClass().contains("split-pane-divider")) {
                    Event.fireEvent(this, event);
                }
            }
        });

        //chat panel
        BorderPane chatPanel = new BorderPane();
        if (allowsChat) {
            ScrollPane chatScrollPane = new ScrollPane();
            chatBox.setEditable(false);
            TextField chatField = new TextField();
            chatField.setPromptText("Type here to chat");
            chatField.getProperties().put("vkType", "text");
            chatField.setOnAction(ae -> {
                client.send("gamechat " + gameID + " " + username + ": " + chatField.getText());
                chatField.clear();
            });
            chatScrollPane.setFitToHeight(true);
            chatScrollPane.setFitToWidth(true);
            chatScrollPane.setContent(chatBox);
            chatPanel.setMaxHeight(100);
            chatPanel.setMinHeight(0);
            chatPanel.setCenter(chatScrollPane);
            chatPanel.setBottom(chatField);
            splitPane.getItems().add(chatPanel);
            splitPane.setDividerPosition(0, 0.85);

            if(isMobile) {
                splitPane.setDividerPosition(0, 1.0);
                hideButton.setPrefWidth(80);
                hideButton.setAlignment(Pos.CENTER_RIGHT);
                hideButton.setPadding(Insets.EMPTY);
                hideButton.setFocusTraversable(false);
                hideButton.translateXProperty().bind(translateXProperty().add(widthProperty()).subtract(89));
                hideButton.translateYProperty().bind(translateYProperty().add(heightProperty()).subtract(28));
                hideButton.setOnAction(e -> {
                    if(hideButton.getText().equals("show chat \u25B2")) { //up
                        splitPane.setDividerPosition(0, 0.0);
                        hideButton.setText("hide chat \u25BC"); //down
                        hideButton.translateYProperty().bind(translateYProperty().add(heightProperty()).subtract(chatPanel.heightProperty()).subtract(12));
                    }
                    else {
                        splitPane.setDividerPosition(0, 1.0);
                        hideButton.setText("show chat \u25B2"); //up
                        hideButton.translateYProperty().bind(translateYProperty().add(heightProperty()).subtract(28));
                    }
                });
                client.anchor.getChildren().add(hideButton);
                Platform.runLater(hideButton::toFront);
            }
        }

        this.getStylesheets().add(getClass().getResource("/anagrams.css").toExternalForm());
        setContents(splitPane);

        if(isMobile) {
            setMovable(false);
            title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            maximizeButton.setScaleX(1.45); maximizeButton.setScaleY(1.45);
            closeButton.setScaleX(1.45); closeButton.setScaleY(1.45);

            controlPanel.getChildren().remove(exitGameButton);
            minWidthProperty().bind(client.stage.widthProperty());
            maxWidthProperty().bind(client.stage.widthProperty());
            minHeightProperty().bind(client.stage.heightProperty());
            maxHeightProperty().bind(client.stage.heightProperty());
            maximizeButton.setOnAction(e -> {
                client.getWebAPI().executeScript("toggleFullscreen();");
                if(isMaximized) {
                    maximizeButton.setGraphic(maximizeButton.maximizeIcon);
                    isMaximized = false;
                }
                else {
                    maximizeButton.setGraphic(maximizeButton.unmaximizeIcon);
                    isMaximized = true;
                }
            });
        }
        else {
            setPrefSize(1000, 674);
            textField.setPrefWidth(310);
            makeResizable();
            maximizeButton.setOnAction(e -> {
                maximizeButton.maximizeAction.handle(e);
                Platform.runLater(() -> {
                    setTiles(tilePool);
                    for (GamePanel gamePanel : gamePanels) {
                        if (!gamePanel.getWords().isEmpty()) {
                            gamePanel.addWords(gamePanel.getWords().toArray(new String[0]));
                        }
                    }
                });
                allocateSpace();
                textField.requestFocus();
            });
        }

        if (!isWatcher) {
            homePanel.takeSeat(username);

            textField.setOnAction(e -> {
                makePlay(textField.getText().toUpperCase());
                textField.clear();
            });
            textField.setPromptText("Enter a word here to play");
            textField.getProperties().put("vkType", "text");

            Pane wordBuilder = new Pane();
            wordBuilder.visibleProperty().bind(textField.focusedProperty());
            wordBuilder.setPrefWidth(310);
            wordBuilder.setPrefHeight(33);
            wordBuilder.setMaxWidth(310);
            wordBuilder.setMaxHeight(33);

            wordBuilder.setBackground(new Background(new BackgroundFill(Color.web("#E5E5E5"), CornerRadii.EMPTY, Insets.EMPTY)));
            wordBuilder.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));
/****************************************************************************************************************/
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("[a-zA-Z]*") || newValue.length() > 15) {
                    Platform.runLater(() -> textField.setText(oldValue));
                    return;
                }

                String entry = textField.textProperty().get().toUpperCase();
                String tiles;

                LinkedHashSet<String> words = new LinkedHashSet<>();
                words.add("");
                for(GamePanel gamePanel : gamePanels)
                    words.addAll(gamePanel.getWords());

                int fewestBlanks = Integer.MAX_VALUE;
                String bestSteal = "";
                String bestWord = "";

                //If the tilePool does not contain a letter from the entry, a blank must be used
                for(String word : words) {
                    int blanksRequired = 0;
                    tiles = tilePool.concat(word.toUpperCase());
                    String newWord = "";

                    for (String s : entry.split("")) {
                        if (tiles.contains(s)) {
                            tiles = tiles.replaceFirst(s, "");
                            newWord = newWord.concat(s);
                        }
                        else {
                            blanksRequired++;
                            newWord = newWord.concat(s.toLowerCase());
                        }
                    }

                    if(blanksRequired == 0) { //best possible score
                        bestWord = newWord;
                        bestSteal = word;
                        break;
                    }
                    else if(blanksRequired < fewestBlanks) {
                        bestWord = newWord;
                        fewestBlanks = blanksRequired;
                        bestSteal = word;
                    }
                }

                wordBuilder.getChildren().clear();

                int x = 5;
                for (Character tile : bestWord.toCharArray()) {
                    Rectangle rect = new Rectangle(x,5, 18, 22);
                    rect.setArcWidth(2);
                    rect.setArcHeight(2);
                    rect.setFill(Color.YELLOW);

                    Text text = new Text(x + 1, 26, String.valueOf(Character.toUpperCase(tile)));
                    text.setFont(Font.font("Monospaced", FontWeight.BOLD, 28));
                    text.setFill(Character.isLowerCase(tile) ? Color.RED : Color.BLACK);

                    wordBuilder.getChildren().addAll(rect, text);
                    x += 20;
                }

                if(entry.length() < GameWindow.this.minLength) {
                    wordBuilder.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));
                }
                else if(attemptMakeWord(entry)) {
                    wordBuilder.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));
                }
                else if(attemptSteal(bestSteal, entry)) {
                    wordBuilder.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));
                }
                else {
                    wordBuilder.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));
                }

            });
            if(isMobile) {
                StackPane.setAlignment(wordBuilder, Pos.BOTTOM_CENTER);
                controlPanel.getChildren().remove(infoPane);
                controlPanel.getChildren().addAll(textField, infoPane);
                client.stack.getChildren().add(wordBuilder);
            }
            else {
                textStack.getChildren().addAll(textField, wordBuilder);
                controlPanel.getChildren().remove(infoPane);
                controlPanel.getChildren().addAll(textStack, infoPane);
            }
        }

        setOnMouseClicked(e -> {
            if(e.getButton() == MouseButton.PRIMARY) {
                if (e.getClickCount() == 2) {
                    maximizeButton.fire();
                }
            }
        });

        if (!gameLog.isEmpty()) {
            endGame();
        }

        client.setColors();
        Platform.runLater(() -> setTiles(tilePool)); // needs testing
        show(false);
        textField.requestFocus();
        closeButton.setOnAction(e -> exitGame());
        gameGrid.layout();

    }

    /**
     *
     */

    void exitGame() {
        hide();
        wordDisplay.hide();
        explorer.hide();
        client.anchor.getChildren().remove(hideButton);
        client.exitGame(gameID, isWatcher);
    }

    /**
     * Sets the robot icon to be visible against the background.
     *
     * @param dark whether the game foreground has a luminance < 40.
     */

    public void setDark(Boolean dark) {
        if(dark)
            robotImage.setImage(whiteRobot);
        else
            robotImage.setImage(blackRobot);
    }


    /**
     *
     */

    void allocateSpace() {

        double remainingSpace = getWidth() - 10;
        for(int i = 0; i < 3; i++) {
            remainingSpace -= gameGrid.getColumnConstraints().get(i).getMinWidth();
        }
        for(int i = 0; i < 3; i++) {
            ColumnConstraints currentColumn = gameGrid.getColumnConstraints().get(i);
            double allocatedWidth = currentColumn.getMinWidth();
            currentColumn.setPrefWidth(allocatedWidth + remainingSpace/3);
        }
        Platform.runLater(() -> setTiles(tilePool));
    }

    /**
     * A panel for displaying the tilePool
     */

    class TilePanel extends Pane {

        /**
         *
         */

        public TilePanel() {
            setId("tile-panel");
            setMinSize(minPanelWidth, minPanelHeight);

            widthProperty().addListener((obs, oldVal, newVal) -> setTiles(tilePool));
            heightProperty().addListener((obs, oldVal, newVal) -> setTiles(tilePool));
        }

        /**
         * Draws the tiles in a spiral pattern
         */

        public void showTiles() {

            getChildren().clear();

            for (int i = 1; i <= tilePool.length(); i++) {
                double x = getWidth()/2 + 16*Math.sqrt(i) * Math.cos(Math.sqrt(i) * Math.PI * 4 / 3);
                double y = 3 + getHeight()/2 + 16*Math.sqrt(i) * Math.sin(Math.sqrt(i) * Math.PI * 4 / 3);

                Rectangle rect = new Rectangle(x - 1, y - 21, 20, 21);
                rect.setArcWidth(2);
                rect.setArcHeight(2);
                rect.setFill(Color.YELLOW);

                Text text = new Text(x, y + 1, tilePool.charAt(i - 1) + "");
                text.setFont(Font.font("Monospaced", FontWeight.BOLD, 28));

                getChildren().addAll(rect, text);
            }
            if(!getChildren().isEmpty()) {
                getChildren().get(0).setTranslateY(10);
                getChildren().get(1).setTranslateY(10);
            }
        }

    }

    /**
     * A panel for displaying the name, score, and words possessed by a player.
     */

    class GamePanel extends BorderPane {

        String playerName = null;
        private final HBox infoPane = new HBox();
        private final FlowPane wordPane = new FlowPane();
        private final ScrollPane scrollPane = new ScrollPane(wordPane);
        private final Label playerNameLabel = new Label();
        private final Label playerScoreLabel = new Label();

        private final SimpleBooleanProperty savingSpace = new SimpleBooleanProperty(false);

        int column = -1;
        private int tileWidth;
        private int tileHeight;
        private int tileFontSize;

        boolean isOccupied = false;
        boolean isAvailable = true;
        final LinkedHashMap<String, WordLabel> words = new LinkedHashMap<>();
        private double prevWidth;
        private double prevHeight;
        int score = 0;

        /**
         * An empty placeholder gamePanel
         */

        GamePanel() {

            setId("game-panel");
            gamePanels.add(this);

            infoPane.getChildren().add(playerNameLabel);
            infoPane.getChildren().add(playerScoreLabel);

            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.prefViewportWidthProperty().bind(widthProperty());
            scrollPane.prefViewportHeightProperty().bind(heightProperty());

            wordPane.setAlignment(Pos.TOP_CENTER);
            wordPane.hgapProperty().bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 6 : 12, savingSpace));
            wordPane.vgapProperty().bind(Bindings.createIntegerBinding(() -> savingSpace.get() ? 2 : 6, savingSpace));

            setTop(infoPane);
            setCenter(scrollPane);

            makeBig();
        }

        /**
         *
         */

        GamePanel(int column) {
            this();
            this.column = column;
            if(column >= 0) {
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            }
        }

        /**
         * Puts the given player's name on this panel and displays their score.
         *
         * @param newPlayer The name of the player to be added.
         */

        public GamePanel takeSeat(String newPlayer) {
            pseudoClassStateChanged(PseudoClass.getPseudoClass("abandoned"), false);
            this.playerName = newPlayer;
            players.put(newPlayer, this);
            playerNameLabel.setText(playerName);

            if(playerName.startsWith("Robot")) {
                playerNameLabel.setGraphic(robotImage);
            }
            if (words.isEmpty() && !gameOver) {
                playerScoreLabel.setText("0");
            }
            isOccupied = true;
            isAvailable = false;

            return this;
        }

        /**
         * The player has left this seat. If they have any words, they remain.
         * Otherwise, the seat becomes available for another player.
         */

        public void abandonSeat() {
            isOccupied = false;
            if(column >= 0) {
                pseudoClassStateChanged(PseudoClass.getPseudoClass("abandoned"), true);
            }
            if (words.isEmpty()) {
                makeAvailable();
            }
        }

        /**
         * The seat is empty and contains no words, so a new player may occupy it.
         */

        private void makeAvailable() {
            isAvailable = true;
            playerNameLabel.setText("");
            playerScoreLabel.setText("");
            players.remove(playerName);
            playerName = null;
        }

        /**
         * Removes the occupant, as well as any words they may have, from this pane.
         * Used only during endgame analysis.
         */

        private void reset() {
            wordPane.getChildren().clear();
            playerNameLabel.setGraphic(null);
            words.clear();
            //    makeBig();
            abandonSeat();
        }

        /**
         * Add a new word to the player's collection and recalculate their score.
         *
         * @param newWord The word to be removed.
         */

        void addWord(String newWord) {
            int PADDING = 4;
            double paneWidth = getWidth();
            double paneHeight = getHeight() - infoPane.getHeight();

            if(prevHeight + wordPane.getHgap() + tileHeight + PADDING > paneHeight) {
                double width = newWord.length() * tileWidth + (newWord.length() - 1) * WordLabel.TILE_GAP + 1 + PADDING;
                if (prevWidth + (wordPane.getVgap() + width + PADDING)/2 > paneWidth) {
                    if (!savingSpace.get()) {
                        makeSmall();
                        wordPane.getChildren().clear();
                        for (String word : words.keySet()) {
                            WordLabel newLabel = new WordLabel(word);
                            wordPane.getChildren().add(newLabel);
                            words.put(word, newLabel);
                        }
                    }
                    else if(column >= 0) {
                        gameGrid.getColumnConstraints().get(column).setMinWidth(paneWidth * 1.15);
                    }
                }
            }
            WordLabel newWordLabel = new WordLabel(newWord);
            wordPane.getChildren().add(newWordLabel);
            wordPane.layout();
            prevWidth = newWordLabel.getBoundsInParent().getMaxX();
            prevHeight = newWordLabel.getBoundsInParent().getMaxY();
            words.put(newWord, newWordLabel);
            score += newWord.length() * newWord.length();
            playerScoreLabel.setText(score + "");
        }


        /**
         * Adds a bunch of words all at once. Used during analysis, when resizing the window,
         * and when joining or starting a game.
         */

        void addWords(String[] wordsToAdd) {
            double paneWidth = (gameGrid.getWidth() - 10)/3;
            if(column < 0) paneWidth = getWidth();
            double paneHeight = getHeight() - infoPane.getHeight();
            makeBig();
            if(column >= 0) {
                gameGrid.getColumnConstraints().get(column).setMinWidth(minPanelWidth);
            }

            if(isMobile) {
                //check if any individual words are too large to fit using large tiles
                for (String word : wordsToAdd) {
                    double width = word.length() * 16 + (word.length() - 1) * WordLabel.TILE_GAP + 1;
                    if (width > paneWidth) {
                        makeSmall();
                        gameGrid.getColumnConstraints().get(column).setMinWidth(word.length() * 12 + (word.length() - 1) * WordLabel.TILE_GAP + 1);
                        break;
                    }
                }
            }

            //check if panel is large enough for adding large tiles
            if (34 * wordsToAdd.length - 4 <= paneHeight) {
                score = 0;
                wordPane.getChildren().clear();
                for (String word : wordsToAdd) {
                    WordLabel newWordLabel = new WordLabel(word);
                    wordPane.getChildren().add(newWordLabel);
                    words.put(word, newWordLabel);
                    score += word.length() * word.length();
                }
                playerScoreLabel.setText(score + "");
                return;
            }

            //check if there is room for large tiles
            double hgap = 12;
            double vgap = 6;
            double x = -1 * hgap / 2 + paneWidth / 2;
            double PADDING = 4;
            double y = 24 + PADDING;

            for (String word : wordsToAdd) {
                double width = word.length() * 16 + (word.length() - 1) * WordLabel.TILE_GAP + 1;
                x += (width + hgap + PADDING) / 2;
                if (x > paneWidth) {
                    x = width / 2 + paneWidth / 2;
                    y += 24 + vgap + PADDING;
                }
            }
            if (y <= paneHeight) {
                score = 0;
                wordPane.getChildren().clear();
                for (String word : wordsToAdd) {
                    WordLabel newWordLabel = new WordLabel(word);
                    wordPane.getChildren().add(newWordLabel);
                    words.put(word, newWordLabel);
                    score += word.length() * word.length();
                }
                playerScoreLabel.setText(score + "");
                return;
            }

            //room is sufficient to add small tiles
            hgap = 6;
            vgap = 2;
            makeSmall();
            if (23 * wordsToAdd.length - 2 <= paneHeight || column < 0) {
                score = 0;
                wordPane.getChildren().clear();
                for (String word : wordsToAdd) {
                    WordLabel newWordLabel = new WordLabel(word);
                    wordPane.getChildren().add(newWordLabel);
                    words.put(word, newWordLabel);
                    score += word.length() * word.length();
                }
                playerScoreLabel.setText(score + "");
                return;
            }

            //widen the pane until there is room for small tiles
            outer: while (true) {
                x = -1 * hgap / 2 + paneWidth / 2;
                y = 17 + PADDING;
                for (String word : wordsToAdd) {
                    double width = word.length() * 12 + (word.length() - 1) * WordLabel.TILE_GAP + 1;
                    x += (width + hgap + PADDING) / 2;
                    if (x > paneWidth) {
                        x = width / 2 + paneWidth / 2;
                        y += 17 + vgap + PADDING;
                    }

                    if (y > paneHeight) {
                        paneWidth *= 1.15;
                        gameGrid.getColumnConstraints().get(column).setMinWidth(paneWidth);
                        continue outer;
                    }
                }
                GameWindow.this.setPrefWidth(Math.max(1000, paneWidth + 366)); //actually sets the minimum width
                if(!isMobile)
                    GameWindow.this.setMinWidth(Math.max(GameWindow.this.getWidth(), paneWidth + 366)); //actually sets the current width

                allocateSpace();
                score = 0;
                wordPane.getChildren().clear();
                for (String word : wordsToAdd) {
                    WordLabel newWordLabel = new WordLabel(word);
                    wordPane.getChildren().add(newWordLabel);
                    words.put(word, newWordLabel);
                    score += word.length() * word.length();
                }
                playerScoreLabel.setText(score + "");
                return;
            }
        }

        /**
         * Remove a word from the player's collection and recalculate their score.
         * If the player has left the table and has no words, the seat is opened up for
         * another player.
         *
         * @param wordToRemove The word to be removed.
         */

        void removeWord(String wordToRemove) {
            wordPane.getChildren().remove(words.remove(wordToRemove));
            score -= wordToRemove.length() * wordToRemove.length();
            playerScoreLabel.setText(score + "");
        }


        /**
         *
         */

        void makeSmall() {
            savingSpace.set(true);
            tileWidth = 12;
            tileHeight = 16;
            tileFontSize = 18;
        }

        /**
         *
         */

        void makeBig() {
            savingSpace.set(false);
            tileWidth = 16;
            tileHeight = 20;
            tileFontSize = 24;
        }

        /**
         * Returns the words at this panel
         */

        Set<String> getWords() {
            return words.keySet();
        }

        /**
         * A clickable object that displays a word
         */

        class WordLabel extends Region {

            final String word;
            final static double TILE_GAP = 2;
            double width;

            /**
             *
             */

            WordLabel(String word) {
                this.word = word;
                width = word.length()*tileWidth + (word.length() - 1)*TILE_GAP;
                drawWord();

                setOnMouseClicked(event -> { ;
                    if(gameOver || isWatcher) {
                        if(!explorer.isVisible()) {
                            explorer.show(false);
                        }
                        explorer.lookUp(word);
                    }
                });
            }

            /**
             * Draws a word, showing blanks in red and regular tiles in black.
             */

            void drawWord() {
                getChildren().clear();
                int x = 0;

                for (Character tile : word.toCharArray()) {

                    Rectangle rect = new Rectangle(x,0, tileWidth, tileHeight);
                    rect.setArcWidth(2);
                    rect.setArcHeight(2);
                    rect.setFill(Color.YELLOW);

                    Text text = new Text(x + 1, tileHeight - 1, String.valueOf(Character.toUpperCase(tile)));
                    text.setFont(Font.font("Monospaced", FontWeight.BOLD, tileFontSize));
                    text.setFill(Character.isLowerCase(tile) ? Color.RED : Color.BLACK);

                    getChildren().addAll(rect, text);
                    x += tileWidth + TILE_GAP;
                }
            }
        }
    }


    /**
     * Displays messages from the server such as the time remaining.
     *
     * @param nextMessage The message to be displayed.
     */

    public void setNotificationArea(String nextMessage) {
        notificationArea.setText(nextMessage);
    }

    /**
     * Updates the tilePool
     *
     * @param nextTiles The new contents of the tilePool.
     */

    public void setTiles(String nextTiles) {
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
     *         none is available
     */

    public GamePanel addPlayer(String newPlayerName) {

        //current player is assigned homePanel
        if (newPlayerName.equals(username)) {
            return homePanel.takeSeat(newPlayerName);
        }

        //player reenters game after leaving
        else if(players.containsKey(newPlayerName)) {
            return players.get(newPlayerName).takeSeat(newPlayerName);
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
     * Removes the named player from their current seat. Their words, if any, remain to be
     * reclaimed if the player returns.
     *
     * @param playerToRemove The name of the player to remove.
     */

    public void removePlayer(String playerToRemove) {
        if (players.containsKey(playerToRemove)) {
            players.get(playerToRemove).abandonSeat();
        }
    }

    /**
     * Method used during gameplay for adding words and updating the tilePool. Plays a sound.
     */

    public void makeWord(String playerName, String wordToAdd, String nextTiles) {
        players.get(playerName).addWord(wordToAdd);

        if (client.prefs.getBoolean("play_sounds", true))
            wordClip.play();

        setTiles(nextTiles);
    }


    /**
     * Removes the given word from the given player from whom it has been stolen.
     *
     * @param playerName   the player whose word has been stolen
     * @param wordToRemove the stolen word
     */

    public void removeWord(String playerName, String wordToRemove) {
        players.get(playerName).removeWord(wordToRemove);
    }

    /**
     *
     */

    public void doSteal(String shortPlayer, String shortWord, String longPlayer, String longWord, String nextTiles) {
        removeWord(shortPlayer, shortWord);
        makeWord(longPlayer, longWord, nextTiles);
    }


    /**
     * Displays the new chat message (and sender) in the chat box and automatically scrolls to view
     *
     * @param msg The message to display.
     */

    public void handleChat(String msg) {
        if(chatBox.getText().isEmpty())
            chatBox.appendText(msg);
        else
            chatBox.appendText("\n" + msg);
    }

    /**
     *
     */

    public void endGame() {
        gameOver = true;

        maxPosition = gameLog.size() - 1;
        position = maxPosition;

        controlPanel.getChildren().removeAll(textStack, textField, infoPane);
        controlPanel.getChildren().addAll(backToStartButton, backTenButton, backButton, showPlaysButton, forwardButton, forwardTenButton, forwardToEndButton, infoPane);
        backToStartButton.setPrefWidth(25); backTenButton.setPrefWidth(25); backButton.setPrefWidth(25);
        forwardButton.setPrefWidth(25); forwardTenButton.setPrefWidth(25); forwardToEndButton.setPrefWidth(25);
        showPlaysButton.textProperty().bind(Bindings.createStringBinding(() -> wordDisplay.visibleProperty().get() ? "Hide plays" : "Show plays", wordDisplay.visibleProperty()));
        showPlaysButton.setOnAction(e -> {
            if(wordDisplay.isVisible()) {
                wordDisplay.hide();
            }
            else {
                wordDisplay.show(false);
                client.send("findplays " + gameID + " " + position);
            }
        });

        backToStartButton.setOnAction(e -> {position = 0; showPosition(gameLog.get(position));});
        backTenButton.setOnAction(e -> {position = Math.max(position - 10, 0); showPosition(gameLog.get(position));});
        backButton.setOnAction(e -> {position = Math.max(position - 1, 0); showPosition(gameLog.get(position));});
        forwardButton.setOnAction(e -> {position = Math.min(position + 1, maxPosition); showPosition(gameLog.get(position));});
        forwardTenButton.setOnAction(e -> {position = Math.min(position + 10, maxPosition); showPosition(gameLog.get(position));});
        forwardToEndButton.setOnAction(e -> {position = maxPosition; showPosition(gameLog.get(position));});

        Platform.runLater(() -> showPosition(gameLog.get(position)));

    }

    /**
     *
     */

    void showPosition(String[] tokens) {

        for (GamePanel panel : gamePanels)
            panel.reset();

        notificationArea.setText("Time remaining " + tokens[0]);
        tilePool = tokens[1];
        setTiles(tilePool);

        if (tokens.length > 2) {
            for (int i = 2; i < tokens.length; i += 2) {
                String playerName = tokens[i];
                GamePanel panel = addPlayer(playerName);
                String[] words = tokens[i + 1].substring(1, tokens[i + 1].length() - 1).split(",");
                panel.addWords(words);
            }
        }
        allocateSpace();

        if (wordDisplay.isVisible()) {
            client.send("findplays " + gameID + " " + position);
        }
    }

    /**
     *
     */

    void showPlays(String plays) {

        Matcher m = Pattern.compile("\\[(.*?)]").matcher(plays);
        m.find();
        String[] wordsInPool = m.group(1).split(",");
        m.find();
        String[] possibleSteals = m.group(1).split(",");

        wordDisplay.setWords(wordsInPool, possibleSteals);
    }

    /**
     * Shows possible plays during postgame analysis
     */

    public class WordDisplay extends PopWindow {

        private final GamePanel poolPanel = new GamePanel();
        private final GamePanel stealsPanel = new GamePanel();

        /**
         *
         */

        public WordDisplay() {
            super(client.anchor);
            setViewOrder(Double.NEGATIVE_INFINITY);
            AnchorPane.setRightAnchor(this, 0.0);
            AnchorPane.setTopAnchor(this, 80.0);

            poolPanel.infoPane.getChildren().remove(poolPanel.playerScoreLabel);
            stealsPanel.infoPane.getChildren().remove(stealsPanel.playerScoreLabel);

            poolPanel.scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            stealsPanel.scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

            GridPane displayGrid = new GridPane();
            displayGrid.setPadding(new Insets(3));
            displayGrid.setVgap(3);
            ColumnConstraints col1 = new ColumnConstraints(175, 360, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);

            RowConstraints row1 = new RowConstraints(120, 171, Double.MAX_VALUE, Priority.SOMETIMES, VPos.CENTER, true);
            RowConstraints row2 = new RowConstraints(180, 213, Double.MAX_VALUE, Priority.SOMETIMES, VPos.CENTER, true);
            row1.setPercentHeight(44.5);

            displayGrid.getColumnConstraints().add(col1);
            displayGrid.getRowConstraints().addAll(row1, row2);

            BorderPane mainPanel = new BorderPane(displayGrid);
            mainPanel.setId("word-display");
            mainPanel.getStylesheets().add(getClass().getResource("/anagrams.css").toExternalForm());
            displayGrid.add(poolPanel, 0, 0);
            displayGrid.add(stealsPanel, 0, 1);
            displayGrid.addEventFilter(MouseEvent.ANY, event -> Event.fireEvent(this, event));

            setPrefSize(380, 450);
            setContents(mainPanel);
            setTitle("Possible plays");
            makeResizable();

        }

        /**
         *
         */

        public void setWords(String[] wordsInPool, String[] possibleSteals) {

            poolPanel.wordPane.getChildren().clear();
            stealsPanel.wordPane.getChildren().clear();
            poolPanel.playerNameLabel.setText("Pool");
            stealsPanel.playerNameLabel.setText("Steals");

            //pool
            if(!wordsInPool[0].isEmpty())
                poolPanel.addWords(wordsInPool);

            //steals
            if(!possibleSteals[0].isEmpty()) {
                Tooltip[] tooltips = new Tooltip[possibleSteals.length];
                for(int i = 0; i < possibleSteals.length; i++) {
                    String[] contents = possibleSteals[i].split(" -> ");
                    possibleSteals[i] = contents[1];
                    Tooltip tooltip = new Tooltip(contents[0]);
                    tooltip.setShowDelay(Duration.seconds(0.5));
                    tooltips[i] = tooltip;
                }
                stealsPanel.addWords(possibleSteals);
                int i = 0;
                for(GamePanel.WordLabel label : stealsPanel.words.values()) {
                    Tooltip.install(label, tooltips[i++]);
                }
            }
        }
    }


    /**
     * Responds to button clicks and textField entries. If the input is of sufficient length
     * and is not already on the board, a steal attempt is made. If no steal is possible,
     * it is attempted to build the word from the letters in the tilePool.
     *
     * @param input Letters entered by the player into the textField
     */

    private void makePlay(String input) {
        if(input.length() >= minLength) {
            for(Map.Entry<String, GamePanel> entry : players.entrySet()) {
                String player = entry.getKey();
                for(String shortWord : entry.getValue().getWords()) {
                    if(shortWord.equalsIgnoreCase(input)) {
                        return; //word is already on the board
                    }
                    else if(input.length() > shortWord.length()) {
                        if(attemptSteal(shortWord, input)) {
                            client.send("steal " + gameID + " " + player + " " + shortWord + " " + username + " " + input);
                            return;
                        }
                    }
                }
            }
            if(attemptMakeWord(input)) {
                client.send("makeword " + gameID + " " + username + " " + input);
            }
        }
    }

    /**
     * Given a shortWord, determines whether a longer word can be constructed from the short word. If so,
     * the method returns true and an instruction is sent to the server to remove the shortWord from the
     * given opponent and reward the current player with the longWord. Otherwise returns false.
     *
     *@param shortWord The word that we are attempting to steal
     *@param longWord The word which may or not be a valid steal of the shortWord
     */

    private boolean attemptSteal(String shortWord, String longWord) {

        // charsToFind contains the letters that cannot be found in the existing word;
        // they must be taken from the pool or a blank must be redesignated.
        String charsToFind = longWord;

        int blanksAvailable = tilePool.length() - tilePool.replace("?", "").length();
        int blanksToChange = 0;
        int blanksToTake = 0;

        //lowercase is used to represent blanks
        //If the shortWord contains a tile not found in the longWord, it cannot be stolen unless that tile is a blank
        for(String s : shortWord.split("")) {

            if(charsToFind.contains(s.toUpperCase()))
                charsToFind = charsToFind.replaceFirst(s.toUpperCase(), "");
            else {
                if(Character.isLowerCase(s.charAt(0))) {
                    blanksToChange++;
                    blanksAvailable++;
                }
                else
                    return false;
            }
        }

        //The number of blanksToTake is the number of letters found in neither the shortWord nor the pool
        String tiles = tilePool;
        for(String s : charsToFind.split("")) {
            if(tiles.contains(s))
                tiles = tiles.replaceFirst(s, "");
            else
                blanksToTake++;
        }

        if(blanksAvailable < blanksToTake)
            return false;

        //Calculate how long the word needs to be, accounting for the blankPenalty
        int additionalTilesRequired = 1;
        if(blanksToTake > 0 || blanksToChange > 0)
            additionalTilesRequired = blankPenalty*blanksToChange + (blankPenalty + 1)*blanksToTake;

        if(longWord.length() - shortWord.length() < additionalTilesRequired)
            return false;

        if(isRearrangement(shortWord.toUpperCase(), longWord)) {
            //steal is successful
            return true;
        }
        else
            return false;
    }

    /**
     * Given a word, determines whether the appropriate tiles can be found in the pool. If so, true is returned
     * and an instruction is sent to the server that the tiles should be removed and the current player should
     * claim them.
     *
     * @param entry The word that was entered.
     */

    private boolean attemptMakeWord(String entry) {

        int blanksAvailable = tilePool.length() - tilePool.replace("?", "").length();
        int blanksRequired = 0;

        //If the tilePool does not contain a letter from the entry, a blank must be used
        String tiles = tilePool;
        for(String s : entry.split("")) {
            if(tiles.contains(s))
                tiles = tiles.replaceFirst(s, "");
            else
                blanksRequired++;
        }

        if(blanksAvailable < blanksRequired)
            return false; //not enough blanks in pool

        if(blanksRequired > 0)
            if(entry.length() - minLength < blanksRequired*(blankPenalty + 1))
                return false; //word not long enough

        //build is successful
        return true;

    }

    /**
     * Given two words, the shorter of which is a subset of the other, determines whether a rearrangement/permutation
     * of letters is necessary to form the longer.
     *
     * @param shortWord a short word
     * @param longWord a longer word
     */

    public static boolean isRearrangement(String shortWord, String longWord) {

        String shortString = shortWord;
        String longString = longWord;

        while(longString.length() >= shortString.length() && shortString.length() > 0) {
            if (shortString.charAt(0) == longString.charAt(0)) {
                shortString = shortString.substring(1);
            }
            longString = longString.substring(1);
        }

        return shortString.length() > longString.length();
    }
}
