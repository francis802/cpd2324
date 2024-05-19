import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class Game implements Runnable {

    private List<Player> players;
    private Map<String, Integer> playerNumbers;
    private Map<String, Integer> playerWins;
    private boolean isRanked;

    private static final int MAX_WINS = 5;
    private static final int ELO_K = 32;
    private static final int RECONNECT_TIMEOUT = 60; // segundos

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void run() {
        List<Player> players = new ArrayList<Player>();
        Database db = new Database();
        players = db.getPlayers();
        Game game = new Game(players, true);
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

    public Game(List<Player> players, boolean isRanked) {
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

    // Dont know if this is well implemented
    public void sendMessage(String message, Socket socket) {
        try {
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);
        } catch (IOException e) {   
            e.printStackTrace();
        }
    }

    // Dont know if this is well implemented
    public String getInputFromPlayer(Socket socket) {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Dont know if this is useful
    public boolean broadcastMessage(String message) {
        try {
            for (Player player : this.players) {
                this.sendMessage(message, player.getSocket());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void start() {
        System.out.println("Starting game with " + this.players.size() + " players");
        // Ask each player for the number to play the game
        for (Player player : this.players) {
            String message = "Please enter a number between 1 and 100";
            // Send a message to the player
            this.sendMessage(message, player.getSocket());
            // Get the player's response
            String response = this.getInputFromPlayer(player.getSocket());
            if(response != null) {
                try {
                    int number = Integer.parseInt(response);
                    if(number >= 1 && number <= 100) {
                        this.playerNumbers.replace(player.getUserName(), number);
                    } else {
                        System.out.println("Invalid number");
                    }
                } 
                catch (NumberFormatException e) {
                    System.out.println("Invalid input");
                }
            }
            
            // RANDOM NUMBER between 1 and 100
            // int number = (int) (Math.random() * 100 + 1);
            // this.playerNumbers.replace(player.getUserName(), number);
        }

        // Update the player wins
        this.updatePlayerWins();
    }

    private float calculateChampionProbability(Player player) {
        float sumElo = this.players.stream().mapToInt(Player::getElo).sum();
        return player.getElo() / sumElo;
    }

    private int calculateEloChampion(Player player) {
        return (int) (ELO_K * (1 - this.calculateChampionProbability(player)));
    }

    private int calculateEloLoser(Player player) {
        return (int) (-ELO_K * this.calculateChampionProbability(player));
    }

    private void updateElos() {
        List<Player> champions = new ArrayList<Player>();

        for (Player player : this.players) {
            if (this.playerWins.get(player.getUserName()) == MAX_WINS) {
                champions.add(player);
            } else {
                player.updateElo(this.calculateEloLoser(player));
            }
        }

        for (Player player : champions) {
            System.out.println(player.getUserName() + " has won the game");
            player.updateElo(this.calculateEloChampion(player) / champions.size());
        }
    }

    private void updatePlayerWins() {
        int winningNumber = this.calculateWinningNumber();
        for (var pair : this.playerNumbers.entrySet()) {
            if (pair.getValue() == winningNumber) {
                this.playerWins.put(pair.getKey(), this.playerWins.get(pair.getKey()) + 1);
            }
        }
    }

    private int calculateWinningNumber() {
        int sumNumbers = this.playerNumbers.values().stream().mapToInt(Integer::intValue).sum();
        float averageTwoThirds = (sumNumbers / this.playerNumbers.size()) * (2 / 3);
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
                if (this.isRanked) {
                    this.updateElos();
                }
                return true;
            }
        }
        return false;
    }

    // Dont know if this is well implemented
    public void checkConnections() {
        for (Player player : this.players) {
            Socket socket = player.getSocket();
            if (socket.isClosed()) {
                this.handleDisconnect(player);
            }
        }
    }

    // Dont know if this is well implemented
    private void handleDisconnect(Player player) {
        //player.logOut();
        player.setDisconnectTime(System.currentTimeMillis());
        scheduler.schedule(() -> {
            if (!player.isLoggedIn() && (System.currentTimeMillis() - player.getDisconnectTime()) >= RECONNECT_TIMEOUT * 1000) {
                String message = "Player " + player.getUserName() + " disconnected.";
                this.broadcastMessage(message);
            }
        }, RECONNECT_TIMEOUT, TimeUnit.SECONDS);
    }

    // Dont know if this is well implemented
    private void handleReconnect(Player player, Socket newSocket) {
        if (!player.isLoggedIn() && (System.currentTimeMillis() - player.getDisconnectTime()) <= RECONNECT_TIMEOUT * 1000) {
            player.setSocket(newSocket);
            //player.logIn();
            String message = "Player " + player.getUserName() + " reconnected.";
            this.broadcastMessage(message);
        } else {
            String message = "Player " + player.getUserName() + " failed to reconnect.";
            this.broadcastMessage(message);
            try {
                newSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
