package client;

import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Base64;

/**
 * Pop-up window used for registering a new account, logging in as a registered
 * user, or logging in as a guest.
 *
 */
class LoginMenu extends PopWindow {

    private final AnagramsClient client;
    private final GridPane grid = new GridPane();

    private final Label instructionLabel = new Label("Enter a username");
    private final Label warningLabel = new Label("");

    private final TextField usernameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final TextField codeField = new TextField();

    private final Button registerButton = new Button("Register");
    private final Button loginButton = new Button("Log in");
    private final Button guestButton = new Button("Play as guest");
    private final Button submitButton = new Button("Log in");
    private final Button sendButton = new Button("Send code");
    private final Button sendPasswordButton = new Button("Send");
    private final Button sendUsernameButton = new Button("Send");
    private final Button confirmButton = new Button("Confirm");
    private final Button cancelButton = new Button("Cancel");


    /**
     *
     */
    LoginMenu(AnagramsClient client) {
        super(client.stack);
        this.client = client;

        if(client.getWebAPI().isMobile()) {
            setScaleX(1.45); setScaleY(1.45);
        }

        usernameField.setPromptText("Username");
        emailField.setPromptText("Email address");
        passwordField.setPromptText("Password");
        confirmField.setPromptText("Confirm password");
        codeField.setPromptText("Enter code from email");
//        codeField.getProperties().put("vkType", "numeric");

        warningLabel.setStyle("-fx-text-fill: red;");

        usernameField.setPrefWidth(300);
        registerButton.setPrefWidth(85);
        loginButton.setPrefWidth(85);
        guestButton.setPrefWidth(100);
        submitButton.setPrefWidth(90);
        sendButton.setPrefWidth(90);
        sendPasswordButton.setPrefWidth(90);
        sendUsernameButton.setPrefWidth(90);
        confirmButton.setPrefWidth(90);
        cancelButton.setPrefWidth(90);

        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            usernameField.setText(newValue.replaceAll("[ /]", ""));
            if(newValue.length() > 21 )
                usernameField.setText(newValue.substring(0, 21));
            registerButton.setDisable(isInvalidUsername(newValue));
            loginButton.setDisable(isInvalidUsername(newValue));
            guestButton.setDisable(isInvalidUsername(newValue));
        });

        codeField.textProperty().addListener((observable, oldValue, newValue) -> {
            codeField.setText(codeField.getText().replaceAll("[^0-9]", ""));
            confirmButton.setDisable(codeField.getText().isEmpty());
        });

        registerButton.setDisable(true);
        loginButton.setDisable(true);
        guestButton.setDisable(true);
        confirmButton.setDisable(true);

        submitButton.disableProperty().bind(passwordField.textProperty().isEmpty());
        sendButton.disableProperty().bind(emailField.textProperty().isEmpty().or(passwordField.textProperty().isEmpty()).or(confirmField.textProperty().isEmpty()));

        grid.setPadding(new Insets(5));
        grid.setHgap(5);
        grid.setVgap(5);

        GridPane.setConstraints(instructionLabel, 0, 0, 3, 1);

        GridPane.setConstraints(usernameField, 0, 1, 3, 1);
        GridPane.setConstraints(emailField, 0, 2, 3, 1);
        GridPane.setConstraints(passwordField, 0, 3, 1, 1, HPos.LEFT, VPos.BASELINE, Priority.ALWAYS, Priority.NEVER);
        GridPane.setConstraints(confirmField, 2, 3, 1, 1, HPos.RIGHT, VPos.BASELINE, Priority.ALWAYS, Priority.NEVER);
        GridPane.setConstraints(codeField, 0, 2, 3, 1);

        GridPane.setConstraints(warningLabel, 0, 4, 3, 1);

        GridPane.setConstraints(confirmButton,0, 5);
        GridPane.setConstraints(registerButton, 0, 5);
        GridPane.setConstraints(sendButton, 0, 5);
        GridPane.setConstraints(sendUsernameButton, 0 , 5);
        GridPane.setConstraints(sendPasswordButton, 0, 5);
        GridPane.setConstraints(submitButton, 0, 5);
        GridPane.setConstraints(loginButton, 1, 5);
        GridPane.setConstraints(guestButton, 2, 5);
        GridPane.setConstraints(cancelButton, 2, 5);

        guestButton.setOnAction(this::guestAction);
        loginButton.setOnAction(this::loginAction);
        registerButton.setOnAction(this::registerAction);
        submitButton.setOnAction(e -> {if(checkPassword()) login(usernameField.getText(), false);});
        cancelButton.setOnAction(this::cancelAction);
        confirmButton.setOnAction(this::confirmAction);
        sendButton.setOnAction(this::sendAction);
        sendPasswordButton.setOnAction(this::sendPassword);
        sendUsernameButton.setOnAction(this::sendUsername);

        confirmButton.setDefaultButton(true);
        submitButton.setDefaultButton(true);
        sendUsernameButton.setDefaultButton(true);
        sendPasswordButton.setDefaultButton(true);
        sendButton.setDefaultButton(true);
        cancelButton.setCancelButton(true);

        grid.getChildren().addAll(instructionLabel, usernameField, warningLabel, registerButton, loginButton, guestButton);

        titleBar.getChildren().remove(closeButton);
        setMaxSize(300, 210);
        setTitle("Log in");
        setContents(grid);
        setAsDragZone(grid);
        show(true);
    }

    /**
     * Restores menu to original state, retaining only the entered username
     *
     * @param actionEvent a click on the cancelButton
     */
    private void cancelAction(ActionEvent actionEvent) {
        usernameField.setDisable(false);
        emailField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        codeField.setText("");
        instructionLabel.setText("Enter a username");
        warningLabel.setText("");
        warningLabel.setOnMouseClicked(null);
        grid.getChildren().clear();
        grid.getChildren().addAll(instructionLabel, usernameField, warningLabel, registerButton, loginButton, guestButton);
    }

    /**
     * Asks the client to attempt login with this username.
     *
     * @param guest whether the player has chosen to log in as a guest
     */
    private void login(String username, boolean guest) {
        client.send("login", new JSONObject().put("name", username).put("guest", guest));

        try {
            JSONObject json = new JSONObject(client.bufferedIn.readLine());
            if(json.getString("cmd").equals("login")) {
                client.login(username, guest, json.getJSONObject("prefs"));
                hide();
            }
        }

        catch(IOException ioe) {
            disconnect();
        }
    }


    /**
     * If the provided username is available, then proceed to loginAction.
     * Otherwise, display message indicating whether the username is registered
     * or is currently being used.
     *
     * @param actionEvent a click on the guestButton
     */
    private void guestAction(ActionEvent actionEvent) {
        String username = usernameField.getText();
        client.send("username", new JSONObject().put("username", username));
        try {
            JSONObject json = new JSONObject(client.bufferedIn.readLine());

            if (json.getString("cmd").equals("availability")) {
                if (json.getBoolean("available")) {
                    login(username, true);
                } else if (json.getBoolean("registered")) {
                    MessageDialog dialog = new MessageDialog(client, "Registered username");
                    dialog.setText("<center>The username you entered has already been<br> registered. " +
                            "Please choose a different name<br> or log in using the password.</center>");
                    dialog.addOkayButton();
                    dialog.show(true);
                } else {
                    MessageDialog dialog = new MessageDialog(client, "Multiple logins");
                    dialog.setText("<center>A user with this username is already logged in.</center>");
                    dialog.addOkayButton();
                    dialog.show(true);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if there is an account with a password associated with the provided username.
     * If there is, the user is prompted to enter their password. Otherwise, prompts the user
     * to register, to log in as a guest, or to recover their username.
     *
     * @param actionEvent a click on the loginButton
     */
    private void loginAction(ActionEvent actionEvent) {
        String username = usernameField.getText();
        client.send("username", new JSONObject().put("username", username));
        try {
            JSONObject json = new JSONObject(client.bufferedIn.readLine());

            if (json.getString("cmd").equals("availability")) {
                if (!json.getBoolean("registered")) {
                    MessageDialog dialog = new MessageDialog(client, "Username not recognized");
                    dialog.setText("<center>The username you entered has not been registered. <br>" +
                            "Please register or choose \"Play as guest\".</center>");
                    dialog.addOkayButton();
                    dialog.show(true);
                    warningLabel.setText("Forgot username?");
                    warningLabel.setOnMouseClicked(this::forgotUsernameAction);
                    return;
                }
            }
        }
        catch(IOException ioe) {
            disconnect();
        }

        warningLabel.setText("");
        grid.getChildren().removeAll(registerButton, loginButton, guestButton);
        grid.getChildren().addAll(passwordField, submitButton, cancelButton);
        usernameField.setDisable(true);
        passwordField.requestFocus();
        passwordField.requestFocus();
    }

    /**
     * Checks whether the provided password matches the stored data
     *
     * @return true if the password matches, false otherwise
     */
    private boolean checkPassword() {
        client.send("password", new JSONObject()
                .put("name", usernameField.getText())
                .put("password", Base64.getEncoder().encodeToString(passwordField.getText().getBytes()))
        );
        try {
            JSONObject json = new JSONObject(client.bufferedIn.readLine());
            System.out.println(json);
            if(json.getString("cmd").equals("password")) {
                if(json.getBoolean("valid")) {
                    return true;
                }
                else {
                    warningLabel.setText("Incorrect password. Forgot?");
                    warningLabel.setOnMouseClicked(this::forgotPasswordAction);
                    return false;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }



    /**
     * @param actionEvent a click on the registerButton.
     */
    private void registerAction(ActionEvent actionEvent) {
        instructionLabel.setText("Username registration");
        warningLabel.setText("");
        String username = usernameField.getText();
        client.send("username", new JSONObject().put("username", username));
        try {
            JSONObject json = new JSONObject(client.bufferedIn.readLine());

            if (json.getString("cmd").equals("availability")) {
                if (json.getBoolean("available")) {
                    instructionLabel.setText("");
                    usernameField.setDisable(true);
                    grid.getChildren().removeAll(loginButton, guestButton, registerButton);
                    grid.getChildren().addAll(emailField, passwordField, confirmField, sendButton, cancelButton);
                }
                else if(json.getBoolean("registered")){
                    MessageDialog dialog = new MessageDialog(client, "Registered username");

                    dialog.setText("<center>The username you entered has already<br> been registered. " +
                            "Please choose a different<br>name or log in using the password.</center>");
                    dialog.addOkayButton();
                    dialog.show(true);
                }
                else {
                    MessageDialog dialog = new MessageDialog(client, "Multiple logins");
                    dialog.setText("<center>A user with this username is already logged in.</center>");
                    dialog.addOkayButton();
                    dialog.show(true);
                }
            }
        }
        catch(IOException ioe) {
            disconnect();
        }

    }


    /**
     * Attempts to send an email containing a 6-digit code to the provided email address
     * in order to verify ownership of the account.
     *
     * @param actionEvent a click on the sendButton
     */
    private void sendAction(ActionEvent actionEvent) {
        String email = emailField.getText();

        if (isInvalidEmailAddress(email)) {
            warningLabel.setText("Not a valid email address");
            return;
        }
        else if(!passwordField.getText().equals(confirmField.getText())) {
            warningLabel.setText("Passwords do not match");
            return;
        }

        client.send("email", new JSONObject().put("email", email));
        try {
            JSONObject json = new JSONObject(client.bufferedIn.readLine());

            if (json.getString("cmd").equals("availability")) {
                if (json.getBoolean("available")) {

                    instructionLabel.setText("A confirmation email has been sent to\n" + email + ".\n" +
                            "Enter the 6-digit code to verify.");
                    warningLabel.setText("");
                    grid.getChildren().removeAll(emailField, usernameField, passwordField, confirmField, sendButton);
                    grid.getChildren().addAll(codeField, confirmButton);
                    codeField.requestFocus();
                }
                else {
                    MessageDialog dialog = new MessageDialog(client, "Invalid email");
                    dialog.setText("This email address is already registered to a username\n" +
                            "Please enter a different email address or click \"Forgot username?\"");
                    dialog.addOkayButton();
                    dialog.show(true);
                    warningLabel.setText("Forgot username?");
                    warningLabel.setOnMouseClicked(this::forgotUsernameAction);
                }
            }
        }
        catch(IOException ioe) {
            disconnect();
        }
    }

    /**
     *
     */
    private void confirmAction(ActionEvent actionEvent) {

        String code = codeField.getText();
        String username = usernameField.getText();

        if(!code.matches("[0-9]{6}")) {
            warningLabel.setText("The code you entered does not match.");
            return;
        }
        client.send("register", new JSONObject()
                .put("username", username)
                .put("code", code)
                .put("email", emailField.getText())
                .put("password", Base64.getEncoder().encodeToString(passwordField.getText().getBytes()))
        );
        try {
            JSONObject json = new JSONObject(client.bufferedIn.readLine());
            if(json.getString("cmd").equals("login")) {
                hide();

                MessageDialog dialog = new MessageDialog(client, "Registration successful");
                dialog.setText("<center>Congratulations, you have successfully registered the username\n" +
                        username + "." +
                        "\nYou may delete this account at any time by\n" +
                        "clicking your name in the right panel.</center>");
                dialog.addOkayButton();

                dialog.closeButton.setOnAction(e -> {
                    client.login(username, false, json.getJSONObject("prefs"));
                    dialog.hide();
                });
                dialog.okayButton.setOnAction(e -> {
                    client.login(username, false, json.getJSONObject("prefs"));
                    dialog.hide();
                });

                dialog.show(true);

            }
            else {
                warningLabel.setText("The code you entered does not match.");
            }
        }
        catch(IOException ioe) {
            disconnect();
        }
    }


    /**
     * Prompts user to enter an email address for username recovery.
     *
     * @param click a click on the forgot username label
     */
    private void forgotUsernameAction(MouseEvent click) {
        warningLabel.setText("");
        warningLabel.setOnMouseClicked(null);
        grid.getChildren().clear();
        grid.getChildren().addAll(instructionLabel, emailField, warningLabel, sendUsernameButton, cancelButton);
        instructionLabel.setText("Enter your email address. If there is an\n" +
                "associated username, it will be sent to you.");
    }

    /**
     * Asks the server to send the username associated with the given email, if any.
     * @param actionEvent A click on the sendUsername Button
     */
    private void sendUsername(ActionEvent actionEvent) {
        String email = emailField.getText();
        if(isInvalidEmailAddress(email)) {
            warningLabel.setText("Invalid email");
            return;
        }

        client.send("forgot", new JSONObject().put("type", "username").put("email", email));

        try {
            JSONObject json = new JSONObject(client.bufferedIn.readLine());
            if(json.getString("cmd").equals("username-recovery")) {
                if(json.getBoolean("success")) {
                    MessageDialog dialog = new MessageDialog(client, "Successful recovery");
                    dialog.setText("<center>An email containing your username has been<br>" +
                            "sent to <a href=https://" + email + ">" + email + "</a>.</center>");
                    dialog.addOkayButton();
                    dialog.show(true);

                    grid.getChildren().removeAll(sendUsernameButton, emailField);
                    instructionLabel.setText("");
                    usernameField.setText("");
                    warningLabel.setText("");
                    passwordField.setText("");
                    grid.getChildren().addAll(usernameField, passwordField, submitButton);
                }
                else {
                    MessageDialog dialog = new MessageDialog(client, "Email not recognized");
                    dialog.setText("<center>There is no account with the provided email address</center>");
                    dialog.addOkayButton();
                    dialog.show(true);
                    emailField.selectAll();
                }
            }
        }
        catch(IOException ioe) {
            disconnect();
        }
    }

    /**
     * Prompts user to enter an email address for password recovery.
     *
     * @param click a click on the forgot password label
     */
    private void forgotPasswordAction(MouseEvent click) {
        grid.getChildren().clear();
        grid.getChildren().addAll(instructionLabel, emailField, warningLabel, sendPasswordButton, cancelButton);

        warningLabel.setText("");
        warningLabel.setOnMouseClicked(null);

        instructionLabel.setText("Enter your email address. If there is an\nassociated password, it will be sent to you.");
    }


    /**
     * Attempts to send an message containing the registered user's password to the associated email.
     *
     * @param actionEvent a click on the sendPassword Button
     */
    private void sendPassword(ActionEvent actionEvent) {
        String email = emailField.getText();
        if (isInvalidEmailAddress(email)) {
            warningLabel.setText("Invalid email");
            return;
        }

        client.send("forgot", new JSONObject().put("type", "password").put("email", email));

        try {
            JSONObject json = new JSONObject(client.bufferedIn.readLine());
            if (json.getString("cmd").equals("password-recovery")) {
                if (json.getBoolean("success")) {
                    MessageDialog dialog = new MessageDialog(client, "Successful recovery");
                    dialog.setText("<center>An email containing your password has been<br>" +
                            "sent to <a href=https://" + email + ">" + email + "</a>.</center>");
                    dialog.addOkayButton();
                    dialog.show(true);

                    grid.getChildren().removeAll(sendPasswordButton, emailField);
                    instructionLabel.setText("");
                    warningLabel.setText("");
                    passwordField.setText("");
                    usernameField.setDisable(true);
                    grid.getChildren().addAll(usernameField, passwordField, submitButton);
                } else {
                    MessageDialog dialog = new MessageDialog(client, "Email not recognized");
                    dialog.setText("<center>There is no account with the provided email address</center>");
                    dialog.addOkayButton();
                    dialog.show(true);
                    emailField.selectAll();
                }
            }
        } catch(IOException ioe) {
            disconnect();
        }
    }

    /**
     *
     */
    private void disconnect() {
        System.out.println("The connection between client and host has been lost.");

        MessageDialog dialog = new MessageDialog(client, "Connection error");
        dialog.setText("The connection to the server has been lost. Try to reconnect?");
        dialog.addYesNoButtons();
        dialog.yesButton.setOnAction(e -> client.getWebAPI().executeScript("window.location.reload(false)"));
        dialog.noButton.setOnAction(e -> dialog.hide());
        dialog.show(true);
    }


    /**
     * Checks if the email address has between 1 and 21 characters and does not start with "Robot".
     * @return true if the username is invalid, false otherwise
     */
    private static boolean isInvalidUsername(String username) {
        return username.isBlank() || username.length() > 21 || username.startsWith("Robot");
    }

    /**
     * Checks if the contents of the emailField is a valid email address.
     * @return true if the email address is invalid, false otherwise
     */
    private static boolean isInvalidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return !m.matches();
    }

}