import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class StreamingReceiver {
    private int streamPort;
    private final int chatPort;
    private final String displayName;
    private String serverAddress;
    private String udpStreamUrl;
    private ChatClient chatClient;

    public StreamingReceiver(String serverAddress, int streamPort, int chatPort, String displayName) {
        this.serverAddress = serverAddress;
        this.streamPort = streamPort;
        this.chatPort = chatPort;
        this.displayName = displayName;
        udpStreamUrl = "udp://@:" + this.streamPort + "?pkt_size=1316";
        initializeChatClient();
        connectToServer(serverAddress, streamPort, chatPort, displayName);
    }
    public StreamingReceiver() {
        this.serverAddress = "127.0.0.1";
        this.streamPort = 8081;
        this.chatPort = 8082;
        this.displayName = "Test";
        udpStreamUrl = "udp://@:" + streamPort + "?pkt_size=1316";
        initializeChatClient();
        connectToServer(serverAddress, streamPort, chatPort, displayName);
    }


    public static void main(String[] args) {
        if (args.length != 4) {
            //System.err.println("Error usage: java StreamingReceiver <serverAddress> <streamPort> <chatPort> <displayName>");
            //System.exit(1);
        }
        new StreamingReceiver();

    }

    private void initializeChatClient() {
        chatClient = new ChatClient(serverAddress, chatPort);
        chatClient.setMessageListener(message -> SwingUtilities.invokeLater(() -> appendChatMessage(message)));
        try {
            chatClient.connect();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to connect to chat server: " + e.getMessage());
        }
    }

    private void connectToServer(String serverAddress, int streamPort, int chatPort, String displayName) {
        try (Socket socket = new Socket(serverAddress, chatPort);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer.println(displayName + ":" + streamPort);
            String response = reader.readLine();
            if (response.equalsIgnoreCase("Registration successful")){
                System.out.println("Successfully registered with the server on: " + serverAddress + " on port: " + chatPort);
            } else {
                System.err.println("Registration failed: " + response);
                return;
            }
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            return;
        }
        SwingUtilities.invokeLater(() -> createAndShowGUI(udpStreamUrl));
    }

    private static void createAndShowGUI(String udpStreamUrl) {

        JFrame frame = new JFrame("Streaming Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());


        EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        frame.add(mediaPlayerComponent, BorderLayout.CENTER);


        frame.setVisible(true);


        mediaPlayerComponent.mediaPlayer().media().play(udpStreamUrl);
        System.out.println("Streaming playback started on " + udpStreamUrl);


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mediaPlayerComponent.release();
            System.out.println("MediaPlayer resources released.");
        }));
    }
    private void appendChatMessage(String message) {
        // Append incoming message to the chat area
        // This can be optimized if `chatArea` is made a field
        JTextArea chatArea = ((JTextArea) ((JScrollPane) ((JPanel) ((JFrame) SwingUtilities.getWindowAncestor(null))
                .getContentPane().getComponent(1)).getComponent(0)).getViewport().getView());
        chatArea.append(message + "\n");
    }
}
