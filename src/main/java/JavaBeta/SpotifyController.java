package JavaBeta;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SpotifyController {

    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button nextButton;
    @FXML private Button prevButton;
    @FXML private Label currentTrackLabel;

    @FXML
    private void initialize() {
        refreshTrackInfo();
    }

    @FXML
    private void onPlayClicked() {
        try {
            SpotifyService.play();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onPauseClicked() {
        try {
            SpotifyService.pause();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onNextClicked() {
        try {
            SpotifyService.nextTrack();
            refreshTrackInfo();
        } catch (Exception e) {
            currentTrackLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onPrevClicked() {
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
