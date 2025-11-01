package JavaBeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.application.Platform;

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
        // Button actions
        searchButton.setOnAction(e -> onSearch());
        playButton.setOnAction(e -> onPlay());
        pauseButton.setOnAction(e -> onPause());
        nextButton.setOnAction(e -> onNext());
        previousButton.setOnAction(e -> onPrevious());
        refreshButton.setOnAction(e -> onRefresh());
        loginButton.setOnAction(e -> onLogin());

        // Double-click search result → play
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

        // Start refreshing track info automatically
        refreshTrackInfo();
        startTrackRefresh();
    }

    /** Automatically refresh track info every 10 seconds */
    private void startTrackRefresh() {
        trackRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), event -> refreshTrackInfo())
        );
        trackRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        trackRefreshTimeline.play();
    }

    /** Search Spotify */
    @FXML
    private void onSearch() {
        String query = searchField.getText();
        if (query == null || query.isBlank()) return;

        new Thread(() -> {
            try {
                Map<String, String> results = SpotifyService.searchAll(query, 5);
                System.out.println("Raw Spotify search results: " + results);

                Platform.runLater(() -> {
                    searchResultsList.getItems().clear();
                    if (results.isEmpty()) {
                        searchResultsList.getItems().add("No results found.");
                        return;
                    }

                    results.keySet().forEach(searchResultsList.getItems()::add);
                    lastSearchResults = results;

                    currentTrackLabel.setText("Found " + results.size() + " results for \"" + query + "\"");

                    String first = results.keySet().iterator().next();
                    songTitle.setText(first);
                    artistName.setText("Tap a result to play");
                    albumCover.setImage(new Image("https://upload.wikimedia.org/wikipedia/commons/3/3c/Spotify_icon.svg"));
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
        // 1. Update UI immediately to show work is starting
        loginButton.setText("Logging in...");
        loginButton.setDisable(true);

        // 2. Start the authentication on a new background thread
        new Thread(() -> {
            try {
                // 3. This is the long, blocking call.
                // It runs safely in the background.
                SpotifyService.authenticate();

                // 4. --- SUCCESS ---
                // Authentication is done! Send the UI updates
                // back to the JavaFX Application Thread.
                Platform.runLater(() -> {
                    loginButton.setText("Logged In!");
                    refreshTrackInfo(); // Now it's safe to refresh
                });

            } catch (Exception e) {
                // 5. --- FAILURE ---
                // If auth fails, print the error
                e.printStackTrace();

                // Let the user try again
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

    /** Refresh current track info */
    private void refreshTrackInfo() {
        new Thread(() -> {
            try {
                String json = SpotifyService.getCurrentTrackJson();
                if (json == null || json.isBlank()) {
                    Platform.runLater(() -> {
                        currentTrackLabel.setText("No track playing.");
                        songTitle.setText("-");
                        artistName.setText("-");
                        albumCover.setImage(new Image("https://upload.wikimedia.org/wikipedia/commons/3/3c/Spotify_icon.svg"));
                    });
                    return;
                }

                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject item = root.getAsJsonObject("item");
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

    /** Utility to execute play/pause/next/previous safely */
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
