import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class StreamingReceiver {
    public static void main(String[] args) {
        int udpPort = 8081;
        byte[] buffer = new byte[4096];

        try (DatagramSocket udpSocket = new DatagramSocket(udpPort)) {
            System.out.println("Receiving stream on UDP port " + udpPort);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);


                System.out.println("Received packet of size: " + packet.getLength());


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
