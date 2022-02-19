package server;

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */

public class Server extends Thread {
	
	private static final int serverPort = 8118;
	private static final String[] lexicons = {"NWL20", "CSW21"};
	private final HashMap<String, AlphagramTrie> dictionaries = new HashMap<>();
	private final ConcurrentHashMap<String, ServerWorker> workerList = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Game> gameList = new ConcurrentHashMap<>();
	final ConcurrentLinkedQueue<String> chatLog = new ConcurrentLinkedQueue<>();


	/**
	*
	*/
	
	public Server() throws IOException {
		System.out.println("Starting server...");

		for(String lexicon : lexicons) {
			dictionaries.put(lexicon, new AlphagramTrie(lexicon));
			getDictionary(lexicon).common(); //generate the common subset
		}

		HttpServer server = HttpServer.create(new InetSocketAddress(8116), 0);
		server.createContext("/CSW21/", this::handleRequest);
		server.createContext("/NWL20/", this::handleRequest);

		server.start();
		System.out.println("Lookup service started on port 8116");

	}

	/**
	*
	*/
	
	public void removeGame(String gameID) {
		gameList.remove(gameID);
		broadcast("removegame " + gameID);
	}
	
	/**
	*
	*/
	
	public Set<String> getUsernames() {
		return workerList.keySet();
	}
	
	/**
	*
	*/
	
	public void addWorker(String username, ServerWorker worker) {
		workerList.put(username, worker);
	}

	/**
	 * Quietly remove this worker.
	 * @param username The name of the worker to be removed
	 */

	synchronized public void removeWorker(String username) {
		workerList.remove(username);
	}

	/**
	*
	*/
	
	synchronized public void logoffPlayer(String username) {
		workerList.remove(username);

		for(Game game : gameList.values()) {
			game.removePlayer(username);
			game.removeWatcher(username);
		}

		broadcast("logoffplayer " + username);
	}

	/**
	 *
	 */
	
	public ServerWorker getWorker(String username) {
		return workerList.get(username);
	}
	
	/**
	*
	*/
	
	public void addGame(String gameID, Game game) {
		gameList.put(gameID, game);
	}

	/**
	*
	*/
	
	public Collection<Game> getGames() {
		return gameList.values();
	}

	/**
	 *
	 */

	public int getRobotCount() {
		Iterator<Game> it = gameList.values().iterator();
		int robotCount = 0;
		while (it.hasNext()) {
			if(it.next().hasRobot) {
				robotCount++;
			}
		}
		return robotCount;
	}
	
	/**
	*
	*/
	
	public Game getGame(String gameID) {
		return gameList.get(gameID);
	}

	/**
	*
	*/
	
	public AlphagramTrie getDictionary(String lexicon) {
		return dictionaries.get(lexicon);
	}

	/**
	 *
	 */

	public void logChat(String line) {
		chatLog.add(line);
		if(chatLog.size() >= 100) {
			chatLog.remove();
		}
	}
	
	/**
	* Send a message or command to every client
	*
	* @param msg the message to be sent
	*/
	
	public void broadcast(String msg) {
		
		synchronized(workerList) {
			for(ServerWorker worker : workerList.values()) {
				worker.send(msg);
			}
		}
	}

	/**
	*
	*/
	
	@Override
	public void run() {
		
		try {
			ServerSocket serverSocket = new ServerSocket(serverPort);
			while(true) {
				System.out.println("Ready to accept client connections on port " + serverPort);
				Socket clientSocket = serverSocket.accept();
				System.out.println("Accepted connection from " + clientSocket);
				ServerWorker newWorker = new ServerWorker(this, clientSocket);
				newWorker.start();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 *
	 */

	private void handleRequest(final HttpExchange exchange) throws IOException {

		String lexicon = exchange.getRequestURI().getPath().split("/")[1].toUpperCase();
		String query = exchange.getRequestURI().getPath().split("/")[2].toUpperCase();
		System.out.println("lexicon: " + lexicon +", query: " + query);
		WordTree tree = new WordTree(query, getDictionary(lexicon));
		tree.generateJSON(tree.rootWord, tree.rootNode, 1);
		final String json = tree.jsonArray.toString();

		byte[] bytes = json.getBytes();
		exchange.getResponseHeaders().add("Content-type", "application/json");
		exchange.getResponseHeaders().add("Content-length", Integer.toString(bytes.length));
		exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.sendResponseHeaders(200, json.getBytes().length);
		try(OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
			os.flush();
		}
		exchange.close();
	}


	/**
	 *
	 */

	public static void main(String[] args) throws IOException {
		Server server = new Server();
		server.start();


	}

}