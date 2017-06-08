package io.cortical.iris.ui.util;

/**
 * Describes the operating mode of the DragPad which
 * describes the format of the text which is expected
 * to be on the clip board following a user copy gesture.
 */
public enum DragMode {
    FREEHAND("Freehand"), TERM_OR_JSON("Word or JSON");
    
    private String display;
    
    private DragMode(String s) {
        this.display = s;
    }
    
    /**
     * Returns the description which can be used for
     * UI labeling.
     * @return
     */
    public String toDisplay() {
        return display;
    }
}
