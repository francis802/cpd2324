import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;


public class Game implements Runnable {

    private List<Player> players;
    private Map<String, Integer> playerNumbers = new HashMap<String, Integer>();
    private Map<String, Integer> playerWins = new HashMap<String, Integer>();
    private Map<String, CommnSocket> commSockets = new HashMap<String, CommnSocket>();
    private Map<String, Integer> itemPrices = new HashMap<String, Integer>();
    private boolean isRanked;
    

    private int chosenPrice = 0;
    private String chosenItem = "";
    

    private static final int MAX_WINS = 5;
    private static final int ELO_K = 32;
    private static final int CHECK_DISCONNECTIONS_INTERVAL = 5;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void run() {
        StringBuilder message = new StringBuilder();
        loadItemPrices();
        message.append("==========GAME STARTED==========\n");
        message.append("Players in game: \n");
        for (Player player : this.players) {
            message.append(player.getUserName() + "\n");
        }
        message.append("===============================");

        scheduler.scheduleAtFixedRate(this::checkDisconnections, 0, CHECK_DISCONNECTIONS_INTERVAL, TimeUnit.SECONDS);
        this.broadcastMessage(message.toString());
        while (true) {
            play();
            if (isGameOver()) {
                Server.db.updateFileElos();
                broadcastMessage("[input]Do you want to play again? (yes/no)");
                for (Player player : this.players) {
                    String input = this.getInputFromPlayer(player.getUserName());
                    if (input.equals("yes")) PlayerQueue.playerQueue.add(player);
                    else{
                        broadcastMessage("Game over! Thanks for playing!");
                    }
                }
                break;
            }
        }
        scheduler.shutdown();
    }

    public Game(List<Player> players, boolean isRanked) throws IOException {
        this.isRanked = isRanked;
        this.players = players;
        this.players.sort((p1, p2) -> p1.getElo() - p2.getElo());
        this.commSockets = new HashMap<String, CommnSocket>();
        for (Player player : this.players) {
            this.commSockets.put(player.getUserName(), new CommnSocket(player.getSocket()));
            this.playerWins.put(player.getUserName(), 0);
            this.playerNumbers.put(player.getUserName(), 0);
        }
    }

    public void play() {
        int randomInt = new Random().nextInt(itemPrices.size());
        this.chosenItem = (String) itemPrices.keySet().toArray()[randomInt];
        this.chosenPrice = itemPrices.get(chosenItem);
        broadcastMessage("[input]How much do you think a " + chosenItem + " costs?");

        for (Player player : this.players) {
            this.playerNumbers.replace(player.getUserName(), 0);
            String input = this.getInputFromPlayer(player.getUserName());
            if(input != null) {
                try {
                    int number = Integer.parseInt(input);
                    if(number >= 1 && number <= 100000) {
                        this.playerNumbers.replace(player.getUserName(), number);
                    } else {
                        this.sendMessage("Invalid number", player.getUserName());
                        this.playerNumbers.replace(player.getUserName(), 0);
                    }
                } 
                catch (Exception e) {
                    this.sendMessage("Invalid input", player.getUserName());
                    this.playerNumbers.replace(player.getUserName(), 0);
                }
            }
            else {
                this.sendMessage("Invalid input", player.getUserName());
                this.playerNumbers.replace(player.getUserName(), 0);
            }
        }
        this.updatePlayerWins();
    }

    public void sendMessage(String message, String userName) {
        try {
            commSockets.get(userName).sendString(message);
        } catch (IOException e) {   
            e.printStackTrace();
        }
    }

