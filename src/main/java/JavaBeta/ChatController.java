package JavaBeta; // Ensure this matches your package name

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatController {

    // FXML injected UI components
    @FXML private TextArea chatDisplay;
    @FXML private TextField messageInput;

    // NEW FXML FIELD: The list where discovered peers will appear
    @FXML private ListView<String> peerListView;

    // NEW: Observable List to hold the discovered peer strings (IP:Port)
    private ObservableList<String> discoveredPeers = FXCollections.observableArrayList();

    private P2PPeer peer;

    // Initialize method is called after FXML components are loaded
    @FXML
    public void initialize() {
        // Link the ObservableList to the ListView component
        peerListView.setItems(discoveredPeers);
    }

    public void setPeer(P2PPeer peer) {
        this.peer = peer;
    }

    @FXML
    private void handleSendAction() {
        String message = messageInput.getText().trim();

        if (!message.isEmpty() && peer != null) {
            // Call the P2PPeer method to broadcast the message
            peer.sendMessage(message);

            // Immediately display the sender's own message
            displayMessage("You", message);

            messageInput.clear();
        }
    }
    // ----------------------------------------------------------------------
    // DISCOVERY MANAGEMENT (Called by UdpDiscovery.java)
    // ----------------------------------------------------------------------

    /**
     * Called by the UdpDiscovery thread to add a new, unique peer to the list.
     */
    public void addDiscoveredPeer(String ip, int port) {
        String peerInfo = ip + ":" + port;

        // CRUCIAL: Must use Platform.runLater() to update the UI list safely
        Platform.runLater(() -> {
            // Only add if the list doesn't already contain this peer
            if (!discoveredPeers.contains(peerInfo)) {
                discoveredPeers.add(peerInfo);
                displayMessage("Discovery", "New peer found: " + peerInfo);
            }
        });
    }

    public void displayMessage(String senderInfo, String message) {
        // CRUCIAL: Must use Platform.runLater() for thread safety
        Platform.runLater(() -> {
            // Appends the formatted message to the main chat display area
            chatDisplay.appendText("[" + senderInfo + "]: " + message + "\n");
        });
    }

    // ----------------------------------------------------------------------
    // NEW UI ACTION HANDLER
    // ----------------------------------------------------------------------

    /**
     * Handles the 'Start Chat' button action to connect to a selected peer.
     */
    @FXML
    private void handleStartDiscoveryChat() {
        String selectedPeer = peerListView.getSelectionModel().getSelectedItem();

        if (selectedPeer == null) {
            displayMessage("System", "Please select a peer from the list.");
            return;
        }

        try {
            // Parse the IP and Port from the selected string (e.g., "192.168.1.5:5001")
            String[] parts = selectedPeer.split(":");
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);

            // Initiate the reliable TCP connection
            peer.connectToPeer(ip, port);

        } catch (Exception e) {
            displayMessage("System", "Connection error: " + e.getMessage());
        }
    }
}