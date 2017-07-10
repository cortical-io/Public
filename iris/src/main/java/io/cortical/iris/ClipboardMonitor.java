package io.cortical.iris;

import java.time.Duration;

import org.reactfx.EventStreams;
import org.reactfx.Subscription;

import io.cortical.iris.ui.WindowBox;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCombination;

/**
 * Monitors the operating system's clipboard for copy or cut actions
 * and stores the last copied item for later retrieval.
 */
public class ClipboardMonitor implements ChangeListener<Boolean> {
    /**
     *  This is needed because the clipboard will only return the last copied data ONCE, so
     * if two calls are made, the second will return null even though something was copied
     * to it.
     */
    private String oldString = "";
    
    /** Stores the subscription so that it may be "unsubscribed" to later. */
    private Subscription subscription;
    
    /**
     *  True when {@link #attach()} has been called and this {@code ClipboardMonitor}
     *  has added itself as a listener to {@link WindowBox#clipDetectorVisibleProperty()}.
     */
    private volatile boolean isAttached;
    
    /** True when the user has issue the keystroke activating this {@code ClipboardMonitor} */
    private volatile boolean isSubscribed;
    
    /** Singleton instance of this {@code ClipboardMonitor} */
    private static volatile ClipboardMonitor instance = new ClipboardMonitor();
    
    
    
    
    /**
     * Private constructor for singleton instance.
     */
    private ClipboardMonitor() {}
    
    /**
     * Returns the singleton instance of this {@code ClipboardMonitor}
     * @return
     */
    public static ClipboardMonitor getInstance() {
        return instance;
    }
    
    /**
     * Called once during the uptime of a JVM to request that this {@code ClipboardMonitor} 
     * adds itself as a listener to the {@link WindowBox#clipDetectorVisibleProperty()} which
     * is toggled by a user to indicate the clipboard should be monitored for new copy or cut
     * actions.
     */
    public synchronized void attach() {
        if(!isAttached) {
            // Initialize the clip board cache to the current state, in order to only
            // track changes since the app has been running.
            if(Clipboard.getSystemClipboard().hasString()) {
                oldString = Clipboard.getSystemClipboard().getString();
            }
            
            WindowService.getInstance().getControlPane().getInputWindowBox().
                clipDetectorVisibleProperty().addListener(this);
            
            isAttached = true;
        }
    }
    
    /**
     * Called when the user issues the {@link KeyCombination} registered to activate this monitor.
     * This monitor then subscribes to the {@link EventStreams#ticks(Duration)} method, passing
     * it a handler which activates the {@link WindowBox#clipDetectedProperty()} if true, or if
     * false, unsubscribes the clipboard monitoring handler.
     * 
     * @param n     true if activated (should subscribe), false if not (should unsubscribe).
     */
    public synchronized void toggleExecution(boolean n) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent clip = new ClipboardContent();
        clip.put(DataFormat.PLAIN_TEXT, "");
        clipboard.setContent(clip);
        
        Platform.runLater(() -> {
            System.out.println("clipboard.hasString() ? "  + clipboard.hasString() + ",  " + clipboard.getContent(DataFormat.PLAIN_TEXT));
            
            if(n && !isSubscribed) {
                oldString = "";
                
                subscription = EventStreams.ticks(Duration.ofMillis(2000)).subscribe(tick -> {
                    if (clipboard.hasString()) {
                        String newString = clipboard.getString();
                        if(!oldString.equals(newString)) {
                            System.out.println("newString = " + newString);
                            
                            oldString = newString;
                            
                            WindowService.getInstance().getControlPane().getInputWindowBox().clipDetectedProperty().set(true);
                        }
                    }
                });
            } else if(!n) {
                if(subscription != null) {
                    subscription.unsubscribe();
                    isSubscribed = ((subscription = null) != null);
                }
            }
        });
    }

    /**
     * Implements the {@link ChangeListener} interface to turn on or off monitoring of
     * the clipboard for new copy/cut actions.
     */
    @Override
    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        toggleExecution(newValue);
    }
    
    /**
     * Returns a flag indicating whether this {@code ClipboardMonitor} is "listening" for 
     * key strokes to activate or deactivate its subscription handler. This does not indicate
     * that this monitor is actively watching the clipboard; this indicates rather that this
     * monitor is simply available to be turned on to watch the clipboard. See {@link #isSubscribed}
     * to see if this monitor is actively watching the clipboard.
     * 
     * @return      true if subscribed to the {@link EventStreams}, false if not.
     */
    public boolean isAttached() {
        return isAttached;
    }
    
    /**
     * Returns a flag indicating whether this {@code ClipboardMonitor} is actively watching
     * the system clipboard or not.
     * 
     * @return      true if listening to the system clipboard for copy/cut actions, false
     *              if not.
     */
    public boolean isSubscribed() {
        return isSubscribed;
    }
    
    /**
     * Returns the last copied String saved by this monitor.
     * @return
     */
    public String getClipboardContent() {
        return oldString;
    }
}
