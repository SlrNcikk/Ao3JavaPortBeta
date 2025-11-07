package JavaBeta;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue; // Import for animation
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser; // For picking images
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

// ✅ --- FIX: This is the correct JavaFX TextArea ---
import javafx.scene.control.TextArea;
import java.io.File; // For the image file
import java.io.IOException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import javafx.scene.control.cell.PropertyValueFactory; // RESTORED
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.TilePane; // Import for TilePane
import javafx.scene.layout.BorderPane; // RESTORED
import javafx.scene.layout.StackPane; // RESTORED
import javafx.util.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kordamp.ikonli.elusive.Elusive;
import org.kordamp.ikonli.entypo.Entypo;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

// ✅ --- FIX: Changed from 'import java.awt.*;' to the specific class you need ---
import java.awt.Desktop;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Keep for Alert
import java.util.StringJoiner; // Keep for buildSearchQuery
import java.util.stream.Stream;

public class Ao3Controller {

    // --- FXML UI Elements (From your original file) ---
    @FXML private TextField anyField, titleField, authorField, tagsField;
    @FXML private Button searchButton, refreshLibraryButton, clearButton;
    @FXML private Button modeSwitchButton;
    @FXML private ImageView modeImageView;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label timeLabel;
    @FXML private ListView<Work> resultsListView;
    @FXML private VBox searchBox;
    @FXML private TableView<Work> libraryTableView;
    @FXML private TableColumn<Work, String> libraryTitleColumn;
    @FXML private TableColumn<Work, String> libraryAuthorColumn;
    @FXML private TableColumn<Work, String> libraryTagsColumn;
    @FXML private TableColumn<Work, String> libraryUpdatedColumn;
    @FXML private HBox libraryControlsBox;
    @FXML private Button deleteStoryButton;
    @FXML private Button viewFolderButton;
    @FXML private FontIcon deleteIcon;
    @FXML private Button settingsButton;
    @FXML private Button themeButton;
    @FXML private FontIcon settingsIcon;
    @FXML private FontIcon themeIcon;

    // --- FXML UI Elements (For new design) ---
    @FXML private BorderPane onlineSearchPane;   // The "Online Search" view
    @FXML private SplitPane offlineSplitPane;    // The "Offline Library + Create" view
    @FXML private VBox offlineLibraryPane; // The "Unlisted" + "My Library" part
    @FXML private VBox createPane;         // The "Create" panel
    @FXML private Button addFicButton;
    @FXML private Button deleteButton; // This is the new delete button in the offline view
    @FXML private ListView<Work> unlistedListView;
    @FXML private TilePane libraryTilePane;
    @FXML private StackPane folderImagePane; // This is the new clickable StackPane
    @FXML private FontIcon folderImageIcon;   // The '+' icon
    @FXML private ImageView folderImageView; // The user's chosen image
    @FXML private TextField folderNameField;
    @FXML private TextArea folderDescriptionArea;
    @FXML private VBox addFicDropTarget;
    @FXML private ListView<String> newFolderFicsListView;
    @FXML private Button createFolderButton;

    // ✅ --- NEW: Label for the tooltip alternative ---
    @FXML private Label tooltipLabel;
    private final Gson gson = new Gson();
    private Path folderSaveFile;

    // --- Data for new folder ---
    private String newFolderImagePath = null;
    private List<Work> ficsForNewFolder = new ArrayList<>();

    private FolderData currentEditingFolderData = null;
    private VBox currentEditingTile = null;

    // --- Restored Properties ---
    private Path libraryPath;
    private Timeline clockTimeline;
    private boolean isOnlineMode = true;
    private Image onlineModeIcon;
    private Image offlineModeIcon;
    private String darkThemePath;

    // --- Restored: handleLaunchChat, openSpotify, openAuthorProfile ---
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

