package JavaBeta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Work {

    private String title;
    private String author;
    private String url;
    private String tags;
    private String lastUpdated;
    private String rating;
    private String category;
    private String warnings;
    private String completionStatus;
    private String fandom;
    private String relationships;
    private String characters;
    private String authorUrl;

    public Work() {
    }

    public Work(String title, String author, String url, String tags, String lastUpdated,
                String rating, String category, String warnings, String completionStatus,
                String fandom, String relationships, String characters) {
        this.title = title;
        this.author = author;
        this.url = url;
        this.tags = tags;
        this.lastUpdated = lastUpdated;
        this.rating = rating;
        this.category = category;
        this.warnings = warnings;
        this.completionStatus = completionStatus;
        this.fandom = fandom;
        this.relationships = relationships;
        this.characters = characters;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }

    public String getTags() {
        return tags;
    }

    public Set<String> getTagsSet() {
        if (this.tags == null || this.tags.isBlank()) {
            return new HashSet<>();
        }

        Set<String> tagSet = Arrays.stream(this.tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toSet());

        return tagSet;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public String getRating() {
        return rating;
    }

    public String getCategory() {
        return category;
    }

    public String getWarnings() {
        return warnings;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public String getFandom() {
        return fandom;
    }

    public String getRelationships() {
        return relationships;
    }

    public String getCharacters() {
        return characters;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}