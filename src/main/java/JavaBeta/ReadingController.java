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
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * This is the UPDATED ReadingController.
 * - All recommendation and review logic has been REMOVED.
 * - A new button 'addReviewButton' has been ADDED.
 * - The 'handleAddReviewClick' method opens the new Review window.
 */
public class ReadingController {

    @FXML private ChoiceBox<String> themeChoiceBox;
    @FXML private WebView storyWebView;
    @FXML private Button downloadButton;
    @FXML private Button addReviewButton; // The new button

    // We still need to store these, so we can pass them
    // to the new ReviewController.
    private Work currentWork;
    private List<Work> allWorks;

    // Base HTML structure with placeholders
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

    @FXML
    public void initialize() {
        themeChoiceBox.getItems().addAll("Default", "Sepia", "Dark Mode");
        themeChoiceBox.setValue("Default");
        themeChoiceBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTheme, newTheme) -> updateTheme(newTheme)
        );
        downloadButton.setDisable(true);
        addReviewButton.setDisable(true); // Disable review button by default
        storyWebView.setContextMenuEnabled(false);
    }

    /**
     * This is the NEW loadStory for OFFLINE stories.
     * It's the same as your old one, but we hide the review button.
     */
    public void loadStory(String title, String fileContent, boolean isOffline) {
        this.storyTitle = title;
        this.storyAuthor = "Unknown";
        this.isOfflineStory = isOffline;
        this.currentWork = null; // No Work object for offline
        this.allWorks = null; // No list for offline

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
        downloadButton.setDisable(true); // Disable download for offline
        addReviewButton.setDisable(true); // Disable reviews for offline
    }

    /**
     * This is the NEW loadStory for ONLINE stories.
     * It now saves the work and allWorks list, and enables the review button.
     */
    public void loadStory(Work work, String htmlContent, List<Work> allWorks) {
        this.storyTitle = work.getTitle();
        this.storyAuthor = work.getAuthor();
        this.rawHtmlContent = htmlContent;
        this.isOfflineStory = false;

        // Store these so we can pass them to the review window
        this.currentWork = work;
        this.allWorks = allWorks;

        Stage stage = getStage();
        if (stage != null) stage.setTitle(storyTitle);

        updateWebViewContent("default");
        downloadButton.setDisable(false); // Enable download
        addReviewButton.setDisable(false); // ENABLE review button
    }

    /**
     * This is the NEW method that runs when you click "Add a Review".
     * It opens the new ReviewView.fxml window.
     */
    @FXML
    private void handleAddReviewClick() {
        if (currentWork == null || allWorks == null) {
            showError("Cannot open review window: data is missing.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/JavaBeta/ReviewView.fxml"));
            Parent root = loader.load();

            ReviewController reviewController = loader.getController();
            reviewController.loadData(currentWork, allWorks);

            Stage reviewStage = new Stage();
            reviewStage.setTitle("Add Review for " + currentWork.getTitle());
            reviewStage.setScene(new Scene(root));
            reviewStage.initModality(Modality.WINDOW_MODAL);
            reviewStage.initOwner(getStage());
            reviewStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not open the review window: " + e.getMessage());
        }
    }

    // --- All your other methods (Download, Theme, Helpers) stay the same ---

    @FXML
    protected void onDownloadButtonClick() {
        if (isOfflineStory || storyTitle == null || storyAuthor == null || rawHtmlContent == null || rawHtmlContent.isEmpty()) {
            showError("Cannot download this story (it might be offline or not fully loaded).");
            return;
        }
        Path libraryPath = getLibraryPath();
        if (libraryPath == null) return;
        String safeTitle = storyTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
        String fileName = safeTitle + " - " + storyAuthor + ".html";
        Path filePath = libraryPath.resolve(fileName);
        try {
            Files.writeString(filePath, rawHtmlContent, StandardCharsets.UTF_8);
            showInfo("Download Complete!", "Saved '" + fileName + "' as HTML to your offline library.");
        } catch (IOException e) {
            showError("Could not save file: " + e.getMessage());
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

