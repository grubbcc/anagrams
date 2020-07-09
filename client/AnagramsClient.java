import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.sound.sampled.*;
import java.net.Socket;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultEditorKit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.InetAddress;
/**
*
*
*/

public class AnagramsClient extends JFrame implements ActionListener {


//	private final String serverName = "anagrams.mynetgear.com"; //connect over internet
//	private final String serverName = "192.168.0.14"; //connect over home network
	private final String serverName = "127.0.0.1"; //connect to this computer
	private final static int port = 8118;
	private final static int testPort = 8117;
	public final String version = "0.9.6";
	
	private Socket socket;
	private static ServerSocket lockSock;    	
	private InputStream serverIn;
	private OutputStream serverOut;
	private BufferedReader bufferedIn;
	
	private LoginWindow loginWindow;
	
	private JPanel controlPanel = new JPanel(new BorderLayout());
	private JButton createGameButton = new JButton("Create Game");
	private ImageIcon settingsIcon = new ImageIcon(getClass().getResource("settings.png"));	
	private JButton settingsButton = new JButton("Settings   ", settingsIcon);

	public String[] lexicons = {"CSW19", "NWL18", "LONG"};		
	private JPanel gamesPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
	private JPanel playersPanel = new JPanel(new BorderLayout());
	private JLabel playersHeader = new JLabel("Players logged in");
	private JTextArea playersTextArea = new JTextArea();
	private JPanel chatPanel = new JPanel(new BorderLayout());
	private JScrollPane chatScrollPane = new JScrollPane(chatPanel);
	private JTextArea chatBox = new JTextArea(5, 70);
	private JTextField chatField = new JTextField("Type here to chat", 70);

	Color mainScreenBackgroundColor;
	Color mainScreenBackgroundText; //unused
	Color chatAreaColor;
	Color chatAreaText;
	Color playersPanelColor;
	Color playersPanelText;
	Color gameForegroundColor;
	Color gameForegroundText;
	Color gameBackgroundColor;
	Color gameBackgroundText;
	
	private FocusListener fl = new FocusListener() {
		public void focusGained(FocusEvent e) {
			chatField.setText("");
			chatField.setForeground(Color.BLACK);
		}
		public void focusLost(FocusEvent e) {
		}
	};

	String workingDirectory;
	String OS = (System.getProperty("os.name")).toUpperCase();
	HashMap<String, String> settings = new HashMap<String, String>();
	String settingsPath;

	private HashMap<String, GameWindow> gameWindows = new HashMap<String, GameWindow>();
	private HashMap<String, GamePane> gamePanes = new HashMap<String, GamePane>();
	private LinkedHashMap<JPanel, ArrayList<String>> addresses = new LinkedHashMap<JPanel, ArrayList<String>>();
	
	public String username;
	private HashSet<String> playersList = new HashSet<String>();
	public HashMap<String, AlphagramTrie> dictionaries = new HashMap<String, AlphagramTrie>();


	
	/**
	* 
	*/

