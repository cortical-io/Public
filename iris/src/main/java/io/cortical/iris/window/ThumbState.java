package io.cortical.iris.window;

import io.cortical.iris.Iris;
import javafx.geometry.Point2D;

/**
 * Class which encapsulates the state of {@link Iris} mouse drag
 * info.
 * 
 * @author cogmission
 *
 */
public class ThumbState {
    private Point2D startLocation;
    private double lastX;
    private double lastY;
    
    /**
     * Creates a new {@code ThumbState}
     * 
     * @param start     the location of the mouse when first pressed
     * @param lastX     the current x location
     * @param lastY     the current y location
     */
    public ThumbState(Point2D start, double lastX, double lastY) {
        this.startLocation = start;
        this.lastX = lastX;
        this.lastY = lastY;
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
     * Returns the last x coordinate
     * @return
     */
    public double lastX() {
        return lastX;
    }
    
    /**
     * Returns the last y coordinate
     * @return
     */
    public double lastY() {
        return lastY;
    }
    
}
