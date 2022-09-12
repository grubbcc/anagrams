package client;

import com.jpro.webapi.JProApplication;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import one.jpro.sound.*;
import org.json.JSONArray;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

/**
 *
 *
 */
class AnagramsClient extends JProApplication {

	private final String host = InetAddress.getLocalHost().getHostAddress();
	private final int port = 8118;
	private static final String version = "1.0.1";

	private boolean connected = false;
	private InputStream serverIn;
	private OutputStream serverOut;
	BufferedReader bufferedIn;

	Stage stage;
	private Thread messageLoop;

	//	private SettingsMenu settingsMenu;
	WordExplorer explorer;

	private final FlowPane gamesPanel = new FlowPane();
	private final ScrollPane gamesScrollPane = new ScrollPane();

	private final Label playersHeader = new Label("Players logged in");
	private final VBox playersListPane = new VBox();
	private final BorderPane borderPane = new BorderPane();
	private final BorderPane sidePanel = new BorderPane();
	private final ScrollPane playersScrollPane = new ScrollPane();

	private final SplitPane splitPane = new SplitPane();
	final AnchorPane anchor = new AnchorPane(splitPane);
	final StackPane stack = new StackPane(anchor);

	private final TextField chatField = new TextField();
	private final TextArea chatBox = new TextArea();
	private final BorderPane chatPanel = new BorderPane();
	private final PlayerPane playerPane = new PlayerPane(this);

	//GameWindow gameWindow;
	final HashMap<String, GameWindow> gameWindows = new HashMap<>();
	private final HashMap<String, GamePane> gamePanes = new HashMap<>();
	private final HashMap<String, Label> playersList = new HashMap<>();
	String username;

	static final String[] lexicons = {"CSW21", "NWL20"};
	Preferences prefs;

	final EnumMap<Colors, String> colors = new EnumMap<>(Colors.class);
	private final String newPlayerSound = getClass().getResource("/sounds/new player sound.mp3").toExternalForm();
	private final String newGameSound = getClass().getResource("/sounds/new game sound.mp3").toExternalForm();
	private AudioClip newPlayerClip;
	private AudioClip newGameClip;
	boolean guest = false;

	LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();

	/**
	 *
	 */
	AnagramsClient() throws UnknownHostException { }


	/**
	 *
	 */
	enum Colors {

		MAIN_SCREEN ("-main-screen", "main_screen", "Main Screen", "#282828"),
		PLAYERS_LIST ("-players-list", "players_list", "Players List", "#3a3a3a"),
		GAME_FOREGROUND ("-game-foreground", "game_foreground", "Game Foreground", "#193a57"),
		GAME_BACKGROUND ("-game-background", "game_background", "Game Background", "#2d6893"),
		CHAT_AREA ("-chat-area", "chat_area", "Chat Area", "#a4dffc"),
		GAME_CHAT ("-game-chat", "game_chat", "Game Chat", "#002868")
		;

		final String css;
		final String key;
		final String display;
		final String defaultCode;

		Colors(String css, String key, String display, String defaultCode) {
			this.css = css;
			this.key = key;
			this.display = display;
			this.defaultCode = defaultCode;
		}
	}

	/**
	 *
	 */
	static void main(String[] args) {
		launch(args);
	}


	/**
	 *
	 */
	@Override
	public void start(Stage stage) {
		this.stage = stage;
		System.out.println("Welcome to Anagrams!");

		newPlayerClip = AudioClip.getAudioClip(newPlayerSound, stage);
		newGameClip = AudioClip.getAudioClip(newGameSound, stage);
		for(Colors color : Colors.values()) {
			colors.put(color, color.defaultCode);
		}

		createAndShowGUI();

		if(connect() ) {
			System.out.println("Connected to server on port " + port);
			new LoginMenu(this);
		}

	}

