package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
* 
*/

public class Game {
	
	private final Server server;

	private final Hashtable<String, ServerWorker> playerList = new Hashtable<>();
	private final Hashtable<String, ServerWorker> watcherList = new Hashtable<>();

	final String gameID;
	private static final String LETTERS = "AAAAAAAAABBCCDDDDEEEEEEEEEEEEFFGGGHHIIIIIIIIIJKLLLLMMNNNNNNOOOOOOOOPPQRRRRRRSSSSTTTTTTUUUUVVWWXYYZ??";
	private char[] tileBag;
	private String tilePool = "";
	private int tileCount = 0;
	boolean gameOver = false;
	final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> words = new ConcurrentHashMap<>();

	private final int maxPlayers;
	private final int numSets;
	final int minLength;
	final int blankPenalty;
	private final int delay;
	final boolean hasRobot;

	final String lexicon;
	private final String speed;
	private final boolean allowsChat;
	private final boolean allowsWatchers;
	private Timer gameTimer = new Timer();
	private Timer deleteTimer = new Timer();
	private final Random rgen = new Random();

	boolean paused = false;
	private int countdown = 10;
	int timeRemaining;
	private int think = 2;
	private Robot robotPlayer;

	final Vector<String> gameLog = new Vector<>();
	final HashMap<Integer, String> plays = new HashMap<>();
	private WordFinder wordFinder;
	
	/**
	*
	*/
	
	public String getGameParams() {
		return (gameID + " " + maxPlayers + " " + minLength + " " + numSets + " " + blankPenalty + " " + lexicon + " " + speed + " " + allowsChat + " " + allowsWatchers + " " + gameOver);
	}
	
	
	/**
	*
	*/

	public Game(Server server, String gameID, int maxPlayers, int minLength, int numSets, int blankPenalty, String lexicon, String speed, boolean allowChat, boolean allowsWatchers, boolean hasRobot) {
		
		this.server = server;
		this.gameID = gameID;
		this.maxPlayers = maxPlayers;
		this.minLength = minLength;
		this.numSets = numSets;
		this.blankPenalty = blankPenalty;
		this.lexicon = lexicon;
		this.speed = speed;
		this.allowsChat = allowChat;
		this.allowsWatchers = allowsWatchers;
		this.hasRobot = hasRobot;

		setUpTileBag(numSets);
		
		if(speed.equals("slow"))
			delay = 9;
		else if(speed.equals("medium"))
			delay = 6;
		else
			delay = 3;

		timeRemaining = delay*tileBag.length + 30;
		saveState();

	}
	
	/**
	* Deletes the game after three minutes of inactivity and saves the log to file.
	*/
	
	private class DeleteTask extends TimerTask {
		
