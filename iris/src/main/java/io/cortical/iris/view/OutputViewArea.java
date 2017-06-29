package io.cortical.iris.view;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bushe.swing.event.EventTopicSubscriber;

import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.fx.webstyle.SegmentedButtonBar;
import io.cortical.iris.CachingClientWrapper;
import io.cortical.iris.RetinaClientFactory;
import io.cortical.iris.ServerMessageService;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.CompareRequest;
import io.cortical.iris.message.CompareResponse;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.MergeOrPassthruValve;
import io.cortical.iris.message.MergeSupplier;
import io.cortical.iris.message.Message;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.message.ServerResponse;
import io.cortical.iris.persistence.ConfigHandler;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.Overlay;
import io.cortical.iris.ui.custom.widget.WindowTitlePane;
import io.cortical.iris.ui.custom.widget.WindowTitlePane.MaxResultsType;
import io.cortical.iris.view.output.ClassifyDisplay;
import io.cortical.iris.view.output.CompareDisplay;
import io.cortical.iris.view.output.CompareDisplay.Comparison;
import io.cortical.iris.view.output.ContextDisplay;
import io.cortical.iris.view.output.FingerprintDisplay;
import io.cortical.iris.view.output.KeywordsDisplay;
import io.cortical.iris.view.output.SimilarTermsDisplay;
import io.cortical.iris.view.output.SlicesDisplay;
import io.cortical.iris.view.output.TokensDisplay;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.iris.window.WindowController;
import io.cortical.iris.window.WindowGroup;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.core.PosTag;
import io.cortical.retina.core.PosType;
import io.cortical.retina.model.Context;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Metric;
import io.cortical.retina.model.Term;
import io.cortical.retina.model.Text;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import javafx.util.Pair;
import rx.Observable;
import rx.Observer;
import rx.Subscription;


/**
 * The central view area of an {@link OutputWindow} containing the dynamic
 * display of api calls given a user defined input arising from one or more
 * selected {@link InputWindow}s.
 * 
 * @author cogmission
 * @see InputViewArea
 * @see ViewArea
 * @see OutputWindow
 * @see InputWindow
 * @see Window
 */
public class OutputViewArea extends ViewArea {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private transient OutputWindow window;
    
    private transient HBox segmentedButtonRow;
    
    private transient List<Pair<Pane, DoubleProperty>> termPairs;
    private transient List<Pair<Pane, DoubleProperty>> allPairs;
    @SuppressWarnings("unused")
    private transient List<Pair<Pane, DoubleProperty>> selectedPairs;
    private transient List<UUID> inputWindowConnections;
    
    private transient ServerRequest lastRequest;
    
    private transient Map<ViewType, Pair<Pane, DoubleProperty>> typePairs = new HashMap<>();
    private transient Map<HBox, ToggleGroup> toggleGroups = new HashMap<>();
    private transient Map<ViewType, ServerResponse> cachedResponseForTypes = new HashMap<>();
    
    
    private transient ViewType lastSelectedViewType = ViewType.FINGERPRINT;
    
    private transient HBox compoundLabel;
    
    private transient ChangeListener<? super Bounds> positioner;
    
    private transient EventTopicSubscriber<Payload> inputViewChangeHandler;
    private transient EventTopicSubscriber<Payload> inputViewClearedHandler;
    private transient EventTopicSubscriber<Payload> secondaryInputViewChangeHandler;
    private transient EventTopicSubscriber<Payload> secondaryInputViewClearedHandler;
    private transient EventTopicSubscriber<Payload> inputWindowServerRequestSubscriber;
    private transient EventTopicSubscriber<Payload> secondaryInputWindowServerRequestSubscriber;
    private transient EventTopicSubscriber<Payload> secondaryInputWindowServerResponseSubscriber;
    private transient EventTopicSubscriber<Payload> compareResponseSubscriber;
    private transient EventTopicSubscriber<Payload> serverResponseSubscriber;
    private transient EventTopicSubscriber<Payload> reloadFromCacheSubscriber;
    private transient EventTopicSubscriber<Payload> resetViewRequestSubscriber;
    private transient EventTopicSubscriber<Payload> newExpressionStateHandler;
    private transient EventTopicSubscriber<Payload> resendLastQueryHandler;
    
    private transient HashSet<EventTopicSubscriber<Payload>> isSubscribed = new HashSet<>();
    
    private static final int TERM_BAR_IDX = 0;
    private static final int TEXT_BAR_IDX = 1;
    private static final int CACHED_POPULATION_DELAY = 1200;
    private transient Map<Integer, HBox> displayBars = new HashMap<>();
    private transient int selectedIndex = TERM_BAR_IDX;
    private transient AnchorPane viewAnchor;
    
    private transient FingerprintDisplay fpDisplay;
    private transient SimilarTermsDisplay simTermsDisplay;
    private transient KeywordsDisplay keywordsDisplay;
    private transient TokensDisplay tokensDisplay;
    private transient SlicesDisplay slicesDisplay;
    private transient CompareDisplay compareDisplay;
    private transient ClassifyDisplay classifyDisplay;
    private transient ContextDisplay contextDisplay;
    
    private transient WindowTitlePane windowTitlePane;
    
    private transient ChangeListener<String> inputWindowTitleHandler;
    private transient ChangeListener<String> inputWindow2TitleHandler;
    
    private transient Button exprSend;
    private transient Button textSend;
    private transient Button termQuestionButton;
    private transient Button textQuestionButton;
    private transient ProgressBar exprProgress;
    private transient ProgressBar textProgress;
    private transient BooleanProperty progressProperty = new SimpleBooleanProperty(false);
    private transient BooleanProperty dirtyProperty = new SimpleBooleanProperty(false);
    
    private transient Map<ViewType, ToggleButton> compareButtonMap = new HashMap<>();
    private transient boolean compareEnabled;
    
    private transient boolean isNewExpression;
    
    @SuppressWarnings("rawtypes")
    private transient MergeOrPassthruValve<ServerResponse, Observable, ServerResponse> mergeValve;
    private transient Subscription mergeSubscription;
    
    private static ConfigHandler<? super WindowConfig> configHandler;    
    
    
    ////////////////////////
    //  Animation vars    //
    ////////////////////////
    // Defines the viewable area and cuts out anything outside of it.
    private transient Rectangle clip = new Rectangle(1, 1, 100, 400);
    private transient Timeline tl = new Timeline();
    private transient Timeline auxilliaryTL = new Timeline();
    private transient Interpolator interpolator = Interpolator.SPLINE(0.5, 0.1, 0.1, 0.5);
    
    // The static Y location of the LabelledRadiusPane view panes
    private static final double PANE_Y = 69;
    
    // Flag to indicate that the target SegmentedButtonBar has never been laid out
    // therefore it doesn't have a location or dimensions and we should "nudge" the
    // layout to re-execute the layout calculations 2 times on the first layout.
    private transient boolean firstOutputTransition = true;
    
    private transient BooleanProperty inputAnimationStatusProperty = new SimpleBooleanProperty(false);
    private transient BooleanProperty outputAnimationStatusProperty = new SimpleBooleanProperty(false);
    
    private transient Toggle prevSelectedToggle;
    
    private transient FadeTransition fadeIn = new FadeTransition(
            Duration.millis(1000)
        );
    private transient FadeTransition fadeOut = new FadeTransition(
            Duration.millis(1000)
        );
    private transient ChangeListener<Boolean> compareInfoButtonListener;
    
    
    
    
    /**
     * Constructs a new {@code OutputViewArea}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OutputViewArea(OutputWindow w) {
        this.window = w;
        
        setFocusTraversable(false);
        
        viewAnchor = new AnchorPane();
        getChildren().add(viewAnchor);
        
        displayBars = createButtonBars();
        segmentedButtonRow = displayBars.get(TERM_BAR_IDX);
        viewAnchor.getChildren().addAll(segmentedButtonRow);
        AnchorPane.setTopAnchor(segmentedButtonRow, 5d);
        
        createToggleDisplayVars();
        addViewDirtyPropertyHandlers();
        addSendButtonDirtyPropertyHandler();
        
        selectedPairs = termPairs;
        allPairs = getAllToggleDisplays();
        
        createViewTypePairMapping();
        
        addTogglePlacementHandler(allPairs);
        
        getChildren().add(createPrimaryInputLabel());
        
        allPairs.stream().forEach(pane -> getChildren().add(pane.getKey()));
        
        clip.setVisible(true);
        setClip(clip);
        
        registerViews();
        
        addPrimaryInputChangeHandler();
        addSecondaryInputChangeHandler();
        addSwapDetectionHandler();
        addPrimaryAdoptionHandler();
        
        inputWindowConnections = new ArrayList<>();
        
        viewAnchor.prefHeightProperty().bind(prefHeightProperty());
        viewAnchor.prefWidthProperty().bind(prefWidthProperty());
        
        // The pattern which invokes the progress meter widget
        EventBus.get().subscribeTo(ViewArea.PROGRESS_PATTERN, getProgressSubscriber());
        
        // Merge Valve is the construct overseeing the compare behavior
        mergeValve = prepareValve();
        mergeValve.setPrimaryObserver(getPrimaryObserver());
        mergeValve.setSecondaryObserver(getSecondaryObserver());
        mergeValve.setMergeObserver((Observer)getMergeObserver());
        mergeValve.disableMerge();
        
        Platform.runLater(() -> { 
            windowTitlePane = WindowService.getInstance().windowTitleFor(this.window); 
            windowTitlePane.dirtyProperty().addListener((v,o,n) -> dirtyProperty.set(n));
            selectedView = registeredViews.get(lastSelectedViewType);
            setTemporaryDisabledStyle();
        });
        
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setCycleCount(1);
        fadeIn.setAutoReverse(false);
        
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setCycleCount(1);
        fadeOut.setAutoReverse(false);
    }
    
    /**
     * Gives unused buttons on the nav bar a disabled look temporarily until the
     * associated functionality has been added.
     */
    private void setTemporaryDisabledStyle() {
        //output-navbar-disabled
        SegmentedButtonBar btnBar = (SegmentedButtonBar)displayBars.get(1).getChildren().get(1);
        ToggleButton btn = (ToggleButton)btnBar.getChildren().get(btnBar.getChildren().size() - 1);
        btn.setStyle(
            "-fx-border-radius: 0 4 4 0;" +
            "-fx-background-insets: 0;" +
            "-fx-background-radius: 0 4 4 0;" +
            "-fx-background-color: linear-gradient(to bottom, ivory 0%, -bar-background 100%);" +
            "-fx-background-radius: 0;" +
            "-fx-font-size: 13;" +
            "-fx-padding: 5 12 5 12;" +
            "-fx-text-fill: gray;" +
            "-fx-border-color: -base-border-color;" +
            "-fx-border-width: 1 1 1 0;");
        btn.setOnMouseClicked(e -> {});
    }
    
