import java.awt.Point;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.SwingUtilities;
import java.awt.event.*;
import java.awt.Component;
import javax.swing.event.*;
import javax.swing.JTree;
import java.util.Vector;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Enumeration;
import java.io.*;

/****
*
* A utility allowing the user to create new WordTrees and read definitions
*
* TO DO: make it so the scrollbar appears only on the TreePanel
*		 put everything in a package, make jar file, etc
*
* 2) scroll to top when new wordlist created
* 3) allow user to choose between extensions or steals
* 4) make messagepane scrollable when number of steals is large
*
*****/

public class WordExplorer extends JDialog implements ActionListener {

	private String lexicon;
	private JTextField textField = new JTextField("", 15);
	private JButton goButton = new JButton("Go");
	private JComboBox<String> lexiconSelector;
	private String[] lexicons = {"CSW19", "NWL18", "LONG"};	
	private TreeMap<String, AlphagramTrie> tries = new TreeMap<String, AlphagramTrie>();
	private WordTree tree;
	private JTextArea messagePane = new JTextArea();
	private JPanel controlPanel = new JPanel();
	private JPanel treePanel = new JPanel();
	private JPanel messagePanel = new JPanel();
	private TreeSet<DefaultMutableTreeNode> nodeList;


	
	/**
	* Create a WordExplorer using the given lexicon
	*/
		
	public WordExplorer(AlphagramTrie trie) {
	
		//Create list of lexicons from wordlist directory
/**		File folder = new File("wordlists");
		String[] wordLists = folder.list();
		for(String wordList : wordLists)
			if(wordList.endsWith(".txt"))8
				lexicons.add(wordList.substring(0, wordList.length() - 4));*/

		

		//Create the WordTree
		this.lexicon = trie.lexicon;
		tries.put(lexicon, trie);
		tree = new WordTree(trie);

		//Set frame parameters
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		setSize(400, 550);
		setMinimumSize(new Dimension(350, 200));
        setContentPane(mainPanel);
		setLayout(new BorderLayout());
		setAlwaysOnTop(true);
		setVisible(false);
		
		//Top panel
		controlPanel.setBackground(Color.BLUE);
		textField.addActionListener(ae -> {goButton.doClick();});
		controlPanel.add(textField);
		goButton.addActionListener(this);
		controlPanel.add(goButton);					
		lexiconSelector = new JComboBox<String>(lexicons);	
		lexiconSelector.addActionListener(this);
		lexiconSelector.setSelectedItem(lexicon);
		controlPanel.add(lexiconSelector);
		

		//Middle panel
		treePanel.setBackground(Color.WHITE);
		treePanel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(treePanel);
//		setUpTree();
		
		//Bottom panel
		messagePanel.setBackground(Color.GREEN);
		messagePanel.setLayout(new BorderLayout());		
		setUpMessagePane();

		//Main panel
		mainPanel.add(controlPanel, BorderLayout.PAGE_START);
        mainPanel.add(scrollPane);
		mainPanel.add(messagePanel, BorderLayout.PAGE_END);
	
		revalidate();
	}
	
	/**
	* Responds to clicks, double-clicks, and keystrokes
	*/

	public void actionPerformed(ActionEvent evt) {

		if(evt.getSource() == lexiconSelector) {
			lexicon = (String)lexiconSelector.getSelectedItem();
	
			if(!tries.containsKey(lexicon)) {
				WordTree newTree = new WordTree(lexicon);
				//catch file not found errors
				tries.put(lexicon, newTree.trie);
			}
			messagePanel.revalidate();
		}
		
		else if (evt.getSource() == goButton) {
			
			if(textField.getText().length() < 4) {
				messagePane.setText("You must enter a word of 4 or more letters.");
				messagePanel.revalidate();
			}
			else {
				lookUp(textField.getText());
			}
		}
	}
	
	/**
	*
	*/

	public void lookUp(String query) {
		treePanel.removeAll();
		tree = new WordTree(new DefaultMutableTreeNode(new UserObject(query.toUpperCase(), "")), tries.get(lexicon));
		setUpTree();
		
		messagePanel.removeAll();

		if(tree.root.getChildCount() > 0) {
			messagePane.setText("");
			messagePanel.add(tree.treeSummary(), BorderLayout.LINE_END);
		}
		else {
			messagePane.setText("This word cannot be stolen.");

		}
		messagePanel.revalidate();
		setUpMessagePane();
		
	}
	
	/**
	*
	*/
	
	public void lookUp(String query, String tilePool) {
		treePanel.removeAll();
		tree = new WordTree(new DefaultMutableTreeNode(new UserObject(query.toUpperCase(), "")), tries.get(lexicon));
		setUpTree();
		
		messagePanel.removeAll();

		if(tree.root.getChildCount() > 0) {
			messagePane.setText("");
			messagePanel.add(tree.treeSummary(), BorderLayout.LINE_END);
		}
		else {
			messagePane.setText("This word cannot be stolen.");

		}
		messagePanel.revalidate();
		setUpMessagePane();		
		
	}

	
	/**
	*
	*/

