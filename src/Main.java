import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws IOException {
        try {
            int portNumber = 40450;
            ServerSocket serverSocket = new ServerSocket(portNumber);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                DataServer dataserver = new DataServer(clientSocket);
                dataserver.start();
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}