package server;

<<<<<<< Updated upstream
=======
import org.json.JSONException;
import org.json.JSONObject;

>>>>>>> Stashed changes
import java.net.Socket;
import java.io.*;

/**
* Handles tasks for a client on the server side.
*/
class ServerWorker extends Thread {

	private final Socket clientSocket;
<<<<<<< Updated upstream
	private final Server server;
	private String username = null;
	private OutputStream outputStream;
	private InputStream inputStream;
	private BufferedReader reader;

=======
	private String username;
	final Server server;
	private boolean guest = true;
	private OutputStream outputStream;
	private InputStream inputStream;
	private BufferedReader reader;
	private UserData prefs;
>>>>>>> Stashed changes

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
	 * Checks whether the provided password matches the stored data
	 *
	 */
	private void checkPassword(String username, String password) {
		System.out.println("received password: " + password);
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		System.out.println("stored password: " + prefs.get("password", null));
		try {
			if(prefs.nodeExists(username)) {
				if(password.equals(prefs.node(username).get("password", null))) {
					System.out.println("response 1");
					send(new JSONObject().put("cmd", "password").put("valid", true));
					return;
				}
				System.out.println("response 2");
				send(new JSONObject().put("cmd", "password").put("valid", false));
				return;
			}
			System.out.println("response 3");
			send(new JSONObject().put("cmd", "username").put("valid", false));

		} catch (BackingStoreException e) {
			throw new RuntimeException(e);
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
<<<<<<< Updated upstream
		ServerWorker duplicate = server.getWorker(username);
		if(duplicate != null) {
			duplicate.send("logoffplayer " + username);
=======
		Optional<ServerWorker> duplicate = server.getWorker(username);
		duplicate.ifPresent(dupe -> {
			dupe.send("logoffplayer", new JSONObject().put("name", username));
>>>>>>> Stashed changes
			server.removeWorker(username);
			System.out.println("Removing duplicate user: " + username);
		}

		prefs = new UserData(username, guest);
		send("login", prefs.get());

		this.username = username;
<<<<<<< Updated upstream
=======
		this.guest = guest;
>>>>>>> Stashed changes
		System.out.println("User logged in successfully: " + username);

		for (String s : server.announcements) {
			send("chat", new JSONObject().put("msg", s));
		}

		for (String s : server.chatLog) {
			send(new JSONObject().put("cmd", "chat").put("msg", s));
		}

<<<<<<< Updated upstream
		//notify new player of other players
		synchronized (server.getUsernames()) {
			for (String playerName : server.getUsernames()) {
				send("loginplayer " + playerName);
			}
=======
		for (ServerWorker worker : server.getWorkers()) {
			send("userdata", worker.getPublicData());
>>>>>>> Stashed changes
		}

		//notify new player of games
		synchronized (server.getGames()) {
			for(Game game : server.getGames()) {
<<<<<<< Updated upstream
				send("addgame " + game.getGameParams());
				for(String playerName : game.getPlayerList()) {
					send("takeseat " + game.gameID + " " + playerName);
=======
				send(game.params.put("cmd", "addgame"));
				for(String playerName : game.players.keySet()) {
					send(new JSONObject()
						.put("cmd", "takeseat")
						.put("gameID", game.gameID)
						.put("name", playerName)
						.put("rating", game.players.get(playerName).getRating() + ""));
>>>>>>> Stashed changes
				}
				if(game.gameOver) {
					send(new JSONObject().put("cmd","endgame").put("gameID", game.gameID).put("gamelog", game.gameLog));
				}
				else {
<<<<<<< Updated upstream
					for(String inactivePlayer : game.getInactivePlayers()) {
						send("abandonseat " + game.gameID + " " + inactivePlayer);
=======
					for(Player player : game.players.values()) {
						if(player.abandoned)
							send(new JSONObject().put("cmd", "abandonseat").put("gameID", game.gameID).put("name", player.name));
>>>>>>> Stashed changes
					}
					if(game.paused) {
						send(new JSONObject().put("cmd", "note").put("game", game.gameID).put("msg", "Game paused"));
					}
					else if(game.timeRemaining > 0) {
						send(new JSONObject().put("cmd", "note").put("game", game.gameID).put("msg", "Time remaining: " + game.timeRemaining));
					}
				}
			}
		}

		//notify other players of the new player
		server.addWorker(username, this);
<<<<<<< Updated upstream
		server.broadcast("loginplayer " + username);
=======
		server.broadcast("userdata", prefs.getPublicData());
	}

	/**
	 *
	 */
	JSONObject getPublicData() {
		return prefs.getPublicData();
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
		interrupt();
=======

		Thread.currentThread().interrupt();
>>>>>>> Stashed changes
	}
	
	/**
	* Creates the game and informs all players
	*/
<<<<<<< Updated upstream
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
=======
	private void handleCreateGame(JSONObject params) {
		server.broadcast(params.put("cmd", "addgame"));
		Game newGame = new Game(server, params);
		newGame.addPlayer(new Player(newGame, username, prefs));
		server.addGame(newGame.gameID, newGame);
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
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
=======
			JSONObject json;
			String cmd;
			try {
				json = new JSONObject(line);
				cmd = json.getString("cmd");
			}
			catch(JSONException je) {
				System.out.println("Malformed JSON expression: " + je);
				continue;
			}

			switch (cmd) {
				case "login" -> handleLogin(json.getString("name"), json.getBoolean("guest"));
				case "password" -> checkPassword(json.getString("name"), json.getString("password"));
 				case "logoff" -> handleLogoff();
				case "chat" -> {
					server.logChat(json.getString("msg"));
					server.broadcast(json);
				}
				case "newgame" -> handleCreateGame(json.getJSONObject("params"));
				case "lookup" -> {
					WordTree tree = new WordTree(json.getString("query"), server.getDictionary(json.getString("lexicon")));
					tree.generateJSON(tree.rootWord, tree.rootNode, 1);
					send("tree", new JSONObject().put("data", tree.jsonArray));
				}
				case "updateprefs" -> prefs.update(json);
				case "updateprofile" -> {
					prefs.setProfile(json.getString("profile"));
					server.broadcast("userdata", getPublicData());
>>>>>>> Stashed changes
				}

				//game-related commands
				default -> {
					Game game = server.getGame(json.getString("gameID"));
					if(game == null) break;

					switch(cmd) {
						case "makeword" -> {
							if (server.getDictionary(game.lexicon).contains(json.getString("word")))
								game.doMakeWord(json.getString("player"), json.getString("word"));
						}
						case "steal" -> {
							if (server.getDictionary(game.lexicon).contains(json.getString("longWord"))) {
								game.doSteal(json.getString("shortPlayer"), json.getString("shortWord"), json.getString("longPlayer"), json.getString("longWord"));
							}
						}
<<<<<<< Updated upstream
						case "gamechat" -> game.notifyRoom(line);
						case "joingame" -> game.addPlayer(this);
						case "watchgame" -> game.addWatcher(this);
						case "stopplaying" -> game.removePlayer(this.username);
						case "stopwatching" -> game.removeWatcher(this.username);
						case "findplays" -> send("plays " + game.gameID + " " + game.findPlays(Integer.parseInt(tokens[2])));
=======
						case "gamechat" -> game.notifyRoom(json);
						case "joingame" -> game.addPlayer(new Player(game, username, prefs));
						case "watchgame" -> game.addWatcher(this);
						case "stopplaying" -> game.removePlayer(username);
						case "stopwatching" -> game.removeWatcher(username);
						case "findplays" -> send(game.findPlays(json.getInt("position")));
>>>>>>> Stashed changes
						default -> System.out.println("Error: Command not recognized: " + line);
					}
				}
			}
		}
	}

	/**
	* Inform the player about events happening on the server
	*
	* @param json The message to be sent.
	*/
	synchronized void send(JSONObject json) {

		try {
			outputStream.write((json + "\n").getBytes());
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

	/**
	 *
	 */
	synchronized void send(String cmd, JSONObject json) {
		send(json.put("cmd", cmd));
	}
}