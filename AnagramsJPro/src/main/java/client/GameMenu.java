package client;

import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A menu for choosing game options and saving them for future use
 *
 */
class GameMenu extends PopWindow {

    private final AnagramsClient client;

    private final static Integer[] numPlayersChoices = {1, 2, 3, 4, 5, 6};
    private final static Integer[] minLengthChoices = {4, 5, 6, 7, 8, 9, 10};
    private final static Integer[] numSetsChoices = {1, 2, 3};
    private final static Integer[] blankPenaltyChoices = {1, 2};
    private final static String[] speedChoices = {"slow", "medium", "fast"};
    private final static String[] skillLevelChoices = {"novice", "standard", "expert", "genius"};

    private final ChoiceBox<Integer> playersChooser = new ChoiceBox<>(FXCollections.observableArrayList(numPlayersChoices));
    private final ChoiceBox<Integer> lengthChooser = new ChoiceBox<>(FXCollections.observableArrayList(minLengthChoices));
    private final ChoiceBox<Integer> setsChooser = new ChoiceBox<>(FXCollections.observableArrayList(numSetsChoices));
    private final ChoiceBox<Integer> blankChooser = new ChoiceBox<>(FXCollections.observableArrayList(blankPenaltyChoices));
    private final ChoiceBox<String> lexiconChooser = new ChoiceBox<>(FXCollections.observableArrayList(AnagramsClient.lexicons));
    private final ChoiceBox<String> speedChooser = new ChoiceBox<>(FXCollections.observableArrayList(speedChoices));
    private final ChoiceBox<String> skillChooser = new ChoiceBox<>(FXCollections.observableArrayList(skillLevelChoices));

    private final CheckBox chatBox = new CheckBox("Allow chatting");
    private final CheckBox watchersBox = new CheckBox("Allow watchers");
    private final CheckBox robotBox = new CheckBox("Add robot player");
    private final CheckBox ratedBox = new CheckBox("Rated");
    private final CheckBox defaultBox = new CheckBox("Save as default");

    private final TextField nameField = new TextField();
    private final Button startButton = new Button("Start");
    private final String gameID;

