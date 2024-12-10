import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class StreamingReceiver {
    public static void main(String[] args) {
        int udpPort = 8081; // Port to listen for the stream
        //String udpStreamUrl = "udp://@:" + udpPort; // VLC-style UDP stream URL
        String udpStreamUrl = "udp://@:" + udpPort + "?pkt_size=1316";// Listening on the local machine's UDP port

        // Register with the server
        String serverAddress = "localhost"; // Replace with actual server address if remote
        int registrationPort = 8082;

        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            InetAddress serverInetAddress = InetAddress.getByName(serverAddress);
            byte[] message = "REGISTER".getBytes();

            DatagramPacket packet = new DatagramPacket(message, message.length, serverInetAddress, registrationPort);
            socket.send(packet);
            System.out.println("Registered with the server on port " + registrationPort);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Initialize Swing and VLCJ components
        SwingUtilities.invokeLater(() -> createAndShowGUI(udpStreamUrl));
    }

    private static void createAndShowGUI(String udpStreamUrl) {
        // Create the JFrame
        JFrame frame = new JFrame("Streaming Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Add VLCJ EmbeddedMediaPlayerComponent
        EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        frame.add(mediaPlayerComponent, BorderLayout.CENTER);

        // Show the frame
        frame.setVisible(true);

        // Start playing the UDP stream
        mediaPlayerComponent.mediaPlayer().media().play(udpStreamUrl);
        System.out.println("Streaming playback started on " + udpStreamUrl);

        // Add shutdown hook to release VLCJ resources on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mediaPlayerComponent.release();
            System.out.println("MediaPlayer resources released.");
        }));
    }
}
