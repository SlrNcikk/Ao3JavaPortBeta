package JavaBeta;

import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.lang.InterruptedException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static JavaBeta.SpotifyService.*;

public class SpotifyController {
    private Map<String, String> currentSearchResults = new HashMap<>();

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

    private Timeline trackRefreshTimeline;
    private String currentAlbumCoverUrl = "";
    @FXML
    private BorderPane rootVBox;

    @FXML
    private ToggleButton themeToggleButton;

    @FXML
    private void initialize() {
        // This line is correct
        searchButton.setOnAction(e -> onSearch(e));
        playButton.setOnAction(e -> onPlay());
        pauseButton.setOnAction(e -> onPause());
        nextButton.setOnAction(e -> onNext());
        previousButton.setOnAction(e -> onPrevious());
        refreshButton.setOnAction(e -> onRefresh());
        loginButton.setOnAction(e -> onLogin());

        // --- THIS IS THE CORRECTED CLICK LISTENER ---
        searchResultsList.setOnMouseClicked(e -> {
            // 1. Get the selected item *first*
            String selected = searchResultsList.getSelectionModel().getSelectedItem();

            // 2. Check for a double-click
            if (e.getClickCount() == 2) {

                // 3. Now check if the item is valid and in our map
                if (selected != null && currentSearchResults.containsKey(selected)) {
                    String uri = currentSearchResults.get(selected);

                    new Thread(() -> {
                        try {
                            playTrack(uri);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Platform.runLater(() -> songTitle.setText("Failed to play: " + selected));
                        }
                    }).start();
                }
            }
        });

        startTrackRefresh();
    }
    private void startTrackRefresh() {
        trackRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> refreshTrackInfo())
        );
        trackRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        trackRefreshTimeline.play();
    }

    @FXML
    void onSearch(ActionEvent event) {
        String query = searchField.getText().trim();
        if (query == null || query.isBlank()) {
            return;
        }

        // Check for "track/", "album/", or "playlist/" anywhere in the text
        boolean isUrl = query.contains("/track/") || query.contains("/album/") || query.contains("/playlist/");

        if (isUrl) {
            // --- IT'S A URL: PARSE AND PLAY ---
            new Thread(() -> {
                try {
                    // 1. Determine the type
                    String type = null;
                    if (query.contains("/track/")) type = "track";
                    else if (query.contains("/album/")) type = "album";
                    else if (query.contains("/playlist/")) type = "playlist";

                    if (type == null) return; // Not a valid URL

                    // 2. Find the ID part
                    // Get the text starting from "/track/" (or album, etc.)
                    String idSection = query.substring(query.indexOf("/" + type + "/"));

                    // Remove the "/track/" part itself
                    idSection = idSection.substring(type.length() + 2); // +2 for the slashes

                    // 3. Clean the ID (removes ?si=... or trailing slashes)
                    String id = idSection.split("[?/]")[0]; // Splits on the first '?' or '/'

                    if (id.isBlank()) {
                        System.err.println("Could not parse ID from URL: " + query);
                        return;
                    }

                    // 4. Construct the official Spotify URI
                    String spotifyUri = "spotify:" + type + ":" + id;

                    // 5. Play it!
                    playTrack(spotifyUri);

                    // 6. (Optional) Clear the search list and field
                    Platform.runLater(() -> {
                        searchResultsList.getItems().clear();
                        searchField.clear();
                    });

                } catch (Exception e) {
                    System.err.println("Failed to parse or play Spotify URL: " + query);
                    e.printStackTrace();
                }
            }).start();

        } else {
            // --- IT'S A SEARCH TERM: DO THE NORMAL SEARCH ---
            new Thread(() -> {
                try {
                    // This is your original search logic
                    Map<String, String> results = searchAll(query, 10);

                    // Save the results to our class variable
                    this.currentSearchResults = results;

                    // Update UI on the JavaFX thread
                    Platform.runLater(() -> {
                        searchResultsList.getItems().clear();
                        searchResultsList.getItems().addAll(results.keySet());
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    private void onLogin() {
        System.out.println("Login clicked");
        // Start authentication in a new thread
        executePlayerAction(() -> authenticate());
    }

    @FXML
    private void onRefresh() {
        System.out.println("Refresh clicked");
        refreshTrackInfo();
    }

    @FXML
    void onToggleTheme(ActionEvent event) {
        var scene = rootVBox.getScene();
        if (scene == null) return; // Scene might not be ready

        scene.getStylesheets().clear();

        if (themeToggleButton.isSelected()) {
            // --- DARK MODE ---
            scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
            themeToggleButton.setText("Light Mode");
        } else {
            // --- LIGHT MODE ---
            scene.getStylesheets().add(getClass().getResource("light-theme.css").toExternalForm());
            themeToggleButton.setText("Dark Mode");
        }
    }


    @FXML private void onPlay() { executePlayerAction(() -> play()); }
    @FXML private void onPause() { executePlayerAction(() -> pause()); }
    @FXML private void onNext() { executePlayerAction(() -> nextTrack()); }
    @FXML private void onPrevious() { executePlayerAction(() -> previousTrack()); }

    private void refreshTrackInfo() {
        new Thread(() -> {
            try {
                JsonObject trackJson = getCurrentTrackJson();

                if (trackJson == null) {
                    // --- A: NOTHING IS PLAYING (204 No Content) ---
                    Platform.runLater(() -> {
                        songTitle.setText("Nothing Playing");
                        artistName.setText("-");

                        if (currentAlbumCoverUrl != null) {
                            currentAlbumCoverUrl = null;
                            albumCover.setImage(null);
                        }
                    });
                    return; // Stop the thread
                }

                // Check if 'item' is null (e.g., an Ad is playing)
                if (!trackJson.has("item") || trackJson.get("item").isJsonNull()) {
                    Platform.runLater(() -> {
                        songTitle.setText("Playback Paused (or Ad)");
                        artistName.setText("-");
                        currentAlbumCoverUrl = null;
                        albumCover.setImage(null);
                    });
                    return; // Stop the thread
                }

                // --- B: SOMETHING IS PLAYING ---
                // Now it's safe to get the item
                JsonObject item = trackJson.getAsJsonObject("item");

                String trackName = item.get("name").getAsString();
                String artist = item.get("artists").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString();
                String newAlbumCoverUrl = item.get("album").getAsJsonObject().get("images").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();

                // Now update the UI
                Platform.runLater(() -> {
                    songTitle.setText(trackName);
                    artistName.setText(artist);

                    if (!newAlbumCoverUrl.equals(currentAlbumCoverUrl)) {
                        currentAlbumCoverUrl = newAlbumCoverUrl;
                        albumCover.setImage(new Image(currentAlbumCoverUrl));
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    songTitle.setText("Error on refresh");
                    artistName.setText(e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void executePlayerAction(PlayerAction action) {
        new Thread(() -> {
            try {
                action.run(); // Try to run the command (play, pause, etc.)
            } catch (Exception e) {
                String msg = e.getMessage();

                // Check if this is the harmless "spam-click" error
                if (msg != null && msg.contains("Restriction violated")) {
                    // Benign error, just log it. We don't need to show it to the user.
                    System.out.println("Ignoring benign 403 error (e.g., play while playing).");
                } else {
                    // This is a REAL error (like "No device found")
                    e.printStackTrace();
                    Platform.runLater(() -> songTitle.setText("Error: " + e.getMessage()));
                }
            } finally {
                // ALWAYS refresh the UI after any command, whether it
                // succeeded or failed, to show the true current state.
                refreshTrackInfo();
            }
        }).start();
    }

    @FunctionalInterface
    private interface PlayerAction {
        void run() throws IOException, InterruptedException;
    }
}
