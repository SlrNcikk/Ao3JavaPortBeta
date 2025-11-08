package JavaBeta;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import com.google.gson.Gson;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate; // Needed for date logging
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the controller for the new Review Submission popup window, now handling persistence.
 */
public class ReviewController {

    // --- FXML UI Elements ---
    @FXML private Label storyTitleLabel;
    @FXML private Slider ratingSlider;
    @FXML private TextArea commentTextArea;
    @FXML private Button submitReviewButton;
    @FXML private Button cancelButton; // Assuming this is added to FXML
    @FXML private ListView<Work> recommendationsListView;

    // --- Data from ReadingController & Persistence Fields ---
    private final Gson gson = new Gson();
    private Work currentWork;
    private List<Work> allWorks;
    private Map<String, UserReview> reviewMap;
    private Path reviewsFilePath;
    private RecommendationService recommendationService;

    @FXML
    public void initialize() {
        // Sets the default slider value and ensures ticks are visible.
        if (ratingSlider != null) {
            ratingSlider.setValue(5.0);
        }
    }

    /**
     * Called by ReadingController to pass all necessary data.
     */
    public void initData(Work work, List<Work> allWorks, Map<String, UserReview> existingReviews, Path savePath) {
        this.currentWork = work;
        this.allWorks = allWorks;
        this.reviewMap = existingReviews != null ? existingReviews : new HashMap<>();
        this.reviewsFilePath = savePath;

        // ✅ Initialize Recommendation Service
        this.recommendationService = new RecommendationService(
                this.allWorks != null ? this.allWorks : new ArrayList<>()
        );
        // --- UI Updates and Pre-filling ---
        storyTitleLabel.setText("Leave a review for: " + currentWork.getTitle());

        if (reviewMap.containsKey(currentWork.getUrl())) {
            UserReview existingReview = reviewMap.get(currentWork.getUrl());
            ratingSlider.setValue(existingReview.getRating());
            commentTextArea.setText(existingReview.getReviewText());
            submitReviewButton.setText("Update Review");
            updateRecommendations(existingReview.getRating());
        } else {
            recommendationsListView.setPlaceholder(new Label("Submit a 4+ star review to see recommendations."));
        }
    }

    @FXML
    private void handleSubmitReview() {
        int rating = (int) Math.round(ratingSlider.getValue());
        String comment = commentTextArea.getText();

        if (rating < 1 || rating > 5) {
            showError("Please select a rating between 1 and 5 stars.");
            return;
        }
        String workUrl = currentWork.getUrl();

        // --- 1. Update/Create Review Object (PERSISTENCE LOGIC START) ---
        UserReview review = reviewMap.getOrDefault(workUrl,
                new UserReview(workUrl, currentWork.getTitle(), rating, comment)
        );

        // Update the data
        review.setRating(rating);
        review.setReviewText(comment);
        review.setDateReviewed(LocalDate.now().toString()); // Log current date
        reviewMap.put(workUrl, review);

        // --- 2. Save to File ---
        if (reviewsFilePath == null) {
            showError("Could not find library path to save review.");
            return;
        }

        try (FileWriter writer = new FileWriter(reviewsFilePath.toFile())) {
            gson.toJson(reviewMap, writer);
            // Optionally, show a transient success message here instead of a blocking alert
        } catch (IOException e) {
            showError("Failed to save review: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        // (PERSISTENCE LOGIC END)

        // --- 3. Update the Recommendations List ---
        updateRecommendations(rating);

        submitReviewButton.setText("Review Submitted!");
        submitReviewButton.setDisable(true);
    }

    private void updateRecommendations(int rating) {
        recommendationsListView.getItems().clear();

        if (rating >= 4) {
            if (recommendationService != null) {
                List<Work> recs = recommendationService.getRecommendations(currentWork);
                if (recs.isEmpty()) {
                    recommendationsListView.setPlaceholder(new Label("No other fictions with similar tags were found."));
                } else {
                    recommendationsListView.getItems().addAll(recs);
                }
            } else {
                recommendationsListView.setPlaceholder(new Label("Recommendation service not initialized."));
            }
        } else {
            recommendationsListView.setPlaceholder(new Label("Give a 4 or 5 star rating to see recommendations."));
        }
    }

    // --- Helper/Control Methods ---

    @FXML
    private void handleCancel() {
        // Closes the modal window
        Stage stage = (Stage) (cancelButton != null ? cancelButton.getScene().getWindow() : submitReviewButton.getScene().getWindow());
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}