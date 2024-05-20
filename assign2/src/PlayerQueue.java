import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class PlayerQueue implements Runnable {
    private final ReentrantLock queueLock;
    private final int gameMode;
    protected static Set<Player> playerQueue; 
    private static Map<Player, Long> playerJoinedAt;

    private int maxEloDifference;
    private long lookingForRankedTime;


    private int MAX_ELO_DIFFERENCE = 100;
    private int INCREMENT_ELO_DIFFERENCE = 50;
    private int DIFFERENCE_UPDATE_INTERVAL = 1000*5;
    private long TIMEOUT = 1000 * 60; 


    public PlayerQueue(ReentrantLock queueLock, int gameMode) {
        this.queueLock = queueLock;
        this.gameMode = gameMode;
        this.playerQueue = new HashSet<Player>();
        this.playerJoinedAt = new HashMap<Player, Long>();
        this.maxEloDifference = MAX_ELO_DIFFERENCE;
        this.lookingForRankedTime = System.currentTimeMillis();
        
    }

    @Override
    public void run() {
        while (true) {
            queueLock.lock();
            try {
                Iterator<Player> iterator = playerQueue.iterator();
                while (iterator.hasNext()) {
                    Player player = iterator.next();
                    if (isPlayerDisconnected(player)) {
                        System.out.println("Player " + player.getUserName() + " disconnected");
                        player.logout();
                    }
                    else {
                        refreshPlayer(player);
                    }
                }
            } finally {
                queueLock.unlock();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isPlayerDisconnected(Player player) {
        try {
            player.getSocket().sendUrgentData(0); // Send a 1-byte urgent data packet
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    // Don't know if this is well implemented
    public boolean addPlayerToQueue(Player player) {    
        queueLock.lock();
        if(player.isLoggedIn()){
            playerQueue.add(player);
            playerJoinedAt.put(player, System.currentTimeMillis());
            queueLock.unlock();
            return true;
        } else {
            queueLock.unlock();
            return false;
        }
    }

    public boolean removePlayerFromQueue(Player player) {
        queueLock.lock();
        if(playerQueue.contains(player)){
            playerQueue.remove(player);
            playerJoinedAt.remove(player);
            queueLock.unlock();
            return true;
        } else {
            queueLock.unlock();
            return false;
        }
    }

    public void timeoutPlayers() {
        queueLock.lock();
        for(Player player : playerQueue){
            if(System.currentTimeMillis() - Server.db.getTokenExpiration(player.getToken()) > TIMEOUT){
                System.out.println("Player " + player.getUserName() + " timed out");
                player.logout();
                removePlayerFromQueue(player);
            }
        }
        queueLock.unlock();
    }

    public void refreshPlayer(Player player) {
        Server.db.refreshToken(player.getToken());
    }


    public ArrayList<Player> getPlayersSimple(int playersInGame) {
        if (playerQueue.size() < playersInGame) {
            return null;
        }
        
        ArrayList<Player> selectedPlayers = new ArrayList<Player>();
        
        queueLock.lock();
        ArrayList<Player> sortedPlayers = playerQueue.stream()
            .filter(Player::isLoggedIn)
            .sorted(Comparator.comparing(playerJoinedAt::get))
            .collect(Collectors.toCollection(ArrayList::new));
        queueLock.unlock();

        queueLock.lock();
        for(int i = 0; i < playersInGame; i++){
            selectedPlayers.add(sortedPlayers.get(i));
            removePlayerFromQueue(sortedPlayers.get(i));
        }
        queueLock.unlock();

        return selectedPlayers;
    } 

    public ArrayList<Player> getPlayersRanked(int playersInGame) {

        

        queueLock.lock();
        ArrayList<Player> sortedPlayers = playerQueue.stream()
            .filter(Player::isLoggedIn) 
            .sorted(Comparator.comparing(Player::getElo)
                            .thenComparing(playerJoinedAt::get))
            .collect(Collectors.toCollection(ArrayList::new));
        queueLock.unlock();

        while (!sortedPlayers.isEmpty()){
            ArrayList<Player> selectedPlayers = new ArrayList<>();
            if(sortedPlayers.size() < playersInGame){
                break;
            }
            for(int i = 0; i < playersInGame; i++){
                selectedPlayers.add(sortedPlayers.get(i));
            }

            if(selectedPlayers.get(selectedPlayers.size() - 1).getElo() - selectedPlayers.get(0).getElo() > this.maxEloDifference){
                sortedPlayers.remove(0);
            }
            else {
                this.lookingForRankedTime = System.currentTimeMillis();
                this.maxEloDifference = MAX_ELO_DIFFERENCE;
                queueLock.lock();
                for(Player player:selectedPlayers){
                    removePlayerFromQueue(player);
                }
                queueLock.unlock();
            return selectedPlayers;
            }
            
        }
        if(System.currentTimeMillis() - this.lookingForRankedTime > DIFFERENCE_UPDATE_INTERVAL){
            this.maxEloDifference += INCREMENT_ELO_DIFFERENCE;
        }

        return null;
    }



}