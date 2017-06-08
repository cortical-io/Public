package io.cortical.iris.window;

import static io.cortical.iris.message.BusEvent.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.bushe.swing.event.EventTopicSubscriber;

import io.cortical.iris.WindowService;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.WindowEventHandler;
import io.cortical.iris.view.MenuRequest;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.window.WindowControl.LineWidget;
import io.cortical.iris.window.WindowControl.Widget;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.geometry.Dimension2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;




/**
 * MVC Controller which handles window scoped operations.
 * 
 * @author cogmission
 *
 */
public class WindowController implements EventTopicSubscriber<Payload> {
    private Window window;
    
    private static WindowEventHandler WINDOW_CLOSE_ACTION;
    private static WindowEventHandler WINDOW_HIDE_ACTION;
    private static WindowEventHandler WINDOW_ICONIZE_ACTION;
    private static WindowEventHandler WINDOW_DE_ICONIZE_ACTION;
    private static Map<String, WindowEventHandler> WINDOW_ACTION_MAP = new HashMap<>();
    
    static {
        installWindowHandlers();
    }
    
    /**
     * Constructs a new {@code WindowController}
     * @param inputWindow
     */
    public WindowController(Window window) {
        this.window = window;
        
        attach(window);
        
        EventBus.get().subscribeTo(Pattern.compile("Window[\\w]+Request"), this);
    }
    
    /**
     * Convenience method to resize the target window by computing its 
     * minimum height. 
     * 
     * @param window
     */
    public void resizeWindow(Window window) {
        Platform.runLater(() -> {
            Dimension2D dims = window.computeMinSize();
            
            window.resize(window.getWidth(), dims.getHeight());
            window.requestLayout();
        });
    }
    
    /**
     * Returns a listener for the {@link ViewType#EXPRESSION}'s radial menu request for
     * adding operators.
     * 
     * @param iw
     */
    public ChangeListener<MenuRequest> getExpressionDisplayRadialMenuRequestListener(InputWindow iw) {
        return (v,o,n) -> {
            if(n.getType() == MenuRequest.Type.SHOW) {
                resizeWindow(iw);
                iw.showExpressionDisplayRadialMenu(n.getSource(), n.getMenu(), n.getSrcX(), 
                    n.getSrcY(), n.isShowOverlay(), n.getFocalShape());
            }else{
                iw.hideExpressionDisplayRadialMenu(n.getMenu());
            }
        };
    }
    
    /**
     * The problem with change listener style event management is that something has to change
     * in order for handlers to get invoked. Ergo this method. We move the window back and forth
     * to get the change listeners to size everything up correctly inside a given window.
     * 
     * @param w
     */
    public void nudgeLayout(Window w, int nudgeAmount) {
        Platform.runLater(() -> {
            w.dragThumb(nudgeAmount, nudgeAmount);
            w.requestLayout();
            (new Thread(() -> {
                try { Thread.sleep(500); }catch(Exception e) { e.printStackTrace(); }
                Platform.runLater(() -> { w.dragThumb(-nudgeAmount, -nudgeAmount); w.requestLayout(); });
            })).start();
        });
    }
    
