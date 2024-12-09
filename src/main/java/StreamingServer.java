import java.io.*;
import java.net.*;

public class StreamingServer {
    private final int TCPport;
    private final int UDPport;

    public StreamingServer(int TCPport, int UDPport) {
        this.TCPport = TCPport;
        this.UDPport = UDPport;
    }
    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            try (InputStream inputStream = clientSocket.getInputStream();
                 FileOutputStream fileOutputStream = new FileOutputStream("output3.ts")) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
