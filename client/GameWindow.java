import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.sound.sampled.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;

/**
* A front end user interface for playing, watching, or analyzing a game.
*
*/

class GameWindow extends JFrame implements ActionListener {

	private JPanel controlPanel = new JPanel();
	private JLabel notificationArea = new JLabel("");
	private JButton exitGameButton = new JButton("Exit Game");
	private JTextField textField = new JTextField("Enter a word here to play", 35);
	private JLabel infoPane = new JLabel();
	private JPanel chatPanel = new JPanel(new BorderLayout());
	private JScrollPane chatScrollPane = new JScrollPane(chatPanel);
	private JTextArea chatBox = new JTextArea(5, 75);
	private JTextField chatField = new JTextField("Type here to chat", 75);
	private JButton backToStartButton = new JButton("|<");
	private JButton backTenButton = new JButton("<<");
	private JButton backButton = new JButton("<");
	private JButton showPlaysButton = new JButton("Show plays");
	private JButton forwardButton = new JButton(">");
	private JButton forwardTenButton = new JButton(">>");
	private JButton forwardToEndButton = new JButton(">|");

	private GridBagLayout boardLayout = new GridBagLayout();
	private GridBagConstraints constraints = new GridBagConstraints();
	private JPanel boardPanel = new JPanel();
	private ArrayList<GamePanel> gamePanels = new ArrayList<GamePanel>();
	private GamePanel homePanel = new GamePanel();
	private WordExplorer explorer;
	private WordFinder wordFinder;
	private WordDisplay wordDisplay;
	
	private final AnagramsClient client;
	private final AlphagramTrie dictionary;
	ArrayList<String[]> gameLog = new ArrayList<>();
	final String gameID;
	private final String username;
	public final boolean isWatcher;
	private int minLength;
	private int blankPenalty;
	private String tilePool = "";
	private HashMap<String, GamePanel> players = new HashMap<>();
	private HashSet<String> allWords = new HashSet<>();
	private TilePanel tilePanel;
	private ImageIcon blackRobot = new ImageIcon(getClass().getResource("black robot.png"));
	private ImageIcon whiteRobot = new ImageIcon(getClass().getResource("white robot.png"));
	boolean gameOver = false;
	
	//fields for analysis
	private int position;
	private int maxPosition;
	
	/**
	* 
	*/
	
