import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Auth implements Runnable{

    private final ReentrantLock lockPlayer;
    private final PlayerQueue playerQueue;
    private final ServerSocket serverSocket;
    private final ExecutorService executor;

    public Auth(ReentrantLock lockPlayer, PlayerQueue playerQueue, ServerSocket serverSocket, ExecutorService executor) {
        this.lockPlayer = lockPlayer;
        this.playerQueue = playerQueue;
        this.serverSocket = serverSocket;
        this.executor = executor;
    }

    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleClient(socket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void handleClient(Socket socket) {
        try  {
            CommnSocket commnSocket = new CommnSocket(socket);
            StringBuilder message = new StringBuilder();
            message.append("Welcome to the game server! 1 - Register 2 - Login");
            commnSocket.sendString(message.toString());
            String clientInput = commnSocket.receiveString();
            boolean success = false;
            while (!success && socket.isConnected()) {
                if (clientInput.equals("1")) {
                    commnSocket.sendString("Enter your username: ");
                    String userName = commnSocket.receiveString();
                    commnSocket.sendString("Enter your password: ");
                    String password = commnSocket.receiveString();
                    if (register(userName, password, socket)) {
                        commnSocket.sendString("Registration successful");
                        playerQueue.addPlayerToQueue(Server.db.findUserName(userName));
                        success = true;
                    } else {
                        commnSocket.sendString("Registration failed");
                    }
                } else if (clientInput.equals("2")) {
                    commnSocket.sendString("Enter your username: ");
                    String userName = commnSocket.receiveString();
                    commnSocket.sendString("Enter your password: ");
                    String password = commnSocket.receiveString();
                    if (login(userName, password, socket)) {
                        commnSocket.sendString("Login successful");
                        playerQueue.addPlayerToQueue(Server.db.findUserName(userName));
                        success = true;
                    } else {
                        commnSocket.sendString("Login failed");
                    }
                } else {
                    commnSocket.sendString("Invalid option");
                }
                commnSocket.sendString(message.toString());
                clientInput = commnSocket.receiveString();
            }
        
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
    }


    public boolean register(String userName, String password, Socket socket) {
        
        boolean success = false;
        
        lockPlayer.lock();
        Database.RegisterStatus status = Server.db.register(userName, password, socket);
        switch (status) {
            case SUCCESS:
                System.out.println("Registration successful");
                success = true;
                break;
            case USERNAME_TAKEN:
                System.out.println("Username already taken");
                break;
            case INVALID_USERNAME:
                System.out.println("Invalid username: must have between 4 and 15 characters");
                break;
            case INVALID_PASSWORD:
                System.out.println("Invalid password: must have at least 4 characters");
                break;
        }  
        lockPlayer.unlock();
        return success;
    }

    public boolean login(String userName, String password, Socket socket) {
        
        boolean success = false;
        
        lockPlayer.lock();
        Database.LoginStatus status = Server.db.login(userName, password, socket);
        switch (status) {
            case SUCCESS:
                System.out.println("Login successful");
                success = true;
                break;
            case INVALID_USERNAME:
                System.out.println("Username not found");
                break;
            case INVALID_PASSWORD:
                System.out.println("Wrong password for user");
                break;
            case ALREADY_LOGGED_IN:
                System.out.println("User already logged in");
                break;
        }
        lockPlayer.unlock();
        return success;
    }   

    public boolean login(String token, Socket socket) {
        
        boolean success = false;        
        
        lockPlayer.lock();
        Database.LoginStatus status = Server.db.login(token, socket);
        switch (status) {
            case SUCCESS:
                System.out.println("Login successful");
                success = true;
                break;
            case ALREADY_LOGGED_IN:
                System.out.println("User already logged in");
                break;
            case INVALID_TOKEN:
                System.out.println("Invalid token");
                break;
            case EXPIRED_TOKEN:
                System.out.println("Token expired");
                break;
        }
        lockPlayer.unlock();
        return success;
    }

    
    
}
