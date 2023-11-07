package server;

import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Tools to interact with and get data from the program.
 * Can be used (once implemented) to send alert messages to all users.
 */
class AdminServer extends Thread {

    private static final int ADMIN_PORT = 8117;
    private final Server gameServer;

    /**
     *
     */
    AdminServer(Server gameServer) {
        this.gameServer = gameServer;
    }

    /**
     *
     */
    @Override
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(ADMIN_PORT)) {

            Socket clientSocket = serverSocket.accept();

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            out.println("Connected to admin server");
            String cmd;
            while ((cmd = in.readLine()) != null) {
                switch (cmd) {
                    case "help" -> out.println("Commands: game, top, users, shutdown");

                    case "shutdown" -> {
                        if (clientSocket.getInetAddress().isLoopbackAddress()) {
                            out.println("Shutdown command received");
                            gameServer.shutdown();
                        }
                    }

                    case "games" -> {
                        List<Game> games = gameServer.getGames().stream().toList();
                        for(int g = 0; g < games.size(); g++) {
                            out.println("Game " + (g + 1) + ": ");
                            ConcurrentHashMap<String, Player> players = games.get(g).players;
                            StringJoiner joiner = new StringJoiner(", ", "[", "]");
                            for(Map.Entry<String, Player> player : players.entrySet()) {
                                joiner.add(player.getKey() + (player.getValue().abandoned ? " (abandoned)" : ""));
                            }
                            out.println("\tPlayers: " + joiner);
                            out.println("\tWatchers: " + games.get(g).watchers.keySet());
                        }
//                        out.println("There " + (numGames == 1 ? "is" : "are") + " currently " + numGames + " active game" + (numGames == 1 ? "." : "s.");
                        out.println("There %s currently %d active games %s."
                                .formatted(games.size() == 1 ? "is" : "are", games.size(), games.size() == 1 ? "" : "s"));
                    }

                    case "top" -> {
                        Preferences prefs = Preferences.userNodeForPackage(Server.class);
                        try {
                            HashMap<String, Integer> ratings = new HashMap<>();
                            for (String user : prefs.childrenNames()) {
                                ratings.put(user, prefs.node(user).getInt("rating", 0));
                            }
                            ratings.entrySet()
                                    .stream()
                                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                    .forEach(entry -> {
                                        if(entry.getValue() > 0)
                                            out.println(entry.getKey() + ": " + entry.getValue());
                                    });

                        }
                        catch(BackingStoreException bse) {
                            bse.printStackTrace();
                        }

                    }

                    case "users" -> out.println("Users: " + gameServer.getUsernames());

                    default -> out.println("Command not recognized");
                }
            }
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
