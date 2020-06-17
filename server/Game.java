import java.util.Random;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.io.IOException;
import java.util.Enumeration;
import javax.swing.tree.*;

/**
* 
*/

public class Game {
	
	private Server server;
	
	private HashSet<ServerWorker> playerList = new HashSet<>();
	private HashSet<ServerWorker> watcherList = new HashSet<>();
	private HashSet<Robot> robotList = new HashSet<>();
	
	final String gameID;
	private final String LETTERS = "AAAAAAAAABBCCDDDDEEEEEEEEEEEEFFGGGHHIIIIIIIIIJKLLLLMMNNNNNNOOOOOOOOPPQRRRRRRSSSSTTTTTTUUUUVVWWXYYZ??";
	private char[] tileBag;
	private String tilePool = "";
	private int tileCount = 0;
	public HashMap<String, Vector<String>> words = new HashMap<>();
	
	private int maxPlayers;
	private int numSets;
	int minLength;
	int blankPenalty;
	private int delay;
	private int timeRemaining;
	String lexicon;
	private String speed;
	private boolean allowsChat;
	private boolean allowsWatchers;
	private Timer gameTimer = new Timer(true);
	private GameTask gameTask;
	private Timer deleteTimer;
	private Random rgen = new Random();	

	boolean lock = false;
	int think = 2;
	private Robot robotPlayer;
//	long startTime;
	
	/**
	*
	*/
	
	public String getGameParams() {
		return (gameID + " " + maxPlayers + " " + minLength + " " + numSets + " " + blankPenalty + " " + lexicon + " " + speed + " " + allowsChat + " " + allowsWatchers);
	}
	
	
	/**
	*
	*/

	public Game(Server server, String gameID, int maxPlayers, int minLength, int numSets, int blankPenalty, String lexicon, String speed, boolean allowsWatchers) {
		
		this.server = server;
		this.gameID = gameID;
		this.maxPlayers = maxPlayers;
		this.minLength = minLength;
		this.numSets = numSets;
		this.blankPenalty = blankPenalty;
		this.lexicon = lexicon;
		this.speed = speed;
		this.allowsWatchers = allowsWatchers;
	
		setUpTileBag(numSets);
		
		if(speed.equals("slow"))
			delay = 9;
		else if(speed.equals("medium"))
			delay = 6;
		else
			delay = 3;

		timeRemaining = delay*tileBag.length + 30;	



	}
	
	/**
	*
	*/
	
	public void startGame() {
		GameTask gameTask = new GameTask();
		gameTimer.schedule(gameTask, 1000, 1000);
	}
	
	/**
	*
	*/
	
	private class DeleteTask extends TimerTask {
		
		@Override
		public void run() {
			server.endGame(gameID);	
		}			
	}
	
	/**
	* The sequence of game events
	*/


	private class GameTask extends TimerTask {
		
		private int countdown = 10;

		@Override
		public void run() {

			//draw initial tiles				
			if(countdown == 10 && tileCount < minLength - 1)
				for(int i = 0; i < minLength - 1; i++)
					drawTile();

			if(countdown > 0) {
				if(tileCount < minLength) {
					String message = "Game will begin in " + countdown + " seconds";
					notifyEveryone("note " + gameID + " @" + message);

				}
				else if (timeRemaining > 0) {
					String message = "Game will resume in " + countdown + " seconds";
					notifyEveryone("note " + gameID + " @" + message);
				}
				countdown--;
				return;
			}
			
			else if(timeRemaining > 0) {
				lock = true;
				String message = "Time remaining: " + timeRemaining--;
				notifyEveryone("note " + gameID + " @" + message);				

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

			if(timeRemaining % delay == 0 && tileCount < tileBag.length) {
				drawTile();
				if(!robotList.isEmpty() && rgen.nextInt(60) <= 2*robotPlayer.skillLevel + delay/3 + 7*(tilePool.length()/minLength - 1)) {
					think = 2; //robot starts thinking of a play
				}
			}
			
			//robot-related tasks
			if(!robotList.isEmpty() && think == 0) {
				synchronized(this) {
					lock = false;
					robotPlayer.found = false;

					if(tilePool.length() >= 2*minLength) {
						robotPlayer.makeWord("", "", tilePool.replace("?", ""), minLength);
					}								
					else if(rgen.nextInt(2) == 0 && tilePool.length() >= minLength + 1) {
						robotPlayer.makeWord("", "", tilePool.replace("?", ""), minLength);
					}
					else if(tilePool.length() < tileCount) {
						robotPlayer.makeSteal(words);
					}
				}
			}
			return;
		}
	};
	
	
	/**
	* Stops the gameTimer and sends a notification to the players and watchers the game is over
	*/
	
