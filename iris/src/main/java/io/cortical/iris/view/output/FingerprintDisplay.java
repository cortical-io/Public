package io.cortical.iris.view.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.bushe.swing.event.EventTopicSubscriber;

import io.cortical.fx.webstyle.Impression;
import io.cortical.fx.webstyle.Impression.ImpressionTooltip;
import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.iris.RetinaClientFactory;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.message.ServerResponse;
import io.cortical.iris.ui.util.DragAssistant;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.window.Window;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Term;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Pair;

/**
 * Shows the grid widget (called an {@link Impression} in this implementation),
 * which displays little dots in sorted order according to the topology (coordinates)
 * of the {@link Fingerprint} object.
 * 
 * This display (or rather the underlying {@link Impression} object),
 * "snapshots" the Fingerprint's {@link Impression} to an
 * image for animation performance. In order for a fingerprint to show up
 * it must be snapshotted after being made visible. Updates to this display
 * can happen when another tab is visible, requiring us to queue the updates
 * for later when the display is visible so that the addition and removal
 * of fingerprints is properly shown.
 * 
 * @author cogmission
 *
 */
public class FingerprintDisplay extends LabelledRadiusPane implements View {
    
    public final ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();
    
    private Impression impression;
    
    private Map<Color, int[]> sdrs = new LinkedHashMap<>();
    
    /** The queue of delayed Fingerprint updates. */
    private List<Runnable> commandQueue = new ArrayList<>();
    
    private Region backPanel;
    
    private EventTopicSubscriber<Payload> sub;
    
    
    /**
     * Constructs a new {@code FingerprintDisplay} specifying the 
     * tab background type.
     * 
     * @param label     the tab text
     * @param spec      the color background
     */
    public FingerprintDisplay(String label, NewBG spec) {
        super(label, spec);
        
        setVisible(true);
        setUserData(ViewType.FINGERPRINT);
        setManaged(false);
        
        impression = createAndConfigureImpression();
        
        DragAssistant.configureDragHandler(impression);
        
        getChildren().addAll(impression, createInstructionLabel());
        
        Platform.runLater(() -> {
            runDelayedDisplayConfig();
        });
    }
    
    /**
     * Implemented by {@code View} subclasses to handle an error
     * 
     * @param	context		the error information container
     */
    @Override
    public void processRequestError(RequestErrorContext context) {}
    
    /**
     * Clears all SDRs added to this display.
     */
    public void clearSDRs() {
        impression.clearSDRs();
        sdrs.clear();
    }
    
    /**
     * Sets the specified SDR on this display.
     * @param sdr   the int[] of positions to add.
     */
    public void setSDR(int[] sdr, Color c) {
        if(!isVisible()) {
            commandQueue.add(() -> setSDR(sdr, c));
            return;
        }
        impression.setSDR(sdr, c);
        sdrs.put(c, sdr);
    }
    
    /**
     * Adds an SDR in the color specified.
     * @param sdr   the "positions" to be added to the display
     * @param c     the color of the position cells
     */
    public void addSDR(int[] sdr, Color c) {
        if(!isVisible()) {
            commandQueue.add(() -> addSDR(sdr, c));
            return;
        }
        impression.addSDR(sdr, c);
        sdrs.put(c, sdr);
    }
    
    /**
     * Removes the SDR which matches both the array and color
     * @param sdr
     * @param color
     */
    public void removeSDRByColor(int[] sdr, Color color) {
        if(!isVisible()) {
            commandQueue.add(() -> removeSDRByColor(sdr, color));
            return;
        }
        
        sdrs.remove(color);
        
        impression.removeSDR(sdr);
        
        for(Color c : sdrs.keySet()) {
            impression.addSDR(sdrs.get(c), c);
        }
    }
    