    public void openAuthorProfile(Work work) {
        if (work == null) {
            return; // Safety check
        }

        // This is the safety check for "orphan_account" or "Anonymous"
        if (work.getAuthorUrl() == null) {
            System.out.println("Cannot open profile for " + work.getAuthor() + ". No URL provided.");
            showInfo("Profile Not Available", "Cannot open a profile for '" + work.getAuthor() + "'.");
            return;
        }

        System.out.println("--- OPENING AUTHOR PROFILE ---");
        System.out.println("Author Name: " + work.getAuthor());
        System.out.println("Author URL: " + work.getAuthorUrl());

        try {
            // 1. Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/AuthorProfileView.fxml"));
            Parent root = loader.load();

            // 2. Get the controller
            AuthorProfileController controller = loader.getController();

            // 3. Pass the data to the new controller
            controller.loadAuthorData(work.getAuthor(), work.getAuthorUrl());

            // 4. Create and show the new window
            Stage stage = new Stage();
            stage.setTitle(work.getAuthor() + "'s Profile");
            stage.setScene(new Scene(root));

            // This blocks the main window, which is good for a profile
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
        } catch (Exception e) {
            System.err.println("ERROR: Could not load mode icon images!");
        }

        // --- Setup Button Icons ---
        if (settingsIcon != null) settingsIcon.setIconCode(Elusive.COG);
        if (themeIcon != null) themeIcon.setIconCode(Entypo.ADJUST);
        if (deleteIcon != null) deleteIcon.setIconCode(FontAwesomeSolid.TRASH);

        // Setup Library Path
        try {
            libraryPath = Paths.get(System.getProperty("user.home"), "AO3_Offline_Library");
            if (!Files.exists(libraryPath)) Files.createDirectories(libraryPath);

            // --- ADDED: Setup folder save file and load existing folders ---
            folderSaveFile = libraryPath.resolve("folders.json");
            loadFolders();
            // --- END OF ADDED CODE ---

        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- ✅ NEW: Configure "Online Search" ListView ---
        if (resultsListView != null) {
            // This tells the ListView to use your new controller class
            // for every cell.
            resultsListView.setCellFactory(listView -> new WorkCellController(this));

            // Add a placeholder label
            resultsListView.setPlaceholder(new Label("No search results."));
        }
        // --- ✅ OLD TABLEVIEW CODE REMOVED ---


        // --- Configure "Offline Library" TableView (RESTORED) ---
        // (This is for your *other* TableView, which is fine)
        if (libraryTitleColumn != null) { libraryTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title")); }
        if (libraryAuthorColumn != null) { libraryAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author")); }
        if (libraryTagsColumn != null) { libraryTagsColumn.setCellValueFactory(new PropertyValueFactory<>("tags")); }
        if (libraryUpdatedColumn != null) { libraryUpdatedColumn.setCellValueFactory(new PropertyValueFactory<>("lastUpdated")); }

        if (libraryTableView != null) {
            libraryTableView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    loadStoryFromLibrary(libraryTableView.getSelectionModel().getSelectedItem());
                }
            });
        }


        // --- Configure "Unlisted" ListView (OFFLINE) ---
        if (unlistedListView != null) {
            unlistedListView.setPlaceholder(new Label("No unlisted fics found."));
            unlistedListView.setOnDragDetected(event -> {
                Work selectedWork = unlistedListView.getSelectionModel().getSelectedItem();
                if (selectedWork != null) {
                    Dragboard db = unlistedListView.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    // We'll use the URL (which is the file path) as the unique identifier
                    content.putString(selectedWork.getUrl());
                    db.setContent(content);
                    event.consume();
                }
            });
        }

        // --- Configure Drag-and-Drop (TARGET) ---
        if (addFicDropTarget != null) {
            // 1. Accept the drag event
            addFicDropTarget.setOnDragOver(event -> {
                if (event.getGestureSource() != addFicDropTarget && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
                event.consume();
            });

            // 2. Handle the drop event
            addFicDropTarget.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    // Get the URL (file path) from the dragboard
                    String workUrl = db.getString();

                    // Use your helper method to find the matching Work object
                    Work workToAdd = findWorkInList(workUrl, unlistedListView.getItems());

                    // Add the fic if we found it and it's not already in the list
                    if (workToAdd != null && !ficsForNewFolder.contains(workToAdd)) {
                        // Add to our internal data list
                        ficsForNewFolder.add(workToAdd);

                        // Add just the title to the visual list
                        newFolderFicsListView.getItems().add(workToAdd.getTitle());

                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });
        }

        // --- Initialize Split Pane State ---
        if (createPane != null) {
            createPane.setVisible(false);
            createPane.setManaged(false);
        }
        if (offlineSplitPane != null) {
            offlineSplitPane.setDividerPosition(0, 1.0);
        }

        // Initialize Time Display
        setupClock();

        // Set initial view to Online Mode
        isOnlineMode = false; // Set to false so switchMode() correctly sets it to Online
        switchMode(); // Call once to set the correct initial state
    }

