package JavaBeta;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PeerReceiver extends Thread {

    private final ServerSocket serverSocket;
    private final P2PPeer peerManager;
    private final ChatController controller;
    private volatile boolean running = true;

    public PeerReceiver(int port, P2PPeer peerManager, ChatController controller) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.peerManager = peerManager;
        this.controller = controller;
    }

    @Override
    public void run() {
        controller.displayMessage("System", "P2P Server listening on port " + serverSocket.getLocalPort());
        while (running) {
            try {
                // Blocks until a new peer connects
                Socket peerSocket = serverSocket.accept();

                // Add the new incoming connection to the peer manager list
                peerManager.addPeer(peerSocket);
                controller.displayMessage("System", "New incoming connection from " + peerSocket.getInetAddress().getHostAddress());

                // Start a new thread to handle continuous messages from this specific peer
                // Pass P2PPeer here for cleanup in case of disconnect
                new MessageHandler(peerSocket, controller, peerManager).start();

            } catch (IOException e) {
                // Ignore the exception if the socket was closed intentionally during shutdown
                if (running) {
                    controller.displayMessage("System", "Peer Receiver error: " + e.getMessage());
                }
            }
        }
    }

    // Method to safely stop the listening thread
    public void shutdown() {
        this.running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {}
    }
}