	synchronized void endGame() {
		gameTimer.cancel();
		gameTimer.purge();

		notifyEveryone("note " + gameID + " @" + "Game Over");
		
		notifyRoom("endgame " + gameID);
	}
	
	/**
	* Add a new player to the playerList, inform the newPlayer of the other players/
	* watchers, and inform the other players/watchers of the newPlayer.
	*
	* @param ServerWorker newPlayer The player to be added
	*/

	synchronized void addPlayer(ServerWorker newPlayer) throws IOException {
		
		if(playerList.isEmpty()) {
			if(deleteTimer != null) {
				deleteTimer.cancel();
				deleteTimer.purge();
				if(timeRemaining > 0) {
					gameTimer = new Timer(true);
					gameTask = new GameTask();
					gameTimer.schedule(gameTask, 1000, 1000);
				}
			}
		}

		//inform new player of players' words
		for(String playerName : words.keySet()) {
			for(String word : words.get(playerName)) {
				newPlayer.send("addword " + gameID + " " + playerName + " " + word);
			}
		}

		//inform newPlayer of watchers
		for(ServerWorker watcher : watcherList) {		
			newPlayer.send("watchgame " + gameID + " " + watcher.getUsername());
		}

		//add the newPlayer
		playerList.add(newPlayer);
		words.put(newPlayer.getUsername(), new Vector<String>());
			
		//inform everyone of the newPlayer
		notifyEveryone("takeseat " + gameID + " " + newPlayer.getUsername());

	}

	/**
	* Remove a player from the playerList and inform the other other players.
	* If there are no more players or watchers left, sends a signal to the server
	* to end the game.
	*
	* @param ServerWorker playerToRemove The player to be removed
	*/
	
	synchronized void removePlayer(ServerWorker playerToRemove) {
		
		playerList.remove(playerToRemove);
		
		if(words.get(playerToRemove.getUsername()) != null) {
		
			if(words.get(playerToRemove.getUsername()).isEmpty()) {
				words.remove(playerToRemove.getUsername());
				notifyEveryone("removeplayer " + gameID + " " + playerToRemove.getUsername());
			}	
		}

		if(playerList.isEmpty() && watcherList.isEmpty()) {

			gameTimer.cancel();
			gameTimer.purge();
			deleteTimer = new Timer(true);
			deleteTimer.schedule(new DeleteTask(), 60000);

		}
	}
	
		
	/**
	* Add a new watcher to the watcherList, inform the newWatcher of the other players/
	* watchers, and inform the other players/watchers of the newWatcher.
	*
	* @param ServerWorker newWatcher The watcher to be added
	*/

	synchronized void addWatcher(ServerWorker newWatcher) throws IOException {
		
		if(deleteTimer != null) {
			deleteTimer.cancel();
			deleteTimer.purge();
		}
		
		//inform newWatcher of players' words
		for(String playerName : words.keySet()) {
			for(String word : words.get(playerName)) {
				newWatcher.send("addword " + gameID + " " + playerName + " " + word);
			}
		}		
		notifyRoom("watchgame " + gameID + " " + newWatcher.username);

		watcherList.add(newWatcher);
	}
	

	
	/**
	* Remove a watcher from the watcherList and inform the other other players.
	* If there are no more players or watchers left, sends a signal to the server
	* to end the game.
	*
	* @param ServerWorker watcherToRemove The watcher to be removed
	*/
	
	synchronized void removeWatcher(ServerWorker watcherToRemove) {
		watcherList.remove(watcherToRemove);
		
		notifyRoom("unwatchgame " + gameID + " " + watcherToRemove.username);

		if(playerList.isEmpty() && watcherList.isEmpty()) {
			gameTimer.cancel();
			gameTimer.purge();
			deleteTimer = new Timer(true);
			deleteTimer.schedule(new DeleteTask(), 60000);
		}
	}

