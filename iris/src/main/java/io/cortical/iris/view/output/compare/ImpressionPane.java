package io.cortical.iris.view.output.compare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bushe.swing.event.EventTopicSubscriber;

import io.cortical.fx.webstyle.Impression;
import io.cortical.fx.webstyle.Impression.ImpressionTooltip;
import io.cortical.iris.RetinaClientFactory;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.message.ServerResponse;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.Window;
import io.cortical.iris.window.WindowGroup;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.model.Term;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Pair;

public class ImpressionPane extends StackPane {
    private enum CombineConfig { BOTH, PRIMARY, SECONDARY };
    
    private static Color PRIMARY_COLOR = Color.rgb(26, 73, 94);
    private static Color SECONDARY_COLOR = Color.rgb(47, 189, 252);
    
    private Impression impression;
    
    private Label label;
    
    private String bgColorString;
    private String bdrColorString;
    private String sdrColor;
    private String title;
    
    private WindowGroup windowGroup;
    
    private EventTopicSubscriber<Payload> sub, sub2;
    
    private Set<Integer> primarySDR;
    private Set<Integer> compareSDR;
    
    
    /**
     * Constructs a new {@code ImpressionPane} with the specified colors
     * to show the 3-color comparison fingerprints.
     * 
     * @param titleStr      The name or title of the containing {@code Window}
     * @param bgColor       background color
     * @param brdrColor     border color
     * @param sdrColor      color of the displayed positions
     */
    public ImpressionPane(String titleStr, String bgColor, String brdrColor, String sdrColor) {
        this.title = titleStr;
        
        VBox box = new VBox(2);
        
        bgColorString = bgColor;
        bdrColorString = brdrColor;
        this.sdrColor = sdrColor;
        
        impression = new Impression();
        impression.setPopupBackground(bgColorString);
        impression.setPopupBorder(bdrColorString);
        impression.setFocusTraversable(false);
        impression.setTooltipDecorator(
            getImpressionTooltipHandler(
                titleStr.indexOf(" ") != -1 ? titleStr.replace(" ", "") : titleStr));
        
        label = new Label(title);
        label.setFont(Font.font(20));
        label.setTextFill(Color.WHITE);            
        label.setPrefSize(100, 20);
        label.resize(100, 30);
        label.setAlignment(Pos.CENTER);
        label.prefWidthProperty().bind(impression.widthProperty());
        
        box.getChildren().addAll(impression, label);
        
        getChildren().add(box);
    }
    
    /**
     * Sets the {@link WindowGroup} used to retrieve the associated FullClient(s)
     * which is mapped to the specified Window.
     * 
     * @param n
     */
    public void setCompareContext(WindowGroup group) {
        this.windowGroup = group;
    }

    /**
     * Composes the Set of primary positions and calls {@link Impression#setSDR}
     * @param sdr   the array of positions
     */
    public void setPrimarySDR(int[] sdr) {
        this.primarySDR = Arrays.stream(sdr).boxed().collect(Collectors.toSet());
        Color c = WindowService.getInstance().compareColorFollowsWindowFor((InputWindow)windowGroup.getPrimaryWindow()) ?
            (Color)((InputWindow)windowGroup.getPrimaryWindow()).getTitleBar().getColorIDTab().getPaint() : PRIMARY_COLOR;
        getImpression().setSDR(sdr, c);
    }
    
    /**
     * Sets the secondary sdr on the {@link Impression#addSDR(int[], Color)}
     * @param sdr   the array of secondary positions
     */
    public void setSecondarySDR(int[] sdr) {
        Color c = WindowService.getInstance().compareColorFollowsWindowFor((InputWindow)windowGroup.getSecondaryWindow()) ?
            (Color)((InputWindow)windowGroup.getSecondaryWindow()).getTitleBar().getColorIDTab().getPaint() : SECONDARY_COLOR;
        getImpression().addSDR(sdr, c);
    }
    
