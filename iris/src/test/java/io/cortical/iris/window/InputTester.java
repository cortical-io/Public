package io.cortical.iris.window;

import java.util.UUID;

import javafx.application.Application;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class InputTester extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        TextField test = new TextField("0");
        test.setPrefSize(275, 25);
        
        InputSelector selector = new InputSelector();
        selector.setPrefSize(25, 24);
        
        Button add = new Button("Add");
        add.setOnAction(e -> selector.getInputWindowList().add(UUID.randomUUID()));
        
        Button rem = new Button("Remove");
        rem.setOnAction(e -> { selector.getInputWindowList().remove(Integer.parseInt(test.getText().trim())); });
        
        BorderPane border = new BorderPane();
        HBox hbox = new HBox();
        hbox.getChildren().addAll(selector, test, add, rem);
        border.setTop(hbox);
        Pane mainPane = new Pane();
        mainPane.setPrefSize(300, 400);
        border.setCenter(mainPane);
        
        Scene scene = new Scene(border);
        scene.setCamera(new PerspectiveCamera());
        scene.getStylesheets().add(getClass().getClassLoader().getResource("iris.css").toExternalForm());
        scene.getStylesheets().add(getClass().getClassLoader().getResource("rich-text.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * For IDEs which aren't JavaFX savvy
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }
}
