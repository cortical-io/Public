package io.cortical.iris.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cortical.fx.webstyle.CorticalLogoBackground;
import io.cortical.iris.WindowService;
import io.cortical.iris.window.Window;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * The node container of the entire UI. The {@link ControlPane}, on the left; the {@link WindowPane}
 * in the center; and the header at the top; and the {@link Overlay} for global dialogs.
 * 
 * <pre>
 *                          -----------------------------------------------
 *      ContentPane --->    |                                             |
 *                          |                   Header                    |
 *                          |                                             |
 *                          -----------------------------------------------
 *                          |     C    :                                  |
 *                          |     o    :                                  |
 *                          |     n    :                                  |
 *                          |     t    :                                  |
 *                          |     r    :                                  |
 *                          |     o    :        Window Pane               |
 *                          |     l    :                                  |
 *                          |          :                                  |
 *                          |     P    :                                  |
 *                          |     a    :                                  |
 *                          |     n    :                                  |
 *                          |     e    :                                  |
 *                          -----------------------------------------------
 * </pre>
 */
public class ContentPane extends StackPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentPane.class);
    
    public static final KeyCodeCombination ALT_L = new KeyCodeCombination(KeyCode.L, KeyCombination.ALT_DOWN);
    public static final KeyCodeCombination ALT_S = new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN);
    public static final KeyCodeCombination ALT_H = new KeyCodeCombination(KeyCode.H, KeyCombination.ALT_DOWN);
    public static final KeyCodeCombination ALT_W = new KeyCodeCombination(KeyCode.W, KeyCombination.ALT_DOWN);
    public static final KeyCodeCombination ALT_SHIFT_W = new KeyCodeCombination(KeyCode.W, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN);
    public static final KeyCodeCombination ALT_M = new KeyCodeCombination(KeyCode.M, KeyCombination.ALT_DOWN);
    public static final KeyCodeCombination ALT_D = new KeyCodeCombination(KeyCode.D, KeyCombination.ALT_DOWN);
    
    private WindowPane windowPane;
    private Header header;
    private Overlay overlay;
    private ControlPane controlPane;
    
    private CorticalLogoBackground backGround;
    
    
    /**
     * Constructs a new {@code ContentPane}
     */
    public ContentPane() {
        WindowService.getInstance().setContentPane(this);
        
        BorderPane mainPane = new BorderPane();
        mainPane.prefWidthProperty().bind(widthProperty());
        mainPane.prefHeightProperty().bind(heightProperty());
        mainPane.setTop(createHeader());
        
        Pane controlPane = getControlPane();
        mainPane.setLeft(controlPane);
        mainPane.getStyleClass().add("main-borderpane");
        BorderPane.setMargin(controlPane, new Insets(20, 20, 0, 0));
        
        WindowPane centerPane = getWindowPane();
        backGround = new CorticalLogoBackground(centerPane);
        backGround.setOpacity(0.05);
        backGround.toBack();
        centerPane.getChildren().add(backGround);
        BorderPane.setMargin(centerPane, new Insets(20, 0, 0, 0));
        mainPane.setCenter(centerPane);
        
        getStyleClass().add("main-background");
        
        getChildren().addAll(mainPane, getOverlay());
        
        addGlobalKeyListener();
        
        setFocusTraversable(false);
    }
    
    /**
     * Adds the application's global key listener for detecting actions 
     * across the entire app.
     */
    public void addGlobalKeyListener() {
        addEventFilter(KeyEvent.ANY, e -> {
            if(e.getEventType().equals(KeyEvent.KEY_RELEASED)) {
                LOGGER.debug("Global Key Event Filter: got key release: " + e.getCode() + " - " + e.getCharacter());
            }
            
            if(ALT_H.match(e) && e.getEventType().equals(KeyEvent.KEY_PRESSED)) {
                Window w = WindowService.getInstance().getSelectedWindow();
                if(w != null) {
                    LOGGER.debug("Global Key Event Filter: trapped Alt+H - Hiding Window: " + w.getTitle());
                    w.setVisible(false);
                }
                
                e.consume();
            }
            
            if(ALT_D.match(e) && e.getEventType().equals(KeyEvent.KEY_PRESSED)) {
                ControlPane controlPane = WindowService.getInstance().getControlPane();
                controlPane.getInputWindowBox().clipDetectorVisibleProperty().set(
                    !controlPane.getInputWindowBox().clipDetectorVisibleProperty().get());
                
                LOGGER.debug("Global Key Event Filter: trapped Alt+D - Toggling Clipboard Monitor to on: " + 
                    controlPane.getInputWindowBox().clipDetectorVisibleProperty().get());
                                
                e.consume();
            }
        });
    }
    
    /**
     * Returns the {@link Overlay} used for stylish display of modal dialogs.
     * @return the {@link Overlay}
     */
    public Overlay getOverlay() {
        if(overlay == null) {
            overlay = new Overlay();
        }
        return overlay;
    }
    
    /**
     * Returns the pane which acts as a palette for all {@link Window}s.
     * @return  the window palette
     */
    public WindowPane getWindowPane() {
        if(windowPane == null) {
            windowPane = new WindowPane();
        }
        return windowPane;
    }
    
    /**
     * Get the logo pane which sits in the very bottom of the application window z-order
     * @return
     */
    public CorticalLogoBackground getLogoBackground() {
        return backGround;
    }
    
    /**
     * Returns the left side {@link Pane} which has the 
     * main application controls for windows of the 
     * application, and resides on the left side of this
     * main content pane.
     * 
     * @return  the control pane
     */
    public ControlPane getControlPane() {
        if(controlPane == null) {
            controlPane = new ControlPane(this);
        }
        
        return controlPane;
    }
    
    /**
     * Creates and returns the header of the entire app residing in the "North"
     * side of the over all display.
     * @return  the app header
     */
    private Header createHeader() {
        if(header == null) {
            header = new Header();
        }
        
        return header;
    }
}
