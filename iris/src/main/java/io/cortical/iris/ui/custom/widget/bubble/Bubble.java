package io.cortical.iris.ui.custom.widget.bubble;

import io.cortical.iris.view.input.expression.Operator;


public interface Bubble {
    public enum Type { WORD, OPERATOR, FIELD, LPAREN, RPAREN, FINGERPRINT };
    
    /**
     * Returns this {@link Type}
     * @return
     */
    public Type getType();
    /**
     * Returns the text string associated with this bubble.
     * @return
     */
    public String getText();
    
    /**
     * Default for subclasses which have no selection state
     * @return
     */
    public default boolean isSelected() {
        return false;
    }
    
    /**
     * Returns the {@link Operator}
     * @return
     */
    public default Operator getOperator() {
        return null;
    }
}
