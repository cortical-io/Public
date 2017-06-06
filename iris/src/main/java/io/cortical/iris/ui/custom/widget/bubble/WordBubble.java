package io.cortical.iris.ui.custom.widget.bubble;

import io.cortical.iris.ui.util.DragAssistant;
import javafx.scene.control.ToggleButton;


public class WordBubble extends ToggleButton implements Bubble {
    private String term;
    
    public WordBubble(String text) {
        super(text);
        
        this.term = text;
        
        getStyleClass().setAll("bubble");
        
        setPrefHeight(30);
        
        setFocusTraversable(false);
        
        DragAssistant.configureDragHandler(this);
        
    }
    
    public Type getType() {
        return Bubble.Type.WORD;
    }
    
    public String getTerm() {
        return term;
    }
}
