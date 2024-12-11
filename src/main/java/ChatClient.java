import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    private final String serverAddress;
    private final int chatPort;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private MessageListener messageListener;

    public ChatClient(String serverAddress, int chatPort) {
        this.serverAddress = serverAddress;
        this.chatPort = chatPort;
    }

    public void connect() throws IOException {
        socket = new Socket(serverAddress, chatPort);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Start a thread to listen for incoming messages
        new Thread(this::listenForMessages).start();
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    private void listenForMessages() {
        try {
            String incomingMessage;
            while ((incomingMessage = reader.readLine()) != null) {
                if (messageListener != null) {
                    messageListener.onMessageReceived(incomingMessage);
                }
            }
        } catch (IOException e) {
            System.err.println("Error receiving messages: " + e.getMessage());
        }
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public interface MessageListener {
        void onMessageReceived(String message);
    }
}

