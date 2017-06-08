package io.cortical.iris.window;

import io.cortical.iris.Iris;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;


/**
 * Triangular node situated at the lower right corner of the
 * {@link Iris} which is used as a drag point to 
 * resize the {@code Iris}.
 * 
 * @author cogmission
 */
public class Thumb extends Pane {
    private Point2D start;
    private SimpleObjectProperty<ThumbState> thumbStateProperty;
    private Shape clip;
    
    public Thumb() {
        Polygon p = new Polygon();
        p.getPoints().addAll(new Double[]{
            26.0, 20.0,
            0.0, 20.0,
            26.0, 0.0 });
        getStyleClass().add("thumb");
        
        Line l = new Line(2, 19, 25, 2);
        l.setStroke(Color.WHITE);
        Line lb = new Line(3, 19, 25, 3);
        lb.setStroke(Color.WHITE.darker());
        
        Line l1 = new Line(7, 19, 25, 6);
        l1.setStroke(Color.WHITE);
        Line l1b = new Line(8, 19, 25, 7);
        l1b.setStroke(Color.WHITE.darker());
        
        Line l2 = new Line(12, 19, 25, 10);
        l2.setStroke(Color.WHITE);
        Line l2b = new Line(13, 19, 25, 11);
        l2b.setStroke(Color.WHITE.darker());
        
        Line l3 = new Line(17, 19, 25, 13.5);
        l3.setStroke(Color.WHITE);
        Line l3b = new Line(18, 19, 25, 14.5);
        l3b.setStroke(Color.WHITE.darker());
        
        getChildren().addAll(l, lb, l1, l1b, l2, l2b, l3, l3b);
                
        setClip(clip = p);
        
        addEventHandler(MouseEvent.ANY, (MouseEvent e) -> {
            if(e.getEventType().equals(MouseEvent.MOUSE_ENTERED)) {
                getScene().setCursor(Cursor.OPEN_HAND);
            }else if(e.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
                this.start = new Point2D(e.getX(), e.getY());
                getScene().setCursor(Cursor.CLOSED_HAND);
            }else if(e.getEventType().equals(MouseEvent.MOUSE_EXITED)) {
                getScene().setCursor(Cursor.DEFAULT);
            }else if(e.getEventType().equals(MouseEvent.MOUSE_DRAGGED)) {
                dragThumb(start, e.getX(), e.getY());
            }else if(e.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
                getScene().setCursor(Cursor.DEFAULT);
            }
        });
       
        thumbStateProperty = new SimpleObjectProperty<>();
    }
    
    /**
     * Returns the property which publishes the user drag gesture.
     * @return
     */
    public SimpleObjectProperty<ThumbState> thumbStateProperty() {
        return thumbStateProperty;
    }
    
    /**
     * Shape describing the lower triangle half of the rectangle 
     * @return
     */
    public Shape getCornerClip() {
        return clip;
    }
    
    /**
     * Publishes the change in the drag state through the 
     * {@link #thumbStateProperty} to indicate that the drag
     * operation is still ongoing
     * @param start
     * @param e
     */
    public void dragThumb(Point2D start, double x, double y) {
        thumbStateProperty.setValue(new ThumbState(start, x, y));
    }
   
}
