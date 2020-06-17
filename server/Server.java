import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Set;
import java.util.Collection;

public class Server extends Thread {
	
	private final int serverPort;	
	private String[] lexicons = {"NWL18", "CSW19", "LONG"};
	private HashMap<String, ServerWorker> workerList = new HashMap<>();
	private HashMap<String, Game> gameList = new HashMap<>();
	private HashMap<String, AlphagramTrie> dictionaries = new HashMap<>();
	
	
	/**
	*
	*/
	
	public Server(int serverPort) {
		this.serverPort = serverPort;
		for(String lexicon : lexicons) {
			dictionaries.put(lexicon, new AlphagramTrie(lexicon));
		}
	}
	
	/**
	*
	*/
	
	synchronized public void endGame(String gameToEnd) {
		
		gameList.remove(gameToEnd);
		String cmd = "removegame " + gameToEnd;
		broadcast(cmd);
	}
	
	/**
	*
	*/
	
	synchronized public Set<String> getUsernames() {
		return workerList.keySet();
	}
	
	/**
	*
	*/
	
	synchronized public Collection<ServerWorker> getWorkers() {
		return workerList.values();
	}
	
	/**
	*
	*/
	
	synchronized public void addWorker(String username, ServerWorker worker) {
		workerList.put(username, worker);
	}
	
	/**
	*
	*/
	
	synchronized public void removeWorker(String username) {
		
		ServerWorker removedWorker = workerList.remove(username);
		broadcast("logoffplayer " + username);

	}
	
	synchronized public ServerWorker getWorker(String username) {
		return workerList.get(username);
	}
	
	/**
	*
	*/
	
	synchronized public void addGame(String gameID, Game game) {
		gameList.put(gameID, game);
	}
	
	
	/**
	*
	*/
	
	synchronized public Collection<Game> getGames() {
		return gameList.values();
	}
	
	/**
	*
	*/
	
	synchronized public Game getGame(String gameID) {
		return gameList.get(gameID);
	}

	/**
	*
	*/
	
	synchronized public AlphagramTrie getDictionary(String lexicon) {
		return dictionaries.get(lexicon);
	}
	
	/**
	* Send a message or command to every client
	*
	* @param msg the message to be sent
	*/
	
	synchronized void broadcast(String msg) {
		for(ServerWorker worker : workerList.values()) {
			worker.send(msg);
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
				ServerWorker worker = new ServerWorker(this, clientSocket);
				worker.start();

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

}
