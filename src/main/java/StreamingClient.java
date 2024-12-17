import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class StreamingClient {

    private VideoStreamer videoStreamer;
    private Thread streamingThread;
    private JLabel videoDisplayLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new StreamingClient()::createAndShowGUI);
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Streaming Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));

        JLabel serverLabel = new JLabel("Server Address:");
        JTextField serverField = new JTextField("localhost");
        inputPanel.add(serverLabel);
        inputPanel.add(serverField);

        JLabel portLabel = new JLabel("Server Port:");
        JTextField portField = new JTextField("8080");
        inputPanel.add(portLabel);
        inputPanel.add(portField);

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

    private void updateVideoDisplay() {
        Java2DFrameConverter converter = new Java2DFrameConverter();
        long lastUpdateTime = 0;
        while (videoStreamer.isStreaming()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > 33) { // 1000 ms / 30 FPS = ~33 ms
                lastUpdateTime = currentTime;

                // Convert the frame to a BufferedImage
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
                            // Update JLabel on the UI thread
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
