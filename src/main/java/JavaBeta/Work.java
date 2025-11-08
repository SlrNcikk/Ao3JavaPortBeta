package JavaBeta;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class Work {
    // --- Properties (Transient) ---
    // We add 'transient' to tell Gson to IGNORE these fields.
    // This stops the StackOverflowError.
    private transient StringProperty title;
    private transient StringProperty author;
    private transient StringProperty url;
    private transient StringProperty tagsString;
    private transient StringProperty lastUpdated;
    private transient StringProperty authorUrl;

    // --- Standard Fields (Saved by Gson) ---
    // These are the simple fields Gson WILL save.
    private final String rating;
    private final String category;
    private final String warnings;
    private final String completionStatus;
    private final String fandom;
    private final String relationships;
    private final String characters;

    // --- NEW Backing Fields (Saved by Gson) ---
    // These new fields will hold the data for the transient properties.
    private String s_title;
    private String s_author;
    private String s_url;
    private String s_tagsString;
    private String s_lastUpdated;
    private String s_authorUrl;

    private transient Set<String> tagsSet; // Also make this transient

    // --- CONSTRUCTOR 1: The 12-argument one (for online search) ---
    public Work(String title, String author, String url, String tags, String lastUpdated,
                String rating, String category, String warnings, String completionStatus,
                String fandom, String relationships, String characters) {

        // Set the transient JavaFX properties
        this.title = new SimpleStringProperty(title);
        this.author = new SimpleStringProperty(author);
        this.url = new SimpleStringProperty(url);
        this.tagsString = new SimpleStringProperty(tags);
        this.lastUpdated = new SimpleStringProperty(lastUpdated);

        // Set the NEW backing strings
        this.s_title = title;
        this.s_author = author;
        this.s_url = url;
        this.s_tagsString = tags;
        this.s_lastUpdated = lastUpdated;

        // Set standard fields
        this.rating = rating;
        this.category = category;
        this.warnings = warnings;
        this.completionStatus = completionStatus;
        this.fandom = fandom;
        this.relationships = relationships;
        this.characters = characters;
    }

    // --- CONSTRUCTOR 2: The 5-argument one (for offline fics) ---
    public Work(String title, String author, String url, String tags, String lastUpdated) {
        // Set the transient JavaFX properties
        this.title = new SimpleStringProperty(title);
        this.author = new SimpleStringProperty(author);
        this.url = new SimpleStringProperty(url);
        this.tagsString = new SimpleStringProperty(tags);
        this.lastUpdated = new SimpleStringProperty(lastUpdated);

        // Set the NEW backing strings
        this.s_title = title;
        this.s_author = author;
        this.s_url = url;
        this.s_tagsString = tags;
        this.s_lastUpdated = lastUpdated;

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
     * Helper method to initialize transient fields after Gson loads.
     * This checks if a property is null and creates it from the backing string.
     */
    private void initTransientFields() {
        if (title == null) title = new SimpleStringProperty(s_title);
        if (author == null) author = new SimpleStringProperty(s_author);
        if (url == null) url = new SimpleStringProperty(s_url);
        if (tagsString == null) tagsString = new SimpleStringProperty(s_tagsString);
        if (lastUpdated == null) lastUpdated = new SimpleStringProperty(s_lastUpdated);
        // authorUrl is handled by its getter
    }

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
    // These are now "lazy" and will initialize the property if it's null
    public StringProperty titleProperty() {
        if (title == null) title = new SimpleStringProperty(s_title);
        return title;
    }
    public StringProperty authorProperty() {
        if (author == null) author = new SimpleStringProperty(s_author);
        return author;
    }
    public StringProperty tagsProperty() {
        if (tagsString == null) tagsString = new SimpleStringProperty(s_tagsString);
        return tagsString;
    }
    public StringProperty lastUpdatedProperty() {
        if (lastUpdated == null) lastUpdated = new SimpleStringProperty(s_lastUpdated);
        return lastUpdated;
    }

    public StringProperty authorUrlProperty() {
        if (authorUrl == null) {
            authorUrl = new SimpleStringProperty(this, "authorUrl", s_authorUrl);
        }
        return authorUrl;
    }

    // --- Standard Getters (for other logic) ---
    // These now check the property first, then the backing string
    public String getTitle() {
        return (title != null) ? title.get() : s_title;
    }
    public String getAuthor() {
        return (author != null) ? author.get() : s_author;
    }
    public String getUrl() {
        return (url != null) ? url.get() : s_url;
    }
    public String getTags() {
        return (tagsString != null) ? tagsString.get() : s_tagsString;
    }
    public String getLastUpdated() {
        return (lastUpdated != null) ? lastUpdated.get() : s_lastUpdated;
    }

    public String getAuthorUrl() {
        return (authorUrl != null) ? authorUrl.get() : s_authorUrl;
    }

    public void setAuthorUrl(String authorUrl) {
        this.s_authorUrl = authorUrl; // Save to the backing field
        authorUrlProperty().set(authorUrl); // Set the (now initialized) property
    }

    public String getRating() { return rating; }
    public String getCategory() { return category; }
    public String getWarnings() { return warnings; }
    public String getCompletionStatus() { return completionStatus; }
    public String getFandom() { return fandom; }
    public String getRelationships() { return relationships; }
    public String getCharacters() { return characters; }

    public Set<String> getTagsSet() {
        if (tagsSet == null) {
            tagsSet = parseTags(getTags());
        }
        return tagsSet;
    }

    @Override
    public String toString() {
        return getTitle() + " by " + getAuthor();
    }
}