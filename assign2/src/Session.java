import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executor;
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
    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
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
            executor.submit(sessionInput);
            Socket socket = new Socket(HOSTNAME, PORT);
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            List<String> credentials = askAuth();
            //writer.println("new Date()?".toString());
            //writer.println("Username: ".toString());

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String line = reader.readLine();
            System.out.println(line);
            line = reader.readLine();
            System.out.println(line);
            line = reader.readLine();
            System.out.println(line);
            while (true) {
                String a = this.sessionInput.readLine();
                writer.println(a);
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
        username = this.sessionInput.readLine();
        System.out.println("Enter your password: ");
        password = this.sessionInput.readLine();
        return Arrays.asList(username, password);
    }

}