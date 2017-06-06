package io.cortical.iris;

import io.cortical.iris.ui.ContentPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;


public class Iris extends Application {
    private static final Bounds DEFAULT_BOUNDS = new BoundingBox(0, 13, 1000, 900);
    
    private static final WindowService WINDOW_SERVICE = WindowService.getInstance();
    private static final ApplicationService APP_SERVICE = ApplicationService.getInstance();
    @SuppressWarnings("unused")
    private static final ServerMessageService SERVER_SERVICE = ServerMessageService.getInstance();
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        Font.loadFont(Iris.class.getClassLoader().getResource("fonts/Questrial-Regular.otf").toExternalForm(), 12);
        
        APP_SERVICE.setHostServices(getHostServices());
        APP_SERVICE.setPrimaryStage(primaryStage);
        APP_SERVICE.restorePrimaryStageState(primaryStage, DEFAULT_BOUNDS);
        
        WINDOW_SERVICE.setStage(primaryStage);
        WINDOW_SERVICE.setContentPane(new ContentPane());
                
        Scene scene = new Scene(WINDOW_SERVICE.getContentPane());
        scene.setCamera(new PerspectiveCamera());
        scene.getStylesheets().add(getClass().getClassLoader().getResource("iris.css").toExternalForm());
        scene.getStylesheets().add(getClass().getClassLoader().getResource("rich-text.css").toExternalForm());
        primaryStage.setScene(scene); 
        primaryStage.show();
        
        Platform.runLater(() -> {
            WINDOW_SERVICE.getContentPane().getLogoBackground().poke();
            WINDOW_SERVICE.getContentPane().requestFocus();
        });
    }
    
    /**
     * For IDEs which aren't JavaFX savvy
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }
}
