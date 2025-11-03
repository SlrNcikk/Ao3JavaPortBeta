package JavaBeta; // Ensure this matches your package name

import javafx.application.Platform;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class UdpDiscovery extends Thread {

    private static final int DISCOVERY_PORT = 9000;
    private static final String BROADCAST_ADDRESS = "255.255.255.255";

    private final ChatController controller;
    private final int tcpListeningPort;
    private volatile boolean running = true;

    // We need the TCP listening port of this peer so others know how to connect back
    public UdpDiscovery(ChatController controller, int tcpListeningPort) {
        this.controller = controller;
        this.tcpListeningPort = tcpListeningPort;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
            // Configure the socket to allow sending broadcasts
            socket.setBroadcast(true);

            // Set a timeout so the thread doesn't block forever and can periodically send beacons
            socket.setSoTimeout(5000);

            while (running) {
                // 1. ANNOUNCE PRESENCE (Beacon Sending)
                sendBeacon(socket);

                // 2. LISTEN FOR OTHERS (Receiving Beacons)
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    processReceivedBeacon(packet);

                } catch (SocketTimeoutException ignored) {
                    // Expected to happen when no traffic is received
                } catch (IOException e) {
                    if (running) {
                        Platform.runLater(() -> controller.displayMessage("System", "Discovery error: " + e.getMessage()));
                    }
                }
            }
        } catch (IOException e) {
            Platform.runLater(() -> controller.displayMessage("System", "Failed to start Discovery Socket: " + e.getMessage()));
        }
    }

    private void sendBeacon(DatagramSocket socket) throws IOException {
        // Beacon format: "DISCOVER:<TCP_PORT>" (e.g., "DISCOVER:5000")
        String message = "DISCOVER:" + tcpListeningPort;
        byte[] buffer = message.getBytes();

        // Send to the broadcast address
        InetAddress broadcastAddress = InetAddress.getByName(BROADCAST_ADDRESS);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, DISCOVERY_PORT);

        socket.send(packet);
    }

    private void processReceivedBeacon(DatagramPacket packet) {
        // Stop processing if the packet came from this peer itself
        if (packet.getAddress().isLoopbackAddress() || packet.getAddress().equals(getOwnBroadcastAddress())) {
            return;
        }

        String received = new String(packet.getData(), 0, packet.getLength()).trim();

        if (received.startsWith("DISCOVER:")) {
            String[] parts = received.split(":");
            if (parts.length == 2) {
                String ip = packet.getAddress().getHostAddress();
                int port = Integer.parseInt(parts[1]);

                // CRUCIAL: Safely update the JavaFX UI
                controller.displayMessage("Discovery", "Found Peer: " + ip + ":" + port);

                // TODO: Add logic here to update a dedicated peer list in the ChatController,
                // instead of just printing it to the main chat display.
            }
        }
    }

    // Helper to try and get the peer's own IP to prevent processing its own broadcast (optional, complex)
    private InetAddress getOwnBroadcastAddress() {
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(InetAddress.getByName("8.8.8.8"), 10002); // Connect to external address to get local IP
            return s.getLocalAddress();
        } catch (Exception e) {
            return InetAddress.getLoopbackAddress();
        }
    }


    public void shutdown() {
        this.running = false;
        // The thread will exit its loop after the next timeout
    }
}