package server;

import com.sun.net.httpserver.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */

public class Server extends Thread {
	
	private static final int GAME_PORT = 8118;
	private static final int LOOKUP_PORT = 8116;

	private static final String[] lexicons = {"NWL20", "CSW21"};
	static final Logger log = Logger.getLogger(Server.class.getName());
	private final HashMap<String, AlphagramTrie> dictionaries = new HashMap<>();
	private final ConcurrentHashMap<String, ServerWorker> workerList = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Game> gameList = new ConcurrentHashMap<>();
	final ConcurrentLinkedQueue<String> chatLog = new ConcurrentLinkedQueue<>();
	private boolean running = true;
	private final ServerSocket serverSocket = new ServerSocket(GAME_PORT);
	private final HttpServer httpServer = HttpServer.create(new InetSocketAddress(LOOKUP_PORT), 0);

	/**
	*
	*/
	
	public Server() throws IOException {

		System.out.println("Starting server...");

		for(String lexicon : lexicons) {
			dictionaries.put(lexicon, new AlphagramTrie(lexicon));
			getDictionary(lexicon).common(); //generate the common subset
		}


		httpServer.createContext("/CSW21/", this::handleRequest);
		httpServer.createContext("/NWL20/", this::handleRequest);

		httpServer.start();
		System.out.println("Lookup service started on port 8116");
	}


	/**
	*
	*/
	
	void removeGame(String gameID) {
		gameList.remove(gameID);
		broadcast("removegame " + gameID);
	}
	
	/**
	*
	*/
	
	Set<String> getUsernames() {
		return workerList.keySet();
	}
	
	/**
	*
	*/
	
	void addWorker(String username, ServerWorker worker) {
		workerList.put(username, worker);
	}

	/**
	 * Quietly remove this worker.
	 * @param username The name of the worker to be removed
	 */

	synchronized void removeWorker(String username) {
		workerList.remove(username);
	}

	/**
	*
	*/
	
	synchronized void logoffPlayer(String username) {
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
	
	ServerWorker getWorker(String username) {
		return workerList.get(username);
	}
	
	/**
	*
	*/
	
	void addGame(String gameID, Game game) {
		gameList.put(gameID, game);
	}

	/**
	*
	*/
	
	Collection<Game> getGames() {
		return gameList.values();
	}

	/**
	 *
	 */

	int getRobotCount() {
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
	
	Game getGame(String gameID) {
		return gameList.get(gameID);
	}

	/**
	*
	*/
	
	AlphagramTrie getDictionary(String lexicon) {
		return dictionaries.get(lexicon);
	}

	/**
	 *
	 */

	void logChat(String line) {
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
	
	void broadcast(String msg) {
		
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

			while(running) {
				System.out.println("Ready to accept client connections on port " + GAME_PORT);
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

		System.setOut(new PrintStream(System.out) {
			public void print(final String string) {
				log.info(string);
			}
		});
		System.setErr(new PrintStream(System.err) {
			public void print(final String string) {
				log.error(string);
			}
		});

		Server server = new Server();
		server.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			server.running = false;

			for(String username : server.getUsernames()) {
				server.getWorker(username).send("logoffplayer " + username );
			}
			try {
				server.serverSocket.close();
				server.httpServer.stop(3);
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("Server shutting down...");
		}));


	}
}