		@Override
		public void run() {
			server.removeGame(gameID);
			deleteTimer.cancel();

			if (gameLog.size() > 20) {
				try {
					Files.createDirectories(Paths.get("gamelogs"));
					PrintStream logger = new PrintStream(new FileOutputStream("gamelogs/log" + gameID + ".txt"));
					logger.println("gameID " + gameID);
					logger.println("lexicon " + lexicon);
					logger.println("numSets " + numSets);
					logger.println("minLength " + minLength);
					logger.println("blankPenalty " + blankPenalty);
					logger.println("speed " + speed);
					logger.println();
					for (String gameState : gameLog) {
						logger.println(gameState);
					}
					logger.close();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	* The sequence of game events
	*/


	private class GameTask extends TimerTask {

		private GameTask() {
			countdown = 10;
		}

		@Override
		public void run() {

			//draw initial tiles
			if(countdown == 10 && tileCount < minLength - 1)
				for(int i = 0; i < minLength - 1; i++)
					drawTile();

			//countdown to start or resume game
			if(countdown > 0) {
				if(tileCount < minLength) {
					String message = "Game will begin in " + countdown + " seconds";
					server.broadcast("note " + gameID + " @" + message);
				}
				else if (timeRemaining > 0) {
					String message = "Game will resume in " + countdown + " seconds";
					server.broadcast("note " + gameID + " @" + message);
				}
				countdown--;
				return;
			}

			//check if game should be paused
			if(tilePool.length() >= 30 && !hasRobot) {
				pauseGame(); //pause game due to inactivity
				return;
			}
			paused = false;

			//update timer and check for game over
			if(timeRemaining > 0) {
				String message = "Time remaining: " + timeRemaining--;
				server.broadcast("note " + gameID + " @" + message);

				think--;
				if(tileCount >= tileBag.length && tilePool.isEmpty()) {
					//no more possible plays
					timeRemaining = 0;
					endGame();
				}
			}
			else {
				endGame();
			}

			//decide whether robot should attempt a move
			if(timeRemaining % delay == 0 && tileCount < tileBag.length) {
				drawTile();
				if(hasRobot) {
					if (tilePool.length() >= 29) {
						think = 2; //robot starts thinking of a play
					}
					else if (rgen.nextInt(50) <= 3*(robotPlayer.skillLevel-1) + delay/3 + 7*(tilePool.length()/minLength-1)) {
						think = 2; //robot starts thinking of a play
					}
				}
			}
			
			//Robot attempts to make a play
			if(hasRobot && think == 0) {
				robotPlayer.makePlay(Game.this, tilePool, words);
			}
		}
	}

	/**
	 *
	 */

	private synchronized void pauseGame() {
		paused = true;
		String message = "Game paused";
		server.broadcast("note " + gameID + " @" + message);
	}

	/**
	* Stops the gameTimer and sends a notification to the players and watchers that the game is over
	*/
	
	synchronized void endGame() {

		gameTimer.cancel();
		gameOver = true;

		saveState();

		server.broadcast("note " + gameID + " @" + "Game over");

		for(String gameState : gameLog) {
			notifyRoom("gamelog " + gameID + " " + gameState);
		}
		server.broadcast("endgame " + gameID);

		wordFinder = new WordFinder(minLength, blankPenalty, server.getDictionary(lexicon));
		if(hasRobot) {
			wordFinder.trees.putAll(robotPlayer.trees);
		}
	}
	
	/**
	* Add a new player to the playerList, inform the newPlayer of the other players/
	* watchers, and inform the other players/watchers of the newPlayer.
	*
	* @param newPlayer The player to be added
	*/

	synchronized void addPlayer(ServerWorker newPlayer) {

		//stop the delete timer
		deleteTimer.cancel();

		if(!gameOver) {
			//restart the game timer
			if (timeRemaining > 0 && playerList.isEmpty()) {
				gameTimer.cancel();
				gameTimer = new Timer();
				gameTimer.schedule(new GameTask(), 1000, 1000);
			}

			//inform newPlayer of players and their words
			newPlayer.send("gamestate " + gameID + " " + gameLog.lastElement());

			//inform newPlayer of inactive players
			synchronized (getInactivePlayers()) {
				for (String playerName : getInactivePlayers()) {
					newPlayer.send("abandonseat " + gameID + " " + playerName);
				}
			}

			//add the newPlayer
			playerList.put(newPlayer.getUsername(), newPlayer);
			words.putIfAbsent(newPlayer.getUsername(), new CopyOnWriteArrayList<>());

			saveState();

			//inform everyone of the newPlayer
			server.broadcast("takeseat " + gameID + " " + newPlayer.getUsername());
		}
	}

	/**
	* Remove a player from the playerList and inform the other players.
	* If there are no more players or watchers left, sends a signal to the server
	* to end the game.
	*
	* @param playerToRemove The name of the player to be removed
	*/
	
	synchronized void removePlayer(String playerToRemove) {

		if(playerList.containsKey(playerToRemove)) {
			playerList.remove(playerToRemove);

			if(playerList.isEmpty()) {
				gameTimer.cancel();
				if(timeRemaining > 0) {
					String message = "Time remaining: " + timeRemaining;
					server.broadcast("note " + gameID + " @" + message);
				}
				if(watcherList.isEmpty()) {
					deleteTimer.cancel();
					deleteTimer = new Timer();
					deleteTimer.schedule(new DeleteTask(), 180000);
				}
			}
		}

		if(words.containsKey(playerToRemove)) {
			if (words.get(playerToRemove).isEmpty()) {
				words.remove(playerToRemove);
				server.broadcast("removeplayer " + gameID + " " + playerToRemove);
			}
			else {
				notifyRoom("abandonseat " + gameID + " " + playerToRemove);
			}
		}

		saveState();

	}
	
	
	/**
	* Add a new watcher to the watcherList, inform the newWatcher of the other players/
	* watchers, and inform the other players/watchers of the newWatcher.
	*
	* @param newWatcher The name of the watcher to be added
	*/

	synchronized void addWatcher(ServerWorker newWatcher) {

		deleteTimer.cancel();

		if(!gameOver) {
			//inform newPlayer of players and their words
			newWatcher.send("gamestate " + gameID + " " + gameLog.lastElement());

			//inform newWatcher of inactive players
			synchronized (getInactivePlayers()) {
				for (String playerName : getInactivePlayers()) {
					newWatcher.send("abandonseat " + gameID + " " + playerName);
				}
			}
		}
		watcherList.put(newWatcher.getUsername(), newWatcher);
	}

	
	/**
	* Remove a watcher from the watcherList and inform the other other players.
	* If there are no more players or watchers left, sends a signal to the server
	* to end the game.
	*
	* @param watcherToRemove The name of the watcher to be removed
	*/
	
	synchronized void removeWatcher(String watcherToRemove) {
		if(watcherList.containsKey(watcherToRemove)) {
			watcherList.remove(watcherToRemove);

			if (playerList.isEmpty() && watcherList.isEmpty()) {
				deleteTimer.cancel();
				deleteTimer = new Timer();
				deleteTimer.schedule(new DeleteTask(), 180000);
				System.out.println("Beginning countdown; game will disappear in 3 minutes");
			}
		}
	}

	/**
	* Add an artificially intelligent robot player to this game.
	*
	* @param newRobot an artificially intelligent robot player
	*/
	
	synchronized void addRobot(Robot newRobot) {

		robotPlayer = newRobot;
		words.put(newRobot.robotName, new CopyOnWriteArrayList<>());

		saveState();

		//inform everyone of the newRobot
		server.broadcast("takeseat " + gameID + " " + newRobot.robotName);
	}
	
	/**
	* Removes the next tile from the tileBag and puts it in the tilePool. Notifies the players and watchers.
	*/

	synchronized private void drawTile() {
		if(tileCount < tileBag.length) {
			tilePool += tileBag[tileCount];
			tileCount++;
			saveState();
			notifyRoom("nexttiles " + gameID + " " + tilePool);
		}
	}
	
	/**
	* Determines whether the longWord can be constructed from letters in the shortWord
	* and the tilePool. If so, the shortWord is taken from its owner, the shortPlayer, the longWord is 
	* formed and awarded to the longPlayer, the tilePool is updated, and the players and 
	* watchers are informed.
	*
	* @param	shortPlayer	The owner of the word that is trying to be stolen
	* @param	shortWord 	The word that the is trying to be stolen.
	* @param	longPlayer	The player that is attempting the steal.
	* @param	longWord	The word that the longPlayer is attempting to form.
	* @return				whether the steal is successful
	*/
	
	synchronized boolean doSteal(String shortPlayer, String shortWord, String longPlayer, String longWord) {

		if(countdown > 0) {
			return false;
		}

		Play play = new Play(shortWord, longWord, tilePool, minLength, blankPenalty);
		if(!play.isValid())
			return false;

		String nextWord = play.nextWord();


			robotPlayer.removeTree(shortWord);
			robotPlayer.makeTree(nextWord);


		words.get(shortPlayer).remove(shortWord);
		words.get(longPlayer).add(nextWord);

		if(tileCount >= tileBag.length && tilePool.length() > 0)
			timeRemaining += 15;

		tilePool = play.nextTiles;
		String tiles = tilePool.isEmpty() ? "#" : tilePool;

		saveState();

		notifyRoom("steal " + gameID + " " + shortPlayer + " " + shortWord + " " + longPlayer + " " + nextWord + " " + tiles);

		//if the shortPlayer has left the game and has no words, make room for another player to join
		if(words.get(shortPlayer).isEmpty()) {
			if (!shortPlayer.startsWith("Robot")) {
				if (server.getWorker(shortPlayer) == null) { //player is not logged in
					server.broadcast("removeplayer " + gameID + " " + shortPlayer);
				}
				else if (!playerList.containsKey(shortPlayer)) { //player has left the game
					server.broadcast("removeplayer " + gameID + " " + shortPlayer);
				}
			}
		}
		
		return true;
	}
	
	
	/**
	* Given a word, determines whether the appropriate tiles can be found in the pool. If so,
	* the word is awarded to the player, the tiles are removed from the pool, and the players
	* and watchers are notified.
	*
	* @param    newWordPlayer    The name of the player attempting to make the word.
	* @param    entry            The word the player is attempting to make.
	 */
	
	synchronized void doMakeWord(String newWordPlayer, String entry) {

		if(countdown > 0) {
			return;
		}
		Play play = new Play("", entry, tilePool, minLength, blankPenalty);
		if(!play.isValid())
			return;

		String nextWord = play.nextWord();

		words.get(newWordPlayer).add(nextWord);

		robotPlayer.makeTree(nextWord);

		if(tileCount >= tileBag.length && tilePool.length() > 0) 
			timeRemaining += 15;
		tilePool = play.nextTiles;
		String tiles = tilePool.isEmpty() ? "#" : tilePool;

		//inform players that a new word has been made	
		saveState();	
	
		notifyRoom("makeword " + gameID + " " + newWordPlayer + " " + nextWord + " " + tiles);

	}


		/**
        * Initialize the tileBag with the chosen number of tiles sets.
        *
        * @param numSets The number of tile sets, each of which consists of 100 tiles
        */

	private void setUpTileBag(int numSets) {
		tileBag = new char[numSets*100];

		for(int n = 0; n < tileBag.length; n += 100) {
			for(int i = 0; i < 100; i++) {
				tileBag[n + i] = LETTERS.charAt(i);
			}
		}

		// Mix tile bag
		Random rgen = new Random();

		for (int i = 0; i < tileBag.length; i++) {
			int randomPosition = rgen.nextInt(tileBag.length);
			char temp = tileBag[i];
			tileBag[i] = tileBag[randomPosition];
			tileBag[randomPosition] = temp;
		}
	}

	/**
	* Adds a String describing the current game state, e.g.
	 * "257 YU?IFOT GrubbTime [HAUYNES] Robot-Genius [BLEWARTS,POTJIES]"
	 * to the gameLog.
	*/

	synchronized private void saveState() {
		if(!gameOver) {
			String tiles = tilePool.isEmpty() ? "#" : tilePool;
			gameLog.add(timeRemaining + " " + tiles + " " + getFormattedWordList());
		}
	}


	/**
	 * Computes what plays can be made at this position, or (if already computed) retrieves from memory.
	 *
	 * @param position the time corresponding to the position to be analyzed
	 */

	synchronized String findPlays(int position) {
		return plays.computeIfAbsent(position, k -> wordFinder.findWords(gameLog.elementAt(position)));
	}
	
	/**
	* @return the names of all players and Robots that are either active or have at least one word.
	*/
	
	synchronized public Set<String> getPlayerList() {

		Set<String> union = new HashSet<>(words.keySet());
		union.addAll(playerList.keySet());
		if(robotPlayer != null)
			union.add(robotPlayer.robotName);
		
		return union;
	}
	
	/**
	* @return a set of all players who have left the game but still have at least one word.
	*/
	
	synchronized public Set<String> getInactivePlayers() {
		Set<String> union = new HashSet<>(words.keySet());
		union.removeAll(playerList.keySet());
		if(robotPlayer != null)
			union.removeAll(Collections.singleton(robotPlayer.robotName));

		return union;
	}

	/**
	 * @return A semantically-ordered String containing all active players and Robots with or without words
	 * as well as inactive players with words, e.g.
	 * player1 [HELLO, WORLD] player2 []
	*/

	synchronized private String getFormattedWordList() {
		StringBuilder wordList = new StringBuilder();
		Set<String> union = getPlayerList();

		for(String playerName : union) {
			if(words.containsKey(playerName)) {
				wordList.append(playerName).append(" ").append(words.get(playerName).toString().replace(", ", ",")).append(" ");
			}
			else {
				wordList.append(playerName).append(" [] ");
			}
		}

		return wordList.toString();
	}
	
	/**
	* Informs players and watchers of events happening in the room.
	*
	* @param msg The message containing the information to be shared
	*/
	
	void notifyRoom(String msg) {
		synchronized(playerList) {
			for(ServerWorker player : playerList.values()) {
				player.send(msg);
			}
		}
		synchronized(watcherList) {
			for(ServerWorker watcher : watcherList.values()) {
				watcher.send(msg);
			}
		}
	}

	
}