package client;

import java.io.*;
import java.net.*;
import java.util.*;
import com.jpro.webapi.JProApplication;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.prefs.Preferences;

/**
 *
 *
 */

public class AnagramsClient extends JProApplication {

	//	private final String serverName = "anagrams.mynetgear.com"; //connect over internet
	//	private final String serverName = "192.168.0.17"; //connect over home network
	    private final String serverName = "127.0.0.1"; //connect to this computer

	private int port;
	public final String version = "0.9.7";

	boolean connected;
	private InputStream serverIn;
	private OutputStream serverOut;
	private BufferedReader bufferedIn;

	Stage stage;
	private Thread messageLoop;

	private LoginWindow loginWindow;
	private final FlowPane gamesPanel = new FlowPane();
	private final ScrollPane gamesScrollPane = new ScrollPane();

	private final Label playersHeader = new Label("Players logged in");
	private final VBox playersListPane = new VBox();
	final BorderPane borderPane = new BorderPane();
	private final BorderPane playersPanel = new BorderPane();
	private final ScrollPane playersScrollPane = new ScrollPane();

	final AnchorPane anchor = new AnchorPane(borderPane);
	final StackPane stack = new StackPane(anchor);

	private final TextArea chatBox = new TextArea();
	private final BorderPane chatPanel = new BorderPane();
	private final ScrollPane chatScrollPane = new ScrollPane();

	private SettingsMenu settingsMenu;
	final HashMap<String, GameWindow> gameWindows = new HashMap<>();
	private final HashMap<String, GamePane> gamePanes = new HashMap<>();
	private final HashSet<String> playersList = new HashSet<>();
	public String username;

	public static final String[] lexicons = {"CSW19", "NWL20"};
	public final HashMap<String, AlphagramTrie> dictionaries = new HashMap<>();

	//load settings
	Preferences prefs = Preferences.userNodeForPackage(getClass());
	EnumMap<Colors, String> colors = new EnumMap<>(Colors.class);

	/**
	 *
	 */

	public enum Colors {

		MAIN_SCREEN ("-main-screen", "mainScreen", "Main Screen", "#F5DEB3"),
		PLAYERS_LIST ("-players-list", "playersList", "Players List", "#B36318"),
		GAME_FOREGROUND ("-game-foreground", "gameForeground", "Game Foreground", "#2080AA"),
		GAME_BACKGROUND ("-game-background", "gameBackground", "Game Background", "#F5DEB3"),
		CHAT_AREA ("-chat-area", "chatArea", "Chat Area", "#00FFFF"),
		;

		final String css;
		final String camel;
		final String display;
		final String defaultCode;

		Colors(String css, String camel, String display, String defaultCode) {
			this.css = css;
			this.camel = camel;
			this.display = display;
			this.defaultCode = defaultCode;
		}
	}

	/**
	 *
	 */

	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Is this necessary?
	 */

	@Override
	public void init() {
		port = 8117;
	}

	/**
	 *
	 */

	@Override
	public void start(Stage stage) {
		this.stage = stage;
		System.out.println("Welcome to Anagrams!");

		for(Colors color : Colors.values()) {
			colors.put(color, color.defaultCode);
		}

		createAndShowGUI();
		if(connect() ) {
			System.out.println("Connected to server on port " + port);
			loginWindow = new LoginWindow(this);
			loginWindow.show(true);
		}
	}

	/**
	 *
	 */

