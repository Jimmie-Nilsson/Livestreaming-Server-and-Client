import java.net.Socket;

public class ClientHandler {
    private final StreamingServer server;
    private final Socket socket;
    private final String name;


    public ClientHandler(StreamingServer server, Socket socket, String name) {
        this.server = server;
        this.socket = socket;
        this.name = name;

    }


}
