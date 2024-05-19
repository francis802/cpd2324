import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class Auth implements Runnable {

    private final ReentrantLock lockPlayer;
    private final PlayerQueue playerQueue;
    private final ServerSocket serverSocket;

    public Auth(ReentrantLock lockPlayer, PlayerQueue playerQueue, ServerSocket serverSocket) {
        this.lockPlayer = lockPlayer;
        this.playerQueue = playerQueue;
        this.serverSocket = serverSocket;
    }


    @Override
    public void run() {

        while (true) {
            try  {
                Socket socket = serverSocket.accept();
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                StringBuilder message = new StringBuilder();
                message.append("Welcome to the game server\n");
                message.append("1 - Register\n");
                message.append("2 - Login\n");
                writer.println(message.toString());
                String clientInput = reader.readLine();
                while (clientInput != null) {
                    if (clientInput.equals("1")) {
                        writer.println("Enter your username: ");
                        while (!reader.ready()) {
                            Thread.sleep(100);
                        }
                        String userName = reader.readLine();
                        writer.println("Enter your password: ");
                        while (!reader.ready()) {
                            Thread.sleep(100);
                        }
                        String password = reader.readLine();
                        if (register(userName, password, socket)) {
                            writer.println("Registration successful");
                        } else {
                            writer.println("Registration failed");
                        }
                    } else if (clientInput.equals("2")) {
                        writer.println("Enter your username: ");
                        while (!reader.ready()) {
                            Thread.sleep(100);
                        }
                        String userName = reader.readLine();
                        writer.println("Enter your password: ");
                        while (!reader.ready()) {
                            Thread.sleep(100);
                        }
                        String password = reader.readLine();
                        if (login(userName, password, socket)) {
                            writer.println("Login successful");
                        } else {
                            writer.println("Login failed");
                        }
                    } else {
                        writer.println("Invalid option");
                    }
                    writer.println(message.toString());
                    while (!reader.ready()) {
                        Thread.sleep(100);
                    }
                    clientInput = reader.readLine();
                }
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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
