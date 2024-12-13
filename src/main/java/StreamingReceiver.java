import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;


public class StreamingReceiver {
    private final int streamPort;
    private final int chatPort;
    private final String displayName;
    private final String serverAddress;
    private final String udpStreamUrl;
    private ChatClient chatClient;
    private JTextArea chatArea;

    public StreamingReceiver(String serverAddress, int streamPort, int chatPort, String displayName) {
        this.serverAddress = serverAddress;
        this.streamPort = streamPort;
        this.chatPort = chatPort;
        this.displayName = displayName;
        udpStreamUrl = "udp://@:" + this.streamPort + "?pkt_size=1316";
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Error usage: java StreamingReceiver <serverAddress> <streamPort> <chatPort> <displayName>");
            System.exit(1);
        }
        new StreamingReceiver(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]).startClient();

    }
    public void startClient(){
        createAndShowGUI();
    }

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

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Streaming Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 900);
        frame.setLayout(new BorderLayout());

        // Media Player
        EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        frame.add(mediaPlayerComponent, BorderLayout.CENTER);

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

        frame.add(chatPanel, BorderLayout.EAST);
        frame.setVisible(true);
        initializeChatClient();
        mediaPlayerComponent.mediaPlayer().media().play(udpStreamUrl);
    }

    private void appendChatMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }
}
