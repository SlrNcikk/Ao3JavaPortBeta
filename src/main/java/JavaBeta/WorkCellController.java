package JavaBeta;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is both the Controller for WorkCell.fxml AND
 * the ListCell that will be displayed in the ListView.
 */
public class WorkCellController extends ListCell<Work> {

    // --- FXML UI Elements ---
    @FXML private VBox root; // The main VBox from the FXML
    @FXML private GridPane iconsGrid;
    @FXML private ImageView ratingIcon;
    @FXML private ImageView categoryIcon;
    @FXML private ImageView warningIcon;
    @FXML private ImageView completionIcon;
    @FXML private Label titleLabel;
    @FXML private Label authorLabel;
    @FXML private Label fandomLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private TextFlow tagsTextFlow;

    // --- Image Cache ---
    private static final Map<String, Image> iconCache = new HashMap<>();

    private FXMLLoader fxmlLoader;
    private Ao3Controller mainController;

    public WorkCellController(Ao3Controller mainController) {
        this.mainController = mainController;
    }

    @Override
    protected void updateItem(Work work, boolean empty) {
        super.updateItem(work, empty);

        if (empty || work == null) {
            setText(null);
            setGraphic(null);
        } else {
            // --- 1. Load the FXML if we haven't already ---
            if (fxmlLoader == null) {
                fxmlLoader = new FXMLLoader(getClass().getResource("/JavaBeta/WorkCell.fxml"));
                fxmlLoader.setController(this); // Tell the FXML this class is its controller
                try {
                    fxmlLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // --- 2. Populate the FXML with data from the Work object ---
            titleLabel.setText(work.getTitle());
            authorLabel.setText(work.getAuthor());
            lastUpdatedLabel.setText(work.getLastUpdated());

            // ✅ --- ADDED FANDOM ---
            fandomLabel.setText(work.getFandom());

            // --- 3. Set Icons ---
            // ✅ --- UPDATED TO SET ALL ICONS ---
            ratingIcon.setImage(getIconForRating(work.getRating()));
            categoryIcon.setImage(getIconForCategory(work.getCategory()));
            warningIcon.setImage(getIconForWarning(work.getWarnings()));
            completionIcon.setImage(getIconForCompletion(work.getCompletionStatus()));

            // --- 4. Build the rich text for the tags ---
            buildTagsTextFlow(work);

            // --- 5. Add click handler for the author ---
            authorLabel.setOnMouseClicked(event -> {
                if (mainController != null) {
                    mainController.openAuthorProfile(work);
                }
            });

            // --- 6. Finally, display the cell ---
            setText(null);
            setGraphic(root);
        }
    }

    /**
     * Creates the styled text for the tags
     */
    private void buildTagsTextFlow(Work work) {
        tagsTextFlow.getChildren().clear(); // Clear old tags

        // ✅ --- UPDATED TO ADD ALL TAGS ---
        addTagToFlow("Warnings:", work.getWarnings(), false);
        addTagToFlow("Relationships:", work.getRelationships(), true);
        addTagToFlow("Characters:", work.getCharacters(), true);
        addTagToFlow("Freeforms:", work.getTags(), false);
    }

    /**
     * Helper to add a styled label and value to the TextFlow
     */
    private void addTagToFlow(String labelText, String valueText, boolean isLinkStyle) {
        // Don't add a tag if the value is empty or N/A
        if (valueText == null || valueText.isBlank() || valueText.equals("N/A")) {
            return;
        }

        Text label = new Text(labelText + " ");
        label.setStyle("-fx-font-weight: bold;");

        Text value = new Text(valueText + "  "); // Add extra space for separation
        if (isLinkStyle) {
            value.setStyle("-fx-fill: #0066cc;"); // Make it look like a link
        }

        tagsTextFlow.getChildren().addAll(label, value);
    }

    /**
     * Gets the correct icon image based on the rating string.
     */
    private Image getIconForRating(String rating) {
        String filename = switch (rating) {
            case "Teen And Up Audiences" -> "teens.PNG";
            case "General Audiences" -> "general.png";
            case "Explicit" -> "explicit.png";
            case "Mature" -> "teens.PNG"; // Fallback, you don't have a mature icon
            default -> "general.png"; // Fallback for "Not Rated"
        };
        return getIconFromCache("/JavaBeta/icons/" + filename);
    }

    // ✅ --- ADDED THIS MISSING METHOD ---
    private Image getIconForCategory(String category) {
        String filename = switch (category) {
            case "M/M" -> "mmalt.png";
            case "F/M" -> "het.png";
            case "F/F" -> "ff alt.png";
            case "Multi" -> "multi.png";
            case "Other" -> "other.png";
            case "Gen" -> "cat_gen.png";
            default -> "other.png"; // Fallback for "No Category"
        };
        return getIconFromCache("/JavaBeta/icons/" + filename);
    }

    private Image getIconForWarning(String warnings) {
        String filename = switch (warnings) {
            case "Creator Chose Not To Use Archive Warnings" -> "exclaimquestion.png";
            case "Graphic Depictions Of Violence" -> "exlamationpoint.png";
            case "Rape/Non-Con" -> "exlamationpoint.png"; // Fallback
            case "Major Character Death" -> "exlamationpoint.png"; // Fallback
            case "Underage" -> "exlamationpoint.png"; // Fallback
            default -> "exclaimquestion.png"; // Default for "No Warnings"
        };
        return getIconFromCache("/JavaBeta/icons/" + filename);
    }

    private Image getIconForCompletion(String completionStatus) {
        String filename = switch (completionStatus) {
            case "Complete Work" -> "check.png";
            default -> "not_finished.png"; // "In Progress"
        };
        return getIconFromCache("/JavaBeta/icons/" + filename);
    }

    /**
     * This is the central method to load and cache all icons.
     */
    private Image getIconFromCache(String path) {
        // Check cache first
        if (!iconCache.containsKey(path)) {
            try {
                Image img = new Image(getClass().getResourceAsStream(path));
                iconCache.put(path, img); // Add to cache
            } catch (Exception e) {
                System.err.println("Could not load icon: " + path);
                iconCache.put(path, null); // Cache the failure
            }
        }
        return iconCache.get(path);
    }

    // ✅ --- THE STRAY CODE BLOCK AT THE END HAS BEEN DELETED ---
}