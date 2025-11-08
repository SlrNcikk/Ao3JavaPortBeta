package JavaBeta;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.ListView; // We only need ListView
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.List; // Import List

public class FolderViewController {

    @FXML private ImageView folderImageView;
    @FXML private Label folderNameLabel;
    @FXML private Label folderDescriptionLabel;
    @FXML private ListView<Work> worksListView; // This matches your FXML

    private Ao3Controller mainController;

    public void setMainController(Ao3Controller mainController) {
        this.mainController = mainController;
    }

    /**
     * This is the main method to populate the view with data.
     * It now accepts the pre-loaded list of fics from the main controller.
     */
    public void loadFolder(Ao3Controller.FolderData folderData, List<Work> fics) {        folderNameLabel.setText(folderData.name);
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
            folderImageView.setImage(null);
        }

        // 3. Populate Fic List (Now it's simple!)
        if (worksListView != null) {
            // Set the cell factory to use your custom WorkCell
            worksListView.setCellFactory(listView -> new WorkCellController(mainController));

            // Set the items from the list we were given
            worksListView.setItems(FXCollections.observableArrayList(fics));

            // 4. Add double-click to open fic
            worksListView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && mainController != null) {
                    Work selectedFic = worksListView.getSelectionModel().getSelectedItem();
                    if (selectedFic != null) {
                        // Call the main controller's method to open the reader
                        mainController.loadStoryFromLibrary(selectedFic);
                    }
                }
            });
        }
    }

    // We no longer need the parseWorkFromPath() method, as the main controller
    // is handling all the data logic.

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