package JavaBeta;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SpotifyController {

    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button nextButton;
    @FXML private Button previousButton;
    @FXML private Label currentTrackLabel;
    @FXML private Button loginButton;
    @FXML private Button refreshButton;

    @FXML
    private void initialize() {
        refreshTrackInfo();

        playButton.setOnAction(e -> {
            try {
                SpotifyService.play();
                refreshTrackInfo();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        pauseButton.setOnAction(e -> {
            try {
                SpotifyService.pause();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        nextButton.setOnAction(e -> {
            try {
                SpotifyService.nextTrack();
                refreshTrackInfo();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        previousButton.setOnAction(e -> {
            try {
                SpotifyService.previousTrack();
                refreshTrackInfo();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

    }
    @FXML
    private Label trackLabel;

    @FXML
    private void onRefresh() {
        // The try...catch block goes INSIDE the method
        try {
            // First, find and set the active device ID
            SpotifyService.findAndSetDeviceId();

            // Then, get the track info
            refreshTrackInfo();

        } catch (Exception e) {
            e.printStackTrace();
            currentTrackLabel.setText("Failed to refresh.");
        }
    } // <-- This is the correct closing brace for the method

    @FXML
    private void onLogin() {
        try {
            SpotifyService.authenticate();
            loginButton.setText("Logged In!");
            loginButton.setDisable(true);
            refreshTrackInfo(); // Try to get track info right after login
        } catch (Exception e) {
            e.printStackTrace();
            currentTrackLabel.setText("Login failed.");
        }
    }


    @FXML
    private void onPlay() {
        try {
            SpotifyService.play();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onPause() {
        try {
            SpotifyService.pause();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onNext() {
        try {
            SpotifyService.nextTrack();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onPrevious() {
        try {
            SpotifyService.previousTrack();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    private void refreshTrackInfo() {
        try {
            String trackInfo = SpotifyService.getCurrentTrack();
            currentTrackLabel.setText(trackInfo);
        } catch (Exception e) {
            currentTrackLabel.setText("Unable to fetch track info");
        }
    }
}