    /**
     * Sets the comparison sdr which is a combination of primary
     * and secondary sdrs, and calls {@link Impression#addSDR(int[], Color))
     * @param sdr      the array of combination positions 
     */
    public void setCompareSDR(int[] sdr) {
        this.compareSDR = Arrays.stream(sdr).boxed().collect(Collectors.toSet());
        getImpression().addSDR(sdr, Color.web(getSdrColor()));
    }
    
    /**
     * Returns the {@link Impression} grid
     * @return the Impression grid
     */
    public Impression getImpression() {
        return impression;
    }
    
    /**
     * Returns the Label containing the title
     * @return  the Label containing the title
     */
    public Label getTitleLabel() {
        return label;
    }
    
    /**
     * Sets the label title
     * @param title     the label to set
     */
    public void setTitle(String title) {
        this.title = title;
        label.setText(title);
        label.requestLayout();
    }
    
    /**
     * @return the sdrColor
     */
    public String getSdrColor() {
        return sdrColor;
    }

    /**
     * Sets the color of the tooltip background
     * @param colorString   the string contain background color info
     */
    public void setPopupBackground(String colorString) {
        bgColorString = colorString;
        impression.setPopupBackground(bgColorString);
    }
    
    /**
     * Sets the color of the tooltip border
     * @param colorString   the string contain background color info
     */
    public void setPopupBorder(String colorString) {
        bdrColorString = colorString;
        impression.setPopupBorder(bdrColorString);
    }
    