    // --- "Create" Panel Methods ---
    @FXML
    private void handleAddFicClick() {
        if (createPane == null || offlineSplitPane == null) return;
        boolean isVisible = createPane.isVisible();

        if (isVisible) {
            // --- HIDING THE PANE ---
            createPane.setVisible(false);
            createPane.setManaged(false);
            animateOfflineDividerTo(1.0);

            // ✅ --- NEW: Always reset state when hiding ---
            currentEditingFolderData = null;
            currentEditingTile = null;
            clearCreateForm(); // Clear the form *after* hiding

        } else {
            // --- SHOWING THE PANE ---

            // ✅ --- NEW: Only clear form if we are NOT editing ---
            if (currentEditingFolderData == null) {
                clearCreateForm(); // This is for "Create New"
            }
            // If we *are* editing, populateCreatePane() has already
            // filled the form, so we don't clear it.

            createPane.setVisible(true);
            createPane.setManaged(true);
            animateOfflineDividerTo(0.5);
        }
    }

    private void handleModifyFolder(FolderData folderData, VBox folderTile) {
        // 1. Store the folder and tile we are editing
        this.currentEditingFolderData = folderData;
        this.currentEditingTile = folderTile;

        // 2. Populate the form fields
        populateCreatePane(folderData);

        // 3. Show the "Create" pane (which now acts as an "Edit" pane)
        if (!createPane.isVisible()) {
            handleAddFicClick();
        }
    }

