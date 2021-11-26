package client;

import com.jpro.webapi.JProApplication;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import one.jpro.sound.*;
import org.json.JSONArray;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

/**
 *
 *
 */

public class AnagramsClient extends JProApplication {

	private final int port = 8118;
	private final String version = "0.9.9";

	private final AtomicBoolean connected = new AtomicBoolean(false);
	private InputStream serverIn;
	private OutputStream serverOut;
	BufferedReader bufferedIn;

	WeakReference<Stage> stage;
	private Thread messageLoop;

	WordExplorer explorer;

	private final FlowPane gamesPanel = new FlowPane();
	private final ScrollPane gamesScrollPane = new ScrollPane();

	private final Label playersHeader = new Label("Players logged in");
	private final VBox playersListPane = new VBox();
	private final BorderPane borderPane = new BorderPane();
	private final BorderPane playersPanel = new BorderPane();
	private final ScrollPane playersScrollPane = new ScrollPane();

	private final SplitPane splitPane = new SplitPane();
	WeakReference<AnchorPane> anchor = new WeakReference<>(new AnchorPane(splitPane));
	WeakReference<StackPane> stack = new WeakReference<>(new StackPane(anchor.get()));

	private final TextArea chatBox = new TextArea();
	private final BorderPane chatPanel = new BorderPane();
	private final ScrollPane chatScrollPane = new ScrollPane();
	private final PlayerPane playerPane = new PlayerPane(this);

	WeakHashMap<String, GameWindow> gameWindows = new WeakHashMap<>();
	private WeakHashMap<String, GamePane> gamePanes = new WeakHashMap<>();
	private final WeakHashMap<String, Label> playersList = new WeakHashMap<>();
	public String username;

	public static final String[] lexicons = {"CSW19", "NWL20"};
	Preferences prefs;

	final EnumMap<Colors, String> colors = new EnumMap<>(Colors.class);
	private final String newPlayerSound = getClass().getResource("/sounds/new player sound.wav").toExternalForm();
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
		this.stage = new WeakReference<>(stage);
		System.out.println("Welcome to Anagrams!");

		newPlayerClip = AudioClip.getAudioClip(newPlayerSound, stage);
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
		createGameButton.setOnAction(e -> {if(gameWindows.size() < 1) new GameMenu(this);});

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
		playersPanel.setPrefWidth(162);
		playersPanel.setId("players-panel");
		playersScrollPane.setFitToHeight(true);
		playersScrollPane.setFitToWidth(true);
		playersScrollPane.setContent(playersListPane);
		playersPanel.setTop(playersHeader);
		playersPanel.setCenter(playersScrollPane);
		playersHeader.setTooltip(new Tooltip("Click a player's name to view profile"));

		//chat panel
		chatBox.setEditable(false);
		TextField chatField = new TextField();
		chatField.setStyle("-fx-font-size: " + (getWebAPI().isMobile() ? 18 : 16) + ";");
		chatField.setPromptText("Type here to chat");
		chatField.setOnAction(ae -> {send("chat " + username + ": " + chatField.getText()); chatField.clear();});
		chatPanel.setBottom(chatField);
		chatScrollPane.setFitToHeight(true);
		chatScrollPane.setFitToWidth(true);
		chatScrollPane.setContent(chatBox);
		chatBox.setStyle("-fx-font-size: " + (getWebAPI().isMobile() ? 18 : 16) + ";");
		chatBox.appendText("Welcome to Anagrams version " + version + "!");
		chatPanel.setCenter(chatScrollPane);

		//main layout
		borderPane.setTop(controlPanel);
		borderPane.setCenter(gamesScrollPane);
		borderPane.setRight(playersPanel);
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

/*		stage.addEventFilter(TouchEvent.TOUCH_PRESSED, e -> {
			if(e.getTarget() instanceof LabeledText) {
				Label label = (Label)e.getTarget();
				if(label.getTooltip() != null)
					label.getTooltip().show(stage);
			}
		});*/

		anchor.get().setMinSize(Double.MIN_VALUE, Double.MIN_VALUE);
		anchor.get().setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		Scene scene;
		try {
			scene = new Scene(stack.get());
		}
		catch(IllegalArgumentException e) {
			getWebAPI().executeScript("window.location.reload(false)");
			return;
		}
		scene.getStylesheets().add(getClass().getResource("/anagrams.css").toExternalForm());

		//main stage
		stage.get().setTitle("Anagrams");

