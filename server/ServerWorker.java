package server;

import java.net.Socket;
import java.io.*;

/**
* Handles tasks for the client on the server side.
*/

public class ServerWorker extends Thread {

	private final Socket clientSocket;
	private final Server server;
	private final int[] latestVersion = {0, 9, 7};
	private String username = null;
	private OutputStream outputStream;
	private InputStream inputStream;
	private BufferedReader reader;
	
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
		try {
			handleClientSocket();
		}
		catch (IOException e) {
			
			System.out.println(username + " has disconnected.");
		/*	e.printStackTrace();
			
			try {				
				handleLogoff();
			}
			catch (IOException e2) {
				e2.printStackTrace();
			}*/
		}
	}



	/**
	* Checks to see if the client is using an "outdated" and/or "unsupported" version of Anagrams.
	*
	* 
	*
	* @param version the latest version of this software
	*/
	
	private void checkVersion(String version) {
		String[] subversions = version.split("\\.");
		for(int i = 0; i < 3; i++) {
			if(Integer.parseInt(subversions[i]) < latestVersion[i]) {
//				send("outdated");
				send("unsupported");
				return;
			}
		}
		send("ok version");
	}

	/**
	* Checks to see whether the user has entered a valid username
	* (And in future versions, a valid password)
	*/
	
	private void handleLogin(String username) {

		if(server.getUsernames().contains(username) || username.startsWith("Robot")) {
			send("Sorry, that username is already taken. Please try again.");
			System.out.println("Login failed for " + username);
		}
		else {
			send("ok login");

			//use this space to send alert messages or chat messages upon login
			
			send("chat Server says: Welcome to Anagrams version 0.9.7!");

			this.username = username;

			System.out.println("User logged in successfully: " + username);

			//notify new player of other players
			synchronized(server.getUsernames()) {
				for(String playerName : server.getUsernames()) {
					send("loginplayer " + playerName);
				}
			}

			//notify new player of games
			synchronized(server.getGames()) {
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
				}
			}

			//notify other players of the new player
			server.addWorker(username, this);
			server.broadcast("loginplayer " + username);
		}
	}


	/**
	*
	*/
	
	private void handleLogoff() throws IOException {
		
		if(username != null) {
			server.removeWorker(username);
		}

		outputStream.flush();
		reader.close();

		inputStream.close();
		outputStream.close();
		clientSocket.close();
		
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
		
		Game newGame = new Game(server, gameID, maxPlayers, minLength, numSets, blankPenalty, lexicon, speed, allowChat, allowWatchers);
		
		server.broadcast("addgame " + gameID + " " + maxPlayers + " " + minLength + " " + numSets + " " + blankPenalty + " " + lexicon + " " + speed + " " + allowChat + " " + allowWatchers + " " + "false");

		newGame.addPlayer(this);

		if(hasRobot) {
			newGame.addRobot(new Robot(newGame, skillLevel, server.getDictionary(lexicon)));
		}
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
			System.out.println("command received: " + line);
			String[] tokens = line.split(" ");

			if (tokens.length > 0) {
				String cmd = tokens[0];

				if (cmd.equals("login")) {
					handleLogin(tokens[1]);
				}
				else if(cmd.equals("version")) {
					checkVersion(tokens[1]);
				}
				else if(cmd.equals("logoff")) {
					handleLogoff();
				}
				else if(cmd.equals("chat")) {
					server.broadcast(line);
				}
				else if(cmd.equals("newgame")) {
					handleCreateGame(tokens);
				}

				//game related commands
				else if(cmd.equals("makeword")) {
					if(server.getGame(tokens[1]) != null) {
						server.getGame(tokens[1]).doMakeWord(tokens[2], tokens[3]);
					}
				}
				else if(cmd.equals("steal")) {
					if(server.getGame(tokens[1]) != null) {
						server.getGame(tokens[1]).doSteal(tokens[2], tokens[3], tokens[4], tokens[5]);
					}
				}
				else if(cmd.equals("gamechat")) {
					if(server.getGame(tokens[1]) != null) {
						server.getGame(tokens[1]).notifyRoom(line);
					}
				}
				else if(cmd.equals("joingame")) {
					if(server.getGame(tokens[1]) != null) {
						server.getGame(tokens[1]).addPlayer(this);
					}
				}
				else if(cmd.equals("watchgame")) {
					if(server.getGame(tokens[1]) != null) {
						server.getGame(tokens[1]).addWatcher(this);
					}
				}
				else if(cmd.equals("stopplaying")) {
					if(server.getGame(tokens[1]) != null) {
						server.getGame(tokens[1]).removePlayer(this.username);
					}
				}
				else if(cmd.equals("stopwatching")) {
					if(server.getGame(tokens[1]) != null) {
						server.getGame(tokens[1]).removeWatcher(this.username);
					}
				}

				else {
					System.out.println("Error: Command not recognized: " + line);
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