import java.net.Socket;
import java.io.IOException;
import java.net.SocketException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/***
*
*/

public class ServerWorker extends Thread {

	private final Socket clientSocket;
	private final Server server;
	private final int[] latestVersion = {0, 9, 2};
	public String username = null;
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
			System.out.println(username + " has unexpectedly disconnected.");

			//attempt to reconnect
			try {
				for(Game game : server.getGames()) {
					game.removePlayer(this);
				}

				outputStream.flush();
				reader.close();

				server.removeWorker(username);

				inputStream.close();
				outputStream.close();
				clientSocket.close();

			}
			catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		catch (InterruptedException e) {
			System.out.println(username + " is experiencing connection interruptions.");
			//attempt to reconnect
		}
	}

	/**
	* Listens for commands from this worker's client and responds appropriately.
	*/
	
	private void handleClientSocket() throws IOException, InterruptedException, SocketException {

		inputStream = clientSocket.getInputStream();
		this.outputStream = clientSocket.getOutputStream();
		reader = new BufferedReader(new InputStreamReader(inputStream));

		String line;
		while((line = reader.readLine()) != null) {
			System.out.println("command received: " + line);
			String[] tokens = line.split(" ");

			if (tokens != null && tokens.length > 0) {
				String cmd = tokens[0];
				
				if (cmd.equals("login")) {
					handleLogin(tokens[1]);
				}
				else if(cmd.equals("version")) {
					checkVersion(tokens[1]);
				}
				else if(cmd.equals("logoff")) {
					handleLogoff();
					break;
				}
				else if(cmd.equals("chat")) {
					server.broadcast(line);
				}
				else if(cmd.equals("gamechat")) {
					server.gameList.get(tokens[1]).handleChat(line);
				}
				else if(cmd.equals("newgame")) {
					handleCreateGame(tokens);			
				}
				else if(cmd.equals("joingame")) {
					server.gameList.get(tokens[1]).addPlayer(this);
				}
				else if(cmd.equals("watchgame")) {
					server.gameList.get(tokens[1]).addWatcher(this);
				}
				else if(cmd.equals("exitgame")) {
					server.gameList.get(tokens[1]).removePlayer(this);
				}
				else if(cmd.equals("stopwatching")) {
					server.gameList.get(tokens[1]).removeWatcher(this);
				}
				else if(cmd.equals("makeword")) {
					server.gameList.get(tokens[1]).doMakeWord(tokens[2], tokens[3]);
				}
				else if(cmd.equals("steal")) {
					server.gameList.get(tokens[1]).doSteal(tokens[2], tokens[3], tokens[4], tokens[5]);
				}
				else {	
					System.out.println("Error: Command not recognized: " + line);
				}
			}
		}
		
	//		System.out.println("socket closing");
	//		clientSocket.close();

	}

	/**
	* Checks to see if the client is using an outdated and/or unsupported version of Anagrams.
	*
	*/
	
	private void checkVersion(String version) {
		String[] subversions = version.split("\\.");
		for(int i = 0; i < 3; i++) {
			if(Integer.parseInt(subversions[i]) < latestVersion[i]) {
				send("unsupported");
				return;
			}
		}
		send("ok");
	}

	/**
	* Checks to see whether the user has entered a valid username
	* (And in future versions, a valid password)
	*/
	
	private void handleLogin(String username) throws IOException {

		if(server.getUsernames().contains(username) || username.startsWith("Robot")) {
			send("Sorry, that username is already taken. Please try again.");
			System.out.println("Login failed for " + username);
		}
		else {
			send("ok login");
			this.username = username;

			System.out.println("User logged in successfully: " + username);

			//notify new player of games
			for(Game game : server.getGames()) {
				send("addgame " + game.getGameParams());
			}
			//notify new player of other players
			for(String playerName : server.getUsernames()) {
				send("addplayer " + playerName);
			}

			//notify other players of new player
			server.addWorker(username, this);
			server.broadcast("addplayer " + username);
		}
	}

	/**
	*
	*/

	
	private void handleLogoff() throws IOException {

		for(Game game : server.getGames()) {
			game.removePlayer(this);
		}
		send("logoff");
		outputStream.flush();
		reader.close();

		server.removeWorker(username);

		inputStream.close();
		outputStream.close();
		clientSocket.close();
		
		System.out.println(username + " has just logged off");
		
	}
	
	/**
	*
	*/
	
	private void handleCreateGame(String[] params) throws IOException {
	
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
		
		Game newGame = new Game(server, gameID, maxPlayers, minLength, numSets, blankPenalty, lexicon, speed, allowWatchers, hasRobot, skillLevel);
		
		newGame.addPlayer(this);

		server.broadcast("addgame " + gameID + " " + maxPlayers + " " + minLength + " " + numSets + " " + blankPenalty + " " + lexicon + " " + speed + " " + allowChat + " " + allowWatchers);

		server.addGame(gameID, newGame);
		
	}


	
	/**
	*
	*/
	
	public String getUsername() {
		return username;
	}
	
	/**
	*
	*/
	

	synchronized void send(String msg) {

		try {
			outputStream.write((msg + "\n").getBytes());
			outputStream.flush();
		}
		catch (IOException e) {
			System.out.println(username + " has unexpectedly disonnected");
			//attempt to reconnect
			try {
				for(Game game : server.getGames()) {
					game.removePlayer(this);
				}

				outputStream.flush();
				reader.close();

				server.removeWorker(username);

				inputStream.close();
				outputStream.close();
				clientSocket.close();

			}
			catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}
	
}