import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class StreamingServer {
    private static final int TCP_PORT = 8080; // Port for streamer connections
    private static final int RECEIVER_REGISTRATION_PORT = 8082; // Port for receiver registration
    private static final CopyOnWriteArrayList<InetSocketAddress> receiverClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try {
            new StreamingServer().startServer();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(TCP_PORT);
        DatagramSocket udpSocket = new DatagramSocket();

        // Thread to handle receiver registration
        new Thread(this::listenForReceivers).start();

        System.out.println("Server started. Waiting for connections...");
        while (true) {
            Socket streamerSocket = serverSocket.accept();
            System.out.println("Streamer connected: " + streamerSocket.getInetAddress());

            // Handle the streamer in a new thread
            new Thread(() -> handleStreamer(streamerSocket, udpSocket)).start();
        }
    }

    private void handleStreamer(Socket streamerSocket, DatagramSocket udpSocket) {
        byte[] buffer = new byte[8192]; // Larger buffer to read data
        byte[] packetBuffer = new byte[1316]; // Fixed-size buffer for packets

        try (InputStream inputStream = streamerSocket.getInputStream()) {
            int bytesRead, offset = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    packetBuffer[offset++] = buffer[i];

                    // Send packet when the buffer is full
                    if (offset == packetBuffer.length) {
                        sendPacket(packetBuffer, udpSocket);
                        offset = 0; // Reset offset for the next packet
                    }
                }
            }

            // Handle remaining bytes if any
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
        for (InetSocketAddress clientAddress : receiverClients) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, clientAddress.getAddress(), clientAddress.getPort());
                udpSocket.send(datagramPacket);
                System.out.println("Sending packet to " + clientAddress);
            } catch (IOException e) {
                System.err.println("Error sending packet: " + e.getMessage());
            }
        }
    }

    private void listenForReceivers() {
        try (DatagramSocket registrationSocket = new DatagramSocket(RECEIVER_REGISTRATION_PORT)) {
            System.out.println("Listening for receiver registrations on port " + RECEIVER_REGISTRATION_PORT);

            byte[] buffer = new byte[256]; // Small buffer for registration messages
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                registrationSocket.receive(packet);

                InetSocketAddress clientAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
                addReceiverClient(clientAddress);
            }
        } catch (IOException e) {
            System.err.println("Error in receiver registration listener: " + e.getMessage());
        }
    }

    private synchronized void addReceiverClient(InetSocketAddress clientAddress) {
        if (!receiverClients.contains(clientAddress)) {
            receiverClients.add(clientAddress);
            System.out.println("Receiver added: " + clientAddress);
        }
    }
}
