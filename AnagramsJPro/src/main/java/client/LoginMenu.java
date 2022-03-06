package client;

import javafx.application.Platform;
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

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Pop-up window used for registering a new account, logging in as a registered
 * user, or logging in as a guest.
 *
 */

public class LoginMenu extends PopWindow {

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

    private final Preferences prefs = Preferences.userNodeForPackage(getClass());
    private String code;

    private final Properties props = new Properties();
    private final String from = "admin@anagrams.site";
    private final String password = prefs.get("password", "password unavailable");

    private final Session session = Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(from, password);
        }
    });

    /**
     *
     */

    public LoginMenu(AnagramsClient client) {
        super(client.stack);
        this.client = client;

        if(client.getWebAPI().isMobile()) {
            setScaleX(1.45); setScaleY(1.45);
        }

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.host", "mail.anagrams.site");
        props.put("mail.smtp.port", "587");

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
            usernameField.setText(newValue.replace(" ", ""));
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
        submitButton.setOnAction(e -> {if(checkPassword()) login(false);});
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
        show(true);
    }

    /**
     * @param actionEvent a click on the regsterButton.
     */

    private void registerAction(ActionEvent actionEvent) {
        try {
            if(prefs.nodeExists(usernameField.getText())) {
                if (prefs.node(usernameField.getText()).getByteArray("password", null) != null) {

                    MessageDialog dialog = new MessageDialog(client, "Registered username");

                    dialog.setText("<center>The username you entered has already<br> been registered. " +
                            "Please choose a different<br>name or log in using the password.</center>");
                    dialog.addOkayButton();
                    Platform.runLater(() -> dialog.show(true));
                    return;
                }
                else {
                    MessageDialog dialog = new MessageDialog(client, "Multiple logins");
                    dialog.setText("<center>A user with this username is already logged in.</center>");
                    dialog.addOkayButton();
                    Platform.runLater(() -> dialog.show(true));
                    return;
                }
            }
        }
        catch (BackingStoreException e) {
            e.printStackTrace();
            return;
        }

        instructionLabel.setText("");
        usernameField.setDisable(true);
        grid.getChildren().removeAll(loginButton, guestButton, registerButton);
        grid.getChildren().addAll(emailField, passwordField, confirmField, sendButton, cancelButton);
    }

    /**
     * Restores menu to original settings, retaining entered username
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
     * @param actionEvent a click on the guestButton
     */

    private void guestAction(ActionEvent actionEvent) {
        String username = usernameField.getText();
        try {
            if (prefs.nodeExists(username)) {
                if (prefs.node(username).getByteArray("password", null) != null) {
                    MessageDialog dialog = new MessageDialog(client, "Registered username");
                    dialog.setText("<center>The username you entered has already been<br> registered. " +
                            "Please choose a different name<br> or log in using the password.</center>");
                    dialog.addOkayButton();
                    Platform.runLater(() -> dialog.show(true));
                }
                else {
                    MessageDialog dialog = new MessageDialog(client, "Multiple logins");
                    dialog.setText("<center>A user with this username is already logged in.</center>");
                    dialog.addOkayButton();
                    Platform.runLater(() -> dialog.show(true));
                }
            }
            else {
                login(true);
            }
        }
        catch(BackingStoreException bse) {
            bse.printStackTrace();
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

        try {
            if (!prefs.nodeExists(username)) {

                    MessageDialog dialog = new MessageDialog(client, "Username not recognized");
                    dialog.setText("<center>The username you entered has not been registered <br>" +
                            "Please register or choose \"Play as guest\".</center>");
                    dialog.addOkayButton();
                    Platform.runLater(() -> dialog.show(true));
                    warningLabel.setText("Forgot username?");
                    warningLabel.setOnMouseClicked(this::forgotUsernameAction);
                    return;

            }
            if (prefs.node(username).getByteArray("password", null) == null) {

                MessageDialog dialog = new MessageDialog(client, "Username not recognized");
                dialog.setText("<center>The username you entered has not been registered <br>" +
                        "Please register or choose \"Play as guest\".</center>");
                dialog.addOkayButton();
                Platform.runLater(() -> dialog.show(true));
                warningLabel.setText("Forgot username?");
                warningLabel.setOnMouseClicked(this::forgotUsernameAction);
                return;
            }
        }
        catch(BackingStoreException bse) {
            bse.printStackTrace();
        }

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
        String username = usernameField.getText();
        String password = passwordField.getText();

        if(password.equals(new String(Base64.getDecoder().decode((prefs.node(username).getByteArray("password", new byte[10])))))) {
            return true;
        }
        else {
            warningLabel.setText("Incorrect password. Forgot?");
            warningLabel.setOnMouseClicked(this::forgotPasswordAction);
            return false;
        }
    }

    /**
     * Asks the client to attempt login with this username.
     *
     * @param guest whether the player has chosen to login in as a guest
     */


    private void login(boolean guest) {
        String username = usernameField.getText();
        client.send("login " + username);
        String response = "";
        try {
            response = client.bufferedIn.readLine();
        }
        catch (IOException ioe) {
            System.out.println("The connection between client and host has been lost.");

            MessageDialog dialog = new MessageDialog(client, "Connection error");
            dialog.setText("The connection to the server has been lost. Try to reconnect?");
            dialog.addYesNoButtons();
            dialog.yesButton.setOnAction(e -> client.getWebAPI().executeScript("window.location.reload(false)"));
            dialog.noButton.setOnAction(e -> dialog.hide());
            Platform.runLater(() -> dialog.show(true));
        }

        if (response.equals("ok login")) {
            client.guest = guest;
            client.login(username);
            hide();
        }

        //login was unsuccessful
        else if (!response.isEmpty()) {
            MessageDialog dialog = new MessageDialog(client, "Login unsuccessful");
            dialog.setText(response);
            dialog.addOkayButton();
            dialog.show(true);
        }
    }


    /**
     * Attempts to send an email containing a 6-digit code to the provided email address
     * in order to verify the account.
     *
     * @param actionEvent a click on the sendButton
     */

    private void sendAction(ActionEvent actionEvent) {
        String email = emailField.getText();
        try {
            for (String username : prefs.childrenNames()) {
                String registeredEmail = prefs.node(username).get("email", null);
                if(registeredEmail != null) {
                    if (registeredEmail.equals(email)) {
                        MessageDialog dialog = new MessageDialog(client, "Invalid email");

                        dialog.setText("This email address is already registered to a username\n" +
                                "Please enter a different email address or click \"Forgot username?\"");
                        dialog.addOkayButton();
                        dialog.show(true);
                        warningLabel.setText("Forgot username?");
                        warningLabel.setOnMouseClicked(this::forgotUsernameAction);
                        return;
                    }
                }
            }
        }
        catch(BackingStoreException bse) {
            bse.printStackTrace();
        }
        if (isInvalidEmailAddress(email)) {
            warningLabel.setText("Not a valid email address");
        }
        else if(!passwordField.getText().equals(confirmField.getText())) {
            warningLabel.setText("Passwords do not match");
        }
        else {
            try {
                Random rand = new Random();
                code = String.format("%06d", rand.nextInt(1000000));

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                message.setSubject("Anagrams registration");
                message.setText("Your registration code for Anagrams is: " + code);

                ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
                emailExecutor.execute(() -> {
                    try {
                        Transport.send(message);
                        System.out.println("Sent message successfully....");
                    }
                    catch (MessagingException e) {
                        e.printStackTrace();
                    }
                });
                emailExecutor.shutdown();

            }
            catch (MessagingException mex) {
                mex.printStackTrace();
            }

            instructionLabel.setText("A confirmation email has been sent to\n" + email + ".\n" +
                    "Enter the 6-digit code to verify.");
            warningLabel.setText("");
            grid.getChildren().removeAll(emailField, usernameField, passwordField, confirmField, sendButton);
            grid.getChildren().addAll(codeField, confirmButton);
            codeField.requestFocus();
        }
    }

    /**
     *
     */

    private void confirmAction(ActionEvent actionEvent) {
        String entry = codeField.getText();
        if(entry.equals(code)) {
            String username = usernameField.getText();
            prefs.node(username).put("email", emailField.getText());
            prefs.node(username).putByteArray("password", Base64.getEncoder().encode(passwordField.getText().getBytes()));

            MessageDialog dialog = new MessageDialog(client, "Registration successful");
            dialog.setText("<center>Congratulations, you have successfully registered the username\n" +
                    username + "." +
                    "\nYou may delete this account at any time by\n" +
                    "clicking your name in the right panel.</center>");
            dialog.addOkayButton();
            dialog.closeButton.setOnAction(e -> {login(false); dialog.hide();});
            dialog.okayButton.setOnAction(e -> {login(false); dialog.hide();});
            hide();
            dialog.show(true);
        }
        else {
            warningLabel.setText("The code you entered does not match.");
        }
    }

    /**
     * Prompts user to enter an email address for password recovery.
     *
     * @param click a click on the forgot password label
     */

    private void forgotPasswordAction(MouseEvent click) {
        grid.getChildren().removeAll(usernameField, passwordField, loginButton);
        grid.getChildren().addAll(emailField, sendPasswordButton);

        warningLabel.setText("");
        warningLabel.setOnMouseClicked(null);

        instructionLabel.setText("Enter your email address. If there is an\nassociated password, it will be sent to you.");
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
     * Attempts to send an confirmation email to the registrant.
     *
     * @param actionEvent A click on the sendUsername Button
     */

    private void sendUsername(ActionEvent actionEvent) {
        String email = emailField.getText();
        if(isInvalidEmailAddress(email)) {
            warningLabel.setText("Invalid email");
        }
        try {
            for(String user : prefs.childrenNames()) {
                if(prefs.node(user).get("email", "").equals(email)) {
                    try {
                        MimeMessage message = new MimeMessage(session);
                        message.setFrom(new InternetAddress(from));
                        message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                        message.setSubject("Username recovery");
                        message.setText("Your username for Anagrams is " + user);

                        ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
                        emailExecutor.execute(() -> {
                            try {
                                Transport.send(message);
                            } catch (MessagingException e) {
                                e.printStackTrace();
                            }
                        });
                        emailExecutor.shutdown();

                        grid.getChildren().removeAll(sendUsernameButton, emailField);
                        instructionLabel.setText("");
                        usernameField.setText("");
                        warningLabel.setText("");
                        passwordField.setText("");
                        grid.getChildren().addAll(usernameField, passwordField, submitButton);
                        return;
                    }
                    catch (MessagingException mex) {
                        mex.printStackTrace();
                    }
                }
            }
        }
        catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Attempts to send an message containing the registered user's password to the associated email.
     *
     * @param actionEvent a click on the sendPassword Button
     */

    private void sendPassword(ActionEvent actionEvent) {

        String email = emailField.getText();
        if(isInvalidEmailAddress(email)) {
            warningLabel.setText("Invalid email");
        }
        else {
            try {
                for (String user : prefs.childrenNames()) {
                    if (prefs.node(user).get("email", "").equals(email)) {
                        try {
                            MimeMessage message = new MimeMessage(session);
                            message.setFrom(new InternetAddress(from));
                            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                            message.setSubject("Password recovery");
                            message.setText("Your password for Anagrams is " + new String(Base64.getDecoder().decode((prefs.node(user).getByteArray("password", new byte[10])))));

                            ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
                            emailExecutor.execute(() -> {
                                try {
                                    Transport.send(message);
                                }
                                catch (MessagingException me) {
                                    me.printStackTrace();
                                }
                            });
                            emailExecutor.shutdown();

                            grid.getChildren().removeAll(sendPasswordButton, emailField);
                            instructionLabel.setText("");
                            warningLabel.setText("");
                            passwordField.setText("");
                            usernameField.setDisable(true);
                            grid.getChildren().addAll(usernameField);
                            grid.getChildren().addAll(passwordField);
                            grid.getChildren().addAll(submitButton);
                            return;
                        }
                        catch (MessagingException mex) {
                            mex.printStackTrace();
                        }

                    }
                }
            }
            catch (BackingStoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if the email address has between 1 and 21 characters and does not start with "Robot".
     * @return true if the username is invalid, false otherwise
     */

    private static boolean isInvalidUsername(String username) {
        return username.startsWith("Robot") || username.isBlank() || username.length() > 21;
    }

    /**
     * Checks if the contents of the emailField is a valid email address.
     * @return true if the email address is invalid, false otherwise
     */

    public static boolean isInvalidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return !m.matches();
    }

}