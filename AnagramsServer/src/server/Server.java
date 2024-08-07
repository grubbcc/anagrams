package server;

import com.sun.net.httpserver.*;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *
 */
class Server extends Thread {

	private static final int LOOKUP_PORT = 8116;
	private static final int ADMIN_PORT = 8117;
	private static final int GAME_PORT = 8118;


	private static final String[] lexicons = {"NWL23", "CSW21"};
	private final Logger consoleLogger = Logger.getLogger("console");
	private final Logger chatLogger = Logger.getLogger("chat"); // currently unused
	private final HashMap<String, AlphagramTrie> dictionaries = new HashMap<>();
	private final ConcurrentHashMap<String, ServerWorker> workerList = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Game> gameList = new ConcurrentHashMap<>();
	final ArrayDeque<String> announcements = new ArrayDeque<>();
	final ConcurrentLinkedDeque<String> chatLog = new ConcurrentLinkedDeque<>();
	private final ServerSocket serverSocket = new ServerSocket(GAME_PORT);
	private final HttpServer httpServer = HttpServer.create(new InetSocketAddress(LOOKUP_PORT), 0);

	/**
	*
	*/
	Server() throws IOException {

		System.setOut(new PrintStream(System.out) {
			public void print(final String string) {
				consoleLogger.info(string);
			}
		});
		System.setErr(new PrintStream(System.err) {
			public void print(final String string) {
				consoleLogger.error(string);
			}
		});
		System.out.println("Starting server...");

		for(String lexicon : lexicons) {
			dictionaries.put(lexicon, new AlphagramTrie(lexicon));
			getDictionary(lexicon).common(); //generate the common subset
		}

		try {
			BufferedReader reader = new BufferedReader(new FileReader("announcements.txt"));
			for(String line = reader.readLine(); line != null; line = reader.readLine()) {
				announcements.add(line);
			}
			System.out.println("announcements loaded");
			reader.close();
		}
		catch (IOException ioexception) {
			ioexception.printStackTrace();
		}

		try {
			BufferedReader reader = new BufferedReader(new FileReader("chat.log"));
			for(String line = reader.readLine(); line != null; line = reader.readLine()) {
				logChat(line);
			}
			System.out.println("chat log loaded");
			reader.close();
		}
		catch (IOException ioexception) {
			ioexception.printStackTrace();
		}

		httpServer.createContext("/CSW21/", this::handleRequest);
		httpServer.createContext("/NWL23/", this::handleRequest);

		httpServer.start();
		System.out.println("Lookup service started on port 8116");

	}

	/**
	*
	*/
	void removeGame(String gameID) {
		gameList.remove(gameID);
		broadcast("removegame", new JSONObject().put("gameID", gameID));
	}
	
	/**
	*
	*/
	synchronized Set<String> getUsernames() {
		return workerList.keySet();
	}

	/**
	 *
	 */
	synchronized Collection<ServerWorker> getWorkers() {
		return workerList.values();
	}
	
	/**
	*
	*/
	synchronized void addWorker(String username, ServerWorker worker) {
		workerList.put(username, worker);
	}

	/**
	 * Quietly remove this worker.
	 * @param username The name of the worker to be removed
	 */
	void removeWorker(String username) {
		workerList.remove(username);
	}

	/**
	*
	*/
	void logoffPlayer(String username) {
		if(username == null) return;
		workerList.remove(username);

		for(Game game : gameList.values()) {
			game.removePlayer(username);
			game.removeWatcher(username);
		}

		broadcast("logoffplayer", new JSONObject().put("name", username));
	}

	/**
	 *
	 */
	Optional<ServerWorker> getWorker(String username) {
		return Optional.ofNullable(workerList.get(username));
	}
	
	/**
	*
	*/
	void addGame(String gameID, Game game) {
		gameList.putIfAbsent(gameID, game);
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
	 * Helper method for broadcast(JSONObject json)
	 */
	void broadcast(String cmd, JSONObject json) {
	//	synchronized(workerList) {
			for(ServerWorker worker : workerList.values()) {
				worker.send(cmd, json);
			}
	//	}
	}

	/**
	*
	*/
	@Override
	public void run() {
		while(!serverSocket.isClosed()) {
			try {
				System.out.println("Ready to accept client connections on port " + GAME_PORT);
				Socket clientSocket = serverSocket.accept();
				System.out.println("Accepted connection from " + clientSocket);
				new Thread(new ServerWorker(this, clientSocket)).start();
			} catch(IOException e) {
				System.out.println("server socket closed");
			}
        }
	}

	/**
	 *
	 */
	private void handleRequest(final HttpExchange exchange) throws IOException {

		String lexicon = exchange.getRequestURI().getPath().split("/")[1].toUpperCase();
		String query = exchange.getRequestURI().getPath().split("/")[2]/*.toUpperCase()*/;
		System.out.println("lexicon: " + lexicon +", query: " + query);
		WordTree tree = new WordTree(query, getDictionary(lexicon));
		tree.generateJSON(tree.rootNode.getWord().letters, tree.rootNode, 1);
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
	void shutdown() {
		for(String username : getUsernames()) {
			getWorker(username).ifPresent(player -> player.send("logoffplayer", new JSONObject().put("name", username)));
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("chat.log"));
			for(String line : chatLog) {
				writer.write(line + "\n");
			}
			System.out.println("chat file saved");
			writer.close();
			interrupt();
			serverSocket.close();
			httpServer.stop(1);


		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			System.out.println("Program exiting");
			System.exit(0);
		}
	}



	/**
	 *
	 */
	public static void main(String[] args) throws IOException {

		Server gameServer = new Server();
		gameServer.start();

		AdminServer adminServer = new AdminServer(gameServer);
		adminServer.start();

	}

}