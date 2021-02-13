package server;

import java.io.*;

public class Logger extends Game {

	FileOutputStream logStream;
	PrintStream logPrinter;

	public Logger(Server server, String gameID, int maxPlayers, int minLength, int numSets, int blankPenalty, String lexicon, String speed, boolean allowChat, boolean allowsWatchers) {
		
		super(server, gameID, maxPlayers, minLength, numSets, blankPenalty, lexicon, speed, allowChat, allowsWatchers);

		try {
			logStream = new FileOutputStream("/game logs/log" + gameID + ".txt");
			logPrinter = new PrintStream(logStream, true);
		}
		catch (Exception e){
			System.out.println(e);
		}

		log("gameID " + gameID);
		log("lexicon " + lexicon);
		log("numSets " + numSets);
		log("minLength " + minLength);
		log("blankPenalty " + blankPenalty);
		log("speed " + speed);
		log("");

	}

	/**
	*
	*/

	void log(String text) {
		logPrinter.println(text); 
	}

	/**
	* Stops the gameTimer and sends a notification to the players and watchers the game is over
	*/
	
	synchronized void endGame() {
		super.endGame();
		logPrinter.flush();
		logPrinter.close();
//		System.out.println("analyzing");
//		new Analyzer(gameID, minLength, blankPenalty, server.getDictionary(lexicon));
	}
	
	/**
	* Informs players and watchers of events happening in the room.
	*
	* @param msg The message containing the information to be shared
	*/
	
	synchronized public void notifyRoom(String msg) {
		super.notifyRoom(msg);
		log(super.timeRemaining + " " + msg.replaceFirst(gameID + " ", ""));
	}

	/**
	* Informs players and watchers of time remaining and other game events.
	* The token "@" is used to mark the start of the message.
	*
	* @param message The message to be sent.
	*/

	synchronized void notifyEveryone(String message) {
		super.notifyEveryone(message);
		if(!message.startsWith("note")) {
			log(super.timeRemaining + " " + message.replaceFirst(gameID + " ", ""));
		}
	}

}