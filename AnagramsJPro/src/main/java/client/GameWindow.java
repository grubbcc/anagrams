package client;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.Event;
import javafx.geometry.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
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
class GameWindow extends PopWindow {

    private final HBox controlPanel = new HBox();
    private final Label notificationArea = new Label("");
    private final Button exitGameButton = new Button("Exit Game");
    private final TextField textField = new TextField();
    private final Pane wordBuilder = new Pane();
    private final Line caret = new Line(0, 6, 0, 27);
    private final StackPane textStack = new StackPane();
    private final Label infoPane = new Label();
    private final MenuButton saveButton = new MenuButton("Download");

    private final TextArea chatBox = new TextArea();
    private final TextField chatField = new TextField();
    private final Button backToStartButton = new Button("|<");
    private final Button backTenButton = new Button("<<");
    private final Button backButton = new Button("<");
    private final Button showPlaysButton = new Button("Show plays");
    private final Button forwardButton = new Button(">");
    private final Button forwardTenButton = new Button(">>");
    private final Button forwardToEndButton = new Button(">|");
    private final Button hideButton = new Button("show chat \u25B2");

    private final GridPane gameGrid = new GridPane();
    private final BorderPane borderPane = new BorderPane();
    private final SplitPane splitPane = new SplitPane();
    private final GamePanel homePanel = new GamePanel();
    private final WordExplorer explorer;
    private final TilePanel tilePanel = new TilePanel();
    private final String wordSound = getClass().getResource("/sounds/steal sound.mp3").toExternalForm();
    private final AudioClip wordClip;
    private final Image blackRobot = new Image(getClass().getResourceAsStream("/images/black robot.png"));
    private final Image whiteRobot = new Image(getClass().getResourceAsStream("/images/white robot.png"));
    private final ImageView robotImage = new ImageView(blackRobot);
    private final Timeline blinker = new Timeline();
    final WordDisplay wordDisplay;

    private final boolean isMobile;
    private final double minPanelWidth;
    private final double minPanelHeight;
    private final AnagramsClient client;
    final String gameID;
    private final String username;
    final boolean isWatcher;
    final boolean allowsChat;
    final String lexicon;
    final int minLength;
    final int blankPenalty;
    final String numSets;
    final String speed;
    private String tilePool = "";
    boolean gameOver = false;

    private final HashMap<String, GamePanel> players = new HashMap<>();
    private final LinkedHashSet<GamePanel> gamePanels = new LinkedHashSet<>();

    //fields for postgame analysis
    ArrayList<String[]> gameLog;
    private int position;
    private int maxPosition;

    /**
     *
     */
    GameWindow(AnagramsClient client, String gameID, String gameName, String username, String minLength, int blankPenalty, String numSets, String speed, boolean allowsChat, String lexicon, ArrayList<String[]> gameLog, boolean isWatcher) {
        super(client.anchor);

        this.client = client;
        client.gameWindows.put(gameID, this);
        explorer = client.explorer;
        explorer.setLexicon(lexicon);
        this.lexicon = lexicon;
        this.gameID = gameID;
        this.username = username;
        this.minLength = Integer.parseInt(minLength);
        this.blankPenalty = blankPenalty;
        this.numSets = numSets;
        this.speed = speed;
        this.gameLog = gameLog;
        this.allowsChat = allowsChat;
        this.isWatcher = isWatcher;

        isMobile = client.getWebAPI().isMobile();
        minPanelWidth = isMobile ? 75 : 175;
        minPanelHeight = isMobile ? 100 : 175;

        wordClip = AudioClip.getAudioClip(wordSound, client.stage);
        wordDisplay = new WordDisplay();

        //title bar
        setTitle(gameName.replaceAll("%", " "));
        makeMaximizable();

        //control panel
        controlPanel.getStyleClass().add("control-panel");
        if (isMobile) controlPanel.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
        controlPanel.setSpacing(isMobile ? 5 : 20);
        exitGameButton.setOnAction(e -> exitGame());

        infoPane.setText(lexicon + (isMobile ? "" : "      Min length = " + minLength));
        controlPanel.getChildren().addAll(notificationArea, exitGameButton, infoPane);

        //game panels
        gameGrid.setPadding(new Insets(3));
        gameGrid.setHgap(3);
        gameGrid.setVgap(3);

        ColumnConstraints col1 = new ColumnConstraints(minPanelWidth, 326, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col2 = new ColumnConstraints(minPanelWidth, 326, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints col3 = new ColumnConstraints(minPanelWidth, 326, Double.MAX_VALUE, Priority.ALWAYS, HPos.CENTER, true);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(36);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(36);
        RowConstraints row3 = new RowConstraints();
        row3.setPercentHeight(28);

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
        borderPane.getStyleClass().add("game-background");
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
            chatBox.setEditable(false);
            chatBox.getStyleClass().add("game-chat");
            chatField.setPromptText("Type here to chat");
            chatField.setOnAction(ae -> {
                String msg = String.format("%1.500s", chatField.getText()); //truncate to 500 characters
                if(!msg.isBlank())
                    client.send("gamechat " + gameID + " " + username + ": " + chatField.getText());
                chatField.clear();
            });

            KeyCombination copyKey = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
            chatBox.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
                if(copyKey.match(keyEvent)) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(chatField.getSelectedText());
                    Clipboard.getSystemClipboard().setContent(content);
                }
            });
            chatPanel.setMaxHeight(100);
            chatPanel.setPrefHeight(100);
            chatPanel.setMinHeight(0);
            chatPanel.setCenter(chatBox);
            chatPanel.setBottom(chatField);
            chatBox.setEditable(false);
            chatBox.setStyle("-fx-font-size: " + (client.getWebAPI().isMobile() ? 18 : 16) + ";");
            chatField.setStyle("-fx-font-size: " + (client.getWebAPI().isMobile() ? 18 : 16) + ";");
            chatField.focusedProperty().addListener((focus, wasFocused, isFocused) -> {
                if(isFocused)
                    chatBox.scrollTopProperty().set(Double.MAX_VALUE);
            });
            chatBox.setWrapText(true);

