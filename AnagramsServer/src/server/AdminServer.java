package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Tools to interact with and get data from the program.
 * Can be used to send alert messages to all users.
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
                            System.out.println("Shutdown command received");
                            gameServer.shutdown();
                        }
                    }

                    case "games" -> out.println("There are currently " + gameServer.getGames().size() + " active games");
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
