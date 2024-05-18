import java.net.*;
import java.io.*;

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

    public void logIn() {
        this.loggedIn = true;
    }

    public void logOut() {
        this.loggedIn = false;
    }

    public void updateElo(int gameResult) {
        this.elo += gameResult;
    }
}