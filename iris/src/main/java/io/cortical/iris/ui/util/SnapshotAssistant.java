package io.cortical.iris.ui.util;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class SnapshotAssistant {
    
    public static Image snapshot(Node node, Color background) {

        WritableImage wi;

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(background);

        int imageWidth = (int) node.getBoundsInLocal().getWidth();
        int imageHeight = (int) node.getBoundsInLocal().getHeight();

        wi = new WritableImage(imageWidth, imageHeight);
        node.snapshot(parameters, wi);

        return wi;

    }
    
    public static Image snapshot(Node node, Color background, double width, double height, boolean preserveRatio) {
        Image source = snapshot(node, background);
        
        ImageView imageView = new ImageView(source);
        imageView.setPreserveRatio(preserveRatio);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setStyle("-fx-background-radius: 7; -fx-border-radius: 7; -fx-background-color: transparent;");
        
        return imageView.snapshot(null, null);
    }
}
