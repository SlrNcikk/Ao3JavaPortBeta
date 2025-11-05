package JavaBeta;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.awt.Desktop; // Keep for onViewFolderClick
import java.util.Optional; // Keep for onDeleteStoryClick
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
// --- Ikonli Imports ---
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.elusive.Elusive;
import org.kordamp.ikonli.entypo.Entypo;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid; // For the trash icon
// --- END Ikonli Imports ---
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
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
import java.util.StringJoiner;
import java.util.stream.Collectors; // NEW IMPORT
import java.util.stream.Stream;

public class Ao3Controller {

    // --- FXML UI Elements (Corrected) ---
    @FXML private TextField anyField, titleField, authorField, tagsField;
    @FXML private Button searchButton, refreshLibraryButton, clearButton;
    @FXML private Button modeSwitchButton;
    @FXML private ImageView modeImageView;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label timeLabel;
    @FXML private VBox searchBox;

    // --- Online TableView ---
    @FXML private TableView<Work> resultsTableView;
    @FXML private TableColumn<Work, String> titleColumn;
    @FXML private TableColumn<Work, String> authorColumn;
    @FXML private TableColumn<Work, String> tagsColumn;
    @FXML private TableColumn<Work, String> updatedColumn;

    // --- Library TableView and Controls (Corrected) ---
    @FXML private TableView<Work> libraryTableView; // Corrected variable name
    @FXML private TableColumn<Work, String> libraryTitleColumn;
    @FXML private TableColumn<Work, String> libraryAuthorColumn;
    @FXML private TableColumn<Work, String> libraryTagsColumn;
    @FXML private TableColumn<Work, String> libraryUpdatedColumn;
    @FXML private HBox libraryControlsBox;
    @FXML private Button deleteStoryButton;
    @FXML private Button viewFolderButton;
    @FXML private FontIcon deleteIcon; // Icon for delete button

    // --- Icon/Theme Buttons ---
    @FXML private Button settingsButton;
    @FXML private Button themeButton;
    @FXML private FontIcon settingsIcon;
    @FXML private FontIcon themeIcon;

    private Path libraryPath;
    private Timeline clockTimeline;
    private boolean isOnlineMode = true;
    private Image onlineModeIcon;
    private Image offlineModeIcon;
    private String darkThemePath;

