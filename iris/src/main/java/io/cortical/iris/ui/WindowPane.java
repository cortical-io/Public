package io.cortical.iris.ui;

import javafx.geometry.Bounds;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

public class WindowPane extends StackPane {
    private FlowPane iconArea;
    
    private double iconAreaHeight = 35;
    
    public WindowPane() {
        iconArea = new FlowPane(5, 5);
        iconArea.setManaged(false);
        iconArea.setFocusTraversable(false);
        iconArea.setPrefHeight(iconAreaHeight);
        iconArea.setVisible(true);
        iconArea.getStyleClass().add("icon-area");
        layoutBoundsProperty().addListener((v,o,n) -> {
            resizeIconArea(n);
        });
                
        setPrefSize(800, 600);
        setFocusTraversable(false);
        getStyleClass().add("window-pane");
        
        getChildren().addAll(iconArea);
    }
    
    public void resizeIconArea(Bounds b) {
        iconArea.resizeRelocate(0, b.getHeight() - iconAreaHeight, b.getWidth(), iconAreaHeight);
    }
    
    public FlowPane getIconArea() {
        return iconArea;
    }
}