    /**
     * Attaches this controller to various {@link Window} components.
     * @param inputWindow
     */
    private void attach(Window window) {
        window.getThumb().thumbStateProperty().addListener(getThumbHandler(window));
        
        // Resize window when another content area view is selected (via InputBar buttons)
        if(window.getType() == Window.Type.INPUT) {
            initDefaultInputView((InputWindow)window);
        }else{
            initDefaultOutputView((OutputWindow)window);
        }
        
        // Always bring the Window to the front when the mouse is clicked 
        // anywhere within its borders.
        window.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            window.toFront();
            window.selectedProperty().set(true);
        });
        
        initTitleBar(window);
        
        window.resizeWindow();
        
        window.setController(this);
    }
    
    /**
     * Initializes the {@link TitleBar} including window control widget behavior.
     * @param window
     */
    private void initTitleBar(Window window) {
        TitleBar tb = window.getTitleBar();
        tb.titleBarStateProperty().addListener(getWindowDragHandler(window));
        
        Widget min = tb.getWindowControl().getMin();
        min.addEventHandler(MouseEvent.ANY, e -> {
            if(e.getEventType() == MouseEvent.MOUSE_ENTERED) {
                window.getScene().setCursor(Cursor.HAND);
                min.setFill(Color.rgb(70, 140, 199));
            }else if(e.getEventType() == MouseEvent.MOUSE_EXITED) {
                window.getScene().setCursor(Cursor.DEFAULT);
                min.setFill(Color.TRANSPARENT);
            }else if(e.getEventType() == MouseEvent.MOUSE_CLICKED) {
                sendRequest(window, WINDOW_ICONIZE_REQUEST.subj(), e);
            }
        });
        
        LineWidget x = tb.getWindowControl().getX();
        x.getOutline().addEventHandler(MouseEvent.ANY, e -> {
            if(e.getEventType() == MouseEvent.MOUSE_ENTERED) {
                window.getScene().setCursor(Cursor.HAND);
                x.getX1().setStroke(Color.rgb(70, 140, 199));
                x.getX2().setStroke(Color.rgb(70, 140, 199));
            }else if(e.getEventType() == MouseEvent.MOUSE_EXITED) {
                window.getScene().setCursor(Cursor.DEFAULT);
                x.getX1().setStroke(Color.rgb(26, 73, 94));
                x.getX2().setStroke(Color.rgb(26, 73, 94));
            }else if(e.getEventType() == MouseEvent.MOUSE_CLICKED) {
                sendRequest(window, WINDOW_CLOSE_REQUEST.subj(), e);
            }
        });
    }
    
    private void sendRequest(Window window, String request, MouseEvent m) {
        Payload p = new Payload(m);
        p.setWindow(window);
        EventBus.get().broadcast(request, p);
    }
    
    /**
     * Initializes the initial view state of the specified {@link InputWindow}
     * @param iw
     */
    private void initDefaultInputView(InputWindow iw) {
        Platform.runLater(() -> {
            iw.requestLayout();
        });
    }
    
    /**
     * Initializes the initial view state of the specified {@link OutputWindow}
     * @param ow
     */
    private void initDefaultOutputView(OutputWindow ow) {
        // Drag the thumb one pixel to get the layout listeners to react and initialize size and position.
        Platform.runLater(() -> {
            ow.dragThumb(1, 1);
            ow.requestLayout();
        });
    }
    
    /**
     * Returns the handler which listens to {@link Thumb} property changes 
     * of the {@link ThumbState}.
     * 
     * @param window    the input window
     * @return
     */
    private ChangeListener<ThumbState> getThumbHandler(Window window) {
        return (v,o,n) -> {
            double w = window.getLayoutBounds().getWidth() + (n.lastX() - n.getDragStart().getX());
            double h = window.getLayoutBounds().getHeight() + (n.lastY() - n.getDragStart().getY());
            
            Dimension2D dims = window.computeMinSize();
            window.resizeWindow(Math.max(dims.getWidth(), w), Math.max(dims.getHeight(), h));
            window.requestLayout();
        };
    }
    
    /**
     * Returns the handler which listens to changes in the {@link TitleBar} drag
     * state propagated by the {@link TitleBarState} changes.
     * @param window
     * @return
     */
    private ChangeListener<TitleBarState> getWindowDragHandler(Window window) {
        final Pane p = WindowService.getInstance().getContentPane().getWindowPane();
        
        return (v,o,n) -> {
            double x = window.getLayoutX() + (n.getLastEvent().getX() - n.getDragStart().getX());
            double y = window.getLayoutY() + (n.getLastEvent().getY() - n.getDragStart().getY());
            
            window.relocate(
                Math.max(0, Math.min(p.getWidth() - window.getWidth(), x)), 
                Math.max(0, Math.min(p.getHeight() - window.getHeight(), y)));
        };
    }
    
    /**
     * Implementation of {@link EventTopicSubscriber} 
     */
    @Override
    public void onEvent(String topic, Payload data) {
        WindowEventHandler handler = WINDOW_ACTION_MAP.get(topic);
        if(data.getWindow() == window) {
            handler.handle((Event)data.getPayload(), window);
        }
    }
    
    /**
     * Global {@link Window} handlers for global window events
     */
    private static void installWindowHandlers() {
        WINDOW_CLOSE_ACTION = (e, w) -> {
            WindowService.getInstance().destroyWindow(w);
            // Reset(): Make sure the the InputSelector flyout is dismissed when its parent window is.
            if(!w.isInput()) {
                ((OutputWindow)w).getInputSelector().reset();
                ((OutputWindow)w).getInputSelector().disconnect();
                ((OutputWindow)w).getViewArea().disconnectAllInputsFromEventBus();
                ((OutputWindow)w).getViewArea().removeInfoButtonListener();
            }
        };
        WINDOW_ACTION_MAP.put(WINDOW_CLOSE_REQUEST.subj(), WINDOW_CLOSE_ACTION);
        
        WINDOW_HIDE_ACTION = (e, w) -> {
            w.setVisible(false);
            // Reset(): Make sure the the InputSelector flyout is dismissed when its parent window is.
            if(!w.isInput()) {
                ((OutputWindow)w).getInputSelector().reset();
            }
        };
        WINDOW_ACTION_MAP.put(WINDOW_HIDE_REQUEST.subj(), WINDOW_HIDE_ACTION);
        
        WINDOW_ICONIZE_ACTION = (e, w) -> {
            w.iconize();
            // Reset(): Make sure that the InputSelector flyout is dismissed when its parent window is.
            if(!w.isInput()) {
                ((OutputWindow)w).getInputSelector().reset();
            }
        };
        WINDOW_ACTION_MAP.put(WINDOW_ICONIZE_REQUEST.subj(), WINDOW_ICONIZE_ACTION);
        
        WINDOW_DE_ICONIZE_ACTION = (e, w) -> {
            w.deIconize();
        };
        WINDOW_ACTION_MAP.put(WINDOW_DE_ICONIZE_REQUEST.subj(), WINDOW_DE_ICONIZE_ACTION);
    }
    
}