	/**
	* Add an artificially intelligent robot player to this game.
	*
	* @param int skillLevel a measure of how quickly the robot plays and how many words it knows
	*/
	
	void addRobot(Robot newRobot) {
		
//		sendNotification("Robot player is getting ready.");
		
//		robotPlayer = new Robot(this, skillLevel);

		robotList.add(newRobot);
		robotPlayer = newRobot;
		words.put(newRobot.robotName, new Vector<String>());

		//inform everyone of the newPlayer
		notifyEveryone("takeseat " + gameID + " " + newRobot.robotName);
	}
	
	/**
	* Removes the next tile from the tileBag and puts it in the tilePool. Notifies the players and watchers.
	*/

	synchronized void drawTile() {

		if(tileCount < tileBag.length) {
			tilePool += tileBag[tileCount];		
			tileCount++;

			notifyRoom("nexttiles " + gameID + " " + tilePool);
		}
	}
	
	/**
	* Determines whether the longWord can be constructed from letters in the shortWord
	* and the tilePool. If so, the shortWord is taken from its owner, the shortPlayer, the longWord is 
	* formed and awarded to the longPlayer, the tilePool is updated, and the players and 
	* watchers are informed.
	*
	* @param	shortPlayer The owner of the word that is trying to be stolen
	* @param	shortWord 	The word that the is trying to be stolen.
	* @param	longPlayer	The player that is attempting the steal.
	* @param	longWord	The word that the longPlayer is attempting to form.
	* @return				whether the steal is successful
	*/
	
	synchronized boolean doSteal(String shortPlayer, String shortWord, String longPlayer, String longWord) {

		// charsToFind contains the letters that cannot be found in the existing word;
		// they must be taken from the pool or a blank must be redesignated.
		String charsToFind = longWord;

		int blanksAvailable = tilePool.length() - tilePool.replace("?", "").length();
		int blanksToChange = 0;
		int blanksToTakeFromPool = 0;
		
		//lowercase is used to represent blanks
		//If the shortWord contains a tile not found in the longWord, it cannot be stolen unless that tile is a blank
		for(String s : shortWord.split("")) {
			if(charsToFind.contains(s.toUpperCase()))
				charsToFind = charsToFind.replaceFirst(s.toUpperCase(), "");
			else {
				if(Character.isLowerCase(s.charAt(0))) {
					blanksToChange++;
					blanksAvailable++;
				}
				else
					return false;
			}
		}

		//The number of blanksToTakeFromPool is the number of letters found in neither the shortWord nor the pool
		String tiles = tilePool;	
		for(String s : charsToFind.split("")) {
			if(tiles.contains(s))
				tiles = tiles.replaceFirst(s, "");
			else
				blanksToTakeFromPool++;
		}
		
		if(blanksAvailable - blanksToChange < blanksToTakeFromPool)
			return false; //not enough blanks in the pool
		
		//Calculate how long the word needs to be, accounting for the blankPenalty
		int additionalTilesRequired = 1;
		if(blanksToTakeFromPool > 0 || blanksToChange > 0)
			additionalTilesRequired = blankPenalty*blanksToChange + (blankPenalty + 1)*blanksToTakeFromPool;

		if(longWord.length() - shortWord.length() < additionalTilesRequired)
			return false;
		
		//steal is successful
		String oldWord = shortWord;
		String newWord = new String();
		for(String s : longWord.split("")) {
			//move a non-blank from the old word to the new word
			if(oldWord.contains(s)) {
				oldWord = oldWord.replaceFirst(s, "");
				newWord = newWord.concat(s);
			}
			//move a blank from the old word to the new word without redesignating it
			else if(oldWord.contains(s.toLowerCase())) {
				oldWord = oldWord.replaceFirst(s.toLowerCase(), "");
				newWord = newWord.concat(s.toLowerCase());
			}

			else if(charsToFind.length() - blanksToChange > 0 ) {
				//take a non-blank from the pool
				if(tilePool.contains(s)) {
					tilePool = tilePool.replaceFirst(s, "");
					charsToFind = charsToFind.replaceFirst(s, "");
					newWord = newWord.concat(s);
				}
				//take a blank from the pool and designate it
				else {
					tilePool = tilePool.replaceFirst("\\?", "");
					newWord = newWord.concat(s.toLowerCase());
				}
			}
			//move a blank from the old word to the new word and redesignate it
			else {
				oldWord = oldWord.replaceFirst("[a-z]","");
				newWord = newWord.concat(s.toLowerCase());
			}
		}

		if(tilePool.isEmpty())	tiles = "#";

		for(Robot robot : robotList) {
			robot.removeTree(shortWord);
			robot.makeTree(newWord);
		}

		words.get(shortPlayer).remove(shortWord);

		//if this player has left the game and has no words, make room for another player to join
		if(words.get(shortPlayer).isEmpty()) {
			if(!robotList.contains(shortPlayer)) {
				if(!playerList.contains(server.getWorker(shortPlayer))) {
					notifyEveryone("removeplayer " + gameID + " " + shortPlayer);
				}
			}
		}

		
		words.get(longPlayer).add(newWord);	

		if(tileCount >= tileBag.length && tilePool.length() > 0) 
			timeRemaining += 30;

		notifyRoom("nexttiles " + gameID + " " + tiles);
		notifyRoom("removeword " + gameID + " " + shortPlayer + " " + shortWord);
		notifyRoom("addword " + gameID + " " + longPlayer + " " + newWord);

		return true;
	}
	
	
	/**
	* Given a word, determines whether the appropriate tiles can be found in the pool. If so,
	* the word is awarded to the player, the tiles are removed from the pool, and the players
	* and watchers are notified.
	*
	* @param 	newWordPlayer	The name of the player attempting to make the word.
	* @param 	entry 			The word the player is attempting to make.
	* @return 					Whether the word is taken successfully
	*/
	
