package server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

/**
 *
 */
class Game {

	private final Server server;

	private static final String LETTERS = "AAAAAAAAABBCCDDDDEEEEEEEEEEEEFFGGGHHIIIIIIIIIJKLLLLMMNNNNNNOOOOOOOOPPQRRRRRRSSSSTTTTTTUUUUVVWWXYYZ??";
	private char[] tileBag;
	String tilePool = "";
	private int tilesPlayed = 0;

	final JSONObject params;
	final String gameID;
	final String gameName;
	private final int maxPlayers;
	private final int numSets;
	final int minLength;
	final int blankPenalty;
	final int delay;
	final boolean hasRobot;
	final boolean rated;
	final String lexicon;
	final AlphagramTrie dictionary;
	private final String speed;
	private final boolean allowChat;
	private final boolean allowWatchers;

	private Timer gameTimer = new Timer();
	private Timer deleteTimer = new Timer();
	private final Random rgen = new Random();

	boolean paused = false;		//true if game stops due to inactivity
	boolean stopped = true;		//true if there are no Players playing
	boolean gameOver = false;

	private int countdown = 10;
	int timeRemaining;
	private Robot robotPlayer;

	final HashMap<Integer, String> plays = new HashMap<>();		//for some reason HashMap<Integer,JSONObject> fails

	final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();
	final ConcurrentHashMap<String, ServerWorker> watchers = new ConcurrentHashMap<>();

	private WordFinder wordFinder;

	JSONArray gameLog = new JSONArray();

