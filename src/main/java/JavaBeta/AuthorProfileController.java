package JavaBeta;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuthorProfileController {

    private static class Ao3Item {
        private final String title;
        private final String url;

        public Ao3Item(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getUrl() { return url; }

        @Override
        public String toString() { return title; }
    }

    @FXML private ImageView pfpImageView;
    @FXML private VBox profileDetailsBox;
    @FXML private TextArea personalNoteArea;
    @FXML private Label authorNameLabel;
    @FXML private Tab creationsTab;
    @FXML private Tab bookmarksTab;
    @FXML private Tab collectionsTab;
    @FXML private ListView<Ao3Item> creationsListView;
    @FXML private ListView<Ao3Item> bookmarksListView;
    @FXML private ListView<Ao3Item> collectionsListView;
    @FXML private Button refreshCreationsButton;
    @FXML private Button refreshBookmarksButton;
    @FXML private Button refreshCollectionsButton;

    private String authorBaseUrl;
    private String authorPseudUrl;

    private Ao3Controller mainController;

    public void setMainController(Ao3Controller mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        creationsTab.setOnSelectionChanged(event -> {
            if (creationsTab.isSelected()) {
                loadTabContent("/works", "li.work", creationsListView, "No creations found.");
            }
        });
        bookmarksTab.setOnSelectionChanged(event -> {
            if (bookmarksTab.isSelected()) {
                loadTabContent("/bookmarks", "li.bookmark", bookmarksListView, "No bookmarks found.");
            }
        });
        collectionsTab.setOnSelectionChanged(event -> {
            if (collectionsTab.isSelected()) {
                loadTabContent("/collections", "li.collection", collectionsListView, "No collections found.");
            }
        });

        refreshCreationsButton.setOnAction(event -> {
            creationsListView.getItems().clear();
            loadTabContent("/works", "li.work", creationsListView, "No creations found.");
        });
        refreshBookmarksButton.setOnAction(event -> {
            bookmarksListView.getItems().clear();
            loadTabContent("/bookmarks", "li.bookmark", bookmarksListView, "No bookmarks found.");
        });
        refreshCollectionsButton.setOnAction(event -> {
            collectionsListView.getItems().clear();
            loadTabContent("/collections", "li.collection", collectionsListView, "No collections found.");
        });

        creationsListView.setOnMouseClicked(this::handleItemClick);
        bookmarksListView.setOnMouseClicked(this::handleItemClick);
        collectionsListView.setOnMouseClicked(this::handleItemClick);

        personalNoteArea.setEditable(false);
        personalNoteArea.setWrapText(true);
    }

    public void loadAuthorData(String initialAuthorName, String initialAuthorUrl) {
        this.authorPseudUrl = initialAuthorUrl;
        this.authorBaseUrl = initialAuthorUrl.replaceAll("/pseuds/.*", "");

        authorNameLabel.setText("Loading profile for " + initialAuthorName + "...");

        new Thread(() -> {
            try {
                if (mainController == null) {
                    throw new IOException("MainController was not provided to AuthorProfileController.");
                }
                Document doc = mainController.getDocument(authorPseudUrl);

                System.out.println("DEBUG: Scraping for PFP...");
                Element pfpElement = doc.select("div.profile.module p.icon img").first();
                String pfpUrl = null;

                if (pfpElement != null) {
                    System.out.println("DEBUG: Found pfpElement with selector 'div.profile.module p.icon img'.");
                    pfpUrl = pfpElement.attr("abs:src");
                } else {
                    System.out.println("DEBUG: Selector 'div.profile.module p.icon img' FAILED.");
                    System.out.println("DEBUG: Trying fallback selector 'div.profile img'...");
                    pfpElement = doc.select("div.profile img").first();

                    if (pfpElement != null) {
                        System.out.println("DEBUG: Fallback selector 'div.profile img' SUCCESS.");
                        pfpUrl = pfpElement.attr("abs:src");
                    } else {
                        System.out.println("DEBUG: Fallback selector 'div.profile img' FAILED.");
                        System.out.println("DEBUG: Trying final fallback 'img.icon'...");
                        pfpElement = doc.select("img.icon").first();
                        if(pfpElement != null) {
                            System.out.println("DEBUG: Final fallback 'img.icon' SUCCESS.");
                            pfpUrl = pfpElement.attr("abs:src");
                        } else {
                            System.out.println("DEBUG: All PFP selectors FAILED.");
                        }
                    }
                }

                System.out.println("DEBUG: Final pfpUrl is: " + pfpUrl);

                Image pfpImage = (pfpUrl != null && !pfpUrl.isEmpty()) ? new Image(pfpUrl, true) : null;

                Element nameElement = doc.select("h2.user.heading").first();
                String officialAuthorName = (nameElement != null) ? nameElement.text().trim() : initialAuthorName;

                Element joinedElement = doc.select("div.profile.module dt:contains(Joined:) + dd").first();
                String joinedDate = (joinedElement != null) ? joinedElement.text().trim() : "Unknown";

                Element bioElement = doc.select("div.bio.module blockquote.userstuff").first();
                String bioText = (bioElement != null) ? bioElement.html().trim() : "No bio provided.";

                Element noteElement = doc.select("div.note.module blockquote.userstuff").first();
                String noteText = (noteElement != null) ? noteElement.html().trim() : "No personal note.";

                final Image finalPfpImage = pfpImage;
                final String finalOfficialAuthorName = officialAuthorName;
                final String finalJoinedDate = joinedDate;
                final String finalBioText = bioText;
                final String finalNoteText = noteText;

                Platform.runLater(() -> {
                    authorNameLabel.setText(finalOfficialAuthorName);

                    if (finalPfpImage != null) {
                        pfpImageView.setImage(finalPfpImage);

                        finalPfpImage.errorProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal) {
                                System.err.println("JavaFX Error: Failed to load image from URL: " + finalPfpImage.getUrl());
                                finalPfpImage.getException().printStackTrace();
                            }
                        });

                    } else {
                        System.out.println("DEBUG: pfpImage object is null. Setting ImageView to null.");
                        pfpImageView.setImage(null);
                    }

                    profileDetailsBox.getChildren().clear();
                    Label joinedLabel = new Label("Joined: " + finalJoinedDate);
                    joinedLabel.setWrapText(true);

                    Label bioHeader = new Label("Bio:");
                    bioHeader.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

                    Label bioLabel = new Label(cleanHtml(finalBioText));
                    bioLabel.setWrapText(true);

                    profileDetailsBox.getChildren().addAll(joinedLabel, bioHeader, bioLabel);

                    personalNoteArea.setText(cleanHtml(finalNoteText));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    authorNameLabel.setText("Error loading profile.");
                    profileDetailsBox.getChildren().add(new Label("Failed to load profile: " + e.getMessage()));
                    personalNoteArea.setText("Error loading personal note.");
                });
            }
        }).start();
    }

    private void loadTabContent(String pagePath, String itemSelector, ListView<Ao3Item> listView, String emptyMessage) {
        Platform.runLater(() -> {
            listView.getItems().clear();
            listView.getItems().add(new Ao3Item("Loading " + pagePath.substring(1) + "...", ""));
        });

        new Thread(() -> {
            try {
                String url = authorBaseUrl + pagePath;
                if (mainController == null) {
                    throw new IOException("MainController was not provided to AuthorProfileController.");
                }

                Document doc = mainController.getDocument(url);

                Elements itemElements = doc.select(itemSelector);

                List<Ao3Item> items = new ArrayList<>();
                for (Element item : itemElements) {
                    Element linkElement = item.select("h4.heading a").first();
                    if (linkElement != null) {
                        String title = linkElement.text();
                        String itemUrl = linkElement.attr("abs:href");
                        items.add(new Ao3Item(title, itemUrl));
                    }
                }

                Platform.runLater(() -> {
                    listView.getItems().clear();
                    if (items.isEmpty()) {
                        listView.getItems().add(new Ao3Item(emptyMessage, ""));
                    } else {
                        listView.getItems().addAll(items);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> listView.getItems().setAll(new Ao3Item("Failed to load " + pagePath.substring(1) + ".", "")));
            }
        }).start();
    }

    private void handleItemClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            @SuppressWarnings("unchecked")
            ListView<Ao3Item> clickedList = (ListView<Ao3Item>) event.getSource();
            Ao3Item selectedItem = clickedList.getSelectionModel().getSelectedItem();

            if (selectedItem != null && !selectedItem.getUrl().isEmpty()) {
                if (mainController != null) {
                    mainController.openWork(selectedItem.getUrl());
                } else {
                    System.err.println("Error: MainController is null. Cannot open work.");
                }
            }
        }
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n\n")
                .replaceAll("<[^>]*>", "")
                .trim();
    }
}