package JavaBeta;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import java.util.*;
import javafx.animation.Timeline;
import javafx.application.Platform;
import org.openqa.selenium.Cookie;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import javafx.util.Duration;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import java.io.File;
import java.io.IOException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.google.gson.Gson;
import java.io.FileWriter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kordamp.ikonli.elusive.Elusive;
import org.kordamp.ikonli.entypo.Entypo;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import java.awt.Desktop;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class Ao3Controller{

    @FXML private TextField anyField, titleField, authorField, tagsField;
    @FXML private Button searchButton, refreshLibraryButton, clearButton;
    @FXML private Button modeSwitchButton;
    @FXML private TitledPane historyTitledPane;
    @FXML private FlowPane historyFlowPane;
    @FXML private ImageView modeImageView;
    @FXML private Button myReviewsButton;
    @FXML private FontIcon myReviewsIcon;
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
    @FXML private BorderPane onlineSearchPane;
    @FXML private SplitPane offlineSplitPane;
    @FXML private VBox offlineLibraryPane;
    @FXML private VBox createPane;
    @FXML private Button addFicButton;
    @FXML private Button deleteButton;
    @FXML private ListView<Work> unlistedListView;
    @FXML private TilePane libraryTilePane;
    @FXML private StackPane folderImagePane;
    @FXML private FontIcon folderImageIcon;
    @FXML private ImageView folderImageView;
    @FXML private TextField folderNameField;
    @FXML private TextArea folderDescriptionArea;
    @FXML private VBox addFicDropTarget;
    @FXML private ListView<Work> newFolderFicsListView;
    @FXML private Button createFolderButton;

    private HistoryManager historyManager = new HistoryManager();

    public Document getDocument(String url) throws IOException, InterruptedException {
        final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
        System.out.println("DEBUG: Selenium connecting to URL -> " + url);

        WebDriver driver = null;
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("DEBUG: Selenium attempt " + attempt + "...");
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new");
                options.addArguments("--user-agent=" + userAgent);
                options.addArguments("--log-level=3");
                options.addArguments("--disable-gpu");
                options.addArguments("--blink-settings=imagesEnabled=false");

                driver = new ChromeDriver(options);

                // --- THIS IS THE FIX ---
                // 1. Go to the base domain first to set a cookie
                driver.get("https://archiveofourown.org/");

                // 2. Add the cookie to bypass adult warnings
                Cookie adultCookie = new Cookie("view_adult", "true");
                driver.manage().addCookie(adultCookie);
                System.out.println("DEBUG: 'view_adult=true' cookie added.");
                // --- END OF FIX ---

                // 3. Now, go to the actual target URL
                driver.get(url);
                System.out.println("DEBUG: Browser navigated to target URL: " + url);

                try {
                    // 4. Try to click "Proceed" as a fallback
                    WebElement proceedButton = driver.findElement(By.cssSelector("form.adult input[type='submit'][value='Proceed']"));
                    if (proceedButton != null) {
                        System.out.println("DEBUG: Found 'Proceed' button (cookie might not have worked). Clicking it...");
                        proceedButton.click();

                        // 5. NEW: Smarter wait logic
                        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
                        if (url.contains("/works/")) {
                            System.out.println("DEBUG: Waiting for story content (#workskin)...");
                            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workskin")));
                        } else if (url.contains("/users/")) {
                            System.out.println("DEBUG: Waiting for profile content (#main)...");
                            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("main")));
                        } else if (url.contains("/search")) {
                            System.out.println("DEBUG: Waiting for search results (li.work or 'No results')...");
                            wait.until(ExpectedConditions.or(
                                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.work.blurb")),
                                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("h4.heading:contains(No results found)"))
                            ));
                        }
                        System.out.println("DEBUG: Page loaded after 'Proceed' click.");
                    }
                } catch (NoSuchElementException e) {
                    System.out.println("DEBUG: 'Proceed' button not found (cookie worked or page is not adult).");
                } catch (TimeoutException e) {
                    throw new IOException("Failed to load page content after clicking 'Proceed'.", e);
                }

                // 6. Check for "Restricted" errors
                try {
                    WebElement flashError = driver.findElement(By.cssSelector("div.flash.error"));
                    if (flashError != null && flashError.isDisplayed()) {
                        System.err.println("DEBUG: AO3 returned a restriction error: " + flashError.getText());
                    }
                } catch (NoSuchElementException e) {
                    // No error, this is good.
                }

                String finalHtml = driver.getPageSource();
                System.out.println("DEBUG: Connection successful. Parsing with Jsoup...");
                return Jsoup.parse(finalHtml);

            } catch (Exception e) {
                System.err.println("DEBUG: Selenium attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxRetries) {
                    System.err.println("DEBUG: All retries failed.");
                    throw e;
                }
                Thread.sleep(2000);
            } finally {
                if (driver != null) {
                    driver.quit();
                    System.out.println("DEBUG: WebDriver quit.");
                }
            }
        }
        throw new IOException("Failed to get document after all retries.");
    }

    private Path getReviewsFilePath() {
        if (libraryPath != null) {
            return libraryPath.resolve("reviews.json");
        }
        try {
            Path path = Paths.get(System.getProperty("user.home"), "AO3_Offline_Library");
            if (!Files.exists(path)) Files.createDirectories(path);
            return path.resolve("reviews.json");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, UserReview> loadReviewMap() {
        Path path = getReviewsFilePath();
        if (path == null || !Files.exists(path)) {
            return new HashMap<>();
        }

        Type type = new TypeToken<HashMap<String, UserReview>>() {}.getType();
        Map<String, UserReview> reviewMap = new HashMap<>();

        try (FileReader reader = new FileReader(path.toFile())) {
            reviewMap = gson.fromJson(reader, type);
            if (reviewMap == null) {
                reviewMap = new HashMap<>();
            }
        } catch (IOException e) {
            System.err.println("Could not load reviews file. Starting fresh.");
            e.printStackTrace();
            return new HashMap<>();
        }
        return reviewMap;
    }

    private void displaySearchHistory() {
        if (historyFlowPane == null) return;
        historyFlowPane.getChildren().clear();
        List<SearchQuery> history = historyManager.getHistory();
        for (SearchQuery query : history) {
            HBox tag = createHistoryTag(query);
            historyFlowPane.getChildren().add(tag);
        }
    }

    private HBox createHistoryTag(SearchQuery query) {
        Label textLabel = new Label(query.toString());
        Label deleteIcon = new Label(" \u2715");
        deleteIcon.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");

        HBox tag = new HBox(5, textLabel, deleteIcon);
        tag.setStyle("-fx-background-color: #383838; -fx-padding: 3 8 3 8; " +
                "-fx-border-radius: 15; -fx-background-radius: 15; " +
                "-fx-text-fill: white; -fx-cursor: hand;");
        tag.setAlignment(Pos.CENTER);

        tag.setOnMouseClicked(event -> {
            anyField.setText(query.getAnyField());
            titleField.setText(query.getTitle());
            authorField.setText(query.getAuthor());
            tagsField.setText(query.getTags());
            onSearchButtonClick();
        });

        deleteIcon.setOnMouseClicked(event -> {
            historyManager.deleteQuery(query);
            historyFlowPane.getChildren().remove(tag);
            event.consume();
        });

        return tag;
    }

    @FXML
    private void onMyReviewsClick() {
        try {
            Map<String, UserReview> reviewMap = loadReviewMap();
            if (reviewMap.isEmpty()) {
                showInfo("No reviews found.", "You haven't written any reviews yet!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/ReviewLibraryView.fxml"));
            Parent root = loader.load();

            ReviewLibraryController controller = loader.getController();
            controller.loadReviews(reviewMap.values(), this);

            Stage stage = new Stage();
            stage.setTitle("My Review Library");
            stage.setScene(new Scene(root));

            if (themeButton != null && themeButton.getScene() != null) {
                stage.getScene().getStylesheets().addAll(themeButton.getScene().getStylesheets());
            }

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open Review Library: " + e.getMessage());
        }
    }

    @FXML private Label tooltipLabel;
    private final Gson gson = new Gson();
    private Path folderSaveFile;
    private String newFolderImagePath = null;
    private List<Work> ficsForNewFolder = new ArrayList<>();
    private FolderData currentEditingFolderData = null;
    private VBox currentEditingTile = null;
    private Path libraryPath;
    private Timeline clockTimeline;
    private boolean isOnlineMode = true;
    private Image onlineModeIcon;
    private Image offlineModeIcon;
    private String darkThemePath;


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
            return;
        }

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

            controller.setMainController(this);

            controller.loadAuthorData(work.getAuthor(), work.getAuthorUrl());

            Stage stage = new Stage();
            stage.setTitle(work.getAuthor() + "'s Profile");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.NONE);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not load the Author Profile window: " + e.getMessage());
        }
    }

    public void openWork(String workUrl) {
        openReadingView(workUrl);
    }

    private void openReadingView(String workUrl) {
        System.out.println("--- OPENING READING VIEW FROM URL ---");
        System.out.println("URL: " + workUrl);

        Work workToOpen = findWorkInList(workUrl, resultsListView.getItems());

        if (workToOpen != null) {
            loadAndShowStory(workToOpen, resultsListView.getItems());
        } else {
            System.out.println("Work not in search results. Creating a temporary Work object.");
            Work tempWork = new Work();
            tempWork.setUrl(workUrl);
            tempWork.setTitle("Loading Story...");
            loadAndShowStory(tempWork, new ArrayList<>());
        }
    }


    @FXML
    public void initialize() {
        try {
            onlineModeIcon = new Image(getClass().getResourceAsStream("/JavaBeta/oslm.png"));
            offlineModeIcon = new Image(getClass().getResourceAsStream("/JavaBeta/ofslm.png"));
        } catch (Exception e) {
            System.err.println("ERROR: Could not load mode icon images!");
        }

        if (myReviewsIcon != null) myReviewsIcon.setIconCode(FontAwesomeSolid.STAR);
        if (settingsIcon != null) settingsIcon.setIconCode(Elusive.COG);
        if (themeIcon != null) themeIcon.setIconCode(Entypo.ADJUST);
        if (deleteIcon != null) deleteIcon.setIconCode(FontAwesomeSolid.TRASH);

        try {
            libraryPath = Paths.get(System.getProperty("user.home"), "AO3_Offline_Library");
            if (!Files.exists(libraryPath)) Files.createDirectories(libraryPath);
            folderSaveFile = libraryPath.resolve("folders.json");
            loadFolders();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (historyFlowPane != null) {
            displaySearchHistory();
        }

        if (resultsListView != null) {
            resultsListView.setCellFactory(listView -> new WorkCellController(this));
            resultsListView.setPlaceholder(new Label("No search results."));
        }

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

        if (unlistedListView != null) {
            unlistedListView.setPlaceholder(new Label("No unlisted fics found."));
            unlistedListView.setCellFactory(listView -> new WorkCellController(this));
            unlistedListView.setOnDragDetected(event -> {
                Work selectedWork = unlistedListView.getSelectionModel().getSelectedItem();
                if (selectedWork != null) {
                    Dragboard db = unlistedListView.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(selectedWork.getUrl());
                    db.setContent(content);
                    event.consume();
                }
            });
        }

        if (newFolderFicsListView != null) {
            newFolderFicsListView.setCellFactory(listView -> new WorkCellController(this));
        }

        if (addFicDropTarget != null) {
            addFicDropTarget.setOnDragOver(event -> {
                if (event.getGestureSource() != addFicDropTarget && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
                event.consume();
            });

            addFicDropTarget.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    String workUrl = db.getString();
                    Work workToAdd = findWorkInList(workUrl, unlistedListView.getItems());
                    List<Work> ficsForFolder = new ArrayList<>();
                    if (workToAdd != null && !ficsForNewFolder.contains(workToAdd)) {
                        ficsForNewFolder.add(workToAdd);
                        newFolderFicsListView.getItems().add(workToAdd);
                        success = true;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });
        }

        if (createPane != null) {
            createPane.setVisible(false);
            createPane.setManaged(false);
        }
        if (offlineSplitPane != null) {
            offlineSplitPane.setDividerPosition(0, 1.0);
        }

        setupClock();
        isOnlineMode = false;
        switchMode();
    }

    @FXML
    private void handleAddFicClick() {
        if (createPane == null || offlineSplitPane == null) return;
        boolean isVisible = createPane.isVisible();

        if (isVisible) {
            createPane.setVisible(false);
            createPane.setManaged(false);
            animateOfflineDividerTo(1.0);
            currentEditingFolderData = null;
            currentEditingTile = null;
            clearCreateForm();
        } else {
            if (currentEditingFolderData == null) {
                clearCreateForm();
            }
            createPane.setVisible(true);
            createPane.setManaged(true);
            animateOfflineDividerTo(0.5);
        }
    }

    private void handleModifyFolder(FolderData folderData, VBox folderTile) {
        this.currentEditingFolderData = folderData;
        this.currentEditingTile = folderTile;
        populateCreatePane(folderData);
        if (!createPane.isVisible()) {
            handleAddFicClick();
        }
    }

    private void saveFolders() {
        if (libraryTilePane == null || folderSaveFile == null) {
            return;
        }

        List<FolderData> allFolders = new ArrayList<>();
        for (javafx.scene.Node node : libraryTilePane.getChildren()) {
            if (node.getUserData() instanceof FolderData) {
                allFolders.add((FolderData) node.getUserData());
            }
        }

        try (FileWriter writer = new FileWriter(folderSaveFile.toFile())) {
            gson.toJson(allFolders, writer);
            System.out.println("Folders saved successfully to " + folderSaveFile);
        } catch (IOException e) {
            System.err.println("Failed to save folders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadFolders() {
        if (libraryTilePane != null) {
            libraryTilePane.getChildren().clear();
        }

        if (folderSaveFile == null || !Files.exists(folderSaveFile)) {
            System.out.println("No folder save file found. Starting fresh.");
            return;
        }

        Type listType = new TypeToken<ArrayList<FolderData>>() {}.getType();

        try (FileReader reader = new FileReader(folderSaveFile.toFile())) {
            List<FolderData> loadedFolders = gson.fromJson(reader, listType);

            if (loadedFolders != null) {
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

    private void populateCreatePane(FolderData data) {
        folderNameField.setText(data.name);
        folderDescriptionArea.setText(data.description);

        newFolderImagePath = data.imagePath;
        if (newFolderImagePath != null && !newFolderImagePath.isBlank()) {
            try {
                Image image = new Image(newFolderImagePath);
                folderImageView.setImage(image);

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
                folderImageView.setImage(null);
                folderImageView.setViewport(null);
                folderImageView.setVisible(false);
                folderImageIcon.setVisible(true);
            }
        } else {
            folderImageView.setImage(null);
            folderImageView.setViewport(null);
            folderImageView.setVisible(false);
            folderImageIcon.setVisible(true);
        }
        ficsForNewFolder.clear();
        newFolderFicsListView.getItems().clear();

        if (data.ficPaths != null) {
            for (String path : data.ficPaths) {
                Work work = findWorkInList(path, unlistedListView.getItems());
                if (work != null) {
                    ficsForNewFolder.add(work);
                    newFolderFicsListView.getItems().add(work);
                } else {
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
                newFolderImagePath = selectedFile.toURI().toString();
                Image image = new Image(newFolderImagePath);

                double width = image.getWidth();
                double height = image.getHeight();

                if (width == height) {
                    folderImageView.setViewport(null);
                } else {
                    double size = Math.min(width, height);
                    double x = (width - size) / 2;
                    double y = (height - size) / 2;
                    Rectangle2D viewport = new Rectangle2D(x, y, size, size);
                    folderImageView.setViewport(viewport);
                }

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

        if (currentEditingFolderData != null) {
            currentEditingFolderData.name = folderName;
            currentEditingFolderData.description = description;
            currentEditingFolderData.imagePath = newFolderImagePath;

            currentEditingFolderData.ficPaths.clear();
            currentEditingFolderData.ficPaths.addAll(
                    ficsForNewFolder.stream()
                            .map(Work::getUrl)
                            .collect(Collectors.toList())
            );

            libraryTilePane.getChildren().remove(currentEditingTile);
            VBox newFolderTile = createFolderTile(currentEditingFolderData);
            libraryTilePane.getChildren().add(newFolderTile);

            currentEditingFolderData = null;
            currentEditingTile = null;

        } else {
            List<String> paths = ficsForNewFolder.stream()
                    .map(Work::getUrl)
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

    @FXML private void onModeSwitchEnter() { }
    @FXML private void onSettingsEnter() { }
    @FXML private void onThemeEnter() { }
    @FXML private void onChangePhotoEnter() { }
    @FXML private void onRefreshEnter() { }
    @FXML private void onViewFolderEnter() { }
    @FXML private void onDeleteStoryEnter() { }
    @FXML private void onMouseExit() { }

    @FXML
    protected void switchMode() {
        isOnlineMode = !isOnlineMode;
        System.out.println("Switch Mode clicked. New state: " + (isOnlineMode ? "Online Search" : "Offline Library"));

        onlineSearchPane.setVisible(isOnlineMode);
        onlineSearchPane.setManaged(isOnlineMode);
        offlineSplitPane.setVisible(!isOnlineMode);
        offlineSplitPane.setManaged(!isOnlineMode);

        if (isOnlineMode) {
            if (modeImageView != null) modeImageView.setImage(offlineModeIcon);
            if (unlistedListView != null) unlistedListView.getSelectionModel().clearSelection();
        } else {
            if (modeImageView != null) modeImageView.setImage(onlineModeIcon);
            populateLibraryViews();
            if (resultsListView != null) resultsListView.getSelectionModel().clearSelection();
        }

        if (createPane != null && createPane.isVisible()) {
            handleAddFicClick();
        }

        if (loadingIndicator != null) loadingIndicator.setVisible(false);
    }

    private void populateLibraryViews() {
        if (unlistedListView == null || libraryTilePane == null) return;

        unlistedListView.getItems().clear();
        if (libraryPath == null) { return; }

        List<Work> offlineWorks = new ArrayList<>();
        List<Path> pathsToDelete = new ArrayList<>();

        try (Stream<Path> stream = Files.list(libraryPath)) {
            stream
                    .filter(p -> p.toString().toLowerCase().endsWith(".json")
                            && !p.getFileName().toString().equals("reviews.json")
                            && !p.getFileName().toString().equals("folders.json"))
                    .forEach(path -> {
                        try (FileReader reader = new FileReader(path.toFile())) {
                            Work offlineWork = gson.fromJson(reader, Work.class);

                            if (offlineWork != null && offlineWork.getUrl() != null
                                    && Files.exists(Paths.get(offlineWork.getUrl()))) {
                                offlineWorks.add(offlineWork);
                            } else if (offlineWork != null) {
                                System.out.println("Orphaned metadata found, marking for deletion: " + path.getFileName());
                                pathsToDelete.add(path);
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to read metadata file: " + path.getFileName());
                            e.printStackTrace();
                            pathsToDelete.add(path);
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not read library directory: " + e.getMessage());
        }

        for (Path path : pathsToDelete) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.err.println("Failed to delete orphaned file: " + path.getFileName());
                e.printStackTrace();
            }
        }

        unlistedListView.getItems().addAll(offlineWorks);
        loadFolders();
    }


    @FXML
    protected void onDeleteStoryClick() {
        Work selectedWork = null;
        if (!isOnlineMode) {
            if (unlistedListView != null) {
                selectedWork = unlistedListView.getSelectionModel().getSelectedItem();
            }
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
                        populateLibraryListView();
                    } else {
                        populateLibraryViews();
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
        SearchQuery newQuery = new SearchQuery(
                anyField.getText(),
                titleField.getText(),
                authorField.getText(),
                tagsField.getText()
        );

        if (!newQuery.toString().isEmpty()){
            historyManager.addQuery(newQuery);
            displaySearchHistory();
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
    }

    public void loadAndShowStory(Work work, List<Work> allWorks) {
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

    public void loadStoryFromLibrary(Work work) {
        if (work == null) return;
        Path filePath = Paths.get(work.getUrl());

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            launchReadingWindow(work.getTitle(), content, true);
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
        alert.setTitle("Project Credits (Easter Egg Found!)");
        alert.setHeaderText("AO3 JavaFX Port Beta - Development Team");

        String content =
                "Lead Developer | Scrum Master: CARONGOY, SIR CARL DOMINIC XV D.\n" +
                        "Developer | Tester: BENJ, KIRBY D.\n" +
                        "Developer | Ideation: MANAL, KIM ANDREI N.\n" +
                        "Developer | Designer: NAVARRO, JOHANSEN L.\n" +
                        "Developer | UI Designer: PENETRANTE, FRANCINE JAE J.\n\n" +

                        "Special Thanks:\n" +
                        " - Gson (Google): JSON Serialization\n" +
                        " - Selenium WebDriver: Web Automation\n" +
                        " - Jsoup: HTML Parsing\n" +
                        " - Ikonli: Icon Library (FontAwesome, Entypo, etc.)\n" +
                        " - Spotify Service: Music Integration\n\n" +
                        "A beta project built for offline reading and enhanced search capabilities.";

        alert.setContentText(content);
        alert.getDialogPane().setPrefWidth(400);
        alert.showAndWait();
    }

    @FXML
    protected void onSettingsButtonClick() {
        System.out.println("Settings button clicked! Showing credits.");
        showCredits();
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
            if (work.getUrl().equals(url)) {
                return work;
            }
        }
        return null;
    }

    private Task<List<Work>> createFetchWorksTask(String query) {
        return new Task<>() {
            @Override
            protected List<Work> call() throws Exception {
                List<Work> worksList = new ArrayList<>();
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
                String url = "https://archiveofourown.org/works/search?work_search[query]=" + encodedQuery;

                Document doc = getDocument(url);

                Elements workElements = doc.select("li.work.blurb");

                if (workElements.isEmpty()) {
                    Element noResultsHeader = doc.selectFirst("h4.heading:contains(No results found)");
                    if (noResultsHeader == null) {
                        System.err.println("DEBUG: Got 0 works, but no 'No results' message.");
                    } else {
                        System.out.println("DEBUG: Found 'No results found' page.");
                    }
                }

                for (Element workEl : workElements) {
                    Element titleEl = workEl.selectFirst("h4.heading a[href^='/works/']");
                    Element authorEl = workEl.selectFirst("a[rel=author]");
                    Element dateEl = workEl.selectFirst("p.datetime");
                    Element fandomEl = workEl.selectFirst("h5.fandoms a");
                    Elements relationshipEls = workEl.select("li.relationships a");
                    Elements characterEls = workEl.select("li.characters a");
                    Elements tagElements = workEl.select("li.freeforms a.tag");

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
                        String fandom = (fandomEl != null) ? fandomEl.text() : "N/A";
                        String relationships = relationshipEls.stream().map(Element::text).collect(Collectors.joining(", "));
                        String characters = characterEls.stream().map(Element::text).collect(Collectors.joining(", "));
                        String tags = tagElements.stream().map(Element::text).collect(Collectors.joining(", "));

                        Element ratingEl = workEl.selectFirst("span.rating span.text");
                        String rating = (ratingEl != null) ? ratingEl.text() : "Not Rated";
                        Element categoryEl = workEl.selectFirst("span.category span.text");
                        String category = (categoryEl != null) ? categoryEl.text() : "No Category";
                        Element warningEl = workEl.selectFirst("span.warnings span.text");
                        String warnings = (warningEl != null) ? warningEl.text() : "Creator Chose Not To Use Archive Warnings";
                        Element completeEl = workEl.selectFirst("span.iswip span.text");
                        String completionStatus = (completeEl != null) ? completeEl.text() : "Complete Work";

                        Work newWork = new Work(title, author, workUrl, tags, lastUpdated,
                                rating, category, warnings, completionStatus,
                                fandom, relationships, characters);
                        newWork.setAuthorUrl(authorUrl);
                        worksList.add(newWork);
                    }
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
                Document doc = getDocument(work.getUrl() + "?view_full_work=true");

                Element workskin = doc.selectFirst("#workskin");
                if (workskin == null) {
                    return "<html><body>Could not find story content. It might be a restricted work.</body></html>";
                }
                return workskin.html();
            }
        };
    }
    public static class FolderData {
        String name;
        String description;
        String imagePath;
        List<String> ficPaths = new ArrayList<>();

        FolderData(String name, String description, String imagePath, List<String> ficPaths) {
            this.name = name;
            this.description = description;
            this.imagePath = imagePath;
            this.ficPaths.addAll(ficPaths);
        }
    }
    private VBox createFolderTile(FolderData folderData) {
        VBox folderTile = new VBox(5);
        folderTile.setPrefSize(130, 130);
        folderTile.setAlignment(Pos.CENTER);
        folderTile.setStyle("-fx-padding: 5; -fx-background-color: -fx-control-inner-background; -fx-cursor: hand;");

        folderTile.setUserData(folderData);

        if (folderData.imagePath != null && !folderData.imagePath.isBlank()) {
            try {
                ImageView tileImageView = new ImageView();
                tileImageView.setFitHeight(80);
                tileImageView.setFitWidth(80);
                tileImageView.setPreserveRatio(true);
                Image tileImage = new Image(folderData.imagePath);
                tileImageView.setImage(tileImage);

                double width = tileImage.getWidth();
                double height = tileImage.getHeight();
                if (width != height) {
                    double size = Math.min(width, height);
                    double x = (width - size) / 2;
                    double y = (height - size) / 2;
                    tileImageView.setViewport(new Rectangle2D(x, y, size, size));
                }
                folderTile.getChildren().add(tileImageView);
            } catch (Exception e) {
                System.err.println("Could not load tile image: " + e.getMessage());
                folderTile.getChildren().add(createFolderPlaceholderIcon());
            }
        } else {
            folderTile.getChildren().add(createFolderPlaceholderIcon());
        }

        Label nameLabel = new Label(folderData.name);
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        folderTile.getChildren().add(nameLabel);

        folderTile.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/FolderView.fxml"));
                    Parent root = loader.load();
                    FolderViewController controller = loader.getController();

                    controller.setMainController(this);

                    List<Work> ficsForFolder = new ArrayList<>();
                    List<Work> allOfflineWorks = unlistedListView.getItems();

                    if (folderData.ficPaths != null) {
                        for (String pathString : folderData.ficPaths) {
                            Work fic = findWorkInList(pathString, allOfflineWorks);
                            if (fic != null) {
                                ficsForFolder.add(fic);
                            } else {
                                System.err.println("Could not find loaded fic for path: " + pathString);
                            }
                        }
                    }

                    controller.loadFolder(folderData, ficsForFolder);

                    Scene newScene = new Scene(root);

                    if (themeButton != null && themeButton.getScene() != null) {
                        newScene.getStylesheets().addAll(themeButton.getScene().getStylesheets());
                    }

                    Stage folderStage = new Stage();
                    folderStage.setTitle(folderData.name);
                    folderStage.setScene(newScene);
                    folderStage.initModality(Modality.NONE);
                    folderStage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    showError("Could not open folder view: " + e.getMessage());
                }
            }
        });

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

        return folderTile;
    }

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
        placeholderIcon.setStyle("-fx-text-fill: #888888;");
        return placeholderIcon;
    }
}