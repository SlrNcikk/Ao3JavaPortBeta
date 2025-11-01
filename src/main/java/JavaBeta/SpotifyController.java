package JavaBeta;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.io.IOException;
import java.util.Map;

public class SpotifyController {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private ListView<String> searchResultsList;
    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button nextButton;
    @FXML private Button previousButton;
    @FXML private Button loginButton;
    @FXML private Button refreshButton;
    @FXML private Label currentTrackLabel; // replaces trackLabel

    private Timeline trackRefreshTimeline;

    @FXML
    private void initialize() {
        // Button actions
        searchButton.setOnAction(e -> onSearch());
        playButton.setOnAction(e -> onPlay());
        pauseButton.setOnAction(e -> onPause());
        nextButton.setOnAction(e -> onNext());
        previousButton.setOnAction(e -> onPrevious());
        refreshButton.setOnAction(e -> onRefresh());
        loginButton.setOnAction(e -> onLogin());

        // Search result click → play
        searchResultsList.setOnMouseClicked(e -> {
            String selected = searchResultsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String uri = SpotifyService.getUriForTrack(selected);
                if (uri != null) {
                    try {
                        SpotifyService.playTrack(uri);
                        currentTrackLabel.setText("Playing: " + selected);
                    } catch (IOException ex) {
                        currentTrackLabel.setText("Playback failed.");
                        ex.printStackTrace();
                    }
                }
            }
        });

        // Start refreshing track info automatically
        refreshTrackInfo();
        startTrackRefresh();
    }

    /** 🔁 Automatically refresh track info every 10 seconds */
    private void startTrackRefresh() {
        trackRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), event -> refreshTrackInfo())
        );
        trackRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        trackRefreshTimeline.play();
    }

    /** 🔍 Handles music search */
    @FXML
    private void onSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            currentTrackLabel.setText("Enter a song name to search.");
            return;
        }

        try {
            Map<String, String> tracks = SpotifyService.searchTracks(query);
            searchResultsList.getItems().clear();
            searchResultsList.getItems().addAll(tracks.keySet());
            currentTrackLabel.setText("Select a track to play.");
        } catch (Exception e) {
            e.printStackTrace();
            currentTrackLabel.setText("Search failed: " + e.getMessage());
        }
    }

    /** 🔁 Refresh device and track info */
    @FXML
    private void onRefresh() {
        try {
            SpotifyService.findAndSetDeviceId();
            refreshTrackInfo();
        } catch (Exception e) {
            e.printStackTrace();
            currentTrackLabel.setText("Failed to refresh.");
        }
    }

    /** 🔑 Log in to Spotify */
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

    /** ▶️ Play */
    @FXML
    private void onPlay() {
        try {
            SpotifyService.play();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    /** ⏸ Pause */
    @FXML
    private void onPause() {
        try {
            SpotifyService.pause();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    /** ⏭ Next track */
    @FXML
    private void onNext() {
        try {
            SpotifyService.nextTrack();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    /** ⏮ Previous track */
    @FXML
    private void onPrevious() {
        try {
            SpotifyService.previousTrack();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    /** 🎵 Get current playing track */
    private void refreshTrackInfo() {
        try {
            String trackInfo = SpotifyService.getCurrentTrack();
            currentTrackLabel.setText(trackInfo);
        } catch (Exception e) {
            currentTrackLabel.setText("Unable to fetch track info.");
            e.printStackTrace();
        }
    }
}
