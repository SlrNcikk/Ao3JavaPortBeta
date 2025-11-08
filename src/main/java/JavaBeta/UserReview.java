package JavaBeta;

import java.time.LocalDate; // Needed for the date reviewed

public class UserReview {

    // --- Data Fields ---
    // Renamed 'username' to 'workUrl' for clarity, as the Work object has the real author/title
    // The review logic will use the Work's title and URL.

    private String workUrl;
    private String workTitle;
    private int rating;                 // Tracks the 1-5 star rating
    private String reviewText;
    private String dateReviewed;         // Tracks the last date reviewed

    // Constructor (Updated to match the data being saved from ReadingController)
    public UserReview(String workUrl, String workTitle, int rating, String reviewText) {
        this.workUrl = workUrl;
        this.workTitle = workTitle;
        this.rating = rating;
        this.reviewText = reviewText;
        this.dateReviewed = LocalDate.now().toString(); // Initialize date
    }

    // Default no-arg constructor (Required by Gson)
    public UserReview() {
    }

    // --- Getters ---
    public String getWorkUrl() {
        return workUrl;
    }

    public String getWorkTitle() {
        return workTitle;
    }

    // ✅ NEW: Getter for the rating
    public int getRating() {
        return rating;
    }

    // ✅ NEW: Getter for the date
    public String getDateReviewed() {
        return dateReviewed;
    }

    public String getReviewText() {
        return reviewText;
    }

    // --- Setters (Required by your ReadingController to UPDATE an existing review) ---

    // ✅ NEW: Setter for the rating
    public void setRating(int rating) {
        this.rating = rating;
    }

    // ✅ NEW: Setter for the date
    public void setDateReviewed(String dateReviewed) {
        this.dateReviewed = dateReviewed;
    }

    // ✅ NEW: Setter for the review text
    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    // Note: We don't typically need setters for final unique IDs like workUrl and workTitle
}