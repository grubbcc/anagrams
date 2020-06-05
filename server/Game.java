import java.util.Random;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.util.Enumeration;
import javax.swing.tree.*;

/**
* To do: put in thread
*/

public class Game {
	
	private Server server;
	private ArrayList<ServerWorker> playerList = new ArrayList<>();
	private ArrayList<ServerWorker> watcherList = new ArrayList<>();
	private ArrayList<Robot> robotList = new ArrayList<>();
	
	private final String gameID;
	private final String LETTERS = "AAAAAAAAABBCCDDDDEEEEEEEEEEEEFFGGGHHIIIIIIIIIJKLLLLMMNNNNNNOOOOOOOOPPQRRRRRRSSSSTTTTTTUUUUVVWWXYYZ??";
	private char[] tileBag;
	private String tilePool = "";
	private int tileCount = 0;
	private HashMap<String, Vector<String>> words = new HashMap<>();
	
	private int maxPlayers;
	private int numSets;
	int minLength;
	int blankPenalty;
	private int delay;
	int timeRemaining;
	String lexicon;
	private String speed;
	private boolean allowsChat;
	private boolean allowsWatchers;
	private Timer timer = new Timer(true);
	private TimerTask task;
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
	
	public HashMap<String, Vector<String>> getWords() {
		return words;
	}
	
	/**
	*
	*/

	public Game(Server server, String gameID, int maxPlayers, int minLength, int numSets, int blankPenalty, String lexicon, String speed, boolean allowsWatchers, boolean hasRobot, int skillLevel) {
		
		this.server = server;
		this.gameID = gameID;
		this.maxPlayers = maxPlayers;
		this.minLength = minLength;
		this.numSets = numSets;
		this.blankPenalty = blankPenalty;
		this.lexicon = lexicon;
		this.speed = speed;
		this.allowsWatchers = allowsWatchers;
		
		if(hasRobot) {		
			addRobot(skillLevel);			
		}
	
		setUpTileBag(numSets);
		
		if(speed.equals("slow"))
			delay = 9;
		else if(speed.equals("medium"))
			delay = 6;
		else
			delay = 3;

		timeRemaining = delay*tileBag.length + 30;		

		start();
	}
	
	/**
	* Begin game loop
	*/
	
	void start() {

		task = new TimerTask() {
			int countdown = 10;
		
			@Override
			public void run() {
			
				//draw initial tiles				
				if(countdown == 10)
					for(int i = 0; i < minLength - 1; i++)
						drawTile();

				if(countdown > 0) {
					sendNotification("Game will begin in " + countdown + " seconds");
					countdown--;
					return;
				}
							
				else if(timeRemaining > 0) {
					lock = true;
					sendNotification("Time remaining: " + timeRemaining--);
					think--;
				}
				
				else {
					timer.cancel();
					timer.purge();
					sendNotification("Game over");
					for(ServerWorker player : playerList) {
						player.send("endgame " + gameID);
					}				
				}

				if(timeRemaining % delay == 0 && tileCount < tileBag.length) {
					drawTile();
					if(!robotList.isEmpty() && rgen.nextInt(50) <= 2*robotPlayer.skillLevel + delay/3 + 7*(tilePool.length()/minLength - 1)) {
						System.out.println("thinking");
						think = 2;
					}
				}
				//robot-related tasks
				if(!robotList.isEmpty() && think == 0) {
					synchronized(this) {
						lock = false;
						robotPlayer.found = false;
//						startTime = System.nanoTime();
						//choose whether to make a word or steal
						if(tilePool.length() >= 2*minLength) {
//							System.out.println(robotPlayer.robotName + " tries to make a word. Pool: " + tilePool);
							robotPlayer.makeWord("", "", tilePool.replace("?",""), minLength);
						}								
						else if(rgen.nextInt(2) == 0 && tilePool.length() >= minLength + 1) {
//							System.out.println(robotPlayer.robotName + " tries to make a word. Pool: " + tilePool);
							robotPlayer.makeWord("", "", tilePool.replace("?",""), minLength);
						}
						else if(tilePool.length() < tileCount) {
							System.out.println(robotPlayer.robotName + " tries to steal. Pool: " + tilePool);
							robotPlayer.makeSteal(words);
						}
					}
				}

			}
		};
		
		timer.scheduleAtFixedRate(task, 1000, 1000);
		
	}
	
	
	/**
	* maybe change to addParticipant() method that works for players, watchers, and robots
	*/

