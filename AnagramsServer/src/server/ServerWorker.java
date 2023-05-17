package server;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;
import java.io.*;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
* Handles tasks for a client on the server side including communication between server and client.
*/
class ServerWorker implements Runnable {

	private final Socket clientSocket;
	private String username;
	final Server server;
	private boolean guest = true;
	private OutputStream outputStream;
	private InputStream inputStream;
	private BufferedReader reader;
	private UserData prefs;

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

		while(!Thread.interrupted()) {
			try {
				handleClientSocket();
			}
			catch (IOException e) {
				Thread.currentThread().interrupt();
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
	private void handleLogin(String username, boolean guest) {

		//Prevents duplicate usernames
		Optional<ServerWorker> duplicate = server.getWorker(username);
		duplicate.ifPresent(dupe -> {
			dupe.send("logoffplayer", new JSONObject().put("name", username));
			server.removeWorker(username);
			System.out.println("Removing duplicate user: " + username);
		});

		prefs = new UserData(username, guest);
		send("login", prefs.get());

		this.username = username;
		this.guest = guest;
		System.out.println("User logged in successfully: " + username);

		for (String s : server.announcements) {
			send("chat", new JSONObject().put("msg", s));
		}

		for (String s : server.chatLog) {
			send(new JSONObject().put("cmd", "chat").put("msg", s));
		}

		for (ServerWorker worker : server.getWorkers()) {
			send("userdata", worker.getPublicData());
		}

		//notify new player of games
		synchronized (server.getGames()) {
			for(Game game : server.getGames()) {
				send(game.params.put("cmd", "addgame"));
				for(String playerName : game.players.keySet()) {
					send(new JSONObject()
						.put("cmd", "takeseat")
						.put("gameID", game.gameID)
						.put("name", playerName)
						.put("rating", game.players.get(playerName).getRating() + ""));
				}
				if(game.gameOver) {
					send(new JSONObject().put("cmd","endgame").put("gameID", game.gameID).put("gamelog", game.gameLog));
				}
				else {
					for(Player player : game.players.values()) {
						if(player.abandoned)
							send(new JSONObject().put("cmd", "abandonseat").put("gameID", game.gameID).put("name", player.name));
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
		server.broadcast("userdata", prefs.getPublicData());
	}

	/**
	 *
	 */
	JSONObject getPublicData() {
		return prefs.getPublicData();
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
	String getUsername() {
		return username;
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

		Thread.currentThread().interrupt();
	}
	
	/**
	* Creates the game and informs all players
	*/
	private void handleCreateGame(JSONObject params) {
		server.broadcast(params.put("cmd", "addgame"));
		Game newGame = new Game(server, params);
		newGame.addPlayer(new Player(newGame, username, prefs));
		server.addGame(newGame.gameID, newGame);
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
						case "gamechat" -> game.notifyRoom(json);
						case "joingame" -> game.addPlayer(new Player(game, username, prefs));
						case "watchgame" -> game.addWatcher(this);
						case "stopplaying" -> game.removePlayer(username);
						case "stopwatching" -> game.removeWatcher(username);
						case "findplays" -> send(game.findPlays(json.getInt("position")));
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