import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;








public class Auth extends Thread{

    private final ReentrantLock lockPlayer;
    private final PlayerQueue playerQueue;
    private final ServerSocket serverSocket;

    public Auth(ReentrantLock lockPlayer, PlayerQueue playerQueue, ServerSocket serverSocket) {
        this.lockPlayer = lockPlayer;
        this.playerQueue = playerQueue;
        this.serverSocket = serverSocket;
    }


    // @Override
    // public void run() {

    //     while (true) {
    //         try {
    //             Socket socket = serverSocket.accept();
    //             executor.submit(() -> handleClient(socket));
    //         } catch (IOException e) {
    //             e.printStackTrace();
    //         }
    //     }
    // }


    public boolean register(String userName, String password) {
        
        boolean success = false;
        
        lockPlayer.lock();
        Database.RegisterStatus status = Server.db.register(userName, password);
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

    public boolean login(String userName, String password) {
        
        boolean success = false;
        
        lockPlayer.lock();
        Database.LoginStatus status = Server.db.login(userName, password);
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

    public boolean login(String token) {
        
        boolean success = false;        
        
        lockPlayer.lock();
        Database.LoginStatus status = Server.db.login(token);
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
