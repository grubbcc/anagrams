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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 */
class AnagramsClient extends JProApplication {

	private final String host = InetAddress.getLocalHost().getHostAddress();
	private final int port = 8118;
	private static final String version = "1.0.2";

	private boolean connected = false;
	private InputStream serverIn;
	private OutputStream serverOut;
	BufferedReader bufferedIn;

	Stage stage;

	WordExplorer explorer;
	final Stack<PopWindow> popWindows = new Stack<>();

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

	private final Button settingsButton = new Button("Settings", new ImageView("/images/settings.png"));
	private SettingsMenu settingsMenu;
	private final TextField chatField = new TextField();
	private final TextArea chatBox = new TextArea();
	private final BorderPane chatPanel = new BorderPane();
	private final PlayerPane playerPane = new PlayerPane(this);

	//GameWindow gameWindow;
	final HashMap<String, GameWindow> gameWindows = new HashMap<>();
	private final HashMap<String, GamePane> gamePanes = new HashMap<>();
	final HashMap<String, Player> playersList = new HashMap<>();

	String username;
	boolean guest = true;
	JSONObject prefs;

	static final String[] lexicons = {"CSW21", "NWL20"}; //change to enum
	final EnumMap<Colors, String> colors = new EnumMap<>(Colors.class);
	private final String newPlayerSound = getClass().getResource("/sounds/new player sound.mp3").toExternalForm();
	private final String newGameSound = getClass().getResource("/sounds/new game sound.mp3").toExternalForm();
	private AudioClip newPlayerClip;
	private AudioClip newGameClip;


	private final LinkedBlockingQueue<JSONObject> commandQueue = new LinkedBlockingQueue<>();

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
		GAME_CHAT ("-game-chat", "game_chat", "Game Chat", "#002868");

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

		settingsButton.setStyle("-fx-font-size: 18");
		settingsButton.setPrefSize(162, 33);

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
				send("chat", new JSONObject().put("msg", username + ": " + msg));
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

