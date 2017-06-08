package io.cortical.iris.window;

import java.util.UUID;

import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Message;
import io.cortical.iris.message.Payload;
import io.cortical.iris.view.ViewType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.util.Pair;

/**
 * Upper left corner display {@code Node} which can have a custom color 
 * set on its displayed shape which can be used as an identifier.
 * 
 * @author cogmission
 * @see ColorPicker
 */
public class ColorIDTab extends StackPane {
    public final static Color DEFAULT_ID_COLOR = Color.rgb(26, 73, 94);
    
    private ObjectProperty<Paint> tabPaintProperty = new SimpleObjectProperty<>();
    
    private ColorPicker colorPicker;
    
    private Polygon mouseTarget;
    
    private EventHandler<MouseEvent> pickerHandler;
    private boolean pickerShowing;
    
    /**
     * Constructs a new {@code IDTab}
     */
    public ColorIDTab() {
        getStyleClass().add("id-tab");
        resizeRelocate(1, 0, 30, 25);
        setManaged(false);
        
        colorPicker = new ColorPicker(DEFAULT_ID_COLOR);
        colorPicker.getStyleClass().add("button");
        colorPicker.setVisible(false);
        
        final Polygon pol = mouseTarget = new Polygon();
        pol.getPoints().addAll(new Double[] {
            0.0, 7.5,
            5.0, 0.0,
            12.0, 0.0,
            17.0, 7.5,
            12.0, 15.0,
            5.0, 15.0,
            0.0, 7.5
        });
        pol.setStroke(Color.rgb(205, 205, 205));
        pol.setManaged(false);
        pol.relocate(5, 4);
        pol.setFill(DEFAULT_ID_COLOR);
        pol.setScaleX(1.2);
        pol.setScaleY(1.1);
        Tooltip t = new Tooltip("Set color ID");
        Tooltip.install(pol, t);
        getChildren().addAll(pol, colorPicker);
        
        toFront();
        
        addEventHandler(MouseEvent.MOUSE_CLICKED, getPickerHandler());
        
        colorPicker.setOnHidden(getHideHandler());
        
        tabPaintProperty.addListener((v,o,n) -> { 
            pol.setFill(n); 
            
            Window w = WindowService.getInstance().windowFor(this);
            if(w != null) {
                Payload request = new Payload(new Pair<UUID, Message>(w.getWindowID(), new Message(ViewType.EXPRESSION, null)));
                request.setWindow(w);
                EventBus.get().broadcast(BusEvent.FINGERPRINT_DISPLAY_REPLACE.subj() + 
                    WindowService.getInstance().windowFor(this).getWindowID().toString(), request);
            }
        });
        
        tabPaintProperty.set(DEFAULT_ID_COLOR);
    }
    
    /**
     * Returns the {@link Polygon} used as the mouse target for
     * displaying a color picker.
     * @return
     */
    public Polygon getPolygon() {
        return mouseTarget;
    }
    
    /**
     * Displays the {@link ColorPicker} attached to this tab.
     */
    public void show() {
        colorPicker.show();
    }
    
    /**
     * Hides the {@link ColorPicker} attached to this tab.
     */
    public void hide() {
        colorPicker.hide();
    }
    
    /**
     * Sets the {@link Paint} identifier filling this tab's
     * shape.
     * 
     * @param p     the Paint to set
     */
    public void setPaint(Paint p) {
        tabPaintProperty.set(p);
    }
    
    /**
     * Returns the {@link Paint} identifier filling this tab's
     * shape.
     * @return  the configured Paint
     */
    public Paint getPaint() {
        return tabPaintProperty.get();
    }
    
    /**
     * Returns the {@link Paint} identifier property filling this tab's
     * shape.
     * @return  the configured Paint property
     */
    public ObjectProperty<Paint> colorIDProperty() {
        return tabPaintProperty;
    }
    
    /**
     * Returns a handler to set the new color property upon the
     * color picker window being hidden.
     * 
     * @return  the "hide" handler
     */
    private EventHandler<Event>getHideHandler() {
        // Bug! When the value property is set, the event gets consumed upon
        //      hiding the color picker so the mouse handler can't toggle the
        //      "pickerShowing" boolean to false; resulting in two clicks to 
        //      show the color picker the next time.
        colorPicker.valueProperty().addListener((v,o,n) -> {
            pickerShowing = false;
        });
        
        // Return the actual handler
        return e -> { 
            tabPaintProperty.set(colorPicker.getValue());
        };
    }
    
    /**
     * Returns a handler for recording the "showing" state 
     * of the color picker since the picker window event 
     * forwarding is idiosyncratic.
     * 
     * @return  the picker hide/show handler.
     */
    private EventHandler<MouseEvent> getPickerHandler() {
        if(pickerHandler == null) { 
            pickerHandler = (MouseEvent e) -> {
                if(pickerShowing) {
                    hide(); 
                }else{
                    show(); 
                }
                
                pickerShowing = !pickerShowing;
            };
        }
         
        return pickerHandler;
    }
    
    
}
