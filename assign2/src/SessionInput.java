import java.io.IOException;
import java.util.Scanner;

class SessionInput implements Runnable {
    private final Scanner scanner;

    public SessionInput(Scanner nativeScanner) {
        this.scanner = nativeScanner;
    }

    public String readLine() {
        return scanner.nextLine();
    }

    public int readInt() {
        return scanner.nextInt();
    }

    public void close() {
        scanner.close();
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (System.in.available() > 0) {
                    System.out.println("Enter a command: ");
                    String command = readLine();
                    System.out.println("Command entered: " + command);
                    if (command.equals("exit")) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
