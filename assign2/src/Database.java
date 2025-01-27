
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.util.*;





public class Database {
    
    private final String PLAYER_DATA = "../data/players.csv";
    private final String SESSION_DATA = "../data/tokens/";
    private static final int DEFAULT_ELO = 250;
    private static final long VALIDIY_TIME = 1000 * 30;
    private File playerFile;
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
    }

    public void refreshToken(String token) {
        tokens.put(token, System.currentTimeMillis() + VALIDIY_TIME);
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

    public long getTokenExpiration(String token) {
        return tokens.get(token);
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

    public RegisterStatus register(String userName, String password, Socket socket, int sessionId) {

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
        addTokenToSession(player, sessionId);
        
        return RegisterStatus.SUCCESS;
    }

    public LoginStatus login(String userName, String password, Socket socket, int sessionId) {
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
        addTokenToSession(player, sessionId);

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

    public void addTokenToSession(Player player, int sessionId) {
        try (FileWriter writer = new FileWriter(SESSION_DATA + "session" + sessionId + ".txt", false)) {
            writer.write(player.getUserName() + "," + player.getToken() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        return false;
    }

    public void updateFileElos() {
        
        try {
            this.playerFile.delete();
            this.playerFile.createNewFile();
            FileWriter writer = new FileWriter(playerFile);
            writer.write("username,password,elo\n");
            for (Player player : players) {
                writer.write(player.getUserName() + "," + player.getPassword() + "," + player.getElo() + "\n");
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
            
    }
    

    public List<Player> getPlayers() {
        return players;
    }

    public String rememberedSession(int sessionId, Socket socket) {
        try {
            Scanner scanner = new Scanner(new File(SESSION_DATA + "session" + sessionId + ".txt"));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] data = line.split(",");
                String token = data[1];
                return token;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}