    private void saveFolders() {
        if (libraryTilePane == null || folderSaveFile == null) {
            return;
        }

        // 1. Get all FolderData objects from the UI tiles
        List<FolderData> allFolders = new ArrayList<>();
        for (javafx.scene.Node node : libraryTilePane.getChildren()) {
            if (node.getUserData() instanceof FolderData) {
                allFolders.add((FolderData) node.getUserData());
            }
        }

        // 2. Write the list to the JSON file
        try (FileWriter writer = new FileWriter(folderSaveFile.toFile())) {
            gson.toJson(allFolders, writer);
            System.out.println("Folders saved successfully to " + folderSaveFile);
        } catch (IOException e) {
            System.err.println("Failed to save folders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadFolders() {
        // --- ADD THESE TWO LINES ---
        if (libraryTilePane != null) {
            libraryTilePane.getChildren().clear();
        }
        // --- END OF ADD ---

        if (folderSaveFile == null || !Files.exists(folderSaveFile)) {
            System.out.println("No folder save file found. Starting fresh.");
            return;
        }

        // 1. Define the type for Gson (a List of FolderData)
        Type listType = new TypeToken<ArrayList<FolderData>>() {}.getType();

        // 2. Read the file and convert it back to a List
        try (FileReader reader = new FileReader(folderSaveFile.toFile())) {
            List<FolderData> loadedFolders = gson.fromJson(reader, listType);

            if (loadedFolders != null) {
                // 3. Re-create the visual tiles for each folder
                for (FolderData folderData : loadedFolders) {
                    VBox folderTile = createFolderTile(folderData);
                    libraryTilePane.getChildren().add(folderTile);
                }
                System.out.println("Loaded " + loadedFolders.size() + " folders.");
            }
        } catch (IOException e) {
            System.err.println("Failed to load folders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to fill all the form fields with an existing folder's data.
     */
    private void populateCreatePane(FolderData data) {
        folderNameField.setText(data.name);
        folderDescriptionArea.setText(data.description);

        // Populate the image
        newFolderImagePath = data.imagePath;
        if (newFolderImagePath != null && !newFolderImagePath.isBlank()) {
            try {
                Image image = new Image(newFolderImagePath);
                folderImageView.setImage(image);

                // Re-apply the same viewport logic to keep it square
                double width = image.getWidth();
                double height = image.getHeight();
                if (width != height) {
                    double size = Math.min(width, height);
                    double x = (width - size) / 2;
                    double y = (height - size) / 2;
                    folderImageView.setViewport(new Rectangle2D(x, y, size, size));
                } else {
                    folderImageView.setViewport(null);
                }

                folderImageView.setVisible(true);
                folderImageIcon.setVisible(false);
            } catch (Exception e) {
                // If image fails to load, reset to placeholder
                folderImageView.setImage(null);
                folderImageView.setViewport(null);
                folderImageView.setVisible(false);
                folderImageIcon.setVisible(true);
            }
        } else {
            // Reset to placeholder if no image path
            folderImageView.setImage(null);
            folderImageView.setViewport(null);
            folderImageView.setVisible(false);
            folderImageIcon.setVisible(true);
        }
        // ✅ --- FIX: Populate fics list from paths ---
        ficsForNewFolder.clear();
        newFolderFicsListView.getItems().clear(); // Clear visual list too

        if (data.ficPaths != null) {
            for (String path : data.ficPaths) {
                // Use your helper to find the full Work object from the master "Unlisted" list
                Work work = findWorkInList(path, unlistedListView.getItems());
                if (work != null) {
                    ficsForNewFolder.add(work);
                    newFolderFicsListView.getItems().add(work.getTitle());
                } else {
                    // Fic might be missing or not loaded yet
                    System.err.println("Could not find fic for path: " + path);
                }
            }
        }
    }

    @FXML
    private void handleSelectImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Folder Image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        File selectedFile = fileChooser.showOpenDialog(folderImagePane.getScene().getWindow());

        if (selectedFile != null) {
            try {
                // We save the path, which is what your createFolder method needs
                newFolderImagePath = selectedFile.toURI().toString();
                Image image = new Image(newFolderImagePath);

                // --- NEW AUTO-CROP LOGIC ---
                double width = image.getWidth();
                double height = image.getHeight();

                if (width == height) {
                    // It's already square, no viewport needed
                    folderImageView.setViewport(null);
                } else {
                    // It's rectangular. Find the smallest dimension.
                    double size = Math.min(width, height);

                    // Calculate (x, y) to center the crop
                    double x = (width - size) / 2;
                    double y = (height - size) / 2;

                    // Create and set the square viewport
                    Rectangle2D viewport = new Rectangle2D(x, y, size, size);
                    folderImageView.setViewport(viewport);
                }
                // --- END OF NEW LOGIC ---

                folderImageView.setImage(image);
                folderImageView.setVisible(true);
                folderImageIcon.setVisible(false);

            } catch (Exception e) {
                showError("Could not load image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCreateFolderClick() {
        String folderName = folderNameField.getText();
        String description = folderDescriptionArea.getText();

        if (folderName == null || folderName.isBlank()) {
            showError("Please enter a folder name.");
            return;
        }

        // --- THIS IS THE NEW LOGIC ---
        if (currentEditingFolderData != null) {
            // --- MODIFY MODE ---
            currentEditingFolderData.name = folderName;
            currentEditingFolderData.description = description;
            currentEditingFolderData.imagePath = newFolderImagePath;

            // ✅ --- FIX: Convert List<Work> to List<String>
            currentEditingFolderData.ficPaths.clear();
            currentEditingFolderData.ficPaths.addAll(
                    ficsForNewFolder.stream()
                            .map(Work::getUrl) // Get the path (URL)
                            .collect(Collectors.toList())
            );

            libraryTilePane.getChildren().remove(currentEditingTile);
            VBox newFolderTile = createFolderTile(currentEditingFolderData);
            libraryTilePane.getChildren().add(newFolderTile);

            currentEditingFolderData = null;
            currentEditingTile = null;

        } else {
            // --- CREATE NEW MODE (Your old logic) ---

            // ✅ --- FIX: Convert List<Work> to List<String>
            List<String> paths = ficsForNewFolder.stream()
                    .map(Work::getUrl) // Get the path (URL)
                    .collect(Collectors.toList());

            FolderData newFolderData = new FolderData(folderName, description, newFolderImagePath, paths);

            VBox folderTile = createFolderTile(newFolderData);

            if (libraryTilePane != null) {
                libraryTilePane.getChildren().add(folderTile);
            }
        }

        handleAddFicClick();
        saveFolders();
    }



    private void clearCreateForm() {
        folderNameField.clear();
        folderDescriptionArea.clear();
        newFolderImagePath = null;
        folderImageView.setImage(null);
        folderImageView.setViewport(null);
        folderImageView.setVisible(false);
        folderImageIcon.setVisible(true);
        ficsForNewFolder.clear();
        newFolderFicsListView.getItems().clear();
    }

    private void animateOfflineDividerTo(double position) {
        if (offlineSplitPane.getDividers().isEmpty()) return;
        final SplitPane.Divider divider = offlineSplitPane.getDividers().get(0);
        final double end = position;
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(divider.positionProperty(), end)));
        timeline.play();
    }

    // --- RESTORED: setupClock ---
    private void setupClock() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        if (timeLabel != null) {
            timeLabel.setText(LocalTime.now().format(timeFormatter));
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

    // ✅ --- Tooltip Alternative: Mouse Enter/Exit Methods ---
    @FXML private void onModeSwitchEnter() { /* if (tooltipLabel != null) tooltipLabel.setText("Switch Mode"); */ }
    @FXML private void onSettingsEnter() { /* if (tooltipLabel != null) tooltipLabel.setText("Settings"); */ }
    @FXML private void onThemeEnter() { /* if (tooltipLabel != null) tooltipLabel.setText("Toggle Theme"); */ }
    @FXML private void onChangePhotoEnter() { /* if (tooltipLabel != null) tooltipLabel.setText("Change Photo"); */ }
    @FXML private void onRefreshEnter() { /* if (tooltipLabel != null) tooltipLabel.setText("Refresh Library"); */ }
    @FXML private void onViewFolderEnter() { /* if (tooltipLabel != null) tooltipLabel.setText("View Folder"); */ }
    @FXML private void onDeleteStoryEnter() { /* if (tooltipLabel != null) tooltipLabel.setText("Delete Selected Story"); */ }
    @FXML private void onMouseExit() { /* if (tooltipLabel != null) tooltipLabel.setText(""); */ }


    @FXML
    protected void switchMode() {
        isOnlineMode = !isOnlineMode;
        System.out.println("Switch Mode clicked. New state: " + (isOnlineMode ? "Online Search" : "Offline Library"));

        // Toggle "Online Search" pane (this now controls the searchBox too)
        onlineSearchPane.setVisible(isOnlineMode);
        onlineSearchPane.setManaged(isOnlineMode);

        // Toggle "Offline Library" pane (this now controls libraryControlsBox too)
        offlineSplitPane.setVisible(!isOnlineMode);
        offlineSplitPane.setManaged(!isOnlineMode);

        if (isOnlineMode) {
            if (modeImageView != null) modeImageView.setImage(offlineModeIcon);
            if (unlistedListView != null) unlistedListView.getSelectionModel().clearSelection();
        } else {
            if (modeImageView != null) modeImageView.setImage(onlineModeIcon);
            populateLibraryViews(); // This now correctly reloads your folders
            if (resultsListView != null) resultsListView.getSelectionModel().clearSelection();
        }

        if (createPane != null && createPane.isVisible()) {
            handleAddFicClick(); // Hide it
        }

        if (loadingIndicator != null) loadingIndicator.setVisible(false);
    }

    /**
     * New method to populate BOTH offline lists.
     */
    private void populateLibraryViews() {
        if (unlistedListView == null || libraryTilePane == null) return;

        // This line is now handled by loadFolders(), so we can remove it
        // libraryTilePane.getChildren().clear();

        unlistedListView.getItems().clear();

        if (libraryPath == null) { return; }

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
                        Work offlineWork = new Work(title, author, path.toFile().getAbsolutePath(), "N/A", "N/A");
                        offlineWork.setAuthorUrl(null);
                        offlineWorks.add(offlineWork);
                    });
            unlistedListView.getItems().addAll(offlineWorks);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not read library directory: " + e.getMessage());
        }

        // --- ADD THIS LINE TO RELOAD THE FOLDERS ---
        loadFolders();
    }


    @FXML
    protected void onDeleteStoryClick() {
        Work selectedWork = null;
        if (!isOnlineMode) {
            if (unlistedListView != null) {
                selectedWork = unlistedListView.getSelectionModel().getSelectedItem();
            }
            // TODO: Add selection logic for your TilePane here
        } else {
            if (libraryTableView != null) {
                selectedWork = libraryTableView.getSelectionModel().getSelectedItem();
            }
        }
        if (selectedWork == null) {
            showInfo("Please select an item to delete.");
            return;
        }
        confirmAndDelete(selectedWork);
    }

    private void confirmAndDelete(Work work) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Story");
        confirmAlert.setHeaderText("Are you sure you want to delete this file?");
        confirmAlert.setContentText(work.getTitle() + " by " + work.getAuthor());
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Path filePath = Paths.get(work.getUrl());
                if (Files.deleteIfExists(filePath)) {
                    if (isOnlineMode) {
                        populateLibraryListView(); // Refresh old table
                    } else {
                        populateLibraryViews(); // Refresh new lists
                    }
                } else {
                    showError("Could not delete the file.");
                }
            } catch (IOException | SecurityException | java.nio.file.InvalidPathException e) {
                showError("Error deleting file: " + e.getMessage());
            }
        }
    }

