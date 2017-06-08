package io.cortical.iris.view;

import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.window.BackPanelSupplier;
import javafx.beans.property.ObjectProperty;

public interface View extends BackPanelSupplier {
    
    /**
     * Default implementation of the configure method, used to reify a
     * given view following de-serialization.
     * @param config
     */
    public default void configure(WindowConfig config) {}
    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    public void reset();
    
    /**
     * Allow view to adjust UI following a tab change
     */
    public default void notifyViewChange() {}
    
    /**
     * Implemented by {@code View} subclasses to handle an error
     */
    public void processRequestError(RequestErrorContext context);
    
    /**
     * Returns the property which is updated with a new {@link Payload} upon
     * message model creation.
     * @return  the message property
     */
    public ObjectProperty<Payload> messageProperty();
    
    /**
     * Sometimes layouts required a bit of "extra" handling upon first layout (i.e.
     * in order to get property listeners to fire and set their values). This flag
     * indicates whether the view has been initially laid out or not.
     */
    public default boolean isFirstLayout() {
        return false;
    }
}
