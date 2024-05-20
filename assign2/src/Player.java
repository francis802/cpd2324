import java.net.*;

/**
 * This program demonstrates a simple TCP/IP socket client.
 *
 * @author www.codejava.net
 */
public class Player {
   
    private final String userName;
    private final String password;
    private int elo;
    private String token;
    private boolean loggedIn;
    private long disconnectTime;
    private Socket socket;
    private int sessionId;

    public Player(String userName, String password, int elo) {
        this.userName = userName;
        this.password = password;
        this.elo = elo; 
        this.token = "";
        this.loggedIn = false;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public int getElo() {
        return elo;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void login() {
        this.loggedIn = true;
    }

    public void logout() {
        this.loggedIn = false;
    }

    public void updateElo(int gameResult) {
        this.elo += gameResult;
    }

    public void setDisconnectTime(long disconnectTime) {
        this.disconnectTime = disconnectTime;
    }

    public long getDisconnectTime() {
        return disconnectTime;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}