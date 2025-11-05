package JavaBeta;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is File 6 (Update This File): Work.java
 * This is the UPDATED Work data model.
 * It now uses JavaFX Properties for better table binding.
 * It also includes logic to parse the 'tags' string into a Set.
 */
public class Work {
    // Use StringProperty for JavaFX TableView bindings
    private final StringProperty title;
    private final StringProperty author;
    private final StringProperty url;
    private final StringProperty tagsString; // This is the raw string "Tag1, Tag2, ..."
    private final StringProperty lastUpdated;

    // This is the new Set for the recommendation engine
    private final Set<String> tagsSet;

    public Work(String title, String author, String url, String tags, String lastUpdated) {
        this.title = new SimpleStringProperty(title);
        this.author = new SimpleStringProperty(author);
        this.url = new SimpleStringProperty(url);
        this.tagsString = new SimpleStringProperty(tags);
        this.lastUpdated = new SimpleStringProperty(lastUpdated);

        // This is the new logic to parse the tags string
        this.tagsSet = parseTags(tags);
    }

    /**
     * NEW: Parses the raw tag string into a Set of individual tags.
     */
    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank() || tags.equals("N/A") || tags.endsWith("...")) {
            // If tags are incomplete (end with "..."), treat them as empty
            // to avoid bad recommendations.
            // We'll fix this in Ao3Controller's scraper.
            if (tags != null && !tags.equals("N/A") && !tags.endsWith("...")) {
                // Fallback for tags like "Fluff" (no comma)
                return Set.of(tags.trim());
            }
            if (tags == null || tags.isBlank() || tags.equals("N/A")) {
                return Collections.emptySet();
            }
        }

        // Split the string by ", " and trim any extra whitespace
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                // Filter out the "..." tag if it exists
                .filter(tag -> !tag.isEmpty() && !tag.equals("..."))
                .collect(Collectors.toSet());
    }

    // --- Getters for Properties (for TableView) ---
    public StringProperty titleProperty() { return title; }
    public StringProperty authorProperty() { return author; }

    // IMPORTANT: This tells the TableView to use the "tagsString" variable
    // for the column that is mapped to the "tags" property.
    // Since your Ao3Controller uses "tags", we rename this.
    public StringProperty tagsProperty() { return tagsString; }

    public StringProperty lastUpdatedProperty() { return lastUpdated; }

    // --- Standard Getters (for other logic) ---
    public String getTitle() { return title.get(); }
    public String getAuthor() { return author.get(); }
    public String getUrl() { return url.get(); }

    // This getter is used by the tagsProperty() method
    public String getTags() { return tagsString.get(); }

    public String getLastUpdated() { return lastUpdated.get(); }

    /**
     * NEW: Getter for the recommendation engine.
     */
    public Set<String> getTagsSet() {
        return tagsSet;
    }

    @Override
    public String toString() {
        // ListView uses toString() by default, so make it look good
        // This is what will appear in the recommendations list
        return title.get() + " by " + author.get();
    }
}

