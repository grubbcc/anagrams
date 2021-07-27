package client;

import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.transform.Scale;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * A menu for choosing game options and saving them for future use
 *
 */

class GameMenu extends PopWindow {

    private final AnagramsClient client;

    private final String[] numPlayersChoices = {"1", "2", "3", "4", "5", "6"};
    private final String[] minLengthChoices = {"4", "5", "6", "7", "8", "9", "10"};
    private final String[] numSetsChoices = {"1", "2", "3"};
    private final String[] blankPenaltyChoices = {"1", "2"};
    private final String[] speedChoices = {"slow", "medium", "fast"};
    private final String[] skillLevelChoices = {"novice", "standard", "expert", "genius"};

    private final ChoiceBox<String> playersSelector = new ChoiceBox<>(FXCollections.observableArrayList(numPlayersChoices));
    private final ChoiceBox<String> lengthsSelector = new ChoiceBox<>(FXCollections.observableArrayList(minLengthChoices));
    private final ChoiceBox<String> setsSelector = new ChoiceBox<>(FXCollections.observableArrayList(numSetsChoices));
    private final ChoiceBox<String> blanksSelector = new ChoiceBox<>(FXCollections.observableArrayList(blankPenaltyChoices));
    private final ChoiceBox<String> lexiconSelector = new ChoiceBox<>(FXCollections.observableArrayList(AnagramsClient.lexicons));
    private final ChoiceBox<String> speedSelector = new ChoiceBox<>(FXCollections.observableArrayList(speedChoices));
    private final ChoiceBox<String> skillLevelSelector = new ChoiceBox<>(FXCollections.observableArrayList(skillLevelChoices));

    private final CheckBox chatChooser = new CheckBox("Allow chatting");
    private final CheckBox watchersChooser = new CheckBox("Allow watchers");
    private final CheckBox robotChooser = new CheckBox("Add robot player");
    private final CheckBox defaultChooser = new CheckBox("Save as default");

    private final Button startButton = new Button("Start");

    /**
     *
     */

