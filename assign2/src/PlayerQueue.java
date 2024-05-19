import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerQueue implements Runnable {
    private final ReentrantLock queueLock;
    private final int gameMode;
    protected static Set<Player> playerQueue;

    public PlayerQueue(ReentrantLock lockPlayerQueue, int gameMode) {
        this.queueLock = lockPlayerQueue;
        this.gameMode = gameMode;
    }

    @Override
    public void run() {
        while (true) {
            // Example task that could be offloaded to a virtual thread
            this.processQueue();
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
            queueLock.unlock();
            return true;
        } else {
            queueLock.unlock();
            return false;
        }
    }
}