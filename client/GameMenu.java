import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.lang.Integer;
import java.io.*;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
* A menu for choosing options whilst creating a new game
*
*/

class GameMenu extends JDialog implements ActionListener {
	
	private String maxPlayers;
	private String minLength;
	private String numSets;
	private String blankPenalty;
	private String lexicon;
	private String speed;
	private String skillLevel;
	private String allowChat;
	private String allowWatchers;
	private String addRobot;
	
	private JComboBox<String> playersSelector;
	private JComboBox<String> lengthsSelector;
	private JComboBox<String> setsSelector;
	private JComboBox<String> blanksSelector;
	private JComboBox<String> lexiconSelector;
	private JComboBox<String> speedSelector;
	private JComboBox<String> skillLevelSelector;
	
	private JCheckBox chatChooser = new JCheckBox("Allow chatting");
	private JCheckBox watchersChooser = new JCheckBox("Allow watchers");
	private JCheckBox robotChooser = new JCheckBox("Add robot player");
	private JCheckBox defaultChooser = new JCheckBox("Save as default");
	
	private String[] numPlayersChoices = {"1", "2", "3", "4", "5", "6"};
	private String[] minLengthChoices = {"4", "5", "6", "7", "8", "9", "10"};
	private String[] numSetsChoices = {"1", "2", "3"};
	private String[] blankPenaltyChoices = {"1", "2"};
	private String[] lexiconChoices;
	private String[] speedChoices = {"slow", "medium", "fast"};
	private String[] skillLevelChoices = {"novice", "standard", "expert", "genius"};


	private HashMap<String, String> gamePreferences = new HashMap<String, String>();
	private HashMap<String, String> newGamePreferences = new HashMap<String, String>();
	private String gamePreferencesPath;

	private JButton startButton = new JButton("Start");

	private GridBagLayout menuLayout = new GridBagLayout();
	private GridBagConstraints constraints;
	
	private AnagramsClient client;

	/**
	*
	*/
	
