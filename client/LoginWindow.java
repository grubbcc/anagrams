import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;

/**
*
*
*/

public class LoginWindow extends JDialog implements ActionListener, KeyListener {

	private JButton loginButton = new JButton("Login");
	private boolean loginButtonClicked = false;
	private JTextField usernameField = new JTextField("", 35);
	private GridBagLayout menuLayout = new GridBagLayout();
	private GridBagConstraints constraints;
	private AnagramsClient client;
	private String username;
	
	/**
	*
	*/
	
	public LoginWindow(AnagramsClient client, int x, int y) {
		this.client = client;

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
	
		setTitle("Login");
		setModal(true);
		loginButton.setEnabled(false);
		loginButton.addActionListener(this);
		loginButton.addKeyListener(this);
		usernameField.addKeyListener(this);
		setLayout(menuLayout);
		
		makeComponent(0, 0, new JLabel("Enter a username"));
		makeComponent(0, 1, usernameField);
		makeComponent(1, 0, loginButton);

		pack();
		setLocation(x + 700/2 - getWidth()/2, y + 500/2 - getHeight()/2);		
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
		doLogin();
	}
	
	
	/**
	*
	*/

	private void doLogin() {
		setVisible(false);
		try {
			if(client.login(username)) {
				//login successful

				dispose();
			}
			else {
				setVisible(true);
			}
		}
		catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	/**
	*
	*/
	
	public void keyPressed(KeyEvent evt) {
		if(evt.getKeyChar() == evt.VK_ENTER) {
			loginButton.doClick();
		}
	}
	
	/**
	*
	*/

	public void keyTyped(KeyEvent e) {
	}
	
	/**
	*
	*/

	public void keyReleased(KeyEvent e) {
		username = usernameField.getText().replace(" ","_");
		if (username.length() > 0 && username.length() < 21) {
			loginButton.setEnabled(true);
		}
		else {
			loginButton.setEnabled(false);
		}		
	}
}
