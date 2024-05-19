import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class CommnSocket {
    private final Socket socket;
    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;

    /**
     * @param socket
     * @throws IOException
     */
    public CommnSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.dataInputStream = new DataInputStream(socket.getInputStream());
        this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
    }

    public void sendString(String message) throws IOException {
        dataOutputStream.writeUTF(message);
    }

    public String receiveString() throws IOException {
        return dataInputStream.readUTF();
    }

    public void sendInt(int number) throws IOException {
        dataOutputStream.writeInt(number);
    }

    public int receiveInt() throws IOException {
        return dataInputStream.readInt();
    }

    public void close() throws IOException {
        dataInputStream.close();
        dataOutputStream.close();
        socket.close();
    }
}
