package io.cortical.iris.ui.custom.widget.bubble;

import java.util.ArrayList;
import java.util.List;

import io.cortical.iris.view.input.expression.ExpressionField;
import io.cortical.iris.view.input.expression.FingerprintBubble;
import io.cortical.iris.view.input.expression.OperatorBubble;
import io.cortical.iris.view.input.expression.ParenthesisBubble;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * Pairs the {@link Bubble} implementation with the selection indicator
 * line into one discrete display unit (arranged within a {@link VBox}).
 * 
 * @author cogmission
 *
 * @param <T>   the {@link Bubble} type.
 */
public class Entry<T extends Bubble> extends VBox {
    Line selectionIndicator;
    Line cursor;
    
    Color selectedColor = Color.rgb(92,183,186);//Color.rgb(235,107,38);
    Color deselectedColor = Color.TRANSPARENT;
    
    List<Color> userDefinedColors = new ArrayList<>();
    
    T contents;
    
    BooleanProperty selectedProperty = new SimpleBooleanProperty();
    BooleanProperty cursorVisibleProperty = new SimpleBooleanProperty(false);
    
    private Entry(T b) {
        this.contents = b;
        
        setPrefHeight(45);
        
        setFocusTraversable(false);
        
        selectionIndicator = new Line();
        selectionIndicator.setManaged(false);
        selectionIndicator.setStrokeWidth(2.0);
        selectionIndicator.setStartX(5);
        selectionIndicator.setStroke(Color.TRANSPARENT);
        selectedProperty.addListener((v, o, n) -> {
            selectionIndicator.strokeProperty().set(n ? selectedColor : deselectedColor);
        });
        
        selectionIndicator.setStartY(44.5);
        selectionIndicator.setEndY(44.5);
        
        selectedProperty.set(b instanceof ExpressionField);
        
        setSpacing(3);
        
        getChildren().addAll((Node)b, selectionIndicator);
    }
    
    @SuppressWarnings("unchecked")
    public Entry(WordBubble bubble) {
        this((T)bubble);
        bubble.setAlignment(Pos.CENTER);
        selectionIndicator.endXProperty().bind(bubble.widthProperty().subtract(5));
    }
    
    @SuppressWarnings("unchecked")
    public Entry(OperatorBubble bubble) {
        this((T)bubble);
        bubble.setAlignment(Pos.CENTER);
        selectionIndicator.endXProperty().bind(bubble.widthProperty().subtract(5));
    }
    
    @SuppressWarnings("unchecked")
    public Entry(ParenthesisBubble bubble) {
        this((T)bubble);
        bubble.setAlignment(Pos.CENTER);
        selectionIndicator.endXProperty().bind(bubble.widthProperty().subtract(5));
    }
    
    @SuppressWarnings("unchecked")
    public Entry(FingerprintBubble bubble) {
        this((T)bubble);
        bubble.setAlignment(Pos.CENTER);
        selectionIndicator.endXProperty().bind(bubble.widthProperty().subtract(5));
    }
    
    @SuppressWarnings("unchecked")
    public Entry(ExpressionField field) {
        this((T)field);
        field.setAlignment(Pos.BOTTOM_CENTER);
        setPadding(new Insets(15, 5, 0, 5));
        field.widthProperty().addListener((v, o, n) -> {
            selectionIndicator.setEndX(Math.max(5, n.doubleValue() + 5));
        });
        
        focusedProperty().addListener((v,o,n) -> {
            if(n) field.requestFocus();
        });
        
        cursor = new Line();
        cursor.setManaged(false);
        cursor.setStrokeWidth(2.5);
        cursor.setStartX(0);
        cursor.setEndX(0);
        cursor.setStartY(10);
        cursor.setEndY(40);
        cursor.setStroke(Color.rgb(237, 93, 37));
        cursor.setVisible(false);
        
        cursor.visibleProperty().bind(cursorVisibleProperty);
        
        getChildren().add(cursor);
    }
    
    /**
     * Returns the property controlling cursor visibility.
     * @return
     */
    public BooleanProperty cursorVisibleProperty() {
        return cursorVisibleProperty;
    }
    
    public Line getSelectionIndicator() {
        return selectionIndicator;
    }
    
    /**
     * Returns the contents of this Entry
     * @return  the contents of this Entry
     */
    public T getBubble() {
        return contents;
    }
    
    /**
     * Sets the flag indicating whether this entry is in the
     * selected state or not.
     * @param b the flag indicating whether this entry is currently
     *          selected
     */
    public void setSelected(boolean b) {
        selectedProperty.set(b);
    }
    
    /**
     * Returns a flag indicating whether this entry 
     * is currently selected.
     * @return  a flag indicating whether this entry
     * is currently selected
     */
    public boolean isSelected() {
        return selectedProperty.get();
    }
    
    /**
     * Returns the property containing the boolean
     * selected state.
     * @return  the property containing the boolean
     * selected state.
     */
    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }
    
    /**
     * Sets a Color applied by the application on the 
     * user's behalf during a color selection or otherwise.
     * @param c the color to add
     */
    public void addUserDefinedColor(Color c) {
        this.userDefinedColors.add(c);
    }
    
    /**
     * Returns the last color added to this entry, removing
     * it from the store of ordered colors
     * @return  the last color added
     */ 
    public Color popLastUserDefinedColor() {
        Color retVal = null;
        if(userDefinedColors.size() > 0) {
            retVal = userDefinedColors.remove(userDefinedColors.size() - 1);
        }
        
        return retVal;
    }
}
