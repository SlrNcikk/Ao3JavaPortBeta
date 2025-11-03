package JavaBeta; // IMPORTANT: Use your actual package name

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ChatController {

    // FXML injected UI components (matches fx:id in chat-view.fxml)
    @FXML
    private TextArea chatDisplay;
    @FXML
    private TextField messageInput;
    @FXML
    private TextField targetIpInput;
    @FXML
    private TextField targetPortInput;

    private P2PPeer peer; // Reference to the networking engine

    // Method called by ChatApplication to inject the P2PPeer instance
    public void setPeer(P2PPeer peer) {
        this.peer = peer;
    }

    // ----------------------------------------------------------------------
    // NETWORK INPUT HANDLER (Called by MessageHandler.java)
    // ----------------------------------------------------------------------

    /**
     * Safely updates the JavaFX UI with a new message from a networking thread.
     * @param senderInfo IP:Port of the peer who sent the message.
     * @param message The content of the message.
     */
    public void displayMessage(String senderInfo, String message) {
        // CRUCIAL: Must use Platform.runLater() for thread safety
        Platform.runLater(() -> {
            chatDisplay.appendText("[" + senderInfo + "]: " + message + "\n");
        });
    }

    // ----------------------------------------------------------------------
    // UI ACTION HANDLERS (Called by FXML on button click/Enter key)
    // ----------------------------------------------------------------------

    /**
     * Handles the 'Connect' button action to initiate a connection to another peer.
     */
    @FXML
    private void handleConnectAction() {
        String ip = targetIpInput.getText().trim();
        int port;

        if (ip.isEmpty()) {
            displayMessage("System", "Error: Please enter a target IP address.");
            return;
        }

        try {
            port = Integer.parseInt(targetPortInput.getText().trim());
            if (peer != null) {
                // Call the P2PPeer method to establish an outgoing connection
                peer.connectToPeer(ip, port);
            }
        } catch (NumberFormatException e) {
            displayMessage("System", "Error: Invalid port number entered.");
        }
    }

    /**
     * Handles the 'Send' button or Enter key action to broadcast a message.
     */
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
}