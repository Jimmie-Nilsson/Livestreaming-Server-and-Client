import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
/**
 * A client class for handling chat communication with a server.
 * It supports connecting to a server, sending messages, receiving messages,
 * and managing the connection lifecycle.
 *
 * @author Jimmie Nilsson jini6619
 */
public class ChatClient {
    private final String serverAddress;
    private final int chatPort;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private MessageListener messageListener;
    /**
     * Constructs a ChatClient instance.
     *
     * @param serverAddress The address of the chat server.
     * @param chatPort      The port of the chat server.
     */
    public ChatClient(String serverAddress, int chatPort) {
        this.serverAddress = serverAddress;
        this.chatPort = chatPort;
    }

    /**
     * Connects to the chat server and starts a thread to listen for incoming messages.
     *
     * @throws IOException If an I/O error occurs while connecting to the server.
     */
    public void connect() throws IOException {
        socket = new Socket(serverAddress, chatPort);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Start a thread to listen for incoming messages
        new Thread(this::listenForMessages).start();
    }

    /**
     * Sends a message to the chat server.
     *
     * @param message The message to send.
     */
    public void sendMessage(String message) {
        writer.println(message);
    }

    /**
     * Listens for incoming messages from the server in a separate thread.
     * Passes received messages to the registered message listener.
     */
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

    /**
     * Sets the message listener to handle incoming messages.
     *
     * @param listener The listener to handle received messages.
     */
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    /**
     * Disconnects from the server and releases all associated resources.
     */
    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * Interface for handling received messages.
     */
    public interface MessageListener {
        /**
         * Called when a new message is received.
         *
         * @param message The received message.
         */
        void onMessageReceived(String message);
    }
}

