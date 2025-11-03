package JavaBeta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MessageHandler extends Thread {

    private final Socket peerSocket;
    private final ChatController controller;
    private final P2PPeer peerManager; // Reference for cleanup

    public MessageHandler(Socket socket, ChatController controller, P2PPeer peerManager) {
        this.peerSocket = socket;
        this.controller = controller;
        this.peerManager = peerManager;
    }

    @Override
    public void run() {
        // Use the IP and Port of the remote peer for displaying messages
        String senderInfo = peerSocket.getInetAddress().getHostAddress() + ":" + peerSocket.getPort();

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(peerSocket.getInputStream())))
        {
            String message;
            // Read loop runs until the stream is closed (connection breaks)
            while ((message = in.readLine()) != null) {
                // Pass the received message to the JavaFX controller for display
                controller.displayMessage(senderInfo, message);
            }

        } catch (IOException e) {
            // This is expected when the remote peer disconnects or the network fails
            controller.displayMessage("System", senderInfo + " connection lost.");
        } finally {
            // CRUCIAL CLEANUP STEP: Remove the socket from the manager's list and close it
            peerManager.removePeer(peerSocket);
        }
    }
}