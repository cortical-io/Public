 package io.cortical.iris.window;

import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A grouping of {@link Window}s used primarily for
 * one-to-many and many-to-one relationships.
 * <p>
 * Usage is as follows:
 * <pre>
 *      primaryWindowProperty().addListener((v,o,n) -> {
 *          System.out.println("primary window set to: " + (n == null ? null : n.getTitle()) );
 *      });
 *      
 *      getWindows().addListener((Change<? extends Window> c) -> {
 *          c.next();
 *          if(c.wasAdded()) {
 *              System.out.println("window added to group: " + c.getAddedSubList().get(0).getTitle());
 *          } else if(c.wasRemoved()) {
 *              System.out.println("window removed from group: " + c.getRemoved().get(0).getTitle());
 *          }
 *      });
 * </pre>
 * 
 * 
 * @author cogmission
 */
public class WindowGroup {

    private ObservableList<Window> windows = FXCollections.observableArrayList();
    
    private ObjectProperty<Window> primaryWindowProperty;
    private ObjectProperty<Window> secondaryWindowProperty;
    private OccurrenceProperty swapDetectedProperty;
    /** When primary is removed from a group, the secondary "adopts" primary status */
    private OccurrenceProperty primaryAdoptionProperty;
    private BooleanProperty removedWindowProperty;
    
    private OccurrenceProperty windowAddedProperty;
    
    private boolean isTest;
    
    
    /**
     * Constructs an empty {@code WindowGroup}
     */
    public WindowGroup() { this((Window[])null); }
    
    /**
     * Constructs a new {@code WindowGroup}
     * @param windows   the {@link Window}s to add
     */
    public WindowGroup(Window... windows) {
        if(windows != null) {
            this.windows.addAll(windows);
        }
        
        primaryWindowProperty = new SimpleObjectProperty<>();
        secondaryWindowProperty = new SimpleObjectProperty<>();
        swapDetectedProperty = new OccurrenceProperty();
        primaryAdoptionProperty = new OccurrenceProperty();
        removedWindowProperty = new SimpleBooleanProperty();
        windowAddedProperty = new OccurrenceProperty();
        
        // Sets the secondary window following a primary window change
        primaryWindowProperty.addListener((v,o,n) -> {
            // Not a swap if window removed
            if(removedWindowProperty.get()) return;
            
            if(n != null) { // If the new primary is null then this isn't a swap
                secondaryWindowProperty.set(o);
                if(o != null) {
                    // If the primary changes and there was an "old" primary - it must be a swap
                    swapDetectedProperty.set();
                }
            }
        });
    }
    
    /**
     * Returns an {@link ObservableList} of this group's
     * {@link Window}s
     * 
     * @return  an {@link ObservableList} of this group's {@link Window}s
     */
    public ObservableList<Window> getWindows() {
        return windows;
    }
    
    /**
     * Returns a property that signals when a swap of the primary and secondary
     * windows has occurred.
     * 
     * @return
     */
    public OccurrenceProperty swapDetectedProperty() {
        return swapDetectedProperty;
    }
    
    /**
     * Returns a property which emits changes to the mutually
     * exclusive primary designate.
     * 
     * @return  the newly set primary window
     */
    public ObjectProperty<Window> primaryWindowProperty() {
        return primaryWindowProperty;
    }
    
    /**
     * Returns the primary window of this group.
     * @return  the primary window of this group
     */
    public Window getPrimaryWindow() {
        return primaryWindowProperty.get();
    }
    
    /**
     * Returns a property which emits changes to the mutually
     * exclusive secondary designate.
     * 
     * @return  the newly set secondary window
     */
    public ObjectProperty<Window> secondaryWindowProperty() {
        return secondaryWindowProperty;
    }
    
    /**
     * Returns the secondary window resulting from a window search.
     * @return  the secondary window
     */
    public Window getSecondaryWindow() {
        return secondaryWindowProperty.get();
    }
    
    /**
     * Returns the property that notifies listeners when the secondary
     * adopts primary status after removal of a primary from the group.
     * @return
     */
    public OccurrenceProperty primaryAdoptionProperty() {
        return primaryAdoptionProperty;
    }
    
    /**
     * Returns an {@link ObservableList} of {@link Window}s
     * @return  an {@link ObservableList} of {@link Window}s
     */
    public ObservableList<Window> getChildren() {
        return windows;
    }
    
    /**
     * Adds the specified {@link Window} to this group.
     * @param w the {@link Window} to add
     */
    public void addWindow(Window w) {
        if(w != null) {
            windows.add(w);
            if(primaryWindowProperty.get() != null) {
                secondaryWindowProperty.set(w);
            }else{
                primaryWindowProperty.set(w);
            }
        }
        
        windowAddedProperty.set();
    }
    
    /**
     * Returns a property which notifies listeners when a window has
     * been added.
     * @return  the window added property
     */
    public OccurrenceProperty windowAddedProperty() {
        return windowAddedProperty;
    }
    
    /**
     * Removes the specified {@link Window} from this group,
     * if it exists.
     * @param w the {@link Window} to remove
     */
    public void removeWindow(Window w) {
        // Mark removed flag for succeeding events to observe and distinguish
        // between "swap" and "removal" of a window.
        removedWindowProperty.set(true);
        
        windows.remove(w);
        if(primaryWindowProperty.get() != null) {
            if(primaryWindowProperty.get().getWindowID().equals(w.getWindowID())) {
                if(secondaryWindowProperty.get() != null) {
                    primaryWindowProperty.set(secondaryWindowProperty.get());
                    secondaryWindowProperty.set(null);
                    primaryAdoptionProperty.set();
                } else {
                    primaryWindowProperty.set(null);
                }
            }else if(secondaryWindowProperty.get() != null && secondaryWindowProperty.get().getWindowID().equals(w.getWindowID())) {
                secondaryWindowProperty.set(null);
            }
        }
        
        if(windows.size() == 0) {
            primaryWindowProperty.set(null);
            secondaryWindowProperty.set(null);
        }
        
        // Reset back after all window removal logic has executed so
        // listeners which check for this property won't see the wrong flag.
        if(!isTest) {
            Platform.runLater(() -> removedWindowProperty.set(false));
        }
    }
    
    /**
     * Returns a flag indicating whether the specified {@link Window}
     * exists in this grouping.
     * @param w     the {@link Window} to search for.
     * @return  true if the Window exists in this group, false if it does not.
     */
    public boolean hasWindow(Window w) {
        return windows.contains(w);
    }
    
    /**
     * Returns the secondary window resulting from a window search.
     * @return  the secondary window
     */
    public Window secondaryWindowSearch() {
        Window secondaryWindow = windows.stream()
            .filter(w -> w.getWindowID() != getPrimaryWindow().getWindowID())
            .findFirst()
            .orElse(null);
        
        return secondaryWindow;
    }
    
    /**
     * Allows manipulation of "isTest" flag (for testing purposes).
     * @param b
     */
    public void setTestMode(boolean b) {
        this.isTest = b;
    }
}
