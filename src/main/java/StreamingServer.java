import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StreamingServer handles the streaming of video data from a streamer to multiple receivers
 * using a combination of TCP and UDP protocols. It also manages chat connections and broadcasts
 * chat messages to connected clients.
 *
 * @author Jimmie Nilsson jini6619
 */
public class StreamingServer {
    private final int streamerPort;
    private final int chatPort;
    private static final ConcurrentHashMap<InetSocketAddress, PrintWriter> clients = new ConcurrentHashMap<>();

    /**
     * Main entry point for the StreamingServer application.
     *
     * Initializes the server with the provided streamer and chat ports or uses default ports
     * if no arguments are provided.
     *
     * @param args Command-line arguments:
     *             args[0] - Streamer port (optional).
     *             args[1] - Chat port (optional).
     */
    public static void main(String[] args) {
        if (args.length == 2) {
            new StreamingServer(Integer.parseInt(args[0]), Integer.parseInt(args[1])).startServer();
        }else if (args.length == 0){
            new StreamingServer().startServer();
        }else {
            System.out.println("Usage: java StreamingServer <streamer port> <chat port>");
        }
    }
    /**
     * Constructs a StreamingServer with specified ports for streaming and chat connections.
     *
     * @param streamerPort Port for the video streamer to connect.
     * @param chatPort     Port for chat clients to connect.
     */
    public StreamingServer(int streamerPort, int chatPort) {
        this.streamerPort = streamerPort;
        this.chatPort = chatPort;
    }
    /**
     * Constructs a StreamingServer with default ports.
     * Streamer port: 8080, Chat port: 8082.
     */
    public StreamingServer() {
        streamerPort = 8080;
        chatPort = 8082;
    }
    /**
     * Starts the server to listen for streamer and chat client connections.
     */
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(streamerPort);
             ServerSocket chatSocket = new ServerSocket(chatPort);
             DatagramSocket udpSocket = new DatagramSocket()) {

            new Thread(() -> listenForChatConnections(chatSocket)).start();

            System.out.println("Server started. Waiting for connections...");
            while (true) {
                Socket streamerSocket = serverSocket.accept();
                System.out.println("Streamer connected: " + streamerSocket.getInetAddress());

                // Handle the streamer in a new thread
                new Thread(() -> handleStreamer(streamerSocket, udpSocket)).start();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
    /**
     * Handles the connection to a streamer and relays video data to connected clients.
     *
     * @param streamerSocket The socket used by the streamer to send data.
     * @param udpSocket      The UDP socket used to broadcast data to clients.
     */
    private void handleStreamer(Socket streamerSocket, DatagramSocket udpSocket) {
        byte[] buffer = new byte[8192]; // larger buffer to read data
        byte[] packetBuffer = new byte[1316]; // fixed size buffer for packets

        try (InputStream inputStream = streamerSocket.getInputStream()) {
            int bytesRead, offset = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    packetBuffer[offset++] = buffer[i];

                    // send packet when the buffer is full
                    if (offset == packetBuffer.length) {
                        sendPacket(packetBuffer, udpSocket);
                        offset = 0; // reset offset for the next packet
                    }
                }
            }

            // remaining bytes if any
            if (offset > 0) {
                byte[] lastPacket = new byte[offset];
                System.arraycopy(packetBuffer, 0, lastPacket, 0, offset);
                sendPacket(lastPacket, udpSocket);
            }
        } catch (IOException e) {
            System.err.println("Streamer disconnected: " + e.getMessage());
        } finally {
            notifyUsers();
        }
    }
    /**
     * Notifies connected chat users that the stream has stopped.
     */
    private void notifyUsers() {
        broadcastChatMessage("Server: The stream has stopped.");
    }
    /**
     * Sends a video packet to all connected clients via UDP.
     *
     * @param packet    The packet to send.
     * @param udpSocket The UDP socket used for sending the packet.
     */
    private void sendPacket(byte[] packet, DatagramSocket udpSocket) {
        for (InetSocketAddress clientAddress : clients.keySet()) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, clientAddress.getAddress(), clientAddress.getPort());
                udpSocket.send(datagramPacket);
                System.out.println("Sending packet to " + clientAddress); // debug message
            } catch (IOException e) {
                System.err.println("Error sending packet: " + e.getMessage()); // debug message
            }
        }
    }
    /**
     * Listens for chat client connections and starts a new thread for each connection.
     *
     * @param chatSocket The server socket used for accepting chat connections.
     */
    private void listenForChatConnections(ServerSocket chatSocket) {
        try {
            System.out.println("Listening for chat clients on port " + chatPort + "...");
            while (true) {
                Socket chatClientSocket = chatSocket.accept();
                new Thread(() -> handleChatClient(chatClientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error in chat listener: " + e.getMessage());
        }
    }
    /**
     * Handles a chat client connection, registering the client and processing chat messages.
     *
     * @param clientSocket The socket connected to the chat client.
     */
    private void handleChatClient(Socket clientSocket) {
        InetSocketAddress clientAddress = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            // First message is for registration
            String registrationMessage = reader.readLine();
            if (registrationMessage != null && registrationMessage.startsWith("REGISTER")) {

                // Add to chat clients list
                String[] parts = registrationMessage.split(":");
                String displayName = parts[1];
                int streamPort = Integer.parseInt(parts[2]);
                System.out.println("Registered client: " + displayName);
                clientAddress = new InetSocketAddress(clientSocket.getInetAddress(), streamPort);
                clients.put(clientAddress, writer);

                // Acknowledge registration
                writer.println("Registration successful");

                // Transition to chat mode
                handleChatMessages(reader, displayName);
            }
        } catch (IOException e) {
            System.err.println("Error handling chat client: " + e.getMessage());
        } finally {
            if (clientAddress != null) {
                clients.remove(clientAddress);
                System.out.println("Client removed: " + clientAddress);
            }
        }
    }
    /**
     * Handles chat messages from a client and broadcasts them to all connected clients.
     *
     * @param reader       BufferedReader for reading messages from the client.
     * @param displayName  The display name of the client.
     */
    private void handleChatMessages(BufferedReader reader, String displayName) {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                String formattedMessage = displayName + ": " + message;
                System.out.println("Broadcasting chat message: " + formattedMessage);
                broadcastChatMessage(formattedMessage);
            }
        } catch (IOException e) {
            System.err.println("Chat client disconnected: " + e.getMessage());
        }
    }
    /**
     * Broadcasts a chat message to all connected chat clients.
     *
     * @param message The message to broadcast.
     */
    private void broadcastChatMessage(String message) {
        for (PrintWriter client : clients.values()) {
            client.println(message);
        }
    }
}
