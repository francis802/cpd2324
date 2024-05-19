import java.io.*;
import java.net.*;
import java.util.Date;
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

            Auth auth = new Auth(lockPlayer, playerQueue, serverSocket); //@jotas implementa o auth
            executor.submit(auth);

            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleClient(socket));
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    private static void handleClient(Socket socket) {
        try (InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true)) {

            String time = reader.readLine();
            System.out.println("New client connected: " + time);
            writer.println(new Date().toString());

        } catch (IOException ex) {
            System.out.println("Client handling exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
