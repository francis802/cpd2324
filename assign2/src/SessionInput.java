import java.io.IOException;
import java.util.Scanner;

class SessionInput {
    private final Scanner scanner;

    public SessionInput(Scanner nativeScanner) {
        this.scanner = nativeScanner;
    }

    public String putLine() {
        return scanner.nextLine();
    }

    public int putInt() {
        return scanner.nextInt();
    }

    public void close() {
        scanner.close();
    }

}
