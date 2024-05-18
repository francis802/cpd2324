import java.net.Socket;
import java.util.List;
import java.util.Map;

import javax.swing.text.html.HTMLDocument.Iterator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;


public class Game {

    private List<Socket> userSockets;
    private List<Player> players;
    private Map<String, Integer> playerNumbers;
    private Map<String, Integer> playerWins;
    private boolean isRanked;

    private static final int MAX_WINS = 5;
    private static final int ELO_K = 32;

    public static void main(String[] args) {
        List<Player> players = new ArrayList<Player>();
        Database db = new Database();
        players = db.getPlayers();
        Game game = new Game(new ArrayList<Socket>(), players, true);
        while (true) {
            game.start();
            if (game.isGameOver()) {
                break;
            }
        }
        for (Player player : players) {
            System.out.println(player.getUserName() + " has " + player.getElo() + " elo");
        }
    }

    public Game(List<Socket> userSockets, List<Player> players, boolean isRanked) {
        this.userSockets = userSockets;
        this.isRanked = isRanked;
        this.players = players;
        this.players.sort((p1, p2) -> p1.getElo() - p2.getElo());
        this.playerWins = new HashMap<String, Integer>();
        this.playerNumbers = new HashMap<String, Integer>();
        for (Player player : this.players) {
            this.playerWins.put(player.getUserName(), 0);
            this.playerNumbers.put(player.getUserName(), 0);
        }
    }
    
    public void start() {
        System.out.println("Starting game with " + userSockets.size() + " players");
        // Ask each player for the number to play the game
        for (Player player : this.players) {
            // String message = "Please enter a number between 1 and 100";
            // Send a message to the player
            // this.sendMessage(message, player.getSocket());
            // Get the player's response
            // int number = this.getNumber(player.getSocket());
            
            
            // RANDOM NUMBER between 1 and 100
            int number = (int) (Math.random() * 100 + 1);

            this.playerNumbers.replace(player.getUserName(), number);
        }

        // Update the player wins
        this.updatePlayerWins();

    }

    private float calculateChampionProbability(Player player) {
        float sumElo = this.players.stream().mapToInt(Player::getElo).sum();
        return player.getElo()/sumElo;
    }

    private int calculateEloChampion(Player player) {
        return (int) (ELO_K*(1-this.calculateChampionProbability(player)));
    }

    private int calculateEloLoser(Player player) {
        return (int) (-ELO_K*this.calculateChampionProbability(player));
    }

    private void updateElos() {
        List<Player> champions = new ArrayList<Player>();
        
        for (Player player : this.players) {
            if (this.playerWins.get(player.getUserName()) == MAX_WINS) {
                champions.add(player);
            }
            else {
                player.updateElo(this.calculateEloLoser(player));
            }
        }

        for (Player player : champions) {
            System.out.println(player.getUserName() + " has won the game");
            player.updateElo(this.calculateEloChampion(player)/champions.size());
        }
    }

    private void updatePlayerWins() {
        int winningNumber = this.calculateWinningNumber();
        for (var pair : this.playerNumbers.entrySet()) {
            if (pair.getValue() == winningNumber) {
                this.playerWins.put(pair.getKey(), this.playerWins.get(pair.getKey())+1);
            }
        }
    }

    private int calculateWinningNumber() {
        int sumNumbers = this.playerNumbers.values().stream().mapToInt(Integer::intValue).sum();
        float averageTwoThirds = (sumNumbers/this.playerNumbers.size())*(2/3);
        return this.closestNumber(averageTwoThirds);
    }

    private int closestNumber(float averageTwoThirds) {
        int closest = 0;
        float minDifference = Integer.MAX_VALUE;
        for (float number : this.playerNumbers.values()) {
            float difference = Math.abs(number - averageTwoThirds);
            if (difference < minDifference) {
                minDifference = difference;
                closest = (int) number;
            }
        }
        return closest;
    }

    public boolean isGameOver() {
        for (var pair : this.playerWins.entrySet()) {
            if (pair.getValue() == MAX_WINS) {
                if(this.isRanked) {
                    this.updateElos();
                }
                return true;
            }
        }
        return false;
    }
}