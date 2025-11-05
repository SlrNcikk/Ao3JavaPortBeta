package JavaBeta;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.util.List;

/**
 * This is File 4 (New File): ReviewController.java
 * This is the controller for the new ReviewView.fxml popup window.
 */
public class ReviewController {

    // --- FXML UI Elements ---
    @FXML private Label storyTitleLabel;
    @FXML private Slider ratingSlider;
    @FXML private TextArea commentTextArea;
    @FXML private Button submitReviewButton;
    @FXML private ListView<Work> recommendationsListView;

    // --- Data from ReadingController ---
    private Work currentWork;
    private RecommendationService recommendationService;

    /**
     * This is NOT initialize(). This method is called by ReadingController
     * to pass in the story data.
     */
    public void loadData(Work work, List<Work> allWorks) {
        this.currentWork = work;
        this.recommendationService = new RecommendationService(allWorks);

        // Update the UI with the story title
        storyTitleLabel.setText("Leave a review for: " + currentWork.getTitle());

        // Set prompt text for recommendations
        recommendationsListView.setPlaceholder(new Label("Submit a 4+ star review to see recommendations."));
    }

    /**
     * Called when the "Submit Review" button is clicked.
     */
    @FXML
    private void handleSubmitReview() {
        int rating = (int) Math.round(ratingSlider.getValue());
        String comment = commentTextArea.getText();

        if (rating == 0) {
            showError("Please select a rating (1-5 stars).");
            return;
        }

        // --- 1. "Save" the Review (currently just prints it) ---
        // TODO: Add logic here to save the rating/comment to a file if you want
        System.out.println("--- Review Submitted ---");
        System.out.println("Story: " + currentWork.getTitle());
        System.out.println("Rating: " + rating + "/5");
        System.out.println("Comment: " + comment);
        System.out.println("-------------------------");

        // --- 2. Update the Recommendations List ---
        recommendationsListView.getItems().clear();

        if (rating >= 4) {
            // Get recommendations from the service
            List<Work> recs = recommendationService.getRecommendations(currentWork);

            if (recs.isEmpty()) {
                recommendationsListView.setPlaceholder(new Label("No other fictions with similar tags were found."));
            } else {
                recommendationsListView.getItems().addAll(recs);
            }
        } else {
            recommendationsListView.setPlaceholder(new Label("Give a 4 or 5 star rating to see recommendations."));
        }

        // --- 3. Close the window ---
        // We can optionally close the window after submitting
        // Stage stage = (Stage) submitReviewButton.getScene().getWindow();
        // stage.close();

        // Or, just disable the button to prevent re-submissions
        submitReviewButton.setText("Review Submitted!");
        submitReviewButton.setDisable(true);
    }


    // --- Helper Methods ---

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