    /**
     * Returns the handler which is passed to the {@link Impression} as action to
     * invoke to populate the {@code Impression}'s tooltip. 
     * @return
     */
    @SuppressWarnings("unchecked")
    private BiConsumer<ImpressionTooltip, Pair<String, int[]>> getImpressionTooltipHandler(String title) {
        return (l, p) -> {
            int[] coords = p.getValue();
            int position = coords[0] * 128 + coords[1];
            
            // Retrieves the comparison terms and invokes populate with them
            if(title.indexOf("Combine") != -1 && compareSDR.contains(position)) {
                AtomicInteger count = new AtomicInteger(2);
                
                Window[] windows = { windowGroup.primaryWindowProperty().get(), windowGroup.secondaryWindowProperty().get() };
                EventTopicSubscriber<Payload>[] subs = new EventTopicSubscriber[] { sub, sub2 };
                List<Term> termList = new ArrayList<>();
                
                // Execute twice. Once for primary terms and the other for secondary
                for(int i = 0;i < 2;i++) {
                    UUID windowID = windows[i].getWindowID();
                    
                    final int index = i;
                    
                    EventBus.get().subscribeTo(
                        Pattern.compile(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_RESPONSE.subj() + windowID), 
                        subs[i] = (topic, payload) -> {
                            List<String> strTemp = termList.stream().map(t -> t.getTerm()).collect(Collectors.toList());
                            List<Term> temp = ((ServerResponse)payload)
                                .getTerms()
                                .stream()
                                .filter(t -> !strTemp.contains(t.getTerm()))
                                .collect(Collectors.toList());
      
                            termList.addAll(temp);
                            
                            // Only populate the tooltip once <em>both</em> primary and secondary
                            // queries have been run and their data collected.
                            if(count.decrementAndGet() == 0) {
                                Platform.runLater(() -> {
                                    (((ServerResponse)payload)).setTerms(termList);
                                    populateTooltip((ServerResponse)payload, l, p, CombineConfig.BOTH);
                                });
                            }
                            EventBus.get().unsubscribe(Pattern.compile(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_RESPONSE.subj() + windowID), subs[index]);
                        }
                    );
                    
                    ServerRequest request = new ServerRequest();
                    FullClient currentClient = WindowService.getInstance().clientRetinaFor(windows[i]);
                    request.setRetinaClient(currentClient);
                    request.setExtendedClient(RetinaClientFactory.getExtendedClient(RetinaClientFactory.getRetinaName(currentClient)));
                    request.setRetinaLanguage(RetinaClientFactory.getRetinaName(currentClient));
                    request.setPosition(position);
                    EventBus.get().broadcast(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_REQUEST.subj() + windowID, request);
                }
            } else { // Retrieves either the primary OR secondary terms
                Window w = null;
                CombineConfig c;
                if(primarySDR == null) {
                    w =  windowGroup.getSecondaryWindow();
                    c = CombineConfig.SECONDARY;
                }else{
                    w = primarySDR.contains(position) ? windowGroup.getPrimaryWindow() : windowGroup.getSecondaryWindow();
                    c = primarySDR.contains(position) ? CombineConfig.PRIMARY : CombineConfig.SECONDARY;
                }
                
                EventBus.get().subscribeTo(Pattern.compile(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_RESPONSE.subj() + title), sub = (topic, payload) -> {
                    Platform.runLater(() -> {
                        populateTooltip((ServerResponse)payload, l, p, c);
                        EventBus.get().unsubscribe(Pattern.compile(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_RESPONSE.subj() + title), sub);
                    });
                });
                
                ServerRequest request = new ServerRequest();
                
                FullClient currentClient = WindowService.getInstance().clientRetinaFor(w);
                request.setRetinaClient(currentClient);
                request.setExtendedClient(RetinaClientFactory.getExtendedClient(RetinaClientFactory.getRetinaName(currentClient)));
                request.setRetinaLanguage(RetinaClientFactory.getRetinaName(currentClient));
                request.setPosition(position);
                EventBus.get().broadcast(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_REQUEST.subj() + title, request);
            }
        };
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
    private void populateTooltip(ServerResponse payload, ImpressionTooltip l, Pair<String, int[]> pair, CombineConfig config) {
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
        l.setLegend(prepareLegend(config));
        l.setContent(text.toString());
    }
    
    /**
     * Returns a compare oriented legend either combining the window id colors
     * of both the primary and secondary windows, or one or the other.
     * @return
     */
    private List<Pair<Color, String>> prepareLegend(CombineConfig c) {
        if(title.equals("Combined") && c == CombineConfig.BOTH) {
            return IntStream.rangeClosed(0, 1)
                .mapToObj(i ->  {
                    Pair<Color, String> p = i == 0 ? 
                        new Pair<Color, String>(
                            WindowService.getInstance().compareColorFollowsWindowFor((InputWindow)windowGroup.getPrimaryWindow()) ?
                                (Color)((InputWindow)windowGroup.getPrimaryWindow()).getTitleBar().getColorIDTab().getPaint() : PRIMARY_COLOR,
                            windowGroup.getPrimaryWindow().getTitleBar().getTitleField().getText()) :
                        new Pair<Color, String>(
                            WindowService.getInstance().compareColorFollowsWindowFor((InputWindow)windowGroup.getSecondaryWindow()) ?
                                (Color)((InputWindow)windowGroup.getSecondaryWindow()).getTitleBar().getColorIDTab().getPaint() : SECONDARY_COLOR,
                            windowGroup.getSecondaryWindow().getTitleBar().getTitleField().getText());
                    return p;
                })
                .collect(Collectors.toList());
        } else {
            if(c == CombineConfig.PRIMARY) {
                return IntStream.range(0, 1)
                    .mapToObj(i ->  new Pair<Color, String>(
                        WindowService.getInstance().compareColorFollowsWindowFor((InputWindow)windowGroup.getPrimaryWindow()) ?
                             (Color)((InputWindow)windowGroup.getPrimaryWindow()).getTitleBar().getColorIDTab().getPaint() : PRIMARY_COLOR,
                        windowGroup.getPrimaryWindow().getTitleBar().getTitleField().getText()))
                    .collect(Collectors.toList());
            } else {
                return IntStream.range(0, 1)
                    .mapToObj(i ->  new Pair<Color, String>(
                        WindowService.getInstance().compareColorFollowsWindowFor((InputWindow)windowGroup.getSecondaryWindow()) ?
                            (Color)((InputWindow)windowGroup.getSecondaryWindow()).getTitleBar().getColorIDTab().getPaint() : SECONDARY_COLOR,
                        windowGroup.getSecondaryWindow().getTitleBar().getTitleField().getText()))
                    .collect(Collectors.toList());
            }
        }
    }
}
