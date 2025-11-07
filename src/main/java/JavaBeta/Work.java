package JavaBeta;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Work {
    // --- Properties ---
    private final StringProperty title;
    private final StringProperty author;
    private final StringProperty url;
    private final StringProperty tagsString; // This is the raw string "Tag1, Tag2, ..."
    private final StringProperty lastUpdated;
    private StringProperty authorUrl;

    // --- Standard Fields ---
    private final String rating;
    private final String category;
    private final String warnings;
    private final String completionStatus;

    // ✅ --- THESE ARE THE FIELDS YOU WERE MISSING ---
    private final String fandom;
    private final String relationships;
    private final String characters;
    // --- END OF MISSING FIELDS ---

    private final Set<String> tagsSet;

    // --- CONSTRUCTOR 1: The 12-argument one (for online search) ---
    // ✅ --- THIS IS THE CONSTRUCTOR YOU WERE MISSING ---
    public Work(String title, String author, String url, String tags, String lastUpdated,
                String rating, String category, String warnings, String completionStatus,
                String fandom, String relationships, String characters) {

        this.title = new SimpleStringProperty(title);
        this.author = new SimpleStringProperty(author);
        this.url = new SimpleStringProperty(url);
        this.tagsString = new SimpleStringProperty(tags);
        this.lastUpdated = new SimpleStringProperty(lastUpdated);

        // Set standard fields
        this.rating = rating;
        this.category = category;
        this.warnings = warnings;
        this.completionStatus = completionStatus;

        // Set new fields
        this.fandom = fandom;
        this.relationships = relationships;
        this.characters = characters;

        this.tagsSet = parseTags(tags);
    }

    // --- CONSTRUCTOR 2: The 5-argument one (for offline fics) ---
    // ✅ --- THIS IS THE UPDATED 5-ARGUMENT CONSTRUCTOR ---
    public Work(String title, String author, String url, String tags, String lastUpdated) {

        // Set the 5 fields you have
        this.title = new SimpleStringProperty(title);
        this.author = new SimpleStringProperty(author);
        this.url = new SimpleStringProperty(url);
        this.tagsString = new SimpleStringProperty(tags);
        this.lastUpdated = new SimpleStringProperty(lastUpdated);

        this.tagsSet = parseTags(tags);

        // Set default "N/A" values for all other fields
        this.rating = "N/A";
        this.category = "N/A";
        this.warnings = "N/A";
        this.completionStatus = "N/A";
        this.fandom = "N/A";
        this.relationships = "N/A";
        this.characters = "N/A";
    }

    /**
     * Parses the raw tag string into a Set of individual tags.
     */
    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank() || tags.equals("N/A") || tags.endsWith("...")) {
            return Collections.emptySet();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toSet());
    }

    // --- Getters for Properties (for TableView) ---
    public StringProperty titleProperty() { return title; }
    public StringProperty authorProperty() { return author; }
    public StringProperty tagsProperty() { return tagsString; }
    public StringProperty lastUpdatedProperty() { return lastUpdated; }

    public StringProperty authorUrlProperty() {
        if (authorUrl == null) {
            authorUrl = new SimpleStringProperty(this, "authorUrl");
        }
        return authorUrl;
    }

    // --- Standard Getters (for other logic) ---
    public String getTitle() { return title.get(); }
    public String getAuthor() { return author.get(); }
    public String getUrl() { return url.get(); }
    public String getTags() { return tagsString.get(); }
    public String getLastUpdated() { return lastUpdated.get(); }

    public String getAuthorUrl() {
        return authorUrlProperty().get();
    }

    public void setAuthorUrl(String authorUrl) {
        this.authorUrlProperty().set(authorUrl);
    }

    public String getRating() { return rating; }
    public String getCategory() { return category; }
    public String getWarnings() { return warnings; }
    public String getCompletionStatus() { return completionStatus; }

    // ✅ --- THESE GETTERS WILL NOW WORK ---
    public String getFandom() { return fandom; }
    public String getRelationships() { return relationships; }
    public String getCharacters() { return characters; }
    // ---

    public Set<String> getTagsSet() {
        return tagsSet;
    }

    @Override
    public String toString() {
        return title.get() + " by " + author.get();
    }
}