	GameMenu(AnagramsClient client, int x, int y) {

		this.client = client;
		gamePreferencesPath = client.workingDirectory + File.separator + "Anagrams" + File.separator + "gamePreferences.ser";
		lexiconChoices = client.lexicons;
		getRootPane().registerKeyboardAction(ae -> {startButton.doClick();}, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);


		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				client.getRootPane().requestFocus();		
				dispose();
			}
		});

		//labels
		JLabel tileSetsLabel = new JLabel("Number of tile sets");
		tileSetsLabel.setToolTipText("<html>100 tiles per set</html>");
		JLabel blankPenaltyLabel = new JLabel("Blank penalty");
		blankPenaltyLabel.setToolTipText("<html>To use a blank, you must take<br> this many additional tiles</html>");
		JLabel wordListLabel = new JLabel("Word list");
		wordListLabel.setToolTipText("<html>NWL18 = North American<br>CSW19 = International</html>");
		JLabel speedLabel = new JLabel("Speed");
		speedLabel.setToolTipText("<html>Slow: 9 seconds per tile<br>Medium: 6 seconds per tile<br>Fast: 3 seconds per tile</html>");
		
		//selectors
		playersSelector = new JComboBox<String>(numPlayersChoices);
		lengthsSelector = new JComboBox<String>(minLengthChoices);
		setsSelector = new JComboBox<String>(numSetsChoices);
		blanksSelector = new JComboBox<String>(blankPenaltyChoices);
		lexiconSelector = new JComboBox<String>(lexiconChoices);
		speedSelector = new JComboBox<String>(speedChoices);
		skillLevelSelector = new JComboBox<String>(skillLevelChoices);
		
		//load default settings
		gamePreferences = loadGamePreferences();		
		playersSelector.setSelectedItem(gamePreferences.get("maxPlayers"));
		lengthsSelector.setSelectedItem(gamePreferences.get("minLength"));
		setsSelector.setSelectedItem(gamePreferences.get("numSets"));
		blanksSelector.setSelectedItem(gamePreferences.get("blankPenalty"));
		lexiconSelector.setSelectedItem(gamePreferences.get("lexicon"));
		speedSelector.setSelectedItem(gamePreferences.get("speed"));
		skillLevelSelector.setSelectedItem(gamePreferences.get("skillLevel"));
		robotChooser.setSelected(Boolean.parseBoolean(gamePreferences.get("addRobot")));
		chatChooser.setSelected(Boolean.parseBoolean(gamePreferences.get("allowChat")));
		watchersChooser.setSelected(Boolean.parseBoolean(gamePreferences.get("allowWatchers")));
		defaultChooser.setSelected(false);
		
		if(playersSelector.getSelectedItem() != "1" && robotChooser.isSelected())
			skillLevelSelector.setEnabled(true);
		else 
			skillLevelSelector.setEnabled(false);
		
		setTitle("New Game");
		setModal(true);
		setResizable(false);
		startButton.addActionListener(this);
		playersSelector.addActionListener(this);
		robotChooser.addActionListener(this);
		
		setLayout(menuLayout);

		makeComponent(0, 0, new JLabel("Maximum number of players"));
		makeComponent(1, 0, playersSelector);
		makeComponent(0, 1, new JLabel("Minimum word length"));
		makeComponent(1, 1, lengthsSelector);
		makeComponent(0, 2, tileSetsLabel);
		makeComponent(1, 2, setsSelector);
		makeComponent(0, 3, blankPenaltyLabel);
		makeComponent(1, 3, blanksSelector);
		makeComponent(0, 4, wordListLabel);
		makeComponent(1, 4, lexiconSelector);
		makeComponent(0, 5, speedLabel);
		makeComponent(1, 5, speedSelector);
		makeComponent(0, 6, chatChooser);
		makeComponent(1, 6, watchersChooser);
		makeComponent(0, 7, robotChooser);
		makeComponent(1, 7, skillLevelSelector);
		makeComponent(0, 8, startButton);
		makeComponent(1, 8, defaultChooser);

		pack();
		setLocation(x - getWidth()/2, y - getHeight()/2);
		setVisible(true);

	}
	
	/**
	*
	*/

	public void makeComponent(int x, int y, JComponent c) {
		constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		if (x == 1) {
			constraints.fill = GridBagConstraints.HORIZONTAL;
		}
		constraints.insets = new Insets(3, 3, 3, 3);
		constraints.anchor = GridBagConstraints.WEST;
		menuLayout.setConstraints(c, constraints);
		add(c);
	}
	
	/**
	*
	*/

	public void actionPerformed(ActionEvent evt) {

		if(!playersSelector.getSelectedItem().equals("1") && robotChooser.isSelected())
			skillLevelSelector.setEnabled(true);
		else 
			skillLevelSelector.setEnabled(false);
		
		if(evt.getSource() == startButton) {
			Date now = new Date();
			SimpleDateFormat ft = new SimpleDateFormat("hhmmssMs");
			String gameID = ft.format(now);	//generates a unique gameID based on the current time
		
			maxPlayers = playersSelector.getSelectedItem() + "";
			minLength = lengthsSelector.getSelectedItem() + "";
			numSets = setsSelector.getSelectedItem() + "";
			blankPenalty = blanksSelector.getSelectedItem() + "";
			lexicon = lexiconSelector.getSelectedItem() + "";
			speed = speedSelector.getSelectedItem() + "";
			skillLevel = skillLevelSelector.getSelectedItem() + "";
			allowChat = chatChooser.isSelected() + "";
			allowWatchers = watchersChooser.isSelected() + "";
			addRobot = robotChooser.isSelected() + "";
			
			setVisible(false);
			
			AlphagramTrie dictionary = null; //later make this default dictionary
			for(String key : client.dictionaries.keySet()) {
				if(key.equals(lexicon)) {
					dictionary = client.dictionaries.get(key);
					if(dictionary == null) {
						dictionary = new AlphagramTrie(key);
						client.dictionaries.put(key, dictionary);
					}
				}
			}

			updatePreferences();
			
			if(defaultChooser.isSelected()) {				
				saveDefaultsToFile();
			}

			client.addGameWindow(gameID, new GameWindow(client, gameID, client.username, minLength, blankPenalty, allowChat, dictionary, new ArrayList<String[]>(), false));

			skillLevel = (skillLevelSelector.getSelectedIndex() + 1) + "";

			String cmd = "newgame " + gameID + " " + maxPlayers + " " + minLength + " " + numSets + " " + blankPenalty + " " + lexicon + " " + speed + " " + allowChat + " " + allowWatchers + " " + addRobot + " " + skillLevel;
			client.send(cmd);
		}
	}

	
	/**
	*
	*/
	
	public HashMap<String, String> loadGamePreferences() {
		
		try {
			ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(gamePreferencesPath));
			@SuppressWarnings("unchecked")
			HashMap<String, String> input = (HashMap<String, String>)inputStream.readObject();
			gamePreferences = input;
		}
		catch (Exception e) {
			System.out.println("Game settings file not found or damaged; using defaults.");

			gamePreferences.put("maxPlayers", "6");
			gamePreferences.put("minLength", "7");
			gamePreferences.put("numSets", "1");
			gamePreferences.put("blankPenalty", "2");
			gamePreferences.put("lexicon", "NWL18");
			gamePreferences.put("speed", "medium");
			gamePreferences.put("allowChat", "true");
			gamePreferences.put("allowWatchers", "true");
			gamePreferences.put("addRobot", "false");
			gamePreferences.put("skillLevel", "standard");

		}
		return gamePreferences;
    }

	/**
	*
	*/
	
	private void updatePreferences() {
		newGamePreferences.put("maxPlayers", maxPlayers);
		newGamePreferences.put("minLength", minLength);
		newGamePreferences.put("numSets", numSets);
		newGamePreferences.put("blankPenalty", blankPenalty);
		newGamePreferences.put("lexicon", lexicon);
		newGamePreferences.put("speed", speed);
		newGamePreferences.put("allowChat", allowChat);
		newGamePreferences.put("allowWatchers", allowWatchers);
		newGamePreferences.put("addRobot", addRobot);
		newGamePreferences.put("skillLevel", skillLevel);	
	}
	
	/**
	*
	*/

	void saveDefaultsToFile() {
		
		gamePreferences = newGamePreferences;

		try {
			File gamePreferencesFile = new File(gamePreferencesPath);
			gamePreferencesFile.getParentFile().mkdirs();
			FileOutputStream fileOut = new FileOutputStream(gamePreferencesFile);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(gamePreferences);
			out.close();
			fileOut.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (SecurityException se) {
			System.out.println("Permission to write to " + gamePreferencesPath + " denied.");
		}
	}	

}
