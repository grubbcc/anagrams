package server;

import java.net.Socket;
import java.io.*;

/**
* Handles tasks for a client on the server side.
*/
class ServerWorker extends Thread {

	private final Socket clientSocket;
	private final Server server;
	private String username = null;
	private OutputStream outputStream;
	private InputStream inputStream;
	private BufferedReader reader;


	/**
	*
	*/
	ServerWorker(Server server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
	}
	
	/**
	*
	*/
	@Override
	public void run() {

		while(!interrupted()) {
			try {
				handleClientSocket();
			}
			catch (IOException e) {
				interrupt();
				System.out.println(username + " has disconnected.");
			}
		}
	}


	/**
	 * Informs new user of other players and games in progress. Informs other players of new user.
	 * Prevents duplicate usernames.
	 *
	 * @param username The new user's name
	*/
	private void handleLogin(String username) {

		//Prevents duplicate usernames
		ServerWorker duplicate = server.getWorker(username);
		if(duplicate != null) {
			duplicate.send("logoffplayer " + username);
			server.removeWorker(username);
			System.out.println("Removing duplicate user: " + username);
		}

		send("ok login");
		this.username = username;
		System.out.println("User logged in successfully: " + username);

		for (String s : server.announcements) {
			send("chat " + s);
		}

		for (String s : server.chatLog) {
			send("chat " + s);
		}

		//notify new player of other players
		synchronized (server.getUsernames()) {
			for (String playerName : server.getUsernames()) {
				send("loginplayer " + playerName);
			}
		}

		//notify new player of games
		synchronized (server.getGames()) {
			for(Game game : server.getGames()) {
				send("addgame " + game.getGameParams());

				if(game.gameOver) {
					for(String gameState : game.gameLog) {
						send("gamelog " + game.gameID + " " + gameState);
					}
					send("note " + game.gameID + " @" + "Game over");
					send("endgame " + game.gameID);
				}
				else {
					for(String playerName : game.getPlayerList()) {
						send("takeseat " + game.gameID + " " + playerName);
					}
					for(String inactivePlayer : game.getInactivePlayers()) {
						send("abandonseat " + game.gameID + " " + inactivePlayer);
					}
					if(game.paused) {
						send("note " + game.gameID + " @" + "Game paused");
					}
					else if(game.timeRemaining > 0) {
						String message = "Time remaining: " + game.timeRemaining;
						send("note " + game.gameID + " @" + message);
					}
				}

			}
		}

		//notify other players of the new player
		server.addWorker(username, this);
		server.broadcast("loginplayer " + username);
	}

	/**
	*
	*/
	private void handleLogoff() throws IOException {
		disconnect();
		server.logoffPlayer(username);
	}

	/**
	 *
	 */
	private void disconnect() throws IOException {
		outputStream.flush();
		reader.close();

		inputStream.close();
		outputStream.close();
		clientSocket.close();

		System.out.println(username + " has disconnected");
		interrupt();
	}
	
	/**
	* Creates the game and informs all players
	*/
	private void handleCreateGame(String[] params) {
	
		String gameID = params[1];
		String gameName = params[2];
		int maxPlayers = Integer.parseInt(params[3]);
		int minLength = Integer.parseInt(params[4]);
		int numSets = Integer.parseInt(params[5]);
		int blankPenalty = Integer.parseInt(params[6]);
		String lexicon = params[7];
		String speed = params[8];
		boolean allowChat = Boolean.parseBoolean(params[9]);
		boolean allowWatchers = Boolean.parseBoolean(params[10]);
		boolean hasRobot = Boolean.parseBoolean(params[11]);
		int skillLevel = Integer.parseInt(params[12]);
		
		Game newGame = new Game(server, gameID, gameName, maxPlayers, minLength, numSets, blankPenalty, lexicon, speed, allowChat, allowWatchers, hasRobot);
		server.broadcast("addgame " + gameID + " " + gameName + " " + maxPlayers + " " + minLength + " " + numSets + " " + blankPenalty + " " + lexicon + " " + speed + " " + allowChat + " " + allowWatchers + " " + "false");
		newGame.addPlayer(this);
		if(hasRobot)
			newGame.addRobot(new Robot(newGame, skillLevel, server.getDictionary(lexicon), minLength, blankPenalty));

		server.addGame(gameID, newGame);
	}

	/**
	*
	*/
	String getUsername() {
		return username;
	}

	/**
	 * Listens for commands from this worker's client and responds appropriately.
	 */
	private void handleClientSocket() throws IOException {

		inputStream = clientSocket.getInputStream();
		this.outputStream = clientSocket.getOutputStream();
		reader = new BufferedReader(new InputStreamReader(inputStream));

		String line;
		while((line = reader.readLine()) != null) {
			System.out.println("command received: " + line);
			String[] tokens = line.split(" ");

			if (tokens.length > 0) {
				String cmd = tokens[0];
				Game game = tokens.length > 1 ? server.getGame(tokens[1]) : null;

				if(game == null) {
					switch (cmd) {
						case "login" -> handleLogin(tokens[1]);
						case "logoff" -> handleLogoff();
						case "chat" -> {
							server.logChat(line.replaceFirst("chat ", ""));
							server.broadcast(line);
						}
						case "newgame" -> handleCreateGame(tokens);
						case "lookup" -> {
							WordTree tree = new WordTree(tokens[2], server.getDictionary(tokens[1]));
							tree.generateJSON(tree.rootWord, tree.rootNode, 1);
							send("json " + tree.jsonArray.toString());
						}
						case "shutdown" -> {
							if(clientSocket.getInetAddress().isLoopbackAddress())
								System.exit(0);
						}
						default -> System.out.println("Error: Command not recognized: " + line);
					}
				}

				//game-related commands
				else {
					switch (cmd) {
						case "makeword" -> {
							if(server.getDictionary(game.lexicon).contains(tokens[3]))
								game.doMakeWord(tokens[2], tokens[3]);
						}
						case "steal" -> {
							if(server.getDictionary(game.lexicon).contains(tokens[5])) {
								game.doSteal(tokens[2], tokens[3], tokens[4], tokens[5]);
							}
						}
						case "gamechat" -> game.notifyRoom(line);
						case "joingame" -> game.addPlayer(this);
						case "watchgame" -> game.addWatcher(this);
						case "stopplaying" -> game.removePlayer(this.username);
						case "stopwatching" -> game.removeWatcher(this.username);
						case "findplays" -> send("plays " + game.gameID + " " + game.findPlays(Integer.parseInt(tokens[2])));
						default -> System.out.println("Error: Command not recognized: " + line);
					}
				}
			}
		}
	}

	/**
	* Inform the player about events happening on the server
	*
	* @param msg The message to be sent.
	*/
	synchronized void send(String msg) {

		try {
			outputStream.write((msg + "\n").getBytes());
			outputStream.flush();
		}
		catch (IOException e) {
			System.out.println(username + " has unexpectedly disconnected");
			try {
				if(username == null)
					disconnect();
				else
					handleLogoff();
			}
			catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}
	
}