    /**
     * {@inheritDoc}
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        ViewType type = registeredViews.keySet().stream().filter(k -> registeredViews.get(k) == selectedView).findFirst().orElse(null);
        out.writeObject(type);
        WindowConfig config = WindowService.getInstance().windowConfigProperty().get();
        out.writeBoolean(config.getPrimaryWindow() != null);
        if(config.getPrimaryWindow() != null) {
            InputWindow w = (InputWindow)config.getPrimaryWindow();
            ViewType inViewType = w.getViewArea().getSelectedViewType();
            out.writeObject(inViewType);
            if(inViewType == ViewType.TEXT) {
                Set<PosTag> tags = tokensDisplay.getSelectedPOSTags();
                out.writeObject(tags);
            } else if(inViewType == ViewType.EXPRESSION) {
                List<Integer> ids = contextDisplay.getSelectedContexts();
                out.writeInt(ids.size() == 1 ? ids.get(0) : -1);
                PosType posType = simTermsDisplay.getSelectedPOS();
                out.writeObject(posType);
            }
        }
        LOGGER.debug("WRITE: Selected View: OutputViewType: " + type);
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
        boolean hasPrimaryConnection = in.readBoolean();
        if(hasPrimaryConnection) {
            ViewType primaryViewType = (ViewType)in.readObject();
            config.primaryViewType = primaryViewType;
            if(primaryViewType == ViewType.TEXT) {
                @SuppressWarnings("unchecked")
                Set<PosTag> tags = (Set<PosTag>)in.readObject();
                config.posTags = tags;
            } else if(primaryViewType == ViewType.EXPRESSION) {
                int selectedContext = in.readInt();
                PosType posType = (PosType)in.readObject();
                config.selectedContextID = selectedContext;
                config.posType = posType;
            }
        }
        LOGGER.debug("READ: Selected View: OutputViewType: " + type);
    }
    
    /**
     * Restores the state of a saved window configuration, 
     * into this {@link Window}
     * @param config
     */
    public void configure(WindowConfig config) {
        (new Thread(() -> {
            try{ Thread.sleep(3000); }catch(Exception e) {}
            
            Platform.runLater(() -> {
                selectView(config.selectedViewType);
                selectToggle(config.selectedViewType);
                config.advanceNotificationChain();                
            });
        })).start();
    }
    
    /**
     * Returns the {@link ConfigHandler} instance used to initialize
     * this {@code OutputViewArea}'s context settings following
     * deserialization of an {@link OutputWindow}.
     * @return
     */
    public static final ConfigHandler<? super WindowConfig> getChainHandler() {
        if(configHandler == null) {
            configHandler = config -> {
                LOGGER.debug("Executing OutputViewArea chain of responsibility handler");
                Platform.runLater(() -> {
                    if(config.getPrimaryViewType() == ViewType.EXPRESSION) {
                        if(config.getSelectedContextID() != -1) {
                            OutputWindow thisWindow = (OutputWindow)WindowService.getInstance().windowFor(config.getWindowID());
                            thisWindow.getViewArea().getContextDisplay().selectContext(config.getSelectedContextID());
                            LOGGER.debug("configure: selecting context id = " + config.getSelectedContextID());
                        }
                    } 
                    config.advanceNotificationChain();
                });
            };
        }
        
        return configHandler;
    }
    
