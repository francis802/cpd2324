
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;




public class Database {
    
    private final String path = "../data/data.csv";
    private static final int defaultElo = 250;
    private File file;
    private static List<Player> players;

    public Database() {
        this.file = new File(path);

        if (!file.exists()) {
            createFile();
        }

        this.players = new ArrayList<Player>();

        try {
            Scanner scanner = new Scanner(file);
            scanner.nextLine();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] data = line.split(",");
                Player player = new Player(data[0], data[1], Integer.parseInt(data[2]));
                players.add(player);
            }
            scanner.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }   
       
    }

    private void createFile() {
        try {
                if (file.createNewFile()) {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write("username,password,elo\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }   

    public Player findToken(String token) {
        for (Player player : players) {
            if (player.getToken().equals(token)) {
                return player;
            }
        }
        return null;
    }

    public Player findUserName(String userName) {
        for (Player player : players) {
            if (player.getUserName().equals(userName)) {
                return player;
            }
        }
        return null;
    }

    public enum RegisterStatus {
        SUCCESS,
        USERNAME_TAKEN,
        INVALID_USERNAME,
        INVALID_PASSWORD
    }

    public enum LoginStatus {
        SUCCESS,
        INVALID_USERNAME,
        INVALID_PASSWORD,
        INVALID_TOKEN,
        ALREADY_LOGGED_IN
    }   

    public RegisterStatus register(String userName, String password) {

        if (userName.length() < 3 || userName.length() > 15) {
            return RegisterStatus.INVALID_USERNAME;
        }

        if (password.length() < 4) {
            return RegisterStatus.INVALID_PASSWORD;
        }

        if (findUserName(userName) != null) {
            return RegisterStatus.USERNAME_TAKEN;
        }

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(userName + "," + password + "," + defaultElo + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Player player = new Player(userName, password, defaultElo);

        createToken(player);
        player.logIn();

        players.add(player);
        

        return RegisterStatus.SUCCESS;
    }

    public LoginStatus loginUserName(String userName, String password) {
        Player player = findUserName(userName);

        if (player == null) {
            return LoginStatus.INVALID_USERNAME;
        }

        if (!player.getPassword().equals(password)) {
            return LoginStatus.INVALID_PASSWORD;
        }

        if (player.isLoggedIn()) {
            return LoginStatus.ALREADY_LOGGED_IN;
        }

        createToken(player);
        player.logIn();

        return LoginStatus.SUCCESS;
    }

    public LoginStatus loginToken(String token) {
        Player player = findToken(token);

        if (player == null) {
            return LoginStatus.INVALID_USERNAME;
        }

        if (player.isLoggedIn()) {
            return LoginStatus.ALREADY_LOGGED_IN;
        }

        player.logIn();

        return LoginStatus.SUCCESS;
    }


    public void logout(String userName) {
        Player player = findUserName(userName);
        player.logOut();
    }

    public void createToken(Player player) {

        SecureRandom secureRandom = new SecureRandom();
        Base64.Encoder base64Encoder = Base64.getUrlEncoder();
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        
        String random = base64Encoder.encodeToString(randomBytes);
        String uuid = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        
        String token = random + "-" + uuid + "-" + timestamp;

        player.setToken(token);

    }
    
    
    public static void main(String[] args) {
            if (args.length > 0) {
                System.out.println("Usage: java Database");
                return;
            }

            Database db = new Database();

            
            db.loginUserName("test", "test");

            
            for (Player player : players) {
                System.out.println(player.getUserName() + " " + player.getPassword() + " " + player.getElo() + " " + player.getToken() + " " + player.isLoggedIn());    
            }

    }
}