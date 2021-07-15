package client;

import com.jpro.webapi.JProApplication;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import one.jpro.sound.*;               //figure thsi out

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.prefs.Preferences;

/**
 *
 *
 */

public class AnagramsClient extends JProApplication {

    private final String serverName = "127.0.0.1";
	private final int port = 8118;
	private final String version = "0.9.8";

	boolean connected;
	private InputStream serverIn;
	private OutputStream serverOut;
	BufferedReader bufferedIn;

	Stage stage;
	private Thread messageLoop;

	private LoginMenu loginMenu;
	private final FlowPane gamesPanel = new FlowPane();
	private final ScrollPane gamesScrollPane = new ScrollPane();

	private final Label playersHeader = new Label("Players logged in");
	private final VBox playersListPane = new VBox();
	private final BorderPane borderPane = new BorderPane();
	private final BorderPane playersPanel = new BorderPane();
	private final ScrollPane playersScrollPane = new ScrollPane();

	final SplitPane splitPane = new SplitPane();
	final AnchorPane anchor = new AnchorPane(splitPane);
	final StackPane stack = new StackPane(anchor);

	private final TextArea chatBox = new TextArea();
	private final BorderPane chatPanel = new BorderPane();
	private final ScrollPane chatScrollPane = new ScrollPane();
	private final PlayerPane playerPane = new PlayerPane(this);

	private SettingsMenu settingsMenu;
	final WordExplorer explorer = new WordExplorer(null, this);
	final HashMap<String, GameWindow> gameWindows = new HashMap<>();
	private final HashMap<String, GamePane> gamePanes = new HashMap<>();
	private final HashMap<String, Label> playersList = new HashMap<>();
	public String username;

	public static final String[] lexicons = {"CSW19", "NWL20"};

	Preferences prefs;
	final EnumMap<Colors, String> colors = new EnumMap<>(Colors.class);
	private final String newPlayerSound = getClass().getResource("/new player sound.wav").toExternalForm();
	private AudioClip newPlayerClip;
	boolean guest = false;

	/**
	 *
	 */

	public enum Colors {

		MAIN_SCREEN ("-main-screen", "main_screen", "Main Screen", "#f5deb3"),
		PLAYERS_LIST ("-players-list", "players_list", "Players List", "#b36318"),
		GAME_FOREGROUND ("-game-foreground", "game_foreground", "Game Foreground", "#2080aa"),
		GAME_BACKGROUND ("-game-background", "game_background", "Game Background", "#f5deb3"),
		CHAT_AREA ("-chat-area", "chat_area", "Chat Area", "#00ffff"),
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

	public static void main(String[] args) {
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
		for(Colors color : Colors.values()) {
			colors.put(color, color.defaultCode);
		}

		createAndShowGUI();

		if(connect() ) {
			System.out.println("Connected to server on port " + port);
			if(loginMenu == null) {
				loginMenu = new LoginMenu(this);
				loginMenu.show(true);
			}
		}
	}

	/**
	 *
	 */

