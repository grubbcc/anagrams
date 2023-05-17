package server;

import org.json.JSONObject;
import java.util.prefs.Preferences;

/**
 *
 */
public class UserData {

    private JSONObject prefs;
    private Preferences backingStore;
    private final boolean guest;

    private final String username;
    private String profile;
    private int rating;

    private static final JSONObject defaults = new JSONObject()
        .put("main_screen", "#282828")
        .put("players_list", "#3a3a3a")
        .put("game_foreground", "#193a57")
        .put("game_background", "#2d6893")
        .put("chat_area", "#a4dffc")
        .put("game_chat", "#002868")
        .put("lexicon", "CSW21")
        .put("speed", "medium")
        .put("robot_skill", "standard")

        .put("max_players", 6)
        .put("min_length", 7)
        .put("num_sets", 1)
        .put("blank_penalty", 2)

        .put("allow_chat", true)
        .put("allow_watchers", false)
        .put("add_robot", false)
        .put("rated", false)
        .put("show_guide", true)
        .put("highlight_words", false)
        .put("play_sounds", true);

    /**
     *
     */
    private static JSONObject getDefaults() {
        return defaults;
    }


    /**
     *
     */
    UserData(String username, boolean guest) {
        this.username = username;
        this.guest = guest;

        if(guest) {
            prefs = defaults;
            rating = 1500;
            profile = username;
            return;
        }

        //public userdata
        backingStore = Preferences.userNodeForPackage(getClass()).node(username);
        rating = backingStore.getInt("rating", 1500);
        profile = backingStore.get("profile", username);

        prefs = new JSONObject()
        //colors
            .put("main_screen", backingStore.get("main_screen", defaults.getString("main_screen")))
            .put("players_list", backingStore.get("players_list", defaults.getString("players_list")))
            .put("game_foreground", backingStore.get("game_foreground", defaults.getString("game_foreground")))
            .put("game_background", backingStore.get("game_background", defaults.getString("game_background")))
            .put("chat_area", backingStore.get("chat_area", defaults.getString("chat_area")))
            .put("game_chat", backingStore.get("game_chat", defaults.getString("game_chat")))
        //game defaults
            .put("max_players", backingStore.getInt("max_players", defaults.getInt("max_players")))
            .put("min_length", backingStore.getInt("min_length", defaults.getInt("min_length")))
            .put("num_sets", backingStore.getInt("num_sets", defaults.getInt("num_sets")))
            .put("blank_penalty", backingStore.getInt("blank_penalty", defaults.getInt("blank_penalty")))
            .put("lexicon", backingStore.get("lexicon", defaults.getString("lexicon")))
            .put("speed", backingStore.get("speed", defaults.getString("speed")))
            .put("allow_chat", backingStore.getBoolean("allow_chat", defaults.getBoolean("allow_chat")))
            .put("allow_watchers", backingStore.getBoolean("allow_watchers", defaults.getBoolean("allow_watchers")))
            .put("add_robot", backingStore.getBoolean("add_robot", defaults.getBoolean("add_robot")))
            .put("robot_skill", backingStore.get("robot_skill", defaults.getString("robot_skill")))
            .put("rated", backingStore.getBoolean("rated", defaults.getBoolean("rated")))
        //other
            .put("show_guide", backingStore.getBoolean("show_guide", defaults.getBoolean("show_guide")))
            .put("highlight_words", backingStore.getBoolean("highlight_words", defaults.getBoolean("highlight_words")))
            .put("play_sounds", backingStore.getBoolean("play_sounds", defaults.getBoolean("play_sounds")));

    }

    /**
     *
     */
    void update(JSONObject newPrefs) {
        switch(newPrefs.getString("type")) {
            case "settings" -> updateSettings(newPrefs.getJSONObject("prefs"));
            case "game" -> updateGamePrefs(newPrefs.getJSONObject("prefs"));
            case "guide" -> backingStore.putBoolean("show_guide", false);
        }
    }

    /**
     * Save the settings from the SettingsMenu the preferences file
     */
    void updateSettings(JSONObject newPrefs) {
        backingStore.put("main_screen", newPrefs.getString("main_screen"));
        backingStore.put("players_list", newPrefs.getString("players_list"));
        backingStore.put("game_foreground", newPrefs.getString("game_foreground"));
        backingStore.put("game_background", newPrefs.getString("game_background"));
        backingStore.put("chat_area", newPrefs.getString("chat_area"));
        backingStore.put("game_chat", newPrefs.getString("game_chat"));
        backingStore.putBoolean("highlight_words", newPrefs.getBoolean("highlight_words"));
        backingStore.putBoolean("play_sounds", newPrefs.getBoolean("play_sounds"));
    }

    /**
     * Save the settings from the GameMenu to the preferences file
     */
    void updateGamePrefs(JSONObject newPrefs) {
        backingStore.putInt("max_players", newPrefs.getInt("max_players"));
        backingStore.putInt("min_length", newPrefs.getInt("min_length"));
        backingStore.putInt("num_sets", newPrefs.getInt("num_sets"));
        backingStore.putInt("blank_penalty", newPrefs.getInt("blank_penalty"));
        backingStore.put("lexicon", newPrefs.getString("lexicon"));
        backingStore.put("speed", newPrefs.getString("speed"));
        backingStore.putBoolean("allow_chat", newPrefs.getBoolean("allow_chat"));
        backingStore.putBoolean("allow_watchers", newPrefs.getBoolean("allow_watchers"));
        backingStore.putBoolean("add_robot", newPrefs.getBoolean("add_robot"));
        backingStore.put("robot_skill", newPrefs.getString("robot_skill"));
        backingStore.putBoolean("rated", newPrefs.getBoolean("rated"));
    }

    /**
     *
     */
    JSONObject get() {
        return new JSONObject().put("prefs", prefs);
    }

    /**
     * Data about this user ready to be send to clients
     */
    JSONObject getPublicData() {
        return new JSONObject()
            .put("name", username)
            .put("rating", rating + "")
            .put("profile", profile);
    }

    /**
     *
     */
    int getRating() {
        return rating;
    }

    /**
     *
     */
    void setRating(int newRating) {
        rating = newRating;
        if(!guest) {
            backingStore.putInt("rating", newRating);
        }
    }

    /**
     *
     */
    void setProfile(String profile) {
        this.profile = profile;
        if(!guest) {
            backingStore.put("profile", profile);
        }
    }


}
