package client;

import java.lang.Integer;
import java.util.EnumMap;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * A menu for choosing preferences and saving them for future use
 *
 */

class SettingsMenu extends PopWindow {

    private final AnagramsClient client;
    private final CheckBox soundChooser = new CheckBox("Play sounds");
    private final ComboBox<String> lexiconChooser = new ComboBox<>(FXCollections.observableArrayList(AnagramsClient.lexicons));
    private final EnumMap<AnagramsClient.Colors, String> newColors;

    /**
     *
     */

    public SettingsMenu(AnagramsClient client) {
        super(client.stack);
        this.client = client;

        newColors = client.colors.clone();


        GridPane grid = new GridPane();
        grid.setPadding(new Insets(3));
        grid.setHgap(3);
        grid.setVgap(3);

        //labels
        Label lexiconLabel = new Label("Word list");
        lexiconLabel.setTooltip(new Tooltip("NWL20 = North American\nCSW19 = International"));

        //selectors
        lexiconChooser.getSelectionModel().select(client.prefs.get(client.username + "/LEXICON", "CSW19"));
        soundChooser.setSelected(client.prefs.getBoolean(client.username + "/PLAY_SOUNDS", true));
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

        OKButton.setOnAction(e -> {applyChanges(); savePreferences(); hide(); });
        CancelButton.setOnAction(e -> hide());
        ApplyButton.setOnAction(e -> {applyChanges(); savePreferences();});

        setContents(grid);
        setMaxSize(310,320);
        setTitle("Settings");

        OKButton.requestFocus();
    }

    /**
     *
     */

    private void applyChanges() {
        client.colors = newColors;
        client.setColors();
    }

    /**
     *
     */

    private void savePreferences() {
        client.prefs.put(client.username + "/LEXICON", lexiconChooser.getSelectionModel().getSelectedItem() + "");
        client.prefs.putBoolean(client.username + "PLAY_SOUNDS", soundChooser.isSelected());
        for(AnagramsClient.Colors color : client.colors.keySet()) {
            client.prefs.put(client.username + "/" + color.toString(), client.colors.get(color));
        }
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
            setColors(client.colors.get(color));

            getColumnConstraints().add(new ColumnConstraints(110));
            setHgap(4);

            comboBox.setColor(colorCode);
            comboBox.setOnAction(e -> setColors(comboBox.getSelectionModel().getSelectedItem()));

            add(new Label(color.display), 0, 0);
            add(new ColorPane(), 1, 0);
            add(comboBox, 2, 0);
        }

        /**
         */

        public void setColors(String colorCode) {

            R = Integer.valueOf(colorCode.substring(1, 3), 16);
            G = Integer.valueOf(colorCode.substring(3, 5), 16);
            B = Integer.valueOf(colorCode.substring(5, 7), 16);

            textFieldR.setText(R + "");
            textFieldG.setText(G + "");
            textFieldB.setText(B + "");

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

                    comboBox.setColor(String.format("#%02X%02X%02X", R, G, B));
                    newColors.put(color, colorCode);
                }
            };
        }

        /**
         *
         */

        public class ColorComboBox extends ComboBox<String> {

            String[] colorCodes = {"#CD5C5C", "#F08080", "#FA8072", "#E9967A", "#FFA07A", "#DC143C", "#FF0000", "#B22222", "#8B0000", "#FFC0CB", "#FFB6C1", "#FF69B4", "#FF1493", "#C71585", "#DB7093", "#FFA07A", "#FF7F50", "#FF6347", "#FF4500", "#FF8C00", "#FFA500", "#FFD700", "#FFFF00", "#FFFFE0", "#FFFACD", "#FAFAD2", "#FFEFD5", "#FFE4B5", "#FFDAB9", "#EEE8AA", "#F0E68C", "#BDB76B", "#E6E6FA", "#D8BFD8", "#DDA0DD", "#EE82EE", "#DA70D6", "#FF00FF", "#FF00FF", "#BA55D3", "#9370DB", "#663399", "#8A2BE2", "#9400D3", "#9932CC", "#8B008B", "#800080", "#4B0082", "#6A5ACD", "#483D8B", "#7B68EE", "#ADFF2F", "#7FFF00", "#7CFC00", "#00FF00", "#32CD32", "#98FB98", "#90EE90", "#00FA9A", "#00FF7F", "#3CB371", "#2E8B57", "#228B22", "#008000", "#006400", "#9ACD32", "#6B8E23", "#808000", "#556B2F", "#66CDAA", "#8FBC8B", "#20B2AA", "#008B8B", "#008080", "#00FFFF", "#00FFFF", "#E0FFFF", "#AFEEEE", "#7FFFD4", "#40E0D0", "#48D1CC", "#00CED1", "#5F9EA0", "#4682B4", "#B0C4DE", "#B0E0E6", "#ADD8E6", "#87CEEB", "#87CEFA", "#00BFFF", "#1E90FF", "#6495ED", "#7B68EE", "#4169E1", "#0000FF", "#0000CD", "#00008B", "#000080", "#191970", "#FFF8DC", "#FFEBCD", "#FFE4C4", "#FFDEAD", "#F5DEB3", "#DEB887", "#D2B48C", "#BC8F8F", "#F4A460", "#DAA520", "#B8860B", "#CD853F", "#D2691E", "#8B4513", "#A0522D", "#A52A2A", "#800000", "#FFFFFF", "#FFFAFA", "#F0FFF0", "#F5FFFA", "#F0FFFF", "#F0F8FF", "#F8F8FF", "#F5F5F5", "#FFF5EE", "#F5F5DC", "#FDF5E6", "#FFFAF0", "#FFFFF0", "#FAEBD7", "#FAF0E6", "#FFF0F5", "#FFE4E1", "#DCDCDC", "#D3D3D3", "#C0C0C0", "#A9A9A9", "#808080", "#696969", "#778899", "#708090", "#2F4F4F", "#000000"};

            /**
             */

            ColorComboBox() {

                setCellFactory(lv -> new ColorCell());
                setId("color-combo-box");
                setVisibleRowCount(30);
                getItems().addAll(colorCodes);
                setButtonCell(new ColorCell());
            }

            /**
             */

            public void setColor(String colorCode) {

                getButtonCell().setBackground(new Background(new BackgroundFill(Color.web(colorCode), CornerRadii.EMPTY, Insets.EMPTY)));
            }

            /**
             *
             */

            public class ColorCell extends ListCell<String> {

                /**
                 */

                @Override
                public void updateItem(String color, boolean empty) {

                    super.updateItem(color, empty);

                    if(!empty) {
                        setBackground(new Background(new BackgroundFill(Color.web(color), CornerRadii.EMPTY, Insets.EMPTY)));
                    }
                }
            }
        }
    }
}