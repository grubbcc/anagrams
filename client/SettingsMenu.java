import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.lang.Integer;
import java.util.HashMap;
import java.io.*;


/**
* A menu for choosing setting game options and saving them for future use
*
*/

class SettingsMenu extends JDialog implements ActionListener {
	
	private JButton OKButton = new JButton("OK");
	private JButton CancelButton = new JButton("Cancel");
	private JButton ApplyButton = new JButton("Apply");
	private JCheckBox soundChooser = new JCheckBox("Play sounds");		
	private GridBagLayout menuLayout = new GridBagLayout();
	private GridBagConstraints constraints = new GridBagConstraints();
	private JComboBox lexiconChooser;
	private AnagramsClient client;
	private HashMap<String, String> settings;
	private HashMap<String, String> newSettings = new HashMap<String, String>();

	public Color chatAreaColor;
	public Color mainScreenBackgroundColor;
	public Color playersPanelColor;
	public Color gameForegroundColor;
	public Color gameBackgroundColor;

	/**
	*
	*/
	
	SettingsMenu(AnagramsClient client, int x, int y) {

		this.client = client;
		String[] lexicons = client.lexicons;
		getRootPane().registerKeyboardAction(ae -> {OKButton.doClick();}, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
		settings = client.settings;
		
		setLocation(x - getWidth()/2, y - getHeight()/2);
		setTitle("Settings");
		setModal(true);
		setResizable(false);

		OKButton.addActionListener(this);
		CancelButton.addActionListener(this);
		ApplyButton.addActionListener(this);
		setLayout(menuLayout);

		//labels
		JLabel lexiconLabel = new JLabel("Word list");
		lexiconLabel.setToolTipText("<html>NWL18 = North American<br>CSW19 = International</html>");
		
		//selectors
		lexiconChooser = new JComboBox<String>(lexicons);
		lexiconChooser.setSelectedItem(settings.get("lexicon"));
		soundChooser.setSelected(Boolean.parseBoolean(settings.get("play sounds")));

		makeComponent(1, 0, lexiconLabel);
		makeComponent(2, 0, lexiconChooser);
		makeComponent(3, 0, soundChooser);
		makeComponent(1, 1, new ColorChooser("Main screen"));
		makeComponent(1, 2, new ColorChooser("Players list"));
		makeComponent(1, 3, new ColorChooser("Game foreground"));
		makeComponent(1, 4, new ColorChooser("Game background"));
		makeComponent(1, 5, new ColorChooser("Chat area"));
		makeComponent(1, 6, OKButton);
		makeComponent(2, 6, CancelButton);
		makeComponent(3, 6, ApplyButton);
		pack();
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
				client.getRootPane().requestFocus();		
			}
		});
		setVisible(true);
	}
	
	/**
	*
	*/

	public void makeComponent(int x, int y, JComponent c) {

		constraints.gridx = x;
		constraints.gridy = y;
		constraints.fill = GridBagConstraints.NONE;
		constraints.insets = new Insets(3, 3, 3, 3);
		if(y == 0) {
			if(x <= 1 ) {
				constraints.anchor = GridBagConstraints.WEST;
			}
			else {
				constraints.anchor = GridBagConstraints.CENTER;
			}				
		}
		else if (y <= 5) {
			constraints.gridwidth = 3;		
			if(x == 0) {
				constraints.anchor = GridBagConstraints.WEST;
			}
			else {
				constraints.anchor = GridBagConstraints.EAST;
			}	
		}
		else {
			constraints.gridwidth = 1;
			constraints.anchor = GridBagConstraints.WEST;
		}

		menuLayout.setConstraints(c, constraints);
		add(c);
	}
	
	/**
	*
	*/
	
	class ColorChooser extends JPanel implements ItemListener {

		private GridBagLayout colorChooserLayout = new GridBagLayout();
		private GridBagConstraints ccc = new GridBagConstraints();
		private ColorPane colorPane = new ColorPane();
		private JColorComboBox comboBox = new JColorComboBox();
		String name;
		String colorCode;
		Color color;
		int R;
		int G;
		int B;


		/**
		*
		*/
	
		ColorChooser(String name) {
			this.name = name;
			colorCode = (String)settings.get(name);
			color = Color.decode(colorCode);

			R = color.getRed();
			G = color.getGreen();
			B = color.getBlue();
			colorPane.setText(R, G, B);
			
			comboBox.setSelectedItem(colorCode);
			comboBox.addItemListener(this);
			comboBox.setFocusable(false);
			
			setLayout(colorChooserLayout);
			makeComponent(0, 0, new JLabel(name));
			makeComponent(1, 0, colorPane);
			makeComponent(2, 0, comboBox);
		}

		/**
		*
		*/

		public void makeComponent(int x, int y, JComponent c) {
			ccc.gridx = x;
			ccc.gridy = y;
			ccc.insets = new Insets(3, 3, 3, 3);
			colorChooserLayout.setConstraints(c, ccc);
			add(c);
		}
		
		/**
		*
		*/
			
		public void itemStateChanged(ItemEvent event) {

			Object item = event.getItem();
			if (event.getStateChange() == ItemEvent.SELECTED) {
				colorCode = item.toString();
			}
			
            R = Integer.valueOf(colorCode.substring(1, 3), 16);
            G = Integer.valueOf(colorCode.substring(3, 5), 16);
            B = Integer.valueOf(colorCode.substring(5, 7), 16);


			colorPane.setText(R, G, B);
			
			newSettings.put(name, colorCode);
			
			double luminance = 0.2126*R + 0.7152*G + 0.0722*B;

			if(luminance > 40)
				newSettings.put(name + " text", "#000000");
			else 
				newSettings.put(name + " text", "#FFFFFF");
        } 
		
		/**
		*
		*/
		
		class ColorPane extends JPanel implements ActionListener, KeyListener {
			
			JTextField textFieldR = new JTextField(3);
			JTextField textFieldG = new JTextField(3);
			JTextField textFieldB = new JTextField(3);
			
			/**
			*
			*/
			
			ColorPane() {
				setPreferredSize(new Dimension(100, 40));
				setLayout(new GridLayout(2, 3));
				add(textFieldR);
				add(textFieldG);
				add(textFieldB);
				add(new JLabel("R"));
				add(new JLabel("G"));
				add(new JLabel("B"));

				textFieldR.addFocusListener(new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						
					}
				});

				textFieldR.addActionListener(this);
				textFieldG.addActionListener(this);
				textFieldB.addActionListener(this);
				textFieldR.addKeyListener(this);
				textFieldG.addKeyListener(this);				
				textFieldB.addKeyListener(this);				
			}
			
			/**
			*
			*/
			
			void setText(int R, int G, int B) {
				textFieldR.setText(R + "");
				textFieldG.setText(G + "");
				textFieldB.setText(B + "");
			}
			
			/**
			*
			*/
			
			public void actionPerformed(ActionEvent evt) {
				
				R = Integer.parseInt(textFieldR.getText());
				G = Integer.parseInt(textFieldG.getText());
				B = Integer.parseInt(textFieldB.getText());

				colorCode = String.format("#%02X%02X%02X", R, G, B);
				
				comboBox.setSelectedItem(colorCode);
				newSettings.put(name, colorCode);

				double luminance = 0.2126*R + 0.7152*G + 0.0722*B;
				if(luminance > 40)
					newSettings.put(name + " text", "#000000");
				else 
					newSettings.put(name + " text", "#FFFFFF");			
			}
			
			public void keyPressed(KeyEvent key) {
				
			}
			
			public void keyReleased(KeyEvent key) {
				
			}
			
			/**
			* Allow only numerical entries beteween 0 and 255
			*/
			
			public void keyTyped(KeyEvent key) {	
				char c = key.getKeyChar();
				if( (c < '0' || c > '9') && c != KeyEvent.VK_BACK_SPACE) {
					key.consume(); 
				}
				
				JTextField source = (JTextField)key.getSource();
				String text = source.getText();
				if(text.isEmpty()) {
					source.setText("0");
				}
				else if(text.startsWith("0")) {
					source.setText(text.substring(1));
				}
				else if(Integer.parseInt(source.getText()) > 255) {
					source.setText("255");
				}
			}
		}
	}
		
	/**
	*
	*/
	
	public void actionPerformed(ActionEvent evt) {

		if(evt.getSource() == OKButton) {
			updateSettings();
			saveSettingsToFile();
			setVisible(false);
		}
		else if(evt.getSource() == CancelButton) {
			newSettings = settings;
			setVisible(false);
		}
		else if(evt.getSource() == ApplyButton) {
			updateSettings();
			saveSettingsToFile();
		}
	}
	
	/**
	*
	*/
	
	private void updateSettings() {
		
		newSettings.put("lexicon", (String)lexiconChooser.getSelectedItem());
		newSettings.put("play sounds", soundChooser.isSelected() + "");
		
		for(String key : settings.keySet()) {
			if(!newSettings.containsKey(key)) {
				newSettings.put(key, settings.get(key));
			}
		}
		client.settings = newSettings;
		client.setColors();
	}
	

	/**
	*
	*/

	void saveSettingsToFile() {
		
		try {

			File settingsFile = new File(client.settingsPath);
			settingsFile.getParentFile().mkdirs();
			FileOutputStream fileOut = new FileOutputStream(client.settingsPath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(newSettings);
			out.close();
			fileOut.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (SecurityException se) {
			System.out.println("Permission to write to " + client.settingsPath + " denied.");
		}
	}
	
	/**
	*
	*/

	public static void main(String[] args) {

		SettingsMenu settingsMenu = new SettingsMenu(null, 300, 300); 
		settingsMenu.setVisible(true);
	}


}
