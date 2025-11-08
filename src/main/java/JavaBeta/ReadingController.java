package JavaBeta;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import com.google.gson.Gson;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;
import javafx.scene.control.ListView;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * This is the fixed ReadingController, now delegating review submission to a separate window.
 */
public class ReadingController {

    @FXML private ChoiceBox<String> themeChoiceBox;
    @FXML private WebView storyWebView;
    @FXML private Button downloadButton;
    // ❌ NOTE: The previous inline review FXML elements (reviewBox, ratingChoiceBox, reviewTextArea, saveReviewButton) have been removed.

    // --- Review Data Fields ---
    private Map<String, UserReview> reviewMap = new HashMap<>();
    private Path reviewsFilePath;

    private Work currentWork;
    private List<Work> allWorks;



    // Base HTML structure with placeholders (omitted for brevity)
    private final String HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { margin: 20px; font-family: sans-serif; }
                .default { background-color: white; color: black; }
                .sepia { background-color: #fbf0d9; color: #5b4636; }
                .dark { background-color: #1e1e1e; color: #dcdcdc; }
                p { margin-bottom: 1em; line-height: 1.5; }
                h1, h2, h3, h4, h5, h6 { margin-top: 1.5em; margin-bottom: 0.5em; }
                hr { border: none; border-top: 1px solid #ccc; margin: 2em 0; }
                a { color: #990000; text-decoration: none; }
            </style>
        </head>
        <body class="%s">
            %s
        </body>
        </html>
        """;

    private String storyTitle;
    private String storyAuthor;
    private String rawHtmlContent;
    private boolean isOfflineStory = false;
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        themeChoiceBox.getItems().addAll("Default", "Sepia", "Dark Mode");
        themeChoiceBox.setValue("Default");
        themeChoiceBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTheme, newTheme) -> updateTheme(newTheme)
        );
        downloadButton.setDisable(true);
        // ❌ NOTE: Inline review UI setup removed here.
    }


    public void loadStory(String title, String fileContent, boolean isOffline) {
        this.storyTitle = title;
        this.storyAuthor = "Unknown";
        this.isOfflineStory = isOffline;
        this.currentWork = null;
        this.allWorks = null;

        if (fileContent != null && (fileContent.trim().toLowerCase().startsWith("<!doctype") || fileContent.trim().startsWith("<p"))) {
            this.rawHtmlContent = fileContent;
        } else if (fileContent != null) {
            this.rawHtmlContent = "<p>" + fileContent.replace("\n", "</p><p>") + "</p>";
        } else {
            this.rawHtmlContent = "<html><body>Error: Could not load content.</body></html>";
        }

        Stage stage = getStage();
        if (stage != null) stage.setTitle(title);

        updateWebViewContent("default");
        downloadButton.setDisable(true);
    }

    /**
     * This is the loadStory for ONLINE stories.
     */
    public void loadStory(Work work, String htmlContent, List<Work> allWorks) {
        this.storyTitle = work.getTitle();
        this.storyAuthor = work.getAuthor();
        this.rawHtmlContent = htmlContent;
        this.isOfflineStory = false;

        this.currentWork = work;
        this.allWorks = allWorks;

        Stage stage = getStage();
        if (stage != null) stage.setTitle(storyTitle);

        updateWebViewContent("default");
        downloadButton.setDisable(false);
        loadReviewMap(); // Load existing data for potential review check
    }

    // --- Review Persistence Logic ---

    private Path getReviewsFilePath() {
        if (reviewsFilePath != null) {
            return reviewsFilePath;
        }
        Path libraryPath = getLibraryPath();
        if (libraryPath != null) {
            reviewsFilePath = libraryPath.resolve("reviews.json");
            return reviewsFilePath;
        }
        return null;
    }

    /**
     * Loads the reviews.json file into the reviewMap.
     */
    private void loadReviewMap() {
        Path path = getReviewsFilePath();
        if (path == null) {
            reviewMap = new HashMap<>();
            return;
        }

        Type type = new TypeToken<HashMap<String, UserReview>>() {}.getType();
        try (FileReader reader = new FileReader(path.toFile())) {
            reviewMap = gson.fromJson(reader, type);
            if (reviewMap == null) {
                reviewMap = new HashMap<>();
            }
            // ❌ NOTE: UI population logic removed here.

        } catch (IOException e) {
            System.err.println("Could not load reviews file. Starting fresh.");
            reviewMap = new HashMap<>();
        }
    }

    /**
     * Launches the modal window for review submission.
     */
    @FXML
    protected void onAddReviewButtonClick() {
        if (currentWork == null || isOfflineStory) {
            showError("Cannot add a review. The story is offline or not fully loaded.");
            return;
        }

        try {
            // Load the FXML file for the new review window
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ReviewView.fxml"));
            Parent root = fxmlLoader.load();

            ReviewController controller = fxmlLoader.getController();

            // ✅ FIX: Using the correct method name (initData) and passing all 4 required parameters in the correct order.
            controller.initData(
                    currentWork,
                    allWorks, // Required for recommendations
                    reviewMap,
                    getReviewsFilePath()
            );

            // Create a new stage (window) for the review submission
            Stage stage = new Stage();
            stage.setTitle("Add Review for " + currentWork.getTitle());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Blocks main window
            stage.showAndWait();

            // Reload the review map after the submission window is closed
            loadReviewMap();

        } catch (IOException e) {
            showError("Failed to open review submission window. Check if ReviewSubmissionView.fxml and ReviewSubmissionController.java exist: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void onDownloadButtonClick() {
        // 1. Check if we have the metadata (currentWork)
        if (isOfflineStory || currentWork == null || rawHtmlContent == null || rawHtmlContent.isEmpty()) {
            showError("Cannot download this story (it might be offline, or metadata is missing).");
            return;
        }

        Path libraryPath = getLibraryPath();
        if (libraryPath == null) {
            showError("Could not find library path.");
            return;
        }

        // 2. Create a base filename (no extension)
        // We use currentWork for accuracy, not storyTitle/storyAuthor
        String safeTitle = currentWork.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
        String safeAuthor = currentWork.getAuthor().replaceAll("[\\\\/:*?\"<>|]", "_");
        String baseFileName = safeTitle + " - " + safeAuthor;

        // 3. Define paths for BOTH files
        Path htmlFilePath = libraryPath.resolve(baseFileName + ".html");
        Path metaFilePath = libraryPath.resolve(baseFileName + ".json");

        try {
            // 4. Save the HTML file (your existing logic)
            Files.writeString(htmlFilePath, rawHtmlContent, StandardCharsets.UTF_8);

            // 5. Create a new 'Work' object that points to the LOCAL HTML file
            Work offlineCopy = new Work(
                    currentWork.getTitle(),
                    currentWork.getAuthor(),
                    htmlFilePath.toFile().getAbsolutePath(), // Use the local .html file path
                    currentWork.getTags(),
                    currentWork.getLastUpdated(),
                    currentWork.getRating(),
                    currentWork.getCategory(),
                    currentWork.getWarnings(),
                    currentWork.getCompletionStatus(),
                    currentWork.getFandom(),
                    currentWork.getRelationships(),
                    currentWork.getCharacters()
            );
            offlineCopy.setAuthorUrl(currentWork.getAuthorUrl());

            // 6. Save the metadata as a .json file
            try (FileWriter writer = new FileWriter(metaFilePath.toFile())) {
                gson.toJson(offlineCopy, writer);
            }

            // 7. Show one success message for both
            showInfo("Download Complete!", "Saved story and metadata for '" + baseFileName + "' to your offline library.");

        } catch (IOException e) {
            showError("Could not save files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTheme(String themeName) {
        String themeClass = "default";
        if ("Sepia".equals(themeName)) {
            themeClass = "sepia";
        } else if ("Dark Mode".equals(themeName)) {
            themeClass = "dark";
        }
        updateWebViewContent(themeClass);
    }

    private void updateWebViewContent(String themeClass) {
        if (rawHtmlContent != null && storyWebView != null && storyWebView.getEngine() != null) {
            String styledHtml = String.format(HTML_TEMPLATE, themeClass, rawHtmlContent);
            storyWebView.getEngine().loadContent(styledHtml);
        } else if (storyWebView != null && storyWebView.getEngine() != null){
            storyWebView.getEngine().loadContent("<html><body>Error: No content available to display.</body></html>");
        }
    }

    private Path getLibraryPath() {
        try {
            Path path = Paths.get(System.getProperty("user.home"), "AO3_Offline_Library");
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path;
        } catch (IOException e) {
            showError("Could not access library directory: " + e.getMessage());
            return null;
        }
    }

    private Stage getStage() {
        if (storyWebView != null && storyWebView.getScene() != null) {
            return (Stage) storyWebView.getScene().getWindow();
        }
        return null;
    }

    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}