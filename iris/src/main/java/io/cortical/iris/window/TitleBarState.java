package io.cortical.iris.window;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;


public class TitleBarState {
    private Point2D startLocation;
    private MouseEvent lastEvent;
    private boolean dragStopped;
    
    /**
     * Creates a new {@code ThumbState}
     * 
     * @param start     the location of the mouse when first pressed
     * @param evt       the current MouseEvent.
     */
    public TitleBarState(Point2D start, MouseEvent evt) {
        this.startLocation = start;
        this.lastEvent = evt;
    }
    
    /**
     * Creates a new {@code ThumbState}
     * 
     * @param start         the location of the mouse when first pressed
     * @param evt           the current MouseEvent.
     * @param dragStopped   the drag stopped indicator flag
     */
    public TitleBarState(Point2D start, MouseEvent evt, boolean dragStopped) {
        this.startLocation = start;
        this.lastEvent = evt;
        this.dragStopped = dragStopped;
    }
    
    /**
     * Returns the location of the mouse when he drag
     * operation started.
     * 
     * @return
     */
    public Point2D getDragStart() {
        return startLocation;
    }
    
    /**
     * 
     * @return
     */
    public MouseEvent getLastEvent() {
        return lastEvent;
    }
    
    /**
     * Returns the flag indicating whether the dragging has stopped or not.
     * @return
     */
    public boolean dragStopped() {
        return dragStopped;
    }
}
