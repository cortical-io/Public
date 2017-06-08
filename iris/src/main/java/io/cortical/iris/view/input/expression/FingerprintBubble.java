package io.cortical.iris.view.input.expression;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.iris.ui.util.DragAssistant;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;


/**
 * Version of {@link Bubble} used for drag-n-drop operations.
 */
public class FingerprintBubble extends ToggleButton implements Bubble {
    private int[] positions;
    private String positionString;
    
    /**
     * Constructs a new {@code FingerprintBubble}
     * @param pos
     */
    public FingerprintBubble(int[] pos) {
        this.positions = pos;
        
        this.positionString = Arrays.stream(pos).mapToObj(i -> String.valueOf(i)).collect(Collectors.joining(","));
        
        getStyleClass().setAll("fingerprint-bubble");
        
        setPrefWidth(50);
        setPrefHeight(30);
        
        setGraphic(new ImageView("fingerprint_exp.png"));
        
        setFocusTraversable(false);
        
        DragAssistant.configureDragHandler(this);
    }
    
    /**
     * Returns a comma separated string of integers
     * @return
     */
    public String getPositionsString() {
        return positionString;
    }
    
    /**
     * Returns the fingerprint's positions.
     * @return
     */
    public int[] getPositions() {
        return positions;
    }

    @Override
    public Type getType() {
        return Bubble.Type.FINGERPRINT;
    }
}
