package server;

import java.net.Socket;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* Handles tasks for the client on the server side.
*/

public class ServerWorker extends Thread {

	private final Socket clientSocket;
	private final Server server;
	private String username = null;
	private OutputStream outputStream;
	private InputStream inputStream;
	private BufferedReader reader;

	private final AtomicBoolean connected = new AtomicBoolean(false);


	/**
	*
	*/
	
	public ServerWorker(Server server, Socket clientSocket) {

		this.server = server;
		this.clientSocket = clientSocket;
	}
	
	/**
	*
	*/


	@Override
	public void run() {
		connected.set(true);
		while(connected.get()) {
			try {
				handleClientSocket();
			}
			catch (IOException e) {
				System.out.println(username + " has disconnected.");
				try {
					handleLogoff();
				}
				catch (IOException ioException) {
					ioException.printStackTrace();
				}
			}
		}
	}


	/**
	* Informs new user of other players and games in progress. Informs other players of new user.
	*/
	
	private void handleLogin(String username) throws IOException {

		//Prevents duplicate usernames
		ServerWorker duplicate = server.getWorker(username);
		if(duplicate != null) duplicate = this;

		send("ok login");
		this.username = username;
		System.out.println("User logged in successfully: " + username);
		//use this space to send alert messages or chat messages upon login

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
				for(String playerName : game.getPlayerList()) {
					send("takeseat " + game.gameID + " " + playerName);
				}
				for(String inactivePlayer : game.getInactivePlayers()) {
					send("abandonseat " + game.gameID + " " + inactivePlayer);
				}
				if(game.gameOver) {
					for(String gameState : game.gameLog) {
						send("gamelog " + game.gameID + " " + gameState);
					}
					send("note " + game.gameID + " @" + "Game over");
					send("endgame " + game.gameID);
				}
				else if(game.paused) {
					send("note " + game.gameID + " @" + "Game paused");
				}
				else if(game.timeRemaining > 0) {
					String message = "Time remaining: " + game.timeRemaining;
					send("note " + game.gameID + " @" + message);
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
		connected.set(false);

		if(username != null) {
			server.removeWorker(username);
		}

		outputStream.flush();
		reader.close();

		inputStream.close();
		outputStream.close();
		clientSocket.close();

		interrupt();
		
		System.out.println(username + " has just logged off");

	}
	
	/**
	*
	*/
	
	private void handleCreateGame(String[] params) {
	
		String gameID = params[1];
		int maxPlayers = Integer.parseInt(params[2]);
		int minLength = Integer.parseInt(params[3]);
		int numSets = Integer.parseInt(params[4]);
		int blankPenalty = Integer.parseInt(params[5]);
		String lexicon = params[6];
		String speed = params[7];
		boolean allowChat = Boolean.parseBoolean(params[8]);
		boolean allowWatchers = Boolean.parseBoolean(params[9]);
		boolean hasRobot = Boolean.parseBoolean(params[10]);
		int skillLevel = Integer.parseInt(params[11]);
		
		Game newGame = new Game(server, gameID, maxPlayers, minLength, numSets, blankPenalty, lexicon, speed, allowChat, allowWatchers, hasRobot, skillLevel);
		server.broadcast("addgame " + gameID + " " + maxPlayers + " " + minLength + " " + numSets + " " + blankPenalty + " " + lexicon + " " + speed + " " + allowChat + " " + allowWatchers + " " + "false");
		newGame.addPlayer(this);
		if(hasRobot)
			newGame.addRobot(new Robot(skillLevel, server.getDictionary(lexicon), minLength, blankPenalty));

		server.addGame(gameID, newGame);
	}

	/**
	*
	*/
	
	public String getUsername() {
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
			//System.out.println("command received: " + line);
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
				handleLogoff();
			}
			catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}
	
}