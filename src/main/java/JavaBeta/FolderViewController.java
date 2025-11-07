package JavaBeta;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class FolderViewController {

    @FXML private ImageView folderImageView;
    @FXML private Label folderNameLabel;
    @FXML private Label folderDescriptionLabel;
    @FXML private TableView<Work> ficsTableView;
    @FXML private TableColumn<Work, String> titleColumn;
    @FXML private TableColumn<Work, String> authorColumn;

    // We'll use this to call back to the main controller to open the reader
    private Ao3Controller mainController;

    public void setMainController(Ao3Controller mainController) {
        this.mainController = mainController;
    }

    /**
     * This is the main method to populate the view with data
     */
    public void loadFolder(Ao3Controller.FolderData folderData) {
        // 1. Set Header Info
        folderNameLabel.setText(folderData.name);
        folderDescriptionLabel.setText(folderData.description);

        // 2. Set Folder Image (with auto-crop)
        if (folderData.imagePath != null && !folderData.imagePath.isBlank()) {
            try {
                Image image = new Image(folderData.imagePath);
                folderImageView.setImage(image);
                applyViewport(image);
            } catch (Exception e) {
                folderImageView.setImage(null); // Or set placeholder
            }
        } else {
            // Optional: set a placeholder icon
            folderImageView.setImage(null);
        }

        // 3. Populate Fic List
        ObservableList<Work> fics = FXCollections.observableArrayList();
        for (String pathString : folderData.ficPaths) {
            // We need to parse the file path back into a Work object
            Work fic = parseWorkFromPath(pathString);
            if (fic != null) {
                fics.add(fic);
            }
        }

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        ficsTableView.setItems(fics);

        // 4. Add double-click to open fic
        ficsTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && mainController != null) {
                Work selectedFic = ficsTableView.getSelectionModel().getSelectedItem();
                if (selectedFic != null) {
                    // Call the main controller's method to open the reader
                    mainController.loadStoryFromLibrary(selectedFic);
                }
            }
        });
    }

    /**
     * Re-creates a Work object by parsing its file path.
     * (This is the same logic from your populateLibraryViews)
     */
    private Work parseWorkFromPath(String pathString) {
        try {
            Path path = Paths.get(pathString);
            String filename = path.getFileName().toString().replaceFirst("\\.(txt|html)$", "");
            String title = "Unknown Title";
            String author = "Unknown Author";

            int separatorIndex = filename.lastIndexOf(" - ");
            if (separatorIndex != -1) {
                title = filename.substring(0, separatorIndex).trim();
                author = filename.substring(separatorIndex + 3).trim();
            } else {
                title = filename;
            }
            // Create a work object (url is the path)
            return new Work(title, author, pathString, "N/A", "N/A");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Applies the center-square crop to the folder image
     */
    private void applyViewport(Image image) {
        double width = image.getWidth();
        double height = image.getHeight();
        if (width != height) {
            double size = Math.min(width, height);
            double x = (width - size) / 2;
            double y = (height - size) / 2;
            folderImageView.setViewport(new Rectangle2D(x, y, size, size));
        }
    }
}