package JavaBeta;

public class Work {
    private final String title;
    private final String author;
    private final String url;
    private final String tags;
    private final String lastUpdated;

    public Work(String title, String author, String url, String tags, String lastUpdated) {
        this.title = title;
        this.author = author;
        this.url = url;
        this.tags = tags;
        this.lastUpdated = lastUpdated;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getUrl() { return url; }
    public String getTags() { return tags; }
    public String getLastUpdated() { return lastUpdated; }

    @Override
    public String toString() {
        return title + " by " + author;
    }
}
