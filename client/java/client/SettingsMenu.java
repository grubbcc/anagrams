package client;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.HashMap;

/**
 * A menu for choosing preferences and saving them for future use
 *
 */

class SettingsMenu extends PopWindow {

    private final AnagramsClient client;
    private final CheckBox soundChooser = new CheckBox("Play sounds");
    private final ChoiceBox<String> lexiconChooser = new ChoiceBox<>(FXCollections.observableArrayList(AnagramsClient.lexicons));
    private final EnumMap<AnagramsClient.Colors, String> newColors;
    private final HashMap<AnagramsClient.Colors, SettingsMenu.ColorChooser> colorChoosers = new HashMap<>();

    /**
     *
     */

    public SettingsMenu(AnagramsClient client) {
        super(client.stack);
        this.client = client;

        newColors = client.colors.clone();

        if(client.getWebAPI().isMobile()) {
            setScaleX(1.45); setScaleY(1.45);
        }

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(3));
        grid.setHgap(3);
        grid.setVgap(3);

        //labels
        Label lexiconLabel = new Label("Word list");
        lexiconLabel.setTooltip(new Tooltip("NWL20 = North American\nCSW19 = International"));

        //selectors
        lexiconChooser.getSelectionModel().select(client.prefs.get("lexicon", "CSW19"));
        lexiconChooser.pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), client.getWebAPI().isMobile());
        soundChooser.setSelected(client.prefs.getBoolean("play_sounds", true));
        soundChooser.setPadding(new Insets(0,9,0,7));

        //buttons
        Button OKButton = new Button("Okay");
        Button CancelButton = new Button("Cancel");
        Button ApplyButton = new Button("Apply");

        grid.add(lexiconLabel, 0, 0);
        grid.add(lexiconChooser, 1, 0);
        grid.add(soundChooser, 2, 0);
        grid.add(new ColorChooser(AnagramsClient.Colors.MAIN_SCREEN), 0, 1, 3, 1);
        grid.add(new ColorChooser(AnagramsClient.Colors.PLAYERS_LIST), 0, 2, 3, 1);
        grid.add(new ColorChooser(AnagramsClient.Colors.GAME_FOREGROUND), 0, 3, 3, 1);
        grid.add(new ColorChooser(AnagramsClient.Colors.GAME_BACKGROUND), 0, 4, 3, 1);
        grid.add(new ColorChooser(AnagramsClient.Colors.CHAT_AREA), 0, 5, 3, 1);
        grid.add(OKButton, 0, 6);
        grid.add(CancelButton, 1, 6);
        grid.add(ApplyButton, 2, 6);

        OKButton.setOnAction(e -> { applyChanges(); savePreferences(); hide(); });
        CancelButton.setOnAction(e -> {revertChanges(); hide();});
        ApplyButton.setOnAction(e -> applyChanges());
        closeButton.setOnAction(e -> {revertChanges(); hide();});

        setContents(grid);
        setMaxSize(340,320);
        setTitle("Settings");
        show(false);

        client.stage.requestFocus(); //Prevents IllegalStateException in touchscreen mode
        OKButton.requestFocus();
    }

    /**
     * Instructs client to apply any requested color changes.
     */

    private void applyChanges() {
        for(AnagramsClient.Colors color : newColors.keySet()) {
            client.colors.put(color, newColors.get(color));
        }
        client.setColors();
    }

    /**
     * Saves current color configuration to preferences file.
     */

    private void savePreferences() {
        client.prefs.put("lexicon", lexiconChooser.getSelectionModel().getSelectedItem() + "");
        client.prefs.putBoolean("play_sounds", soundChooser.isSelected());
        for(AnagramsClient.Colors color : client.colors.keySet()) {
            client.prefs.put(color.key, client.colors.get(color));
        }
    }

    /**
     * Restores colors to previous values.
     */

    private void revertChanges() {
        lexiconChooser.getSelectionModel().select(client.prefs.get("lexicon", "CSW19"));
        soundChooser.setSelected(client.prefs.getBoolean("play_sounds", true));
        for (AnagramsClient.Colors color : AnagramsClient.Colors.values()) {
            client.colors.put(color, client.prefs.get(color.key, color.defaultCode));
            colorChoosers.get(color).setColor(client.colors.get(color));
        }
        client.setColors();
    }

    /**
     *
     */

    class ColorChooser extends GridPane {

        AnagramsClient.Colors color;

        private int R;
        private int G;
        private int B;

        private final TextField textFieldR = new TextField();
        private final TextField textFieldG = new TextField();
        private final TextField textFieldB = new TextField();

        private final ColorComboBox comboBox = new ColorComboBox();
        String colorCode;

        /**
         *
         */

        ColorChooser(AnagramsClient.Colors color) {
            this.color = color;
            setColor(client.colors.get(color));
            colorChoosers.put(color, this);

            getColumnConstraints().add(new ColumnConstraints(130));
            getColumnConstraints().add(new ColumnConstraints(120));
            getColumnConstraints().add(new ColumnConstraints(USE_COMPUTED_SIZE));
            setHgap(4);

            setColor(colorCode);

            add(new Label(color.display), 0, 0);
            add(new ColorPane(), 1, 0);
            add(comboBox, 2, 0);
        }

        /**
         */

        public void setColor(String colorCode) {

            R = Integer.valueOf(colorCode.substring(1, 3), 16);
            G = Integer.valueOf(colorCode.substring(3, 5), 16);
            B = Integer.valueOf(colorCode.substring(5, 7), 16);

            textFieldR.setText(R + "");
            textFieldG.setText(G + "");
            textFieldB.setText(B + "");

            comboBox.getButtonCell().setBackground(new Background(new BackgroundFill(Color.web(colorCode), CornerRadii.EMPTY, Insets.EMPTY)));

            this.colorCode = colorCode;
            newColors.put(color, colorCode);
        }

        /**
         *
         */

        class ColorPane extends GridPane {

            /**
             */

            ColorPane() {
                setId("color-pane");
                add(textFieldR, 0, 0);
                add(textFieldG, 1, 0);
                add(textFieldB, 2, 0);
                add(new Label("R"), 0, 1);
                add(new Label("G"), 1, 1);
                add(new Label("B"), 2, 1);
                textFieldR.textProperty().addListener(textListener);
                textFieldG.textProperty().addListener(textListener);
                textFieldB.textProperty().addListener(textListener);
            }

            /**
             */

            ChangeListener<String> textListener = new ChangeListener<>() {

                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {

                    TextField textField = (TextField) ((StringProperty)observable).getBean();

                    if (!newValue.matches("\\d*")) {
                        textField.setText(oldValue);
                        return;
                    }
                    if(oldValue.equals("0"))
                        if(newValue.startsWith("0"))
                            textField.setText(newValue.substring(1));
                    if(newValue.isEmpty())
                        textField.setText("0");
                    else if (Integer.parseInt(newValue) > 255)
                        textField.setText("255");

                    if(textField.getText().isEmpty()) textField.setText("0");
                    if(textField.equals(textFieldR)) R = Integer.parseInt(textField.getText());
                    if(textField.equals(textFieldG)) G = Integer.parseInt(textField.getText());
                    if(textField.equals(textFieldB)) B = Integer.parseInt(textField.getText());

                    colorCode = String.format("#%02x%02x%02x", R, G, B);
                    setColor(colorCode);
                    newColors.put(color, colorCode);
                }
            };
        }

        /**
         *
         */

        public class ColorComboBox extends ComboBox<String> {

            String[] colorCodes = {"#cd5c5c", "#f08080", "#fa8072", "#e9967a", "#ffa07a", "#dc143c", "#ff0000", "#b22222", "#8b0000", "#ffc0cb", "#ffb6c1", "#ff69b4", "#ff1493", "#c71585", "#db7093", "#ffa07a", "#ff7f50", "#ff6347", "#ff4500", "#ff8c00", "#ffa500", "#ffd700", "#ffff00", "#ffffe0", "#fffacd", "#fafad2", "#ffefd5", "#ffe4b5", "#ffdab9", "#eee8aa", "#f0e68c", "#bdb76b", "#e6e6fa", "#d8bfd8", "#dda0dd", "#ee82ee", "#da70d6", "#ff00ff", "#ff00ff", "#ba55d3", "#9370db", "#663399", "#8a2be2", "#9400d3", "#9932cc", "#8b008b", "#800080", "#4b0082", "#6a5acd", "#483d8b", "#7b68ee", "#adff2f", "#7fff00", "#7cfc00", "#00ff00", "#32cd32", "#98fb98", "#90ee90", "#00fa9a", "#00ff7f", "#3cb371", "#2e8b57", "#228b22", "#008000", "#006400", "#9acd32", "#6b8e23", "#808000", "#556b2f", "#66cdaa", "#8fbc8b", "#20b2aa", "#008b8b", "#008080", "#00ffff", "#00ffff", "#e0ffff", "#afeeee", "#7fffd4", "#40e0d0", "#48d1cc", "#00ced1", "#5f9ea0", "#4682b4", "#b0c4de", "#b0e0e6", "#add8e6", "#87ceeb", "#87cefa", "#00bfff", "#1e90ff", "#6495ed", "#7b68ee", "#4169e1", "#0000ff", "#0000cd", "#00008b", "#000080", "#191970", "#fff8dc", "#ffebcd", "#ffe4c4", "#ffdead", "#f5deb3", "#deb887", "#d2b48c", "#bc8f8f", "#f4a460", "#daa520", "#b8860b", "#cd853f", "#d2691e", "#8b4513", "#a0522d", "#a52a2a", "#800000", "#ffffff", "#fffafa", "#f0fff0", "#f5fffa", "#f0ffff", "#f0f8ff", "#f8f8ff", "#f5f5f5", "#fff5ee", "#f5f5dc", "#fdf5e6", "#fffaf0", "#fffff0", "#faebd7", "#faf0e6", "#fff0f5", "#ffe4e1", "#dcdcdc", "#d3d3d3", "#c0c0c0", "#a9a9a9", "#808080", "#696969", "#778899", "#708090", "#2f4f4f", "#000000"};

            /**
             */

            ColorComboBox() {
                setCellFactory(lv -> new ColorCell());
                setId("color-combo-box");
                setVisibleRowCount(30);
                getItems().addAll(colorCodes);
                setButtonCell(new ColorCell());
                pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), client.getWebAPI().isMobile());
                setOnAction(e -> setColor(getSelectionModel().getSelectedItem()));
            }

            /**
             *
             */

            public class ColorCell extends ListCell<String> {

                String colorCode;

                public ColorCell() {
                    if(client.getWebAPI().isMobile())
                        addEventFilter(TouchEvent.TOUCH_RELEASED, event -> setColor(colorCode));
                }
                /**
                 */

                @Override
                public void updateItem(String color, boolean empty) {

                    super.updateItem(color, empty);

                    if(!empty) {
                        this.colorCode = color;
                        setBackground(new Background(new BackgroundFill(Color.web(color), CornerRadii.EMPTY, Insets.EMPTY)));
                    }
                }
            }
        }
    }
}