	private void createAndShowGUI() {
		//control panel
		Button createGameButton = new Button("Create Game");
		createGameButton.setPrefHeight(36);

		createGameButton.setOnAction(e -> new GameMenu(this));
		Image settingsImage = new Image(getClass().getResourceAsStream("/settings.png"), 30, 30, true ,true);
		Button settingsButton = new Button("Settings", new ImageView(settingsImage));
		settingsButton.setPrefSize(143, 33);
		settingsButton.setOnAction(e -> settingsMenu.show(false));
		HBox controlPanel = new HBox();
		controlPanel.setFillHeight(true);
        createGameButton.prefWidthProperty().bind(controlPanel.widthProperty().subtract(143));
		controlPanel.getChildren().addAll(createGameButton, settingsButton);

		//games panel
		gamesPanel.setId("games-panel");
		gamesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
		gamesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		gamesScrollPane.setFitToHeight(true);
		gamesScrollPane.setFitToWidth(true);
		gamesScrollPane.setContent(gamesPanel);

		//players panel
		playersHeader.setFont(new Font("Arial Bold", 16));
		playersPanel.setPrefWidth(143);
		playersPanel.setId("players-panel");
		playersScrollPane.setFitToHeight(true);
		playersScrollPane.setFitToWidth(true);
		playersScrollPane.setContent(playersListPane);
		playersPanel.setTop(playersHeader);
		playersPanel.setCenter(playersScrollPane);

		//chat panel
		chatBox.setEditable(false);
		TextField chatField = new TextField();
		chatField.setPromptText("Type here to chat");
		chatField.setOnAction(ae -> {send("chat " + username + ": " + chatField.getText()); chatField.clear();});
		chatPanel.setBottom(chatField);
		chatPanel.setPrefHeight(100);
		chatScrollPane.setFitToHeight(true);
		chatScrollPane.setFitToWidth(true);
		chatScrollPane.setContent(chatBox);
		chatPanel.setCenter(chatScrollPane);

		//main layout
		borderPane.setTop(controlPanel);
		borderPane.setCenter(gamesScrollPane);
		borderPane.setRight(playersPanel);
		borderPane.setBottom(chatPanel);
        borderPane.setDisable(true);

        anchor.setMinSize(Double.MIN_VALUE, Double.MIN_VALUE);
		anchor.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        AnchorPane.setTopAnchor(borderPane, 0.0);
		AnchorPane.setRightAnchor(borderPane, 0.0);
		AnchorPane.setBottomAnchor(borderPane, 0.0);
		AnchorPane.setLeftAnchor(borderPane, 0.0);

		Scene scene = new Scene(stack);
		scene.getStylesheets().add(getClass().getResource("/anagrams.css").toExternalForm());

		//main stage
		if(port == 8117) stage.setTitle("Anagrams (testing mode)");
		else stage.setTitle("Anagrams");
		stage.setMinWidth(792);
		stage.setMinHeight(400);
		stage.setScene(scene);

		setColors();
		stage.show();

		getWebAPI().addInstanceCloseListener(() -> {System.out.println("closing instance");  logOut();});

	}

	/**
	 *
	 */

	public void setColors() {

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

        return luminance > 40 ? "black" : "white";
    }

	/**
	 *
	 */

	public boolean connect() {
		try {
			Socket socket = new Socket(serverName, port);
			this.serverOut = socket.getOutputStream();
			this.serverIn = socket.getInputStream();
			this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
			connected = true;
			return true;
		}
		catch (IOException ioe) {
			System.out.println("Unable to connect to server on port " + port);

			MessageDialog dialog = new MessageDialog(this, "Connection issues");
			dialog.setText("The program is having trouble connecting to the host server.");
			dialog.addOkayButton();
			dialog.show(true);
			return false;
		}
	}


	/**
	 *
	 */

	public boolean login(String username) throws IOException {
		this.username = username;
		send("login " + username);
		String response = "";
		try {
			response = this.bufferedIn.readLine();
		}
		catch(IOException ioe) {
			System.out.println("response not received");
			MessageDialog dialog = new MessageDialog(this, "Connection error");
			dialog.setText("The connection to the server has been lost. Exiting program.");
			dialog.addOkayButton();
			dialog.show(true);
		}

		//successful login
		if ("ok login".equals(response)) {
			System.out.println(username + " has just logged in.");

			//set user preferences
			String lexicon = prefs.get(username + "/LEXICON", "CSW19");
			dictionaries.put(lexicon, new AlphagramTrie(lexicon));
			for(Colors color : Colors.values())
				colors.put(color, prefs.get(username + "/" + color.toString(), color.defaultCode));
			setColors();
			settingsMenu = new SettingsMenu(this);

			messageLoop = new Thread(this::readMessageLoop);
			messageLoop.start();
			if (Thread.currentThread().isInterrupted()) {
				messageLoop = null;
				System.out.println("Thread + " + Thread.currentThread().toString() + " interrupted!");
				if(connected) logOut();
			}
			loginWindow.hide();
            borderPane.setDisable(false); //is this necessary?
			return true;
		}

		//login was unsuccessful
		else {
			MessageDialog dialog = new MessageDialog(this, "Login unsuccessful");
			dialog.setText(response);
			dialog.addOkayButton();
			dialog.show(true);
			return false;
		}
	}