    /**
     * Removes the specified SDR from the display
     * @param sdr
     */
    public void removeSDR(int[] sdr) {
        if(!isVisible()) {
            commandQueue.add(() -> removeSDR(sdr));
            return;
        }
        
        Color toRemoveKey = null;
        for(Color c : sdrs.keySet()) {
            if(Arrays.equals(sdrs.get(c), sdr)) {
                toRemoveKey = c;
                break;
            }
        }
        
        if(toRemoveKey != null) {
            sdrs.remove(toRemoveKey);
        }
        
        impression.removeSDR(sdr);
        for(Color c : sdrs.keySet()) {
            impression.addSDR(sdrs.get(c), c);
        }
    }
    
    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        clearSDRs();
    }
    
    /**
     * Returns a flag indicating whether this {@code FingerprintDisplay} is currently
     * populated with a {@link Fingerprint} or not.
     * @return
     */
    public boolean isPopulated() {
        return !impression.isClear();
    }
    
    /**
     * Returns the property which is updated with a new {@link Payload} upon
     * message model creation.
     * @return  the message property
     */
    @Override
    public ObjectProperty<Payload> messageProperty() {
        return messageProperty;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Region getOrCreateBackPanel() {
        if(backPanel == null) {
            backPanel = new FingerprintBackPanel(10).getScroll();
        }
        
        return backPanel;
    }
    
    /**
     * Returns the handler which is passed to the {@link Impression} as action to
     * invoke to populate the {@code Impression}'s tooltip. 
     * @return
     */
    private BiConsumer<ImpressionTooltip, Pair<String, int[]>> getImpressionTooltipHandler() {
        return (l, p) -> {
            UUID uuid = WindowService.getInstance().windowFor(this).getWindowID();
            
            int[] coords = p.getValue();
            int position = coords[0] * 128 + coords[1];
            
            EventBus.get().subscribeTo(Pattern.compile(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_RESPONSE.subj() + uuid.toString()), sub = (topic, payload) -> {
                Platform.runLater(() -> {
                    populateTooltip((ServerResponse)payload, l, p);
                    EventBus.get().unsubscribe(Pattern.compile(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_RESPONSE.subj() + uuid.toString()), sub);
                });
             });
            
            ServerRequest request = new ServerRequest();
            Window w = WindowService.getInstance().windowFor(this);
            FullClient currentClient = WindowService.getInstance().clientRetinaFor(w);
            String retinaName = RetinaClientFactory.getRetinaName(currentClient);
            request.setRetinaClient(currentClient);
            request.setExtendedClient(RetinaClientFactory.getExtendedClient(retinaName));
            request.setRetinaLanguage(retinaName);
            request.setPosition(position);
            EventBus.get().broadcast(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_REQUEST.subj() + uuid.toString(), request);
        };
    }
    
    /**
     * Creates and configures the {@link Impression} and layout details.
     * @return
     */
    private Impression createAndConfigureImpression() {
        impression = new Impression();
        impression.setTooltipDecorator(getImpressionTooltipHandler());
        impression.layoutXProperty().bind(widthProperty().divide(2).subtract(impression.prefWidthProperty().doubleValue() / 2.0));
        impression.layoutYProperty().bind(labelHeightProperty().add(10));
        
        return impression;
    }
    
    /**
     * Creates and returns the label describing how to use the similar terms lookup tooltip
     * @return
     */
    private Label createInstructionLabel() {
        String text = "Hold down \"L\" mouse button to show similar terms for position.";
        Label l = new Label(text);
        // Default location to reduce jitter when window is becoming visible
        l.relocate(114.18, 422.0);
        Font f = Font.font(l.getFont().getFamily(), FontWeight.THIN, FontPosture.ITALIC, 10);
        Text positioner = new Text(text);
        positioner.setFont(f);
        Bounds b = positioner.getLayoutBounds();
        l.resize(b.getWidth(), b.getHeight());
        l.setFont(f);
        l.setManaged(false);
        
        // Dynamically layout label during window size changes
        impression.layoutXProperty().addListener((v,o,n) -> {
            l.relocate((n.doubleValue() + (impression.getWidth() / 2) - (l.getWidth() / 2)), impression.getLayoutY() - l.getHeight() - 15);
        });
        
        return l;
    }
    
    /**
     *  Formats a list of terms found in the ServerResponse specified,
     *  as a list of comma separated strings - then sets the formatted
     *  values on the {@link ImpressionTooltip} specified.
     * @param payload       the {@link ServerResponse}
     * @param l             the {@link ImpressionTooltip} which delegates to the 
     *                      the displayed tooltip
     * @param pair          title text and array containing positions.
     */
    private void populateTooltip(ServerResponse payload, ImpressionTooltip l, Pair<String, int[]> pair) {
        List<Term> list = ((ServerResponse)payload).getTerms();
        StringBuilder text = new StringBuilder();
        StringBuilder line = new StringBuilder();
        Text sizer = new Text();
        for(Term t : list) {
            sizer.setText(line.toString() + t.getTerm());
            if(sizer.getLayoutBounds().getWidth() > 200) {
                text.append(line.toString()).append(",\n");
                line.setLength(0);
            }
            line.append(line.length() > 0 ? ", " + t.getTerm() : t.getTerm());
        }
        if(line.length() > 0) {
            text.append(line.toString());
        }
        if(text.toString().endsWith(",")) {
            text.setLength(text.length() - 1);
        }else if(text.toString().endsWith(",\n")) {
            text.setLength(text.length() - 2);
        }
        
        l.setTitle(pair.getKey() + " [" + (pair.getValue()[0] * 128 + pair.getValue()[1]) + "]");
        l.setContent(text.toString());
    }
    
    /**
     * Called during construction to queue the configuration of some contents
     * which require other dependencies to be fully constructed (containment
     * hierarchy established).
     */
    @SuppressWarnings("unchecked")
    private void runDelayedDisplayConfig() {
        UUID uuid = WindowService.getInstance().windowFor(this).getWindowID();
        
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.FINGERPRINT_DISPLAY_ADD.subj() + uuid.toString()), (topic, p) -> {
            Pair<Term, Color> pair = (Pair<Term, Color>)p.getPayload();
            addSDR(pair.getKey().getFingerprint().getPositions(), pair.getValue());
        });
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.FINGERPRINT_DISPLAY_REMOVE.subj() + uuid.toString()), (topic, p) -> {
            Pair<Fingerprint, Color> pair = (Pair<Fingerprint, Color>)p.getPayload();
            
            // If the term in the payload's pair has no positions (if the user 
            // invoked an action before the lookup communication to the server was complete...)
            if(pair.getKey().getPositions() == null) return;
            
            removeSDR(pair.getKey().getPositions());
        });
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.FINGERPRINT_DISPLAY_REMOVE_BY_COLOR.subj() + uuid.toString()), (topic, p) -> {
            Pair<Fingerprint, Color> pair = (Pair<Fingerprint, Color>)p.getPayload();
            
            // If the term in the payload's pair has no positions (if the user 
            // invoked an action before the lookup communication to the server was complete...)
            if(pair.getKey().getPositions() == null) return;
            
            removeSDRByColor(pair.getKey().getPositions(), pair.getValue());
        });
        
        // Adds the command queue handler invoked when the screen becomes visible.
        addVisiblityHandler();
        
        //Adds the flip over info about the FingerprintDisplay
        addInfoButton(); 
    }
    
    /**
     * Adds the visibility property handler which checks the command queue
     * for commands to run when the screen becomes visible.
     */
    private void addVisiblityHandler() {
        // Add delay so that the command updates don't share time with the transition
        // animation.
        visibleProperty().addListener((v,o,n) -> {
            if(n && !commandQueue.isEmpty()) {
                (new Thread(() -> {
                    try { Thread.sleep(500); }catch(Exception e) { e.printStackTrace(); }
                    
                    Platform.runLater(() -> {
                        for(Runnable r : commandQueue) {
                            r.run();
                        }
                        commandQueue.clear();
                    });
                })).start();
            }
        });
    }
    
    private void addInfoButton() {
        Window w = WindowService.getInstance().windowFor(this);
        ObjectProperty<Point2D> infoLoc = new SimpleObjectProperty<>();
        w.layoutBoundsProperty().addListener((v,o,n) -> {
            infoLoc.set(new Point2D(n.getWidth() - 35, 5));
        });
        getChildren().add(WindowService.createInfoButton(w, this, infoLoc));
    }
}
