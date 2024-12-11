import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class StreamingServer {
    private int streamerPort;
    private int chatPort;
    private static final CopyOnWriteArrayList<InetSocketAddress> receiverClients = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<PrintWriter> chatClients = new CopyOnWriteArrayList<>();

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
             DatagramSocket udpSocket = new DatagramSocket()) {


            // each registration is put in a new thread.
            new Thread(this::listenForReceiversTCP).start();

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
        for (InetSocketAddress clientAddress : receiverClients) {
            try {
                DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, clientAddress.getAddress(), clientAddress.getPort());
                udpSocket.send(datagramPacket);
                System.out.println("Sending packet to " + clientAddress); // debug message
            } catch (IOException e) {
                System.err.println("Error sending packet: " + e.getMessage()); // debug message
            }
        }
    }

    private void listenForReceiversTCP() {
        try (ServerSocket registrationSocket = new ServerSocket(chatPort)) {
            System.out.println("Listening for receiver registrations on port " + chatPort);

            while (true) {
                Socket clientSocket = registrationSocket.accept();
                new Thread(() -> handleReceiverRegistration(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error in receiver registration listener: " + e.getMessage());
        }
    }

    private void handleReceiverRegistration(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String message = reader.readLine();
            String[] parts = message.split(":");
            String displayName = parts[0];
            int streamPort = Integer.parseInt(parts[1]);
            ClientHandler client = new ClientHandler(this, clientSocket, displayName, streamPort);
            InetSocketAddress clientAddress = new InetSocketAddress(clientSocket.getInetAddress(), streamPort);
            addReceiverClient(clientAddress);
            writer.println("Registration successful");
            System.out.println("Receiver registered: " + clientAddress);
        } catch (IOException e) {
            System.err.println("Error handling receiver registration: " + e.getMessage());
        }
    }

    private synchronized void addReceiverClient(InetSocketAddress clientAddress) {
        if (!receiverClients.contains(clientAddress)) {
            receiverClients.add(clientAddress);
            System.out.println("Receiver added: " + clientAddress);
        }
    }
}
