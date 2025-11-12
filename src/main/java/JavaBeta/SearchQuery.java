package JavaBeta;

import java.util.ArrayList;
import java.util.List;

public class SearchQuery {
    private String anyField;
    private String title;
    private String author;
    private String tags;

    // Default constructor needed for JSON deserialization (using Gson/Jackson)
    public SearchQuery() {
    }

    // Constructor to create a new query from the controller
    public SearchQuery(String anyField, String title, String author, String tags) {
        this.anyField = anyField == null ? "" : anyField.trim();
        this.title = title == null ? "" : title.trim();
        this.author = author == null ? "" : author.trim();
        this.tags = tags == null ? "" : tags.trim();
    }

    // Getters and Setters (needed for JSON serialization/deserialization)
    public String getAnyField() { return anyField; }
    public void setAnyField(String anyField) { this.anyField = anyField; }
    // ... add similar getters/setters for title, author, and tags

    /**
     * Generates the concise, comma-separated string for the history tag display.
     */
    @Override
    public String toString() {
        // Collect all non-empty fields into a list
        List<String> parts = new ArrayList<>();
        if (!anyField.isEmpty()) { parts.add(anyField); }
        if (!title.isEmpty()) { parts.add(title); }
        if (!author.isEmpty()) { parts.add(author); }
        if (!tags.isEmpty()) { parts.add(tags); }

        // Join them with a comma and space
        return String.join(", ", parts);
    }
}