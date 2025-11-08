package JavaBeta;

import javafx.scene.control.ListCell;

public class ReviewCellController extends ListCell<UserReview> {

    @Override
    protected void updateItem(UserReview review, boolean empty) {
        super.updateItem(review, empty);

        if (empty || review == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(
                    review.getWorkTitle() + "\n" +
                            "\"" + review.getReviewText() + "\""
            );
        }
    }
}
