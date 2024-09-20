package server;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;

/**
* Handles tasks for a client on the server side including communication between server and client.
*/
class ServerWorker implements Runnable {

	private final Socket clientSocket;
	private String username;
	final Server server;
	private OutputStream outputStream;
	private InputStream inputStream;
	private BufferedReader reader;
	private UserData prefs;

	private static final Properties props = new Properties() {{
		put("mail.smtp.auth", "true");
		put("mail.smtp.starttls.enable", "true");
		put("mail.smtp.ssl.protocols", "TLSv1.2");
		put("mail.smtp.host", "mail.anagrams.site");
		put("mail.smtp.port", "587");
	}};
	private static final String from = "admin@anagrams.site";
	private String code;
	private final Session session = Session.getInstance(props, new Authenticator() {
		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			Preferences prefs = Preferences.userNodeForPackage(Server.class);
			String password = prefs.get("password", "");
			return new PasswordAuthentication(from, password);
		}
	});

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
     *
     * Checks whether the provided username is available and, if not, whether
     * the username is registered or currently being used.
     */
    private void checkUsername(String username) {
		boolean inUse = server.getUsernames().contains(username);

		try {
			boolean registered = Preferences.userNodeForPackage(Server.class).nodeExists(username);
			boolean available = !(inUse || registered);
			send("availability", new JSONObject()
					.put("available", available)
					.put("registered", registered));
		} catch (BackingStoreException e) {
			throw new RuntimeException(e);
		}
    }

	/**
	 * Check if the provided email address is already associated with a username. If it is not,
	 * send an email with a 6-digit code to verify ownership of the email account.
	 */
	void checkEmail(String email) {

		Preferences prefs = Preferences.userNodeForPackage(Server.class);
		try {
			for (String user : prefs.childrenNames()) {
				if (prefs.node(user).get("email", "").equals(email)) {
					send("availability", new JSONObject().put("available", false));
					return;
				}
			}
		}
		catch(BackingStoreException bse) {
			bse.printStackTrace();
		}
		send("availability", new JSONObject().put("available", true));
		try(ExecutorService emailExecutor = Executors.newSingleThreadExecutor()) {
			Random rand = new Random();
			code = String.format("%06d", rand.nextInt(1_000_000));

			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
			message.setSubject("Anagrams registration");
			message.setText("Your registration code for Anagrams is: " + code);

			emailExecutor.execute(() -> {
				try {
					Transport.send(message);
				}
				catch (MessagingException e) {
					e.printStackTrace();
				}
			});
			emailExecutor.shutdown();
		}
		catch (MessagingException mex) {
			mex.printStackTrace();
		}

	}

	/**
	 * Check if the user has provided the correct registration code that was sent to their email.
	 * If so, save username, password, and email to the prefs.xml file and log in.
	 */
	private void register(JSONObject json) {
		if(json.getString("code").equals(code)) {
			String username = json.getString("username");
			Preferences prefs = Preferences.userNodeForPackage(Server.class).node(username);

			prefs.put("password", json.getString("password"));
			prefs.put("email", json.getString("email"));
			handleLogin(json.getString("username"), false);
		}
		else {
			send("code", new JSONObject().put("valid", false));
		}
	}

	/**
	 * Checks whether the provided password matches the stored data and informs the user.
	 */
	private void checkPassword(String username, String password) {

		Preferences prefs = Preferences.userNodeForPackage(Server.class).node(username);

		if(password.equals(prefs.get("password", ""))) {
			send("password", new JSONObject().put("valid", true));
		}
		else {
			send("password", new JSONObject().put("valid", false));
		}
	}


	/**
	 * Provides the username/password associated with the given email address, if any.
	 */
	void recover(JSONObject json) {
		Preferences prefs = Preferences.userNodeForPackage(Server.class);
		String email = json.getString("email");
		String type = json.getString("type");
		try {
			for (String user : prefs.childrenNames()) {
				if (prefs.node(user).get("email", "").equals(email)) {
					MimeMessage message = new MimeMessage(session);
					try(ExecutorService emailExecutor = Executors.newSingleThreadExecutor()) {
						message.setFrom(new InternetAddress(from));
						message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
						if(type.equals("password")) {
							message.setSubject("Password recovery");
							message.setText("Your password for Anagrams is " + new String(Base64.getDecoder().decode(prefs.node(user).get("password", ""))));
						}
						else {
							message.setSubject("Username recovery");
							message.setText("Your username for Anagrams is " + user);
						}
						send(type + "-recovery", new JSONObject().put("success", true));

						emailExecutor.execute(() -> {
							try {
								Transport.send(message);
							} catch (MessagingException e) {
								throw new RuntimeException(e);
							}

						});
						emailExecutor.shutdown();
						return;
					}
					catch(MessagingException mex) {
						mex.printStackTrace();
					}
				}

			}
			//email not found among registered accounts
			send(type + "-recovery", new JSONObject().put("success", false));
		}
		catch(BackingStoreException bse) {
			bse.printStackTrace();
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
		System.out.println("User logged in successfully: " + username);

		for (String s : server.announcements) {
			send("chat", new JSONObject().put("msg", s));
		}

		for (String s : server.chatLog) {
			send("chat", new JSONObject().put("msg", s));
		}

		for (ServerWorker worker : server.getWorkers()) {
			send("userdata", worker.getPublicData());
		}

		//notify new player of games
		synchronized (server.getGames()) {
			for(Game game : server.getGames()) {
				send("addgame", game.params);
				for(String playerName : game.players.keySet()) {
					send("takeseat", new JSONObject()
						.put("gameID", game.gameID)
						.put("name", playerName)
						.put("rating", game.players.get(playerName).getRating() + ""));
				}
				if(game.gameOver) {
					send("endgame", new JSONObject().put("gameID", game.gameID).put("gamelog", game.gameLog));
				}
				else {
					for(Player player : game.players.values()) {
						if(player.abandoned)
							send("abandonseat", new JSONObject().put("gameID", game.gameID).put("name", player.name));
					}
					if(game.paused) {
						send("note", new JSONObject().put("gameID", game.gameID).put("msg", "Game paused"));
					}
					else if(game.timeRemaining > 0) {
						send("note", new JSONObject().put("gameID", game.gameID).put("msg", "Time remaining: " + game.timeRemaining));
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
	void deleteAccount() {
		prefs.remove();
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
		reader.close();
		clientSocket.close();

		System.out.println(username + " has disconnected");

		Thread.currentThread().interrupt();
	}

	/**
	* Creates the game and informs all players
	*/
	private void handleCreateGame(JSONObject params) {
		server.broadcast("addgame", params);
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
				case "chat" -> {
					server.logChat(json.getString("msg"));
					server.broadcast("chat", json);
				}
				case "delete" -> deleteAccount();
				case "email" -> checkEmail(json.getString("email"));
				case "forgot" -> recover(json);
				case "login" -> handleLogin(json.getString("name"), json.getBoolean("guest"));
				case "logoff" -> handleLogoff();
				case "newgame" -> handleCreateGame(json.getJSONObject("params"));
				case "password" -> checkPassword(json.getString("name"), json.getString("password"));
				case "register" -> register(json);
				case "username" -> checkUsername(json.getString("username"));
				case "lookup" -> {
					WordTree tree = new WordTree(json.getString("query"), server.getDictionary(json.getString("lexicon")));
					tree.generateJSON(tree.rootWord + tree.rootNode.getWord().suffix, tree.rootNode, 1);
					send("tree", new JSONObject().put("data", tree.jsonArray));
				}
				case "updateprefs" -> prefs.update(json);
				case "updateprofile" -> {
					prefs.setProfile(json.getString("profile"));
					server.broadcast("userdata", getPublicData());
				}

				//game-related commands
				default -> {
					String gameID = json.optString("gameID");
					if(gameID.isEmpty()) {
						System.out.println("Error: command not recognized " + line);
						break;
					}
					Game game = server.getGame(json.optString("gameID"));

					if(game != null) {
						switch (cmd) {
							case "findplays" -> send("plays", game.findPlays(json.getInt("position")));
							case "gamechat" -> game.notifyRoom("gamechat", json);
							case "joingame" -> {
								if (game.gameOver)
									game.addWatcher(this);
								else
									game.addPlayer(new Player(game, username, prefs));
							}
							case "makeword" -> {
								if (server.getDictionary(game.lexicon).contains(json.getString("word")))
									game.doMakeWord(json.getString("player"), json.getString("word"));
							}
							case "steal" -> {
								if (server.getDictionary(game.lexicon).contains(json.getString("longWord"))) {
									game.doSteal(json.getString("shortPlayer"), json.getString("shortWord"), json.getString("longPlayer"), json.getString("longWord"));
								}
							}
							case "stopplaying" -> game.removePlayer(username);
							case "stopwatching" -> game.removeWatcher(username);
							case "watchgame" -> game.addWatcher(this);
						}
					}
				}
			}
		}
	}

    /**
	* Inform the user about events happening on the server
	*
	* @param json The message to be sent.
	*/
	synchronized void send(String cmd, JSONObject json) {

		try {
			outputStream.write((json.put("cmd", cmd) + "\n").getBytes());
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