	public AnagramsClient(int port) {

		System.out.println("Welcome to Anagrams!");
		setSize(792, 500);
		setMinimumSize(new Dimension(792,400));
		if(port == 8117) setTitle("Anagrams (testing mode)");
		else setTitle("Anagrams");
		
		//default settings
		if (OS.contains("WIN")) {
			workingDirectory = System.getenv("AppData");
		}	
		else {
			workingDirectory = System.getProperty("user.home");
			workingDirectory += "/Library/Application Support";
		}
		settingsPath = workingDirectory + File.separator + "Anagrams" + File.separator + "settings.ser";
		settings = loadSettings();

		for(String lexicon : lexicons) {
			dictionaries.put(lexicon, null);
		}
		dictionaries.put(settings.get("lexicon"), new AlphagramTrie(settings.get("lexicon")));

		//exit the program when the user presses ESC or CTRL + W
		getRootPane().registerKeyboardAction(ae -> {send("logoff");}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		getRootPane().registerKeyboardAction(ae -> {send("logoff");}, KeyStroke.getKeyStroke("control W"), JComponent.WHEN_IN_FOCUSED_WINDOW);

		//control panel
		createGameButton.addActionListener(this);
		settingsButton.addActionListener(this);
		settingsButton.setPreferredSize(new Dimension(143, 33));
		settingsButton.setHorizontalTextPosition(SwingConstants.LEFT);
		controlPanel.add(settingsButton, BorderLayout.EAST);
		controlPanel.add(createGameButton, BorderLayout.CENTER);

		
		//games panel
		JScrollPane gamesScrollPane = new JScrollPane(gamesPanel);
		gamesPanel.setLayout(new BoxLayout(gamesPanel, BoxLayout.Y_AXIS));
		JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
		gamesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		gamesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);


		//players panel
		JScrollPane playersScrollPane = new JScrollPane(playersPanel);
		playersScrollPane.setPreferredSize(new Dimension(143, 1000));

		playersHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
		playersHeader.setBorder(new EmptyBorder(3, 3, 3, 3));
		playersHeader.setOpaque(false);

		playersTextArea.setEditable(false);
		playersTextArea.setOpaque(false);
		playersPanel.add(playersHeader, BorderLayout.NORTH);
		playersPanel.add(playersTextArea);
		
		//chat pane
		chatScrollPane.setPreferredSize(new Dimension(600, 100));
		chatBox.setLineWrap(true);
		chatBox.setEditable(false);
		chatField.setForeground(Color.GRAY);
		chatField.addFocusListener(fl);
		chatField.addActionListener(ae -> {send("chat " + username + ": " + chatField.getText()); chatField.setText("");});
		chatField.getActionMap().put(DefaultEditorKit.deletePrevCharAction, new CustomDeletePrevCharAction());
		chatField.setBorder(new EmptyBorder(1,1,1,1));
		chatField.setBackground(Color.LIGHT_GRAY);
		chatPanel.add(chatBox);
		chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum());		
		chatPanel.add(chatField, BorderLayout.SOUTH);

