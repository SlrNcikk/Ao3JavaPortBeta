package JavaBeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
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
    @FXML private Label songTitle;
    @FXML private Label artistName;
    @FXML private ImageView albumCover;
    @FXML private VBox resultsBox;
    @FXML private Label currentTrackLabel;

    private Timeline trackRefreshTimeline;
    private Map<String, String> lastSearchResults = new HashMap<>();

    @FXML
    private void initialize() {
        searchButton.setOnAction(e -> onSearch());
        playButton.setOnAction(e -> onPlay());
        pauseButton.setOnAction(e -> onPause());
        nextButton.setOnAction(e -> onNext());
        previousButton.setOnAction(e -> onPrevious());
        refreshButton.setOnAction(e -> onRefresh());
        loginButton.setOnAction(e -> onLogin());

        searchResultsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selected = searchResultsList.getSelectionModel().getSelectedItem();
                if (selected != null && lastSearchResults.containsKey(selected)) {
                    String uri = lastSearchResults.get(selected);
                    new Thread(() -> {
                        try {
                            SpotifyService.playTrack(uri);
                            Platform.runLater(() -> currentTrackLabel.setText("Playing: " + selected));
                        } catch (IOException | InterruptedException ex) {
                            ex.printStackTrace();
                            Platform.runLater(() -> currentTrackLabel.setText("Failed to play: " + selected));
                        }
                    }).start();
                }
            }
        });

        startTrackRefresh();
    }

    private void startTrackRefresh() {
        trackRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), event -> refreshTrackInfo())
        );
        trackRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        trackRefreshTimeline.play();
    }

    @FXML
    private void onSearch() {
        String query = searchField.getText();
        if (query == null || query.isBlank()) return;

        new Thread(() -> {
            try {
                Map<String, String> results = SpotifyService.searchAll(query, 5);
                Platform.runLater(() -> {
                    searchResultsList.getItems().clear();
                    if (results.isEmpty()) {
                        searchResultsList.getItems().add("No results found.");
                        return;
                    }
                    results.keySet().forEach(searchResultsList.getItems()::add);
                    lastSearchResults = results;
                    String first = results.keySet().iterator().next();
                    songTitle.setText(first);
                    artistName.setText("Tap a result to play");
                    albumCover.setImage(new Image("https://upload.wikimedia.org/wikipedia/commons/3/3c/Spotify_icon.svg"));
                    currentTrackLabel.setText("Found " + results.size() + " results for \"" + query + "\"");
                });
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> currentTrackLabel.setText("Search failed: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onRefresh() {
        new Thread(() -> {
            try {
                SpotifyService.findAndSetDeviceId();
                refreshTrackInfo();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> currentTrackLabel.setText("Failed to refresh."));
            }
        }).start();
    }

    @FXML
    private void onLogin() {
        // Disable login button immediately and show progress
        loginButton.setText("Logging in...");
        loginButton.setDisable(true);

        // Start authentication on a background thread
        new Thread(() -> {
            try {
                // Perform Spotify authentication
                SpotifyService.authenticate();

                // SUCCESS: update UI on JavaFX thread
                Platform.runLater(() -> {
                    loginButton.setText("Logged In!");
                    refreshTrackInfo();  // refresh now that authentication is done
                });

            } catch (Exception e) {
                e.printStackTrace();

                // FAILURE: update UI so user can try again
                Platform.runLater(() -> {
                    loginButton.setText("Login Failed! Try Again.");
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }


    @FXML private void onPlay() { executePlayerAction(() -> SpotifyService.play()); }
    @FXML private void onPause() { executePlayerAction(() -> SpotifyService.pause()); }
    @FXML private void onNext() { executePlayerAction(() -> SpotifyService.nextTrack()); }
    @FXML private void onPrevious() { executePlayerAction(() -> SpotifyService.previousTrack()); }

    private void refreshTrackInfo() {
        new Thread(() -> {
            try {
                JsonObject trackJson = SpotifyService.getCurrentTrackJson();
                if (trackJson == null) {
                    Platform.runLater(() -> {
                        currentTrackLabel.setText("No track playing.");
                        songTitle.setText("-");
                        artistName.setText("-");
                        albumCover.setImage(new Image("https://upload.wikimedia.org/wikipedia/commons/3/3c/Spotify_icon.svg"));
                    });
                    return;
                }

                JsonObject item = trackJson.getAsJsonObject("item");
                if (item == null) return;

                String trackName = item.get("name").getAsString();
                String artist = item.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                String imageUrl = item.getAsJsonObject("album").getAsJsonArray("images").get(0).getAsJsonObject().get("url").getAsString();

                Platform.runLater(() -> {
                    songTitle.setText(trackName);
                    artistName.setText(artist);
                    albumCover.setImage(new Image(imageUrl, true));
                    currentTrackLabel.setText("Now Playing: " + trackName + " - " + artist);
                });
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> currentTrackLabel.setText("Unable to fetch track info."));
            }
        }).start();
    }

    private void executePlayerAction(PlayerAction action) {
        new Thread(() -> {
            try {
                action.run();
                refreshTrackInfo();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Platform.runLater(() -> currentTrackLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    @FunctionalInterface
    private interface PlayerAction {
        void run() throws IOException, InterruptedException;
    }
}