	/**
	 * Displays information about games and tools for joining
	 */

	class GamePane extends GridPane {

		String gameID;
		int maxPlayers;
		boolean allowWatchers;
		boolean allowChat;
		boolean gameOver;

		Label lexiconLabel = new Label();
		Label minLengthLabel = new Label();
		Label numSetsLabel = new Label();
		Label blankPenaltyLabel = new Label();
		Label speedLabel = new Label();
		Label notificationLabel = new Label();
		Label playersLabel = new Label();

		ArrayList<String[]> gameLog = new ArrayList<>();
		HashMap<String, Boolean> players = new HashMap<>();
		HashSet<String> watchers = new HashSet<>();
		String toolTipText = "";
		AlphagramTrie dictionary = null;

		/**
		 *
		 */

		GamePane(String gameID, String playerMax, String minLength, String numSets, String blankPenalty, String lexicon, String speed, String allowsChat, String allowsWatchers, String isOver) {

			this.gameID = gameID;
            gamePanes.put(gameID, this);
			gameOver = Boolean.parseBoolean(isOver);
			allowWatchers = Boolean.parseBoolean(allowsWatchers);
			allowChat = Boolean.parseBoolean(allowsChat);
			maxPlayers = Integer.parseInt(playerMax);

			//labels
			lexiconLabel.setText("Lexicon: " + lexicon);
			if(lexicon.equals("CSW19"))
				lexiconLabel.setTooltip(new Tooltip("Collins Official Scrabble Words \u00a9 2019"));
			else if(lexicon.equals("NWL20"))
				lexiconLabel.setTooltip(new Tooltip("NASPA Word List \u00a9 2020"));
			minLengthLabel.setText("Minimum word length: " + minLength);
			numSetsLabel.setText("Number of sets: " + numSets);
			numSetsLabel.setTooltip(new Tooltip(100*Integer.parseInt(numSets) + " total tiles"));
			blankPenaltyLabel.setText("Blank Penalty: " + blankPenalty);
			blankPenaltyLabel.setTooltip(new Tooltip("To use a blank, you must\ntake " + blankPenalty + " additional tiles"));
			speedLabel.setText("Speed: " + speed);
			if(speed.equals("slow"))
				speedLabel.setTooltip(new Tooltip("9 seconds per tile"));
			else if(speed.equals("medium"))
				speedLabel.setTooltip(new Tooltip("6 seconds per tile"));
			else
				speedLabel.setTooltip(new Tooltip("3 seconds per tile"));
			playersLabel.setText("Players: 0/" + maxPlayers);

			for(String key : dictionaries.keySet()) {
				if(key.equals(lexicon)) {
					dictionary = dictionaries.get(key);
					if(dictionary == null) {
						dictionary = new AlphagramTrie(key);
						dictionaries.put(key, dictionary);
					}
				}
			}

			//join button
			Button joinButton = new Button("Join game");
			joinButton.setOnAction(e -> {
                if(!gameWindows.containsKey(gameID) && gameWindows.size() < 1) {
                    if(players.size() < maxPlayers || gameOver && allowWatchers) {

                        GameWindow newGame = new GameWindow(AnagramsClient.this, gameID, username, minLength, blankPenalty, allowChat, dictionary, gameLog, gameOver);
                        gameWindows.put(gameID, newGame);
                        if(gameOver) {
                            send("watchgame " + gameID);
                        }
                        else {
                            send("joingame " + gameID);
                        }
                    }
                }
			});

			//watch button
			Button watchButton = new Button("Watch");
			watchButton.setDisable(!allowWatchers);
			if(allowWatchers) {
				watchButton.setOnAction(e -> {
                    if(!gameWindows.containsKey(gameID) && gameWindows.size() < 1) {
                        if(!players.containsKey(username) || gameOver) {

                            GameWindow newGame = new GameWindow(AnagramsClient.this, gameID, username, minLength, blankPenalty, allowChat, dictionary, gameLog, true);
                            gameWindows.put(gameID, newGame);

                            send("watchgame " + gameID);
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

		void addPlayer(String newPlayer) {
			players.put(newPlayer, true);
			setPlayersToolTip();
		}

		/**
		 *
		 */

		void removePlayer(String playerToRemove) {
			players.remove(playerToRemove);
			setPlayersToolTip();
		}

		/**
		 *
		 */

		void setPlayersToolTip() {
			playersLabel.setText("Players: " + players.size() + "/" + maxPlayers);
			toolTipText = "";
			playersLabel.setTooltip(null);

			if(!players.isEmpty()) {
				for(String player : players.keySet()) {
					toolTipText = toolTipText.concat("\n" + player);
				}
				toolTipText = toolTipText.replaceFirst("\n", "");
				playersLabel.setTooltip(new Tooltip(toolTipText));
			}
		}


		/**
		 * not currently used
		 */

		void addWatcher(String newWatcher) {
			watchers.add(newWatcher);
		}

		/**
		 * not currently used
		 */

		void removeWatcher(String watcherToRemove) {
			watchers.remove(watcherToRemove);
		}

		/**
		 *
		 */

		void endGame() {
			gameOver = true;
			if(gameWindows.get(gameID) != null) {
				gameWindows.get(gameID).gameLog = gameLog;
				gameWindows.get(gameID).endGame();
			}
		}
	}

	/**
	 * Adds a player to the playerList, updates the textArea, and plays a notification sound
	 *
	 * @param newPlayerName The name of the new player
	 */

	void addPlayer(String newPlayerName) {
		playersList.add(newPlayerName);
		playersListPane.getChildren().clear();
		for(String player : playersList) {
			playersListPane.getChildren().add(new Label(player));
		}
		if(prefs.getBoolean(username + "/PLAY_SOUNDS", true)) {
			new AudioClip(this.getClass().getResource("/new player sound.wav").toExternalForm()).play();
		}
	}

	/**
	 * Inform the server that the player is no longer an active part of the specified game.
	 *
	 * @param gameID the game to exit
	 * @param isWatcher whether the player is watching
	 */

	void exitGame(String gameID, boolean isWatcher) {

		gameWindows.remove(gameID);
		if(isWatcher)
			send("stopwatching " + gameID);
		else
			send("stopplaying " + gameID);

	}


	/**
	 * Respond to commands from the server
	 */

	private void readMessageLoop() {
		System.out.println("reading messages");
		try {
			String line;
			while((line = this.bufferedIn.readLine()) != null) {

				String[] tokens = line.split(" ");
				if (tokens.length > 0) {

					String cmd = tokens[0];
					if(!cmd.equals("note") && !cmd.equals("nexttiles")) {
						System.out.println("command received: " + line);
					}
					String finalLine = line;
					Platform.runLater(() -> {
						switch (cmd) {
							case "note":
								String note = finalLine.split("@")[1];
								if (gamePanes.get(tokens[1]) != null)
									gamePanes.get(tokens[1]).notificationLabel.setText(note);

								if (gameWindows.get(tokens[1]) != null)
									gameWindows.get(tokens[1]).setNotificationArea(note);
								break;

							case "alert":
								MessageDialog dialog = new MessageDialog(this, "Alert");
								dialog.setText(finalLine.split("@")[1]);
								dialog.addOkayButton();
								dialog.show(true);
								break;

							case "loginplayer":
								addPlayer(tokens[1]);
								break;

							case "logoffplayer":
								playersList.remove(tokens[1]);
								playersListPane.getChildren().clear();
								for (String player : playersList)
									playersListPane.getChildren().add(new Label(player));
								break;

							case "chat":
								String msg = finalLine.replaceFirst("chat ", "");
								if(chatBox.getText().isEmpty()) chatBox.appendText(msg);
								else chatBox.appendText("\n" + msg);
								break;

							case "addgame":
								new GamePane(tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6],
										tokens[7],tokens[8], tokens[9], tokens[10]);
								break;

							case "removegame":
								gamesPanel.getChildren().remove(gamePanes.remove(tokens[1]));
								break;

							//gamePane commands
							case "takeseat":
								if (gamePanes.containsKey(tokens[1]))
									gamePanes.get(tokens[1]).addPlayer(tokens[2]);
								if (gameWindows.get(tokens[1]) != null)
									if (!gameWindows.get(tokens[1]).gameOver)
										gameWindows.get(tokens[1]).addPlayer(tokens[2]);
								break;

							case "removeplayer":
								if (gamePanes.containsKey(tokens[1]))
									if (!gamePanes.get(tokens[1]).gameOver)
										gamePanes.get(tokens[1]).removePlayer(tokens[2]);
								if (gameWindows.get(tokens[1]) != null)
									if (!gameWindows.get(tokens[1]).gameOver)
										gameWindows.get(tokens[1]).removePlayer(tokens[2]);
								break;

							case "watchgame":
								if (gamePanes.get(tokens[1]) != null)
									gamePanes.get(tokens[1]).addWatcher(tokens[2]);
								break;

							case "unwatchgame":
								if (gamePanes.get(tokens[1]) != null)
									gamePanes.get(tokens[1]).removeWatcher(tokens[2]);
								break;

							case "endgame":
								if (gamePanes.get(tokens[1]) != null)
									gamePanes.get(tokens[1]).endGame();
								break;

							case "gamestate":
								if (gameWindows.get(tokens[1]) != null) {
								//	String[] gameState = Arrays.copyOfRange(tokens, 2, tokens.length);
									gameWindows.get(tokens[1]).showPosition(Arrays.copyOfRange(tokens, 2, tokens.length));

								}
								break;

							case "gamelog":
								if (gamePanes.get(tokens[1]) != null)
									gamePanes.get(tokens[1]).gameLog.add(Arrays.copyOfRange(tokens, 2, tokens.length));
								break;

							//gameWindow commands
							case "nexttiles":
								if (gameWindows.get(tokens[1]) != null)
									gameWindows.get(tokens[1]).setTiles(tokens[2]);
								break;

							case "addword":
								if (gameWindows.get(tokens[1]) != null)
									gameWindows.get(tokens[1]).addWord(tokens[2], tokens[3]);
								break;

							case "makeword":
								if (gameWindows.get(tokens[1]) != null)
									gameWindows.get(tokens[1]).makeWord(tokens[2], tokens[3], tokens[4]);
								break;

							case "steal":
								if (gameWindows.get(tokens[1]) != null)
									gameWindows.get(tokens[1]).doSteal(tokens[2], tokens[3], tokens[4], tokens[5], tokens[6]);
								break;

							case "abandonseat":
								if (gameWindows.get(tokens[1]) != null)
									gameWindows.get(tokens[1]).removePlayer(tokens[2]);
								break;

							case "gamechat":
								if (gameWindows.get(tokens[1]) != null)
									gameWindows.get(tokens[1]).handleChat((finalLine.split(tokens[1]))[1]);
								break;

							case "removeword":
								if (gameWindows.get(tokens[1]) != null)
									gameWindows.get(tokens[1]).removeWord(tokens[2], tokens[3]);
								break;

							default:
								System.out.println("Command " + cmd + " not recognized");
								break;
						}
					});
				}
			}
		}
		catch (Exception ex) {
     //       ex.printStackTrace();
			System.out.println("The connection between client and host has been lost.");
			if(stage.isShowing()) {
				if(connected) {
					logOut();
				}
				MessageDialog dialog = new MessageDialog(this, "Connection error");
				dialog.setText("The connection to the server has been lost. Exiting program.");
				dialog.addOkayButton();
				Platform.runLater(() -> dialog.show(true));
			}
		}
	}


	/**
	 * Closes the connection to the server and terminates the program.
	 */

	public void logOut() {

		try {
			if(messageLoop != null) messageLoop.interrupt();
			send("logoff");
			System.out.println(username + " has just logged out.");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
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
		catch (IOException e) {
			System.out.println("Command " + cmd + " not transmitted.");
		}
	}

}