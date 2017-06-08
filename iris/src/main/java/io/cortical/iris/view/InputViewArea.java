package io.cortical.iris.view;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.view.input.expression.ExpressionDisplay;
import io.cortical.iris.view.input.text.TextDisplay;
import io.cortical.iris.window.InputBar;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.Resizable;
import io.cortical.iris.window.Window;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.util.Pair;


/**
 * Container for a single view selected by the user via the {@link InputBar}'s 
 * view options. Each view is registered and associated with known keys represented
 * by the {@link ViewType} enum. These keys are then used to indicate view transitions
 * upon the user selecting a new view.
 * 
 * @author cogmission
 *
 */
public class InputViewArea extends ViewArea {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private transient WindowContext context;
    
    private transient ObjectProperty<Window> windowAvailableProperty = new SimpleObjectProperty<>();
    private transient ReadOnlyObjectWrapper<ViewType> viewChangedPropertyWrapper = new ReadOnlyObjectWrapper<>();
    
    private transient List<Pair<Group, DoubleProperty>> pairs;
    
    private transient ViewType lastSelectedViewType;
    
    ////////////////////////
    //  Animation vars    //
    ////////////////////////
    // Defines the viewable area and cuts out anything outside of it.
    private transient Rectangle clip = new Rectangle(1, 1, 100, 400);
    private transient Timeline tl = new Timeline();
    private transient Interpolator interpolator = Interpolator.SPLINE(0.5, 0.1, 0.1, 0.5);
    
    private transient ChangeListener<? super Bounds> positioner;
    
    private transient InputWindow window;
    
    private static final double PANE_Y = 0;
    
    
    /**
     * Constructs a new instance of {@code InputViewArea}
     * @param wc
     */
    public InputViewArea(WindowContext wc) {
        this.context = wc;
        
        pairs = getToggleDisplays();
        pairs.stream().forEach(pane -> getChildren().add(pane.getKey()));
        
        clip.setVisible(true);
        setClip(clip);
        
        registerViews();
        
        EventBus.get().subscribeTo(BusEvent.INPUT_VIEWTYPE_CHANGE_BROADCAST_REQUEST.subj(), (s,p) -> {
            announceViewChange(lastSelectedViewType);
        });
        
        // Initialize listeners which respond to queries for the current model in the input window
        // Note: WindowID here is the InputWindow and its UUID
        windowAvailableProperty.addListener((v,o,n) -> {
            EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_SEND_CURRENT_MODEL_QUERY.subj() + n.getWindowID(), (s,p) -> {
                sendCurrentModelMessage(lastSelectedViewType, n.getWindowID());
            });
        });
        