	synchronized void addPlayer(ServerWorker newPlayer) throws IOException {
		//inform newPlayer of all opponents and their words		
		for(String opponent : words.keySet()) {		
			newPlayer.send("joingame " + gameID + " " + opponent);
			for(String word : words.get(opponent)) {
				newPlayer.send("addword " + gameID + " " + opponent + " " + word);
			}
		}
		//inform newPlayer of watchers and vice versa
		for(ServerWorker watcher : watcherList) {		
			newPlayer.send("watchgame " + gameID + " " + watcher.username);
			watcher.send("joingame " + gameID + " " + newPlayer.username);
		}

		//inform opponents of the newPlayer
		for(ServerWorker opponent : playerList) {
			opponent.send("joingame " + gameID + " " + newPlayer.username);
		}

		playerList.add(newPlayer);
		if(!words.containsKey(newPlayer.getUsername())) {
			words.put(newPlayer.getUsername(), new Vector<String>());	
		}
	}
	
	/**
	*
	*/
	
	void addRobot(int skillLevel) {
		
		sendNotification("Robot player is getting ready.");
		
		robotPlayer = new Robot(this, skillLevel);
		robotList.add(robotPlayer);
		
		//inform watchers of the new robot player
		for(ServerWorker watcher : watcherList) {		
			watcher.send("joingame " + gameID + " " + robotPlayer.robotName);
		}

		//inform opponents of the new robot player
		for(ServerWorker opponent : playerList) {
			opponent.send("joingame " + gameID + " " + robotPlayer.robotName);
		}

		words.put(robotPlayer.robotName, new Vector<String>());
		
	}
		
	/**
	*
	*/

	synchronized void addWatcher(ServerWorker newWatcher) throws IOException {
		//inform newWatcher of all players and their words		
		for(String player : words.keySet()) {		
			newWatcher.send("joingame " + gameID + " " + player);
			for(String word : words.get(player)) {
				newWatcher.send("addword " + gameID + " " + player + " " + word);
			}
		}
		//inform players of the newWatcher
		for(ServerWorker player : playerList) {
			player.send("watchgame " + gameID + " " + newWatcher.username);
		}
		//inform watchers of the newWatcher
		for(ServerWorker watcher : watcherList) {
			watcher.send("watchgame " + gameID + " " + newWatcher.username);
		}
		watcherList.add(newWatcher);
	}
	
	/**
	*
	*/
	
	synchronized void removePlayer(ServerWorker playerToRemove) {

		playerList.remove(playerToRemove);
		for(ServerWorker player : playerList) {
			player.send("leavegame " + gameID + " " + playerToRemove.username);
		}


		if(playerList.isEmpty() && watcherList.isEmpty()) {
			task.cancel();
			server.endGame(gameID);
		}
	}
	
	/**
	*
	*/
	
	synchronized void removeWatcher(ServerWorker watcherToRemove) {
		watcherList.remove(watcherToRemove);
		for(ServerWorker watcher : watcherList) {
			watcher.send("unwatchgame " + gameID + " " + watcherToRemove.username);
		}

		if(playerList.isEmpty() && watcherList.isEmpty()) {
			task.cancel();
			server.endGame(gameID);
		}
	}

	
	/**
	* Informs players and watchers of time remaining and other game events
	* 
	* The token "@" is used to mark the start of the message.
	*/