	synchronized boolean doMakeWord(String newWordPlayer, String entry) {
		
		String tiles = tilePool;
		int blanksAvailable = tilePool.length() - tilePool.replace("?", "").length();
		int blanksRequired = 0;
	
		//If the tilePool does not contain a letter from the entry, a blank must be used
		for(String s : entry.split("")) {
			if(tiles.contains(s)) 
				tiles = tiles.replaceFirst(s, "");
			else
				blanksRequired++;
		}
		
		if(blanksAvailable < blanksRequired)
			return false; //not enough blanks in pool
		
		if(blanksRequired > 0)
			if(entry.length() - minLength < blanksRequired*(blankPenalty + 1))
				return false; //word not long enough

		String newWord = new String();	
		for(String s : entry.split("")) {
			//move a non-blank tile to the new word
			if(tilePool.contains(s)) {
				tilePool = tilePool.replaceFirst(s, "");
				newWord = newWord.concat(s);
			}
			//move a blank to the new word and designate it
			else {
				tilePool = tilePool.replaceFirst("\\?", "");
				newWord = newWord.concat(s.toLowerCase());
			}
		}
	
		words.get(newWordPlayer).add(newWord);		

		for(Robot robot : robotList)
			robot.makeTree(newWord);

		if(tileCount >= tileBag.length && tilePool.length() > 0) 
			timeRemaining += 15;		
	
		if(tilePool.isEmpty())	tiles = "#";	
		//inform players that a new word has been made
	
		notifyRoom("nexttiles " + gameID + " " + tiles);
		notifyRoom("addword " + gameID + " " + newWordPlayer + " " + newWord);		

		return true;
	}
	
	/**
	* Initialize the tileBag with the chosen number of tiles sets.
	*
	* @param numSets The number of tile sets, each of which contains 100 tiles
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
	* Informs players and watchers of events happening in the room.
	*
	* @param msg The message containing the information to be shared
	*/
	
	synchronized public void notifyRoom(String msg) {
		for(ServerWorker player : playerList) {
			player.send(msg);
		}
		for(ServerWorker watcher : watcherList) {
			watcher.send(msg);
		}	
	}

	/**
	* Informs players and watchers of time remaining and other game events.
	* The token "@" is used to mark the start of the message.
	*
	* @param message The message to be sent.
	*/

	synchronized void notifyEveryone(String message) {
		server.broadcast(message);
	}
	
}