    public GameMenu(AnagramsClient client) {
        super(client.stack);
        this.client = client;

        if(client.getWebAPI().isMobile()) {
            setScaleX(1.35); setScaleY(1.35);
        }
        GridPane grid = new GridPane();
        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        cc.setFillWidth(true);
        grid.getColumnConstraints().add(cc);
        grid.setPadding(new Insets(3));
        grid.setHgap(3);
        grid.setVgap(6);

        //labels
        Label tileSetsLabel = new Label("Number of tile sets");
        tileSetsLabel.setTooltip(new Tooltip("100 tiles per set"));
        Label blankPenaltyLabel = new Label("Blank penalty");
        blankPenaltyLabel.setTooltip(new Tooltip("To use a blank, you must take\n this many additional tiles"));
        Label wordListLabel = new Label("Word list");
        wordListLabel.setTooltip(new Tooltip("NWL20 = North American\nCSW19 = International"));
        Label speedLabel = new Label("Speed");
        speedLabel.setTooltip(new Tooltip("Slow: 9 seconds per tile\nMedium: 6 seconds per tile\nFast: 3 seconds per tile"));

        //selectors
        playersSelector.getSelectionModel().select(client.prefs.get("max_players", "6"));
        lengthsSelector.getSelectionModel().select(client.prefs.get("min_length", "7"));
        setsSelector.getSelectionModel().select(client.prefs.get("num_sets", "1"));
        blanksSelector.getSelectionModel().select(client.prefs.get("blank_penalty", "2"));
        lexiconSelector.getSelectionModel().select(client.prefs.get("lexicon", "CSW19"));
        speedSelector.getSelectionModel().select(client.prefs.get("speed", "medium"));
        skillLevelSelector.getSelectionModel().select(client.prefs.get("robot_skill", "standard"));
        skillLevelSelector.disableProperty().bind(robotChooser.selectedProperty().not());

        //choosers
        chatChooser.setSelected(client.prefs.getBoolean("allow_chat",true));
        watchersChooser.setSelected(client.prefs.getBoolean("allow_watchers", true));
        robotChooser.setSelected(client.prefs.getBoolean("add_robot", false));

        grid.add(new Label("Maximum number of players"), 0, 0);
        grid.add(playersSelector, 1, 0);
        grid.add(new Label("Minimum word length"), 0, 1);
        grid.add(lengthsSelector, 1, 1);
        grid.add(tileSetsLabel, 0, 2);
        grid.add(setsSelector, 1, 2);
        grid.add(blankPenaltyLabel, 0, 3);
        grid.add(blanksSelector, 1, 3);
        grid.add(wordListLabel, 0, 4);
        grid.add(lexiconSelector, 1, 4);
        grid.add(speedLabel, 0, 5);
        grid.add(speedSelector, 1, 5);
        grid.add(chatChooser, 0, 6);
        grid.add(watchersChooser, 1, 6);
        grid.add(robotChooser, 0, 7);
        grid.add(skillLevelSelector, 1, 7);
        grid.add(startButton, 0, 8);
        grid.add(defaultChooser, 1, 8);

        setTitle("Game Options");
        setContents(grid);
        setMaxSize(320, 350);

        startButton.setOnAction(e -> {
            if(defaultChooser.isSelected())
                savePreferences();
            createGame();
            hide();
        });

        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER)
                startButton.fire();
        });

        show(true);
        startButton.requestFocus();
    }

    /**
     *
     */

    private void savePreferences() {
        client.prefs.put("max_players", playersSelector.getSelectionModel().getSelectedItem() + "");
        client.prefs.put("min_length", lengthsSelector.getSelectionModel().getSelectedItem() + "");
        client.prefs.put("num_sets", setsSelector.getSelectionModel().getSelectedItem() + "");
        client.prefs.put("blank_penalty", blanksSelector.getSelectionModel().getSelectedItem() + "");
        client.prefs.put("lexicon", lexiconSelector.getSelectionModel().getSelectedItem() + "");
        client.prefs.put("speed", speedSelector.getSelectionModel().getSelectedItem() + "");
        client.prefs.put("robot_skill", skillLevelSelector.getSelectionModel().getSelectedItem() + "");
        client.prefs.putBoolean("allow_chat", chatChooser.isSelected());
        client.prefs.putBoolean("allow_watchers", watchersChooser.isSelected());
        client.prefs.putBoolean("add_robot", robotChooser.isSelected());
    }

    /**
     *
     */

    public void createGame() {

        Date now = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("hhmmssMs");
        String gameID = ft.format(now);	//generates a unique gameID based on the current time

        String maxPlayers = playersSelector.getSelectionModel().getSelectedItem() + "";
        String minLength = lengthsSelector.getSelectionModel().getSelectedItem() + "";
        String numSets = setsSelector.getSelectionModel().getSelectedItem() + "";
        String blankPenalty = blanksSelector.getSelectionModel().getSelectedItem() + "";
        String lexicon = lexiconSelector.getSelectionModel().getSelectedItem() + "";
        String speed = speedSelector.getSelectionModel().getSelectedItem() + "";
        String skillLevel = (skillLevelSelector.getSelectionModel().getSelectedIndex() + 1) + "";
        String allowChat = chatChooser.isSelected() + "";
        String allowWatchers = watchersChooser.isSelected() + "";
        String addRobot = robotChooser.isSelected() + "";


        client.gameWindows.put(gameID, new GameWindow(client, gameID, client.username, minLength, blankPenalty, chatChooser.isSelected(), lexicon, new ArrayList<>(), false));


        String cmd = "newgame " + gameID + " " + maxPlayers + " " + minLength + " " + numSets + " " + blankPenalty + " " + lexicon + " " + speed + " " + allowChat + " " + allowWatchers + " " + addRobot + " " + skillLevel;
        client.send(cmd);
    }
}