		stage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if(connected && e.getCode().equals(KeyCode.ESCAPE)) {
				if (!popWindows.isEmpty()) {
					if(popWindows.peek() instanceof LoginMenu) return;
					popWindows.pop().hide();
				}
			}
		});

		//main stage
		stage.setTitle("Anagrams");

		getWebAPI().registerJavaFunction("logOff", event -> logOut());

		if(getWebAPI().isMobile()) {
			gamesPanel.getTransforms().add(new Scale(1.25, 1.25));
			getWebAPI().registerJavaFunction("setWidth", newWidth ->
				stage.setWidth(Double.parseDouble(newWidth))
			);
			getWebAPI().registerJavaFunction("setHeight", newHeight ->
				stage.setHeight(Double.parseDouble(newHeight))
			);
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
			dialog.show(true);
			return false;
		}

	}

	/**
	 *
	 */
	void login(String username, boolean guest, JSONObject prefs) {
		this.username = username;
		this.guest = guest;
		this.prefs = prefs;

		explorer = new WordExplorer(prefs.getString("lexicon"), this);

		//set user colors
		for (Colors color : Colors.values())
			colors.put(color, prefs.getString(color.key));
		setColors();

		settingsMenu = new SettingsMenu(this);
		settingsButton.setOnAction(e -> {if(!settingsMenu.isVisible()) settingsMenu.show(false);});

		new Thread(this::readMessageLoop).start();
		new Thread(this::executeCommandLoop).start();

		borderPane.setDisable(false);
		splitPane.setDisable(false);

		if(prefs.getBoolean("show_guide")) {
			showStartupGuide();
		}
	}

	/**
	 * Creates and displays a MessageDialog with information on how to play and use the application
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
			if(!continueShowing.isSelected()) {
				send("updateprefs", new JSONObject().put("type", "guide"));
			}
			dialog.hide();
		});
		dialog.show(true);
	}


	/**
	 * Displays information about a game and tools for joining
	 */
	private class GamePane extends GridPane {

		private final String gameID;
		private final int maxPlayers;
		private final boolean allowWatchers;
		private boolean gameOver;

		private final Label notificationLabel = new Label();
		private final Label playersLabel = new Label();

		private JSONArray gameLog;
		private final HashSet<String> players = new HashSet<>();

		/**
		 *
		 */
		private GamePane(JSONObject gameParams) {
			gameID = gameParams.getString("gameID");
			final String gameName = gameParams.getString("game_name");
			allowWatchers = gameParams.getBoolean("allow_watchers");
			maxPlayers = gameParams.getInt("max_players");
			final int blankPenalty = gameParams.getInt("blank_penalty");
			final String speed = gameParams.getString("speed");
			final String lexicon = gameParams.getString("lexicon");
			final int minLength = gameParams.getInt("min_length");
			final int numSets = gameParams.getInt("num_sets");
			final boolean rated = gameParams.getBoolean("rated");

			gamePanes.put(gameID, this);

			if(prefs.getBoolean("play_sounds")) {
				newGameClip.play();
			}
			getStyleClass().setAll("game-panel");

			//labels
			Label lexiconLabel = new Label("Lexicon: " + lexicon);
			if(lexicon.equals("CSW21"))
				lexiconLabel.setTooltip(new Tooltip("Collins Official Scrabble Words © 2021"));
			else if(lexicon.equals("NWL20"))
				lexiconLabel.setTooltip(new Tooltip("NASPA Word List © 2020"));
			Label minLengthLabel = new Label("Minimum word length: " + minLength);
			Label numSetsLabel = new Label("Number of sets: " + numSets);
			numSetsLabel.setTooltip(new Tooltip(100*numSets + " total tiles"));
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
			Button joinButton = new Button("Join " + (rated && !gameOver ? "(rated)" : "game"));
			joinButton.setTooltip(new Tooltip(gameName));
			joinButton.setOnMouseClicked(e -> {
				if(gameWindows.isEmpty()) {
					if(players.size() < maxPlayers || gameOver && allowWatchers) {
						joinButton.setDisable(true);
						new GameWindow(AnagramsClient.this, gameParams, username, gameOver, gameLog);
						send("joingame", new JSONObject().put("gameID", gameID));
						Platform.runLater(() -> joinButton.setDisable(false));
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
							new GameWindow(AnagramsClient.this, gameParams, username, true, gameLog);
                            send("watchgame", new JSONObject().put("gameID", gameID));
							Platform.runLater(() -> watchButton.setDisable(false));
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
		private void addPlayerToGame(String newPlayer, String newPlayerRating) {
			players.add(newPlayer);
			updatePlayersLabel();
			if (gameWindows.containsKey(gameID)) {
				gameWindows.get(gameID).addPlayer(newPlayer, newPlayerRating);
			}
		}

		/**
		 *
		 */
		private void removePlayerFromGame(String playerToRemove) {
			players.remove(playerToRemove);
			updatePlayersLabel();
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
		private void updatePlayersLabel() {
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
		private void endGame(JSONArray gameLog) {
			gameOver = true;
			this.gameLog = gameLog;
			notificationLabel.setText("Game over");

			GameWindow gameWindow = getGame();
			if(gameWindow != null) {
				gameWindow.endGame(gameLog);
			}
		}
	}

	/**
	 * Adds a player to the playerList, updates the textArea, and plays a notification sound
	 *
	 * @param data The data of the new player
	 */
	private void addPlayer(JSONObject data) {

		String name = data.getString("name");
		if(playersList.containsKey(name)) {
			playersList.get(name).rating = data.getString("rating");
			playersList.get(name).profile = data.getString("profile");
			return;
		}

		if(name.equals(username))
			Platform.runLater(() -> chatBox.scrollTopProperty().set(0));

		if(prefs.getBoolean("play_sounds"))
			newPlayerClip.play();

		String rating = data.getString("rating");
		String profile = data.getString("profile");
		Player newPlayer = new Player(name, rating, profile);
		playersList.put(name, newPlayer);

		Label newLabel = newPlayer.label;
		newLabel.setOnMouseClicked(click -> {
			playerPane.displayPlayerInfo(playersList.get(name));
			if(!playerPane.isVisible()) {
				playerPane.setTranslateX(stage.getWidth() - 453);
				playerPane.setTranslateY(newLabel.getLayoutY() + 66);
				playerPane.setPrefSize(300, 240);
				playerPane.setMinSize(300, 140);
				playerPane.show(false);
			}
		});
		playersListPane.getChildren().add(newLabel);
	}

	/**
	 *
	 */
	static class Player {

		final String name;
		String rating;
		String profile;
		Label label;

		Player(String name, String rating) {
			this.name = name;
			this.rating = rating;
		}

		Player(String name, String rating, String profile) {
			this(name, rating);
			this.profile = profile;
			label = new Label(name);
		}
	}

	/**
	 *
	 */
	private void updateRatings(JSONArray ratings) {
		for(int i = 0; i < ratings.length(); i++) {
			JSONObject datum = ratings.getJSONObject(i);
			Optional<Player> player = Optional.ofNullable(playersList.get(datum.getString("name")));
			player.ifPresent(p -> p.rating = datum.getString("rating"));
		}
	}

	/**
	 *
	 */
	private void removePlayer(String playerToRemove) {
		Player removedPlayer = playersList.remove(playerToRemove);
		playersListPane.getChildren().remove(removedPlayer.label);
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
            send("stopwatching", new JSONObject().put("gameID", gameID));
		else
            send("stopplaying", new JSONObject().put("gameID", gameID));
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
					try {
						commandQueue.put(new JSONObject(line));
					}
					catch(JSONException je) {
						System.out.println("Malformed JSON expression:\n" + line);
					}
				}
			}
			catch (IOException | InterruptedException ex) {
				if (connected) {
					logOut();
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
				if(blocked.get()) continue;
				blocked.set(true);

				final JSONObject json = commandQueue.take();
				final String cmd = json.getString("cmd");

				if (!cmd.equals("note") && !cmd.equals("nexttiles") && !cmd.equals("chat"))
					System.out.println("command received: " + json);

				Platform.runLater(() -> {
					switch (cmd) {
						case "addgame" -> new GamePane(json);
						case "alert" -> {
							MessageDialog dialog = new MessageDialog(this, "Alert");
							dialog.setText(json.getString("msg"));
							dialog.addOkayButton();
							dialog.show(true);
						}
						case "chat" -> chatBox.appendText("\n" + json.getString("msg"));
						case "logoffplayer" -> removePlayer(json.getString("name"));
						case "ratings" -> updateRatings(json.getJSONArray("ratings"));
						case "removegame" -> gamesPanel.getChildren().remove(gamePanes.remove(json.getString("gameID")));
						case "tree" -> {
							if (explorer.isVisible())
								explorer.setUpTree(json.getJSONArray("data"));
						}
						case "userdata" -> addPlayer(json);

						//GamePane commands
						default -> {
							String gameID = json.optString("gameID");
							if(gameID.isEmpty()) break;
							GamePane gamePane = gamePanes.get(gameID);
							if(gamePane == null) break;

							switch (cmd) {
								case "endgame" -> gamePane.endGame(json.getJSONArray("gamelog"));
								case "note" -> gamePane.setNotificationLabel(json.getString("msg"));
								case "removeplayer" -> gamePane.removePlayerFromGame(json.getString("name"));
								case "takeseat" -> gamePane.addPlayerToGame(json.getString("name"), json.getString("rating"));

								//GameWindow commands
								default -> {
									GameWindow gameWindow = gamePane.getGame();
									if (gameWindow == null) break;
									switch (cmd) {
										case "abandonseat" -> gameWindow.removePlayer(json.getString("name"));
										case "gamechat" -> gameWindow.handleChat(json.getString("msg"));
										case "gamestate" -> gameWindow.showPosition(json);
										case "makeword" -> gameWindow.makeWord(json.getString("player"), json.getString("word"), json.getString("tiles"));
										case "nexttiles" -> gameWindow.setTiles(json.getString("tiles"));
										case "plays" -> gameWindow.showPlays(json.getJSONObject("data"));
										case "steal" -> gameWindow.doSteal(json.getString("shortPlayer"), json.getString("shortWord"), json.getString("longPlayer"),json.getString("longWord"),json.getString("tiles"));

										default -> System.out.println("Command not recognized: " + cmd);
									}
								}
							}
						}
					}
					blocked.set(false); 		//is this working?
				});
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		logOut();
	}

	/**
	 * Quietly disconnects the client and terminates the session.
	 */
	private void disconnect() {
		connected = false;
		try {
			serverOut.close();
			serverIn.close();

			System.out.println(username + " has disconnected.");
			if (stage.isShowing()) {

				Platform.runLater(() -> {
					MessageDialog dialog = new MessageDialog(this, "Connection error");
					dialog.setText("The connection to the server has been lost. Try to reconnect?");
					dialog.addYesNoButtons();
					dialog.yesButton.setOnAction(e -> getWebAPI().executeScript("window.location.reload(false)"));
					dialog.noButton.setOnAction(e -> getWebAPI().openURL("https://www.anagrams.site"));
					dialog.show(true);
				});
			}

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
			send("logoff");
			disconnect();
			Platform.runLater(() -> getWebAPI().closeInstance());
		}
	}


	/**
	 * Transmits a command to the server
	 * @param cmd The command to send
	 * @param json The payload of data
	 */
	void send(String cmd, JSONObject json) {

		try {
			serverOut.write((json.put("cmd", cmd) + "\n").getBytes());
		}
		catch (Exception e) {
			System.out.println("Command not transmitted: \n" + json);
		}
	}

	/**
	 * Transmits a simple command ot the server which consists an empty JSONObject
	 * containing just the command.
	 * @param cmd The command to send
	 */
	void send(String cmd) {
		send(cmd, new JSONObject());
	}

}