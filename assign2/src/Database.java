
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.util.*;





public class Database {
    
    private final String PLAYER_DATA = "../data/players.csv";
    private final String TOKEN_DATA = "../data/tokens.csv";
    private static final int DEFAULT_ELO = 250;
    private static final long VALIDIY_TIME = 1000 * 60 * 60;
    private File playerFile;
    private File tokenFile;
    private static List<Player> players;
    private Map<String, Long> tokens;

    public Database() {
        this.playerFile = new File(PLAYER_DATA);

        if (!playerFile.exists()) {
            createPlayerFile();
        }

        this.players = new ArrayList<Player>();
        this.tokens = new HashMap<String, Long>();

        try {
            Scanner scanner = new Scanner(playerFile);
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

        this.tokenFile = new File(TOKEN_DATA);

        if (!tokenFile.exists()) {
            createTokenFile();
        }

       
    }

    private void createTokenFile() {
        try { if (tokenFile.createNewFile()) {
                    try (FileWriter writer = new FileWriter(tokenFile)) {
                        writer.write("username,token\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void createPlayerFile() {
        try {
                if (playerFile.createNewFile()) {
                    try (FileWriter writer = new FileWriter(playerFile)) {
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
        EXPIRED_TOKEN,
        ALREADY_LOGGED_IN
    }   

    public RegisterStatus register(String userName, String password, Socket socket) {

        if (userName.length() < 3 || userName.length() > 15) {
            return RegisterStatus.INVALID_USERNAME;
        }

        if (password.length() < 4) {
            return RegisterStatus.INVALID_PASSWORD;
        }

        if (findUserName(userName) != null) {
            return RegisterStatus.USERNAME_TAKEN;
        }

        try (FileWriter writer = new FileWriter(playerFile, true)) {
            writer.write(userName + "," + password + "," + DEFAULT_ELO + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Player player = new Player(userName, password, DEFAULT_ELO);

        player.setSocket(socket);
        createToken(player);
        player.login();
        players.add(player);
        
        return RegisterStatus.SUCCESS;
    }

    public LoginStatus login(String userName, String password, Socket socket) {
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

        player.setSocket(socket);
        createToken(player);
        player.login();

        return LoginStatus.SUCCESS;
    }

    public LoginStatus login(String token, Socket socket) {
        Player player = findToken(token);

        if (player == null) {
            return LoginStatus.INVALID_TOKEN;
        }

        if (player.isLoggedIn()) {
            return LoginStatus.ALREADY_LOGGED_IN;
        }

        if (!validToken(token)) {
            return LoginStatus.EXPIRED_TOKEN;
        }

        player.setSocket(socket);
        player.login();

        return LoginStatus.SUCCESS;
    }


    public void logout(String userName) {
        Player player = findUserName(userName);
        player.logout();
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

        try (FileWriter writer = new FileWriter(tokenFile, true)) {
            writer.write(player.getUserName() + "," + token + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

        tokens.put(token, timestamp + VALIDIY_TIME);
        player.setToken(token);

    }

    public boolean validToken(String token) {

        if (tokens.containsKey(token)) {
            long timestamp = tokens.get(token);
            if (timestamp > System.currentTimeMillis()) {
                return true;
            }
        }

        updateTokenFile(token);
        return false;
    }

    public void updateTokenFile(String token) {
        try {
            File tempFile = new File("temp.csv");
            FileWriter writer = new FileWriter(tempFile);
            Scanner scanner = new Scanner(tokenFile);
            writer.write(scanner.nextLine() + "\n");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] data = line.split(",");
                if (data[1].equals(token)) {
                    continue;
                }
                writer.write(line + "\n");
            }
            writer.close();
            scanner.close();
            tokenFile.delete();
            tempFile.renameTo(tokenFile);
            this.tokenFile = tempFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    public List<Player> getPlayers() {
        return players;
    }
    
    
    // public static void main(String[] args) {
    //         if (args.length > 0) {
    //             System.out.println("Usage: java Database");
    //             return;
    //         }

    //         Database db = new Database();

            
    //         db.register("user1", "password1");
    //         db.register("user2", "password2");
    //         db.register("user3", "password3");

            
    //         for (Player player : players) {
    //             System.out.println(player.getUserName() + " " + player.getPassword() + " " + player.getElo() + " " + player.getToken() + " " + player.isLoggedIn());    
    //         }

    //         var a = db.validToken(players.get(0).getToken());  

    //         for(Map.Entry<String, Long> entry : db.tokens.entrySet()) {
    //             System.out.println(entry.getKey() + " " + entry.getValue());
    //         }

    // }
}