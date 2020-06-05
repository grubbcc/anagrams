import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Set;
import java.util.Collection;

public class Server extends Thread {
	
	private final int serverPort;	
	public int numGames;
	
	private HashMap<String, ServerWorker> workerList = new HashMap<>();
	public HashMap<String, Game> gameList = new HashMap<>();
	
	
	/**
	*
	*/
	
	public Server(int serverPort) {
		this.serverPort = serverPort;
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
		broadcast("removeplayer " + username);
/*		try {
			removedWorker.join();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}*/
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
	
	synchronized public Collection<String> getGameIDs() {
		return gameList.keySet();
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