    @FXML
    private void handleLaunchChat() {
        try {
            // 1. Load the Chat Application FXML
            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("chat-view.fxml"));
            Parent root = fxmlLoader.load();

            // 2. Initialize the Chat Logic (Same as what was in ChatApplication.start)
            Stage chatStage = new Stage();

            // NOTE: The PEER_PORT needs to be dynamic if you plan to launch
            // multiple chat instances from the same main application in the future.
            final int PEER_PORT = 5000;

            // Initialize the networking core and inject it into the controller
            ChatController chatController = fxmlLoader.getController();
            P2PPeer peer = new P2PPeer(PEER_PORT, chatController);

            chatController.setPeer(peer);
            peer.startListening();

            // 3. Display the new chat window
            chatStage.setTitle("P2P Chat - Listening on Port " + PEER_PORT);
            chatStage.setScene(new Scene(root, 600, 400));

            // Optional: Add a listener to stop the peer when the chat window is closed
            chatStage.setOnCloseRequest(e -> peer.stopListening());

            chatStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            // Handle error, e.g., show an Alert
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



    @FXML
    public void initialize() {
        // --- Load Mode Icons ---
        try {
            onlineModeIcon = new Image(getClass().getResourceAsStream("/JavaBeta/oslm.png"));
            offlineModeIcon = new Image(getClass().getResourceAsStream("/JavaBeta/ofslm.png"));
            if (onlineModeIcon == null || offlineModeIcon == null) {
                throw new NullPointerException("One or both mode icon images failed to load from resources.");
            }
            if (modeImageView != null) {
                modeImageView.setImage(offlineModeIcon);
                Tooltip.install(modeSwitchButton, new Tooltip("Switch to Offline Library"));
            } else {
                System.err.println("ERROR: modeImageView is null. Check FXML fx:id.");
                if (modeSwitchButton != null) modeSwitchButton.setDisable(true);
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            System.err.println("ERROR: Could not load mode icon images! Check paths /JavaBeta/oslm.png and /JavaBeta/ofslm.png");
            e.printStackTrace();
            if (modeSwitchButton != null) modeSwitchButton.setDisable(true);
        }
        // --- END Load Mode Icons ---

        // --- Setup Button Icons ---
        if (settingsIcon != null) {
            settingsIcon.setIconCode(Elusive.COG);
        } else {
            System.err.println("WARN: settingsIcon FXML injection failed.");
        }
        if (themeIcon != null) {
            themeIcon.setIconCode(Entypo.ADJUST);
        } else {
            System.err.println("WARN: themeIcon FXML injection failed.");
        }
        if (deleteIcon != null) {
            deleteIcon.setIconCode(FontAwesomeSolid.TRASH);
        } else {
            System.err.println("WARN: deleteIcon FXML injection failed.");
        }
        // --- END Icon Setup ---

        // Setup Library Path
        try {
            libraryPath = Paths.get(System.getProperty("user.home"), "AO3_Offline_Library");
            if (!Files.exists(libraryPath)) Files.createDirectories(libraryPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- Configure Online TableView Columns ---
        // This setup is correct because your new Work.java (File 6)
        // has titleProperty(), authorProperty(), tagsProperty(), etc.
        if (titleColumn != null) { titleColumn.setCellValueFactory(new PropertyValueFactory<>("title")); }
        if (authorColumn != null) { authorColumn.setCellValueFactory(new PropertyValueFactory<>("author")); }
        if (tagsColumn != null) { tagsColumn.setCellValueFactory(new PropertyValueFactory<>("tags")); }
        if (updatedColumn != null) { updatedColumn.setCellValueFactory(new PropertyValueFactory<>("lastUpdated")); }

        // --- Configure Offline TableView Columns ---
        if (libraryTitleColumn != null) { libraryTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title")); }
        if (libraryAuthorColumn != null) { libraryAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author")); }
        if (libraryTagsColumn != null) { libraryTagsColumn.setCellValueFactory(new PropertyValueFactory<>("tags")); }
        if (libraryUpdatedColumn != null) { libraryUpdatedColumn.setCellValueFactory(new PropertyValueFactory<>("lastUpdated")); }

        // --- Original selection listeners (kept but disabled opening on single click) ---
        if (resultsTableView != null) {
            resultsTableView.getSelectionModel().selectedItemProperty().addListener((obs, old, work) -> {
                // removed auto-opening here; replaced below with double-click logic
            });
        }
        if (libraryTableView != null) {
            libraryTableView.getSelectionModel().selectedItemProperty().addListener((obs, old, work) -> {
                // removed auto-opening here; replaced below with double-click logic
            });
        }

        // --- Added double-click handling ---
        if (resultsTableView != null) {
            resultsTableView.setOnMouseClicked(event -> {
                Work selected = resultsTableView.getSelectionModel().getSelectedItem();
                if (selected == null) return;

                if (event.getClickCount() == 1) {
                    System.out.println("Selected (online): " + selected.getTitle());
                } else if (event.getClickCount() == 2) {
                    System.out.println("Opening (online): " + selected.getTitle());

                    // --- CHANGED ---
                    // Get the full list of works from the table
                    List<Work> allWorks = new ArrayList<>(resultsTableView.getItems());
                    // Pass the selected work AND the full list
                    loadAndShowStory(selected, allWorks);
                    // ---
                }
            });
        }

        if (libraryTableView != null) {
            libraryTableView.setOnMouseClicked(event -> {
                Work selected = libraryTableView.getSelectionModel().getSelectedItem();
                if (selected == null) return;

                if (event.getClickCount() == 1) {
                    System.out.println("Selected (library): " + selected.getTitle());
                } else if (event.getClickCount() == 2) {
                    System.out.println("Opening (library): " + selected.getTitle());
                    loadStoryFromLibrary(selected);
                }
            });
        }

        // Initialize Time Display
        setupClock();
    }



    private void setupClock() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        timeLabel.setText(LocalTime.now().format(timeFormatter)); // Set initial time
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            Platform.runLater(() -> timeLabel.setText(LocalTime.now().format(timeFormatter)));
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    /**
     * Switches the view between Online Search and Offline Library
     * and updates the button IMAGE accordingly. (Corrected)
     */
    @FXML
    protected void switchMode() {
        isOnlineMode = !isOnlineMode;

        resultsTableView.setVisible(isOnlineMode);
        resultsTableView.setManaged(isOnlineMode);
        libraryTableView.setVisible(!isOnlineMode); // <-- CORRECTED
        libraryTableView.setManaged(!isOnlineMode); // <-- CORRECTED
        searchBox.setVisible(isOnlineMode);
        searchBox.setManaged(isOnlineMode);

        libraryControlsBox.setVisible(!isOnlineMode);
        libraryControlsBox.setManaged(!isOnlineMode);

        if (onlineModeIcon == null || offlineModeIcon == null || modeImageView == null) {
            System.err.println("ERROR: Cannot switch mode icon - images or ImageView not loaded.");
            return;
        }

        if (isOnlineMode) {
            modeImageView.setImage(offlineModeIcon);
            Tooltip.install(modeSwitchButton, new Tooltip("Switch to Offline Library"));
            if (libraryTableView.getSelectionModel() != null) { // <-- CORRECTED
                libraryTableView.getSelectionModel().clearSelection(); // <-- CORRECTED
            }
        } else {
            modeImageView.setImage(onlineModeIcon);
            Tooltip.install(modeSwitchButton, new Tooltip("Switch to Online Search"));
            populateLibraryListView();
            if (resultsTableView.getSelectionModel() != null) {
                resultsTableView.getSelectionModel().clearSelection();
            }
        }
        loadingIndicator.setVisible(false);
    }

    /**
     * Handles the "Delete Selected" button click. (Corrected)
     */
    @FXML
    protected void onDeleteStoryClick() {
        // 1. Get the selected Work object from the libraryTableView
        Work selectedWork = libraryTableView.getSelectionModel().getSelectedItem(); // <-- CORRECTED

        if (selectedWork == null) {
            showInfo("Please select a story to delete.");
            return;
        }

        // 3. Confirm with the user
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Story");
        confirmAlert.setHeaderText("Are you sure you want to delete this file?");
        confirmAlert.setContentText(selectedWork.getTitle() + " by " + selectedWork.getAuthor());

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 4. Delete the file (path is stored in 'url')
            try {
                Path filePath = Paths.get(selectedWork.getUrl());
                if (Files.deleteIfExists(filePath)) {
                    System.out.println("Deleted file: " + filePath);
                    // 5. Refresh the library list
                    populateLibraryListView();
                } else {
                    showError("Could not delete the file. It may have been moved or deleted already.");
                }
            } catch (IOException | SecurityException | java.nio.file.InvalidPathException e) {
                showError("Error deleting file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles the "View Folder" button click.
     */
    @FXML
    protected void onViewFolderClick() {
        if (libraryPath == null || !Files.exists(libraryPath)) {
            showError("Library folder not found or hasn't been created yet.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(libraryPath.toFile());
            } else {
                showError("Opening folders is not supported on this system.");
            }
        } catch (IOException | UnsupportedOperationException e) {
            showError("Could not open library folder: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void onSearchButtonClick() {
        if (!isOnlineMode) return;
        String query = buildSearchQuery();
        if (query.isEmpty()) {
            showInfo("Please fill in at least one search field.");
            return;
        }
        loadingIndicator.setVisible(true);
        searchButton.setDisable(true);
        clearButton.setDisable(true);
        resultsTableView.getItems().clear();
        Task<List<Work>> fetchWorksTask = createFetchWorksTask(query);
        fetchWorksTask.setOnSucceeded(e -> resultsTableView.getItems().setAll(fetchWorksTask.getValue()));
        fetchWorksTask.setOnFailed(e -> showError("Failed to fetch works. Check connection."));
        fetchWorksTask.runningProperty().addListener((obs, wasRunning, isRunning) -> {
            loadingIndicator.setVisible(isRunning);
            searchButton.setDisable(isRunning);
            clearButton.setDisable(isRunning);
        });
        new Thread(fetchWorksTask).start();
    }

    @FXML
    protected void onClearButtonClick() {
        anyField.clear();
        titleField.clear();
        authorField.clear();
        tagsField.clear();
        if (isOnlineMode) {
            resultsTableView.getItems().clear();
        }
    }

    @FXML
    protected void onRefreshLibraryClick() {
        if (isOnlineMode) return;
        populateLibraryListView();
    }

    /**
     * Populates the offline library TableView. (Corrected)
     */
    private void populateLibraryListView() {
        libraryTableView.getItems().clear(); // <-- CORRECTED
        if (libraryPath == null) {
            System.err.println("ERROR: Library path is null, cannot populate library view.");
            return;
        }

        List<Work> offlineWorks = new ArrayList<>();
        try (Stream<Path> stream = Files.list(libraryPath)) {
            stream
                    .filter(p -> p.toString().toLowerCase().endsWith(".html") || p.toString().toLowerCase().endsWith(".txt"))
                    .forEach(path -> {
                        String filename = path.getFileName().toString().replaceFirst("\\.(txt|html)$", "");
                        String title = "Unknown Title";
                        String author = "Unknown Author";

                        int separatorIndex = filename.lastIndexOf(" - ");
                        if (separatorIndex != -1) {
                            title = filename.substring(0, separatorIndex).trim();
                            author = filename.substring(separatorIndex + 3).trim();
                        } else {
                            title = filename;
                        }

                        // Store file path in 'url' field, "N/A" for tags/date
                        // We use the new Work.java constructor
                        offlineWorks.add(new Work(title, author, path.toFile().getAbsolutePath(), "N/A", "N/A"));
                    });

            libraryTableView.getItems().addAll(offlineWorks); // <-- CORRECTED

        } catch (IOException e) {
            System.err.println("ERROR: Could not read library directory: " + libraryPath);
            e.printStackTrace();
            showError("Could not read library directory: " + e.getMessage());
        }
    }

    // --- Methods for loading stories and launching reader ---

    // --- CHANGED ---
    // This method now takes the full list of works to pass it along.
    private void loadAndShowStory(Work work, List<Work> allWorks) {
        // ---
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Loading Story");
        loadingAlert.setHeaderText("Please wait, fetching story content...");
        loadingAlert.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        loadingAlert.show();

        Task<String> task = createFetchStoryTask(work);
        task.setOnSucceeded(e -> {
            loadingAlert.close();
            // --- CHANGED ---
            // Pass the allWorks list to the launch method
            launchReadingWindow(work, task.getValue(), allWorks);
            // ---
        });
        task.setOnFailed(e -> {
            loadingAlert.close();
            showError("Failed to load story content.");
            e.getSource().getException().printStackTrace();
        });
        new Thread(task).start();
    }

    /**
     * Loads story from library using Work object. (Corrected)
     */
    private void loadStoryFromLibrary(Work work) { // <-- CORRECTED parameter
        if (work == null) return;

        Path filePath = Paths.get(work.getUrl()); // Get path from 'url' field

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            launchReadingWindow(work.getTitle(), content, true); // Pass flag for offline
        } catch (IOException | java.nio.file.InvalidPathException e) {
            showError("Could not read story file: " + e.getMessage());
        }
    }

    // --- CHANGED ---
    // Overload for Online stories
    // Now accepts the allWorks list
    private void launchReadingWindow(Work work, String content, List<Work> allWorks) {
        launchReadingWindowInternal(work.getTitle(), content, work, false, allWorks);
    }
    // ---

    // Overload for Offline stories
    private void launchReadingWindow(String title, String content, boolean isOffline) {
        // --- CHANGED ---
        // Pass null for the allWorks list
        launchReadingWindowInternal(title, content, null, isOffline, null);
        // ---
    }

    // --- CHANGED ---
    // Combined internal method to launch the reader window
    // Now accepts the allWorks list
    private void launchReadingWindowInternal(String title, String content, Work work, boolean isOffline, List<Work> allWorks) {
        // ---
        try {
            // This path MUST match your project structure
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/ReadingView.fxml"));
            Parent root = loader.load();
            ReadingController readingController = loader.getController();

            Stage readerStage = new Stage();
            Scene scene = new Scene(root);

            // Note: styles.css is applied to the main scene, not the reader
            // URL stylesheetUrl = getClass().getResource("/JavaBeta/styles.css");
            // if (stylesheetUrl != null) scene.getStylesheets().add(stylesheetUrl.toExternalForm());

            readerStage.setScene(scene);
            readerStage.setTitle(title);

            if (isOffline) {
                // Call the offline loadStory method
                readingController.loadStory(title, content, true);
            } else if (work != null) {
                // --- CHANGED ---
                // Call the NEW online loadStory method, passing the full list
                readingController.loadStory(work, content, allWorks);
                // ---
            } else {
                showError("Error determining story type for reader window.");
                return;
            }

            readerStage.initModality(Modality.NONE); // Changed to NONE so you can use both windows
            readerStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not open the reading window: " + e.getMessage());
        }
    }

    // --- Helper & Utility Methods ---
    private String buildSearchQuery() {
        StringJoiner sj = new StringJoiner(" ");
        addQueryPart(sj, "", anyField.getText(), false);
        addQueryPart(sj, "title:", titleField.getText(), true);
        addQueryPart(sj, "author:", authorField.getText(), true);
        addQueryPart(sj, "", tagsField.getText(), false);
        return sj.toString();
    }
    private void addQueryPart(StringJoiner sj, String p, String v, boolean q) {
        String val = v.trim();
        if (!val.isEmpty()) sj.add(q && val.contains(" ") ? p + "\"" + val + "\"" : p + val);
    }
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
    public void showCredits() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Credits");
        alert.setHeaderText("AO3 Reader Application");
        alert.setContentText("Developed by: [Your Name/Handle Here]\n" + // Remember to change this!
                "Using JavaFX and Jsoup.\n\n" +
                "Thanks for using the app!");
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
        String cssPath = "/JavaBeta/styles.css"; // Using styles.css for the dark theme toggle
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

    // --- Web Scraping Tasks ---
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

                    // --- CHANGED ---
                    // Removed the 3-tag limit to get ALL tags for better recommendations
                    // final int MAX_TAGS_TO_SHOW = 3;
                    // ---

                    for (Element workEl : workElements) {
                        Element titleEl = workEl.selectFirst("h4.heading a[href^='/works/']");
                        Element authorEl = workEl.selectFirst("a[rel=author]");
                        Element dateEl = workEl.selectFirst("p.datetime");
                        Elements tagElements = workEl.select("ul.tags a.tag");

                        if (titleEl != null && authorEl != null && dateEl != null) {
                            String title = titleEl.text();
                            String workUrl = "https://archiveofourown.org" + titleEl.attr("href");
                            String author = authorEl.text();
                            String lastUpdated = dateEl.text();

                            // --- CHANGED ---
                            // Get ALL tags, not just the first 3
                            List<String> tagsList = tagElements.stream()
                                    .map(Element::text)
                                    .collect(Collectors.toList());
                            String tags = String.join(", ", tagsList);
                            // ---

                            // We pass the full tag string to the new Work.java constructor
                            worksList.add(new Work(title, author, workUrl, tags, lastUpdated));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("DEBUG: Scraping failed!");
                    e.printStackTrace();
                    throw e;
                }
                if (!worksList.isEmpty()) { Thread.sleep(300); }
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

} // End of Ao3Controller class

