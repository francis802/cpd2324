import java.util.ArrayList;
import java.util.Set;
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
    private int DIFFERENCE_UPDATE_INTERVAL = 1000*60;
    private long TIMEOUT = 1000 * 60 * 5; 


    public PlayerQueue(ReentrantLock queueLock, int gameMode) { //TODO: Did not put lockDB as parameter
        this.queueLock = queueLock;
        this.gameMode = gameMode;
        this.playerQueue = new HashSet<Player>();
        this.playerJoinedAt = new HashMap<Player, Long>();
        this.maxEloDifference = MAX_ELO_DIFFERENCE;
    }

    @Override
    public void run() {
        while (true) {
            // Example task that could be offloaded to a virtual thread
            //this.processQueue();
            try {
                Thread.sleep(1000); // Sleep to simulate periodic processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processQueue() {
        queueLock.lock();
        try {
            // Process the player queue
            System.out.println("Processing player queue in game mode: " + gameMode);
            // Add your queue processing logic here
        } finally {
            queueLock.unlock();
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
            if(System.currentTimeMillis() - playerJoinedAt.get(player) > TIMEOUT){
                player.logout();
                removePlayerFromQueue(player);
            }
        }
        queueLock.unlock();
    }


    public ArrayList<Player> getPlayersSimple(int playersInGame) {
        
        ArrayList<Player> selectedPlayers = new ArrayList<Player>();
        
        queueLock.lock();
        ArrayList<Player> sortedPlayers = playerQueue.stream()
            .filter(Player::isLoggedIn)
            .sorted(Comparator.comparing(playerJoinedAt::get))
            .collect(Collectors.toCollection(ArrayList::new));
        queueLock.unlock();

        for(Player player : sortedPlayers){           

            selectedPlayers.add(player);
            removePlayerFromQueue(player);

            if(selectedPlayers.size() == playersInGame){
                break;
            }
            
        }
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
            if(selectedPlayers.get(0).getElo() - selectedPlayers.get(selectedPlayers.size() - 1).getElo() > this.maxEloDifference){
                sortedPlayers.remove(0);
                continue;
            }

            this.lookingForRankedTime = System.currentTimeMillis();
            this.maxEloDifference = MAX_ELO_DIFFERENCE;
            queueLock.lock();
            for(Player player:selectedPlayers){
                removePlayerFromQueue(player);
            }
            queueLock.unlock();
            return selectedPlayers;
            
        }
        if(System.currentTimeMillis() >= this.lookingForRankedTime + DIFFERENCE_UPDATE_INTERVAL){
            this.maxEloDifference += INCREMENT_ELO_DIFFERENCE;
        }

        return null;
    }



}