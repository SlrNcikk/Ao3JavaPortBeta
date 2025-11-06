package JavaBeta;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue; // Import for animation
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.TilePane; // Import for TilePane
import javafx.util.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kordamp.ikonli.elusive.Elusive;
import org.kordamp.ikonli.entypo.Entypo;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.*; // Keep for Desktop
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Keep for Alert
import java.util.StringJoiner; // Keep for buildSearchQuery
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Ao3Controller {

    // --- REMOVED FXML UI Elements ---
    // (Old fields like searchBox, resultsTableView, etc., are gone)
    // @FXML private TableView<Work> resultsTableView;
    // @FXML private TableColumn<Work, String> titleColumn;

    // --- NEW FXML UI Elements ---
    @FXML private SplitPane mainSplitPane;
    @FXML private VBox createPane;
    @FXML private Button addFicButton;
    @FXML private Button deleteButton; // This should match the fx:id in your new FXML
    @FXML private ListView<Work> unlistedListView; // Replaces resultsTableView
    @FXML private TilePane libraryTilePane; // Replaces libraryTableView
    // @FXML private HBox libraryControlsBox; // This is now gone from the FXML

    // --- Existing FXML UI Elements (Kept) ---
    @FXML private Button modeSwitchButton;
    @FXML private ImageView modeImageView;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label timeLabel;
    @FXML private Button settingsButton;
    @FXML private Button themeButton;
    @FXML private FontIcon settingsIcon;
    @FXML private FontIcon themeIcon;

    private Path libraryPath;
    private Timeline clockTimeline;
    private boolean isOnlineMode = true; // You'll need to adapt this
    private Image onlineModeIcon;
    private Image offlineModeIcon;
    private String darkThemePath;

    // --- (handleLaunchChat, openSpotify, openAuthorProfile methods are unchanged) ---
    @FXML
    private void handleLaunchChat() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("chat-view.fxml"));
            Parent root = fxmlLoader.load();
            Stage chatStage = new Stage();
            final int PEER_PORT = 5000;
            ChatController chatController = fxmlLoader.getController();
            P2PPeer peer = new P2PPeer(PEER_PORT, chatController);
            chatController.setPeer(peer);
            peer.startListening();
            chatStage.setTitle("P2P Chat - Listening on Port " + PEER_PORT);
            chatStage.setScene(new Scene(root, 600, 400));
            chatStage.setOnCloseRequest(e -> peer.stopListening());
            chatStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading chat view: " + e.getMessage());
        }
    }

    @FXML
    private void openSpotify() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/SpotifyView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Spotify Player");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openAuthorProfile(Work work) {
        if (work == null) return;

        if (work.getAuthorUrl() == null) {
            System.out.println("Cannot open profile for " + work.getAuthor() + ". No URL provided.");
            showInfo("Profile Not Available", "Cannot open a profile for '" + work.getAuthor() + "'.");
            return;
        }

        System.out.println("--- OPENING AUTHOR PROFILE ---");
        System.out.println("Author Name: " + work.getAuthor());
        System.out.println("Author URL: " + work.getAuthorUrl());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/AuthorProfileView.fxml"));
            Parent root = loader.load();
            AuthorProfileController controller = loader.getController();
            controller.loadAuthorData(work.getAuthor(), work.getAuthorUrl());
            Stage stage = new Stage();
            stage.setTitle(work.getAuthor() + "'s Profile");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not load the Author Profile window: " + e.getMessage());
        }
    }


    @FXML
    public void initialize() {
        // --- Load Mode Icons ---
        try {
            onlineModeIcon = new Image(getClass().getResourceAsStream("/JavaBeta/oslm.png"));
            offlineModeIcon = new Image(getClass().getResourceAsStream("/JavaBeta/ofslm.png"));
            if (modeImageView != null) {
                modeImageView.setImage(offlineModeIcon); // Set default icon
                Tooltip.install(modeSwitchButton, new Tooltip("Switch Mode"));
            }
        } catch (Exception e) {
            System.err.println("ERROR: Could not load mode icon images!");
        }

        // --- Setup Button Icons ---
        if (settingsIcon != null) settingsIcon.setIconCode(Elusive.COG);
        if (themeIcon != null) themeIcon.setIconCode(Entypo.ADJUST);
        // Note: deleteIcon is now associated with 'deleteButton'
        // if (deleteIcon != null) deleteIcon.setIconCode(FontAwesomeSolid.TRASH);

        // Setup Library Path
        try {
            libraryPath = Paths.get(System.getProperty("user.home"), "AO3_Offline_Library");
            if (!Files.exists(libraryPath)) Files.createDirectories(libraryPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- NEW: Configure ListView ---
        if (unlistedListView != null) {
            unlistedListView.setPlaceholder(new Label("No unlisted fics found."));
            // TODO: Set a custom CellFactory for unlistedListView

            // Example double-click to read:
            unlistedListView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Work selected = unlistedListView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        // Assuming this is an "online" work
                        loadAndShowStory(selected, unlistedListView.getItems());
                    }
                }
            });
        }

        // --- NEW: Configure TilePane ---
        if (libraryTilePane != null) {
            // You will populate this in a method like populateLibrary()
            // TODO: Add click listener to tiles
        }

        // --- Initialize Split Pane State ---
        if (createPane != null) {
            createPane.setVisible(false);
            createPane.setManaged(false);
        }
        if (mainSplitPane != null) {
            mainSplitPane.setDividerPosition(0, 1.0); // 1.0 = 100% (all the way right)
        }

        // Initialize Time Display
        setupClock();
    }

    // --- NEW METHOD for the "+" Button ---
    @FXML
    private void handleAddFicClick() {
        if (createPane == null || mainSplitPane == null) {
            System.err.println("Error: createPane or mainSplitPane is null. Check FXML.");
            return;
        }

        boolean isVisible = createPane.isVisible();

        if (isVisible) {
            // --- HIDE THE PANEL ---
            createPane.setVisible(false);
            createPane.setManaged(false);
            animateDividerTo(1.0);
        } else {
            // --- SHOW THE PANEL ---
            createPane.setVisible(true);
            createPane.setManaged(true);
            animateDividerTo(0.5); // 50/50 split
        }
    }

    // --- NEW HELPER METHOD for animation ---
    private void animateDividerTo(double position) {
        if (mainSplitPane.getDividers().isEmpty()) return;

        final SplitPane.Divider divider = mainSplitPane.getDividers().get(0);
        final double end = position;

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(divider.positionProperty(), end)
                )
        );
        timeline.play();
    }

    // --- RE-ADDED MISSING METHOD ---
    private void setupClock() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        if (timeLabel != null) {
            timeLabel.setText(LocalTime.now().format(timeFormatter)); // Set initial time
        }
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            Platform.runLater(() -> {
                if (timeLabel != null) {
                    timeLabel.setText(LocalTime.now().format(timeFormatter));
                }
            });
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    /**
     * This method will need to be completely rewritten to work with the new UI.
     * The old logic is commented out.
     */
    @FXML
    protected void switchMode() {
        isOnlineMode = !isOnlineMode;

        // --- NEW LOGIC ---
        // This button might now switch between "Online Search" (Unlisted) and "Offline Library" (TilePane)
        System.out.println("Switch Mode clicked. New state: " + (isOnlineMode ? "Online" : "Offline"));

        // Example:
        unlistedListView.setVisible(isOnlineMode);
        unlistedListView.setManaged(isOnlineMode);
        libraryTilePane.setVisible(!isOnlineMode);
        libraryTilePane.setManaged(!isOnlineMode);
        deleteButton.setVisible(!isOnlineMode);
        deleteButton.setManaged(!isOnlineMode);

        if (isOnlineMode) {
            modeImageView.setImage(offlineModeIcon);
            Tooltip.install(modeSwitchButton, new Tooltip("Switch to Offline Library"));
        } else {
            modeImageView.setImage(onlineModeIcon);
            Tooltip.install(modeSwitchButton, new Tooltip("Switch to Online Search"));
            // You would call your new populateLibrary() method here
            // populateLibraryTilePane();
        }
    }

    // --- RE-ADDED/MODIFIED METHODS (to be implemented) ---

    @FXML
    protected void onDeleteStoryClick() {
        // TODO: This logic needs to change. You need to get the "selected" item
        // from your TilePane, which requires custom logic.
        System.out.println("Delete button clicked.");
        // Work selectedWork = libraryTilePane.getSelectionModel().getSelectedItem(); // TilePane doesn't have this
        // ... (old delete logic) ...
    }

    @FXML
    protected void onViewFolderClick() {
        // ... (This method is probably gone, but keeping it just in case)
    }

    @FXML
    protected void onSearchButtonClick() {
        // TODO: You have no search button. You need to add one
        // or call createFetchWorksTask from somewhere else.
    }

    private void populateLibraryListView() {
        // TODO: This needs to be rewritten to populate the libraryTilePane
        // with VBox "cells" (ImageView + Label) instead of the TableView.
    }

    private void loadAndShowStory(Work work, List<Work> allWorks) {
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Loading Story");
        loadingAlert.setHeaderText("Please wait, fetching story content...");
        loadingAlert.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        loadingAlert.show();

        Task<String> task = createFetchStoryTask(work);
        task.setOnSucceeded(e -> {
            loadingAlert.close();
            launchReadingWindow(work, task.getValue(), allWorks);
        });
        task.setOnFailed(e -> {
            loadingAlert.close();
            showError("Failed to load story content.");
            e.getSource().getException().printStackTrace();
        });
        new Thread(task).start();
    }

    private void loadStoryFromLibrary(Work work) {
        if (work == null) return;
        Path filePath = Paths.get(work.getUrl());
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            launchReadingWindow(work.getTitle(), content, true); // Pass flag for offline
        } catch (IOException | java.nio.file.InvalidPathException e) {
            showError("Could not read story file: " + e.getMessage());
        }
    }

    private void launchReadingWindow(Work work, String content, List<Work> allWorks) {
        launchReadingWindowInternal(work.getTitle(), content, work, false, allWorks);
    }

    private void launchReadingWindow(String title, String content, boolean isOffline) {
        launchReadingWindowInternal(title, content, null, isOffline, null);
    }

    private void launchReadingWindowInternal(String title, String content, Work work, boolean isOffline, List<Work> allWorks) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/ReadingView.fxml"));
            Parent root = loader.load();
            ReadingController readingController = loader.getController();
            Stage readerStage = new Stage();
            Scene scene = new Scene(root);
            readerStage.setScene(scene);
            readerStage.setTitle(title);
            if (isOffline) {
                readingController.loadStory(title, content, true);
            } else if (work != null) {
                readingController.loadStory(work, content, allWorks);
            } else {
                showError("Error determining story type for reader window.");
                return;
            }
            readerStage.initModality(Modality.NONE);
            readerStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not open the reading window: " + e.getMessage());
        }
    }

    // --- RE-ADDED HELPER METHODS ---
    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    private void showInfo(String message) { showInfo(null, message); }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    protected void onSettingsButtonClick() {
        System.out.println("Settings button clicked!");
        showInfo("Settings", "Settings functionality not yet implemented.");
    }

    @FXML
    protected void onThemeButtonClick() {
        if (themeButton == null || themeButton.getScene() == null) {
            System.err.println("Error: Could not get scene to toggle theme.");
            return;
        }
        Scene scene = themeButton.getScene();
        String cssPath = "/JavaBeta/styles.css";
        if (darkThemePath == null) {
            URL stylesheetUrl = getClass().getResource(cssPath);
            if (stylesheetUrl != null) {
                darkThemePath = stylesheetUrl.toExternalForm();
            } else {
                showError("Could not find the theme stylesheet (" + cssPath + ")!");
                themeButton.setDisable(true);
                return;
            }
        }
        boolean isDarkMode = scene.getStylesheets().contains(darkThemePath);
        if (isDarkMode) {
            scene.getStylesheets().remove(darkThemePath);
            System.out.println("DEBUG: Dark theme removed.");
        } else {
            scene.getStylesheets().add(darkThemePath);
            System.out.println("DEBUG: Dark theme applied.");
        }
    }

    // --- RE-ADDED WEB SCRAPING TASKS ---
    private Task<List<Work>> createFetchWorksTask(String query) {
        return new Task<>() {
            @Override
            protected List<Work> call() throws Exception {
                List<Work> worksList = new ArrayList<>();
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
                String url = "https://archiveofourown.org/works/search?work_search[query]=" + encodedQuery;
                System.out.println("DEBUG: Connecting to URL -> " + url);
                try {
                    Document doc = Jsoup.connect(url)
                            .userAgent(userAgent)
                            .referrer("https.www.google.com")
                            .get();
                    Elements workElements = doc.select("li.work.blurb");
                    System.out.println("DEBUG: Connection successful. Found " + workElements.size() + " works on the page.");
                    for (Element workEl : workElements) {
                        Element titleEl = workEl.selectFirst("h4.heading a[href^='/works/']");
                        Element authorEl = workEl.selectFirst("a[rel=author]");
                        Element dateEl = workEl.selectFirst("p.datetime");
                        Elements tagElements = workEl.select("ul.tags a.tag");
                        if (titleEl != null && dateEl != null) {
                            String title = titleEl.text();
                            String workUrl = "https://archiveofourown.org" + titleEl.attr("href");
                            String author;
                            String authorUrl;
                            if (authorEl != null) {
                                author = authorEl.text();
                                if (author.equals("orphan_account") || author.equals("Anonymous")) {
                                    authorUrl = null;
                                } else {
                                    authorUrl = "https://archiveofourown.org" + authorEl.attr("href");
                                }
                            } else {
                                Element headingEl = workEl.selectFirst("h4.heading");
                                String headingText = (headingEl != null) ? headingEl.text() : "";
                                String[] headingParts = headingText.split(" by ");
                                if (headingParts.length > 1) {
                                    author = headingParts[1];
                                } else {
                                    author = "Anonymous";
                                }
                                authorUrl = null;
                            }
                            String lastUpdated = dateEl.text();
                            List<String> tagsList = tagElements.stream().map(Element::text).collect(Collectors.toList());
                            String tags = String.join(", ", tagsList);
                            Work newWork = new Work(title, author, workUrl, tags, lastUpdated);
                            newWork.setAuthorUrl(authorUrl);
                            worksList.add(newWork);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("DEBUG: Scraping failed!");
                    e.printStackTrace();
                    throw e;
                }
                if (!worksList.isEmpty()) {
                    Thread.sleep(300);
                }
                return worksList;
            }
        };
    }

    private Task<String> createFetchStoryTask(Work work) {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
                Document doc = Jsoup.connect(work.getUrl() + "?view_full_work=true").userAgent(userAgent).get();
                Element workskin = doc.selectFirst("#workskin");
                if (workskin == null) {
                    return "<html><body>Could not find story content. It might be a restricted work.</body></html>";
                }
                return workskin.html(); // Return HTML content
            }
        };
    }
}