    /**
     *
     */
    GameMenu(AnagramsClient client) {
        super(client.stack);
        this.client = client;

        getStylesheets().add("css/game-menu.css");

        final Date now = new Date();
        final SimpleDateFormat ft = new SimpleDateFormat("hhmmss");
        gameID = ft.format(now);	//generates a unique gameID based on the current time
        nameField.setPromptText("Game " + gameID);
        nameField.setStyle("-fx-font-weight: bold;");

        nameField.setPrefWidth(112);
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            nameField.setText(newValue.replace("%", ""));
            if(newValue.length() > 50)
                nameField.setText(newValue.substring(0, 50));
        });

        if(client.getWebAPI().isMobile()) {
            pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
            playersChooser.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
            lengthChooser.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
            setsChooser.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
            blankChooser.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
            lexiconChooser.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
            speedChooser.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
            skillChooser.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), true);
        }

        final GridPane grid = new GridPane();
        final ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        cc.setFillWidth(true);
        grid.getColumnConstraints().add(cc);
        grid.setPadding(new Insets(3));
        grid.setHgap(3);
        grid.setVgap(6);

        //labels
        final Label nameLabel = new Label("Name");
        nameLabel.setTooltip(new Tooltip("e.g. \"Beginners' table\",\n\"Mary's club\", etc"));
        final Label tileSetsLabel = new Label("Number of tile sets");
        tileSetsLabel.setTooltip(new Tooltip("100 tiles per set"));
        final Label blankPenaltyLabel = new Label("Blank penalty");
        blankPenaltyLabel.setTooltip(new Tooltip("To use a blank, you must take\n this many additional tiles"));
        final Label wordListLabel = new Label("Word list");
        wordListLabel.setTooltip(new Tooltip("NWL20 = North American\nCSW21 = International"));
        final Label speedLabel = new Label("Speed");
        speedLabel.setTooltip(new Tooltip("Slow: 9 seconds per tile\nMedium: 6 seconds per tile\nFast: 3 seconds per tile"));

        //choosers
        playersChooser.getSelectionModel().select(Integer.valueOf(client.prefs.getInt("max_players")));
        lengthChooser.getSelectionModel().select(Integer.valueOf(client.prefs.getInt("min_length")));
        setsChooser.getSelectionModel().select(Integer.valueOf(client.prefs.getInt("num_sets")));
        blankChooser.getSelectionModel().select(Integer.valueOf(client.prefs.getInt("blank_penalty")));
        lexiconChooser.getSelectionModel().select(client.prefs.getString("lexicon"));
        speedChooser.getSelectionModel().select(client.prefs.getString("speed"));
        skillChooser.getSelectionModel().select(client.prefs.getString("robot_skill"));
        skillChooser.disableProperty().bind(robotBox.selectedProperty().not());

        //checkboxes
        chatBox.setSelected(client.prefs.getBoolean("allow_chat"));
        watchersBox.setSelected(client.prefs.getBoolean("allow_watchers"));
        robotBox.setSelected(client.prefs.getBoolean("add_robot"));
        ratedBox.setSelected(client.prefs.getBoolean("rated"));

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Maximum number of players"), 0, 1);
        grid.add(playersChooser, 1, 1);
        grid.add(new Label("Minimum word length"), 0, 2);
        grid.add(lengthChooser, 1, 2);
        grid.add(tileSetsLabel, 0, 3);
        grid.add(setsChooser, 1, 3);
        grid.add(blankPenaltyLabel, 0, 4);
        grid.add(blankChooser, 1, 4);
        grid.add(wordListLabel, 0, 5);
        grid.add(lexiconChooser, 1, 5);
        grid.add(speedLabel, 0, 6);
        grid.add(speedChooser, 1, 6);
        grid.add(chatBox, 0, 7);
        grid.add(watchersBox, 1, 7);
        grid.add(robotBox, 0, 8);
        grid.add(skillChooser, 1, 8);
        grid.add(ratedBox,0, 9);
        grid.add(defaultBox, 1,9);
        grid.add(startButton, 0, 10);

        setTitle("Game Options");
        setContents(grid);
        setMaxSize(335, 387);

        startButton.setPrefWidth(75.0);
        startButton.setPrefHeight(25.0);
        startButton.setOnAction(e -> {
            if(defaultBox.isSelected())
                savePreferences();
            createGame();
            hide();
        });

        startButton.setDefaultButton(true);
        setAsDragZone(grid);
        show(true);

        client.stage.requestFocus(); //Prevents IllegalStateException in touchscreen mode
        startButton.requestFocus();

    }

    /**
     *
     */
    private void savePreferences() {
        client.prefs.put("max_players", playersChooser.getSelectionModel().getSelectedItem())
            .put("min_length", lengthChooser.getSelectionModel().getSelectedItem())
            .put("num_sets", setsChooser.getSelectionModel().getSelectedItem())
            .put("blank_penalty", blankChooser.getSelectionModel().getSelectedItem())
            .put("lexicon", lexiconChooser.getSelectionModel().getSelectedItem())
            .put("speed", speedChooser.getSelectionModel().getSelectedItem())
            .put("robot_skill", skillChooser.getSelectionModel().getSelectedItem())
            .put("allow_chat", chatBox.isSelected())
            .put("allow_watchers", watchersBox.isSelected())
            .put("add_robot", robotBox.isSelected())
            .put("rated", ratedBox.isSelected());
        if(!client.guest)
            client.send("updateprefs", new JSONObject().put("type", "game").put("prefs", client.prefs));
    }

    /**
     *
     */
    void createGame() {

        int maxPlayers = playersChooser.getSelectionModel().getSelectedItem();
        if(robotBox.isSelected())
            maxPlayers = Math.min(6, maxPlayers + 1);

        JSONObject newGameParams = new JSONObject()
            .put("max_players", maxPlayers)
            .put("gameID", gameID)
            .put("game_name", nameField.getText().isBlank() ? nameField.getPromptText() : nameField.getText())
            .put("min_length", lengthChooser.getSelectionModel().getSelectedItem())
            .put("num_sets", setsChooser.getSelectionModel().getSelectedItem())
            .put("blank_penalty", blankChooser.getSelectionModel().getSelectedItem())
            .put("lexicon", lexiconChooser.getSelectionModel().getSelectedItem())
            .put("speed", speedChooser.getSelectionModel().getSelectedItem())
            .put("skill_level", skillChooser.getSelectionModel().getSelectedIndex())
            .put("allow_chat", chatBox.isSelected())
            .put("allow_watchers", watchersBox.isSelected())
            .put("add_robot", robotBox.isSelected())
            .put("rated", ratedBox.isSelected());

        new GameWindow(client, newGameParams, client.username, false, null);

        client.send(new JSONObject().put("cmd", "newgame").put("params", newGameParams));
    }
}