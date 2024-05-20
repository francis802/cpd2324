import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This program demonstrates a simple TCP/IP socket client.
 *
 * @author www.codejava.net
 */
public class Session {
    public static final String HOSTNAME = "localhost";
    public static final int PORT = 8080;
    private final int sessionId;
    private SessionInput sessionInput;
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Session <Session Id>");
            return;
        }

        int sessionId = Integer.parseInt(args[0]);
        Session session = new Session(sessionId);
        session.createTokenFile();
        session.run();

    }

    public Session(int sessionId) {
        this.sessionId = sessionId;
    }

    public void createTokenFile() {
        try {
            File tokenFile = new File("../data/tokens/session" + sessionId + ".txt");
            if (tokenFile.createNewFile()) {
                System.out.println("Token file created: " + tokenFile.getName());
            } else {
                System.out.println("Token file already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


    public void run(){
        try  {
            this.sessionInput = new SessionInput(new Scanner(System.in));
            Socket socket = new Socket(HOSTNAME, PORT);
            CommnSocket commnSocket = new CommnSocket(socket);

            String line;
            boolean input = false;
            while (true) {
                input = false;
                line = commnSocket.receiveString();
                if (line.contains("[input]")) {
                    line = line.substring(7);
                    input = true;
                }
                System.out.println(line);
                if (input){
                    String a = this.sessionInput.putLine();
                    commnSocket.sendString(a);
                }
            }



        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }
    


    public List<String> askAuth() {
        String username, password;
        System.out.println("Enter your username: ");
        username = this.sessionInput.putLine();
        System.out.println("Enter your password: ");
        password = this.sessionInput.putLine();
        return Arrays.asList(username, password);
    }

}