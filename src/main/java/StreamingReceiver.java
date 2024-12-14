import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * A client application to receive a video stream over UDP and manage chat functionality.
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

        // Add event listeners for the media player to handle stream interruptions
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventListener() {
            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                System.out.println("Stream error detected. Retrying...");
                restartStream(mediaPlayerComponent);
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                System.out.println("Stream finished. Retrying...");
                restartStream(mediaPlayerComponent);
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                System.out.println("Stream error detected. Retrying...");
                restartStream(mediaPlayerComponent);
            }

            // Other unused methods are intentionally left empty
            @Override public void buffering(MediaPlayer mediaPlayer, float newCache) {}
            @Override public void playing(MediaPlayer mediaPlayer) {}
            @Override public void paused(MediaPlayer mediaPlayer) {}
            @Override public void forward(MediaPlayer mediaPlayer) {}
            @Override public void backward(MediaPlayer mediaPlayer) {}
            @Override public void mediaPlayerReady(MediaPlayer mediaPlayer) {}
            @Override public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {}
            @Override public void opening(MediaPlayer mediaPlayer) {}
            @Override public void timeChanged(MediaPlayer mediaPlayer, long newTime) {}
            @Override public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {}
            @Override public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {}
            @Override public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {}
            @Override public void titleChanged(MediaPlayer mediaPlayer, int i) {}
            @Override public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {}
            @Override public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {}
            @Override public void videoOutput(MediaPlayer mediaPlayer, int newCount) {}
            @Override public void scrambledChanged(MediaPlayer mediaPlayer, int i) {}
            @Override public void elementaryStreamAdded(MediaPlayer mediaPlayer, TrackType trackType, int i) {}
            @Override public void elementaryStreamDeleted(MediaPlayer mediaPlayer, TrackType trackType, int i) {}
            @Override public void elementaryStreamSelected(MediaPlayer mediaPlayer, TrackType trackType, int i) {}
            @Override public void corked(MediaPlayer mediaPlayer, boolean b) {}
            @Override public void muted(MediaPlayer mediaPlayer, boolean b) {}
            @Override public void volumeChanged(MediaPlayer mediaPlayer, float v) {}
            @Override public void audioDeviceChanged(MediaPlayer mediaPlayer, String s) {}
            @Override public void chapterChanged(MediaPlayer mediaPlayer, int i) {}
        });

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

    /**
     * Attempts to restart the video stream in case of interruptions.
     *
     * @param mediaPlayerComponent The media player component handling the video stream.
     */
    private void restartStream(EmbeddedMediaPlayerComponent mediaPlayerComponent) {
        SwingUtilities.invokeLater(() -> {
            // if media player is running stop it.
            if (mediaPlayerComponent.mediaPlayer().status().isPlaying()) {
                mediaPlayerComponent.mediaPlayer().controls().stop();
            }

            // restart stream if possible.
            System.out.println("Restarting stream...");
            mediaPlayerComponent.mediaPlayer().media().play(udpStreamUrl);
        });
    }
}
