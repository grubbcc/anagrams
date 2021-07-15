package server;

import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server extends Thread {
	
	private static final int serverPort = 8118;
	private static final String[] lexicons = {"NWL20", "CSW19"};
	private final ConcurrentHashMap<String, ServerWorker> workerList = new ConcurrentHashMap<>();
	private final Hashtable<String, Game> gameList = new Hashtable<>();
	private final HashMap<String, AlphagramTrie> dictionaries = new HashMap<>();
	final ConcurrentLinkedQueue<String> chatLog = new ConcurrentLinkedQueue<>();

	/**
	*
	*/
	
	public Server() {
		System.out.println("Starting server...");

		for(String lexicon : lexicons) {
			dictionaries.put(lexicon, new AlphagramTrie(lexicon));
		}
	}

	/**
	*
	*/
	
	public void endGame(String gameToEnd) {
		gameList.remove(gameToEnd);
		broadcast("removegame " + gameToEnd);
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
	
	public Collection<ServerWorker> getWorkers() {
		return workerList.values();
	}
	
	/**
	*
	*/
	
	public void addWorker(String username, ServerWorker worker) {
		workerList.put(username, worker);
	}
	
	/**
	*
	*/
	
	public void removeWorker(String username) {
		workerList.remove(username);
		synchronized(gameList) {
			for(Game game : gameList.values()) {
				game.removePlayer(username);
				game.removeWatcher(username);
			}
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

	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}

}