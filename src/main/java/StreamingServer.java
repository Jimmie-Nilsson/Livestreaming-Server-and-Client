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

        public void startServer () throws IOException {
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
        private void handleStreamer (Socket streamerSocket, DatagramSocket udpSocket){
            byte[] buffer = new byte[8192]; // Buffer size for UDP packets
            try (InputStream inputStream = streamerSocket.getInputStream()) {
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    // Send received data to all connected receiver clients
                    for (InetSocketAddress clientAddress : receiverClients) {
                        DatagramPacket packet = new DatagramPacket(buffer, bytesRead, clientAddress.getAddress(), clientAddress.getPort());
                        System.out.println("Sending " + bytesRead + " bytes to " + clientAddress + "to port:  " + clientAddress.getPort());
                        udpSocket.send(packet);
                    }
                }
            } catch (IOException e) {
                System.err.println("Streamer disconnected: " + e.getMessage());
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