	GameWindow(AnagramsClient client, String gameID, String username, String minLength, String blankPenalty, String allowsChat, AlphagramTrie dictionary, ArrayList<String[]> gameLog, boolean isWatcher) {

		this.client = client;
		this.gameID = gameID;
		this.username = username;
		this.minLength = Integer.parseInt(minLength);
		this.blankPenalty = Integer.parseInt(blankPenalty);
		this.gameLog = gameLog;
		this.isWatcher = isWatcher;
		
		if(!gameLog.isEmpty()) {
			gameOver = true;
		}

		JPanel mainPanel = new JPanel();
		setContentPane(mainPanel);
		
		//register keyboard closing actions.
		mainPanel.registerKeyboardAction(ae -> {exitGameButton.doClick();}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		mainPanel.registerKeyboardAction(ae -> {exitGameButton.doClick();}, KeyStroke.getKeyStroke("control W"), JComponent.WHEN_IN_FOCUSED_WINDOW);
		mainPanel.registerKeyboardAction(ae -> {backButton.doClick();}, KeyStroke.getKeyStroke("DOWN"), JComponent.WHEN_IN_FOCUSED_WINDOW);
		mainPanel.registerKeyboardAction(ae -> {forwardButton.doClick();}, KeyStroke.getKeyStroke("RIGHT"), JComponent.WHEN_IN_FOCUSED_WINDOW);		
		
		this.dictionary = dictionary;
		explorer = new WordExplorer(dictionary);
		wordFinder = new WordFinder(this.minLength, this.blankPenalty, dictionary);		
		wordDisplay = new WordDisplay();

		setSize(1000, 700);
		setMinimumSize(new Dimension(750, 523));
		setLocation(client.getX() + 10, client.getY() + 20);
		setLayout(new BorderLayout());
	
		//control panel
		exitGameButton.addActionListener(this);
		controlPanel.add(notificationArea);
		controlPanel.add(exitGameButton);
		infoPane.setText(dictionary.lexicon + "      Minimum length = " + minLength);
		controlPanel.add(infoPane);
		
		//game panels
		boardPanel.setLayout(boardLayout);
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
		makeComponent(0, 2, homePanel);
		
		mainPanel.add(controlPanel, BorderLayout.PAGE_START);
		mainPanel.add(boardPanel);

		//chat panel
		if(allowsChat.equals("true")) {
			chatScrollPane.setPreferredSize(new Dimension(600, 85));
			chatPanel.setBackground(client.chatAreaColor);
			chatBox.setLineWrap(true);
			chatBox.setEditable(false);
			chatField.setForeground(Color.DARK_GRAY);
			new CustomFocusListener(chatField);
			chatField.addActionListener(ae -> {client.send("gamechat " + gameID + " " + username + ": " + chatField.getText()); chatField.setText("");});
			chatField.getActionMap().put(DefaultEditorKit.deletePrevCharAction, new CustomDeletePrevCharAction());
			chatField.setBorder(new EmptyBorder(1,1,1,1));
			chatField.setBackground(Color.LIGHT_GRAY);
			chatPanel.add(chatBox);
			chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum());

			chatPanel.add(chatField, BorderLayout.SOUTH);
			mainPanel.add(chatScrollPane, BorderLayout.PAGE_END);
		}
		if(!isWatcher) {
			textField.setForeground(Color.GRAY);
			textField.addActionListener(this);
			new CustomFocusListener(textField);
			textField.getActionMap().put(DefaultEditorKit.deletePrevCharAction, new CustomDeletePrevCharAction());
			controlPanel.remove(infoPane);
			controlPanel.add(textField);
			controlPanel.add(infoPane);

			homePanel.takeSeat(username);
		}
		
		if(gameOver) {
			endGame();
		}
		
		setColors();
		setVisible(true);
		
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent componentEvent) {
				for(GamePanel gamePanel : gamePanels) {
					if(!gamePanel.words.isEmpty()) {
						gamePanel.updateTileSize();
					}
				}
				revalidate();
			chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum());
			}
		});

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				client.exitGame(gameID, isWatcher);
				dispose();
			}
		});

	}


	/**
	* Sets the GridBagConstraints for the JPanel to be added to the mainPanel
	*
	* @param xCoord the horizontal position of the panel in the GridBagLayout scheme
	* @param int yCoord vertical position of the panel in the GridBagLayout scheme
	* @param JComponent panel The gamePanel or tilePanel to be added
	*/

	public void makeComponent(int xCoord, int yCoord, JComponent panel) {

		constraints.gridx = xCoord;
		constraints.gridy = yCoord;
		constraints.insets = new Insets(3, 3, 3, 3);
		constraints.anchor = GridBagConstraints.WEST;
		boardLayout.setConstraints(panel, constraints);

		boardPanel.add(panel);
		if(panel instanceof GamePanel) {
			gamePanels.add((GamePanel)panel);
		}
	}
	
	/**
	*
	*/
	
	public void setColors() {
		controlPanel.setBackground(client.gameBackgroundColor);
		notificationArea.setForeground(client.gameBackgroundText);
		infoPane.setForeground(client.gameBackgroundText);
		boardPanel.setBackground(client.gameBackgroundColor);
		tilePanel.setBackground(client.gameForegroundColor);
		chatPanel.setBackground(client.chatAreaColor);
		chatBox.setBackground(client.chatAreaColor);
		wordDisplay.setColors();
		for(GamePanel gamePanel : gamePanels) {
			gamePanel.setColors();
		}
	}
	
	/**
	* An object which can tell if a textField has been clicked yet.
	*/

	class CustomFocusListener implements FocusListener {
		
		JTextField co;
		
		/**
		*/
		
		CustomFocusListener(JTextField c) {
			co = c;
			co.addFocusListener(this);
		}
		
		/**
		*/
		
		public void focusGained(FocusEvent e) {
			co.setText("");
			co.setForeground(Color.BLACK);

			if(e.getSource() == chatField) {
				chatField.removeFocusListener(this);
			}	
		}
		
		/**
		*/
		
		public void focusLost(FocusEvent e) {
		}
	};
	
	/**
	* A panel for displaying the tilePool
	*/

	class TilePanel extends JPanel {
		
		/**
		*/
		
		public TilePanel() {
			tilePanel = this;
		}
		
		/**
		*/
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			//Draw tile pool
			setMinimumSize(new Dimension(135, 135));
			g.setFont(new Font("Monospaced", Font.BOLD, 28));

			for (int i = 1; i <= tilePool.length(); i++) {
				int x = (int)(getSize().width/2 + 16*Math.sqrt(i)*Math.cos(Math.sqrt(i)*Math.PI*4/3));
				int y = 3 + (int)(getSize().height/2 + 16*Math.sqrt(i)*Math.sin(Math.sqrt(i)*Math.PI*4/3));				
				
				g.setColor(Color.YELLOW);
				g.fillRoundRect(x-1, y-19, 20, 21, 3, 3);

				g.setColor(Color.BLACK);
				g.drawString(tilePool.charAt(i-1) + "", x, y); //tile to display

			}
		}
	}
	
	/**
	* A Panel for displaying the name, score, and words possesed by a player.
	*/

	class GamePanel extends JPanel {
	
		String playerName = null;
		private JPanel infoPane = new JPanel();
		private JPanel wordPane = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(300,300);
			}
		};
		private JLabel playerNameLabel = new JLabel();
		private JLabel playerScoreLabel = new JLabel();
		boolean savingSpace = false;		
		private int tilePadding = 4;
		private int tileWidth = 18;
		private int tileHeight = 20;
		boolean isOccupied = false;
		boolean isAvailable = true;
		private int wordArea;
		LinkedHashMap<String, WordLabel> words = new LinkedHashMap<>();
		
		int score = 0;
		
		/**
		* An empty placeholder gamePanel
		*/
		
		GamePanel() {

			setLayout(new BorderLayout());
			add(infoPane, BorderLayout.NORTH);
			add(wordPane, BorderLayout.CENTER);
			
			playerNameLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
			playerScoreLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
				
			infoPane.setOpaque(false);
			infoPane.add(playerNameLabel);
			infoPane.add(playerScoreLabel);
			
			wordPane.setOpaque(false);
		}
		
		/**
		*
		*/


		
		public void setColors() {
			setBackground(client.gameForegroundColor);
			playerNameLabel.setForeground(client.gameForegroundText);
			playerScoreLabel.setForeground(client.gameForegroundText);
			
			if(playerName != null) {

				if(playerName.startsWith("Robot")) {
					if(client.gameForegroundText.equals(Color.BLACK)) {
						playerNameLabel.setIcon(new ImageIcon(getClass().getResource("black robot.png")));
					}
					else {
						playerNameLabel.setIcon(new ImageIcon(getClass().getResource("white robot.png")));
					}
				}
			}
			
			else if(!gameOver) {
				playerNameLabel.setForeground(Color.GRAY);
				playerScoreLabel.setForeground(Color.GRAY);
			}
		}
		
		/**
		* Puts the given player's name on this panel and displays their score.
		*
		* @param newPlayer The player to be added.
		*/
		
		public void takeSeat(String newPlayer) {
			this.playerName = newPlayer;
			players.put(newPlayer, this);
			playerNameLabel.setText(playerName);

			playerScoreLabel.setBorder(new EmptyBorder(0,30,0,0));
			if(words.isEmpty() ) {
				playerScoreLabel.setText("0");
			}
			isOccupied = true;
			isAvailable = false;
			setColors();
		}
		
		/**
		* The player has left this seat. If they have any words, they remain. 
		* Otherwise, the seat becomes available for another player.
		*/
		
		public void abandonSeat() {
			isOccupied = false;
			if(words.isEmpty()) {
				makeAvailable();
			}
			setColors();
		}
		
		/**
		* The seat is empty and contains no words, so a new player may occupy it.
		*/
		
		private void makeAvailable() {
			isAvailable = true;
			playerNameLabel.setText("");
			playerNameLabel.setIcon(null);
			playerScoreLabel.setText("");
			players.remove(playerName);
			playerName = null;
		}
		
		/**
		* Removes the occupant, as well as any words they may have, from this pane. 
		* Used only during endgame analyss.
		*/
		
		private void reset() {
			for(WordLabel labelToRemove : words.values()) {
				wordPane.remove(labelToRemove);
			}
			score = 0;
			words = new LinkedHashMap<>();
			abandonSeat();
			makeBig();
			
		}
		
		/**
		* Add a new word to the player's collection and recalculate their score.
		*
		* @param String word The word to be removed.		
		*/
		
		WordLabel addWord(String newWord) {

			WordLabel newWordLabel = new WordLabel(newWord);
			words.put(newWord, newWordLabel);
			wordPane.add(newWordLabel);
			
			score += newWord.length()*newWord.length();
			playerScoreLabel.setText(score + "");
			
			validate();
			
			updateTileSize();

			if(!gameOver) {
				try {
					InputStream audioSource = getClass().getResourceAsStream("steal sound.wav");
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
			return newWordLabel;
		}
	
		/**
		* Remove a word from the player's collection and reclalculate their score.
		* If the player has left the table and has no words, the seat is opened up for 
		* another player.
		*
		* @param String word The word to be removed.
		*/
		
		void removeWord(String wordToRemove) {
			wordPane.remove(words.remove(wordToRemove));
			score -= wordToRemove.length()*wordToRemove.length();
			playerScoreLabel.setText(score + "");

			updateTileSize();
			wordPane.repaint();

		}
		
		/**
		*
		*/
		
		void updateTileSize() {

			if(savingSpace == false) {

				Iterator<WordLabel> iterator = words.values().iterator();
				WordLabel newestWord = null;
				while (iterator.hasNext()) {
					newestWord = iterator.next();
				}

				if(newestWord != null) {
					if(newestWord.getLocation().y + tileHeight > wordPane.getHeight()) {
						makeSmall();
					}
				}
			}
			
			else {
				int newLength = -16;
				int newHeight = 26;
				for(String word : words.keySet()) {
					newLength += 20 + 18*word.length();
					if(newLength > wordPane.getWidth()) {
						newHeight += 25;
						newLength = -16;
					}
				}
				if(newHeight < wordPane.getHeight()) {
					makeBig();
				}
			}
		}
		
		/**
		*
		*/
		
		void makeBig() {
			
			savingSpace = false;
			tilePadding = 4;
			tileWidth = 16;
			tileHeight = 18;
			revalidate();
			wordPane.revalidate();			
			for(WordLabel wordLabel : words.values()) {
				wordLabel.resize();
			}
			wordPane.repaint();
		}
		
		/**
		*
		*/

		void makeSmall() {
			savingSpace = true;
			tilePadding = 3;
			tileWidth = 12;
			tileHeight = 16;
			revalidate();
			wordPane.revalidate();			
			for(WordLabel wordLabel : words.values()) {
				wordLabel.resize();
			}
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
		
		class WordLabel extends JPanel {
			
			final String word;
			int length;
			int height;
			
			/**
			*
			*/
			
			WordLabel(String word) {
				this.word = word;

				setPreferredSize(new Dimension(length, height));
				setOpaque(false);

				length = tilePadding + tileWidth*word.length();
				height = tileHeight;
				setPreferredSize(new Dimension(length, height));
		
				MouseListener mouseListener = new MouseAdapter() {
					
					/**
					* Displays possible steals for this word.
					*
					* MouseEvent e A click in this panel.
					*/
					
					public void mousePressed(MouseEvent e) {
						if(gameOver || isWatcher) {

							explorer.lookUp(word);
							explorer.setVisible(true);
						}
					}
				};
				addMouseListener(mouseListener);
			}

			/**
			*
			*/
			
			void resize() {
				length = tilePadding + tileWidth*word.length();
				height = tileHeight;
				setPreferredSize(new Dimension(length, height));
				revalidate();
				repaint();
			}
			
			/**
			* Draws a word, showing blanks as red and regular tiles as black.
			*/

			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				if(savingSpace) {
					int WORD_GAP = 12;
					g.setFont(new Font("Monospaced", Font.BOLD, 18));
					int x = 2;

					for(Character tile : word.toCharArray()) {
					
						g.setColor(Color.YELLOW);
						g.fillRoundRect(x - 1 , -1, 11, 17, 2, 2);	 

						g.setColor(Color.BLACK);
						if(Character.isLowerCase(tile))
							g.setColor(Color.RED);
						g.drawString(String.valueOf(Character.toUpperCase(tile)), x, 14);
						
						x += WORD_GAP;
					}

				}
				else {
					int WORD_GAP = 16;
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
	* @param String nextTiles The new contents of the tilePool.
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
	*
	* @param String playerName the player to be added.
	*/

	public void addPlayer(String playerName) {
		
		//current player reenters game after leaving
		if(playerName.equals(username)) {
			if(!homePanel.isOccupied) {
				homePanel.takeSeat(playerName);
			}
		}

		else {
			//the current player is informed of other players in the game
			for(GamePanel panel : gamePanels) {
				//the other player is assigned the first available seat
				if(panel.isAvailable && panel != homePanel) {
					panel.takeSeat(playerName);
					return;
				}
			}
		}
	}

	/**
	* Removes the named player from their current seat. Their words, if any, remain to be
	* reclaimed if the player returns.
	*
	* @param String playerToRemove The name of the player to remove.
	*/
	
	public void removePlayer(String playerToRemove) {
		if(players.containsKey(playerToRemove)) {
			players.get(playerToRemove).abandonSeat();
		}
	}

	
	/**
	* Places the given word in the panel occupied by the given player.
	*
	* @param String playerName The player that has claimed the new word
	* @param String wordToAdd The new word that the player has created
	*/
	
	public void addWord(String playerName, String wordToAdd) {

		players.get(playerName).addWord(wordToAdd);
	}
	
	/**
	* Removes the given word from the given player from whom it has been stolen.
	*
	* @param String playerName the player whose word has been stolen
	* @param String the stolen word
	*/
	
	public void removeWord(String playerName, String wordToRemove) {
		players.get(playerName).removeWord(wordToRemove);
		revalidate();
		repaint();
	}
	

	/**
	* Displays the new chat message (and sender) in the chat box and automatically scrolls to view
	* 
	* @param String msg The message to display.
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
	*
	*/
	
	public void endGame() {
		gameOver = true;

		maxPosition = gameLog.size() - 1;
		position = maxPosition;	
		
		controlPanel.remove(infoPane);
		if(!isWatcher) {
			controlPanel.remove(textField);
		}
		
		controlPanel.add(backToStartButton);
		backToStartButton.addActionListener(this);
		controlPanel.add(backTenButton);
		backTenButton.addActionListener(this);
		controlPanel.add(backButton);
		backButton.addActionListener(this);
		controlPanel.add(showPlaysButton);
		showPlaysButton.addActionListener(this);
		controlPanel.add(forwardButton);
		forwardButton.addActionListener(this);
		controlPanel.add(forwardTenButton);
		forwardTenButton.addActionListener(this);
		controlPanel.add(forwardToEndButton);
		forwardToEndButton.addActionListener(this);
		
		controlPanel.add(infoPane);
		for(GamePanel gamePanel : gamePanels) {
			gamePanel.setColors();
		}
		showPosition(maxPosition);
		revalidate();
		controlPanel.revalidate() ;
	}

	/**
	*
	*/
	
	void showPosition(int position) {
		
		if(!gameOver) {
			return;
		}
		
		allWords = new HashSet<>();
		for(GamePanel panel : gamePanels) {
			panel.reset();
		}

		String[] tokens = gameLog.get(position);
		
		notificationArea.setText("Time remaining " + tokens[0]);
		setTiles(tokens[1]);
		
		if(tokens.length > 2) {
			for(int i = 2; i < tokens.length; i += 2) {

				String playerName = tokens[i];
				addPlayer(playerName);
				
				for(String word : tokens[i+1].substring(1, tokens[i+1].length()-1).split(",")) {
					if(!word.isEmpty()) {
						addWord(playerName, word);
						allWords.add(word);
					}
				}
			}
		}
		
		if(wordDisplay.isVisible()) {
			findPlays();
		}
	}
	
	/**
	*
	*/
	
	void findPlays() {

		LinkedHashSet<String> wordsInPool = wordFinder.findWordsInPool(tilePool);
		LinkedHashSet<String[]> possibleSteals = wordFinder.searchForSteals(allWords);
		wordDisplay.setWords(wordsInPool, possibleSteals);	
		wordDisplay.setVisible(true);

	}
	
	/**
	*
	*/

	public class WordDisplay extends JDialog {
		
		private GamePanel poolPanel = new GamePanel();
		private GamePanel stealsPanel = new GamePanel();
		private TitledBorder poolBorder = new TitledBorder("Pool");
		private TitledBorder stealsBorder = new TitledBorder("Steals");
			
		/**
		*
		*/
		
		public WordDisplay() {

			setSize(480, 380);
			setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

			poolBorder.setTitleFont(new Font("SansSerif", Font.PLAIN, 18));				
			poolPanel.setBorder(BorderFactory.createCompoundBorder(
				poolBorder, new EmptyBorder(5, 5, 5, 5))
			);
			
			stealsBorder.setTitleFont(new Font("SansSerif", Font.PLAIN, 18));
			stealsPanel.setBorder(BorderFactory.createCompoundBorder(
				stealsBorder, new EmptyBorder(5, 5, 5, 5))
			);

			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					showPlaysButton.doClick();
				}
			});

			addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent componentEvent) {
					if(!poolPanel.words.isEmpty()) {
						poolPanel.updateTileSize();
					}

					if(!stealsPanel.words.isEmpty()) {
						stealsPanel.updateTileSize();
					}					
					revalidate();
				}
			});
			
			setColors();
			
			poolPanel.remove(poolPanel.infoPane);
			stealsPanel.remove(stealsPanel.infoPane);
			add(poolPanel);
			add(stealsPanel);
			
			setLocation((client.getLocation().x + client.getWidth() - this.getWidth())/2, (client.getLocation().y + client.getHeight() - this.getHeight())/2);		
			setModal(false);
			setAlwaysOnTop(true);
			setVisible(false);
		}
		
		/**
		*
		*/
		
		void setColors() {
			setBackground(client.gameBackgroundColor);

			poolPanel.setBackground(client.gameForegroundColor);
			poolPanel.setForeground(client.gameForegroundText);
			stealsPanel.setBackground(client.gameForegroundColor);
			stealsPanel.setForeground(client.gameForegroundText);
			
			poolBorder.setTitleColor(client.gameForegroundText);
			stealsBorder.setTitleColor(client.gameForegroundText);
		}
		
		/**
		*
		*/
		
		public void setWords(LinkedHashSet<String> wordsInPool, LinkedHashSet<String[]> possibleSteals) {
		
			poolPanel.reset();
			stealsPanel.reset();

			//pool
			if(!wordsInPool.isEmpty()) {	
				for(String word : wordsInPool) {
					poolPanel.addWord(word);
				}
			}
			
			//steals
			if(!possibleSteals.isEmpty()) {
				for(String[] steal : possibleSteals) {
					GamePanel.WordLabel shortWord = stealsPanel.new WordLabel(steal[0]);

					stealsPanel.addWord(steal[2]).setToolTipText(steal[0] + " + " + steal[1]);
				}
			}
		}
	}
	

	/**
	* Responds to button clicks and textField entries
	*
	* @param ActionEvent evt
	*/
	
	public void actionPerformed(ActionEvent evt) {

		//attempt to make a play
		if (evt.getSource() == textField) {
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

				if(dictionary.contains(input)) {
					
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
		
		else if (evt.getSource() == exitGameButton) {
			client.exitGame(gameID, isWatcher);
			explorer.setVisible(false);
			wordDisplay.setVisible(false);
			dispose();
		}
		else if(evt.getSource() == backToStartButton) {
			position = 0; showPosition(position);
		}		
		else if(evt.getSource() == backTenButton) {
			position = Math.max(position - 10, 0); showPosition(position);
		}
		else if(evt.getSource() == backButton) {
			position = Math.max(position - 1, 0); showPosition(position);
		}
		else if(evt.getSource() == forwardButton) {
			position = Math.min(position + 1, maxPosition); showPosition(position);
		}
		else if(evt.getSource() == forwardTenButton) {
			position = Math.min(position + 10, maxPosition); showPosition(position);
		}		
		else if(evt.getSource() == forwardToEndButton) {
			position = maxPosition; showPosition(position);
		}
		
		else if(evt.getSource() == showPlaysButton) {
			if(wordDisplay.isVisible()) {
				wordDisplay.setVisible(false);
				showPlaysButton.setText("Show plays");
			}
			else {
				wordDisplay.setVisible(true);
				showPlaysButton.setText("Hide plays");
			}
			showPosition(position);
		}
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
	* Given a word, determines whether the appropriate tiles can be found in the pool. If so, true is returned
	* and an instruction is sent to the server that the tiles should be removed and the current player should 
	* claim them.
	*
	* @param String entry The word that was entered.
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
