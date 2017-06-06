package io.cortical.iris.view.input.expression;

import java.util.ArrayList;

import io.cortical.iris.ui.custom.widget.bubble.Bubble;

/**
 * Guarantees the contents of this list DO NOT include the
 * type {@link Bubble.Type#FIELD}.
 */
public class FilteredBubbleList extends ArrayList<Bubble> {
    
    private static final FilteredBubbleList EMPTY = new FilteredBubbleList();

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Returns the singleton empty list.
     * @return
     */
    public static FilteredBubbleList emptyList() {
        return EMPTY;
    }
    
    public static class Builder {
        private FilteredBubbleList retVal = new FilteredBubbleList();
        
        public void add(Bubble b) {
            if(b.getType() == Bubble.Type.FIELD) {
                throw new IllegalArgumentException("Type \"FIELD\" not allowed in contents.");
            }
            
            retVal.add(b);
        }
        
        public void addAll(FilteredBubbleList l) {
            retVal.addAll(l);
        }
        
        public FilteredBubbleList build() {
            return retVal;
        }
    }
}