	synchronized void sendNotification(String message) {

		for(ServerWorker player : playerList) {
			player.send("note " + gameID + " @" + message);
		}
		for(ServerWorker watcher : watcherList) {
			watcher.send("note " + gameID + " @" + message);
		}
	}
	
	/**
	*
	*/
	
	synchronized void handleChat(String message) {
		for(ServerWorker player : playerList) {
			player.send(message);
		}
		for(ServerWorker watcher : watcherList) {
			watcher.send(message);
		}		
	}
	
	/**
	* Removes the next tile from the tileBag and puts it in the tilePool. Notifies the players and watchers.
	*/

	synchronized void drawTile() {

		if(tileCount < tileBag.length) {
			tilePool += tileBag[tileCount];		
			tileCount++;
			
			for(ServerWorker player : playerList) {
				player.send("nexttiles " + gameID + " " + tilePool);
			}
			for(ServerWorker watcher : watcherList) {
				watcher.send("nexttiles " + gameID + " " + tilePool);
			}
		}
	}
	
	/**
	*
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

String oldTiles = tilePool; //for testing only
		
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

			else if(charsToFind.length() - blanksToChange > 0 )
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
			//move a blank from the old word to the new word and redesignate it
			else {
				oldWord = oldWord.replaceFirst("[a-z]","");
				newWord = newWord.concat(s.toLowerCase());
			}
		}

		System.out.println(shortWord + " stolen from " + shortPlayer + " by " + longPlayer + " to make " + longWord + ". old pool: " + oldTiles + ", new pool: " + tilePool);
		if(shortWord.length() + oldTiles.length() != longWord.length() + tilePool.length())
			System.out.println("TILE COUNT ERROR OCCURRED WHILE STEALING");	//for testing only
		

		if(tilePool.isEmpty())	tiles = "#";

		for(Robot robot : robotList) {
			robot.removeTree(shortWord);
			robot.makeTree(newWord);
		}

		words.get(shortPlayer).remove(shortWord);
		words.get(longPlayer).add(newWord);	

		if(tileCount >= tileBag.length && tilePool.length() > 0) 
			timeRemaining += 30;

		//inform players that steal has occurred
		try {
			for(ServerWorker player : playerList) {
				player.send("nexttiles " + gameID + " " + tiles);
				player.send("removeword " + gameID + " " + shortPlayer + " " + shortWord);
				player.send("addword " + gameID + " " + longPlayer + " " + newWord);
			}
			for(ServerWorker watcher : watcherList) {
				watcher.send("nexttiles " + gameID + " " + tiles);
				watcher.send("removeword " + gameID + " " + shortPlayer + " " + shortWord);
				watcher.send("addword " + gameID + " " + longPlayer + " " + newWord);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	
	/**
	*
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
String oldTiles = tilePool; //for testing only
		
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

		System.out.println( "new word: " + newWord + " played by " + newWordPlayer + " old pool: " + oldTiles + ", new pool: " + tilePool);
		if(oldTiles.length() != newWord.length() + tilePool.length())
			System.out.println("TILE COUNT ERROR OCCURRED WHILE MAKING A WORD"); //for testing only
		
		words.get(newWordPlayer).add(newWord);		

		for(Robot robot : robotList)
			robot.makeTree(newWord);

		if(tileCount > tileBag.length && tilePool.length() > 0) 
			timeRemaining += 15;		
	
		if(tilePool.isEmpty())	tiles = "#";	
		//inform players that a new word has been made
		
		for(ServerWorker player : playerList) {

			player.send("nexttiles " + gameID + " " + tiles);
			player.send("addword " + gameID + " " + newWordPlayer + " " + newWord);
		}
		for(ServerWorker watcher : watcherList) {
			watcher.send("nexttiles " + gameID + " " + tiles);
			watcher.send("addword " + gameID + " " + newWordPlayer + " " + newWord);
		}
		return true;
	}
	
	/**		
	* Initialize the tileBag with the chosen number of tiles sets.
	*
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
}