		//main panel
		setColors();
		getContentPane().add(controlPanel, BorderLayout.NORTH);
		getContentPane().add(gamesScrollPane, BorderLayout.CENTER);
		getContentPane().add(playersScrollPane, BorderLayout.EAST);
		getContentPane().add(chatScrollPane, BorderLayout.SOUTH);
        
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				send("logoff");
			}
		});

		setVisible(true);
		
		connect(serverName, port);
		checkVersion();
		loginWindow = new LoginWindow(this, getLocation().x, getLocation().y);
		startMessageReader();

	}

	/**
	*
	*/
	
	public static void main(String args[]) {
		
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				if(args.length == 0) {
					checkIfRunning();
					new AnagramsClient(port).setVisible(true);
				}
				else if(args[0].equals("test")) {
					new AnagramsClient(testPort).setVisible(true);
				}
			}
		});
	}
	
	/**
	* Prevents multiple connections from the same computer
	*/
	
	private static void checkIfRunning() {
		try {
			//Bind to localhost adapter with a zero connection queue 
			lockSock = new ServerSocket(8119, 0, InetAddress.getByAddress(new byte[] {127,0,0,1}));
		}
		catch (BindException e) {
			System.out.println("Already running.");
			JOptionPane.showMessageDialog(new JFrame(), "It appears you already have an instance of this application running.", "Anagrams already running", JOptionPane.INFORMATION_MESSAGE);
			System.exit(1);
		}
		catch (IOException e) {

			e.printStackTrace();
			System.exit(2);
		}
	}	
	
	/**
	*
	*/

	public void setColors() {

		mainScreenBackgroundColor = Color.decode(settings.get("Main screen"));
		mainScreenBackgroundText = Color.decode(settings.get("Main screen text")); //unused
		playersPanelColor = Color.decode(settings.get("Players list"));
		playersPanelText = Color.decode(settings.get("Players list text"));
		chatAreaColor = Color.decode(settings.get("Chat area"));
		chatAreaText = Color.decode(settings.get("Chat area text"));
		gameForegroundColor = Color.decode(settings.get("Game foreground"));
		gameForegroundText = Color.decode(settings.get("Game foreground text"));
		gameBackgroundColor = Color.decode(settings.get("Game background"));
		gameBackgroundText = Color.decode(settings.get("Game background text"));
		
		gamesPanel.setBackground(mainScreenBackgroundColor);
		chatBox.setBackground(chatAreaColor);
		chatBox.setForeground(chatAreaText);
		playersPanel.setBackground(playersPanelColor);
		playersHeader.setForeground(playersPanelText);
		playersTextArea.setForeground(playersPanelText);

		for(GamePane gamePane : gamePanes.values()) {
			gamePane.setBackground(gameForegroundColor);
		}
		
		for(GameWindow gameWindow : gameWindows.values()) {
			gameWindow.setColors();
		}
	}

	/**
	* 
	*/
	
	public void addGameWindow(String id, GameWindow g) {
		gameWindows.put(id, g);
	}
	
	/**
	*
	*/

	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == createGameButton) {
			if(gameWindows.size() < 4) { //maximum of 4 windows open at a time
				new GameMenu(this, getLocation().x + getWidth()/2, getLocation().y + getHeight()/2);
			}
		}
		else if(evt.getSource() == settingsButton) {
			new SettingsMenu(this, getLocation().x + getWidth()/2, getLocation().y + getHeight()/2);		
		}
	}
	
	/**
	* Displays information about games and tools for joining
	*/

	class GamePane extends JPanel {
		
		String gameID;
		int maxPlayers;
		boolean allowWatchers;
		boolean gameOver;
		
		JLabel lexiconLabel = new JLabel();
		JLabel minLengthLabel = new JLabel();
		JLabel numSetsLabel = new JLabel();
		JLabel blankPenaltyLabel = new JLabel();
		JLabel speedLabel = new JLabel();
		JLabel notificationLabel = new JLabel();
		JLabel playersLabel = new JLabel();
		
		ArrayList<String[]> gameLog = new ArrayList<String[]>();
		HashMap<String, Boolean> players = new HashMap<String, Boolean>();
		HashSet<String> watchers = new HashSet<String>();
		String toolTipText = "";
		AlphagramTrie dictionary = null;
		
		/**
		*
		*/
		
		GamePane(String gameID, String playerMax, String minLength, String numSets, String blankPenalty, String lexicon, String speed, String allowsChat, String allowsWatchers, String isOver) {

			this.gameID = gameID;
			gameOver = Boolean.parseBoolean(isOver);
			allowWatchers = Boolean.parseBoolean(allowsWatchers);
			maxPlayers = Integer.parseInt(playerMax);
			setBackground(gameForegroundColor);

			lexiconLabel.setText("Lexicon: " + lexicon);
			lexiconLabel.setForeground(gameForegroundText);
			if(lexicon.equals("CSW19")) 
				lexiconLabel.setToolTipText("Collins Official Scrabble Words \u00a9 2019");
			else if(lexicon.equals("NWL18")) 
				lexiconLabel.setToolTipText("NASPA Word List \u00a9 2018");
			
			minLengthLabel.setText("Minimum word length: " + minLength);
			minLengthLabel.setForeground(gameForegroundText);

			numSetsLabel.setText("Number of sets: " + numSets);
			numSetsLabel.setForeground(gameForegroundText);
			numSetsLabel.setToolTipText(100*Integer.parseInt(numSets) + " total tiles");			

			blankPenaltyLabel.setText("Blank Penalty: " + blankPenalty);	
			blankPenaltyLabel.setForeground(gameForegroundText);
			blankPenaltyLabel.setToolTipText("<html>To use a blank, you must<br>take " + blankPenalty + " additional tiles</html>");
			
			speedLabel.setText("Speed: " + speed);
			speedLabel.setForeground(gameForegroundText);
			if(speed.equals("slow"))
				speedLabel.setToolTipText("9 seconds per tile");
			else if(speed.equals("medium"))
				speedLabel.setToolTipText("6 seconds per tile");
			else
				speedLabel.setToolTipText("3 seconds per tile");
			
			notificationLabel.setForeground(gameForegroundText);

			playersLabel.setText("Players: 0/" + maxPlayers);
			playersLabel.setForeground(gameForegroundText);
			

			for(String key : dictionaries.keySet()) {
				if(key.equals(lexicon)) {
					dictionary = dictionaries.get(key);
					if(dictionary == null) {
						dictionary = new AlphagramTrie(key);
						dictionaries.put(key, dictionary);
					}
				}
			}

			setLayout(new GridLayout(5, 2, 10, 7));
			setPreferredSize(new Dimension(300, 140));
			setBorder(new EmptyBorder(2, 3, 2, 3));
		
			//join button
			JButton joinButton = new JButton("Join game");
			joinButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent evt) {
					if(!gameWindows.containsKey(gameID) && gameWindows.size() < 4) {
						if(players.size() < maxPlayers || gameOver && allowWatchers) {
							
							GameWindow newGame = new GameWindow(AnagramsClient.this, gameID, username, minLength, blankPenalty, allowsChat, dictionary, gameLog, gameOver);
							gameWindows.put(gameID, newGame);
							if(gameOver) {
								send("watchgame " + gameID);
							}
							else {
								send("joingame " + gameID);		
							}
						}
					}
				}
			});
			add(joinButton);	
			
			//watch button
			JButton watchButton = new JButton("Watch");
			watchButton.setEnabled(allowWatchers);
			if(allowWatchers) {
				watchButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent evt) {
						if(!gameWindows.containsKey(gameID) && gameWindows.size() < 4) {
							if(!players.containsKey(username) || gameOver) {

								GameWindow newGame = new GameWindow(AnagramsClient.this, gameID, username, minLength, blankPenalty, allowsChat, dictionary, gameLog, true);
								gameWindows.put(gameID, newGame);

								send("watchgame " + gameID);
							}
						}
					}
				});
			}
			add(watchButton);

			add(lexiconLabel);
			add(minLengthLabel);
			add(numSetsLabel);
			add(blankPenaltyLabel);
			add(speedLabel);
			add(playersLabel);
			add(notificationLabel);
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
			playersLabel.setToolTipText(null);
			
			if(!players.isEmpty()) {
				for(String player : players.keySet()) {
					toolTipText = toolTipText.concat("<br>" + player);
				}
				toolTipText = toolTipText.replaceFirst("<br>", "");
				playersLabel.setToolTipText("<html>" + toolTipText + "</html>");
			}			
		}
		
		
		/**
		* not currently used
		*/
		
		void addWatcher(String newWatcher) {
			watchers.add(newWatcher);
		}
		
		/*
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
	* Adds a new game to the gamePanel 
	* #This is probably way more convoluted than it needs to be
	* 
	* @param String newGameID 
	* @param GamePane newGamePane
	*/

	void addGame(String newGameID, GamePane newGamePane) {
		
		gamePanes.put(newGameID, newGamePane);
		
		boolean placed = false;
		
		//tries to place the newGamePane in an existing row
		for(JPanel row : addresses.keySet()) {
			if(addresses.get(row).size() < 2) {
				row.add(newGamePane);
				addresses.get(row).add(newGameID);
				placed = true;
				break;
			}
		}
		//creates a new row and puts the newGamePane in the first position
		if(!placed) {
		
			JPanel nextRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
			nextRow.setOpaque(false);
			addresses.put(nextRow, new ArrayList<String>(2));
			addresses.get(nextRow).add(newGameID);

			nextRow.add(newGamePane);
			gamesPanel.add(nextRow);
		}
		
		revalidate();
	}

	/**
	* Adds a player to the playerList, updates the textArea, and plays a notification sound
	*
	* @param String newPlayerName The name of the new player
	*/

	void addPlayer(String newPlayerName) {
		playersList.add(newPlayerName);
		String players = " ";
		for(String player : playersList)
			players += player + "\n ";
		playersTextArea.setText(players);
		if(isFocused()) {
			try {
				InputStream audioSource = getClass().getResourceAsStream("new player sound.wav");
				InputStream audioBuffer = new BufferedInputStream(audioSource);
				AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioBuffer);
				Clip clip = AudioSystem.getClip();
				clip.open(audioStream);
				clip.start();
			}
			catch(Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	/**
	* Inform the server that the player is no longer an active part of the specified game.
	*
	* @param String gameID the game to exit
	* @param boolean isWatcher whether the player is watching
	*/
	
	void exitGame(String gameID, boolean isWatcher) {

		gameWindows.remove(gameID);

		if(isWatcher) {
			send("stopwatching " + gameID);
		}
		else {
			send("stopplaying " + gameID);
		}
		
		getRootPane().requestFocus();
	}
	
	/**
	*
	*/
	
	public HashMap<String, String> loadSettings() {

		try {
			ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(settingsPath));

			@SuppressWarnings("unchecked")
			HashMap<String, String> input = (HashMap<String, String>)inputStream.readObject();
			settings = input;
		}
		catch (Exception e) {
			System.out.println("Settings file missing or damaged; using defaults.");

			settings.put("lexicon", "CSW19");
			settings.put("play sounds", "true");
			settings.put("Main screen", "#F5DEB3");
			settings.put("Main screen text", "#000000");
			settings.put("Players list", "#B36318");
			settings.put("Players list text", "#000000");
			settings.put("Game foreground", "#2080AA");
			settings.put("Game foreground text", "#000000");
			settings.put("Game background", "#F5DEB3");
			settings.put("Game background text", "#000000");
			settings.put("Chat area", "#00FFFF");
			settings.put("Chat area text", "#000000");
		}
		return settings;
    }


	/**
	*
	*/
	
	public boolean connect(String serverName, int serverPort) {
		try {
			Socket socket = new Socket(serverName, serverPort);
			this.serverOut = socket.getOutputStream();
			this.serverIn = socket.getInputStream();
			this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
			return true;

		}
		catch (IOException e) {

			if (JOptionPane.showConfirmDialog(this, "The program is having trouble connecting to the host server. Try again?", "Connection issues", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				connect(serverName, serverPort);
			}
			else {
				System.exit(0);
			}
		}
		
		return false;
	}
	
	/**
	* Verifies that the user is using a up-to-date version of the client software.
	* Directs the user to the project homepage if they are not.
	*/
	
	public void checkVersion() {
		try {
			send("version " + version);
			String response = this.bufferedIn.readLine();
			
			if("ok version".equalsIgnoreCase(response)) {
				return;
			}
			else if("outdated".equalsIgnoreCase(response)) {
				JOptionPane.showMessageDialog(this, new HypertextMessage("An updated version of Anagrams available. <br> Please visit <a href=\"https://www.seattlephysicstutor.com/anagrams.html\">the project home page</a> to get the latest features."), "Warning", JOptionPane.INFORMATION_MESSAGE);
			}				
			else if("unsupported".equalsIgnoreCase(response)) {
				JOptionPane.showMessageDialog(this, new HypertextMessage("You are using an out-of-date version of Anagrams which is no longer supported. <br> Please visit <a href=\"https://www.seattlephysicstutor.com/anagrams.html\">the project home page</a> to download the latest version."), "Warning", JOptionPane.WARNING_MESSAGE);

				System.exit(0);
			}
		}
		catch (IOException e) {

			if (JOptionPane.showConfirmDialog(this, "The program is having trouble connecting to the host server. Try again?", "Connection issues", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				connect(serverName, port);
			}
			else {
				System.exit(0);
			}
		}		
	}
	
	/**
	*
	*/

	public boolean login(String username) throws IOException {

		this.username = username;
		send("login " + username);
		String response = this.bufferedIn.readLine();
		
		//successful login
		if("ok login".equals(response)) {
			System.out.println(username + " has just logged in.");
			return true;
		}

		//login was unsuccessful
		else {
			JOptionPane.showMessageDialog(this, response);
			return false;
		}
	}
	
	/**
	*
	*/
	
	private void startMessageReader() {
		Thread t = new Thread() {
			@Override
			public void run() {
				readMessageLoop();
			}
		};
		t.start();
	}
	
	/**
	* Respond to commands from the server
	*/
	
	private void readMessageLoop() {
		try {
			String line;
			while( (line = this.bufferedIn.readLine()) != null) {

				String[] tokens = line.split(" ");
				if (tokens != null && tokens.length > 0) {

					String cmd = tokens[0];
					if(!cmd.equals("note")) {
						System.out.println("command received: " + line);
					}
					if(cmd.equals("note")) {
						if(gamePanes.get(tokens[1]) != null) {
							gamePanes.get(tokens[1]).notificationLabel.setText(line.split("@")[1]);
						}
						if(gameWindows.get(tokens[1]) != null) {
							gameWindows.get(tokens[1]).setNotificationArea(line.split("@")[1]);
						}
					}
					else if(cmd.equals("alert")) {

						JOptionPane.showMessageDialog(this, new HypertextMessage(line.split("@")[1]), "Warning", JOptionPane.WARNING_MESSAGE);
					}
					else if(cmd.equals("loginplayer")) {
						addPlayer(tokens[1]);
					}
					else if(cmd.equals("logoffplayer")) {
						playersList.remove(tokens[1]);
						String players = " ";
						for(String player : playersList)
							players += player + "\n ";
						playersTextArea.setText(players);
						getContentPane().revalidate();
					}
					else if(cmd.equals("chat")) {
						handleChat(line.replaceFirst("chat ", ""));
					}
					else if(cmd.equals("addgame")) {
						addGame(tokens[1], new GamePane(tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6], tokens[7], tokens[8], tokens[9], tokens[10]));
					}
					else if (cmd.equals("logoff")) {
						bufferedIn.close();
						break;
					}	
					else if(cmd.equals("removegame")) {
						outerloop:
						for(JPanel row : addresses.keySet()) {
							for(String gameID : addresses.get(row)) {
								if(gameID.equals(tokens[1])) {
									addresses.get(row).remove(gameID);
									row.remove(gamePanes.remove(tokens[1]));
									if(addresses.get(row).isEmpty()) {
										gamesPanel.remove(row);
										addresses.remove(row);
									}
									break outerloop;
								}
							}
						}
						gamesPanel.repaint();
					}
					
					//gamePane commands
					else if(cmd.equals("takeseat")) {
						if(gamePanes.containsKey(tokens[1])) {
							gamePanes.get(tokens[1]).addPlayer(tokens[2]);
						}
						if(gameWindows.get(tokens[1]) != null) {
							if(!gameWindows.get(tokens[1]).gameOver) {
								gameWindows.get(tokens[1]).addPlayer(tokens[2]);
							}
						}
					}
					else if(cmd.equals("removeplayer")) {
						if(gamePanes.containsKey(tokens[1])) {
							if(!gamePanes.get(tokens[1]).gameOver) {
								gamePanes.get(tokens[1]).removePlayer(tokens[2]);
							}
						}
						if(gameWindows.get(tokens[1]) != null) {
							if(!gameWindows.get(tokens[1]).gameOver) {
								gameWindows.get(tokens[1]).removePlayer(tokens[2]);
							}
						}
					}
					else if(cmd.equals("watchgame")) {
						if(gamePanes.get(tokens[1]) != null) {
							gamePanes.get(tokens[1]).addWatcher(tokens[2]);
						}
					}
					else if(cmd.equals("unwatchgame")) {
						if(gamePanes.get(tokens[1]) != null) {
							gamePanes.get(tokens[1]).removeWatcher(tokens[2]);
						}
					}
					else if(cmd.equals("endgame")) {
						if(gamePanes.get(tokens[1]) != null) {
							gamePanes.get(tokens[1]).endGame();
						}
					}
					else if(cmd.equals("gamelog")) {
						if(gamePanes.get(tokens[1]) != null) {
							gamePanes.get(tokens[1]).gameLog.add(Arrays.copyOfRange(tokens, 2, tokens.length));
						}
					}

					//gameWindow commands
					else if(cmd.equals("nexttiles")) {
						if(gameWindows.get(tokens[1]) != null) {
							gameWindows.get(tokens[1]).setTiles(tokens[2]);
						}
					}
					else if(cmd.equals("addword")) {
						if(gameWindows.get(tokens[1]) != null) {
							gameWindows.get(tokens[1]).addWord(tokens[2], tokens[3]);
						}
					}
					else if(cmd.equals("removeword")) {
						if(gameWindows.get(tokens[1]) != null) {
							gameWindows.get(tokens[1]).removeWord(tokens[2], tokens[3]);
						}
					}
					else if (cmd.equals("abandonseat")) {
						if(gameWindows.get(tokens[1]) != null) {
							gameWindows.get(tokens[1]).removePlayer(tokens[2]);
						}
					}
					else if(cmd.equals("gamechat")) {
						if(gameWindows.get(tokens[1]) != null) {							
							gameWindows.get(tokens[1]).handleChat((line.split(tokens[1]))[1]);
						}
					}
					else {
						System.out.println("Command " + cmd + " not recognized");
					}
				}
			}
		}
		catch (Exception ex) {
			System.out.println("The connection between client and host has been lost. Now logging out.");
			JOptionPane.showMessageDialog(this, "The connection to the server has been lost. Exiting program.", "Connection error", JOptionPane.INFORMATION_MESSAGE);
			ex.printStackTrace();
			System.exit(0);
		}
		finally {
			logOut();
		}
	}
	
	/**
	* Closes the connection to the server and terminates the program.
	*/
	
	public void logOut() {
		
		try {
			serverIn.close();
			serverOut.close();

			System.out.println(username + " has just logged out.");
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		dispose();
		System.exit(0);
	}
	
	/**
	* Displays the new chat message (and sender) in the chat box and automatically scrolls to view
	*
	* @param String msg The chat message to display
	*/
	
	public void handleChat(String msg) {
		chatBox.append("\n" + msg);
		if(chatField.hasFocus()) {
			chatField.removeFocusListener(fl);
		}	
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum());
			}
		});
	}
	
	
	/**
	* Transmit a command to the server
	*
	* @param String cmd The command to send
	*/
	
	void send(String cmd) {
		try {
			serverOut.write((cmd + "\n").getBytes());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}