    public String getInputFromPlayer(String userName) {
        try {
            return commSockets.get(userName).receiveString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    
    public boolean broadcastMessage(String message) {
        try {
            for (Player player : this.players) {
                this.sendMessage(message, player.getUserName());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private float calculateChampionProbability(Player player) {
        float sumElo = this.players.stream().mapToInt(Player::getElo).sum();
        return player.getElo() / sumElo;
    }

    private int calculateEloChampion(Player player) {
        return (int) (ELO_K * (1 - this.calculateChampionProbability(player)));
    }

    private int calculateEloLoser(Player player) {
        return (int) (ELO_K * this.calculateChampionProbability(player));
    }

    private void gameOver() {
        List<Player> champions = new ArrayList<Player>();

        for (Player player : this.players) {
            if (this.playerWins.get(player.getUserName()) == MAX_WINS) {
                champions.add(player);
                this.broadcastMessage("Player " + player.getUserName() + " WON THE GAME!\n\n");
            }
        }
        if(isRanked) updateElos(champions);
        
    }

    private void updateElos(List<Player> champions){

        if(champions.size() == players.size()){
            broadcastMessage("All players won, no elo changes");
            return;
        }

        for(Player player : players){
            
            if(champions.contains(player)){
                int elo = calculateEloChampion(player)/champions.size();
                player.updateElo(elo);
                sendMessage("You won "+ elo + " elo!", player.getUserName());
            }
            else{

                int lostElo = calculateEloLoser(player);
                if(lostElo == 0){
                    sendMessage("Your elo wasn't affected by the loss", player.getUserName()); 
                    return;
                }
                sendMessage("You lost "+ lostElo + " elo!", player.getUserName());
                player.updateElo(-lostElo);

            }
        }       
    }

    private void updatePlayerWins() {
        int winningNumber = this.closestNumber(chosenPrice);
        StringBuilder message = new StringBuilder();
        message.append("=================WINS TABLE=================\n");
        message.append("A " + chosenItem + " costs " + chosenPrice + "!\n");
    
        for (var pair : this.playerNumbers.entrySet()) {
            if (pair.getValue() == winningNumber) {
                this.playerWins.put(pair.getKey(), this.playerWins.get(pair.getKey()) + 1);
                message.append("WON - Player " + pair.getKey() + " played " + pair.getValue() + " and has now " + this.playerWins.get(pair.getKey()) + " wins\n");
            }
            else message.append("LOST - Player " + pair.getKey() + " played " + pair.getValue() + " and has " + this.playerWins.get(pair.getKey()) + " wins\n");
        }
    
        message.append("============================================");
        this.broadcastMessage(message.toString());
    }
    

    private int closestNumber(int result) {
        int closest = 0;
        float minDifference = Integer.MAX_VALUE;
        for (float number : this.playerNumbers.values()) {
            float difference = Math.abs(number - result);
            if (difference < minDifference && number <= result) {
                minDifference = difference;
                closest = (int) number;
            }
        }
        return closest;
    }

    public boolean isGameOver() {
        for (var pair : this.playerWins.entrySet()) {
            if (pair.getValue() == MAX_WINS) {
                gameOver();
                return true;
            }
        }
        return false;
    }

    // Dont know if this is well implemented
    public void checkDisconnections() {
        for (Player player : this.players) {
            if (!player.isLoggedIn()) {
                StringBuilder immediateMessage = new StringBuilder();
                immediateMessage.append("Player " + player.getUserName() + " disconnected.\n");
                this.broadcastMessage(immediateMessage.toString());
            }
        }
    }


    private void loadItemPrices() {
    
       
        itemPrices.put("TV Samsung", 599);
        itemPrices.put("Apple iPhone 13", 999);
        itemPrices.put("HP Laptop", 750);
        itemPrices.put("Sony PlayStation 5", 499);
        itemPrices.put("Bose Headphones", 299);
        itemPrices.put("Nike Air Max", 150);
        itemPrices.put("Adidas Running Shoes", 130);
        itemPrices.put("Canon DSLR Camera", 850);
        itemPrices.put("Dell Monitor", 220);
        itemPrices.put("KitchenAid Mixer", 380);
        itemPrices.put("Nespresso Coffee Machine", 200);
        itemPrices.put("IKEA Sofa", 450);
        itemPrices.put("Samsung Refrigerator", 1200);
        itemPrices.put("Bosch Washing Machine", 650);
        itemPrices.put("Dyson Vacuum Cleaner", 550);
        itemPrices.put("Garmin Smartwatch", 400);
        itemPrices.put("GoPro Hero Camera", 350);
        itemPrices.put("Samsung Galaxy Tab", 450);
        itemPrices.put("Xbox Series X", 499);
        itemPrices.put("Apple MacBook Pro", 1400);
        itemPrices.put("LG OLED TV", 1500);
        itemPrices.put("Philips Air Fryer", 180);
        itemPrices.put("Sony Bluetooth Speaker", 120);
        itemPrices.put("Razer Gaming Mouse", 70);
        itemPrices.put("Apple Watch", 399);
        itemPrices.put("Samsung Galaxy S21", 799);
        itemPrices.put("Lenovo ThinkPad", 1100);
        itemPrices.put("Bose Soundbar", 600);
        itemPrices.put("Microsoft Surface Pro", 900);
        itemPrices.put("Nintendo Switch", 299);
        itemPrices.put("Panasonic Microwave", 130);
        itemPrices.put("Fitbit Charge", 150);
        itemPrices.put("Asus Gaming Laptop", 1250);
        itemPrices.put("Samsung Sound System", 350);
        itemPrices.put("Philips Hue Lights", 250);
        itemPrices.put("Yamaha Keyboard", 300);
        itemPrices.put("Canon Printer", 100);
        itemPrices.put("Sony Mirrorless Camera", 1200);
        itemPrices.put("Harman Kardon Speaker", 400);
        itemPrices.put("Logitech Webcam", 90);
        itemPrices.put("Amazon Echo", 100);
        itemPrices.put("Google Nest Thermostat", 250);
        itemPrices.put("Apple AirPods Pro", 249);
        itemPrices.put("DJI Drone", 800);
        itemPrices.put("Roku Streaming Stick", 50);
        itemPrices.put("Instant Pot", 100);
        itemPrices.put("Nikon DSLR", 950);
        itemPrices.put("Samsung QLED TV", 1300);
        itemPrices.put("LG Washer Dryer", 1100);
        itemPrices.put("Bose QuietComfort Earbuds", 279);
        itemPrices.put("Sony 4K Camcorder", 1000);
        itemPrices.put("Apple iPad Pro", 999);
        itemPrices.put("Samsung Galaxy Watch", 350);
        itemPrices.put("Canon EOS Camera", 700);
        itemPrices.put("Sony PS5 Controller", 70);
        itemPrices.put("Logitech Gaming Keyboard", 150);
        itemPrices.put("Garmin GPS", 200);
        itemPrices.put("HP OfficeJet Printer", 180);
        itemPrices.put("Dell XPS Laptop", 1300);
        itemPrices.put("Bose Home Speaker", 300);
        itemPrices.put("Microsoft Xbox Controller", 60);
        itemPrices.put("Samsung Portable SSD", 150);
        itemPrices.put("Apple Magic Keyboard", 99);
        itemPrices.put("Sony Noise Cancelling Headphones", 400);
        itemPrices.put("Nikon Mirrorless Camera", 1100);
        itemPrices.put("Philips Sonicare Toothbrush", 120);
        itemPrices.put("Braun Electric Shaver", 180);
        itemPrices.put("Oral-B Toothbrush", 80);
        itemPrices.put("Sony Bravia TV", 2000);
        itemPrices.put("Asus ROG Laptop", 2000);
        itemPrices.put("Apple Mac Mini", 700);
        itemPrices.put("Google Pixel Phone", 600);
        itemPrices.put("LG Soundbar", 300);
        itemPrices.put("JBL Bluetooth Speaker", 150);
        itemPrices.put("Samsung Curved Monitor", 500);
        itemPrices.put("Apple TV", 180);
        itemPrices.put("Roku TV", 400);
        itemPrices.put("Philips Air Purifier", 200);
        itemPrices.put("Dyson Air Purifier", 600);
        itemPrices.put("Sony Portable Speaker", 100);
        itemPrices.put("Apple HomePod", 300);
        itemPrices.put("Samsung Galaxy Buds", 150);
        itemPrices.put("Nest Cam", 200);
        itemPrices.put("Amazon Kindle", 130);
        itemPrices.put("Sony Alpha Camera", 1500);
        itemPrices.put("Logitech Gaming Headset", 100);
        itemPrices.put("HP Envy Printer", 150);
        itemPrices.put("Lenovo Yoga Tablet", 450);
        itemPrices.put("Garmin Forerunner Watch", 300);
        itemPrices.put("Canon PowerShot Camera", 400);
        itemPrices.put("Sony WH-1000XM4", 350);
        itemPrices.put("Samsung Galaxy Note", 950);
        itemPrices.put("LG Gram Laptop", 1200);
        itemPrices.put("Apple iMac", 1800);
        itemPrices.put("Bose Frames", 200);
        itemPrices.put("Sony Xperia Phone", 900);
        itemPrices.put("Oculus Quest 2", 299);
        itemPrices.put("Nintendo 3DS", 199);
        itemPrices.put("Apple Pencil", 129);
        itemPrices.put("Google Chromecast", 50);
        itemPrices.put("Huawei MateBook", 1100);
        itemPrices.put("OnePlus 9 Pro", 969);
        itemPrices.put("Sony A7 III", 2000);
        itemPrices.put("Surface Headphones", 249);
        itemPrices.put("Samsung Odyssey Monitor", 699);
        itemPrices.put("Beats Solo Pro", 299);
        itemPrices.put("DJI Osmo Pocket", 349);
        itemPrices.put("Fitbit Versa", 230);
        itemPrices.put("Logitech G Pro Mouse", 130);
        itemPrices.put("Netgear Nighthawk Router", 200);
        itemPrices.put("Ring Doorbell", 199);
        itemPrices.put("Samsung Gear VR", 129);
        itemPrices.put("Tile Pro", 35);
        itemPrices.put("Arlo Pro 3", 500);
        itemPrices.put("Wyze Cam", 30);
        itemPrices.put("Razer Blade Laptop", 2000);
        itemPrices.put("Corsair K95 Keyboard", 199);
        itemPrices.put("HyperX Cloud II Headset", 99);
        itemPrices.put("TP-Link Smart Plug", 25);
        itemPrices.put("Anker PowerCore Battery", 50);
        itemPrices.put("Blue Yeti Microphone", 130);
        itemPrices.put("SteelSeries Arctis Pro", 250);
        itemPrices.put("Elgato Stream Deck", 150);
        itemPrices.put("Roku Ultra", 100);
        itemPrices.put("Sonos One Speaker", 200);
        itemPrices.put("Nest Learning Thermostat", 249);
        itemPrices.put("Logitech MX Master 3", 99);
        itemPrices.put("Alienware Gaming Desktop", 2500);
    }


}