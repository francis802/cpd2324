import java.io.*;
import java.net.*;
import java.util.Date;

public class Server {
    public static final String HOSTNAME = "localhost";
    public static final int PORT = 8080;

    public static void main(String[] args) {
        if (args.length > 0) {
            System.out.println("Usage: java Server");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String time = reader.readLine();

                System.out.println("New client connected: "+ time);

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                writer.println(new Date().toString());
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}