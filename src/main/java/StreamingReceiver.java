import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class StreamingReceiver {
    public static void main(String[] args) {
        int udpPort = 8081; // Port to listen for the stream
        String udpStreamUrl = "udp://@:" + udpPort; // VLC-style UDP stream URL

        // Register with the server
        String serverAddress = "localhost"; // Replace with actual server address if remote
        int registrationPort = 8082;

        try (DatagramSocket socket = new DatagramSocket(udpPort)) {
            InetAddress serverInetAddress = InetAddress.getByName(serverAddress);
            byte[] message = "REGISTER".getBytes();

            DatagramPacket packet = new DatagramPacket(message, message.length, serverInetAddress, registrationPort);
            socket.send(packet);
            System.out.println("Registered with the server on port " + udpPort);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Initialize VLCJ media player
        uk.co.caprica.vlcj.factory.MediaPlayerFactory mediaPlayerFactory = new uk.co.caprica.vlcj.factory.MediaPlayerFactory();
        uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer = mediaPlayerFactory.mediaPlayers().newMediaPlayer();

        // Start playback from the UDP stream
        mediaPlayer.media().play("udp://127.0.0.1@:8081");
        System.out.println("Streaming playback started on " + udpStreamUrl);

        // Keep the application running to allow playback
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Clean up resources on exit
        mediaPlayer.release();
        mediaPlayerFactory.release();
    }
}
