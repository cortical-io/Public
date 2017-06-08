package io.cortical.iris.view.input.expression;

import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;


public class OperatorBubble extends Button implements Bubble {
    @SuppressWarnings("unused")
    private String term;
    private Operator opType;
    
    private BooleanProperty selectedProperty = new SimpleBooleanProperty();
    
    public OperatorBubble(Operator op) {
        super(op.toString());
        
        getStyleClass().setAll("operator");
        
        this.opType = op;
        this.term = op.toString();
        
        setPrefHeight(10);
        setMaxHeight(10);
        
        setFocusTraversable(false);
        
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> setSelected(!isSelected()));
    }
    
    public Type getType() {
        return Bubble.Type.OPERATOR;
    }
    
    public Operator getOperator() {
        return opType;
    }
    
    public void setOperator(Operator op) {
        this.opType = op;
    }
    
    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }
    
    public boolean isSelected() {
        return selectedProperty.get();
    }
    
    public void setSelected(boolean b) {
        System.out.println("setSelected to " + b);
        this.selectedProperty.set(b);
    }
}
