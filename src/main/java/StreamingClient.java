import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.net.Socket;

/**
 * A client application for streaming video to a server using FFmpeg and displaying it locally.
 *
 * @author Jimmie Nilsson jini6619
 */
public class StreamingClient {

    private static boolean isStreaming = false;
    private static Thread streamingThread;
    private static JLabel videoDisplayLabel;
    private static FFmpegFrameGrabber videoGrabber;
    private static FFmpegFrameRecorder recorder;

    /**
     * Entry point for the streaming client application.
     *
     * @param args Command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(StreamingClient::createAndShowGUI);
    }


    /**
     * Creates and displays the main graphical user interface for the application.
     */
    private static void createAndShowGUI() {
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
            // Code only works on windows and desktop capture atm but there is room to expand the code.
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
            if (!isStreaming) {
                String serverAddress = serverField.getText().trim();
                int serverPort;
                try {
                    serverPort = Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid port number.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String videoSource = (String) sourceCombo.getSelectedItem();

                isStreaming = true;
                startStopButton.setText("Stop Streaming");

                streamingThread = new Thread(() -> startStreaming(serverAddress, serverPort, videoSource, startStopButton));
                streamingThread.start();

            } else {
                stopStreaming();
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopStreaming(); // Ensure resources are cleaned up
                frame.dispose();
            }
        });
        frame.setVisible(true);
    }

    /**
     * Starts the streaming process, connecting to the server and sending video frames.
     *
     * @param serverAddress   The address of the streaming server.
     * @param serverPort      The port of the streaming server.
     * @param videoSource     The video source (e.g., "desktop", "camera").
     * @param startStopButton The button used to toggle streaming, updated upon stopping.
     */
    private static void startStreaming(String serverAddress, int serverPort, String videoSource, JButton startStopButton) {
        try (Socket socket = new Socket(serverAddress, serverPort);
             OutputStream socketStream = socket.getOutputStream()) {

            org.bytedeco.ffmpeg.global.avutil.av_log_set_level(org.bytedeco.ffmpeg.global.avutil.AV_LOG_INFO);

            videoGrabber = new FFmpegFrameGrabber(videoSource);
            videoGrabber.setFormat("gdigrab"); // Default format for screen capture on Windows
            videoGrabber.setImageHeight(1080);
            videoGrabber.setImageWidth(1920);
            videoGrabber.start();

            recorder = new FFmpegFrameRecorder(
                    socketStream,
                    videoGrabber.getImageWidth(),
                    videoGrabber.getImageHeight()
            );
            recorder.setFormat("mpegts");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFrameRate(30);
            recorder.setVideoBitrate(2000000);

            recorder.setOption("movflags", "faststart");
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setOption("mpegts_flags", "resend_headers");
            recorder.setOption("pkt_size", "1316"); // Set packet size

            recorder.start();
            Java2DFrameConverter converter = new Java2DFrameConverter();
            long lastUpdateTime = 0;
            while (isStreaming) {
                try {
                    handleStream(converter, lastUpdateTime);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Streaming Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            cleanupResources();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Streaming Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            isStreaming = false;
            SwingUtilities.invokeLater(() -> startStopButton.setText("Start Streaming"));
        }
    }

    /**
     * Handles the streaming process, grabbing video frames, recording them, and updating the UI.
     *
     * @param converter      The frame converter for converting video frames to BufferedImages.
     * @param lastUpdateTime The last time the UI was updated with a frame.
     * @throws Exception If grabbing a frame fails or  recording a frame fails.
     */
    private static void handleStream(Java2DFrameConverter converter, long lastUpdateTime) throws Exception {
        Frame videoFrame = videoGrabber.grab();
        if (videoFrame != null) {
            recorder.record(videoFrame);

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdateTime > 33) { // 1000 ms / 30 FPS = ~33 ms
                lastUpdateTime = currentTime;

                // Convert the frame to a BufferedImage
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
        }
    }

    /**
     * Stops the streaming process and cleans up resources.
     */
    private static void stopStreaming() {
        isStreaming = false;
        if (streamingThread != null && streamingThread.isAlive()) {
            try {
                streamingThread.join(1000); // Wait for the thread to terminate
            } catch (InterruptedException ignored) {
            }
        }
        cleanupResources();
    }

    /**
     * Cleans up resources such as the video grabber and recorder.
     */
    private static void cleanupResources() {
        try {
            if (videoGrabber != null) {
                videoGrabber.stop();
                videoGrabber.release();
            }
            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}