	/**
	 *
	 */
	Game(Server server, JSONObject params) {

		this.server = server;
		this.params = params;
		gameID = params.getString("gameID");
		gameName = params.getString("game_name");
		maxPlayers = params.getInt("max_players");
		minLength = params.getInt("min_length");
		numSets = params.getInt("num_sets");
		blankPenalty = params.getInt("blank_penalty");
		lexicon = params.getString("lexicon");
		dictionary = server.getDictionary(lexicon);
		speed = params.getString("speed");
		allowChat = params.getBoolean("allow_chat");
		allowWatchers = params.getBoolean("allow_watchers");
		hasRobot = params.getBoolean("add_robot");
		int skillLevel = params.getInt("skill_level");
		rated = params.getBoolean("rated");

		switch(speed) {
			case "slow" -> delay = 9;
			case "medium" -> delay = 6;
			default -> delay = 3;
		}

		setUpTileBag(numSets);
		for (int i = 0; i < minLength - 1; i++)
			drawTile();

		timeRemaining = delay*tileBag.length + 31;

		saveState();

		if(hasRobot) {
			addRobot(new Robot(this, skillLevel, dictionary, minLength, blankPenalty));
		}

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

			if (gameLog.length() > 20) {
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
					logger.println(gameLog);
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

		int thinkTime = 0;

		/**
		 *
		 */
		private GameTask() {
			countdown = 10;
		}

		/**
		 *
		 */
		@Override
		public void run() {

			//countdown to start or resume game
			if(countdown > 0) {
				if(tilesPlayed < minLength) {
					String message = "Game will begin in " + countdown + " second" + (countdown == 1 ? "" : "s");
					server.broadcast("note", new JSONObject().put("gameID", gameID).put("msg", message));
				}
				else if (timeRemaining > 0) {
					String message = "Game will resume in " + countdown + " seconds";
					server.broadcast("note", new JSONObject().put("gameID", gameID).put("msg", message));
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
			String message = "Time remaining: " + --timeRemaining;
			server.broadcast("note", new JSONObject().put("gameID", gameID).put("msg", message));

			if(timeRemaining <= 0) {
				endGame();
				return;
			}

			if(tilesPlayed < tileBag.length) {
				if(timeRemaining % delay == 0) {
					drawTile();
					saveState();

					//Robot tries to think of a play
					if(hasRobot && thinkTime < 0) {
						if (robotPlayer.think(tilesPlayed, tileBag.length, tilePool.length())) {
							thinkTime = 2;
						}
					}
				}
			}
			else {
				if (tilePool.isEmpty()) { //no more possible plays
					timeRemaining = 0;
					endGame();
				}
				else if (hasRobot && thinkTime < 0) {
					thinkTime = 15 + rgen.nextInt(50 / (robotPlayer.skillLevel + 1));
				}
			}

			if (hasRobot && thinkTime-- == 0) {
				robotPlayer.makePlay();
			}
		}
	}

	/**
	 *
	 */
	private void pauseGame() {
		paused = true;
		String message = "Game paused";
		server.broadcast("note", new JSONObject().put("gameID", gameID).put("msg", message));
	}


	/**
	 * Stops the gameTimer and sends a notification to the players and watchers that the game is over
	 */
	private void endGame() {

		gameTimer.cancel();
		gameOver = true;

		saveState();

		if(rated) {
			JSONArray ratings = new JSONArray();
			StringJoiner ratingsSummary = new StringJoiner(", ", "New ratings: ", "");
			for(Player player : players.values()) {
				int newRating = player.getNewRating(player.getRating(), players.values());
				player.updateRating(newRating);
				ratings.put(new JSONObject()
						.put("name", player.name)
						.put("rating", String.valueOf(newRating)));
				ratingsSummary.add(player.name + " → " + newRating);
			}
			if(allowChat) {
				notifyRoom("gamechat", new JSONObject()
					.put("gameID", gameID)
					.put("msg", ratingsSummary.toString()));
			}

			server.broadcast("ratings", new JSONObject()
				.put("ratings", ratings));
		}

		server.broadcast("endgame", new JSONObject()
			.put("gameID", gameID)
			.put("gamelog", gameLog));

		wordFinder = new WordFinder(minLength, blankPenalty, dictionary);
		if(hasRobot) {
			wordFinder.trees = robotPlayer.trees;
		}
	}

	/**
	 *
	 */
	JSONObject getState() {
		return gameLog.getJSONObject(gameLog.length() - 1);
	}

	/**
	 * Add a new player to the playerList, inform the newPlayer of the other players/
	 * watchers, and inform the other players/watchers of the newPlayer.
	 *
	 * @param newPlayer The player to be added
	 */
	synchronized void addPlayer(Player newPlayer) {

		if(players.keySet().contains(newPlayer.name)) {
			newPlayer = players.get(newPlayer.name);
			newPlayer.abandoned = false;
		}
		else if(players.size() >= maxPlayers) {
			System.out.println("could not add " + newPlayer.name + " b/c table was full");
			return;
		}

		deleteTimer.cancel();

		//resume game if stopped
		if(stopped) {
			stopped = false;
			gameTimer.cancel();
			gameTimer = new Timer();
			gameTimer.schedule(new GameTask(), 1000, 1000);
		}

		//inform newPlayer of players and their words
		server.getWorker(newPlayer.name).ifPresent(p -> p.send("gamestate", getState().put("gameID", gameID)));

		//inform newPlayer of inactive players
		for (Player player : players.values()) {
			if (player.abandoned)
				server.getWorker(newPlayer.name).ifPresent(p -> p.send("abandonseat", new JSONObject()
						.put("gameID", gameID)
						.put("name", player.name)
				));
		}

		//add the newPlayer
		players.put(newPlayer.name, newPlayer);

		saveState();

		//inform everyone of the newPlayer
		server.broadcast("takeseat", new JSONObject()
				.put("gameID", gameID)
				.put("name", newPlayer.name)
				.put("rating", String.valueOf(newPlayer.getRating())));
	}


	/**
	 *
	 */
	void addRobot(Robot newRobot) {
		robotPlayer = newRobot;
		players.put(newRobot.name, newRobot);

		saveState();

		//inform everyone of the newRobot
		server.broadcast("takeseat", new JSONObject()
				.put("gameID", gameID)
				.put("name", newRobot.name)
				.put("rating", String.valueOf(newRobot.getRating())));
	}

	/**
	 * Remove a player from the playerList and inform the other players.
	 * If there are no more players or watchers left, sends a signal to the server
	 * to end the game.
	 *
	 * @param playerName The name of the player to be removed
	 */
	synchronized void removePlayer(String playerName) {

		Player playerToRemove = players.get(playerName);
		if(playerToRemove == null) return;

		if(gameOver) {
			playerToRemove.abandoned = true;
		}
		else {
			if (playerToRemove.words.isEmpty()) {
				players.remove(playerName);
				server.broadcast("removeplayer", new JSONObject()
						.put("gameID", gameID)
						.put("name", playerName));
			} else {
				playerToRemove.abandoned = true;
				notifyRoom("abandonseat", new JSONObject()
						.put("gameID", gameID)
						.put("name", playerName));
			}
		}

		if(players.values().stream().allMatch(player -> player instanceof Robot || player.abandoned)) {
			stopped = true;
			gameTimer.cancel();
			if(!gameOver) {
				server.broadcast("note", new JSONObject()
						.put("gameID", gameID)
						.put("msg", "Time remaining: " + timeRemaining));
			}
			if(watchers.isEmpty()) {
				deleteTimer.cancel();
				deleteTimer = new Timer();
				deleteTimer.schedule(new DeleteTask(), 180000);
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
			//inform newWatcher of players and their words
			newWatcher.send("gamestate", getState().put("gameID", gameID));

			//inform newWatcher of inactive players
			for(Player player : players.values()) {
				if(player.abandoned) {
					newWatcher.send("abandonseat", new JSONObject().put("gameID", gameID).put("name", player.name));
				}
			}
		}

		watchers.put(newWatcher.getUsername(), newWatcher);
	}


	/**
	 * Remove a watcher from the watcherList and inform the other other players.
	 * If there are no more players or watchers left, sends a signal to the server
	 * to end the game.
	 *
	 * @param watcherToRemove The name of the watcher to be removed
	 */
	synchronized void removeWatcher(String watcherToRemove) {
		watchers.remove(watcherToRemove);

		if(watchers.isEmpty() && players.values().stream().allMatch(player -> player instanceof Robot || player.abandoned)) {
			deleteTimer.cancel();
			deleteTimer = new Timer();
			deleteTimer.schedule(new DeleteTask(), 180000);
		}
	}

	/**
	 * Removes the next tile from the tileBag and puts it in the tilePool. Notifies the players and watchers.
	 */
	private synchronized void drawTile() {
		if(tilesPlayed < tileBag.length) {
			tilePool += tileBag[tilesPlayed];
			tilesPlayed++;
			notifyRoom("nexttiles", new JSONObject().put("gameID", gameID).put("tiles", tilePool));
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

		if(countdown > 0) return false;

		Play play = new Play(shortWord, longWord, tilePool, minLength, blankPenalty);
		if(!play.isValid()) return false;

		if(!Utils.isRearrangement(shortWord.toUpperCase(), longWord.toUpperCase()))
			return false;

		//prevent duplicate words
		for(Player player : players.values()) {
			if(Utils.containsCaseInsensitive(player.words, longWord)) return false;
		}

		String nextWord = play.nextWord();

		if(hasRobot) {
			robotPlayer.removeTree(shortWord);
			robotPlayer.makeTree(nextWord);
		}

		players.get(shortPlayer).words.remove(shortWord);
		players.get(longPlayer).words.add(nextWord);

		tilePool = play.nextTiles;
		String tiles = tilePool.isEmpty() ? "#" : tilePool;

		saveState();

		if(tilesPlayed >= tileBag.length)
			timeRemaining += 15;

		notifyRoom("steal", new JSONObject()
				.put("gameID",gameID)
				.put("shortPlayer", shortPlayer)
				.put("shortWord", dictionary.annotate(shortWord))
				.put("longPlayer", longPlayer)
				.put("longWord", dictionary.annotate(nextWord))
				.put("tiles", tiles));

		//if the shortPlayer has abandoned the game and has no words, make room for another player to join
		Player player = players.get(shortPlayer);
		if(player != null && player.abandoned) {
			if(player.words.isEmpty()) {
				players.remove(shortPlayer);
				server.broadcast("removeplayer", new JSONObject()
						.put("gameID", gameID)
						.put("name", player.name));
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

		if(countdown > 0) return;

		//prevent duplicate words
		for(Player player : players.values()) {
			if(Utils.containsCaseInsensitive(player.words, entry)) return;
		}

		Play play = new Play("", entry, tilePool, minLength, blankPenalty);
		if(!play.isValid()) return;

		String nextWord = play.nextWord();

		players.get(newWordPlayer).words.add(nextWord);

		if(hasRobot) robotPlayer.makeTree(nextWord);

		tilePool = play.nextTiles;
		String tiles = tilePool.isEmpty() ? "#" : tilePool;

		saveState();
		if(tilesPlayed >= tileBag.length) timeRemaining += 15;

		//inform players that a new word has been made
		notifyRoom("makeword", new JSONObject()
				.put("gameID", gameID)
				.put("player", newWordPlayer)
				.put("word", dictionary.annotate(nextWord))
				.put("tiles", tiles));
	}


	/**
	 * Initialize the tileBag with the chosen number of tiles in random order.
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
	 * to the gameLog. The symbol "#" stands in for an empty tile pool.
	 */
	private synchronized void saveState() {
		if(!gameOver) {
			gameLog.put(new JSONObject()
				.put("time", timeRemaining)
				.put("tiles", tilePool.isEmpty() ? "#" : tilePool)
				.put("players", getFormattedWordList()));
		}

	}

	/**
	 * Computes what plays can be made at this position, or (if already computed) retrieves from memory.
	 *
	 * @param position the time corresponding to the position to be analyzed
	 */
	JSONObject findPlays(int position) {
		synchronized(plays) {
			JSONObject data = new JSONObject().put("gameID", gameID);
			if(plays.containsKey(position)) {
				return data.put("data", new JSONObject(this.plays.get(position)));
			}
			else {
				JSONObject plays = wordFinder.findWords(gameLog.getJSONObject(position));
				this.plays.put(position, plays.toString());
				return data.put("data", plays);
			}
		}
	}

	/**
	 * @return A semantically-ordered String containing all active players and Robots with or without words
	 * as well as inactive players with words
	 */
	synchronized JSONArray getFormattedWordList() {
		JSONArray json = new JSONArray();
		for(Player player : players.values()) {
			json.put(new JSONObject()
				.put("name", player.name)
				.put("rating", String.valueOf(player.getRating()))
				.put("words", new JSONArray(player.words.stream().map(word -> dictionary.annotate(word)).toArray())));
		}
		return json;
	}

	/**
	 * Informs players and watchers of events happening in the room.
	 *
	 * @param json The message containing the information to be shared
	 */
	void notifyRoom(String cmd, JSONObject json) {
		synchronized(players) {
			for(Player player : players.values()) {
				if(!player.abandoned) {
					server.getWorker(player.name).ifPresent(worker -> worker.send(cmd, json));
				}
			}
		}
		synchronized(watchers) {
			for(ServerWorker watcher : watchers.values()) {
				watcher.send(cmd, json);
			}
		}
	}

}