	public class ToolTipTreeRenderer  extends DefaultTreeCellRenderer  {	
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			final Component rc = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);		

			if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
				Object object = ((DefaultMutableTreeNode) value).getUserObject();
				if (object instanceof UserObject) {
					UserObject uo = (UserObject) object;
					if(!uo.getToolTip().isEmpty()) {
						this.setToolTipText(uo.getToolTip());
					}
					else {
						System.out.println("nullify");
						this.setToolTipText(null);
					}
				}
			}
 			
			return rc;
		}

	}	
	
	/**
	* Performs routine actions whenever a new WordTree is created.
	*/
	
	private void setUpTree() {
		treePanel.add(tree, BorderLayout.LINE_START);
		tree.setRootVisible(true);
		tree.expandRow(0);

		//add tool tips
		tree.setCellRenderer(new ToolTipTreeRenderer());
		javax.swing.ToolTipManager.sharedInstance().registerComponent(tree);

		//find steals of selected node when user pressers enter
		KeyListener keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_ENTER && tree.getSelectionCount() == 1) {
					
					DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
					String selectedWord = selectedNode.toString();
					textField.setText(selectedWord);
					goButton.doClick();
				}
			}
		};
		
		//display definition when node is selected
		TreeSelectionListener selectionListener = new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

				if (selectedNode == null) {
					return;
				}

				String selectedWord = selectedNode.toString();
				String definition = tree.trie.getDefinition(selectedWord);
				if(definition != null)
					messagePane.setText(tree.trie.getDefinition(selectedWord));
				else {
					messagePane.setText("Definition not available");
					messagePanel.revalidate();
				}
			}
		};

		//respond to mouse actions
		MouseListener mouseListener = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if(selRow != -1) {
					String selectedWord = selPath.getLastPathComponent().toString();

					//allow user to save list on right click
					if (SwingUtilities.isRightMouseButton(e)) {
						doPop(e);
					}
					
					//find steals of selected node on double-click
					else if(e.getClickCount() == 2) {
						textField.setText(selectedWord);
						goButton.doClick();
					}			
				}					
			}		
		};
		
		tree.addKeyListener(keyListener);		
		tree.addTreeSelectionListener(selectionListener);
		tree.addMouseListener(mouseListener);

		treePanel.repaint();
	}
	
	/**
	*
	*/

	private void doPop(MouseEvent e){
		JPopupMenu menu = new JPopupMenu();
		JMenuItem menuItem = new JMenuItem("Save List to File");
		menu.add(menuItem);
	
		MouseListener mouseListener = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {

	/**			try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} 
				catch (Exception ex) {
					ex.printStackTrace();
				*/
	
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File(System.getProperty("user.dir")+"/saved lists"));

				JTextField fileChooserTextField = getFileChooserTextField(chooser);
				fileChooserTextField.setText(tree.root.toString() + ".txt");			
				int returnVal = chooser.showSaveDialog(treePanel);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
				
					File file = chooser.getSelectedFile();
					
					if (!file.getName().toLowerCase().endsWith(".txt")) {
						file = new File(file.getParentFile(), file.getName() + ".txt");
					}
					try {
						nodeList = new TreeSet<DefaultMutableTreeNode>(new WordTree.TreeNodeComparator());
						visitAllNodes(tree.root);
						PrintStream out = new PrintStream(new FileOutputStream(file));
						while(!nodeList.isEmpty()) {
							out.print(nodeList.pollLast().toString() + "\n");
						}
						out.flush();
						out.close();
					}
					catch (Exception ex) {
						ex.printStackTrace();	
					}			
				}
			}
		};
		
		menuItem.addMouseListener(mouseListener);

		menu.show(e.getComponent(), e.getX(), e.getY());
	}
	
	/**
	*
	*/

    private static JTextField getFileChooserTextField(JFileChooser chooser) {
        JTextField f = null;
        for (Component c : getComponents(chooser)) {
            if (c instanceof JTextField){
                f = (JTextField) c;
                break;
            }
        }

        return f;
    }
	
	/**
	*
	*/

    private static Vector<Component> getComponents(JComponent component) {
        Vector<Component> list = new Vector<>();
        for (Component c : component.getComponents()) {
            if (c instanceof JPanel)
                list.addAll(getComponents((JPanel) c));
            else if (c instanceof JTextField)
                list.add((JTextField) c);
        }
        return list;
    }
	
	/**
	* Recursively traverses the tree and adds each node to the nodeList for export to file
	*/
	
	public void visitAllNodes(DefaultMutableTreeNode node) {
		nodeList.add(node);
		
		if (node.getChildCount() >= 0) {
			for (Enumeration e = node.children(); e.hasMoreElements();) {
				DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
				visitAllNodes(n);
			}
		}
	}


	/**
	* Performs routine actions whenever a new messagePane is created.
	*/
	
	public void setUpMessagePane() {
		messagePane.setEditable(false);
		messagePane.setWrapStyleWord(true);	
		messagePane.setLineWrap(true);		
		messagePane.setBackground(Color.GREEN);		
		messagePanel.add(messagePane, BorderLayout.CENTER);
		messagePanel.revalidate();
	}
	
}
