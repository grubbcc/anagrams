/***
*
* To do: 
* animate steals
* read a default lexicon from file and load on startup
* show time remaining and names of players on game panes
*/

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Vector;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AnagramsClient extends JFrame implements ActionListener {

	private final String serverName = "anagrams.mynetgear.com"; //connect over internet
//	private final String serverName = "192.168.0.11"; //connect over home network
//	private final String serverName = "localhost"; //connect to this computer
	private final static int port = 8118;
	private final static int testPort = 8117;
	public final String version = "0.9.1";
	
	private Socket socket;
	private InputStream serverIn;
	private OutputStream serverOut;
	private BufferedReader bufferedIn;
	
	private LoginWindow loginWindow;
	private GameMenu menu;
	
	private JButton createGameButton = new JButton("Create Game");
	private JPanel gamesPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
	private JPanel playersPanel = new JPanel(new BorderLayout());
	private JTextArea playersTextArea = new JTextArea();
	private JPanel chatPanel = new JPanel(new BorderLayout());
	private JScrollPane chatScrollPane = new JScrollPane(chatPanel);
	private JTextArea chatBox = new JTextArea(5, 100);

	private HashMap<String, GameWindow> gameWindows = new HashMap<String, GameWindow>();
	private HashMap<String, JPanel> gamePanes = new HashMap<String, JPanel>();
	private LinkedHashMap<JPanel, Vector<String>> addresses = new LinkedHashMap<JPanel, Vector<String>>();
	
	public String username;
	private ArrayList<String> playersList = new ArrayList<String>();
	public AlphagramTrie dictionary;
	public HashMap<String, AlphagramTrie> dictionaries = new HashMap<String, AlphagramTrie>();

	/**
	*
	*/
	
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				if(args.length == 0) {
					new AnagramsClient(port).setVisible(true);
				}
				else if(args[0].equals("test")) {
					new AnagramsClient(testPort).setVisible(true);
				}

			}
		});
	}
	
	/**
	* 
	*/

	public AnagramsClient(int port) {
		System.out.println("Welcome to Anagrams!");
		setSize(800, 500);
		if(port == 8117) setTitle("Anagrams (testing mode)");
		else setTitle("Anagrams");
		setBackground(Color.BLUE);

		//exit the program when the user presses ESC or CTRL + W
		getRootPane().registerKeyboardAction(ae -> {send("logoff");}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
		getRootPane().registerKeyboardAction(ae -> {send("logoff");}, KeyStroke.getKeyStroke("control W"), JComponent.WHEN_FOCUSED);
		
		createGameButton.addActionListener(this);
		menu = new GameMenu(this, getLocation().x + getWidth()/2, getLocation().y + getHeight()/2);
		
		//games pane
        JScrollPane gamesScrollPane = new JScrollPane(gamesPanel);
		gamesPanel.setLayout(new BoxLayout(gamesPanel, BoxLayout.Y_AXIS));
		JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
		gamesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		gamesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		gamesPanel.setBackground(Color.BLUE);

		//players pane
		JScrollPane playersScrollPane = new JScrollPane(playersPanel);
		playersPanel.setBackground(Color.GREEN);
		JLabel playersHeader = new JLabel("Players logged in");
		playersHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
		playersHeader.setBorder(new EmptyBorder(3, 3, 3, 3));
		playersTextArea.setBackground(Color.GREEN);
		playersTextArea.setEditable(false);
		playersPanel.add(playersHeader, BorderLayout.NORTH);
		playersPanel.add(playersTextArea);
		
		//chat pane
		chatPanel.setBackground(Color.WHITE);
		chatBox.setLineWrap(true);
		chatBox.setEditable(false);
		JTextField chatField = new JTextField("", 100);		
		chatField.addActionListener(ae -> {send("chat " + username + ": " + chatField.getText()); chatField.setText("");});
		chatField.setBorder(new EmptyBorder(1,1,1,1));
		chatField.setBackground(Color.LIGHT_GRAY);
		chatPanel.add(chatBox);
		handleChat("<------------------------------------------------------------------------Chat------------------------------------------------------------------------>");


		chatPanel.add(chatField, BorderLayout.SOUTH);

		//main panel
		getContentPane().add(createGameButton, BorderLayout.NORTH);
		getContentPane().add(gamesScrollPane, BorderLayout.CENTER);
		getContentPane().add(playersScrollPane, BorderLayout.EAST);
		getContentPane().add(chatScrollPane, BorderLayout.SOUTH);
        

		//load default dictionary from player profile

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
	
	public void addGameWindow(String id, GameWindow g) {
		gameWindows.put(id, g);
	}
	
	/**
	*
	*/

	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == createGameButton) {
			menu.setVisible(true);
		}
		repaint();
	}


	
	/**
	*
	*/

	void addGame(String gameID, String maxPlayers, String minLength, String numSets, String blankPenalty, String lexicon, String speed, String allowsWatchers) {

		JPanel gamePane = new JPanel(new GridLayout(4, 2, 10, 10));
		gamePane.setBorder(new EmptyBorder(10, 10, 10, 10));
		gamePanes.put(gameID, gamePane);

		//join button
		JButton joinButton = new JButton("Join game");
		joinButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent evt) {
					if(!gameWindows.containsKey(gameID)) {
						dictionaries.putIfAbsent(lexicon, new AlphagramTrie(lexicon));
						dictionary = dictionaries.get(lexicon);
						
						gameWindows.put(gameID, new GameWindow(AnagramsClient.this, gameID, username, Integer.parseInt(minLength), Integer.parseInt(blankPenalty)));
						send("joingame " + gameID + " " + username);
					}
				}
			}
		);
		gamePane.add(joinButton);
		
		//watch button
		if(Boolean.parseBoolean(allowsWatchers)) {
			JButton watchButton = new JButton("Watch");
			watchButton.addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent evt) {
						if(!gameWindows.containsKey(gameID)) {
							gameWindows.put(gameID, new GameWindow(AnagramsClient.this, gameID, username, Integer.parseInt(minLength)));
							send("watchgame " + gameID + " " + username);
						
						}
					}
				}
			);	
			gamePane.add(watchButton);
		}
		
		gamePane.setBackground(new Color(0, 255, 51));
		gamePane.add(new JLabel("Lexicon: "  + lexicon));
		gamePane.add(new JLabel("Minimum word length: " + minLength));
		gamePane.add(new JLabel("Number of sets: " + numSets));
		gamePane.add(new JLabel("Blank Penalty: " + blankPenalty));	
		gamePane.add(new JLabel("Speed: " + speed));

		//this is probably way more convluted than it needs to be
		boolean placed = false;
		System.out.println(addresses.size());
		for(JPanel row : addresses.keySet()) {
			if(addresses.get(row).size() < 2) {
				row.add(gamePane);
				addresses.get(row).add(gameID);
				placed = true;
				break;
			}
		}
		if(!placed) {
		
			JPanel nextRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
			nextRow.setBackground(Color.BLUE);
			addresses.put(nextRow, new Vector<String>(2));
			addresses.get(nextRow).add(gameID);

			nextRow.add(gamePane);
			gamesPanel.add(nextRow);
			System.out.println("adding new row");
			
		}
		
		revalidate();
	}
	
	/**
	*
	*/
	
	void exitGame(String gameID, boolean isWatcher) {

		if(isWatcher) {
			send("stopwatching " + gameID);
		}
		else {
			send("exitgame " + gameID);
		}
		getRootPane().requestFocus();
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
			} else {
				System.exit(0);
			}
		}
		return false;
	}
	
	/**
	* Verifies that the user is using a up-date-version of the client software.
	* Directs the user to the project homepage if they are not.
	*/
	
	public void checkVersion() {
		try {
			send("version " + version);
			String response = this.bufferedIn.readLine();
			
			if("ok version".equalsIgnoreCase(response)) {
				return;
			}
			else if("unsupported".equalsIgnoreCase(response)) {
JOptionPane.showMessageDialog(this, new MessageWithLink("You are using an out-of-date version of Anagrams which is no longer supported. <br> Please visit <a href=\"http://www.seattlephysicstutor.com/anagrams.html\">the project home page</a> to download the latest version."), "Warning", JOptionPane.WARNING_MESSAGE);

				System.exit(0);
			}
		}
		catch (IOException e) {

			if (JOptionPane.showConfirmDialog(this, "The program is having trouble connecting to the host server. Try again?", "Connection issues", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				connect(serverName, port);
			} else {
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
		
		if("ok login".equalsIgnoreCase(response)) {
			System.out.println(username + " has just logged in.");
			return true;
		}
		else {
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
					System.out.println("command received: " + line);
					String cmd = tokens[0];
					
					if(cmd.equals("addplayer")) {
						playersList.add(tokens[1]);
						String players = " ";
						for(String player : playersList)
							players += player + "\n ";
						playersTextArea.setText(players);
						getContentPane().revalidate();
					}
					else if(cmd.equals("removeplayer")) {
						playersList.remove(tokens[1]);
						String players = "";
						for(String player : playersList)
							players += player + "\n ";
						playersTextArea.setText(players);
						getContentPane().revalidate();
					}
					else if(cmd.equals("chat")) {
						handleChat(line.replaceFirst("chat ", ""));
					}
					else if(cmd.equals("addgame")) {
						addGame(tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6], tokens[7], tokens[8]);
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
					//game related commands
					else if(gameWindows.get(tokens[1]) != null) {
						if(cmd.equals("nexttiles")) {;
							gameWindows.get(tokens[1]).setTiles(tokens[2]);
						}
						else if(cmd.equals("note")) {
							gameWindows.get(tokens[1]).setNotificationArea((line.split("@"))[1]);
						}
						else if(cmd.equals("addword")) {
								gameWindows.get(tokens[1]).addWord(tokens[2], tokens[3]);
						}
						else if(cmd.equals("removeword")) {
							gameWindows.get(tokens[1]).removeWord(tokens[2], tokens[3]);
						}
						else if(cmd.equals("joingame")) {
							gameWindows.get(tokens[1]).addPlayer(tokens[2]);
						}
						else if(cmd.equals("endgame")) {
							gameWindows.get(tokens[1]).gameOver = true;
						}
						else if(cmd.equals("leavegame")) {
							gameWindows.get(tokens[1]).removePlayer(tokens[2]);
							if(tokens[2].equals(username)) {
								gameWindows.remove(tokens[1]);
							}
						}
						else if(cmd.equals("watchgame")) {
							gameWindows.get(tokens[1]).addWatcher(tokens[2]);
						}
						else if(cmd.equals("unwatchgame")) {
							gameWindows.get(tokens[1]).removeWatcher(tokens[2]);
							if(tokens[2].equals(username)) {
								gameWindows.remove(tokens[1]);
							}
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
	*/
	
	public void handleChat(String msg) {
		chatBox.append("\n" + msg);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum());
			}
		});
	}
	
	
	/**
	* Transmit a command to the server
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

