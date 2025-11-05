package JavaBeta;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class AuthorProfileController {

    // --- FXML Fields ---
    @FXML private ImageView pfpImageView;
    @FXML private VBox profileDetailsBox;
    @FXML private TextArea personalNoteArea;
    @FXML private Label authorNameLabel;

    @FXML private Tab creationsTab;
    @FXML private Tab bookmarksTab;
    @FXML private Tab collectionsTab;

    @FXML private ListView<String> creationsListView;
    @FXML private ListView<String> bookmarksListView;
    @FXML private ListView<String> collectionsListView;

    // --- Class Fields ---
    private String authorUrl;

    /**
     * This method is called once the FXML is loaded.
     * We set up listeners to load tab content *only* when a tab is clicked.
     */
    @FXML
    private void initialize() {
        creationsTab.setOnSelectionChanged(event -> {
            if (creationsTab.isSelected()) {
                loadCreations();
            }
        });

        bookmarksTab.setOnSelectionChanged(event -> {
            if (bookmarksTab.isSelected()) {
                loadBookmarks();
            }
        });

        collectionsTab.setOnSelectionChanged(event -> {
            if (collectionsTab.isSelected()) {
                loadCollections();
            }
        });
    }

    /**
     * This is the main entry point, called by MainController.
     * It scrapes the main profile info (PFP, Joined Date, Bio).
     */
    public void loadAuthorData(String authorName, String authorUrl) {
        this.authorUrl = authorUrl;
        authorNameLabel.setText(authorName);

        // Start a new thread for network scraping
        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(authorUrl).get();

                // 1. Scrape PFP
                Element pfpElement = doc.select("img.icon").first();
                String pfpUrl = (pfpElement != null) ? pfpElement.attr("abs:src") : null;
                Image pfpImage = (pfpUrl != null) ? new Image(pfpUrl) : null;

                // 2. Scrape Profile Details (Joined, Bio)
                Element joinedElement = doc.select("dt:contains(Joined:) + dd").first();
                String joinedDate = (joinedElement != null) ? joinedElement.text() : "Unknown";

                Element bioElement = doc.select("div.bio.module blockquote.userstuff").first();
                String bioText = (bioElement != null) ? bioElement.html() : "No bio provided.";

                // Update the UI on the JavaFX thread
                Platform.runLater(() -> {
                    if (pfpImage != null) {
                        pfpImageView.setImage(pfpImage);
                    }

                    // Build the "Profile Details" VBox dynamically
                    profileDetailsBox.getChildren().clear();
                    Label joinedLabel = new Label("Joined: " + joinedDate);
                    joinedLabel.setWrapText(true);

                    Label bioHeader = new Label("Bio:");
                    bioHeader.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

                    Label bioLabel = new Label(bioText.replaceAll("<br>", "\n").replaceAll("<[^>]*>", "")); // Basic HTML to text
                    bioLabel.setWrapText(true);

                    profileDetailsBox.getChildren().addAll(joinedLabel, bioHeader, bioLabel);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> profileDetailsBox.getChildren().add(new Label("Failed to load profile.")));
            }
        }).start();
    }

    /**
     * Scrapes the author's creations (works) page.
     * This is called when the "Creations" tab is clicked.
     */
    private void loadCreations() {
        // Don't re-load if we already have data
        if (!creationsListView.getItems().isEmpty()) return;

        creationsListView.getItems().add("Loading creations...");

        new Thread(() -> {
            try {
                String creationsUrl = authorUrl + "/works";
                Document doc = Jsoup.connect(creationsUrl).get();
                Elements workElements = doc.select("li.work");

                List<String> workTitles = new ArrayList<>();
                for (Element work : workElements) {
                    String title = work.select("h4.heading a").first().text();
                    workTitles.add(title);
                }

                Platform.runLater(() -> {
                    creationsListView.getItems().clear();
                    if (workTitles.isEmpty()) {
                        creationsListView.getItems().add("No creations found.");
                    } else {
                        creationsListView.getItems().addAll(workTitles);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> creationsListView.getItems().setAll("Failed to load creations."));
            }
        }).start();
    }

    /**
     * Scrapes the author's bookmarks page.
     * This is called when the "Bookmarks" tab is clicked.
     */
    private void loadBookmarks() {
        if (!bookmarksListView.getItems().isEmpty()) return;
        bookmarksListView.getItems().add("Loading bookmarks...");

        new Thread(() -> {
            try {
                String bookmarksUrl = authorUrl + "/bookmarks";
                Document doc = Jsoup.connect(bookmarksUrl).get();
                Elements bookmarkElements = doc.select("li.bookmark"); // AO3 uses "li.bookmark"

                List<String> bookmarkTitles = new ArrayList<>();
                for (Element bookmark : bookmarkElements) {
                    String title = bookmark.select("h4.heading a").first().text();
                    bookmarkTitles.add(title);
                }

                Platform.runLater(() -> {
                    bookmarksListView.getItems().clear();
                    if (bookmarkTitles.isEmpty()) {
                        bookmarksListView.getItems().add("No bookmarks found.");
                    } else {
                        bookmarksListView.getItems().addAll(bookmarkTitles);
                    }
                });

            } catch (Exception e) {
                // ✅ FIX: Changed e.FprintStackTrace() to e.printStackTrace()
                e.printStackTrace();
                Platform.runLater(() -> bookmarksListView.getItems().setAll("Failed to load bookmarks."));
            }
        }).start();
    }

    /**
     * Scrapes the author's collections page.
     * This is called when the "Collections" tab is clicked.
     */
    private void loadCollections() {
        if (!collectionsListView.getItems().isEmpty()) return;
        collectionsListView.getItems().add("Loading collections...");

        new Thread(() -> {
            try {
                String collectionsUrl = authorUrl + "/collections";
                Document doc = Jsoup.connect(collectionsUrl).get();
                Elements collectionElements = doc.select("li.collection"); // AO3 uses "li.collection"

                List<String> collectionTitles = new ArrayList<>();
                for (Element collection : collectionElements) {
                    String title = collection.select("h4.heading a").first().text();
                    collectionTitles.add(title);
                }

                Platform.runLater(() -> {
                    collectionsListView.getItems().clear();
                    if (collectionTitles.isEmpty()) {
                        collectionsListView.getItems().add("No collections found.");
                    } else {
                        collectionsListView.getItems().addAll(collectionTitles);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> collectionsListView.getItems().setAll("Failed to load collections."));
            }
        }).start();
    }
}