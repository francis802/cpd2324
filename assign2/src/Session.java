import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * This program demonstrates a simple TCP/IP socket client.
 *
 * @author www.codejava.net
 */
public class Session {
    public static final String HOSTNAME = "localhost";
    public static final int PORT = 8080;
    
    public static void main(String[] args) {
        if (args.length > 0) {
            System.out.println("Usage: java Session");
            return;
        }

        // try (Socket socket = new Socket(HOSTNAME, PORT)) {

        //     OutputStream output = socket.getOutputStream();
        //     PrintWriter writer = new PrintWriter(output, true);
        //     writer.println("new Date()?".toString());

        //     InputStream input = socket.getInputStream();
        //     BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        //     String time = reader.readLine();

        //     System.out.println(time);


        // } catch (UnknownHostException ex) {

        //     System.out.println("Server not found: " + ex.getMessage());

        // } catch (IOException ex) {

        //     System.out.println("I/O error: " + ex.getMessage());
        // }
        List<String> credentials = askAuth();
        System.out.println("Username: " + credentials.get(0) + " Password: " + credentials.get(1));
    }

    


    public static List<String> askAuth() {
        try {
            String username, password;
            System.out.println("Enter your username: ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            username = reader.readLine();
            System.out.println("Enter your password: ");
            password = reader.readLine();
            
            return Arrays.asList(username, password);
        } catch (IOException e) {
            e.printStackTrace();
            return Arrays.asList();
        }
    }

}