	/**
	 *
	 */
	private void createAndShowGUI() {

		//control panel
		Button createGameButton = new Button("Create Game");
		createGameButton.setStyle("-fx-font-size: 18");
		createGameButton.setPrefHeight(39);
		createGameButton.setOnAction(e -> {if(gameWindows.isEmpty()) new GameMenu(this);});

		Button settingsButton = new Button("Settings", new ImageView("/images/settings.png"));
		settingsButton.setStyle("-fx-font-size: 18");
		settingsButton.setPrefSize(162, 33);
		settingsButton.setOnAction(e -> new SettingsMenu(this));

		HBox controlPanel = new HBox();
		controlPanel.setFillHeight(true);
		createGameButton.prefWidthProperty().bind(controlPanel.widthProperty().subtract(162));
		controlPanel.getChildren().addAll(createGameButton, settingsButton);

		//games panel
		gamesPanel.setId("games-panel");
		gamesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
		gamesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		gamesScrollPane.setFitToHeight(true);
		gamesScrollPane.setFitToWidth(true);
		gamesScrollPane.setContent(gamesPanel);

		//players panel
		Image wooglesLogo = new Image("/images/woogles.png", 162, 33, false, true);
		Image anagramsLogo = new Image("/images/anagrams-icon.png", 37, 33, false, true);
		Button wooglesButton = new Button("", new ImageView(wooglesLogo));
		Button anagramsButton = new Button("Anagrams Home", new ImageView(anagramsLogo));
		anagramsButton.setPrefWidth(162);
		wooglesButton.setOnAction(e -> getWebAPI().openURLAsTab("https://woogles.io"));
		anagramsButton.setOnAction(e -> getWebAPI().openURLAsTab("https://anagrams.site"));
		VBox links = new VBox(wooglesButton, anagramsButton, playersHeader);
		sidePanel.setPrefWidth(162);
		sidePanel.setId("side-panel");
		playersScrollPane.setFitToHeight(true);
		playersScrollPane.setFitToWidth(true);
		playersScrollPane.setContent(playersListPane);
		sidePanel.setTop(links);
		sidePanel.setCenter(playersScrollPane);
		playersHeader.setTooltip(new Tooltip("Click a player's name to view profile"));

		//chat panel
		chatBox.setEditable(false);
		chatField.setStyle("-fx-font-size: " + (getWebAPI().isMobile() ? 18 : 16) + ";");
		chatField.setPromptText("Type here to chat");
		chatField.focusedProperty().addListener((focus, wasFocused, isFocused) -> {
			if(isFocused)
				chatBox.scrollTopProperty().set(Double.MAX_VALUE);
		});
		chatField.setOnAction(ae -> {
			String msg = String.format("%1.500s", chatField.getText()); //ISN'T WORKING
			if(!msg.isBlank())
				send("chat " + username + ": " + msg);
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

		chatPanel.setBottom(chatField);
		chatBox.setWrapText(true);
		chatBox.setStyle("-fx-font-size: " + (getWebAPI().isMobile() ? 18 : 16) + ";");
		chatBox.appendText("Welcome to Anagrams version " + version + "!");
		chatPanel.setCenter(chatBox);

		//main layout
		borderPane.setTop(controlPanel);
		borderPane.setCenter(gamesScrollPane);
		borderPane.setRight(sidePanel);
		borderPane.setMinHeight(300);
		borderPane.setDisable(true);

		splitPane.setOrientation(Orientation.VERTICAL);
		splitPane.getItems().addAll(borderPane, chatPanel);
		splitPane.setDividerPosition(0, 0.9);
		splitPane.setDisable(true);

		AnchorPane.setRightAnchor(splitPane, 0.0);
		AnchorPane.setBottomAnchor(splitPane, 0.0);
		AnchorPane.setLeftAnchor(splitPane, 0.0);
		AnchorPane.setTopAnchor(splitPane, 0.0);

		anchor.setMinSize(Double.MIN_VALUE, Double.MIN_VALUE);
		anchor.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		final Scene scene;
		try {
			scene = new Scene(stack);
		}
		catch(IllegalArgumentException e) {
			getWebAPI().executeScript("window.location.reload(false)");
			return;
		}
		scene.getStylesheets().setAll("css/anagrams.css", "css/main.css");


		//main stage
		stage.setTitle("Anagrams");

		if(getWebAPI().isMobile()) {
			gamesPanel.getTransforms().add(new Scale(1.25, 1.25));
			getWebAPI().registerJavaFunction("setWidth", newWidth -> {
				stage.setWidth(Double.parseDouble(newWidth));
			});
			getWebAPI().registerJavaFunction("setHeight", newHeight -> {
				stage.setHeight(Double.parseDouble(newHeight));
			});
		}

		stage.setScene(scene);
		setColors();
		stage.show();

		getWebAPI().addInstanceCloseListener(this::logOut);

	}

	/**
	 *
	 */
	void setColors() {
		String newStyle = "";
		for(Colors color : colors.keySet()) {
			newStyle += color.css + ": " + colors.get(color) + "; ";
			newStyle += color.css + "-text: " + getTextColor(colors.get(color)) + "; ";
		}

		stage.getScene().getRoot().setStyle(newStyle);
		for(GameWindow gameWindow : gameWindows.values()) {
			gameWindow.setStyle(newStyle);
			gameWindow.setDark(getTextColor(colors.get(Colors.GAME_FOREGROUND)).equals("white"));
		}
	}

	/**
	 * Sets the text color to white or black as appropriate for text readability.
	 *
	 * @param colorCode a hexadecimal String representing the background color of a container node
	 * @return a String representing the color "black" if the luminance of the background color > 40
	 *         and a String representing the color "white" otherwise.
	 */
	static String getTextColor(String colorCode) {
		int R = Integer.valueOf(colorCode.substring(1, 3), 16);
		int G = Integer.valueOf(colorCode.substring(3, 5), 16);
		int B = Integer.valueOf(colorCode.substring(5, 7), 16);

		double luminance = 0.2126*R + 0.7152*G + 0.0722*B;

		return luminance > 90 ? "black" : "white";
	}

	/**
	 * Connect to the AnagramsServer instance running on the local host
	 * @return true if connection was successful, false if not
	 */
	private boolean connect() {
		try {
			Socket socket = new Socket(host, port);
			this.serverOut = socket.getOutputStream();
			this.serverIn = socket.getInputStream();
			this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
			connected = true;
			return true;
		}
		catch (IOException ioe) {
			System.out.println("Unable to connect to server on port " + port);

			MessageDialog dialog = new MessageDialog(this, "Connection issues");
			dialog.setText("The program is having trouble connecting to the host server. Try again?");
			dialog.addYesNoButtons();
			dialog.yesButton.setOnAction(e -> getWebAPI().executeScript("window.location.reload(false)"));
			dialog.noButton.setOnAction(e -> dialog.hide());
			Platform.runLater(() -> dialog.show(true));
			return false;
		}

	}

	/**
	 *
	 */
	void login(String username) {
		this.username = username;

		prefs = Preferences.userNodeForPackage(getClass()).node(username);
		explorer = new WordExplorer(prefs.get("lexicon", "CSW21"), this);

		//set user colors
		for (Colors color : Colors.values())
			colors.put(color, prefs.get(color.key, color.defaultCode));
		setColors();

		//	settingsMenu = new SettingsMenu(this);
		new Thread(this::readMessageLoop).start();
		new Thread(this::executeCommandLoop).start();

		borderPane.setDisable(false);
		splitPane.setDisable(false);

		if(prefs.getBoolean("showguide", true)) {
			showStartupGuide();
		}
	}

	/**
	 * Creates and displays a MessageDialog with information on how to play and use the applicaiton
	 */
	private void showStartupGuide() {

		String[] titles = {
				"How to play (1/5)",
				"How to play (2/5)",
				"How to play (3/5)",
				"How to play (4/5)",
				"How to play (5/5)",

				"Did you know? (1/6)",
				"Did you know? (2/6)",
				"Did you know? (3/6)",
				"Did you know? (4/6)",
				"Did you know? (5/6)",
				"Did you know? (6/6)"
		};
		String[] intro = {
				"Spell a word using tiles from the pool or add tiles to an existing word to form a longer one.",
				"To steal a word, you must change the order of at least two of the letters.",
				"Blanks can be used as any tile, but there is a cost: for each blank used (or changed), you must take <i>additional</i> tiles from the pool. You can set how many when you create a game.",
				"The border of the text entry will turn green if you have created a valid play according to the rules of Anagrams. (But it won't check whether the word is in the dictionary; that part is up to you!)",
				"Each word is worth n&sup2; points where n is the length of the word. The player with the most points at the end of the game is the winner.",

				"You can hover over the Players label of the Game Pane to see who's currently playing.",
				"You can click on a player's name in the Players panel to see their profile (or to edit your own).",
				"You can use the arrow keys (&#11013; and &#10145;), PgDn, PgUp, Home, and End to navigate through the postgame analysis window.",
				"At the end of a game (or while watching a game), you can click on any word to see how it can be stolen.",
				"In the Word Explorer window, words are colored according to probability. The redder a word is, the more likely it is to occur.",
				"You can right-click in the Word Explorer window to save the word tree to a file or view it as an image like this one.",

		};

		String[] images = {
				"images/play.gif",
				"images/steal.png",
				"images/blank_penalty.png",
				"images/valid_play.png",
				"images/score.png",

				"images/players.png",
				"images/profile.png",
				"images/possible_plays.png",
				"images/word_explorer.png",
				"images/word_explorer.png",
				"images/word_tree.png"
		};
		MessageDialog dialog = new MessageDialog(this, titles[0]);
		dialog.setText(intro[0]);
		dialog.setImage(images[0]);
		CheckBox continueShowing = new CheckBox("Show this guide");
		continueShowing.setSelected(true);

		if(!guest) {
			dialog.buttonPane.getChildren().add(continueShowing);
		}
		dialog.addBackNextButtons();
		AtomicInteger i = new AtomicInteger();
		dialog.backButton.setOnAction(e -> {
			int j = Math.floorMod(i.decrementAndGet(), intro.length);
			dialog.setText(intro[j]);
			dialog.setTitle(titles[j]);
			dialog.setImage(images[j]);
		});
		dialog.nextButton.setOnAction(e -> {
			int j = Math.floorMod(i.incrementAndGet(), intro.length);
			dialog.setText(intro[j]);
			dialog.setTitle(titles[j]);
			dialog.setImage(images[j]);
		});
		dialog.addOkayButton();
		dialog.okayButton.setText("I'm ready!");
		dialog.okayButton.setMinWidth(Region.USE_COMPUTED_SIZE);
		dialog.okayButton.setOnAction(e -> {
			if(!continueShowing.isSelected())
				prefs.putBoolean("showguide", false);
			dialog.hide();
		});
		Platform.runLater(() -> dialog.show(true));
	}


	/**
	 * Displays information about a game and tools for joining
	 */
	private class GamePane extends GridPane {

		private final String gameID;
		private final int maxPlayers;
		private final boolean allowWatchers;
		private final boolean allowChat;
		private boolean gameOver;

		private final Label notificationLabel = new Label();
		private final Label playersLabel = new Label();

		private final ArrayList<String[]> gameLog = new ArrayList<>();
		private final HashSet<String> players = new HashSet<>();

		/**
		 *
		 */
		private GamePane(String gameID, String gameName, String playerMax, String minLength, String numSets, String blanksPenalty, String lexicon, String speed, String allowsChat, String allowsWatchers, String isOver) {

			this.gameID = gameID;
			gamePanes.put(gameID, this);
			gameOver = Boolean.parseBoolean(isOver);
			allowWatchers = Boolean.parseBoolean(allowsWatchers);
			allowChat = Boolean.parseBoolean(allowsChat);
			maxPlayers = Integer.parseInt(playerMax);
			int blankPenalty = Integer.parseInt(blanksPenalty);

			if(prefs.getBoolean("play_sounds", true)) {
				newGameClip.play();
			}
			getStyleClass().setAll("game-panel");

			//labels
			Label lexiconLabel = new Label("Lexicon: " + lexicon);
			if(lexicon.equals("CSW21"))
				lexiconLabel.setTooltip(new Tooltip("Collins Official Scrabble Words \u00a9 2021"));
			else if(lexicon.equals("NWL20"))
				lexiconLabel.setTooltip(new Tooltip("NASPA Word List \u00a9 2020"));
			Label minLengthLabel = new Label("Minimum word length: " + minLength);
			Label numSetsLabel = new Label("Number of sets: " + numSets);
			numSetsLabel.setTooltip(new Tooltip(100*Integer.parseInt(numSets) + " total tiles"));
			Label blankPenaltyLabel = new Label("Blank Penalty: " + blankPenalty);
			blankPenaltyLabel.setTooltip(new Tooltip("To use a blank, you must\ntake " + blankPenalty + " additional tile" + (blankPenalty > 1 ? "s" : "")));
			Label speedLabel = new Label("Speed: " + speed);
			if(speed.equals("slow"))
				speedLabel.setTooltip(new Tooltip("9 seconds per tile"));
			else if(speed.equals("medium"))
				speedLabel.setTooltip(new Tooltip("6 seconds per tile"));
			else
				speedLabel.setTooltip(new Tooltip("3 seconds per tile"));
			playersLabel.setText("Players: 0/" + maxPlayers);

			//join button
			Button joinButton = new Button("Join game");
			joinButton.setTooltip(new Tooltip(gameName.replaceAll("%", " ")));
			joinButton.setOnMouseClicked(e -> {
				if(gameWindows.isEmpty()) {
					if(players.size() < maxPlayers || gameOver && allowWatchers) {
						joinButton.setDisable(true);
						Platform.runLater(() -> {
							new GameWindow(AnagramsClient.this, gameID, gameName, username, minLength, blankPenalty, numSets, speed, allowChat, lexicon, gameLog, gameOver);
							send((gameOver ? "watchgame " : "joingame ") + gameID);
							joinButton.setDisable(false);
						});
					}
				}
			});

			//watch button
			Button watchButton = new Button("Watch");
			watchButton.setTooltip(new Tooltip(gameName.replaceAll("%", " ")));
			watchButton.setDisable(!allowWatchers);
			if(allowWatchers) {
				watchButton.setOnMouseClicked(e -> {
					if (gameWindows.isEmpty()) {
						if (!players.contains(username) || gameOver) {
							watchButton.setDisable(true);
							Platform.runLater(() -> {
								new GameWindow(AnagramsClient.this, gameID, gameName, username, minLength, blankPenalty, numSets, speed, allowChat, lexicon, gameLog, true);
								send("watchgame " + gameID);
								watchButton.setDisable(false);
							});
						}
					}
				});
			}

			add(joinButton, 0, 0);
			add(watchButton, 1, 0);
			add(lexiconLabel, 0, 1);
			add(minLengthLabel, 1,1 );
			add(numSetsLabel, 0, 2);
			add(blankPenaltyLabel, 1, 2);
			add(speedLabel, 0, 3);
			add(playersLabel, 1, 3);
			add(notificationLabel, 0, 4, 2,1);
			gamesPanel.getChildren().add(this);
		}

		/**
		 *
		 */
		private void addPlayerToGame(String newPlayer) {
			if(gameOver) return;
			players.add(newPlayer);
			setPlayersLabel();
			if (gameWindows.containsKey(gameID)) {
				gameWindows.get(gameID).addPlayer(newPlayer);
			}
		}

		/**
		 *
		 */
		private void removePlayerFromGame(String playerToRemove) {
			if(gameOver) return;
			players.remove(playerToRemove);
			setPlayersLabel();
			if (gameWindows.containsKey(gameID)) {
				gameWindows.get(gameID).removePlayer(playerToRemove);
			}
		}

		/**
		 *
		 */
		private void setNotificationLabel(String note) {
			notificationLabel.setText(note);
			if (gameWindows.containsKey(gameID)) {
				gameWindows.get(gameID).setNotificationArea(note);
			}
		}
		/**
		 *
		 */
		private void setPlayersLabel() {
			playersLabel.setText("Players: " + players.size() + "/" + maxPlayers);
			playersLabel.setTooltip(null);

			if(!players.isEmpty()) {
				String toolTipText = "";
				for(String player : players) {
					toolTipText = toolTipText.concat("\n" + player);
				}
				toolTipText = toolTipText.replaceFirst("\n", "");
				playersLabel.setTooltip(new Tooltip(toolTipText));
			}
		}

		/**
		 *
		 */
		GameWindow getGame() {
			return gameWindows.get(gameID);
		}

		/**
		 *
		 */
		private void endGame() {
			gameOver = true;

			GameWindow gameWindow = getGame();
			if(gameWindow != null) {
				gameWindow.gameLog = gameLog;
				gameWindow.endGame();
			}
		}
	}

	/**
	 * Adds a player to the playerList, updates the textArea, and plays a notification sound
	 *
	 * @param newPlayerName The name of the new player
	 */
	private void addPlayer(String newPlayerName) {

		if(playersList.containsKey(newPlayerName))
			return;

		if(prefs.getBoolean("play_sounds", true)) {
			newPlayerClip.play();
		}

		Label newLabel = new Label(newPlayerName);
		playersList.put(newPlayerName, newLabel);
		playersListPane.getChildren().add(newLabel);

		newLabel.setOnMouseClicked(click -> {
			playerPane.displayPlayerInfo(newPlayerName);
			if(!playerPane.isVisible()) {
				playerPane.setTranslateX(stage.getWidth() - 453);
				playerPane.setTranslateY(newLabel.getLayoutY() + 66);
				playerPane.setPrefSize(300, 240);
				playerPane.setMinSize(300, 140);
				playerPane.show(false);
			}
		});
	}

	/**
	 *
	 */
	private void removePlayer(String playerToRemove) {
		playersListPane.getChildren().remove(playersList.remove(playerToRemove));
		if(playerToRemove.equals(username)) {
			disconnect();
		}
	}

	/**
	 * Inform the server that the player is no longer an active part of the specified game.
	 *
	 * @param gameID the game to exit
	 * @param isWatcher whether the user is watching
	 */
	void exitGame(String gameID, boolean isWatcher) {
		gameWindows.clear();
		if(isWatcher)
			send("stopwatching " + gameID);
		else
			send("stopplaying " + gameID);
	}


	/**
	 * Reads commands from the server and stores them in a BlockingQueue to run later
	 */
	private void readMessageLoop() {
		System.out.println("reading messages");
		while(connected) {
			try {
				String line;
				while ((line = this.bufferedIn.readLine()) != null) {
					commandQueue.put(line);
				}
			}
			catch (Exception ex) {
				if (stage.isShowing()) {
					if (connected) {
						logOut();
					}
					MessageDialog dialog = new MessageDialog(this, "Connection error");
					dialog.setText("The connection to the server has been lost. Try to reconnect?");
					dialog.addYesNoButtons();
					dialog.yesButton.setOnAction(e -> getWebAPI().executeScript("window.location.reload(false)"));
					dialog.noButton.setOnAction(e -> getWebAPI().openURL("https://www.anagrams.site"));
					Platform.runLater(() -> dialog.show(true));
				}
			}
		}
	}

	/**
	 * Executes commands from the server
	 */
	private void executeCommandLoop() {
		AtomicBoolean blocked = new AtomicBoolean(false);
		while(connected) {
			try {
				if(blocked.get()) {
					continue;
				}
				blocked.set(true);
				final String line = commandQueue.take();
				final String[] tokens = line.split(" ");

				if (tokens.length == 0) continue;

				final String cmd = tokens[0];

//				if (!cmd.equals("note") && !cmd.equals("nexttiles"))
//					System.out.println("command received: " + line);

				Platform.runLater(() -> {
					switch (cmd) {
						case "alert" -> {
							MessageDialog dialog = new MessageDialog(this, "Alert");
							dialog.setText(line.split("@")[1]);
							dialog.addOkayButton();
							dialog.show(true);
						}
						case "loginplayer" -> addPlayer(tokens[1]);
						case "logoffplayer" -> removePlayer(tokens[1]);
						case "chat" -> {
							String msg = line.replaceFirst("chat ", "");
							chatBox.appendText("\n" + msg);
						}
						case "addgame" -> new GamePane(tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6],
								tokens[7], tokens[8], tokens[9], tokens[10], tokens[11]);
						case "removegame" -> gamesPanel.getChildren().remove(gamePanes.remove(tokens[1]));
						case "json" -> {
							if (explorer.isVisible())
								explorer.setUpTree(new JSONArray(line.replaceFirst("json ", "")));
						}

						//GamePane commands
						default -> {
							GamePane gamePane = gamePanes.get(tokens[1]);
							if (gamePane == null) break;
							switch (cmd) {
								case "takeseat" -> gamePane.addPlayerToGame(tokens[2]);
								case "removeplayer" -> gamePane.removePlayerFromGame(tokens[2]);
								case "note" -> gamePane.setNotificationLabel(line.split("@")[1]);
								case "endgame" -> gamePane.endGame();
								case "gamelog" -> gamePane.gameLog.add(Arrays.copyOfRange(tokens, 2, tokens.length));

								//GameWindow commands
								default -> {
									GameWindow gameWindow = gamePane.getGame();
									if (gameWindow == null) break;
									switch (cmd) {
										case "nexttiles" -> gameWindow.setTiles(tokens[2]);
										case "makeword" -> gameWindow.makeWord(tokens[2], tokens[3], tokens[4]);
										case "steal" -> gameWindow.doSteal(tokens[2], tokens[3], tokens[4], tokens[5], tokens[6]);
										case "abandonseat" -> gameWindow.removePlayer(tokens[2]);
										case "gamechat" -> gameWindow.handleChat(line.replaceFirst("gamechat " + tokens[1] + " ", ""));
										case "removeword" -> gameWindow.removeWord(tokens[2], tokens[3]);
										case "gamestate" -> gameWindow.showPosition(Arrays.copyOfRange(tokens, 2, tokens.length));
										case "plays" -> gameWindow.showPlays(line);

										default -> System.out.println("Command not recognized: " + line);
									}
								}
							}
						}
					}
					blocked.set(false);
				});
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Quietly disconnects the client and terminates the session.
	 */
	private void disconnect() {
		connected = false;
		try {
			if(messageLoop != null) messageLoop.interrupt();

			serverOut.close();
			serverIn.close();
			if(guest)
				prefs.removeNode();

			System.out.println(username + " has disconnected.");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Terminates the session and informs the server
	 */
	private void logOut() {
		if(connected) {
			for(GameWindow gameWindow : gameWindows.values())
				exitGame(gameWindow.gameID, gameWindow.isWatcher);
			send("logoff ");
		}
		disconnect();
	}


	/**
	 * Transmit a command to the server
	 *
	 * @param cmd The command to send
	 */
	void send(String cmd) {
		try {
			serverOut.write((cmd + "\n").getBytes());
		}
		catch (Exception e) {
			System.out.println("Command " + cmd + " not transmitted.");
		}
	}

}