            splitPane.getItems().add(chatPanel);
            splitPane.setDividerPosition(0, 0);
            if (isMobile) {
                splitPane.setDividerPosition(0, 1.0);
                hideButton.setPrefWidth(85);
                hideButton.setAlignment(Pos.CENTER_RIGHT);
                hideButton.setPadding(Insets.EMPTY);
                hideButton.setFocusTraversable(false);
                hideButton.translateXProperty().bind(translateXProperty().add(widthProperty()).subtract(89));
                hideButton.translateYProperty().bind(translateYProperty().add(heightProperty()).subtract(28));

                client.anchor.getChildren().add(hideButton);
                hideButton.toFront();
//                Platform.runLater(hideButton::toFront);
            }
        }

        getStylesheets().add("css/game-window.css");
        setDark(AnagramsClient.getTextColor(client.colors.get(AnagramsClient.Colors.GAME_FOREGROUND)).equals("white"));

        setContents(splitPane);

        if (isMobile) {
            title.setStyle("-fx-font-size: 16;");
            maximizeButton.setScaleX(1.45);
            maximizeButton.setScaleY(1.45);
            closeButton.setScaleX(1.45);
            closeButton.setScaleY(1.45);

            controlPanel.getChildren().remove(exitGameButton);
            minWidthProperty().bind(client.stage.widthProperty());
            maxWidthProperty().bind(client.stage.widthProperty());
            minHeightProperty().bind(client.stage.heightProperty());
            maxHeightProperty().bind(client.stage.heightProperty());
            client.getWebAPI().registerJavaFunction("toggleFullscreenIcon", e -> maximizeButton.toggle());
            maximizeButton.setOnAction(e -> {
                double dividerPosition = splitPane.getDividerPositions()[0];
                client.getWebAPI().executeScript("toggleFullscreen();");

                splitPane.setDividerPosition(0, dividerPosition);
            });
        }
        else {
            setMinSize(1000, 674);
            setPrefSize(1000, 674);
            textField.setPrefWidth(310);
            makeResizable();
            setAsDragZone(controlPanel, tilePanel);
            tilePanel.widthProperty().addListener((obs, oldVal, newVal) -> setTiles(tilePool));
            tilePanel.heightProperty().addListener((obs, oldVal, newVal) -> setTiles(tilePool));

            maximizeButton.setOnAction(click -> {

                double dividerPosition = splitPane.getDividerPositions()[0];
                for (int i = 0; i < 3; i++) {
                    gameGrid.getColumnConstraints().get(i).setMinWidth(minPanelWidth);
                    gameGrid.getColumnConstraints().get(i).setPrefWidth(326);
                }
                maximizeButton.maximizeAction.handle(click);
                gameGrid.layout();

                Platform.runLater(() -> {

                    splitPane.setDividerPosition(0, dividerPosition);

                    if (gameOver) {
                        showPosition(gameLog.get(position));
                        return;
                    }

                    for (GamePanel panel : players.values()) {
                        panel.wordPane.getChildren().clear();

                        if (!panel.words.isEmpty()) {
                            panel.savingSpace.set(false);
                            panel.addWords(panel.words.keySet().toArray(new String[0]));
                        }
                    }

                });

            });
        }

        if (!isWatcher) {
            if(gameLog.isEmpty()) {
                homePanel.takeSeat(username);

                textField.setOnAction(e -> {
                    makePlay(textField.getText().toUpperCase());
                    textField.clear();
                });
                textField.setPromptText("Enter a word here to play");
                textField.getStyleClass().add("text-field");

                wordBuilder.visibleProperty().bind(textField.focusedProperty());
                wordBuilder.setPrefWidth(310);
                wordBuilder.setPrefHeight(33);
                wordBuilder.setMaxWidth(310);
                wordBuilder.setMaxHeight(33);

                wordBuilder.setBackground(new Background(new BackgroundFill(Color.web("#E5E5E5"), CornerRadii.EMPTY, Insets.EMPTY)));
                wordBuilder.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));


                textField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("[a-zA-Z]*")) {
                        textField.setText(oldValue);

                    } else if (newValue.length() > 15) {
                        textField.setText(oldValue);
                        textField.positionCaret(oldValue.length());
                    } else
                        updateWordBuilder(newValue.toUpperCase());
                });

                caret.setStrokeWidth(2);
                caret.translateXProperty().bind(textField.caretPositionProperty().multiply(20).add(4));
                caret.setViewOrder(Double.NEGATIVE_INFINITY);
                blinker.getKeyFrames().addAll(
                        new KeyFrame(Duration.seconds(1.0), ae -> caret.setOpacity(1)),
                        new KeyFrame(Duration.seconds(0.5), ae -> caret.setOpacity(0))
                );
                blinker.setCycleCount(Animation.INDEFINITE);
                blinker.play();
                wordBuilder.getChildren().add(caret);
            }

            if (isMobile) {
                StackPane.setAlignment(wordBuilder, Pos.BOTTOM_CENTER);
                controlPanel.getChildren().remove(infoPane);
                controlPanel.getChildren().addAll(textField, infoPane);
                client.stack.getChildren().add(wordBuilder);
            } else {
                textStack.getChildren().addAll(textField, wordBuilder);
                controlPanel.getChildren().remove(infoPane);
                controlPanel.getChildren().addAll(textStack, infoPane);
            }
        }

        setAsMaximizeZone(titleBar, controlPanel, gameGrid);

        client.setColors();
        closeButton.setOnAction(e -> exitGame());

        if (!gameLog.isEmpty())
            endGame();

        else {
            textField.requestFocus();
            layout();
        }

        show(false);

    }

    /**
     * Colors the wordBuilder's outline according to whether it contains a playable letter sequence
     * according to the rules of Anagrams. The method will consider all possible steals as well as
     * a play from the pool and rank them according to a point based system. Tiles will be designated
     * as blanks or as normal tiles according to the method's prediction of the user's most likely
     * play.
     *
     * @param entry The contents of the wordBuilder's text field
     */
    private void updateWordBuilder(String entry) {

        wordBuilder.getChildren().retainAll(caret);
        if (entry.isEmpty()) {
            wordBuilder.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));
            return;
        }

        //Find the best play
        Play bestPlay = new Play("", entry, tilePool);
        int bestScore = bestPlay.getScore();
        for(GamePanel panel : players.values()) {
            for (GamePanel.Word word : panel.words.values()) {
                Play play = new Play(word.letters, entry, tilePool);
                int score = play.getScore();
                if (score > bestScore) {
                    bestPlay = play;
                    bestScore = score;
                }
            }

        }

        //paint the tiles
        int x = 5;
        for (Character tile : bestPlay.buildWord()) {
            Rectangle rect = new Rectangle(x, 5, 18, 22);
            rect.setArcWidth(2);
            rect.setArcHeight(2);
            rect.setFill(Color.YELLOW);
            Text text = new Text(x + 1, 26, String.valueOf(Character.toUpperCase(tile)));
            text.setFont(Font.font("Courier New", FontWeight.BOLD, 28));
            text.setFill(Character.isLowerCase(tile) ? Color.RED : Color.BLACK);
            wordBuilder.getChildren().addAll(rect, text);
            x += 20;
        }

        //set the wordBuilder's border color
        Color borderColor = Color.BLACK;
        if(entry.length() >= minLength)
            borderColor = bestPlay.isValid() ? Color.CHARTREUSE : Color.RED;
        wordBuilder.setBorder(new Border(new BorderStroke(borderColor, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));

    }


    /**
     *
     */
    private void exitGame() {

        hide();
        wordDisplay.hide();
        explorer.hide();
        blinker.stop();
        client.anchor.getChildren().remove(hideButton);
        client.exitGame(gameID, isWatcher);
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
    private void allocateSpace() {

        double usedSpace = 0;

        double availableSpace = getMinWidth() - 10;

        for (int i = 0; i < 3; i++) {
            usedSpace += gameGrid.getColumnConstraints().get(i).getMinWidth();
            availableSpace -= gameGrid.getColumnConstraints().get(i).getMinWidth();
        }
        if(availableSpace < 0) {
            setPrefWidth(usedSpace + 20);
            for (int i = 0; i < 3; i++) {
                ColumnConstraints currentColumn = gameGrid.getColumnConstraints().get(i);
                currentColumn.setPrefWidth(currentColumn.getMinWidth());
            }
        }
        else {
            setPrefWidth(Math.max(1000, usedSpace + 10));
            for (int i = 0; i < 3; i++) {
                ColumnConstraints currentColumn = gameGrid.getColumnConstraints().get(i);
                currentColumn.setPrefWidth(currentColumn.getMinWidth() + availableSpace / 3);
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
        private final ScrollPane scrollPane = new ScrollPane(wordPane);
        private final Label playerNameLabel = new Label();
        private final Label playerScoreLabel = new Label();

        private final SimpleBooleanProperty savingSpace = new SimpleBooleanProperty(false);

        int column = -1;

        boolean isAvailable = true;
        private final LinkedHashMap<String, Word> words = new LinkedHashMap<>();
        private int score = 0;

        /**
         * An empty placeholder gamePanel
         */
        private GamePanel() {
            getStyleClass().add("game-panel");
            if (isMobile) pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);

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
            setAsDragZone(infoPane, wordPane);
            savingSpace.set(false);
        }

        /**
         *
         */
        private GamePanel(int column) {
            this();
            this.column = column;
            if (column >= 0) {
                gamePanels.add(this);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            }
        }

        /**
         * Puts the given player's name on this panel and displays their score.
         *
         * @param newPlayer The name of the player to be added.
         */
        private GamePanel takeSeat(String newPlayer) {
            pseudoClassStateChanged(PseudoClass.getPseudoClass("abandoned"), false);
            this.playerName = newPlayer;

            players.put(newPlayer, this);
            playerNameLabel.setText(playerName);

            if (playerName.startsWith("Robot")) {
                playerNameLabel.setGraphic(robotImage);
            }
            if (words.isEmpty() && !gameOver) {
                playerScoreLabel.setText("0");
            }
            isAvailable = false;

            return this;
        }

        /**
         * The player has left this seat. If they have any words, they remain.
         * Otherwise, the seat becomes available for another player.
         */
        private void abandonSeat() {
            if (!gameOver && column >= 0) {
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
            savingSpace.set(false);
        }

        /**
         * Removes the occupant, as well as any words they may have, from this pane.
         * Used only during endgame analysis.
         */
        private void reset() {
            wordPane.getChildren().clear();
            playerName = null;
            playerNameLabel.setText("");
            playerNameLabel.setGraphic(null);
            playerScoreLabel.setText("");
            isAvailable = true;
            savingSpace.set(false);
        }

        /**
         * Add a new word to the player's collection and recalculate their score.
         *
         * @param wordToAdd The word to be removed.
         */
        private void addWord(String wordToAdd) {

            Word newWord = new Word(wordToAdd, savingSpace.get());
            words.put(wordToAdd, newWord);

            double paneWidth = getWidth();
            double paneHeight = getHeight() - infoPane.getHeight();

            if (isMobile && newWord.width() > paneWidth) {
                savingSpace.set(true);
                gameGrid.getColumnConstraints().get(column).setPrefWidth(newWord.width());
            }

            if(!wordPane.getChildren().isEmpty()) {
                Bounds bounds = wordPane.getChildren().get(wordPane.getChildren().size() - 1).getBoundsInParent();

                if (bounds.getMaxY() + wordPane.getHgap() + newWord.height() > paneHeight) {
                    if (bounds.getMaxX() + (wordPane.getVgap() + newWord.width()) / 2 > paneWidth) {
                        if (!savingSpace.get()) {
                            savingSpace.set(true);
                            addWords(words.keySet().toArray(new String[0]));
                            return;
                        } else if (column >= 0) {
                            gameGrid.getColumnConstraints().get(column).setMinWidth(paneWidth * 1.15);
                            allocateSpace();
                        }
                    }
                }
            }

            newWord.setOpacity(0);
            wordPane.getChildren().add(newWord);
            FadeTransition ft = new FadeTransition(Duration.seconds(0.9), newWord);
            ft.setToValue(1.0);
            ft.play();

            score += newWord.length() * newWord.length();
            playerScoreLabel.setText(score + "");
        }


        /**
         * Adds a bunch of words all at once. Used during postgame analysis, when resizing the window,
         * and when joining or starting a game.
         */
        private void addWords(String[] wordsToAdd) {

            wordPane.getChildren().clear();
            words.clear();

            for (String word : wordsToAdd) {
                Word newWord = new Word(word, savingSpace.get());
                words.put(word, newWord);
                wordPane.getChildren().add(newWord);
            }

            layout();

            double paneWidth = getWidth();
            double paneHeight = getHeight() - infoPane.getHeight();
            double maxY = wordPane.getChildren().get(wordsToAdd.length - 1).getBoundsInParent().getMaxY();

            //make sure layout is complete
            if(maxY <= 0 || paneHeight <= 0) {
                Platform.runLater(() -> addWords(wordsToAdd));
                return;
            }
            //check if we need to save space by switching to small tiles
            double minWidth = Math.max(minPanelWidth, paneWidth * maxY / paneHeight);

            if(maxY > paneHeight) {
                if (!savingSpace.get()) {
                    if(column >= 0)
                        gameGrid.getColumnConstraints().get(column).setMinWidth(324);
                    savingSpace.set(true);
                    addWords(wordsToAdd);
                    return;
                }
                //resize column
                else if (column >= 0) {
                    gameGrid.getColumnConstraints().get(column).setMinWidth(minWidth);
                    allocateSpace();
                }
            }
            else if(column >= 0) {
                gameGrid.getColumnConstraints().get(column).setMinWidth(minWidth);
            }
            setScore();
        }


        /**
         * Remove a word from the player's collection and recalculate their score.
         * If the player has left the table and has no words, the seat is opened up for
         * another player.
         *
         * @param wordToRemove The word to be removed.
         */
        private void removeWord(String wordToRemove) {
            Word word = words.remove(wordToRemove);
            if(word == null) return;

            FadeTransition ft = new FadeTransition(Duration.seconds(1), word);
            ft.setToValue(0);
            ft.setOnFinished(actionEvent -> wordPane.getChildren().remove(word));
            ft.play();

            score -= word.length() * word.length();
            playerScoreLabel.setText(score + "");
        }

        /**
         *
         */
        private void setScore() {
            score = 0;
            for (Word word : words.values()) {
                score += word.length() * word.length();
            }
            playerScoreLabel.setText(score + "");
        }

        /**
         *
         */
        private class Word extends Region {

            private final static double TILE_GAP = 2;
            private static final int PADDING = 4;
            private final String letters;
            private boolean highlight = false;

            final int tileWidth;
            final int tileHeight;
            final int tileFontSize;

            /**
             *
             */
            private Word(String symbols, boolean savingSpace) {
                if(symbols.endsWith("#") || symbols.endsWith("$")) {
                    letters = symbols.substring(0, symbols.length() - 1);
                    if(client.prefs.getBoolean("highlight_words", false))
                        highlight = true;
                }
                else {
                    letters = symbols;
                }

                tileWidth = savingSpace ? 12 : 16;
                tileHeight = savingSpace ? 16 : 20;
                tileFontSize = savingSpace ? 18: 24;

                if (gameOver || isWatcher) {
                    setOnMouseClicked(event -> {
                        if (!explorer.isVisible()) {
                            explorer.show(false);
                        }
                        explorer.lookUp(letters);
                    });
                }

                draw();
            }

            /**
             *
             */
            private int length() {
                return letters.length();
            }

            /**
             * Calculates the size of this Word in pixels
             */
            private double width() {
                return length()*tileWidth + (length() - 1)*TILE_GAP + 1 + 2*PADDING;
            }

            /**
             *
             */
            private double height() {
                return tileHeight + PADDING;
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
     * Displays messages from the server such as the time remaining.
     *
     * @param nextMessage The message to be displayed.
     */
    void setNotificationArea(String nextMessage) {
        notificationArea.setText(nextMessage);
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
        tilePanel.showTiles(tilePool);
        if(!gameOver)
            updateWordBuilder(textField.getText().toUpperCase());
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

        //player reenters game after leaving
        else if (players.containsKey(newPlayerName)) {
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
    void removePlayer(String playerToRemove) {
        if (players.containsKey(playerToRemove)) {
            players.get(playerToRemove).abandonSeat();
        }
    }

    /**
     * Method used during gameplay for adding words and updating the tilePool. Plays a sound.
     */
    void makeWord(String playerName, String wordToAdd, String nextTiles) {
        if(players.containsKey(playerName))
            players.get(playerName).addWord(wordToAdd);
        setTiles(nextTiles);
        if (client.prefs.getBoolean("play_sounds", true))
            wordClip.play();
    }


    /**
     * Removes the given word from the given player from whom it has been stolen.
     *
     * @param playerName   the player whose word has been stolen
     * @param wordToRemove the stolen word
     */
    void removeWord(String playerName, String wordToRemove) {
        if(players.containsKey(playerName))
            players.get(playerName).removeWord(wordToRemove);
    }

    /**
     *
     */
    void doSteal(String shortPlayer, String shortWord, String longPlayer, String longWord, String nextTiles) {
        removeWord(shortPlayer, shortWord);
        makeWord(longPlayer, longWord, nextTiles);
    }


    /**
     * Displays the new chat message (and sender) in the chat box and automatically scrolls to view
     *
     * @param msg The message to display.
     */
    void handleChat(String msg) {
        if (chatBox.getText().isEmpty())
            chatBox.appendText(msg);
        else
            chatBox.appendText("\n" + msg);
    }

    /**
     * Removes the textEntry and exitGame button; adds buttons for navigating through the game history.
     */
    void endGame() {

        gameOver = true;
        blinker.stop();

        maxPosition = gameLog.size() - 1;
        position = maxPosition;

        controlPanel.getChildren().removeAll(textStack, textField, infoPane);
        controlPanel.getChildren().addAll(backToStartButton, backTenButton, backButton, showPlaysButton, forwardButton, forwardTenButton, forwardToEndButton, infoPane);

        backToStartButton.setPrefWidth(29);
        backTenButton.setPrefWidth(29);
        backButton.setPrefWidth(29);
        forwardButton.setPrefWidth(29);
        forwardTenButton.setPrefWidth(29);
        forwardToEndButton.setPrefWidth(29);
        showPlaysButton.textProperty().bind(Bindings.createStringBinding(() -> wordDisplay.visibleProperty().get() ? "Hide plays" : "Show plays", wordDisplay.visibleProperty()));
        showPlaysButton.setOnAction(e -> {
            if (wordDisplay.isVisible()) {
                wordDisplay.hide();
            } else {
                wordDisplay.show(false);
                client.send("findplays " + gameID + " " + position);
            }
        });

        backToStartButton.setOnAction(e -> {
            position = 0;
            showPosition(gameLog.get(position));
        });
        backTenButton.setOnAction(e -> {
            position = Math.max(position - 10, 0);
            showPosition(gameLog.get(position));
        });
        backButton.setOnAction(e -> {
            position = Math.max(position - 1, 0);
            showPosition(gameLog.get(position));
        });
        forwardButton.setOnAction(e -> {
            position = Math.min(position + 1, maxPosition);
            showPosition(gameLog.get(position));
        });
        forwardTenButton.setOnAction(e -> {
            position = Math.min(position + 10, maxPosition);
            showPosition(gameLog.get(position));
        });
        forwardToEndButton.setOnAction(e -> {
            position = maxPosition;
            showPosition(gameLog.get(position));
        });

        if (!isMobile) {
            addEventFilter(KeyEvent.KEY_PRESSED, event -> {

                if (!chatField.isFocused()) {
                    if (event.getCode().isArrowKey()) {
                        event.consume();
                    }
                    switch (event.getCode()) {
                        case RIGHT -> forwardButton.fire();
                        case LEFT -> backButton.fire();
                        case PAGE_DOWN -> forwardTenButton.fire();
                        case PAGE_UP -> backTenButton.fire();
                        case END -> forwardToEndButton.fire();
                        case HOME -> backToStartButton.fire();
                    }
                }
            });

            //Download button
            MenuItem textOption = new MenuItem("Save as Text");
            textOption.setOnAction(click -> saveAsText());
            MenuItem gifOption = new MenuItem("Save as GIF");
            gifOption.setOnAction(click -> saveAsGif());

            saveButton.getItems().addAll(textOption, gifOption);
            saveButton.setTooltip(new Tooltip("Save record of game"));
            saveButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), client.getWebAPI().isMobile());
            controlPanel.getChildren().add(saveButton);
        }

        showPosition(gameLog.get(maxPosition));
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

        for (GamePanel panel : players.values())
            panel.reset();

        players.clear();

        for (int i = 0; i < 3; i++) {
            gameGrid.getColumnConstraints().get(i).setMinWidth(minPanelWidth);
            gameGrid.getColumnConstraints().get(i).setPrefWidth(326);
        }
        gameGrid.layout();

        notificationArea.setText("Time remaining " + tokens[0]);

        //should probably parse with regex or json
        if (tokens.length > 2) {
            for (int i = 2; i < tokens.length; i += 2) {
                String playerName = tokens[i];
                GamePanel panel = addPlayer(playerName);
                String[] words = tokens[i + 1].substring(1, tokens[i + 1].length() - 1).split(",");
                if(words[0].length() >= minLength)
                    panel.addWords(words);
            }
        }

        setTiles(tokens[1]);

        if (wordDisplay.isVisible()) {
            client.send("findplays " + gameID + " " + position);
        }
    }

    /**
     * Displays possible plays in the WordDisplay PopWindow
     *
     * @param plays A formatted String containing possible plays from the pool and steals, e.g.
     *              [FUMITORY] @ [BLEWARTS + I -> BRAWLIEST, BLEWARTS + I -> WARBLIEST, BLEWARTS + FO -> BATFOWLERS]
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
    private class WordDisplay extends PopWindow {

        private final GamePanel poolPanel = new GamePanel();
        private final GamePanel stealsPanel = new GamePanel();

        /**
         *
         */
        private WordDisplay() {
            super(client.anchor);
            setViewOrder(-1000);
            AnchorPane.setLeftAnchor(this, client.stage.getWidth() - 453);
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
            mainPanel.getStyleClass().add("word-display");
            getStylesheets().add("css/game-window.css");

            displayGrid.add(poolPanel, 0, 0);
            displayGrid.add(stealsPanel, 0, 1);
            displayGrid.addEventFilter(MouseEvent.ANY, event -> {
                if(event.getTarget() instanceof FlowPane || event.getTarget() instanceof Text) {
                    Event.fireEvent(mainPanel, event);
                    if(event.getEventType().equals(MouseEvent.MOUSE_DRAGGED)) {
                        event.consume();
                    }
                }
            });

            makeResizable();
            setPrefSize(380, 450);
            setContents(mainPanel);
            setTitle("Possible plays");
            setAsDragZone(mainPanel);
        }

        /**
         *
         */

        private void setWords(String[] wordsInPool, String[] possibleSteals) {

            poolPanel.wordPane.getChildren().clear();
            stealsPanel.wordPane.getChildren().clear();
            poolPanel.playerNameLabel.setText("Pool");
            stealsPanel.playerNameLabel.setText("Steals");

            //pool
            if (!wordsInPool[0].isEmpty())
                poolPanel.addWords(wordsInPool);

            //steals
            if (!possibleSteals[0].isEmpty()) {
                Tooltip[] tooltips = new Tooltip[possibleSteals.length];
                for (int i = 0; i < possibleSteals.length; i++) {
                    String[] contents = possibleSteals[i].split(" -> ");
                    possibleSteals[i] = contents[1];
                    Tooltip tooltip = new Tooltip(contents[0]);
                    tooltips[i] = tooltip;
                }
                stealsPanel.addWords(possibleSteals);
                for (int i = 0; i < possibleSteals.length; i++) {
                    Tooltip.install(stealsPanel.wordPane.getChildren().get(i), tooltips[i]);
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
        if (input.length() >= minLength) {
            for (Map.Entry<String, GamePanel> player : players.entrySet()) {
                String playerName = player.getKey();

                for (GamePanel.Word shortWord : player.getValue().words.values()) {
                    if (shortWord.letters.equalsIgnoreCase(input)) {
                        return; //word is already on the board
                    }
                    else if (input.length() > shortWord.length()) {
                        //Attempt to steal
                        Play play = new Play(shortWord.letters, input, tilePool);
                        if (play.isValid()) {
                            client.send("steal " + gameID + " " + playerName + " " + shortWord.letters + " " + username + " " + input);
                            return;
                        }
                    }
                }
            }
            //Attempt to form word from pool
            Play play = new Play("", input, tilePool);
            if (play.isValid()) {
                client.send("makeword " + gameID + " " + username + " " + input);
            }
        }
    }

    /**
     *
     *
     */
    private void saveAsText() {

        String gameData = "gameID " + gameID + "\\n" +
                "lexicon " + lexicon + "\\n" +
                "numSets " + numSets + "\\n" +
                "minLength " + minLength + "\\n" +
                "blankPenalty " + blankPenalty + "\\n" +
                "speed " + speed + "\\n\\n";

        for (String[] gameState : gameLog) {
            for(String datum : gameState) {
                gameData = gameData.concat(datum + " ");
            }
            gameData = gameData.concat("\\n");

        }
        client.getWebAPI().executeScript("""
            var pom = document.createElement('a');
            pom.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent('%s'));
            pom.setAttribute('download', 'game %s.txt');
            if (document.createEvent) {
                var event = document.createEvent('MouseEvents');
                event.initEvent('click', true, true);
                pom.dispatchEvent(event);
            }
            else {
                pom.click();
            }
            """.formatted(gameData, gameID)
        );
    }

    /**
     *
     */
    private void saveAsGif() {

        SnapshotPane snapshotPane = new SnapshotPane(client, gameID, username, minLength + "", blankPenalty, numSets, speed, lexicon, gameLog);

        ProgressBar progressBar = new ProgressBar();
        progressBar.progressProperty().bind(snapshotPane.progress);

        Platform.runLater(() -> {
            controlPanel.getChildren().remove(saveButton);
            controlPanel.getChildren().add(progressBar);
        });

        snapshotPane.finished.addListener((finished, wasFinished, isFinished) -> {
            if (isFinished) {
                controlPanel.getChildren().remove(progressBar);
                controlPanel.getChildren().add(saveButton);
            }
        });
    }

    /**
     * An attempt to form a word, either by building from the pool or by stealing an existing word
     */
    private class Play {

        final String shortWord;
        final String longWord;
        final String tiles;

        private Play(String shortWord, String longWord, String tiles) {
            this.shortWord = shortWord;
            this.longWord = longWord;
            this.tiles = tiles;
        }

        /**
         *
         * @return a measure of how "playable" this play is. All valid plays will have a score >= 0.
         */
        private int getScore() {

            String charsToSteal = shortWord;
            String tiles = tilePool;
            int blanksToChange = 0;
            int blanksToTakeFromPool = 0;
            int missingFromPool = 0;

            for(String s : longWord.split("")) {
                if(charsToSteal.toUpperCase().contains(s)) {
                    charsToSteal = charsToSteal.replaceFirst(s, "");
                }
                else if(tiles.contains(s)) {
                    tiles = tiles.replaceFirst(s, "");
                }
                else if(charsToSteal.matches("[a-z]+")) {
                    charsToSteal = charsToSteal.replaceFirst("[a-z]", "");
                    blanksToChange++;
                }
                else if(tiles.contains("?")) {
                    tiles = tiles.replaceFirst("\\?", "");
                    blanksToTakeFromPool++;
                }
                else {
                    missingFromPool++;
                }
            }
            int unstolen = charsToSteal.replace("#", "").length();

            if (shortWord.isEmpty())
                return longWord.length() - minLength  - blanksToTakeFromPool*(blankPenalty + 1) - 2*missingFromPool - 2*unstolen;

            else
                return longWord.length() - shortWord.length() - blanksToChange*blankPenalty - blanksToTakeFromPool*(blankPenalty + 1) - 2*missingFromPool - 2*unstolen;
        }

        /**
         * Designate letters as blanks or as normal tiles
         */
        private char[] buildWord() {
            String tiles = this.tiles + shortWord.toUpperCase();
            String newWord = "";

            for (String s : longWord.split("")) {
                if (tiles.contains(s)) {
                    tiles = tiles.replaceFirst(s, "");
                    newWord = newWord.concat(s);
                }
                else {
                    newWord = newWord.concat(s.toLowerCase());
                }
            }
            return newWord.toCharArray();
        }


        /**
         *
         */
        private boolean isValid() {

            String entry = longWord;
            String blanksToKeep = "";
            String tiles = this.tiles;
            int blanksToChange = 0;

            //Search for characters in the word to be stolen
            for (String s : shortWord.split("")) {
                if (entry.contains(s)) {
                    //Transfer a tile from the shortWord to the longWord
                    entry = entry.replaceFirst(s, "");
                }
                else if (Character.isLowerCase(s.charAt(0))) {
                    if (entry.contains(s.toUpperCase())) {
                        //Transfer the blank without re-designating
                        blanksToKeep += s.toUpperCase();
                        entry = entry.replaceFirst(s.toUpperCase(), "");
                    }
                    else {
                        //Mark a blank for re-designation
                        blanksToChange++;
                    }
                }
                else {
                    //The shortWord contains a letter not found in the longWord
                    return false;
                }
            }

            //Search pool for missing tiles
            for (String s : entry.split("")) {
                if(entry.length() > blanksToChange) {
                    if (tiles.contains(s)) {
                        //Add a regular tile to the word
                        tiles = tiles.replaceFirst(s, "");
                        entry = entry.replaceFirst(s, "");
                    }
                    else if(!blanksToKeep.isEmpty()) {
                        for (String t : blanksToKeep.split("")) {
                            //Mark a retained blank for re-designation
                            if (tiles.contains(t)) {
                                blanksToKeep = blanksToKeep.replaceFirst(t, "");
                                tiles = tiles.replaceFirst(t, "");
                                blanksToChange++;
                                break;
                            }
                        }
                    }
                }
            }

            //Designate blanks to missing letters
            int penalty = 0;

            for (int i = 0; i < entry.length(); i++) {
                if (blanksToChange-- > 0) {
                    //Re-designate a blank
                    penalty += blankPenalty;
                } else if (tiles.contains("?")) {
                    //Take a blank from the pool and designate it
                    tiles = tiles.replaceFirst("\\?", "");
                    penalty += blankPenalty + 1;
                } else {
                    //Not enough blanks available
                    return false;
                }
            }


            if(shortWord.isEmpty()) {
                return longWord.length() - minLength >= penalty;
            }
            else if(longWord.length() - shortWord.length() >= Math.max(penalty, 1))  {
                return isRearrangement(shortWord, longWord);
            }
            return false;
        }


        /**
         * Given two words, the shorter of which is a subset of the other, determines whether a rearrangement/permutation
         * of letters is necessary to form the longer.
         *
         * @param shortWord     a short word (case must match longWord)
         * @param longWord      a longer word (case must match that of shortWord)
         */
        private static boolean isRearrangement(String shortWord, String longWord) {

            while(longWord.length() >= shortWord.length() && shortWord.length() > 0) {
                if (shortWord.charAt(0) == longWord.charAt(0)) {
                    shortWord = shortWord.substring(1);
                }
                longWord = longWord.substring(1);
            }

            return shortWord.length() > longWord.length();
        }
    }
}