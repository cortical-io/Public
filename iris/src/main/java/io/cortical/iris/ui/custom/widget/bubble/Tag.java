package io.cortical.iris.ui.custom.widget.bubble;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class Tag extends StackPane {
    private String term;
    
    public Tag(String text) {
        this.term = text;
        
        getStyleClass().setAll("tag");
        
        setPrefHeight(25);
        
        setFocusTraversable(false);
        
        getChildren().add(new Label(this.term));
    }
}
