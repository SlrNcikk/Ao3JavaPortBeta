package JavaBeta; // Adjust package name if necessary

import java.util.ArrayList;
import java.util.List;

public class SearchQuery {
    private String anyField;
    private String title;
    private String author;
    private String tags;

    public SearchQuery() {}

    public SearchQuery(String anyField, String title, String author, String tags) {
        this.anyField = anyField == null ? "" : anyField.trim();
        this.title = title == null ? "" : title.trim();
        this.author = author == null ? "" : author.trim();
        this.tags = tags == null ? "" : tags.trim();
    }

    // Getters and Setters (needed for JSON serialization/deserialization)
    public String getAnyField() { return anyField; }
    public void setAnyField(String anyField) { this.anyField = anyField; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    /**
     * Generates the concise, comma-separated string for the history tag display.
     */
    @Override
    public String toString() {
        List<String> parts = new ArrayList<>();
        if (!anyField.isEmpty()) { parts.add(anyField); }
        if (!title.isEmpty()) { parts.add(title); }
        if (!author.isEmpty()) { parts.add(author); }
        if (!tags.isEmpty()) { parts.add(tags); }
        return String.join(", ", parts);
    }
}