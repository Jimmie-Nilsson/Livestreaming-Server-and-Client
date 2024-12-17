import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * A client application to receive a video stream over UDP and manage chat functionality.
 *
 * @author Jimmie Nilsson jini6619
 */
public class StreamingReceiver {
    private final int streamPort;
    private final int chatPort;
    private final String displayName;
    private final String serverAddress;
    private final String udpStreamUrl;
    private ChatClient chatClient;
    private JTextArea chatArea;

    /**
     * Constructs a StreamingReceiver instance.
     *
     * @param serverAddress The address of the server.
     * @param streamPort    The port to receive the video stream.
     * @param chatPort      The port for chat communication.
     * @param displayName   The display name of the user.
     */
    public StreamingReceiver(String serverAddress, int streamPort, int chatPort, String displayName) {
        this.serverAddress = serverAddress;
        this.streamPort = streamPort;
        this.chatPort = chatPort;
        this.displayName = displayName;
        udpStreamUrl = "udp://@:" + this.streamPort + "?pkt_size=1316";
    }

    /**
     * Main entry point for the StreamingReceiver application.
     *
     * @param args Command-line arguments:
     *             - serverAddress: Server address.
     *             - streamPort: Port for video stream.
     *             - chatPort: Port for chat.
     *             - displayName: User's display name.
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Error usage: java StreamingReceiver <serverAddress> <streamPort> <chatPort> <displayName>");
            System.exit(1);
        }
        new StreamingReceiver(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]).startClient();

    }

    /**
     * Starts the client application by initializing the GUI.
     */
    public void startClient() {
        createAndShowGUI();
    }

    /**
     * Initializes the chat client and sets up message handling.
     */
    private void initializeChatClient() {
        chatClient = new ChatClient(serverAddress, chatPort);
        chatClient.setMessageListener(this::appendChatMessage);
        try {
            chatClient.connect();
            chatClient.sendMessage("REGISTER" + ":" + displayName + ":" + streamPort); // Register on connect
        } catch (IOException e) {
            System.err.println("Failed to connect to chat server: " + e.getMessage());
        }
    }

    /**
     * Creates and displays the GUI for the streaming receiver.
     */
    private void createAndShowGUI() {
        JFrame frame = new JFrame("Streaming Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 900);
        frame.setLayout(new BorderLayout());

        // Media Player
        EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();

        // Chat Panel
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        JTextField chatInput = new JTextField();
        chatInput.addActionListener(e -> {
            String message = chatInput.getText();
            if (!message.isEmpty()) {
                chatClient.sendMessage(message);
                chatInput.setText("");
            }
        });

        chatPanel.add(chatInput, BorderLayout.SOUTH);


        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7); //  70% of the space to the VLC media player
        splitPane.setDividerLocation(850);


        splitPane.setLeftComponent(mediaPlayerComponent); // Left side: Media Player
        splitPane.setRightComponent(chatPanel); // Right side: Chat Panel

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mediaPlayerComponent.release();
                chatClient.disconnect();
            }
        });
        frame.add(splitPane, BorderLayout.CENTER);
        frame.setVisible(true);


        initializeChatClient();
        mediaPlayerComponent.mediaPlayer().media().play(udpStreamUrl);
    }

    /**
     * Appends a new chat message to the chat area.
     *
     * @param message The chat message to append.
     */
    private void appendChatMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

}
