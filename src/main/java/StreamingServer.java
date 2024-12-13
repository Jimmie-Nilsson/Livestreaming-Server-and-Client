import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;


public class StreamingServer {
    private final int streamerPort;
    private final int chatPort;
    private static final ConcurrentHashMap<InetSocketAddress, PrintWriter> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new StreamingServer().startServer();
    }

    public StreamingServer(int streamerPort, int chatPort) {
        this.streamerPort = streamerPort;
        this.chatPort = chatPort;
    }

    public StreamingServer() {
        streamerPort = 8080;
        chatPort = 8082;
    }

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
        }
    }

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

    private void broadcastChatMessage(String message) {
        for (PrintWriter client : clients.values()) {
            client.println(message);
        }
    }
}