    /**
     * The label which shows which {@link InputWindow} is currently connected to the current
     * {@link OutputWindow}
     * @return
     */
    private HBox createPrimaryInputLabel() {
        String text = "No Input Selected";
        Hyperlink t = getFXText(text, false);
        TextFlow l = new TextFlow(t);
        l.setFocusTraversable(false);
        Font f = Font.font(t.getFont().getFamily(), FontWeight.EXTRA_BOLD, FontPosture.ITALIC, 13);
        javafx.scene.text.Text positioner = new javafx.scene.text.Text(text);
        positioner.setFont(f);
        Bounds b = positioner.getLayoutBounds();
        l.resize(b.getWidth(), b.getHeight());
        t.setFont(f);
        t.setTextFill(Color.rgb(49, 109, 160));
        
        l.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            javafx.scene.control.Hyperlink x = (javafx.scene.control.Hyperlink)l.getChildren().stream().filter(tx -> {
                    Point2D p = l.screenToLocal(e.getScreenX(), e.getScreenY());
                    return ((javafx.scene.control.Hyperlink)tx).getBoundsInParent().contains(p);
                })
                .findFirst()
                .orElse(null);
            
           if(x == null) return;
           
           String matchingTitle = x.getText().trim();
           OutputWindow ow = (OutputWindow)WindowService.getInstance().windowFor(this);
           InputWindow primary = (InputWindow)ow.getInputSelector().getWindowGroup().getPrimaryWindow();
           
           if(primary != null && matchingTitle.equals(primary.getTitleField().getText())) {
               primary.flash();
           }
        });
        
        compoundLabel = new HBox(10);
        compoundLabel.getChildren().add(l);
        compoundLabel.setManaged(false);
        compoundLabel.resize(b.getWidth() + 40, b.getHeight() + 8);
        // Default location to reduce jitter when window is becoming visible
        compoundLabel.relocate(232.485, 38.0);
        compoundLabel.setAlignment(Pos.CENTER);
        compoundLabel.getStyleClass().add("output-input-label");
        
        Platform.runLater(() -> {
            OutputWindow ow = (OutputWindow)WindowService.getInstance().windowFor(this);
            ow.getInputSelector().getWindowGroup().primaryWindowProperty().addListener((v,o,n) -> {
                if(n == null) {
                    layoutInputLabel(l, "No Input Selected", f, compoundLabel);
                    return;
                }
                
                InputWindow iw2 = (InputWindow)ow.getInputSelector().getWindowGroup().secondaryWindowProperty().get();
                String titles = n.getTitleField().textProperty().get() + (iw2 != null ? ", " + iw2.getTitleField().textProperty().get() : ""); 
                layoutInputLabel(l, titles, f, compoundLabel);
                
                if(o != null) {
                    removePreviousTitleHandler((InputWindow)o);
                }
                addNewInputTitleHandler(l, f, compoundLabel);
                l.setVisible(true);
            });
            ow.getInputSelector().getWindowGroup().secondaryWindowProperty().addListener((v,o,n) -> {
                InputWindow iw = (InputWindow)ow.getInputSelector().getWindowGroup().primaryWindowProperty().get();
                if(iw == null) {
                    layoutInputLabel(l, "No Input Selected", f, compoundLabel);
                    return;
                }else if(n == null) {
                    layoutInputLabel(l, iw.getTitleField().textProperty().get(), f, compoundLabel);
                    return;
                }
                
                String titles = iw.getTitleField().textProperty().get() + ", " + n.getTitleField().textProperty().get();
                
                layoutInputLabel(l, titles, f, compoundLabel);
                if(o != null) {
                    removePreviousSecondaryTitleHandler((InputWindow)o);
                }
                addNewInputTitleHandler(l, f, compoundLabel);
            });
            
            addNewInputTitleHandler(l, f, compoundLabel);
            
            Window primaryWindow = ow.getInputSelector().getWindowGroup().primaryWindowProperty().get();
            String labelText = primaryWindow != null ? primaryWindow.getTitle() : "No Input Selected";
            layoutInputLabel(l, labelText, f, compoundLabel);
            
            ow.layoutBoundsProperty().addListener((v,o,n) -> {
                layoutInputLabel(l, l.getChildren().stream().map(tx -> ((javafx.scene.control.Hyperlink)tx).getText()).collect(Collectors.joining(" ")), f, compoundLabel);
            });
            
            requestLayout();
        });
        
        return compoundLabel;
    }
    
    /**
     * Removes the obsolete title change handler when a new primary
     * window is selected.
     * @param iw    the old {@link InputWindow}
     */
    private void removePreviousTitleHandler(InputWindow iw) {
        if(inputWindowTitleHandler != null) {
            iw.getTitleField().textProperty().removeListener(inputWindowTitleHandler);
        }
    }
    
    /**
     * Removes the obsolete title change handler when a new primary
     * window is selected.
     * @param iw    the old {@link InputWindow}
     */
    private void removePreviousSecondaryTitleHandler(InputWindow iw) {
        if(inputWindow2TitleHandler != null) {
            iw.getTitleField().textProperty().removeListener(inputWindow2TitleHandler);
        }
    }
    
    /**
     * Adds a new handler to detect window title changes
     * @param l                 the label reflecting input window title changes
     * @param f                 the font of the label
     * @param compoundLabel     the {@link HBox} containing the label and providing
     *                          the radiused background.
     */
    private void addNewInputTitleHandler(
        TextFlow l, Font f, HBox compoundLabel) {
        
        OutputWindow ow = (OutputWindow)WindowService.getInstance().windowFor(this);
        
        InputWindow iw = (InputWindow)ow.getInputSelector().getWindowGroup().primaryWindowProperty().get();
        InputWindow iw2 = (InputWindow)ow.getInputSelector().getWindowGroup().secondaryWindowProperty().get();
        if(iw != null) {
            iw.getTitleField().textProperty().addListener(inputWindowTitleHandler = (v,o,n) -> {
                String titles = n + (iw2 != null ? ", " + iw2.getTitleField().textProperty().get() : ""); 
                layoutInputLabel(l, titles, f, compoundLabel);
            });
            if(iw2 != null) {
                iw2.getTitleField().textProperty().addListener(inputWindow2TitleHandler = (v,o,n) -> {
                    String titles = iw.getTitleField().textProperty().get() + ", " + n; 
                    layoutInputLabel(l, titles, f, compoundLabel);
                });
            }
        } else {
            layoutInputLabel(l, l.getChildren().stream().map(t -> ((javafx.scene.control.Hyperlink)t).getText()).collect(Collectors.joining(" ")), f, compoundLabel);
        }
    }
    
    /**
     * Resizes and lays out the inner label as well as the HBox
     * container holding it.
     * 
     * @param l                 the label to layout
     * @param n                 the new text
     * @param f                 the label font
     * @param compoundLabel     the container to position
     */
    private void layoutInputLabel(TextFlow l, String n, Font f, HBox compoundLabel) {
        int commaIndex = n.indexOf(",");
        l.getChildren().clear();
        if(n.indexOf(",") != -1) {
            l.getChildren().addAll(getFXText(n.substring(0, commaIndex + 1), false), getFXText(n.substring(commaIndex + 1, n.length()), true));
        }else{
            l.getChildren().add(getFXText(n, false));
        }
        javafx.scene.text.Text t = new javafx.scene.text.Text(n);
        t.setFont(f);
        Bounds b3 = t.getLayoutBounds();
        l.resize(b3.getWidth() + 20, b3.getHeight());
        
        double labelWidth = l.getWidth() + 40;
        compoundLabel.resize(labelWidth, b3.getHeight() + 8);
        
        double ix = layoutXProperty().get();
        double labelX = (ix + (getWidth() / 2) - (compoundLabel.getWidth() / 2));
        compoundLabel.relocate(Math.max(15, labelX), segmentedButtonRow.getBoundsInLocal().getMaxY() + 12);
    }
    
    /**
     * Returns a {@link javafx.scene.control.Hyperlink} object styled to desired 
     * requirements.
     * @param s
     * @param leadingSpace      flag indicating whether to prepend a space
     * @return
     */
    private javafx.scene.control.Hyperlink getFXText(String s, boolean leadingSpace) {
        javafx.scene.control.Hyperlink t = new javafx.scene.control.Hyperlink(s.trim());
        t.getStyleClass().add("connected-input-window-title");
        t.setFocusTraversable(false);
        
        t.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
           if(s == null || s.isEmpty()) return;
           
           String matchingTitle = s.indexOf(",") != -1 ? s.substring(0, s.length() - 1).trim() : s.trim();
           OutputWindow ow = (OutputWindow)WindowService.getInstance().windowFor(this);
           
           InputWindow primary = (InputWindow)ow.getInputSelector().getWindowGroup().getPrimaryWindow();
           InputWindow secondary = (InputWindow)ow.getInputSelector().getWindowGroup().getSecondaryWindow();
           
           if(primary != null && matchingTitle.equals(primary.getTitleField().getText())) {
               WindowService.getInstance().flashWindow(primary);
           }else if(secondary != null && matchingTitle.equals(secondary.getTitleField().getText())) {
               WindowService.getInstance().flashWindow(secondary);
           }
        });
        
        return t;
    }
    
    /**
     * Returns a flag indicating whether the compare function can run
     * or not based on number of user defined inputs. (anything over 1)
     * @return  true if compare can be run, false if not
     */
    public boolean compareEnabled() {
        return compareEnabled;
    }

    /**
     * Animates the specified view into position and transitions the previously 
     * shown view out of the viewable area.
     * 
     * @param type  the {@link ViewType} specifying the view to show
     */
    @Override
    public void selectView(ViewType type) {
        if(type == lastSelectedViewType || tl.getStatus() == Animation.Status.RUNNING) return;
        
        runSwitch(tl, type, false);
        
        lastSelectedViewType = type;
        selectedView = registeredViews.get(type);
    }
    
    /**
     * Version of "selectView" which is called internally to change the behavior
     * when the view change results from selection of a different InputView (in which
     * case, the data in all the associated OutputViews must be cleared).
     *  
     * @param type  the {@link ViewType} representing the type of the output view to be selected.
     */
    public void selectViewWithFeedback(ViewType type) {
        if(type == lastSelectedViewType || auxilliaryTL.getStatus() == Animation.Status.RUNNING) {
            resetViews();
        } else {
            runSwitch(auxilliaryTL, type, true);
            lastSelectedViewType = type;
        }
        selectedView = registeredViews.get(type);
     }
    
    /**
     * The common animation logic between {@link #selectView(ViewType)} and
     * {@link #selectViewWithFeedback(ViewType)}. Called from the previously
     * mentioned to begin the animation of output view changes.
     * 
     * @param tl                            the {@link Timeline}
     * @param type                          the in-coming view's type
     * @param requestAnimationFeedBack      specifies whether special end animation task should be 
     *                                      run or not.
     */
    private void runSwitch(Timeline tl, ViewType type, boolean requestAnimationFeedBack) {
        Pane inComing = (Pane)registeredViews.get(type);
        inComing.setVisible(true);
        Pane outGoing = (Pane)registeredViews.get(lastSelectedViewType);
        
        DoubleProperty inProp = typePairs.get(type).getValue();
        DoubleProperty outProp = typePairs.get(lastSelectedViewType).getValue();
        
        double destX = 0;
        inComing.resize(outGoing.getWidth(), outGoing.getHeight());
        if(type.ordinal() < lastSelectedViewType.ordinal()) {
            inComing.relocate(-outGoing.getWidth(), outGoing.getLayoutBounds().getMinY());
            destX = outGoing.getWidth() + 10;
            inProp.bind(outProp.subtract(inComing.getWidth() + 5));
        }else{
            inComing.relocate(clip.getWidth(), outGoing.getLayoutBounds().getMinY());
            destX = -outGoing.getWidth();
            inProp.bind(outProp.add(inComing.getWidth() + 5));
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
                // requestAnimationFeedBack is set by the bus listener for input view changes.
                if(requestAnimationFeedBack) {
                    inputAnimationStatusProperty.set(true);
                    inputAnimationStatusProperty.set(false);
                }
                // Always run output view initializer
                outputAnimationStatusProperty.set(true);
                outputAnimationStatusProperty.set(false);
                
                if(registeredViews.get(type).isFirstLayout()) {
                    OutputWindow w = (OutputWindow)WindowService.getInstance().windowFor(this);
                    w.getController().nudgeLayout(w, 10);
                }
            });
        });
        
        tl.play();
    }
    
    /**
     * Returns the {@link ViewType} of the currently
     * selected view.
     * 
     * @return  the currently selected {@link ViewType}
     */
    public ViewType getSelectedViewType() {
        return lastSelectedViewType;
    }
    
    /**
     * Returns the "ReSend" button specified by the view type
     * passed in. There is a ReSend button for every input view
     * type due to each input view having its own special output view
     * navigation options.
     * 
     * @param viewType  indicates the ReSend button used for the specified view.
     * @return
     */
    public Button getReSendButton(ViewType viewType) {
        switch(viewType) {
            case EXPRESSION: return exprSend; 
            case TEXT: return textSend;
            default: return null;
        }
    }
    
    /**
     * Called by the ServerResponseSubscriber to update the {@link FingerprintDisplay}
     * with the latest {@link Fingerprint}
     * @param model     the Rest API parent object
     * @param r         the {@link ServerResponse}
     */
    public void setFingerprint(Fingerprint model, ServerResponse r) {
        Platform.runLater(() -> {
            fpDisplay.clearSDRs();
            (new Thread() {
                public void run() {
                    try { Thread.sleep(400); } catch(Exception e) { e.printStackTrace(); }
                    
                    Platform.runLater(() -> {
                        Color c = (Color)r.getRequest().getWindow().getTitleBar().getColorIDTab().getPaint();
                        fpDisplay.setSDR(model.getPositions(), c);
                    });
                }
            }).start();
        });
    }
    
    /**
     * Called by the ServerResponseSubscriber to update the {@link SimilarTermsDisplay}
     * with the latest {@link Term}s 
     * @param terms
     */
    public void setSimilarTerms(List<Term> terms) {
        Platform.runLater(() -> {
            simTermsDisplay.clear();
            
            (new Thread() {
                public void run() {
                    try { Thread.sleep(400); } catch(Exception e) { e.printStackTrace(); }
                    
                    Platform.runLater(() -> {
                        for(Term term : terms) {
                            simTermsDisplay.addTerm(term);
                        }
                    });
                }
            }).start();
        });
    }
    
    /**
     * Called by the ServerResponseSubscriber to update the {@link ContextDisplay}
     * with the latest {@link Context}s
     * @param request
     * @param contexts
     */
    public void setContexts(ServerRequest request, List<Context> contexts) {
        System.out.println("OutputViewArea: setContexts(): contexts = " + contexts + "  -  lookup ? " + request.lookupContexts()); 
        if(request.lookupContexts()) {
            Platform.runLater(() -> {
                contextDisplay.setContexts(contexts);
            });
        }
    }
    
    /**
     * Populates the {@link KeywordsDisplay} with the keywords retrieved from 
     * the server.
     * @param r
     */
    public void setKeywords(ServerResponse r) {
        List<String> keyWords = r.getKeywords();
        String text = r.getRequest().getText();
        
        Platform.runLater(() -> {
            keywordsDisplay.reset();
            
            (new Thread() {
                public void run() {
                    try { Thread.sleep(400); } catch(Exception e) { e.printStackTrace(); }
                    
                    Platform.runLater(() -> {
                        Color c = (Color)r.getRequest().getWindow().getTitleBar().getColorIDTab().getPaint();
                        keywordsDisplay.setKeywords(text, keyWords, c);
                    });
                }
            }).start();
        });
    }
    
    public void setSentencesAndTokens(ServerResponse r) {
        System.out.println("received sentences: " + r.getSentences());
        System.out.println("received tokens: " + r.getTokens());
        List<String> sentences = r.getSentences();
        Platform.runLater(() -> {
            (new Thread() {
                public void run() {
                    try { Thread.sleep(400); } catch(Exception e) { e.printStackTrace(); } 
                    
                    Platform.runLater(() -> {
                        tokensDisplay.setSentencesAndTokens(r.getRequest().getText(), sentences, r.getTokens());
                    });
                }
            }).start();
        });
    }
        
    /**
     * Forwards the {@link ServerResponse} payload to the respective {@link SlicesDisplay}
     * screen.
     * @param r
     */
    public void setSlices(ServerResponse r) {
        System.out.println("received slices: " + r.getSlices());
        List<Text> slices = r.getSlices();
        
        Platform.runLater(() -> {
            (new Thread() {
                public void run() {
                    try { Thread.sleep(400); } catch(Exception e) { e.printStackTrace(); }
                    
                    Platform.runLater(() -> {
                        slicesDisplay.setResponseSlices(slices);
                    });
                }
            }).start();
        });
    }
    
    /**
     * Convenience method to "turn or/off" the progress meter widget.
     * 
     * @param uuidOfWindow  the {@link UUID} of the OutputWindow
     * @param b             true if it should be turned on, false if turned off.
     */
    public static void setProgressIndicatorActive(UUID uuidOfWindow, boolean b) {
        if(b) {
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_IN_PROGRESS.subj() + uuidOfWindow, Payload.DUMMY_PAYLOAD);
        }else{
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_HALT_PROGRESS.subj() + uuidOfWindow, Payload.DUMMY_PAYLOAD);
        }
    }
    
    /**
     * Returns the index of the {@link SegmentedButtonBar} used for the specified
     * {@link ViewType}.
     * 
     * @param type      the type for which the SegmentedButtonBar's index is returned.
     * @return  the index into the list of bars for the SegementedButtonBar specified by "type" 
     */
    private int indexForType(ViewType type) {
        switch(type) {
            case EXPRESSION: return TERM_BAR_IDX;
            case TEXT: return TEXT_BAR_IDX;
            
            default: return -1;
        }
    }
    
    /**
     * Executes the logic necessary to change button bar views and their
     * corresponding button bars.
     * @param type  the input view type to which the output views and button
     *              bars correspond.
     */
    public void swapButtonBar(ViewType type) {
//        viewAnchor.getChildren().remove(segmentedButtonRow);
//        segmentedButtonRow = displayBars.get(selectedIndex = indexForType(type));
//        viewAnchor.getChildren().addAll(segmentedButtonRow);
//        AnchorPane.setTopAnchor(segmentedButtonRow, 5d);
        
        HBox outgoing = segmentedButtonRow;
        viewAnchor.getChildren().remove(segmentedButtonRow);
        segmentedButtonRow = displayBars.get(selectedIndex = indexForType(type));
        segmentedButtonRow.setOpacity(0.0);
        viewAnchor.getChildren().addAll(segmentedButtonRow);
        AnchorPane.setTopAnchor(segmentedButtonRow, 5d);
        fadeNode(segmentedButtonRow, outgoing, true, false, 1500);
        
        ToggleGroup group = toggleGroups.get(segmentedButtonRow.getChildren().get(1));
        group.getToggles().get(0).setSelected(true);
        prevSelectedToggle = group.getToggles().get(0);
                
        Platform.runLater(() -> {
            positionSegmentedButtonRow(segmentedButtonRow);
        });
    }
    
    /**
     * Sets the {@link ToggleButton} corresponding to the specified {@link ViewType}
     * as selected. Note, {@link #selectView(ViewType)} doesn't reliably select 
     * the correct toggle button although it will change the view to the view 
     * corresponding to the specified view type - so this method is needed also.
     * 
     * @param type      the {@link ViewType} corresponding to the desired view.
     */
    public void selectToggle(ViewType type) {
        ToggleGroup group = toggleGroups.get(segmentedButtonRow.getChildren().get(1));
        ToggleButton toggleButton = group.getToggles().stream()
            .map(t -> (ToggleButton)t)
            .filter(tb -> tb.getUserData() == type)
            .findFirst()
            .orElse(null);
        
        if(toggleButton != null) {
            toggleButton.setSelected(true);
        }
    }
    
    /**
     * Locate the segmented button row in the center at the correct height
     * @param row   the segmented button row container node
     */
    private void positionSegmentedButtonRow(HBox row) {
        if(segmentedButtonRow.getLayoutBounds().getWidth() == 0) {
            Platform.runLater(() -> {
                positionSegmentedButtonRow(row);
            });
        }
        
        double x = (getWidth() / 2) - (segmentedButtonRow.getLayoutBounds().getWidth() / 2);
        segmentedButtonRow.setLayoutX(x);
        segmentedButtonRow.relocate(x, 5);
    }
    
    /**
     * Creates the special info button for the {@link CompareDisplay}, because the 
     * compare screen cannot be accessed until input selections are enabled.
     * 
     * @param isTextButton
     * @return
     */
    private Button createQuestionButton(boolean isTextButton) {
        Button questionButton = new Button(" ?");
        questionButton.setFocusTraversable(false);
        questionButton.setManaged(false);
        questionButton.getStyleClass().setAll(isTextButton ? "text-question-button" : "question-button");
        questionButton.resize(13, 15);
        questionButton.setOpacity(WindowService.getInstance().compareInfoButtonVisibleProperty().get() ? 1.0 : 0.0);
        questionButton.setOnAction(e -> {
            Platform.runLater(() -> {
                window.setBackPanel(compareDisplay.getOrCreateBackPanel());
                window.flip();
            });
        });
        
        return questionButton;
    }
    
    /**
     * Called upon window destruction.
     */
    public void removeInfoButtonListener() {
        WindowService.getInstance().compareInfoButtonVisibleProperty().removeListener(compareInfoButtonListener);
    }
    
    /**
     * Creates all button bars associated with the different input views,
     * one for each.
     * @return  a mapping of input view indexes to their respective button bars
     */
    public Map<Integer, HBox> createButtonBars() {
        Map<Integer, HBox> bars = new HashMap<>();
        
        exprSend = createSendButton();
        ToggleButton[] toggles = createTermButtons();
        SegmentedButtonBar bar = createSegmentedButtonBar();
        bar.getChildren().addAll(toggles);
        
        HBox termNav = createSegmentedButtonRow(exprSend, bar);
        termNav.getChildren().add(termQuestionButton = createQuestionButton(false));
        termNav.boundsInLocalProperty().addListener((v,o,n) -> {
            termQuestionButton.relocate(n.getMaxX() - 13, 0);
        });
        
        textSend = createSendButton();
        toggles = createTextButtons();
        bar = createSegmentedButtonBar();
        bar.getChildren().addAll(toggles);
        
        HBox textNav = createSegmentedButtonRow(textSend, bar);
        textNav.getChildren().add(textQuestionButton = createQuestionButton(true));
        textNav.boundsInLocalProperty().addListener((v,o,n) -> {
            textQuestionButton.relocate(n.getMaxX() - 84, 0);
        });
        
        WindowService.getInstance().compareInfoButtonVisibleProperty().addListener(compareInfoButtonListener = (v,o,n) -> {
            fadeIn.setDuration(Duration.millis(1000));
            fadeOut.setDuration(Duration.millis(1000));
            if(selectedIndex == TERM_BAR_IDX) {
                if(n) {
                    fadeIn.setNode(termQuestionButton);
                    fadeIn.playFromStart();
                    textQuestionButton.setOpacity(1.0);
                } else {
                    fadeOut.setNode(termQuestionButton);
                    fadeOut.playFromStart();
                    textQuestionButton.setOpacity(0.0);
                }
            } else {
                if(n) {
                    fadeIn.setNode(textQuestionButton);
                    fadeIn.playFromStart();
                    termQuestionButton.setOpacity(1.0);
                } else {
                    fadeOut.setNode(textQuestionButton);
                    fadeOut.playFromStart();
                    termQuestionButton.setOpacity(0.0);
                }
            }
        });
        
        progressProperty.addListener((v,o,n) -> {
            ((SegmentedButtonBar)termNav.getChildren().get(0)).getChildren().remove(0);
            ((SegmentedButtonBar)textNav.getChildren().get(0)).getChildren().remove(0);
            if(n) {
                ((SegmentedButtonBar)termNav.getChildren().get(0)).getChildren().add(0, exprProgress);
                ((SegmentedButtonBar)textNav.getChildren().get(0)).getChildren().add(0, textProgress);
            }else{
                ((SegmentedButtonBar)termNav.getChildren().get(0)).getChildren().add(0, exprSend);
                ((SegmentedButtonBar)textNav.getChildren().get(0)).getChildren().add(0, textSend);
            }
        });
        
        bars.put(TERM_BAR_IDX, termNav);
        bars.put(TEXT_BAR_IDX, textNav);
        
        exprProgress = new ProgressBar();
        exprProgress.prefWidthProperty().bind(exprSend.widthProperty());
        exprProgress.prefHeightProperty().bind(exprSend.heightProperty());
        exprProgress.getStyleClass().addAll("resend-progress");
        
        textProgress = new ProgressBar();
        textProgress.prefWidthProperty().bind(textSend.widthProperty());
        textProgress.prefHeightProperty().bind(textSend.heightProperty());
        textProgress.getStyleClass().addAll("resend-progress");
        
        return bars;
    }
    
    /**
     * Executes a Fade in/out transition on the specified node.
     * 
     * @param n
     * @param altNode
     * @param isIn
     * @param altInSync
     * @param duration
     */
    public void fadeNode(Node n, Node altNode, boolean isIn, boolean altInSync, long duration) {
        fadeIn.setDuration(Duration.millis(duration));
        fadeOut.setDuration(Duration.millis(duration));
        if(isIn) {
            fadeIn.setNode(n);
            fadeIn.playFromStart();
            altNode.setOpacity(altInSync ? 1.0 : 0.0);
        } else {
            fadeOut.setNode(n);
            fadeOut.playFromStart();
            altNode.setOpacity(altInSync ? 0.0 : 1.0);
        }
    }
    
    /**
     * <p>
     * Returns the property which indicates whether the state of this output
     * view is "dirty". This view becomes dirty if any one of its child views
     * become dirty - meaning they have query criteria which has changed, which
     * then indicates a new query must be made to fulfill that criteria. 
     * </p><p>
     * This property however participates (through one single listener), in only one
     * view state change to handle the send button coloring to indicate to the user
     * that a new query must be made to satisfy new query criteria.
     * </p>
     * @return  the dirty property
     */
    public BooleanProperty dirtyProperty() {
        return dirtyProperty;
    }
    
    /**
     * Utility method to execute setup necessary for the resend
     * button.
     * @return  the created resend button
     */
    private Button createSendButton() {
        Button button1 = new Button("Re-Send");
        button1.setFocusTraversable(false);
        button1.getStyleClass().addAll("send");
        button1.setOnAction(e -> {
            Platform.runLater(() -> {
            	if(e.getSource().equals(exprSend)) {
            		isNewExpression = true;
            	}
                resendLastQuery();
            });
        });
        
        return button1;
    }
    
    /**
     * Resends the last query, recollecting all current parameters which
     * if any have changed, will result in a query being sent to the server.
     */
    private void resendLastQuery() {
        if(lastRequest != null) {
            ServerRequest req = buildServerRequest((Payload)lastRequest.getPayload(), false);
            UUID windowId = req.getWindow().getWindowID();
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_REQUEST.subj() + windowId, req);
            ServerMessageService.getInstance().setForcedMode(false);
        }
        dirtyProperty.set(false);
    }
    
    /**
     * Utility method to execute setup necessary to create a {@link SegmentedButtonBar}
     * @return  the created SegmentedButtonBar
     */
    private SegmentedButtonBar createSegmentedButtonBar() {
        SegmentedButtonBar buttonBar1 = new SegmentedButtonBar();
        buttonBar1.setFocusTraversable(false);
        buttonBar1.setAlignment(Pos.CENTER);
        buttonBar1.getStyleClass().setAll("output-navbar");
        
        ///////////////////////////////////////////////
        // Leave this in case we want to switch back //
        ///////////////////////////////////////////////
        //buttonBar1.getStyleClass().setAll("out-segmented-button-bar");
       
        return buttonBar1;
    }
    
    /**
     * Utility method to execute setup necessary to create a {@link SegmentedButtonBar}
     * @return  the created SegmentedButtonBar
     */
    private SegmentedButtonBar createSegmentedSendBar() {
        SegmentedButtonBar buttonBar1 = new SegmentedButtonBar();
        buttonBar1.setFocusTraversable(false);
        buttonBar1.setAlignment(Pos.CENTER);
        buttonBar1.getStyleClass().setAll("out-segmented-button-bar");
        return buttonBar1;
    }
    
    /**
     * Creates the row consisting of 2 bars: the re-send button and the larger
     * output view button row.
     * 
     * @param sendButton
     * @param navigationBar
     * @return  the entire row
     */
    private HBox createSegmentedButtonRow(Button sendButton, SegmentedButtonBar navigationBar) {
        assignToggleGroup(navigationBar);
        
        HBox displayBox = new HBox();
        displayBox.setFocusTraversable(false);
        displayBox.setSpacing(5);
        displayBox.setAlignment(Pos.CENTER);
        
        layoutBoundsProperty().addListener((v,o,n) -> {
            displayBox.setLayoutX((n.getWidth() / 2) - (displayBox.getLayoutBounds().getWidth() / 2));
        });
        
        ToggleButton firstButton = (ToggleButton)navigationBar.getChildren().get(0);
        
        Platform.runLater(() -> {
            displayBox.setLayoutX(
                (layoutBoundsProperty().get().getWidth() / 2) - (displayBox.getLayoutBounds().getWidth() / 2));
            firstButton.setSelected(false);
            firstButton.setSelected(true);
        });
        
        SegmentedButtonBar resendButtonBar = createSegmentedSendBar();
        resendButtonBar.getChildren().addAll(sendButton);
        
        navigationBar.getChildren().stream()
            .filter(b -> (b instanceof ToggleButton))
            .forEach(b -> ((ToggleButton)b)
                .setOnAction(e -> { 
                    selectView((ViewType)b.getUserData());
                }));
        
        displayBox.getChildren().addAll(resendButtonBar, navigationBar);
        
        return displayBox;
    }
    
    /**
     * Adds each Toggle in the specified button bar to a new ToggleGroup
     * used for mutual exclusion of toggle buttons.
     * @param bar
     */
    private void assignToggleGroup(SegmentedButtonBar bar) {
        ToggleGroup toggleGroup = new ToggleGroup();
        bar.getChildren().stream()
            .filter(b -> {
                return (b instanceof ToggleButton) && !((ToggleButton)b).getText().equals("Classify");
            })
            .forEach(b -> toggleGroup.getToggles().add((ToggleButton)b));
        
        ((ToggleButton)toggleGroup.getToggles().get(0)).requestFocus();
        toggleGroup.selectedToggleProperty().addListener((v,o,n) -> {
            if(n != null) { 
                if(n.getUserData() == ViewType.COMPARE) {
                    if(!compareEnabled) {
                        toggleGroup.selectToggle(prevSelectedToggle);
                        return;
                    }
                }
                prevSelectedToggle = n;
                if(n.getUserData() == ViewType.FINGERPRINT) {
                    Platform.runLater(() -> {
                        Window w = WindowService.getInstance().windowFor(OutputViewArea.this);
                        if(w == null) return;
                        w.resize(Math.max(w.getWidth(), 400), Math.max(w.getHeight(), 530));
                    });
                }else if(n.getUserData() != ViewType.SIMILAR_TERMS) {
                    Window w = WindowService.getInstance().windowFor(OutputViewArea.this);
                    w.setMinHeight(w.MIN_HEIGHT);
                }
            }
        });
        toggleGroup.selectToggle(toggleGroup.getToggles().get(0));
        
        toggleGroups.put(bar, toggleGroup);
    }
    
    /**
     * Buttons used for single Terms, or Expressions
     * @return
     */
    private ToggleButton[] createTermButtons() {
        ToggleButton sampleButton = new ToggleButton("Fingerprint");
        prevSelectedToggle = sampleButton;
        sampleButton.setFocusTraversable(false);
        sampleButton.getStyleClass().addAll("first");
        sampleButton.setUserData(ViewType.FINGERPRINT);
        ToggleButton sampleButton2 = new ToggleButton("Contexts");
        sampleButton2.setFocusTraversable(false);
        sampleButton2.setUserData(ViewType.CONTEXTS);
        ToggleButton sampleButton3 = new ToggleButton("Similar Terms");
        sampleButton3.setFocusTraversable(false);
        sampleButton3.setUserData(ViewType.SIMILAR_TERMS);
        ToggleButton sampleButton4 = new ToggleButton("Compare ");
        sampleButton4.setTooltip(new Tooltip("Connect 2 InputWindows to use \"Compare\""));
        // Add the Compare button to special mapping for later recall
        compareButtonMap.put(ViewType.TERM, sampleButton4);
        sampleButton4.getStyleClass().addAll("last");
        sampleButton4.setFocusTraversable(false);
        sampleButton4.setUserData(ViewType.COMPARE);
        
        ToggleButton[] toggles = new ToggleButton[] { sampleButton, sampleButton2, sampleButton3, sampleButton4 };
        for(ToggleButton tb : toggles) {
            tb.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                if(!tb.isSelected() && !tb.isDisabled()) {
                    tb.setSelected(true);
                    e.consume();
                }
            });
        }
        
        sampleButton4.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if(!compareEnabled) {
                e.consume();
            }
        });
        
        
        return toggles;
    }
    
    /**
     * Buttons used for multiple Terms (more than one, i.e. Text), Documents,
     * URL Scrapes, or Social Site Profiles
     * @return
     */
    private ToggleButton[] createTextButtons() {
        ToggleButton sampleButton = new ToggleButton("Fingerprint");
        sampleButton.setFocusTraversable(false);
        sampleButton.getStyleClass().addAll("first");
        sampleButton.setUserData(ViewType.FINGERPRINT);
        ToggleButton sampleButton3 = new ToggleButton("Keywords");
        sampleButton3.setFocusTraversable(false);
        sampleButton3.setUserData(ViewType.KEYWORDS);
        ToggleButton sampleButton4 = new ToggleButton("Tokens");
        sampleButton4.setFocusTraversable(false);
        sampleButton4.setUserData(ViewType.TOKENS);
        ToggleButton sampleButton5 = new ToggleButton("Slices");
        sampleButton5.setFocusTraversable(false);
        sampleButton5.setUserData(ViewType.SLICES);
        ToggleButton sampleButton6 = new ToggleButton("Compare ");
        // Add the Compare button to special mapping for later recall
        compareButtonMap.put(ViewType.TEXT, sampleButton6);
        sampleButton6.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if(!compareEnabled) {
                sampleButton6.setSelected(false);
                e.consume();
            }
        });
        sampleButton6.setFocusTraversable(false);
        sampleButton6.setUserData(ViewType.COMPARE);
        ToggleButton sampleButton7 = new ToggleButton("Classify");
        sampleButton7.setFocusTraversable(false);
        sampleButton7.setUserData(ViewType.CLASSIFY);
        sampleButton7.getStyleClass().addAll("last");
        sampleButton7.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            sampleButton7.setSelected(false);
            e.consume();
        });
        
        return new ToggleButton[] { sampleButton, sampleButton3, sampleButton4, 
            sampleButton5, sampleButton6, sampleButton7 };
    }
    
    /**
     * Iterates over the {@link ViewType}s and registers each display by its
     * view type to the view registry mechanism used to select views.
     */
    private void registerViews() {
        List<Pair<Pane, DoubleProperty>> allPairs = getAllToggleDisplays();
        IntStream.range(5, 13).forEach(i -> registerView(ViewType.values()[i], (View)allPairs.get(i - 5).getKey()));
    }
    
    /**
     * Creates the different displays and assigns pointer to their
     * variable holders.
     */
    private void createToggleDisplayVars() {
        fpDisplay = createFingerprintDisplay();
        
        keywordsDisplay = createKeywordsDisplay();
        
        tokensDisplay = createTokensDisplay();
        
        slicesDisplay = createSlicesDisplay();
        
        compareDisplay = createCompareDisplay();
        
        classifyDisplay = createClassifyDisplay();
        
        contextDisplay = createContextDisplay();
        
        simTermsDisplay = createSimilarTermsDisplay();
    }
    
    /**
     * Collects the "dirty" state of all the views which then updates 
     * this {@link ViewArea}'s general dirty state.
     */
    private void addViewDirtyPropertyHandlers() {
        simTermsDisplay.dirtyProperty().addListener((v,o,n) -> {
            dirtyProperty.set(n);
        });
        
        contextDisplay.dirtyProperty().addListener((v,o,n) -> {
            dirtyProperty.set(n);
        });
        
        tokensDisplay.dirtyProperty().addListener((v,o,n) -> {
            dirtyProperty.set(n);
        });
    }
    
    /**
     * Adds a handler to update the Resend button 
     * color indicating whether the button state (and therefore the
     * OutputArea), is "dirty" or not.
     */
    private void addSendButtonDirtyPropertyHandler() {
        dirtyProperty.addListener((v,o,n) -> {
            if(n) {
                exprSend.getStyleClass().remove("send");
                exprSend.getStyleClass().addAll("senddirty");
                textSend.getStyleClass().remove("send");
                textSend.getStyleClass().addAll("senddirty");
            }else{
                exprSend.getStyleClass().remove("senddirty");
                exprSend.getStyleClass().addAll("send");
                textSend.getStyleClass().remove("senddirty");
                textSend.getStyleClass().addAll("send");
            }
        });
    }
    
    /**
     * Returns a list of {@link Pair}s containing displays and their location
     * x property which will be manipulated during view change animations.
     * @return
     */
    private List<Pair<Pane, DoubleProperty>> getAllToggleDisplays() {
        List<Pair<Pane, DoubleProperty>> retVal = new ArrayList<>();
        
        retVal.addAll(Arrays.asList(
            new Pair<>(fpDisplay, new SimpleDoubleProperty()), 
            new Pair<>(contextDisplay, new SimpleDoubleProperty()),
            new Pair<>(keywordsDisplay, new SimpleDoubleProperty()),
            new Pair<>(tokensDisplay, new SimpleDoubleProperty()),
            new Pair<>(slicesDisplay, new SimpleDoubleProperty()),
            new Pair<>(simTermsDisplay, new SimpleDoubleProperty()), 
            new Pair<>(compareDisplay, new SimpleDoubleProperty()),
            new Pair<>(classifyDisplay, new SimpleDoubleProperty())));
        
            retVal.stream().forEach(
                pair -> pair.getValue().addListener(
                    (v,o,n) -> pair.getKey().relocate(n.doubleValue(), PANE_Y)));
        
        return retVal;
    }
    
    /**
     * Loads the type to view pairing map so views may be accessed by their
     * types.
     */
    private void createViewTypePairMapping() {
        IntStream.range(5, 13).forEach(i -> {
            typePairs.put(ViewType.values()[i], allPairs.get(i - 5));
        });
    }
    
    private FingerprintDisplay createFingerprintDisplay() {
        return new FingerprintDisplay("Fingerprints", LabelledRadiusPane.NewBG.ORANGE);
    }
    
    private SimilarTermsDisplay createSimilarTermsDisplay() {
        SimilarTermsDisplay display = new SimilarTermsDisplay("Similar Terms", LabelledRadiusPane.NewBG.ORANGE);
        display.observeContexts(contextDisplay.getContextsObservable());
        return display;
    }
    
    private KeywordsDisplay createKeywordsDisplay() {
        return new KeywordsDisplay("Keywords", LabelledRadiusPane.NewBG.ORANGE);
    }
    
    private TokensDisplay createTokensDisplay() {
        return new TokensDisplay("Tokens", LabelledRadiusPane.NewBG.ORANGE);
    }
    
    private SlicesDisplay createSlicesDisplay() {
        return new SlicesDisplay("Slices", LabelledRadiusPane.NewBG.ORANGE);
    }
    
    private CompareDisplay createCompareDisplay() {
        return new CompareDisplay("Compare", LabelledRadiusPane.NewBG.ORANGE);
    }
    
    private ClassifyDisplay createClassifyDisplay() {
        return new ClassifyDisplay("Classify", LabelledRadiusPane.NewBG.ORANGE);
    }
    
    private ContextDisplay createContextDisplay() {
        return new ContextDisplay("Contexts", LabelledRadiusPane.NewBG.ORANGE);
    }
    
    /**
     * Returns the {@link TokensDisplay}
     * @return
     */
    public TokensDisplay getTokensDisplay() {
        return tokensDisplay;
    }
    
    /**
     * Returns the {@link ContextDisplay}
     * @return
     */
    public ContextDisplay getContextDisplay() {
        return contextDisplay;
    }
    
    /**
     * Returns the {@link SimilarTermsDisplay}
     * @return
     */
    public SimilarTermsDisplay getSimilarTermsDisplay() {
        return simTermsDisplay;
    }
    
    /**
     * Called from the {@link WindowController} to clean up after closing a 
     * given window.
     */
    public void disconnectAllInputsFromEventBus() {
        for(UUID id : inputWindowConnections) {
            EventBus.get().unsubscribeTo(BusEvent.INPUT_EVENT_NAVIGATION_ACCEPTED.subj() + id, inputViewChangeHandler);
            EventBus.get().unsubscribeTo(BusEvent.INPUT_EVENT_INPUT_CLEARED.subj() + id, inputViewClearedHandler);
            EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_REQUEST_CREATED.subj() + id, inputWindowServerRequestSubscriber);
            EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_RESPONSE.subj() + id, serverResponseSubscriber);
            EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_RELOAD_CACHED_RESPONSE.subj() + id, reloadFromCacheSubscriber);
            EventBus.get().unsubscribeTo(BusEvent.RESET_CONNECTED_OUTPUT_VIEW_REQUEST.subj() + id, resetViewRequestSubscriber);
            EventBus.get().unsubscribeTo(BusEvent.INPUT_EVENT_NEW_EXPRESSION_STATE.subj() + id, newExpressionStateHandler);
            EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_RE_EXECUTE_LAST_QUERY.subj() + window.getWindowID(), resendLastQueryHandler);
        }
        isSubscribed.remove(inputViewChangeHandler);
        isSubscribed.remove(inputViewClearedHandler);
        isSubscribed.remove(inputWindowServerRequestSubscriber);
        isSubscribed.remove(serverResponseSubscriber);
        isSubscribed.remove(reloadFromCacheSubscriber);
        isSubscribed.remove(resetViewRequestSubscriber);
        isSubscribed.remove(newExpressionStateHandler);
        isSubscribed.remove(resendLastQueryHandler);
        inputWindowConnections.clear();
    }
    
    public void disconnectFromEventBus(UUID windowId) {
        EventBus.get().unsubscribeTo(BusEvent.INPUT_EVENT_NAVIGATION_ACCEPTED.subj() + windowId, inputViewChangeHandler);
        EventBus.get().unsubscribeTo(BusEvent.INPUT_EVENT_INPUT_CLEARED.subj() + windowId, inputViewClearedHandler);
        EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_REQUEST_CREATED.subj() + windowId, inputWindowServerRequestSubscriber);
        EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_RESPONSE.subj() + windowId, serverResponseSubscriber);
        EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_RELOAD_CACHED_RESPONSE.subj() + windowId, reloadFromCacheSubscriber);
        EventBus.get().unsubscribeTo(BusEvent.RESET_CONNECTED_OUTPUT_VIEW_REQUEST.subj() + windowId, resetViewRequestSubscriber);
        EventBus.get().unsubscribeTo(BusEvent.INPUT_EVENT_NEW_EXPRESSION_STATE.subj() + windowId, newExpressionStateHandler);
        EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_RE_EXECUTE_LAST_QUERY.subj() + window.getWindowID(), resendLastQueryHandler);
        isSubscribed.remove(inputViewChangeHandler);
        isSubscribed.remove(inputViewClearedHandler);
        isSubscribed.remove(inputWindowServerRequestSubscriber);
        isSubscribed.remove(serverResponseSubscriber);
        isSubscribed.remove(reloadFromCacheSubscriber);
        isSubscribed.remove(resetViewRequestSubscriber);
        isSubscribed.remove(newExpressionStateHandler);
        isSubscribed.remove(resendLastQueryHandler);
        inputWindowConnections.remove(windowId);
    }
    
    public void connectInputToEventBus(UUID windowId, boolean isFromPrimaryWindowChange) {
        if(inputViewClearedHandler == null || !isSubscribed.contains(inputViewClearedHandler)) {
            EventBus.get().subscribeTo(BusEvent.INPUT_EVENT_INPUT_CLEARED.subj() + windowId, inputViewClearedHandler = (t,p) -> {
                resetViews();
                isSubscribed.add(inputViewClearedHandler);
            });
        }
        
        
        if(inputViewChangeHandler == null || !isSubscribed.contains(inputViewChangeHandler)) {
            EventBus.get().subscribeTo(BusEvent.INPUT_EVENT_NAVIGATION_ACCEPTED.subj() + windowId, inputViewChangeHandler = (t,p) -> {
                swapButtonBar((ViewType)p.getPayload());
                
                OutputWindow ow = (OutputWindow)WindowService.getInstance().windowFor(OutputViewArea.this);
                selectViewWithFeedback(ViewType.FINGERPRINT);
                
                runDelayedPopulateFromCache((ViewType)p.getPayload(), true, CACHED_POPULATION_DELAY);
                
                // View Node needs extra layout signal when displayed for the first time
                if(firstOutputTransition) {
                    Platform.runLater(() -> {
                        ow.getController().nudgeLayout(ow, 10);
                        requestLayout();
                    });
                    firstOutputTransition = false;
                }
            });
            
            isSubscribed.add(inputViewChangeHandler);
            isNewExpression = true;
            Platform.runLater(() -> EventBus.get().broadcast(
                BusEvent.SERVER_MESSAGE_SEND_CURRENT_MODEL_QUERY.subj() + windowId, Payload.DUMMY_PAYLOAD));
        }
        
        if(inputWindowServerRequestSubscriber == null || !isSubscribed.contains(inputWindowServerRequestSubscriber)) {
            resetViews();
            
            // Install the handler for Server Message Requests upon InputWindow connection to an OutputWindow
            EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_REQUEST_CREATED.subj() + windowId, inputWindowServerRequestSubscriber = getServerRequestSubscriber());
            EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_RESPONSE.subj() + windowId, serverResponseSubscriber = getServerResponseSubscriber());
            EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_RELOAD_CACHED_RESPONSE.subj() + windowId, reloadFromCacheSubscriber = getServerResponseSubscriber());
            EventBus.get().subscribeTo(BusEvent.RESET_CONNECTED_OUTPUT_VIEW_REQUEST.subj() + windowId, resetViewRequestSubscriber = (s,p) -> resetViews());
            EventBus.get().subscribeTo(BusEvent.INPUT_EVENT_NEW_EXPRESSION_STATE.subj() + windowId, newExpressionStateHandler = (s,p) -> isNewExpression = true);
            EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_RE_EXECUTE_LAST_QUERY.subj() + window.getWindowID(), resendLastQueryHandler = (s,p) -> resendLastQuery());
            
            isSubscribed.add(inputWindowServerRequestSubscriber);
            isSubscribed.add(serverResponseSubscriber);
            isSubscribed.add(reloadFromCacheSubscriber);
            isSubscribed.add(resetViewRequestSubscriber);
            isSubscribed.add(newExpressionStateHandler);
            isSubscribed.add(resendLastQueryHandler);
            inputWindowConnections.add(windowId);
            
            outputAnimationStatusProperty.addListener(getOutputViewChangeAnimationHandler());
            inputAnimationStatusProperty.addListener(getInputViewChangeAnimationStatusHandler());
        }
    }
    
    /**
     * Disconnect the listeners for the secondary window
     * @param windowId  the id of the secondary window
     */
    public void disconnectSecondaryInputFromEventBus(UUID secWinID) {
        EventBus.get().unsubscribeTo(BusEvent.INPUT_EVENT_NAVIGATION_ACCEPTED.subj() + secWinID, secondaryInputViewChangeHandler);
        EventBus.get().unsubscribeTo(BusEvent.INPUT_EVENT_INPUT_CLEARED.subj() + secWinID, secondaryInputViewClearedHandler);
        EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_REQUEST_CREATED.subj() + secWinID, secondaryInputWindowServerRequestSubscriber);
        EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_SECONDARY_RESPONSE.subj() + secWinID, secondaryInputWindowServerResponseSubscriber);
        
        isSubscribed.remove(secondaryInputWindowServerRequestSubscriber);
        isSubscribed.remove(secondaryInputWindowServerResponseSubscriber);
        inputWindowConnections.remove(secWinID);
    }
    
    /**
     * Connect the listeners for the secondary window
     * @param secWinID  the id of the secondary window
     */
    private void connectSecondaryInputToEventBus(UUID secWinID) {
        if(!isSubscribed.contains(secondaryInputWindowServerRequestSubscriber)) {
            EventBus.get().subscribeTo(BusEvent.INPUT_EVENT_NAVIGATION_ACCEPTED.subj() + secWinID, secondaryInputViewChangeHandler = getSecondaryInputViewChangeHandler());
            EventBus.get().subscribeTo(BusEvent.INPUT_EVENT_INPUT_CLEARED.subj() + secWinID, secondaryInputViewClearedHandler = getSecondaryInputViewClearedHandler());
            EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_REQUEST_CREATED.subj() + secWinID, secondaryInputWindowServerRequestSubscriber = getSecondaryServerRequestSubscriber());
            EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_SECONDARY_RESPONSE.subj() + secWinID, secondaryInputWindowServerResponseSubscriber = getSecondaryServerResponseSubscriber());
            
            isSubscribed.add(secondaryInputWindowServerRequestSubscriber);
            isSubscribed.add(secondaryInputWindowServerResponseSubscriber);
            inputWindowConnections.add(secWinID);
        }
    }
    
    private void addSwapDetectionHandler() {
        WindowGroup windowGroup = window.getTitleBar().getInputSelector().getWindowGroup();
        
        windowGroup.swapDetectedProperty().addListener((v,o,n) -> {
            Platform.runLater(() -> mergeValve.swap());
        });
    }
    
    private void addPrimaryAdoptionHandler() {
        WindowGroup windowGroup = window.getTitleBar().getInputSelector().getWindowGroup();
        
        windowGroup.primaryAdoptionProperty().addListener((v,o,n) -> {
            // Removal of old primary when group existed, thus making the secondary the new primary
            // and the group contains only one window (the new primary).
            
            // Invoke progress meter so user can see a change happening
            setProgressIndicatorActive(window.getWindowID(), true);
            
            mergeValve.shiftPrimary();
            
            setProgressIndicatorActive(window.getWindowID(), false);
            
            // Make sure that the correct segmented button bar is showing
            syncButtonBar((InputWindow)windowGroup.getPrimaryWindow());
        });
    }
    
    /**
     * Add the handler which handles connecting this OutputViewArea to the appropriate
     * events for handling server message requests.
     */
    private void addPrimaryInputChangeHandler() {
        WindowGroup windowGroup = window.getTitleBar().getInputSelector().getWindowGroup();
        
        windowGroup.primaryWindowProperty().addListener((v,o,n) -> {
            if(o != null) {
                disconnectFromEventBus(o.getWindowID());
                resetViews();
            }
            
            if(n != null) {
                // o != null, means there was a primary window change, otherwise o == null
                connectInputToEventBus(n.getWindowID(), o != null);
                // Make sure that the correct segmented button bar is showing
                syncButtonBar((InputWindow)n);
            }
        });
    }
    
    /**
     * Add the handlers which observe secondary window groupings for the purpose
     * of invoking Compare API methods.
     */
    public void addSecondaryInputChangeHandler() {
        WindowGroup windowGroup = window.getTitleBar().getInputSelector().getWindowGroup();
        
        windowGroup.secondaryWindowProperty().addListener((v,o,n) -> {
            compareButtonMap.values().stream()
                .forEach(tb -> {
                    compareEnabled = n != null;
            });
            
            if(o != null) {
                disconnectSecondaryInputFromEventBus(o.getWindowID());
            }
            
            // Enable Merge
            if(n != null) { 
                connectSecondaryInputToEventBus(n.getWindowID());
                mergeValve.enableMerge();
                
                // If the old secondary is null, this is a simple addition of a secondary so attempt to resend that window's
                // last model query to populate the compare.
                if(o == null) {
                    // Tell the new secondary window to resend its last model if any...
                    EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_SEND_CURRENT_MODEL_QUERY.subj() + n.getWindowID(), Payload.DUMMY_PAYLOAD);
                }
            } else { // Disable Merge
                mergeValve.disableMerge();
                compareDisplay.reset();
            }
        });
    }
    
    /**
     * Called from the primary window group change listeners to make sure
     * the correct navigation bar is showing for the current primary input 
     * type.
     * @param w
     */
    private void syncButtonBar(InputWindow w) {
        ViewType type = w.getViewArea().getSelectedViewType();
        if(selectedIndex != indexForType(type)) {
            swapButtonBar(type);
        }
    }
    
    /**
     * Adds the handler which sizes and positions the view area upon change of the 
     * parent's dimensions.
     * 
     * @param panes     a list of {@link Pair}s of Panes and their x position properties.
     */
    private void addTogglePlacementHandler(List<Pair<Pane, DoubleProperty>> panes) {
        positioner = (v,o,n) -> {
            positionViewElements(panes, n);
        };
        layoutBoundsProperty().addListener(positioner);
    }
    
    /**
     * Called after tab transition animations to make sure the new tab views are in the
     * exact correct location.
     * @param panes
     * @param outputViewBounds
     */
    private void positionViewElements(List<Pair<Pane, DoubleProperty>> panes, Bounds outputViewBounds) {
        for(Pair<Pane, DoubleProperty> p : panes) {
            Pane pn = p.getKey();
            if(pn.isVisible()) {
                pn.resize(outputViewBounds.getWidth() - 10, 
                    outputViewBounds.getHeight() - 50 - segmentedButtonRow.getLayoutBounds().getMaxY());
                pn.relocate(5, segmentedButtonRow.getLayoutBounds().getMaxY() + 45);
                // Make sure viewable area defined by the clip dims is correct
                clip.setWidth(outputViewBounds.getWidth() - 2);
                clip.setHeight(outputViewBounds.getHeight());
            }
        }
    }

    /**
     * Do nothing
     */
    @Override
    public double computeHeight() { return 0; }
    
    /**
     * Collects the output parameters from the various OutputWindow tabs,
     * creates a {@link ServerRequest} and loads the output parameters into
     * it and then returns it. 
     * @param p     the {@link Payload} received from the {@link InputWindow} associated
     *              with this OutputViewArea's OutputWindow.
     * @return  a configured {@code ServerRequest}
     */
    @SuppressWarnings("unchecked")
    private ServerRequest buildServerRequest(Payload p, boolean isSecondaryRequest) {
        ServerRequest req = new ServerRequest();
        req.setWindow(p.getWindow());
        req.setOutputWindow(window);
        req.setSimTermsStartIndex(windowTitlePane.getResultStartIndexProperty(MaxResultsType.SIMTERMS).get());
        req.setSimTermsMaxResults(windowTitlePane.getMaxResultsProperty(MaxResultsType.SIMTERMS).get());
        req.setContextsStartIndex(windowTitlePane.getResultStartIndexProperty(MaxResultsType.CONTEXTS).get());
        req.setContextsMaxResults(windowTitlePane.getMaxResultsProperty(MaxResultsType.CONTEXTS).get());
        req.setSlicesStartIndex(windowTitlePane.getResultStartIndexProperty(MaxResultsType.SLICES).get());
        req.setSlicesMaxResults(windowTitlePane.getMaxResultsProperty(MaxResultsType.SLICES).get());
        FullClient currentClient = WindowService.getInstance().clientRetinaFor(p.getWindow());
        req.setRetinaClient(currentClient);
        String retinaName = RetinaClientFactory.getRetinaName(currentClient);
        req.setExtendedClient(RetinaClientFactory.getExtendedClient(retinaName));
        req.setRetinaLanguage(retinaName);
        
        Pair<UUID, Message> pair = (Pair<UUID, Message>)p.getPayload();
        Message mesg = pair.getValue();
        switch(mesg.getType()) {
            case TEXT: {
                req.setModel(mesg.getMessage());
                if(mesg.getMessage() instanceof Text) {
                    req.setText(((Text)mesg.getMessage()).getText());
                }else{
                    req.setText(((Term)mesg.getMessage()).getTerm());
                }
                req.setPayload(p);
                req.setInputViewType(mesg.getType());
                loadSelectedPosTags(req);
                break;
            }
            case EXPRESSION: {
                req.setModel(mesg.getMessage());
                req.setPayload(p);
                req.setInputViewType(mesg.getType());
                loadContextSelection(req, isNewExpression || isSecondaryRequest);
                req.setLookupContexts(isNewExpression || isSecondaryRequest);
                isNewExpression = false;
                loadPosSelection(req);
                break;
            }
            default:
        }
        
        return req;
    }
    
    /**
     * Returns the {@link EventTopicSubscriber} responsible for listening to InputWindow
     * requests and sending server message requests as a result.
     * @return
     */
    private EventTopicSubscriber<Payload> getServerRequestSubscriber() {
        return (s,p) -> {
            System.out.println("Got Payload: " + p + " for topic: " + s);
            if(p == null) {
                resetViews();
                return;
            }
            ServerRequest req = lastRequest = buildServerRequest(p, false);
            UUID windowId = req.getWindow().getWindowID();
            
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_REQUEST.subj() + windowId, req);
        };
    }
    
    /**
     * Returns the handler which monitors secondary input changes. This
     * is used to reset the {@link ComparisonDisplay}
     * @return
     */
    private EventTopicSubscriber<Payload> getSecondaryInputViewChangeHandler() {
        return (s,p) -> {
            compareDisplay.reset();
        };
    }
    
    /**
     * Returns the handler which monitors secondary input clearance changes. This
     * is used to reset the {@link ComparisonDisplay}
     * @return
     */
    private EventTopicSubscriber<Payload> getSecondaryInputViewClearedHandler() {
        return (s,p) -> {
            compareDisplay.reset();
        };
    }   
    
    /**
     * Returns the {@link EventTopicSubscriber} responsible for listening to InputWindow
     * requests and sending server message requests as a result.
     * @return
     */
    private EventTopicSubscriber<Payload> getSecondaryServerRequestSubscriber() {
        return (s,p) -> {
            System.out.println("Got Secondary Payload: " + p + " for topic: " + s);
            OutputWindow thisWindow = (OutputWindow)WindowService.getInstance().windowFor(this);
            InputWindow primary = (InputWindow)thisWindow.getInputSelector().getPrimaryWindow();
            if(primary.getViewArea().getSelectedView().messageProperty().get() == null) {
                resetViews();
                return;
            }
           
            ServerRequest req = lastRequest = buildServerRequest(p, true);
            UUID windowId = req.getWindow().getWindowID();
            
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_SECONDARY_REQUEST.subj() + windowId, req);
        };
    }
    
    /**
     * Loads the specified {@link ServerRequest} object with the ids
     * of the selected Contexts
     * @param req
     */
    private void loadContextSelection(ServerRequest req, boolean isNewExpression) {
        List<Integer> ids = contextDisplay.getSelectedContexts();
        if(isNewExpression || ids.size() == 0 || ids.size() > 1) {
            // Return if they are all selected. In that case we will
            // Use the "Any" category which is a quicker query.
            req.setContextID(-1);
            return;
        }
        
        req.setContextID(ids.get(0));
    }
    
    /**
     * Loads the specified {@link ServerRequest} object with the 
     * user's selected {@link PosType}
     * @param req
     */
    private void loadPosSelection(ServerRequest req) {
        PosType type = simTermsDisplay.getSelectedPOS();
        req.setPartOfSpeech(type);
    }
    
    /**
     * Loads the specified {@link ServerRequest} object with the 
     * user's selected {@link PosType}
     * @param req
     */
    private void loadSelectedPosTags(ServerRequest req) {
        Set<PosTag> tags = tokensDisplay.getSelectedPOSTags();
        System.out.println("PosTags = " + tags);
        req.setPosTags(tags);
    }
    
    /**
     * Populates this {@code OutputViewArea}'s relevant displays with
     * the current query response.
     * <p>
     * NOTE: The calling method is responsible for filtering updates to
     * only the relevant OutputWindows.
     * 
     * @param r
     */
    private void populateFromPrimaryResponse(ServerResponse r) {
        setFingerprint(r.getFingerPrint(), r);
        
        switch(r.getInputType()) {
            case EXPRESSION: {
                setSimilarTerms(r.getTerms());
                setContexts(r.getRequest(), r.getContexts());
                break;
            }
            case TEXT: {
                setKeywords(r);
                setSentencesAndTokens(r);
                setSlices(r);
            }
            default:
        }
    }
    
    /**
     * Returns the handler which receives the server response and populates the 
     * OutputWindow's different tabs with output.
     * 
     * @return  the server response handler
     */
    private EventTopicSubscriber<Payload> getServerResponseSubscriber() {
        return (s,p) -> {
            ServerResponse r = (ServerResponse)p;
            populate(r, true);
            cachedResponseForTypes.put(r.getInputType(), r);
        };
    }
    
    /**
     * Returns the handler which receives the secondary server response.
     * 
     * @return  the server response handler
     */
    private EventTopicSubscriber<Payload> getSecondaryServerResponseSubscriber() {
        return (s,p) -> {
            ServerResponse r = (ServerResponse)p;
            populate(r, false);
        };
    }
    
    /**
     * Runs the population from cache following a delay so that screen
     * transition animations can finish before populating the UI.
     * @param type
     * @param isPrimary
     */
    private void runDelayedPopulateFromCache(ViewType type, boolean isPrimary, int delay) {
        (new Thread(() -> {
            try{ Thread.sleep(delay); }catch(Exception e) { e.printStackTrace(); }
            
            Platform.runLater(() -> {
                ServerResponse cachedResponse = cachedResponseForTypes.get(type);
                if(cachedResponse != null) {
                    populate(cachedResponse, isPrimary);
                }
            });
        })).start();
    }
    
    /**
     * Called internally to execute the Output's population mechanisms. If
     * this is from a single {@link InputWindow}, the merge valve will invoke
     * the handler for single inputs; otherwise if a "grouped" input, the population
     * is done from the response directly. 
     * @param r
     * @param isPrimary
     */
    private void populate(ServerResponse r, boolean isPrimary) {
        // Special case where two new windows are opened; they are then grouped and the 
        // user has entered in primary input for the first time and there is no sec. input.
        if(isPrimary && mergeValve.mergeEnabled() && mergeValve.getLastSecondaryInput() == null) {
            populateFromPrimaryResponse(r);
        }
        
        // This is added within a different thread so that the merge function
        // will be called on that different thread instead of the main render thread
        (new Thread(() -> { 
            if(isPrimary) { 
                mergeValve.addPrimaryInput(r);
            }else{ 
                mergeValve.addSecondaryInput(r); 
            } 
        }, 
            isPrimary ? "ServerResponseSubscriber_PrimaryInputThread" : 
                "SecondaryServerResponseSubscriber_SecondaryInputThread")
        ).start();
    }
    
    /**
     * Returns the subscriber which handles invoking the progress meter.
     * @return  the subscriber for the progress meter.
     */
    private EventTopicSubscriber<Payload> getProgressSubscriber() {
        return (m,t) -> {
            if(!Platform.isFxApplicationThread()) {
                Platform.runLater(() -> {
                    invokeProgressHandler(m);
                });
            }else{
                invokeProgressHandler(m);
            }
        };
    }
    
    /**
     * Some routes originate from the {@link Overlay} and should not concern us...
     * This check returns true if the specified route is not from an {@link InputWindow}.
     * @param route
     * @return
     */
    private boolean isRouteException(String route) {
        if(route.indexOf("ombine") != -1 || route.indexOf("ext") != -1) {
            return true;
        }
        return false;
    }
    
    /**
     * Handles signaling the progress meter, and handling of dirty
     * state properties.
     * @param topic     the Bus message string
     */
    private void invokeProgressHandler(String topic) {
        // If not sent from one of this OutputWindow's connected InputWindows
        String route = topic.substring(topic.lastIndexOf("_") + 1);
        if(isRouteException(route) || !inputWindowConnections.contains(UUID.fromString(route))) {
            return;
        }
        
        if(topic.indexOf("InProgress") != -1) {
            progressProperty.set(true);
        }else if(topic.indexOf("HaltProgress") != -1) {
            progressProperty.set(false);
            contextDisplay.dirtyProperty().set(false);
            simTermsDisplay.dirtyProperty().set(false);
            tokensDisplay.dirtyProperty().set(false);
            windowTitlePane.dirtyProperty().set(false);
        }
    }
    
    /**
     * Returns a handler which does special initialization following
     * a regular output view change.
     * @return
     */
    private ChangeListener<Boolean> getOutputViewChangeAnimationHandler() {
        return (v,o,n) -> {
            if(n) {
                ViewType viewType = getSelectedViewType();
                WindowService.getInstance().windowTitleFor(
                    WindowService.getInstance().windowFor(OutputViewArea.this))
                        .setMaxResultsControl(MaxResultsType.typeFor(viewType));
            }
        };
    }
    
    /**
     * Returns a handler which clears (resets) the views when notified
     * following a view transition animation. We don't want to clear
     * after every transition, only the transitions that result from an
     * input view change - which mandates all previous data is cleared.
     * @return
     */
    private ChangeListener<Boolean> getInputViewChangeAnimationStatusHandler() {
        return (v,o,n) -> {
            if(n) {
                resetViews();
                simTermsDisplay.getPOSTypesCombo().getSelectionModel().select(0);
                dirtyProperty.set(false);
            }
        };
    }
    
    /**
     * Returns the routing valve which is an enhanced RxObservable combine function
     * utilizing the {@link Observable#combineLatest(List, rx.functions.FuncN)} function
     * to route singly or by merging all inputs and composing an aggregate output.
     * <p>
     * Sequence of operations:
     * <ol>
     *  <li>Following the addition of a primary when a secondary previously has been added or vice versa...</li>
     *  <li>The MergeSupplier's FunctionN is immediately called (below)</li>
     *  <li> The MergeSupplier's FunctionN returns an Observable&lt;CompareRequest&gt; which the valve's supplied
     *  merge observer is called with in its "onNext()"</li>
     *  <li>The merge observer "subscribes" to the Observable&lt;CompareRequest&gt;, and the onSubscribe function
     *  kicks off two things (in order):
     *      <ul>
     *          <li>First, a BusEvent listener is added for the CompareRequest's CompareResponse</li>
     *          <li>...then that CompareRequest is sent over the EventBus so that the server request will be made.</li>
     *      </ul></li>
     *  <li>The {@link ServerMessageService} sends the CompareRequest to the server API (wrapped by the {@link CachingClientWrapper}),
     *  which receives a {@link Metric} in return.</li>
     *  <li>The ServerMessageService (see {@link ServerMessageService#getCompareRequestHandler}) then places the Metric on the bus
     *  within a {@link CompareResponse} using the SERVER_MESSAGE_EXECUTE_COMPARE_RESPONSE channel.</li>
     *  <li> The compare response subscriber receives that {@code CompareResponse}; adds some information to it, then emits it so 
     *  that the valve's supplied merge observer can then construct a {@link Comparison} object which it then sends to the
     *  {@link CompareDisplay}, which then displays the information.
     * </ol></p>
     * 
     * @return  the {@link MergeOrPassthruValve}
     */
    @SuppressWarnings("rawtypes")
    private MergeOrPassthruValve<ServerResponse, Observable, ServerResponse> prepareValve() {
        MergeSupplier<ServerResponse, Observable, ServerResponse> ms = 
            new MergeSupplier<ServerResponse, Observable, ServerResponse>(ServerResponse.class, Observable.class, ServerResponse.class, 2, 
                (Object[] oa) -> {
                    ServerResponse primeResponse = ((ServerResponse)oa[0]);
                    ServerResponse secondResponse = ((ServerResponse)oa[1]);
                    ServerRequest primeRequest = primeResponse.getRequest();
                    primeRequest.setModel(primeResponse.getFingerPrint());
                    ServerRequest secondRequest = secondResponse.getRequest();
                    secondRequest.setModel(secondResponse.getFingerPrint());
                    
                    CompareRequest req = new CompareRequest();
                    
                    UUID primWinID = primeRequest.getWindow().getWindowID();
                    req.setPrimaryInputWindowID(primWinID);
                    req.setPrimaryEndpointIndicator(primeRequest.getInputViewType());
                    req.setPrimaryModel(primeRequest.getModel());
                    
                    UUID secWinID = secondRequest.getWindow().getWindowID();
                    req.setSecondaryInputWindowID(secWinID);
                    req.setSecondaryEndpointIndicator(secondRequest.getInputViewType());
                    req.setSecondaryModel(secondRequest.getModel());
                    
                    FullClient reqClient = WindowService.getInstance().clientRetinaFor(window);
                    req.setCompareClient(reqClient);
                    String retinaName = RetinaClientFactory.getRetinaName(reqClient);
                    req.setExtendedClient(RetinaClientFactory.getExtendedClient(retinaName));
                    req.setRetinaLanguage(retinaName);
                    
                    UUID route = window.getWindowID();
                    
                    Observable<CompareResponse> retVal = Observable.create(subscriber -> {
                        EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_EXECUTE_COMPARE_RESPONSE.subj() + route, compareResponseSubscriber = (s,p) -> {
                            primeRequest.setOutputWindow(window);
                            secondRequest.setOutputWindow(window);
                            
                            populateFromPrimaryResponse((ServerResponse)oa[0]);
                            
                            CompareResponse resp = (CompareResponse)p;
                            resp.setPrimaryRequest(primeRequest);
                            resp.setSecondaryRequest(secondRequest);
                            resp.setPrimaryResponse((ServerResponse)oa[0]);
                            resp.setSecondaryResponse((ServerResponse)oa[1]);
                            subscriber.onNext((CompareResponse)p);
                            subscriber.onCompleted();
                            EventBus.get().unsubscribeTo(BusEvent.SERVER_MESSAGE_EXECUTE_COMPARE_RESPONSE.subj() + route, compareResponseSubscriber);
                        });
                        
                        EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_EXECUTE_COMPARE_REQUEST.subj() + route, req);
                    });
                    
                    return retVal;
                });
        
        return new MergeOrPassthruValve<>(ms);
    }
    
    private Observer<ServerResponse> getPrimaryObserver() {
        return new Observer<ServerResponse>() {
            public void onNext(ServerResponse r) {
                populateFromPrimaryResponse(r);
            }
            public void onError(Throwable t) {}
            public void onCompleted() {}
        };
    }
    
    private Observer<ServerResponse> getSecondaryObserver() {
        return new Observer<ServerResponse>() {
            public void onNext(ServerResponse r) {
                
            }
            public void onError(Throwable t) {}
            public void onCompleted() {}
        };
    }
    
    private Observer<Observable<CompareResponse>> getMergeObserver() {
        return new Observer<Observable<CompareResponse>>() {
            public void onNext(Observable<CompareResponse> r) {
                mergeSubscription = r.subscribe(cr -> {
                    CompareResponse response = (CompareResponse)cr;
                    Comparison c = new Comparison(response);
                    compareDisplay.addComparison(c);
                    mergeSubscription.unsubscribe();
                });
            }
            public void onError(Throwable t) {}
            public void onCompleted() {}
        };
    }
    
    
}
