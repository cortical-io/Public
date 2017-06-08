package io.cortical.iris.window;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;


/**
 * Encloses the window control widgets used to control 
 * maximize, iconize and close behavior for a given {@link Window}
 * 
 * @author cogmission
 * @see Widget
 * @see LineWidget
 */
public class WindowControl extends HBox {
    
    private Widget min, x;
    
    
    /**
     * Constructs a new {@code WindowControl}
     */
    public WindowControl() {
        min = new Widget(15, 4);
        x = new LineWidget(23, 7, 33, 20);
        
        getChildren().addAll(min, x.getX1(), x.getX2(), x.getOutline());
        setAlignment(Pos.BOTTOM_CENTER);
        setSpacing(5);
        getStyleClass().add("window-icon-box");
    }
    
    /**
     * Return the minimize or iconize {@link Widget}
     * @return
     */
    public Widget getMin() {
        return min;
    }
    
    /**
     * Return the close {@link Widget}
     * @return
     */
    public LineWidget getX() {
        return (LineWidget)x;
    }
    
    /**
     * Window control widget button
     */
    public static class Widget extends Rectangle {
        
        public Widget(double w, double h) {
            this(0, 0, w, h);
        }
        
        public Widget(double x, double y, double w, double h) {
            super(x, y, w, h);
            
            setArcWidth(5);
            setArcHeight(5);
            setFill(Color.TRANSPARENT);
            getStyleClass().add("window-icon-box-icons");
        }
        
        protected Line getX1() {
            return null;
        }
        
        protected Line getX2() {
            return null;
        }
        
        protected Rectangle getOutline() {
            return null;
        }
    }
    
    /**
     * Window control widget close ("x") button
     */
    public static class LineWidget extends Widget {
        Rectangle r;
        Line x1;
        Line x2;
        
        public LineWidget(double x1, double y1, double x2, double y2) {
            super(x1, y1, x2 - x1, y2 - y1);
            
            this.r = new Rectangle(x1, y1, x2 - x1, y2 - y1);
            
            this.x1 = new Line(x1, y1, x2, y2);
            this.x2 = new Line(x1, y2, x2, y1);
            this.x1.setManaged(false);
            this.x2.setManaged(false);
            this.r.setManaged(false);
            this.x1.setStroke(Color.rgb(26, 73, 94));
            this.x1.setStrokeWidth(2);
            this.x2.setStroke(Color.rgb(26, 73, 94));
            this.x2.setStrokeWidth(2);
            this.r.setStroke(Color.TRANSPARENT);
            this.r.setFill(Color.TRANSPARENT);
            this.x1.setStrokeLineCap(StrokeLineCap.ROUND);
            this.x2.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        
        @Override
        public Line getX1() {
            return x1;
        }
        
        @Override
        public Line getX2() {
            return x2;
        }
        
        @Override
        public Rectangle getOutline() {
            return r;
        }
    }
}
