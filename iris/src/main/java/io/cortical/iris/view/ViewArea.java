package io.cortical.iris.view;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.window.InputBar;
import io.cortical.iris.window.Resizable;
import io.cortical.iris.window.Window;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;


/**
 * Container for a single view selected by the user via the {@link InputBar}'s 
 * view options. Each view is registered and associated with known keys represented
 * by the {@link ViewType} enum. These keys are then used to indicate view transitions
 * upon the user selecting a new view.
 * 
 * @author cogmission
 *
 */
public abstract class ViewArea extends Pane implements Resizable, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(ViewArea.class);

    /** All View Areas are concerned with server message progress */
    public static final Pattern PROGRESS_PATTERN = Pattern.compile("ServerMessage[\\w]+Progress[\\-\\w]+");
    
    /** The view currently visible to the user */
    protected transient View selectedView;
    
    /** Registry of known views */
    protected transient Map<ViewType, View> registeredViews;
    
    /** Wrapper for read-only view selection property retrieval */
    protected transient ReadOnlyObjectWrapper<ViewType> selectedViewPropertyWrapper;
    
    
    
    /**
     * Constructs a new {@code ContentArea}
     */
    public ViewArea() {
        registeredViews = new HashMap<>();
        selectedViewPropertyWrapper = new ReadOnlyObjectWrapper<ViewType>();
        getStyleClass().add("content-area");
    }
    
    /**
     * Restores the state of a saved window configuration, 
     * into this {@link Window}
     * @param config
     */
    public abstract void configure(WindowConfig config);
    
    /**
     * Handles any view-specific release of listeners or resources when the
     * parent window is being closed.
     */
    public abstract void releaseResourcesForWindowClose();
    
    /**
     * Registers the specified {@link Parent} (or a derivative of Parent)
     * as the view indicated when the specified {@link ViewType} enum
     * is specified.
     * 
     * @param view
     * @param pane
     */
    public void registerView(ViewType view, View parent) {
        registeredViews.put(view, parent);
    }
    
    /**
     * Selects the view Object represented by the
     * enum "view"
     * @param type
     */
    public abstract void selectView(ViewType type);
    
    /**
     * Returns the currently selected view
     * @return
     */
    public View getSelectedView() {
        return selectedView;
    }
    
    /**
     * Returns the view specified by the registered type indicated
     * by the argument "type".
     * 
     * @param type
     * @return
     */
    public View getView(ViewType type) {
        return registeredViews.get(type);
    }
    
    /**
     * Returns a property which is set when the user selects a new view.
     * @return
     */
    public ReadOnlyObjectProperty<ViewType> selectedViewProperty() {
        return selectedViewPropertyWrapper.getReadOnlyProperty();
    }
    
    /**
     * Iterates over the list of registered views and calls {@link View#reset()}
     * on them.
     */
    public void resetViews() {
        registeredViews.values().stream().forEach(v -> v.reset());
    }

    /**
     * Returns the height of the view which is currently
     * selected.
     */
    @Override
    public abstract double computeHeight();
}
