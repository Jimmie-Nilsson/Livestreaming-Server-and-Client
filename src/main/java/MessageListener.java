/**
 * Interface for handling received messages.
 *
 * @author Jimmie Nilsson jini6619
 */
public interface MessageListener {
    /**
     * Called when a new message is received.
     *
     * @param message The received message.
     */
    void onMessageReceived(String message);
}
