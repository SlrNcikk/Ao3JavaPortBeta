package JavaBeta;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ChatApplication extends Application {

    private static final int PEER_PORT = 5000;
    private P2PPeer peer;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                ChatApplication.class.getResource("chat-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 600, 400);

        ChatController controller = fxmlLoader.getController();

        this.peer = new P2PPeer(PEER_PORT, controller);
        controller.setPeer(this.peer);

        this.peer.startListening();

        this.peer.startDiscovery();

        stage.setTitle("P2P Chat - Listening on Port " + PEER_PORT);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Called when the user closes the application.
     * Ensures all networking threads and sockets are closed gracefully.
     */
    @Override
    public void stop() {
        if (peer != null) {
            // This is the essential part: stopping the networking threads.
            peer.stopListening();
        }
        // Removed: super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}