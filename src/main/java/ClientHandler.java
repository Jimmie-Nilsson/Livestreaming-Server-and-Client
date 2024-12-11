import java.net.Socket;

public class ClientHandler {
    private final StreamingServer server;
    private final Socket socket;
    private final String name;
    private int port;


    public ClientHandler(StreamingServer server, Socket socket, String name, int port) {
        this.server = server;
        this.socket = socket;
        this.name = name;
        this.port = port;

    }


}