        Platform.runLater(() -> {
            window.selectedProperty().addListener((v,o,n) -> {
                if(n) {
                    ViewType type = getSelectedViewType();
                    if(type == ViewType.EXPRESSION) {
                       ((ExpressionDisplay)getSelectedView()).getExpressionField().requestFocus(); 
                    }
                }
            });
        });
    }
    
    /**
     * {@inheritDoc}
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        ViewType type = registeredViews.keySet().stream().filter(k -> registeredViews.get(k) == selectedView).findFirst().orElse(null);
        out.writeObject(type);
        out.writeObject(getView(ViewType.EXPRESSION));
        out.writeObject(getView(ViewType.TEXT));
        LOGGER.debug("WRITE: Selected View: InputViewType: " + type);
    }
    
    /**
     * {@inheritDoc}
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        WindowConfig config = WindowService.getInstance().windowConfigProperty().get();
        ViewType type = (ViewType)in.readObject();
        config.selectedViewType = type;
        in.readObject();
        in.readObject();
        LOGGER.debug("READ: Selected View: InputViewType: " + type);
    }
    
    /**
     * Restores the state of a saved window configuration, 
     * into this {@link Window}
     * @param config
     */
    public void configure(WindowConfig config) {
        getView(ViewType.EXPRESSION).configure(config);
        getView(ViewType.TEXT).configure(config);
        Platform.runLater(() -> {
            InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(InputViewArea.this);
            iw.getInputBar().selectToggle(config.selectedViewType == ViewType.EXPRESSION ? 0 : 1);
        });
    }
    
    /**
     * Returns the type ({@link ViewType}) of the last selected view.
     * @return
     */
    public ViewType getSelectedViewType() {
        return lastSelectedViewType;
    }
    
    /**
     * Sends the current view's prepared message if it exists on the <em>normal</em>
     * channel. This is invoked via SERVER_MESSAGE_SEND_CURRENT_MODEL_QUERY for primary
     * switching
     * @param currentViewType   the {@link ViewType} of the currently displayed view
     * @param uuid              the UUID of the current input window
     */
    public void sendCurrentModelMessage(ViewType currentViewType, UUID uuid) {
        Payload p = registeredViews.get(currentViewType).messageProperty().get();
        if(p != null) {
            EventBus.get().broadcast(
                BusEvent.SERVER_MESSAGE_REQUEST_CREATED.subj() + uuid, p);
        }
    }
    
    /**
     * Returns the property which emits an event when the {@link Window}
     * containing this child node becomes able to be referenced.
     * @return  the property making the current Window available for reference
     */
    public ObjectProperty<Window> windowAvailableProperty() {
        return windowAvailableProperty;
    }
    
    /**
     * Announces on the event bus that a different input view has been selected.
     * @param type
     */
    public void announceViewChange(ViewType type) {
        Payload p = new Payload(type);
        p.setWindow(window);
        EventBus.get().broadcast(BusEvent.INPUT_EVENT_NAVIGATION_ACCEPTED.subj() + window.getWindowID(), p);
        viewChangedPropertyWrapper.set(type);
    }
    
    /**
     * Returns the read-only property which is set when the view changes.
     * @return
     */
    public ReadOnlyObjectProperty<ViewType> viewChangedProperty() {
        return viewChangedPropertyWrapper.getReadOnlyProperty();
    }
    
    /**
     * Selects the view Object represented by the
     * enum "view"
     * @param type
     */
    public void selectView(ViewType type) {
       if(type == lastSelectedViewType || tl.getStatus() == Animation.Status.RUNNING) return;
       selectedView = registeredViews.get(type);
       
       announceViewChange(type);
             
       (new Thread(() -> {
           // Insert Delay so animation isn't encumbered by node clean up above
           try { Thread.sleep(500); } catch(Exception e) {}
           
           Platform.runLater(() -> {
               selectedViewPropertyWrapper.set(type);
               window.resizeWindow();
               
               Group inComing = (Group)registeredViews.get(type);
               inComing.setVisible(true);
               Group outGoing = (Group)registeredViews.get(lastSelectedViewType);
               
               // The screens in the list of pairs are ordered same as the screens
               // type ordinal in the ViewType enum.
               DoubleProperty inProp = pairs.get(type.ordinal()).getValue();
               DoubleProperty outProp = pairs.get(lastSelectedViewType.ordinal()).getValue();
               
               double destX = 0;
               Bounds inBounds = inComing.boundsInLocalProperty().get();
               Bounds outBounds = outGoing.boundsInLocalProperty().get();
               inComing.resize(outBounds.getWidth(), outBounds.getHeight());
               if(type.ordinal() < lastSelectedViewType.ordinal()) {
                   inComing.relocate(-outBounds.getMinX() - 5, outGoing.getLayoutBounds().getMinY());
                   destX = outBounds.getWidth() + 5;
                   inProp.bind(outProp.subtract(inBounds.getWidth() + 5));
               }else{
                   inComing.relocate(clip.getWidth() + 5, PANE_Y);
                   destX = -outBounds.getWidth() - 5;
                   inProp.bind(outProp.add(inBounds.getWidth() + 5));
               }
               
               KeyValue keyValue = new KeyValue(outProp, destX, interpolator);
               // create a keyFrame with duration 500ms
               KeyFrame keyFrame = new KeyFrame(Duration.millis(500), keyValue);
               // erase last keyframes: forward & reverse have different frames
               tl.getKeyFrames().clear();
               // add the keyframe to the timeline
               tl.getKeyFrames().add(keyFrame);
               // remove binding above after animation is finished
               tl.setOnFinished((e) -> {
                   Platform.runLater(() -> {
                       inProp.unbind();
                       outGoing.setVisible(false);
                       // Reset the previous view (clear out all data). 
                       if(lastSelectedViewType != null) {
                           registeredViews.get(lastSelectedViewType).notifyViewChange();
                       }
                   });
               });
               
               tl.play();
               
               selectedViewPropertyWrapper.set(lastSelectedViewType = type);
           });
       })).start();
    }
    
    /**
     * Dispatches the view registration to the {@code #registerView(ViewType, View)} method.
     */
    public void registerViews() {
        IntStream.range(0, 2).forEach(i -> registerView(ViewType.values()[i], (View)pairs.get(i).getKey()));
    }
    
    /**
     * Returns a list of Pairs of Group objects (Displays) and DoubleProperties which have
     * a listener added to them which sets the x coordinate locations to which their value 
     * is changed - on the paired display. This property is used during tab transition
     * animations to manipulate the x coordinate of the screens.
     * 
     * @return  a list of pairs of displays and double properties used for location management.
     */
    private List<Pair<Group, DoubleProperty>> getToggleDisplays() {
        List<Pair<Group, DoubleProperty>> retVal = new ArrayList<>();
        
        ExpressionDisplay exp = new ExpressionDisplay(context);
        selectedView = exp;
        lastSelectedViewType = ViewType.EXPRESSION;
        exp.setVisible(true);
        
        TextDisplay text = new TextDisplay(context);
        text.setVisible(false);
        
        // The following 2 statements create Pairs of Display->DoubleProperty objects
        // which are used to adjust the display x coordinate during animations.
        retVal.addAll(Arrays.asList(
            new Pair<>(exp, new SimpleDoubleProperty()), 
            new Pair<>(text, new SimpleDoubleProperty())));
        // Adds a listener to adjust the x coordinate value described above.
        retVal.stream().forEach(
            pair -> pair.getValue().addListener(
                (v,o,n) -> pair.getKey().relocate(n.doubleValue(), PANE_Y)));
        
        // Delays the addition of the toggle listener which actually invokes
        // the selectView() method to change views.
        Platform.runLater(() -> {
            InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(InputViewArea.this);
            
            iw.getInputBar().selectedToggleProperty().addListener((v,o,n) -> {
                Optional<ViewType> vt = registeredViews.keySet()
                    .stream()
                    .filter(k -> k.toString().equalsIgnoreCase(n))
                    .findFirst();
                if(vt.isPresent()) {
                    selectView(vt.get());
                }
            });
        });
        
        addTogglePlacementHandler(retVal);
        
        return retVal;
    }
    
    /**
     * Adds the handler which sizes and positions the view area upon change of the 
     * parent's dimensions.
     * 
     * @param panes     a list of {@link Pair}s of Panes and their x position properties.
     */
    private void addTogglePlacementHandler(List<Pair<Group, DoubleProperty>> panes) {
        positioner = (v,o,b) -> {
            double h = b.getHeight() - window.getTitleBar().getHeight() - window.getInputBar().getHeight() - window.getStatusBar().getHeight();
            
            clip.setWidth(b.getWidth() - 2);
            clip.setHeight(h);
        };
        
        Platform.runLater(() -> {
            window = (InputWindow)WindowService.getInstance().windowFor(InputViewArea.this);
            window.boundsInLocalProperty().addListener(positioner);
            windowAvailableProperty.set(window);
        });
        
    }
    
    /**
     * Returns the height of the view which is currently
     * selected.
     */
    @Override
    public double computeHeight() {
        if(selectedView == null) return 0;
        return ((Resizable)selectedView).computeHeight();
    }
    
}