	private void createAndShowGUI() {
		//control panel
		Button createGameButton = new Button("Create Game");
		createGameButton.setPrefHeight(39);

		createGameButton.setOnAction(e -> {if(gameWindows.size() < 1) new GameMenu(this);});
		Button settingsButton = new Button("Settings", new ImageView("/settings.png"));
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
	//	chatPanel.setPrefHeight(100);
		chatScrollPane.setFitToHeight(true);
		chatScrollPane.setFitToWidth(true);
		chatScrollPane.setContent(chatBox);

	/*	Clipboard clipboard = Clipboard.getSystemClipboard();
		ClipboardContent content = new ClipboardContent();
		ContextMenu contextMenu = new ContextMenu();
		MenuItem copyItem = new MenuItem("Copy");
		copyItem.setOnAction(e -> {
			content.putString(chatBox.getSelectedText());
			clipboard.setContent(content);
		});
		MenuItem selectAllItem = new MenuItem("Select All");
		selectAllItem.setOnAction(e -> chatBox.selectAll());
		contextMenu.getItems().addAll(copyItem, selectAllItem);*/
		chatBox.appendText("Welcome to Anagrams version " + version + "!");

		chatPanel.setCenter(chatScrollPane);

		//main layout
		borderPane.setTop(controlPanel);
		borderPane.setCenter(gamesScrollPane);
		borderPane.setRight(playersPanel);
		borderPane.setMinHeight(300);

		splitPane.setOrientation(Orientation.VERTICAL);
		splitPane.getItems().addAll(borderPane, chatPanel);
		splitPane.setDividerPosition(0, 0.9);

		AnchorPane.setRightAnchor(splitPane, 0.0);
		AnchorPane.setBottomAnchor(splitPane, 0.0);
		AnchorPane.setLeftAnchor(splitPane, 0.0);
		AnchorPane.setTopAnchor(splitPane, 0.0);

        borderPane.setDisable(true);
        splitPane.setDisable(true);

        anchor.setMinSize(Double.MIN_VALUE, Double.MIN_VALUE);
		anchor.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        AnchorPane.setTopAnchor(borderPane, 0.0);
		AnchorPane.setRightAnchor(borderPane, 0.0);
		AnchorPane.setBottomAnchor(borderPane, 0.0);
		AnchorPane.setLeftAnchor(borderPane, 0.0);

		Scene scene;
		try {
			scene = new Scene(stack);
		}
		catch(IllegalArgumentException e) {
			System.out.println("Scene already exists on account of server restart");
			scene = stack.getScene();
		}
		scene.getStylesheets().add(getClass().getResource("/anagrams.css").toExternalForm());

		//main stage
		stage.setTitle("Anagrams");
		stage.setMinWidth(792);
		stage.setMinHeight(400);
		stage.setScene(scene);

		setColors();
		stage.show();

		getWebAPI().addInstanceCloseListener(this::logOut);
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

	public void login(String username) {
		this.username = username;

		prefs = Preferences.userNodeForPackage(getClass()).node(username);

		//set user colors
		for (Colors color : Colors.values())
			colors.put(color, prefs.get(color.key, color.defaultCode));
		setColors();

		settingsMenu = new SettingsMenu(this);
		messageLoop = new Thread(this::readMessageLoop);
		messageLoop.start();
		borderPane.setDisable(false);
		splitPane.setDisable(false);
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
		HashSet<String> players = new HashSet<>();
		String toolTipText = "";

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


			//join button
			Button joinButton = new Button("Join game");
			joinButton.setOnAction(e -> {
                if(!gameWindows.containsKey(gameID) && gameWindows.size() < 1) {
                    if(players.size() < maxPlayers || gameOver && allowWatchers) {

                        GameWindow newGame = new GameWindow(AnagramsClient.this, gameID, username, minLength, blankPenalty, allowChat, lexicon, gameLog, gameOver);
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
                        if(!players.contains(username) || gameOver) {
                            GameWindow newGame = new GameWindow(AnagramsClient.this, gameID, username, minLength, blankPenalty, allowChat, lexicon, gameLog, true);
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
			players.add(newPlayer);
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

		if(prefs.getBoolean("play_sounds", true)) {
			newPlayerClip.play();
		}

		Label newLabel = new Label(newPlayerName);
		playersList.put(newPlayerName, newLabel);
		playersListPane.getChildren().add(newPlayerName.equals(username) ? 0 : 1, newLabel);

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

	void removePlayer(String playerToRemove) {
		playersListPane.getChildren().remove(playersList.remove(playerToRemove));
	}

	/**
	 *
	 */


	/**
	 * Inform the server that the player is no longer an active part of the specified game.
	 *
	 * @param gameID the game to exit
	 * @param isWatcher whether the user is watching
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
	/*				if(!cmd.equals("note") && !cmd.equals("nexttiles")) {
						System.out.println("command received: " + line);
					}*/

					String finalLine = line;

					Platform.runLater(() -> {
						switch (cmd) {
							//other commands
							case "alert" -> {
								MessageDialog dialog = new MessageDialog(this, "Alert");
								dialog.setText(finalLine.split("@")[1]);
								dialog.addOkayButton();
								dialog.show(true);
							}
							case "loginplayer" -> addPlayer(tokens[1]);
							case "logoffplayer" -> removePlayer(tokens[1]);
							case "chat" -> {
								String msg = finalLine.replaceFirst("chat ", "");
								chatBox.appendText("\n" + msg);
							}
							case "addgame" -> new GamePane(tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6],
									tokens[7], tokens[8], tokens[9], tokens[10]);
							case "removegame" -> gamesPanel.getChildren().remove(gamePanes.remove(tokens[1]));
							case "json" -> {if (explorer.isVisible()) explorer.setUpTree(finalLine.substring(5));}
							case "def" -> {if (explorer.isVisible()) explorer.showDefinition(finalLine.replaceFirst("def ", ""));}

							//gamePane commands
							default -> {
								GamePane gamePane = gamePanes.get(tokens[1]);
								if(gamePane != null) {
									switch(cmd) {
										case "takeseat" -> {
											gamePane.addPlayer(tokens[2]);
											if(gameWindows.containsKey(tokens[1]))
												if (!gameWindows.get(tokens[1]).gameOver)
													gameWindows.get(tokens[1]).addPlayer(tokens[2]);
										}
										case "removeplayer" -> {
											gamePane.removePlayer(tokens[2]);
											if(gameWindows.containsKey(tokens[1])) {
												gameWindows.get(tokens[1]).removePlayer(tokens[2]);
											}
										}
										case "note" -> {
											gamePane.notificationLabel.setText(finalLine.split("@")[1]);
											if(gameWindows.containsKey(tokens[1])) {
												gameWindows.get(tokens[1]).setNotificationArea(finalLine.split("@")[1]);
											}
										}
										case "endgame" -> gamePane.endGame();
										case "gamelog" -> gamePane.gameLog.add(Arrays.copyOfRange(tokens, 2, tokens.length));

										//gameWindow commands
										default -> {
											GameWindow gameWindow = gameWindows.get(tokens[1]);
											if(gameWindow != null) {
												switch(cmd) {
													case "nexttiles" -> gameWindow.setTiles(tokens[2]);
													case "makeword" -> gameWindow.makeWord(tokens[2], tokens[3], tokens[4]);
													case "steal" -> gameWindow.doSteal(tokens[2], tokens[3], tokens[4], tokens[5], tokens[6]);
													case "abandonseat" -> gameWindow.removePlayer(tokens[2]);
													case "gamechat" -> gameWindow.handleChat(finalLine.replaceFirst("gamechat " + tokens[1] + " ", ""));
													case "removeword" -> gameWindow.removeWord(tokens[2], tokens[3]);
													case "gamestate" -> gameWindow.showPosition(Arrays.copyOfRange(tokens, 2, tokens.length));
													case "plays" -> gameWindow.showPlays(finalLine);

													default -> System.out.println("Command not recognized: " + finalLine);
												}
											}
										}
									}
								}
							}
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
				dialog.setText("The connection to the server has been lost. Try to reconnect?");
				dialog.addYesNoButtons();
				dialog.yesButton.setOnAction(e -> getWebAPI().executeScript("window.location.reload(false)"));
				dialog.noButton.setOnAction(e -> dialog.hide());
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
			serverOut.close();
			serverIn.close();
			if(guest)
				prefs.removeNode();
			connected = false;
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