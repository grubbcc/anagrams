package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

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
        try {
            ServerSocket serverSocket = new ServerSocket(ADMIN_PORT);

            Socket clientSocket = serverSocket.accept();

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String cmd;
            while ((cmd = in.readLine()) != null) {
                switch (cmd) {
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
                        out.println("There are currently " + gameServer.getGames().size() + " active games");
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