    @FXML
    protected void onViewFolderClick() {
        if (libraryPath == null || !Files.exists(libraryPath)) {
            showError("Library folder not found.");
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
        resultsListView.getItems().clear();
        Task<List<Work>> fetchWorksTask = createFetchWorksTask(query);
        fetchWorksTask.setOnSucceeded(e -> {
            resultsListView.getItems().setAll(fetchWorksTask.getValue());
            loadingIndicator.setVisible(false);
            searchButton.setDisable(false);
            clearButton.setDisable(false);
        });
        fetchWorksTask.setOnFailed(e -> {
            showError("Failed to fetch works. Check connection.");
            loadingIndicator.setVisible(false);
            searchButton.setDisable(false);
            clearButton.setDisable(false);
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
            resultsListView.getItems().clear();
        }
    }

    @FXML
    protected void onRefreshLibraryClick() {
        if (isOnlineMode) return;
        populateLibraryViews();
    }

    private void populateLibraryListView() {
        // ... (This is your old method, now replaced by populateLibraryViews) ...
    }

    // --- RESTORED: All story loading methods ---
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

    public void loadStoryFromLibrary(Work work) {
        if (work == null) return;
        Path filePath = Paths.get(work.getUrl()); // Get path from 'url' field

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

    // --- RESTORED: Helper Methods ---
    private String buildSearchQuery() {
        StringJoiner sj = new StringJoiner(" ");
        if(anyField != null) addQueryPart(sj, "", anyField.getText(), false);
        if(titleField != null) addQueryPart(sj, "title:", titleField.getText(), true);
        if(authorField != null) addQueryPart(sj, "author:", authorField.getText(), true);
        if(tagsField != null) addQueryPart(sj, "", tagsField.getText(), false);
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
        // ... (rest of method) ...
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

    private Work findWorkInList(String url, List<Work> list) {
        if (list == null) return null;
        for (Work work : list) {
            // We use getUrl() because that's what we stored (the file path)
            if (work.getUrl().equals(url)) {
                return work;
            }
        }
        return null; // Not found
    }

    // --- RESTORED: Web Scraping Tasks ---
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
                            .timeout(10000) // 10-second timeout
                            .get();
                    Elements workElements = doc.select("li.work.blurb");
                    System.out.println("DEBUG: Connection successful. Found " + workElements.size() + " works on the page.");


                    for (Element workEl : workElements) {
                        Element titleEl = workEl.selectFirst("h4.heading a[href^='/works/']");
                        Element authorEl = workEl.selectFirst("a[rel=author]");
                        Element dateEl = workEl.selectFirst("p.datetime");

                        // ✅ --- NEW SCRAPING LOGIC ---
                        Element fandomEl = workEl.selectFirst("h5.fandoms a");
                        Elements relationshipEls = workEl.select("li.relationships a");
                        Elements characterEls = workEl.select("li.characters a");
                        Elements tagElements = workEl.select("li.freeforms a.tag"); // This is the "freeform" tags
                        // --- END NEW LOGIC ---

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

                            // --- NEW: Scrape Fandom, Relationships, Characters, and Freeforms ---
                            String fandom = (fandomEl != null) ? fandomEl.text() : "N/A";

                            String relationships = relationshipEls.stream()
                                    .map(Element::text)
                                    .collect(Collectors.joining(", "));

                            String characters = characterEls.stream()
                                    .map(Element::text)
                                    .collect(Collectors.joining(", "));

                            String tags = tagElements.stream()
                                    .map(Element::text)
                                    .collect(Collectors.joining(", "));
                            // ---

                            // Scrape Header Icons
                            String rating = "Not Rated";
                            String category = "No Category";
                            String warnings = "No Warnings";
                            String completionStatus = "In Progress";

                            Elements requiredTags = workEl.select("ul.required-tags span.tag-h");

                            for (Element tagSpan : requiredTags) {
                                String titleAttr = tagSpan.attr("title");

                                if (titleAttr.startsWith("Rating:")) {
                                    rating = titleAttr.substring("Rating: ".length());
                                } else if (titleAttr.startsWith("Category:")) {
                                    category = titleAttr.substring("Category: ".length());
                                } else if (titleAttr.startsWith("Warnings:")) {
                                    warnings = titleAttr.substring("Warnings: ".length());
                                } else if (titleAttr.startsWith("Completion Status:")) {
                                    completionStatus = titleAttr.substring("Completion Status: ".length());
                                }
                            }

                            // ✅ --- THIS IS THE FIX ---
                            // This now correctly calls the 12-argument constructor
                            Work newWork = new Work(title, author, workUrl, tags, lastUpdated,
                                    rating, category, warnings, completionStatus,
                                    fandom, relationships, characters);

                            newWork.setAuthorUrl(authorUrl); // Set the author URL
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
    public static class FolderData {
        String name;
        String description;
        String imagePath;
        List<String> ficPaths = new ArrayList<>();

        // This constructor now correctly accepts a List<String>
        FolderData(String name, String description, String imagePath, List<String> ficPaths) {
            this.name = name;
            this.description = description;
            this.imagePath = imagePath;
            this.ficPaths.addAll(ficPaths); // And adds the list
        }
    }
    private VBox createFolderTile(FolderData folderData) {
        VBox folderTile = new VBox(5); // 5px spacing
        folderTile.setPrefSize(130, 130); // A good square size
        folderTile.setAlignment(Pos.CENTER);
        folderTile.setStyle("-fx-padding: 5; -fx-background-color: -fx-control-inner-background; -fx-cursor: hand;");

        folderTile.setUserData(folderData);

        // --- CORRECTED Add Image Logic ---
        if (folderData.imagePath != null && !folderData.imagePath.isBlank()) {
            try {
                // 1. Create the ImageView
                ImageView tileImageView = new ImageView();
                tileImageView.setFitHeight(80);
                tileImageView.setFitWidth(80);
                tileImageView.setPreserveRatio(true);

                // 2. Load and set the image
                Image tileImage = new Image(folderData.imagePath);
                tileImageView.setImage(tileImage);

                // 3. Apply the square auto-crop viewport
                double width = tileImage.getWidth();
                double height = tileImage.getHeight();
                if (width != height) {
                    double size = Math.min(width, height);
                    double x = (width - size) / 2;
                    double y = (height - size) / 2;
                    tileImageView.setViewport(new Rectangle2D(x, y, size, size));
                }

                // 4. Add the SUCCESSFUL ImageView to the tile
                folderTile.getChildren().add(tileImageView);

            } catch (Exception e) {
                // 5. If it FAILS, add the placeholder icon INSTEAD
                System.err.println("Could not load tile image: " + e.getMessage());
                folderTile.getChildren().add(createFolderPlaceholderIcon()); // This was one error
            }
        } else {
            // 6. If NO IMAGE PATH, add the placeholder icon
            folderTile.getChildren().add(createFolderPlaceholderIcon()); // This was the other error
        }
        // --- End of Corrected Logic ---


        // --- Add Name ---
        Label nameLabel = new Label(folderData.name);
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        folderTile.getChildren().add(nameLabel);

        // --- Add double-click event (to open) ---
        folderTile.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                try {
                    // 1. Load the FXML
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/FolderView.fxml"));
                    Parent root = loader.load();

                    // 2. Get the new controller
                    FolderViewController controller = loader.getController();

                    // 3. Pass it the data and a reference to this main controller
                    controller.setMainController(this); // 'this' is the Ao3Controller
                    controller.loadFolder(folderData);

                    // 4. Create the new scene
                    Scene newScene = new Scene(root); // We create the scene first

                    // ✅ --- NEW, BETTER THEME LOGIC ---
                    // This copies all active stylesheets (light or dark) from the main window
                    if (themeButton != null && themeButton.getScene() != null) {
                        newScene.getStylesheets().addAll(themeButton.getScene().getStylesheets());
                    }
                    // ✅ --- END OF NEW LOGIC ---

                    // 5. Create and show the new window (Stage)
                    Stage folderStage = new Stage();
                    folderStage.setTitle(folderData.name);

                    folderStage.setScene(newScene); // Set the new, themed scene

                    folderStage.initModality(Modality.NONE);
                    folderStage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    showError("Could not open folder view: " + e.getMessage());
                }
            }
        });

        // --- Add Right-Click Context Menu ---
        ContextMenu contextMenu = new ContextMenu();
        MenuItem modifyItem = new MenuItem("Modify Folder");
        MenuItem deleteItem = new MenuItem("Delete Folder");

        modifyItem.setOnAction(e -> handleModifyFolder(folderData, folderTile));
        deleteItem.setOnAction(e -> handleDeleteFolder(folderTile));

        contextMenu.getItems().addAll(modifyItem, deleteItem);

        folderTile.setOnContextMenuRequested(event -> {
            contextMenu.show(folderTile, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        return folderTile; // This is the line that fixes the "Incompatible types" error
    }

    /**
     * Handles deleting the folder tile
     */
    private void handleDeleteFolder(VBox folderTile) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Folder");
        confirmAlert.setHeaderText("Are you sure you want to delete this folder?");
        confirmAlert.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            libraryTilePane.getChildren().remove(folderTile);
            saveFolders();
        }
    }

    private FontIcon createFolderPlaceholderIcon() {
        FontIcon placeholderIcon = new FontIcon("fa-folder");
        placeholderIcon.setIconSize(60);
        placeholderIcon.setStyle("-fx-text-fill: #888888;"); // Give it a subtle color
        return placeholderIcon;
    }
}