package server;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implements a multiplayer version of the ELO rating system.
 */
class Player {

    boolean abandoned = false;

    static final int D = 400;
    static final int K = 100;
    static final double L = 0.1;

    Game game;
    String name;
    UserData prefs;
    final CopyOnWriteArrayList<String> words = new CopyOnWriteArrayList<>();

    /**
     *
     */
    Player(Game game, String name) {
        this.game = game;
        this.name = name;
    }

    /**
     *
     */
    Player(Game game, String name, UserData prefs) {
        this(game, name);
        this.prefs = prefs;
    }

    /**
     *
     */
    int getRating() {
        return prefs.getRating();
    }

    /**
     *
     */
    void updateRating(int newRating) {
        prefs.setRating(newRating);
    }

    /**
     *
     */
    int getNewRating(int rating, Collection<Player> players) {
        int numPlayers = players.size();
        int numMatches = numPlayers * (numPlayers - 1) / 2;
        double score = getScore();
        double wins = getWins(score, players);
        double expectedWins = getExpectedWins(rating, players);
        double expectedScore = getExpectedScore(rating, players, numMatches);
        double totalScore = getTotalScore(players);

        return (int)Math.round(rating + K*(wins - expectedWins)/numPlayers + K*L*(score - expectedScore)/totalScore);
    }

    /**
     *
     */
    private double getWins(double score, Collection<Player> players) {
        double wins = -0.5;
        for(Player player : players) {
            if(score > player.getScore())
                wins += 1;
            else if(score == player.getScore())
                wins += 0.5;
        }
        return wins;
    }

    /**
     *
     */
    private double getExpectedWins(int rating, Collection<Player> players) {
        double expectedWins = -0.5;
        for(Player player : players) {
            expectedWins += getExpectedWinRate(rating, player.getRating());
        }
        return expectedWins;
    }

    /**
     *
     */
    private static double getExpectedWinRate(double rating, double oppoRating) {
        return 1/(1 + Math.pow(10,(oppoRating - rating)/D));
    }

    /**
     *
     */
    private int getScore() {
        return words.stream()
                .mapToInt(n -> n.length() * n.length())
                .sum();
    }

    /**
     *
     */
    private double getExpectedScore(int rating, Collection<Player> players, int numMatches) {
        return getTotalScore(players)*getExpectedWins(rating, players)/numMatches;
    }

    /**
     *
     */
    private double getTotalScore(Collection<Player> players) {
        return players.stream().mapToDouble(Player::getScore).reduce(0, Double::sum);
    }






}
