import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Vector;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;

class GameWindow extends JDialog implements ActionListener {

	private JLabel notificationArea = new JLabel("New Game");
	private JButton exitGameButton = new JButton("Exit Game");	
	private JTextField textField = new JTextField("", 35);

	private GridBagLayout boardLayout = new GridBagLayout();
	private GridBagConstraints constraints = new GridBagConstraints();;
	private JPanel boardPanel = new JPanel();
	private ArrayList<GamePanel> gamePanels = new ArrayList<GamePanel>();
	private WordExplorer explorer;
	
	private final AnagramsClient client;
	final String gameID;
	private final String username;
	public final boolean isWatcher;
	private int minLength;
	private int blankPenalty;	
	private String tilePool = "";
	private HashMap<String, GamePanel> players = new HashMap<>();
	private ArrayList<String> watchers = new ArrayList<String>();
	private TilePanel tilePanel;
	private ImageIcon robotIcon = new ImageIcon(getClass().getResource("robot.png"));		
//	private int[] weights = {1,1,2,3,5,8,13};
	boolean gameOver = false;
	
	/**
	* Constructor for watching
	*/
	
	GameWindow(AnagramsClient client, String gameID, String username, int minLength) {
		super(client);

		this.client = client;
		this.gameID = gameID;
		this.username = username;
		this.minLength = minLength;
		isWatcher = true;

		JPanel mainPanel = new JPanel();
		setContentPane(mainPanel);		
		
		//register keyboard closing actions.
		mainPanel.registerKeyboardAction(ae -> {exitGameButton.doClick();}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		mainPanel.registerKeyboardAction(ae -> {exitGameButton.doClick();}, KeyStroke.getKeyStroke("control W"), JComponent.WHEN_IN_FOCUSED_WINDOW);		

		explorer = new WordExplorer(client.dictionary);
	
		setSize(900, 800);
		setLocation(client.getX() + 10, client.getY() + 20);
		setLayout(new BorderLayout());
	
		exitGameButton.addActionListener(this);
		textField.addActionListener(this);
		JPanel controlPanel = new JPanel();
		boardPanel.setBackground(new Color(0, 255, 51));
		controlPanel.add(notificationArea);
		controlPanel.add(exitGameButton);
		controlPanel.setBackground(new Color(0, 255, 51));
		
		boardPanel.setLayout(boardLayout);
		boardPanel.setBackground(new Color(0, 255, 51));
		boardLayout.columnWeights = new double[] {1,1,1};
		boardLayout.rowWeights = new double[] {1,1,1};
		constraints.fill = GridBagConstraints.BOTH;
		
		makeComponent(0, 0, new GamePanel());
		makeComponent(1, 0, new GamePanel());
		makeComponent(2, 0, new GamePanel());
		makeComponent(0, 1, new GamePanel());
		makeComponent(1, 1, new TilePanel());
		makeComponent(2, 1, new GamePanel());
		constraints.gridwidth = 3;
		makeComponent(0, 2, new GamePanel());
		
		mainPanel.add(controlPanel, BorderLayout.PAGE_START);
		mainPanel.add(boardPanel);

		revalidate();
		setVisible(true);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				client.exitGame(gameID, isWatcher);							
			}
		});	
	}
	
	/**
	* Constructor for playing
	*/
	
	GameWindow(AnagramsClient client, String gameID, String username, int minLength, int blankPenalty) {

		super(client);

		this.client = client;
		this.gameID = gameID;
		this.username = username;
		this.minLength = minLength;
		this.blankPenalty = blankPenalty;
		isWatcher = false;

		JPanel mainPanel = new JPanel();
		setContentPane(mainPanel);		
		
		//register keyboard closing actions.
		mainPanel.registerKeyboardAction(ae -> {exitGameButton.doClick();}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		mainPanel.registerKeyboardAction(ae -> {exitGameButton.doClick();}, KeyStroke.getKeyStroke("control W"), JComponent.WHEN_IN_FOCUSED_WINDOW);		

		explorer = new WordExplorer(client.dictionary);
		
		setSize(900, 700);
		setLocation(client.getX() + 10, client.getY() + 20);
		setLayout(new BorderLayout());
	
		exitGameButton.addActionListener(this);
		textField.addActionListener(this);
		JPanel controlPanel = new JPanel();
		boardPanel.setBackground(new Color(0, 255, 51));

		controlPanel.add(notificationArea);
		controlPanel.add(exitGameButton);
		controlPanel.add(textField);
		controlPanel.setBackground(new Color(0, 255, 51));
		
		boardPanel.setLayout(boardLayout);
		boardPanel.setBackground(new Color(0, 255, 51));
		boardLayout.columnWeights = new double[] {1,1,1};
		boardLayout.rowWeights = new double[] {1,1,1};
		constraints.fill = GridBagConstraints.BOTH;
		
		makeComponent(0, 0, new GamePanel());
		makeComponent(1, 0, new GamePanel());
		makeComponent(2, 0, new GamePanel());
		makeComponent(0, 1, new GamePanel());
		makeComponent(1, 1, new TilePanel());
		makeComponent(2, 1, new GamePanel());
		constraints.gridwidth = 3;
		makeComponent(0, 2, new GamePanel(username));
		
		mainPanel.add(controlPanel, BorderLayout.PAGE_START);
		mainPanel.add(boardPanel);

		setVisible(true);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				client.exitGame(gameID, isWatcher);							
			}
		});
	}
	

	/**
	*
	*/

	public void makeComponent(int x, int y, JComponent c) {

		constraints.gridx = x;
		constraints.gridy = y;
		constraints.insets = new Insets(3, 3, 3, 3);
		constraints.anchor = GridBagConstraints.WEST;
		boardLayout.setConstraints(c, constraints);
		c.setBackground(Color.GREEN);
		boardPanel.add(c);
	}
	
	/**
	*
	*/

	class TilePanel extends JPanel {
		
		public TilePanel() {
			tilePanel = this;
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			//Draw tile pool
			setMinimumSize(new Dimension(150, 150));
			g.setFont(new Font("Monospaced", Font.BOLD, 28));

			for (int i = 1; i <= tilePool.length(); i++) {
				int x = (int)(getSize().width/2 + 16*Math.sqrt(i)*Math.cos(Math.sqrt(i)*Math.PI*4/3));
				int y = (int)(getSize().height/2 + 16*Math.sqrt(i)*Math.sin(Math.sqrt(i)*Math.PI*4/3));				
				
				g.setColor(Color.YELLOW);
				g.fillRoundRect(x-1, y-19, 20, 21, 3, 3);	 

				g.setColor(Color.BLACK);	
				g.drawString(tilePool.charAt(i-1) + "", x, y); //tile to display

			}
		}
	}
	
	/**
	* Display the tiles on the table and the words owned by the player.
	*/	

	class GamePanel extends JPanel {
	
		String playerName = null;
		JPanel infoPane = new JPanel();
		JPanel wordPane = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(300,300);
			}
		};
		JLabel playerNameLabel = new JLabel();
		JLabel playerScore = new JLabel();
		
		boolean isOccupied = false;
		boolean isAvailable = true;
		TreeMap<String, WordLabel> words = new TreeMap<>();
		int score = 0;
		
		/**
		* An empty placeholder gamePanel
		*/
		
		GamePanel() {
			gamePanels.add(this);

			setLayout(new BorderLayout());

			add(infoPane, BorderLayout.NORTH);
			add(wordPane, BorderLayout.CENTER);
			
			playerNameLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
			playerScore.setFont(new Font("SansSerif", Font.PLAIN, 24));
			
			infoPane.setBackground(Color.GREEN);			
			infoPane.add(playerNameLabel);
			infoPane.add(playerScore);
			
			wordPane.setBackground(Color.GREEN);
		}
		
		/**
		* A gamePanel with a player seated
		*/
		
		GamePanel(String playerName) {
			this();
			takeSeat(playerName);
		}
		
		/**
		*
		*/
		
		public void takeSeat(String newPlayer) {
			this.playerName = newPlayer;
			players.put(newPlayer, this);
			playerNameLabel.setText(playerName);
			if(playerName.startsWith("Robot")) {		
				playerNameLabel.setIcon(robotIcon);
			}
			playerScore.setText("          0");
			isOccupied = true;
			isAvailable = false;
			revalidate();
			repaint();
		}
		
		/**
		*
		*/
		
		public void abandonSeat() {
			isOccupied = false;
			if(words.isEmpty()) {
				makeAvailable();
			}
		}
		
		/**
		* The seat is empty and contains no words, so a new player may occupy it.
		*/
		
		private void makeAvailable() {
			isAvailable = true;
			playerNameLabel.setText("");
			playerScore.setText("");
			players.remove(playerName);
			playerName = null;
		}
		
		/**
		* Add a new word to the player's collection and recalculate their score.
		*
		* @param String word The word to be removed.		
		*/
		
		void addWord(String word) {
			WordLabel newWord = new WordLabel(word);

			words.put(word, newWord);
			wordPane.add(newWord);
				
			score += word.length()*word.length();
			playerScore.setText("          " + score);

			validate();
			revalidate();
			repaint();
		}	
	
		/**
		* Remove a word from the player's collection and reclalculate their score.
		* If the player has left the table and has no words, the seat is opened up for 
		* another player.
		*
		* @param String word The word to be removed.
		*/
		
		void removeWord(String word) {
			wordPane.remove(words.remove(word));
//			score -= weights[word.length() - minLength];
			score -= word.length()*word.length();
			playerScore.setText(score + "");
			if(words.isEmpty() && isOccupied == false) {
				makeAvailable();
			}
		}	


		/**
		* Returns the words at this panel
		*/
		
		Set<String> getWords() {
			return words.keySet();
		}
			
		
		
		/**
		*
		*/
		
		class WordLabel extends JPanel {
			
			final int PADDING = 10;			
			final int WORD_GAP = 16;	
			final String word;
			public final int length;
			public final int height = 20;
			
			WordLabel(String word) {
				this.word = word;
				length = 4 + 18*word.length();
				setPreferredSize(new Dimension(length, height));
				setBackground(Color.GREEN);
		
				MouseListener mouseListener = new MouseAdapter() {
					public void mousePressed(MouseEvent e) {
						if(gameOver || isWatcher) {

							explorer.lookUp(word);
							explorer.setVisible(true);
						}
					}
					
				};
				addMouseListener(mouseListener);
				
				revalidate();
			}
			
			/**
			*
			*/

			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setFont(new Font("Monospaced", Font.BOLD, 24));
				
				int x = 3;

				for(Character tile : word.toCharArray()) {
				
					g.setColor(Color.YELLOW);
					g.fillRoundRect(x - 1 , -1, 15, 27, 2, 2);	 

					g.setColor(Color.BLACK);
					if(Character.isLowerCase(tile))
						g.setColor(Color.RED);
					g.drawString(String.valueOf(Character.toUpperCase(tile)), x, 17);
					
					x += WORD_GAP;
				}				
			}
		}	

		
	}
	
	
	/**
	*
	*/
	
	public void setNotificationArea(String nextMessage) {
		notificationArea.setText(nextMessage);
		
	}
	
	/**
	*
	*/
	
	public void setTiles(String nextTiles) {
		if(nextTiles.equals("#"))
			nextTiles = "";
		tilePool = nextTiles;
		tilePanel.repaint();
	}
	
	/**
	* If the player already has words at this table, the player claims them.
	* Otherwise, the player takes the next available seat, if there is one.
	*/

	public void addPlayer(String playerName) {

		if(players.containsKey(playerName))	 {
			players.get(playerName).takeSeat(playerName);
		}

		else {
			for(GamePanel panel : gamePanels) {
				if(panel.isAvailable) {
					panel.takeSeat(playerName);
					return;
				}
			}
		}
	}
	
	/**
	*
	*/
	
	public void addWord(String playerName, String wordToAdd) {
		players.get(playerName).addWord(wordToAdd);
		repaint();
	}
	
	/**
	*
	*/
	
	public void removeWord(String playerName, String wordToRemove) {
		players.get(playerName).removeWord(wordToRemove);
		repaint();
	}
	
	/**
	*
	*/
	
	public void removePlayer(String playerToRemove) {
		if(players.containsKey(playerToRemove)) {
			players.get(playerToRemove).abandonSeat();
		}
	}
	
	/**
	*
	*/

	public void addWatcher(String watcherName) {
		watchers.add(watcherName);
	}
	
	/**
	*
	*/
	
	public void removeWatcher(String watcherToRemove) {
		if(watchers.contains(watcherToRemove)) {
			watchers.remove(watcherToRemove);
		}
	}
	

	/**
	*
	*/
	
	public void actionPerformed(ActionEvent evt) {

		if (evt.getSource() == exitGameButton) {
			//JOptionPane "Are you sure you want to exit the game?"
			client.exitGame(gameID, isWatcher);
			explorer.setVisible(false);
			dispose();
		}

		//attempt to make a play
		else if (evt.getSource() == textField && gameOver == false) {
			String input = textField.getText().toUpperCase();
			boolean stolen = false;
			
			if(input.length() >= minLength) {
				for(GamePanel panel : gamePanels) {
					for(String existingWord : panel.getWords()) {
						if(existingWord.equalsIgnoreCase(input)) { //word is already on the board
							textField.setText("");
							return;
						}
					}
				}

				if(client.dictionary.contains(input)) {
					
					for(Map.Entry<String, GamePanel> entry : players.entrySet()) {
						String player = entry.getKey();
						for(String shortWord : entry.getValue().getWords()) {					
							if(input.length() > shortWord.length()) {	
								if(attemptSteal(player, shortWord, input)) {
									stolen = true;
									break;
								}
							}
						}
					}
					if(!stolen) {
						attemptMakeWord(input);
					}
				}
			}
			textField.setText("");
		}
		repaint();
	}


	/**
	* Given a shortWord, determines whether a longer word can be contructed from the short word. If so, 
	* the method returns true and an instruction is sent to the server to remove the shortWord from the
	* given opponent and reward the current player with the longWord. Otherwise returns false.
	*
	*@param String player Name of the player who owns the shortWord
	*@param String shortWord The word that we are attempting to steal
	*@param String longWord The word which may or not be a valid stal of the shortWord
	*/
	
	private boolean attemptSteal(String player, String shortWord, String longWord) {

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
		
		tiles = tilePool;
		String oldWord = shortWord;
		
		if(isRearrangement(oldWord.toUpperCase(), longWord)) {
			//steal is successful
			client.send("steal " + gameID + " " + player + " " + shortWord + " " + username + " " + longWord);
			return true;
		}
		else
			return false;
	}
	
	/**
	*
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
		client.send("makeword " + gameID + " " + username + " " + entry);
		return true;
	
	}
	
	/**
	* Given two words, the shorter of which is a subset of the other, determines whether a rearrangement/permutation
	* of letters is necessary to form the longer.
	*
	* @param String shortWord
	* @param String longWord
	*/
	
	public static boolean isRearrangement(String shortWord, String longWord) {
	
		String shortString = shortWord;
		String longString = longWord;
		
		while(longString.length() >= shortString.length() && shortString.length() > 0) {

			if(shortString.charAt(0) != longString.charAt(0)) {
				longString = longString.substring(1);
			}
			else {
				shortString = shortString.substring(1);
				longString = longString.substring(1);
			}
		}
		
		if(shortString.length() > longString.length()) 
			return true;
		else 
			return false;
		
	}

}