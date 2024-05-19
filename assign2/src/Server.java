import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    public static final String HOSTNAME = "localhost";
    public static final int PORT = 8080;
    private final static ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    protected static Database db = new Database();

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Server <gameMode> <PlayersInGame>");
            return;
        }
        int gameMode = 0;
        int playersInGame = 0;
        try {
            gameMode = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number for gameMode (1 - simple or 2 - ranked)");
            return;
        }
        try {
            playersInGame = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number for playersInGame (>= 2)");
            return;
        }
        if (gameMode != 1 && gameMode != 2) {
            System.out.println("Please enter a valid number for gameMode (1 - simple or 2 - ranked)");
            return;
        }
        if (playersInGame < 2) {
            System.out.println("Please enter a valid number for playersInGame (>= 2)");
            return;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);

            ReentrantLock lockPlayer = new ReentrantLock();
            PlayerQueue playerQueue = new PlayerQueue(lockPlayer, gameMode);
            executor.submit(playerQueue);

            Auth auth = new Auth(lockPlayer, playerQueue, serverSocket, executor); //@jotas implementa o auth
            executor.submit(auth);

            while (true) {
                ArrayList<Player> players = new ArrayList<Player>();
                if (gameMode == 1) {
                    players = playerQueue.getPlayersSimple(playersInGame);
                } else if (gameMode == 2){
                    players = playerQueue.getPlayersRanked(playersInGame);
                }
                if (players != null && players.size() == playersInGame) {
                    boolean isRanked = gameMode == 2;
                    Game game = new Game(players, isRanked);
                    executor.submit(game);
                }   
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            executor.shutdown();
            System.out.println("Server is shutting down");
        }
    }
}
