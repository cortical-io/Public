package io.cortical.iris.view;

import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Thumb;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.layout.Region;

/**
 * Decouples the nested Nodes from their containing Nodes so that
 * contained children don't refer directly nor have any knowledge of
 * their containing Nodes. 
 * <p>
 * Provides property retrieval services so that child nodes can "hook"
 * into their unknown parents upon usage.
 * 
 * @author cogmission
 *
 */
public interface WindowContext {
    
    /**
     * Returns the parent node implementing this interface.
     * @return
     */
    public Region getRegion();
    
    /**
     * Returns a flag indicating whether this {@code WindowContext} 
     * refers to an {@link InputWindow} or {@link OutputWindow}.
     * 
     * @return
     */
    public boolean isInput();
    
    /**
     * Returns the read only width property of this {@code DisplayHost}
     * @return
     */
    public default ReadOnlyDoubleProperty widthProperty() {
        return getRegion().widthProperty();
    }
    
    /**
     * Returns the read only height property of this {@code DisplayHost}
     * @return
     */
    public default ReadOnlyDoubleProperty heightProperty() {
        return getRegion().heightProperty();
    }
    
    /**
     * Returns the preferred width property of this {@code DisplayHost}
     * @return
     */
    public default DoubleProperty prefWidthProperty() {
        return getRegion().prefWidthProperty();
    }
    
    /**
     * Returns the preferred height property of this {@code DisplayHost}
     * @return
     */
    public default DoubleProperty prefHeightProperty() {
        return getRegion().prefHeightProperty();
    }
    
    /**
     * Adds the specified handler as a handler to this context implementor's
     * events of the specified type.
     * 
     * @param type          the event type
     * @param handler       the handler for the event type
     */
    public default <T extends Event> void addEventHandler(EventType<T> type, EventHandler<? super T> handler) {
        getRegion().addEventHandler(type, handler);
    }
    
    /**
     * Returns the handler for sizing the window.
     * @return
     */
    public Thumb getThumb();
}