		if(getWebAPI().isMobile()) {
			gamesPanel.getTransforms().add(new Scale(1.25, 1.25));
			getWebAPI().registerJavaFunction("setWidth", newWidth -> {
				stage.get().setWidth(Double.parseDouble(newWidth));
			});
			getWebAPI().registerJavaFunction("setHeight", newHeight -> {
				stage.get().setHeight(Double.parseDouble(newHeight));
			});
		}

		stage.get().setScene(scene);
		setColors();
		stage.get().show();

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

		stage.get().getScene().getRoot().setStyle(newStyle);
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

        return luminance > 43 ? "black" : "white";
    }

	/**
	 * Connect to the AnagramsServer instance running on the local host
	 */

	public boolean connect() {
		try {
			Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), port);
			this.serverOut = socket.getOutputStream();
			this.serverIn = socket.getInputStream();
			this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
			connected.set(true);
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
		explorer = new WordExplorer(prefs.get("lexicon", "CSW19"), this);

		//set user colors
		for (Colors color : Colors.values())
			colors.put(color, prefs.get(color.key, color.defaultCode));
		setColors();

		messageLoop = new Thread(this::readMessageLoop);
		messageLoop.start();
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
			"At the end of game (or while watching a game), you can click on any word to see how it can be stolen.",
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
						gameWindows.put(gameID, new GameWindow(AnagramsClient.this, gameID, username, minLength, blankPenalty, allowChat, lexicon, gameLog, gameOver));
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
					    gameWindows.put(gameID, new GameWindow(AnagramsClient.this, gameID, username, minLength, blankPenalty, allowChat, lexicon, gameLog, true));
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
		playersListPane.getChildren().add(newLabel);

		newLabel.setOnMouseClicked(click -> {
			playerPane.displayPlayerInfo(newPlayerName);
			if(!playerPane.isVisible()) {
				playerPane.setTranslateX(stage.get().getWidth() - 453);
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
		while(connected.get()) {
			try {
				String line;
				while ((line = this.bufferedIn.readLine()) != null) {

					String[] tokens = line.split(" ");
					if (tokens.length > 0) {

						String cmd = tokens[0];

//						if(!cmd.equals("note") && !cmd.equals("nexttiles"))
//								System.out.println("command received: " + line);

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
								case "json" -> {
									if (explorer.isVisible()) explorer.setUpTree(new JSONArray(finalLine.substring(5)));
								}

								//gamePane commands
								default -> {
									GamePane gamePane = gamePanes.get(tokens[1]);
									if (gamePane != null) {
										switch (cmd) {
											case "takeseat" -> {
												gamePane.addPlayer(tokens[2]);
												if (gameWindows.containsKey(tokens[1]))
													if (!gameWindows.get(tokens[1]).gameOver)
														gameWindows.get(tokens[1]).addPlayer(tokens[2]);
											}
											case "removeplayer" -> {
												gamePane.removePlayer(tokens[2]);
												if (gameWindows.containsKey(tokens[1])) {
													gameWindows.get(tokens[1]).removePlayer(tokens[2]);
												}
											}
											case "note" -> {
												gamePane.notificationLabel.setText(finalLine.split("@")[1]);
												if (gameWindows.containsKey(tokens[1])) {
													gameWindows.get(tokens[1]).setNotificationArea(finalLine.split("@")[1]);
												}
											}
											case "endgame" -> gamePane.endGame();
											case "gamelog" -> gamePane.gameLog.add(Arrays.copyOfRange(tokens, 2, tokens.length));

											//gameWindow commands
											default -> {
												GameWindow gameWindow = gameWindows.get(tokens[1]);
												if (gameWindow != null) {
													switch (cmd) {
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

			} catch (Exception ex) {
				if (stage.get().isShowing()) {
					if (connected.get()) {
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
	 * Closes the connection to the server and terminates the program.
	 */

	public void logOut() {
		connected.set(false);
		try {
			if(messageLoop != null) messageLoop.interrupt();

			send("logoff");
			serverOut.close();
			serverIn.close();
			if(guest)
				prefs.removeNode();

			System.out.println(username + " has just logged out.");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			//Prepare for garbage collection
			gameWindows.values().forEach(GameWindow::exitGame);
			gamePanes.values().forEach(GamePane::endGame);

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
		catch (Exception e) {
			System.out.println("Command " + cmd + " not transmitted.");
		}
	}

}
