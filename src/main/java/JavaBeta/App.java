package JavaBeta;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color; // <-- CHANGE HERE: Added import
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // --- 1. LOAD YOUR MAIN APP (but don't show it) ---
        URL fxmlLocation = getClass().getResource("/JavaBeta/main-view.fxml");
        if (fxmlLocation == null) {
            System.err.println("CRITICAL ERROR: Cannot find FXML file. Path: /JavaBeta/main-view.fxml");
            return;
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent mainRoot = loader.load();
        Scene mainScene = new Scene(mainRoot, 1000, 600);

        // --- 2. APPLY STYLESHEET (to the main scene) ---
        URL stylesheetUrl = getClass().getResource("/JavaBeta/styles.css");
        if (stylesheetUrl != null) {
            mainScene.getStylesheets().add(stylesheetUrl.toExternalForm());
            System.out.println("DEBUG: Dark theme applied.");
        } else {
            System.err.println("Warning: Could not find the stylesheet.");
        }

        // --- 3. PREPARE THE MAIN STAGE (but don't show it) ---
        stage.setTitle("Ao3JavaFXPortBeta");
        stage.setScene(mainScene);

        // --- 4. LOAD AND SHOW THE SPLASH SCREEN ---
        URL splashFxmlLocation = getClass().getResource("/JavaBeta/splash.fxml");
        if (splashFxmlLocation == null) {
            System.err.println("CRITICAL ERROR: Cannot find FXML file. Path: /JavaBeta/splash.fxml");
            stage.show();
            return;
        }

        FXMLLoader splashLoader = new FXMLLoader(splashFxmlLocation);
        Parent splashRoot = splashLoader.load();
        // Make the scene background transparent
        Scene splashScene = new Scene(splashRoot, Color.TRANSPARENT);

        Stage splashStage = new Stage();
        // Make the whole window transparent (removes title bar, etc.)
        splashStage.initStyle(StageStyle.TRANSPARENT);
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();

        // --- 5. CREATE THE "PHASE OUT" ANIMATION ---

        // Pause for 2 seconds
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(event -> {

            // Start a 1-second fade-out
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(fadeEvent -> {
                // When fade is finished:
                splashStage.hide(); // Hide the splash screen
                stage.show();       // Show your main app
            });
            fadeOut.play();
        });
        delay.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}