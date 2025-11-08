package JavaBeta;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import java.util.Collection;

public class ReviewLibraryController {

    @FXML
    private ListView<UserReview> reviewListView;

    private Ao3Controller mainController;

    public void initialize() {
        // We'll tell the ListView to use a custom cell, just like your WorkCell
        reviewListView.setCellFactory(listView -> new ReviewCellController());
    }

    // This method will be called by Ao3Controller to pass in the data
    public void loadReviews(Collection<UserReview> reviews, Ao3Controller mainController) {
        this.mainController = mainController;
        reviewListView.setItems(FXCollections.observableArrayList(reviews));

        // Add click handler to open the fic (optional, but cool)
        reviewListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                UserReview review = reviewListView.getSelectionModel().getSelectedItem();
                if (review != null && review.getWorkUrl() != null) {
                    // This is a bit tricky, we need to find the Work object
                    // For now, let's just log it. We can implement this later.
                    System.out.println("Clicked on fic: " + review.getWorkTitle());
                }
            }
        });
    }
}