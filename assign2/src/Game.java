import java.net.Socket;
import java.util.List;

import javax.swing.text.html.HTMLDocument.Iterator;

import java.util.ArrayList;


public class Game {

    private List<Socket> userSockets;
    private List<Player> players;
    private int playersNumber;
    private List<Object[]> numbers; // [Player, number]

    public Game(int playersNumber, List<Socket> userSockets, List<Player> players) {
        this.playersNumber = playersNumber;
        this.userSockets = userSockets;
        this.players = players;
    }
    public void start() {
        System.out.println("Starting game with " + userSockets.size() + " players");
        // Ask each player for the number to play the game
        for (Player player : this.players) {
            String message = "Please enter a number between 1 and 100";
            // Send a message to the player
            player.sendMessage(message, player.getSocket());
            // Get the player's response
            int number = player.getNumber();

            this.numbers.add(new Object[]{player, number});
        }

        // Calculate the winner
        int numberWinner = this.calculateWinner();

        // Get the winner/s
        List<Player> winners = this.getWinners(numberWinner);

    }

    private List<Player> getWinners(int numberWinner) {
        List<Player> winners = new ArrayList<>();
        for (Object[] pair : this.numbers) {
            Player player = (Player) pair[0];
            int number = (int) pair[1];
            if (number == numberWinner) {
                winners.add(player);
            }
        }
        return winners;
    }

    private int calculateWinner() {
        int sumNumbers = 0;
        for (Object[] pair : this.numbers) {
            int number = (int) pair[1];
            sumNumbers += number;
        }

        int averageTwoThirds = (sumNumbers / this.numbers.size()) * (2 / 3);
        int closest = this.closestNumber(averageTwoThirds);

        return closest;
    }

    private int closestNumber(int averageTwoThirds) {
        int closest = 0;
        int minDifference = Integer.MAX_VALUE;
        for (Object[] pair : this.numbers) {
            int number = (int) pair[1];
            int difference = Math.abs(averageTwoThirds - number);
            if (difference < minDifference) {
                minDifference = difference;
                closest = number;
            }
        }
        return closest;
    }
}