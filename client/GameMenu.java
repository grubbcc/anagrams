import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.lang.Integer;
import java.io.IOException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
* A menu for choosing options when creating a new game
*
*/

class GameMenu extends JDialog implements ActionListener {
	
	private JComboBox<Integer> playersSelector;
	private JComboBox<Integer> lengthsSelector;
	private JComboBox<Integer> setsSelector;
	private JComboBox<Integer> blanksSelector;
	private JComboBox<String> lexiconSelector;
	private JComboBox<String> speedSelector;
	private JComboBox<String> difficultySelector;
	
	private JCheckBox chatChooser = new JCheckBox("Allow chatting");
	private JCheckBox watchersChooser = new JCheckBox("Allow watchers");
	private JCheckBox robotChooser = new JCheckBox("Add robot player");
	
	private Integer[] players = {1, 2, 3, 4, 5, 6};
	private Integer[] lengths = {4, 5, 6, 7, 8, 9, 10};
	private Integer[] sets = {1, 2, 3};
	private Integer[] blankPenalty = {1, 2};
	private String[] lexicons = {"CSW19", "NWL18", "LONG"};
	private String[] speeds = {"slow", "medium", "fast"};
	private String[] difficulties = {"novice", "standard", "expert", "genius"};
	private String lexicon = "";

	private JButton startButton = new JButton("Start");

	private GridBagLayout menuLayout = new GridBagLayout();
	private GridBagConstraints constraints;
	
	private AnagramsClient client;

	/**
	*
	*/
	
	GameMenu(AnagramsClient client, int x, int y) {

		this.client = client;
		getRootPane().registerKeyboardAction(ae -> {startButton.doClick();}, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);

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
		playersSelector = new JComboBox<Integer>(players);
		lengthsSelector = new JComboBox<Integer>(lengths);
		setsSelector = new JComboBox<Integer>(sets);
		blanksSelector = new JComboBox<Integer>(blankPenalty);
		lexiconSelector = new JComboBox<String>(lexicons);
		speedSelector = new JComboBox<String>(speeds);
		difficultySelector = new JComboBox<String>(difficulties);

		//Set default values
		playersSelector.setSelectedItem(players[5]);
		lengthsSelector.setSelectedItem(lengths[3]);
		setsSelector.setSelectedItem(sets[0]);
		blanksSelector.setSelectedItem(blankPenalty[1]);
		speedSelector.setSelectedItem(speeds[1]);
		difficultySelector.setSelectedItem(difficulties[1]);
		difficultySelector.setEnabled(false);
		chatChooser.setSelected(true);
		watchersChooser.setSelected(true);		
		
		setTitle("New Game");
		setModal(true);
		setResizable(false);		
		setVisible(false);
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
		makeComponent(1, 7, difficultySelector);		
		makeComponent(0, 8, startButton);

		pack();
		setLocation(x - getWidth()/2, y - getHeight()/2);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				
					client.getRootPane().requestFocus();		
			}
		});
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

		if((Integer)playersSelector.getSelectedItem() != 1 && robotChooser.isSelected())
			difficultySelector.setEnabled(true);
		else 
			difficultySelector.setEnabled(false);
		
		
		if(evt.getSource() == startButton) {
			Date now = new Date();
			SimpleDateFormat ft = new SimpleDateFormat("hhmmssMs");
			String gameID = ft.format(now);	//generates a unique gameID based on the current time
		
			Integer maxPlayers = (Integer)playersSelector.getSelectedItem();
			Integer minLength = (Integer)lengthsSelector.getSelectedItem();
			Integer numSets = (Integer)setsSelector.getSelectedItem();
			Integer blankPenalty = (Integer)blanksSelector.getSelectedItem();
			if(!client.dictionaries.containsKey(lexicon)) {
				lexicon = (String)lexiconSelector.getSelectedItem();
				client.dictionaries.put(lexicon, new AlphagramTrie(lexicon));
			}
			String speed = (String)speedSelector.getSelectedItem();
			boolean allowChat = chatChooser.isSelected();
			boolean allowWatchers = watchersChooser.isSelected();
			boolean addRobot = robotChooser.isSelected();
			int skillLevel = (Integer)difficultySelector.getSelectedIndex() + 1;
			
			setVisible(false);

			client.dictionary = client.dictionaries.get(lexicon);

			client.addGameWindow(gameID, new GameWindow(client, gameID, client.username, minLength, blankPenalty, allowChat));

			String cmd = "newgame " + gameID + " " + maxPlayers + " " + minLength + " " + numSets + " " + blankPenalty + " " + lexicon + " " + speed + " " + allowChat + " " + allowWatchers + " " + addRobot + " " + skillLevel;
			client.send(cmd);


		}
	}

}
