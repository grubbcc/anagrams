package client;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import java.io.IOException;

/**
 *
 *
 */

public class LoginWindow extends PopWindow {

	private final AnagramsClient client;
	private final Button loginButton = new Button("Login");
	private final TextField usernameField = new TextField();

	/**
	 *
	 */
	
	public LoginWindow(AnagramsClient client) {
		super(client.stack);
		this.client = client;

		Label loginLabel = new Label("Enter a username");

		usernameField.setPrefWidth(275);
		usernameField.textProperty().addListener((observable, oldValue, newValue) ->
			loginButton.setDisable(newValue.isBlank() || newValue.length() > 21)
		);

		loginButton.setDisable(true);
		loginButton.setOnAction(e -> doLogin(usernameField.getText()));

		GridPane grid = new GridPane();
		grid.setPadding(new Insets(5));
		grid.setHgap(5);
		grid.setVgap(5);

		GridPane.setConstraints(loginLabel, 0, 0);
		GridPane.setConstraints(usernameField, 0, 1);
		GridPane.setConstraints(loginButton, 1, 1);
		grid.getChildren().addAll(loginLabel, loginButton, usernameField);

		titleBar.getChildren().remove(closeButton);

		grid.setOnKeyPressed(this::handleKeystroke);
		setMaxSize(350, 115);
		setTitle("Log in");
		setContents(grid);
	}

	/**
	 * Responds to a user pressing ENTER by firing the loginButton.
	 *
	 * @param event any key being pressed
	 */

	public void handleKeystroke(KeyEvent event) {
		if (event.getCode() == KeyCode.ENTER) {
			loginButton.fire();
		}
	}

	/**
	 * Asks the client to attempt login with this username.
	 *
	 * @param username Contents of the textField
	 */

	private void doLogin(String username) {
		try {
			client.login(username.replace(" ", "_"));
		}
		catch (IOException ioException) {
	//		ioException.printStackTrace();
			MessageDialog dialog = new MessageDialog(client, "Connection error");
			dialog.setText("The connection to the server has been lost. Exiting program.");
			dialog.addOkayButton();
			dialog.show(true);
	//		client.logOut();
		}
	}
}