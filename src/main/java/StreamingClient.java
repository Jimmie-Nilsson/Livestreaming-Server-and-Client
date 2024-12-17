import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * StreamingClient is a GUI-based Java application that captures video
 * from a specified source, streams it to a server, and displays the video
 * locally in a window.
 */
public class StreamingClient {

    private VideoStreamer videoStreamer; // Handles video streaming
    private Thread streamingThread; // Thread for streaming process
    private JLabel videoDisplayLabel; // Displays the video frames locally

    /**
     * The entry point for the StreamingClient application.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new StreamingClient()::createAndShowGUI);
    }

    /**
     * Creates and displays the graphical user interface for the streaming client.
     * The interface includes input fields for server address, server port,
     * and video source selection, along with a button to start or stop streaming.
     */
    private void createAndShowGUI() {
        JFrame frame = new JFrame("Streaming Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));

        // Server address input
        JLabel serverLabel = new JLabel("Server Address:");
        JTextField serverField = new JTextField("localhost");
        inputPanel.add(serverLabel);
        inputPanel.add(serverField);

        // Server port input
        JLabel portLabel = new JLabel("Server Port:");
        JTextField portField = new JTextField("8080");
        inputPanel.add(portLabel);
        inputPanel.add(portField);

        // Video source selection currently only works for windows but this makes room for expansion
        JLabel sourceLabel = new JLabel("Video Source:");
        JComboBox<String> sourceCombo = new JComboBox<>();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            sourceCombo.addItem("desktop");
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            sourceCombo.addItem("default");
        } else {
            sourceCombo.addItem("/dev/video0");
        }
        inputPanel.add(sourceLabel);
        inputPanel.add(sourceCombo);

        JButton startStopButton = new JButton("Start Streaming");

        videoDisplayLabel = new JLabel(); // JLabel for video display
        videoDisplayLabel.setHorizontalAlignment(JLabel.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(inputPanel);
        splitPane.setBottomComponent(videoDisplayLabel);
        splitPane.setResizeWeight(0.3);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(startStopButton, BorderLayout.SOUTH);

        // Action listener for the start/stop button
        startStopButton.addActionListener(e -> {
            if (streamingThread == null || !streamingThread.isAlive()) {
                // Start streaming
                String serverAddress = serverField.getText().trim();
                int serverPort;
                try {
                    serverPort = Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid port number.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String videoSource = (String) sourceCombo.getSelectedItem();
                videoStreamer = new VideoStreamer(videoSource, serverAddress, serverPort);

                // Start the streaming thread
                streamingThread = new Thread(() -> {
                    try {
                        videoStreamer.startStreaming();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Streaming Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                streamingThread.start();
                new Thread(this::updateVideoDisplay).start();
                startStopButton.setText("Stop Streaming");
            } else {
                // Stop streaming
                videoStreamer.stopStreaming();
                try {
                    streamingThread.join();
                } catch (InterruptedException ignored) {
                }
                startStopButton.setText("Start Streaming");
            }
        });

        // Window closing to stop resources properly
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (videoStreamer != null) {
                    videoStreamer.stopStreaming();
                }
                if (streamingThread != null) {
                    try {
                        streamingThread.join();
                    } catch (InterruptedException ignored) {
                    }
                }
                frame.dispose();
            }
        });
        frame.setVisible(true);
    }

    /**
     * Updates the video display by retrieving video frames from the VideoStreamer
     * and displaying them in a scaled format on the JLabel.
     */
    private void updateVideoDisplay() {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        long lastUpdateTime = 0;
        while (videoStreamer.isStreaming()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > 33) { // Update approximately every 33 ms (30 FPS)
                // this is to make sure the process doesn't use too many resources
                lastUpdateTime = currentTime;

                try {
                    Frame videoFrame = videoStreamer.grabFrame();
                    if (videoFrame != null) {
                        BufferedImage image = converter.convert(videoFrame);


                        // Scale image to fit JLabel dimensions
                        int labelWidth = videoDisplayLabel.getWidth();
                        int labelHeight = videoDisplayLabel.getHeight();
                        if (labelWidth > 0 && labelHeight > 0) { // Ensure label is visible
                            BufferedImage scaledImage = new BufferedImage(labelWidth, labelHeight, image.getType());
                            Graphics2D g2d = scaledImage.createGraphics();
                            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            g2d.drawImage(image, 0, 0, labelWidth, labelHeight, null);
                            g2d.dispose();


                            // Update JLabel with the new image on the Swing thread
                            SwingUtilities.invokeLater(() -> videoDisplayLabel.setIcon(new ImageIcon(scaledImage)));
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error grabbing frame:" + ex.getMessage());
                }
            }
        }
    }
}
