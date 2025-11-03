package JavaBeta;

import JavaBeta.ChatController;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class P2PPeer {

    private final int listeningPort;
    private final ChatController controller;
    // Use a thread-safe set to manage connections from multiple threads
    private final Set<Socket> connectedPeers;
    private PeerReceiver receiver; // The dedicated server listener thread

    public P2PPeer(int port, ChatController controller) {
        this.listeningPort = port;
        this.controller = controller;
        this.connectedPeers = Collections.synchronizedSet(new HashSet<>());
    }

    // Starts the dedicated listening thread (Server Role)
    public void startListening() {
        try {
            // Pass 'this' P2PPeer object to the receiver for connection management
            this.receiver = new PeerReceiver(listeningPort, this, controller);
            this.receiver.start();
        } catch (IOException e) {
            controller.displayMessage("System", "Error starting listening server: " + e.getMessage());
        }
    }

    // Stops all networking threads and closes all sockets
    public void stopListening() {
        if (receiver != null) {
            receiver.shutdown();
        }
        synchronized (connectedPeers) {
            for (Socket socket : connectedPeers) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }

    // Initiates an outgoing connection (Client Role)
    public void connectToPeer(String ip, int port) {
        try {
            Socket socket = new Socket(ip, port);
            addPeer(socket); // Add the new outgoing connection

            // Start a MessageHandler for this socket to receive incoming messages from the peer
            new MessageHandler(socket, controller, this).start();

            controller.displayMessage("System", "Successfully connected to " + ip + ":" + port);
        } catch (IOException e) {
            controller.displayMessage("System", "Failed to connect to peer: " + e.getMessage());
        }
    }

    // Broadcasts message to all connected peers (Client Role)
    public void sendMessage(String message) {
        synchronized (connectedPeers) {
            if (connectedPeers.isEmpty()) {
                controller.displayMessage("System", "Not connected to any peers.");
                return;
            }
            // Use a temporary list to safely iterate and check for failures
            Set<Socket> failedPeers = new HashSet<>();

            for (Socket socket : connectedPeers) {
                try {
                    // Send message via the output stream, ensuring it's flushed (autoFlush=true)
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(message);
                } catch (IOException e) {
                    failedPeers.add(socket);
                }
            }
            // Clean up any peers that failed to receive the message
            for (Socket socket : failedPeers) {
                removePeer(socket);
                controller.displayMessage("System", "Connection lost with " + socket.getInetAddress().getHostAddress());
            }
        }
    }

    // Utility to manage the peer list (Called by PeerReceiver & MessageHandler)
    public void addPeer(Socket socket) {
        connectedPeers.add(socket);
    }

    // Utility to remove a peer and close its socket (Crucial for cleanup)
    public void removePeer(Socket socket) {
        connectedPeers.remove(socket);
        try { socket.close(); } catch (IOException ignored) {}
    }
}