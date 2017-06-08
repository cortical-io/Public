package io.cortical.iris.view;

import io.cortical.iris.ui.custom.radialmenu.RadialMenu;
import javafx.scene.Node;
import javafx.scene.shape.Shape;

/**
 * Container for submission of required menu request attributes.
 * 
 * {@code MenuRequest}s are constructed using the "fluent" pattern
 * and fluent method style as follows:
 * <pre>
 * MenuRequest request = 
 *     MenuRequest.builder()
 *         .source(Node)
 *         .context(WindowContext)
 *         .menu(RadialMenu)
 *         .srcX(double)
 *         .srcY(double)
 *         .overlay(boolean)
 *         .focalShape(Shape)
 *         .build();
 * </pre>
 *         
 * 
 * @author cogmission
 */
public class MenuRequest {
    public enum Type { SHOW, HIDE };
    
    private Node source;
    
    private WindowContext context;
    
    private RadialMenu menu;
    
    private double srcX;
    private double srcY;
    private boolean showOverlay;
    
    private Type type;
    
    private Shape focalShape;
    
    /**
     * Enforces creation through builder only.
     * 
     * @param type              {@link Type#SHOW} or {@link Type#HIDE}
     * @param source            the "target" object acting as trigger
     * @param context           the {@link WindowContext}
     * @param menu              the {@link RadialMenu} to show
     * @param srcX              the x location of the source object
     * @param srcY              the y location of the source object
     * @param showOverlay       flag indicating whether to show the menu on top of overlay
     * @param focalShape        optional shape to subtract (show as open cut-out of overlay)
     */
    private MenuRequest(Type type, Node source, WindowContext context, RadialMenu menu, double srcX, double srcY, boolean showOverlay, Shape focalShape) {
        super();
        this.type = type;
        this.source = source;
        this.context = context;
        this.menu = menu;
        this.srcX = srcX;
        this.srcY = srcY;
        this.showOverlay = showOverlay;
        this.focalShape = focalShape;
    }

    /**
     * Returns a new Builder.
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Returns the request type
     * @return
     */
    public Type getType() {
        return type;
    }
    
    /**
     * @return the source
     */
    public Node getSource() {
        return source;
    }

    /**
     * @return the context
     */
    public WindowContext getContext() {
        return context;
    }

    /**
     * Sets the mutable window context.
     * @param context
     */
    public void setContext(WindowContext context) {
        this.context = context;
    }
    
    /**
     * @return the menu
     */
    public RadialMenu getMenu() {
        return menu;
    }

    /**
     * @return the srcX
     */
    public double getSrcX() {
        return srcX;
    }

    /**
     * @return the srcY
     */
    public double getSrcY() {
        return srcY;
    }

    /**
     * @return the showOverlay
     */
    public boolean isShowOverlay() {
        return showOverlay;
    }

    /**
     * @return the focalShape
     */
    public Shape getFocalShape() {
        return focalShape;
    }

    /**
     * Builds a new {@link MenuRequest} performing any needed fail-fast validation
     * required.
     * 
     * @author cogmission
     */
    public static class Builder {
        private Type type;
        
        private Node source;
        
        private WindowContext context;
        
        private RadialMenu menu;
        
        private double srcX;
        private double srcY;
        private boolean showOverlay;
        
        private Shape focalShape;
        
        public MenuRequest build() {
            return new MenuRequest(type, source, context, menu, srcX, srcY, showOverlay, focalShape);
        }
        
        public MenuRequest.Builder type(Type type) {
            this.type = type;
            return this;
        }
        
        public MenuRequest.Builder source(Node src) {
            this.source = src;
            return this;
        }
        
        public MenuRequest.Builder context(WindowContext ctx) {
            this.context = ctx;
            return this;
        }
        
        public MenuRequest.Builder menu(RadialMenu menu) {
            this.menu = menu;
            return this;
        }
        
        public MenuRequest.Builder srcX(double x) {
            this.srcX = x;
            return this;
        }
        
        public MenuRequest.Builder srcY(double y) {
            this.srcY = y;
            return this;
        }
        
        public MenuRequest.Builder overlay(boolean showOverlay) {
            this.showOverlay = showOverlay;
            return this;
        }
        
        public MenuRequest.Builder focalShape(Shape focusShape) {
            this.focalShape = focusShape;
            return this;
        }
    }
    
}
