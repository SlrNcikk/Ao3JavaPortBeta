package JavaBeta;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.io.IOException;

public class SpotifyController {

    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button nextButton;
    @FXML private Button previousButton;
    @FXML private Button loginButton;
    @FXML private Button refreshButton;
    @FXML private Label currentTrackLabel; // <-- Use this instead of trackLabel

    private Timeline trackRefreshTimeline;

    @FXML
    private void initialize() {
        refreshTrackInfo(); // Initial fetch
        startTrackRefresh(); // Start automatic updates every 10 seconds

        // Button actions
        playButton.setOnAction(e -> onPlay());
        pauseButton.setOnAction(e -> onPause());
        nextButton.setOnAction(e -> onNext());
        previousButton.setOnAction(e -> onPrevious());
        refreshButton.setOnAction(e -> onRefresh());
        loginButton.setOnAction(e -> onLogin());
    }

    // Timeline that updates currentTrackLabel every 10 seconds
    private void startTrackRefresh() {
        trackRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), event -> refreshTrackInfo())
        );
        trackRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        trackRefreshTimeline.play();
    }

    @FXML
    private void onRefresh() {
        try {
            SpotifyService.findAndSetDeviceId(); // Make sure we have the active device
            refreshTrackInfo();
        } catch (Exception e) {
            e.printStackTrace();
            currentTrackLabel.setText("Failed to refresh.");
        }
    }

    @FXML
    private void onLogin() {
        try {
            SpotifyService.authenticate();
            loginButton.setText("Logged In!");
            loginButton.setDisable(true);
            refreshTrackInfo();
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

    // Fetch current track from SpotifyService and update currentTrackLabel
    private void refreshTrackInfo() {
        try {
            String trackInfo = SpotifyService.getCurrentTrack();
            currentTrackLabel.setText(trackInfo);
        } catch (Exception e) {
            currentTrackLabel.setText("Unable to fetch track info");
            e.printStackTrace();
        }
    }
}
