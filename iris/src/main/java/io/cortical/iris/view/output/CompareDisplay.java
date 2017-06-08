package io.cortical.iris.view.output;

import static io.cortical.iris.message.BusEvent.OVERLAY_SHOW_MODAL_DIALOG;

import java.awt.MouseInfo;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.cortical.fx.webstyle.Impression;
import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.CompareResponse;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.ui.util.DragAssistant;
import io.cortical.iris.view.OutputViewArea;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.view.output.compare.CompareImpressionDisplay;
import io.cortical.iris.view.output.compare.ImpressionPane;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Metric;
import io.cortical.retina.model.Model;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * One of the {@link OutputWindow} displays residing within the {@link OutputViewArea}
 * which displays comparison information using the Compare API within the Retina Client's
 * many API categories.
 * <p>
 * This display consists of three parts:<br>
 * <ul>
 *  <li>A multi-axis display which shows comparative distance metrics</li>
 *  <li>An overlap display, lending a visual aspect to the overlap metrics</li>
 *  <li>3 {@link Impression}s or {@link Fingerprint} grids showing the fingerprints of the
 *  two language items being compared, and another showing the two overlapped.</li>
 * </ul>
 * 
 * @author cogmission
 *
 */
public class CompareDisplay extends LabelledRadiusPane implements View {
    private enum FadeDirection { IN, OUT };
    
    private static final Metric DUMMY_METRIC = getDummyMetric();
    
    public final ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();
    
    /** The queue of delayed Comparison Metric updates. */
    private List<Runnable> commandQueue = new ArrayList<>();
    
    private String[] metricLabels = { "Cosine Similarity", "Euclidean Distance", "Jaccard Distance" };
    
    private Label weightedScoringLabel = new Label("Weighted Scoring: ");
    
    private OverlappingDisplay olDisplay;
    
    private HorizontalMultiAxisChart chart;
    
    private ObjectProperty<Bounds> viewportBoundsProperty;
    
    private CompareImpressionDisplay comparisonView;
    
    private ScrollPane metricContentScroll;
    
    private ImpressionPane compareImpression;
    
    private FadeTransition fadeOut, fadeIn;
    
    private Comparison lastComparison;
    
    private Button showFingerprintsBtn, showMetricsBtn;
    
    private Region backPanel;
    
    
    
    
    /**
     * Constructs a new {@code CompareDisplay}
     * 
     * @param label     the label for the radiused tab
     * @param spec      the color type
     */
    public CompareDisplay(String label, NewBG spec) {
        super(label, spec);
        
        setVisible(false);
        setUserData(ViewType.COMPARE);
        setManaged(false);
        getStyleClass().add("compare-display");
        
        chart = new HorizontalMultiAxisChart(this, metricLabels);
        chart.setFocusTraversable(false);
        olDisplay = new OverlappingDisplay(this);
        
        comparisonView = createCompareFingerprintsDialog();
        
        fadeIn = createFadeTransition(FadeDirection.IN, compareImpression);
        fadeOut = createFadeTransition(FadeDirection.OUT, metricContentScroll);
        
        compareImpression = new ImpressionPane("Combined", "rgb(90, 90, 90)", "rgb(188, 72, 27)", "rgb(237, 93, 37)");
        compareImpression.setFocusTraversable(false);
        compareImpression.prefWidthProperty().bind(widthProperty().subtract(10));
        compareImpression.prefHeightProperty().bind(heightProperty().subtract(labelHeightProperty().add(20)));
        compareImpression.layoutYProperty().set(40);
        compareImpression.layoutXProperty().set(5);
        compareImpression.setVisible(false);
        Platform.runLater(() -> {
            compareImpression.setCompareContext(
                ((OutputWindow)WindowService.getInstance().windowFor(this)).getInputSelector().getWindowGroup());
            
            addInfoButton();
        });
        
        DragAssistant.configureDragHandler(compareImpression.getImpression());
        
        showMetricsBtn = new Button("Show Fingerprint");
        showMetricsBtn.getStyleClass().addAll("impression");
        showMetricsBtn.setManaged(false);
        showMetricsBtn.setFocusTraversable(false);
        showMetricsBtn.setOnAction(e -> {
            if(showMetricsBtn.getText().equals("Show Fingerprint")) {
                showMetricsBtn.setText("Show Metrics");
                runFade(compareImpression, metricContentScroll);
            } else {
                showMetricsBtn.setText("Show Fingerprint");
                runFade(metricContentScroll, compareImpression);
            }
        });
        
        showFingerprintsBtn = new Button("Show Comparison View");
        showFingerprintsBtn.getStyleClass().addAll("impression");
        showFingerprintsBtn.setManaged(false);
        showFingerprintsBtn.setFocusTraversable(false);
        showFingerprintsBtn.setOnAction(e -> {
            showCompareFingerprints(comparisonView);
        });
        
        metricContentScroll = new ScrollPane();
        metricContentScroll.setVbarPolicy(ScrollBarPolicy.ALWAYS);
        metricContentScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        metricContentScroll.prefWidthProperty().bind(widthProperty().subtract(10));
        metricContentScroll.prefHeightProperty().bind(heightProperty().subtract(labelHeightProperty().add(20)));
        metricContentScroll.layoutYProperty().set(40);
        metricContentScroll.layoutXProperty().set(5);
        
        Line separator = new Line();
        separator.setManaged(false);
        separator.setStrokeLineCap(StrokeLineCap.ROUND);
        separator.setStrokeMiterLimit(10);
        separator.setStroke(Color.rgb(26, 73, 94));
        
        weightedScoringLabel.setManaged(false);
        weightedScoringLabel.setFont(Font.font(12d));
        weightedScoringLabel.textProperty().addListener((v,o,n) -> {
            Text t = new Text(n);
            t.setFont(weightedScoringLabel.getFont());
            weightedScoringLabel.resize(t.getLayoutBounds().getWidth() + 5, 15);
            Platform.runLater(() -> {
                double w = viewportBoundsProperty.get().getWidth();
                weightedScoringLabel.relocate(w / 2 - weightedScoringLabel.getLayoutBounds().getWidth() / 2, 273);
            });
        });
        
        Line separator2 = new Line();
        separator2.setManaged(false);
        separator2.setStrokeLineCap(StrokeLineCap.ROUND);
        separator2.setStrokeMiterLimit(10);
        separator2.setStroke(Color.rgb(26, 73, 94));
        
        viewportBoundsProperty = metricContentScroll.viewportBoundsProperty();
        metricContentScroll.viewportBoundsProperty().addListener((v,o,n) -> {
            chart.setPrefWidth(n.getWidth());
            chart.setPrefHeight(263);
            olDisplay.setPrefWidth(n.getWidth());
            olDisplay.setPrefHeight(30);
            
            double lineWidth = n.getWidth() / 4;
            separator.setStartX(n.getWidth() / 2 - lineWidth / 2);
            separator.setEndX(separator.getStartX() + lineWidth);
            separator.setStartY(268);
            separator.setEndY(268);
            
            Platform.runLater(() -> {
                Text t = new Text(weightedScoringLabel.getText());
                t.setFont(weightedScoringLabel.getFont());
                weightedScoringLabel.resize(t.getLayoutBounds().getWidth() + 5, 15);
                weightedScoringLabel.relocate(n.getWidth() / 2 - weightedScoringLabel.getLayoutBounds().getWidth() / 2, 273);
            });
            
            separator2.setStartX(n.getWidth() / 2 - lineWidth / 2);
            separator2.setEndX(separator.getStartX() + lineWidth);
            separator2.setStartY(296);
            separator2.setEndY(296);
            
            showMetricsBtn.resizeRelocate((n.getWidth() / 2) - (255 / 2), 5, 125, 20);
            showFingerprintsBtn.resizeRelocate(130 + (n.getWidth() / 2) - (125 / 2), 5, 125, 20);
        });
        
        VBox display = new VBox(40);
        display.getChildren().addAll(chart, separator, weightedScoringLabel, separator2, olDisplay);
        
        metricContentScroll.setContent(display);
        
        getChildren().addAll(showMetricsBtn, showFingerprintsBtn, metricContentScroll, compareImpression);
        
        addVisiblityHandler();
    }
    
    /**
     * Implemented by {@code View} subclasses to handle an error
     * 
     * @param	context		the error information container
     */
    @Override
    public void processRequestError(RequestErrorContext context) {}
    
    /**
     * Returns the property which emits changes for the main
     * ScrollPane of this {@code CompareDisplay}
     * @return  the property
     */
    public ObjectProperty<Bounds> viewportBoundsProperty() {
        return viewportBoundsProperty;
    }
    
    /**
     * Called externally to invoke this {@code CompareDisplay} to display
     * the current query data.
     * @param c     internally defined container object for this display's data
     * @see #setComparison(Comparison)
     */
    public void addComparison(Comparison c) {
        if(!isVisible()) {
            commandQueue.add(() -> setComparison(c));
            return;
        }
        
        // Add Delay so that any animations currently running will finish
        (new Thread(() -> {
            try { Thread.sleep(500); }catch(Exception e) { e.printStackTrace(); }
            Platform.runLater(() -> setComparison(c));
        })).start();
    }
    
    /**
     * Called internally to configure the display with new data.
     * @param c the container object for comparison data
     * @see #addComparison(Comparison)
     */
    private void setComparison(Comparison c) {
        lastComparison = c;
        
        // Set chart data
        IntStream.range(0, 3).forEach(i -> { 
            switch(i) { 
                case 0: chart.setCategoryData(metricLabels[i], c.metric.getCosineSimilarity()); break;
                case 1: chart.setCategoryData(metricLabels[i], c.metric.getEuclideanDistance()); break;
                case 2: chart.setCategoryData(metricLabels[i], c.metric.getJaccardDistance()); break;
            }
        });
        
        // Set overlap display data
        olDisplay.setMetric(c.metric);
        
        weightedScoringLabel.textProperty().set("Weighted Scoring: " + c.metric.getWeightedScoring());
        
        comparisonView.populate(c);
        
        if(lastComparison != null && showMetricsBtn.getText().indexOf("etrics") != -1) {
            showCompareSDR(lastComparison);
        }
    }
    
    private void showCompareSDR(Comparison c) {
        compareImpression.getImpression().clear();
        
        int[] primarySDR = c.getFP1().getPositions();
        int[] secondarySDR = c.getFP2().getPositions();
        
        if(primarySDR == null || primarySDR.length < 1 || secondarySDR == null || secondarySDR.length < 1) {
            throw new IllegalStateException("Primary or Secondary SDR was null, therfore couldn't populate compare fingerprint!");
        }
        
        Set<Integer> intermediate = Arrays.stream(primarySDR)
            .boxed()
            .collect(Collectors.toSet());
        
        int[] compareSDR = Arrays.stream(secondarySDR)
            .boxed()
            .filter(s -> intermediate.contains(s))
            .mapToInt(i -> i)
            .toArray();
        
        compareImpression.setPrimarySDR(primarySDR);
        compareImpression.setSecondarySDR(secondarySDR);
        compareImpression.setCompareSDR(compareSDR);
    }
    
    /**
     * Adds the visibility property handler which checks the command queue
     * for commands to run when the screen becomes visible.
     */
    private void addVisiblityHandler() {
        // Add delay so that the command updates don't share time with the transition
        // animation.
        visibleProperty().addListener((v,o,n) -> {
            OutputWindow w = (OutputWindow)WindowService.getInstance().windowFor(this);
            UUID uuid = w.getInputSelector().getSecondaryInputSelection();
            if(uuid != null) {
                InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(uuid);
                if(iw.getViewArea().getSelectedView().messageProperty().get() == null) {
                    Platform.runLater(() -> reset());
                    commandQueue.clear();
                }
            }
            
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
            } else {
                chart.reset();
                commandQueue.add(() -> {
                    Arrays.stream(metricLabels)
                        .forEach(l -> {
                            chart.setCategoryData(l, chart.barAttributes.get(l).value);
                        });
                });
            }
        });
    }

    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        chart.reset();
        olDisplay.reset();
        weightedScoringLabel.textProperty().set("Weighted Scoring:");
        commandQueue.clear();
        commandQueue.add(() -> {
            Arrays.stream(metricLabels)
                .forEach(l -> {
                    chart.setCategoryData(l, chart.barAttributes.get(l).resetValue);
                });
        });
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
            backPanel = new CompareBackPanel(10).getScroll();
        }
        
        return backPanel;
    }
    
    /**
     * Returns a {@link Metric} used to "blank" the screen.
     * @return
     */
    private static final Metric getDummyMetric() {
        Map<String, Double> values = new HashMap<>();
        values.put(Metric.JACCARD_DISTANCE, 1.0);
        values.put(Metric.EUCLIDEAN_DISTANCE, 1.0);
        values.put(Metric.COSINE_SIMILARITY, 0.0);
        values.put(Metric.WEIGHTED_SCORING, 0.0);
        values.put(Metric.SIZE_LEFT, 0.0);
        values.put(Metric.SIZE_RIGHT, 0.0);
        values.put(Metric.OVERLAPPING_ALL, 0.0);
        values.put(Metric.OVERLAPPING_LEFT_RIGHT, 0.0);
        values.put(Metric.OVERLAPPING_RIGHT_LEFT, 0.0);
        Metric metric = new Metric(values);
        return metric;
    }
    
    /**
     * Displays the "comparison view"
     * @param dialog
     */
    private void showCompareFingerprints(Pane dialog) {
        Payload p = new Payload(dialog);
        EventBus.get().broadcast(OVERLAY_SHOW_MODAL_DIALOG.subj(), p);
    }
    
    /**
     * Creates and returns the {@link Pane} used to contain the widgets used to
     * display input view switching information.
     * @return
     */
    private CompareImpressionDisplay createCompareFingerprintsDialog() {
        CompareImpressionDisplay display = new CompareImpressionDisplay();
        
        // Delay setting the {@link WindowGroup} on the display until after all 
        // windows are fully formed.
        Platform.runLater(() -> display.initWindowGroup(
            ((OutputWindow)WindowService.getInstance().windowFor(this))
                .getTitleBar().getInputSelector().getWindowGroup()));
        
        return display;        
    }
    
    /**
     * Creates and returns the transition for showing/hiding the metrics/fingerprint
     * @param fd        the direction this transition will run in.
     * @param n         the node the transition acts upon
     * @return          the newly created {@link FadeTransition}
     */
    public FadeTransition createFadeTransition(FadeDirection fd, Node n) {
        FadeTransition fadeTransition = 
            new FadeTransition(Duration.millis(750), n);
        fadeTransition.setFromValue(fd == FadeDirection.IN ? 0.0 : 1.0);
        fadeTransition.setToValue(fd == FadeDirection.IN ? 1.0 : 0.0);
        fadeTransition.setCycleCount(1);
        fadeTransition.setAutoReverse(false);
        
        return fadeTransition;
    }
    
    /**
     * Invokes the fade animation to switch views.
     * @param inNode        the Node becoming visible
     * @param outNode       the Node becoming invisible
     */
    public void runFade(Node inNode, Node outNode) {
        fadeOut.setNode(outNode);
        fadeIn.setNode(inNode);
        
        fadeOut.setOnFinished(e -> {
            outNode.setVisible(false);
            fadeIn.play();
            if(inNode == compareImpression && lastComparison != null) {
                showCompareSDR(lastComparison);
            }
        });
        
        inNode.setOpacity(0.0);
        inNode.setVisible(true);
        
        fadeOut.play();
    }
    
    ///////////////////////////////////////////////////////////
    //                Inner Class Definitions                //
    ///////////////////////////////////////////////////////////
    
    /**
     * Container class for comparison query request and response
     * information.
     */
    public static class Comparison {
        private Fingerprint fp1, fp2;
        private Model model1, model2;
        private Metric metric;
        
        public Comparison(CompareResponse p) {
            this.fp1 = p.getPrimaryFingerprint();
            this.fp2 = p.getSecondaryFingerprint();
            this.model1 = p.getPrimaryModel();
            this.model2 = p.getSecondaryModel();
            this.metric = p.getMetric();
        }
        
        public Fingerprint getFP1() {
            return fp1;
        }
        
        public Fingerprint getFP2() {
            return fp2;
        }
        
        public Model getModel1() {
            return model1;
        }
        
        public Model getModel2() {
            return model2;
        }
        
        public Metric getMetric() {
            return metric;
        }
    }
    
    /**
     * Layout node for the visualization of overlap metrics.
     */
    public static class OverlappingDisplay extends VBox {
        private Group overlapContainer;
        private Rectangle leftBar;
        private Rectangle rightBar;
        private Rectangle overlapBar;
        
        private Label sizeLeft, sizeRight, overlapAll, olLeftRight, olRightLeft;
        private Label sizeLeftValue, sizeRightValue, overlapAllValue, olLeftRightValue, olRightLeftValue;
        
        private AnchorPane topLabelAnchor;
        private AnchorPane bottomLabelAnchor;
        
        private Metric metric;
        
        private CompareDisplay parentContainer;
        
        private Point2D savedMouseOverPoint;
        private Point2D savedMouseOverBarPoint;
        
        private Rectangle mutuallyExclusiveTarget;
        private Rectangle mutuallyExclusiveBarTarget;
        
        private Map<Rectangle, HidingTimer> timers = new HashMap<>();
        private Map<Rectangle, HidingTimer> barTimers = new HashMap<>();
        
        
        
        public OverlappingDisplay(CompareDisplay parent) {
            super(20);
            
            this.parentContainer = parent;
            
            getStyleClass().add("compare-display-overlap");
            
            Blend blend = new Blend();
            blend.setMode(BlendMode.SOFT_LIGHT);
            
            topLabelAnchor = new AnchorPane();
            topLabelAnchor.setPrefHeight(30);
            overlapAll = new Label("Overlap All");
            overlapAll.setTextFill(Color.rgb(237, 93, 37));
            overlapAll.setManaged(false);
            overlapAll.relocate(210, 0);
            overlapAllValue = new Label();
            overlapAllValue.setFont(Font.font(10d));
            overlapAllValue.setTextFill(Color.rgb(49, 109, 160).darker());
            overlapAllValue.setManaged(false);
            overlapAllValue.setVisible(false);
            sizeLeft = new Label("Size Left");
            sizeLeft.setTextFill(Color.rgb(49, 109, 160));
            sizeLeftValue = new Label();
            sizeLeftValue.setFont(Font.font(10d));
            sizeLeftValue.setTextFill(Color.rgb(49, 109, 160).darker());
            sizeLeftValue.setManaged(false);
            sizeLeftValue.setVisible(false);
            sizeLeftValue.textProperty().addListener((v,o,n) -> {
                Text helper = new Text(n);
                Bounds b = helper.getLayoutBounds();
                Bounds b2 = sizeLeft.getBoundsInParent();
                sizeLeftValue.resizeRelocate(Math.max(5, b2.getMinX() + b2.getWidth() / 2 - b.getWidth() / 2 + 5), b2.getMaxY(),
                    b.getWidth() + 5, b.getHeight() + 5);
            });
            sizeRight = new Label("Size Right");
            sizeRight.setTextFill(Color.rgb(97, 157, 206));
            sizeRight.layoutBoundsProperty().addListener((v,o,n) -> {
                overlapAll.resize(n.getWidth() + 20, n.getHeight());
            });
            sizeRightValue = new Label();
            sizeRightValue.setFont(Font.font(10d));
            sizeRightValue.setTextFill(Color.rgb(49, 109, 160).darker());
            sizeRightValue.setManaged(false);
            sizeRightValue.setVisible(false);
            sizeRightValue.textProperty().addListener((v,o,n) -> {
                Text helper = new Text(n);
                Bounds b = helper.getLayoutBounds();
                Bounds b2 = sizeRight.getBoundsInParent();
                Bounds p = topLabelAnchor.getBoundsInLocal();
                sizeRightValue.resizeRelocate(Math.min(p.getMaxX() - 5 - b.getWidth(), b2.getMinX() + b2.getWidth() / 2 - b.getWidth() / 2 + 5), b2.getMaxY(),
                    b.getWidth() + 5, b.getHeight() + 5);
            });
            topLabelAnchor.getChildren().addAll(sizeLeft, sizeLeftValue, overlapAll, overlapAllValue, sizeRight, sizeRightValue);
            AnchorPane.setLeftAnchor(sizeLeft, 5d);
            AnchorPane.setTopAnchor(overlapAll, 0d);
            AnchorPane.setRightAnchor(sizeRight, 5d);
            olLeftRight = new Label("Overlapping Left/Right");
            olLeftRight.setTextFill(Color.rgb(237, 93, 37));
            olLeftRightValue = new Label();
            olLeftRightValue.setFont(Font.font(10d));
            olLeftRightValue.setTextFill(Color.rgb(49, 109, 160).darker());
            olLeftRightValue.setManaged(false);
            olLeftRightValue.setVisible(false);
            olLeftRightValue.textProperty().addListener((v,o,n) -> {
                Text helper = new Text(n);
                Bounds b = helper.getLayoutBounds();
                Bounds b2 = olLeftRight.getBoundsInParent();
                olLeftRightValue.resizeRelocate(Math.max(5, b2.getMinX() + b2.getWidth() / 2 - b.getWidth() / 2 + 5), b2.getMinY() - b.getHeight() - 5,
                    b.getWidth() + 5, b.getHeight() + 5);
            }); 
            olRightLeft = new Label("Overlapping Right/Left");
            olRightLeft.setTextFill(Color.rgb(237, 93, 37));
            olRightLeftValue = new Label();
            olRightLeftValue.setFont(Font.font(10d));
            olRightLeftValue.setTextFill(Color.rgb(49, 109, 160).darker());
            olRightLeftValue.setManaged(false);
            olRightLeftValue.setVisible(false);
            olRightLeftValue.textProperty().addListener((v,o,n) -> {
                Text helper = new Text(n);
                Bounds b = helper.getLayoutBounds();
                Bounds b2 = olRightLeft.getBoundsInParent();
                Bounds p = bottomLabelAnchor.getBoundsInLocal();
                olRightLeftValue.resizeRelocate(Math.min(p.getMaxX() - b.getWidth() - 5, b2.getMinX() + b2.getWidth() / 2 - b.getWidth() / 2 + 5), b2.getMinY() - b.getHeight() - 5,
                    b.getWidth() + 5, b.getHeight() + 5);
            }); 
            bottomLabelAnchor = new AnchorPane();
            bottomLabelAnchor.setPrefHeight(20);
            bottomLabelAnchor.getChildren().addAll(olLeftRight, olLeftRightValue, olRightLeft, olRightLeftValue);
            AnchorPane.setLeftAnchor(olLeftRight, 5d);
            AnchorPane.setRightAnchor(olRightLeft, 5d);
            
            overlapContainer = new Group();
            leftBar = new Rectangle();
            leftBar.setFill(Color.rgb(49, 109, 160));
            leftBar.setHeight(15);
            leftBar.setX(0);
            leftBar.setY(15);
            rightBar = new Rectangle();
            rightBar.setFill(Color.rgb(97, 157, 206));
            rightBar.setY(15);
            rightBar.setHeight(15);
            overlapBar = new Rectangle();
            overlapBar.setHeight(30);
            overlapBar.setX(0);
            overlapBar.setY(0);
            overlapBar.setFill(Color.rgb(237, 93, 37, 0.2));
            
            overlapContainer.getChildren().add(leftBar);
            overlapContainer.getChildren().add(rightBar);
            overlapContainer.getChildren().add(overlapBar);
            overlapContainer.setBlendMode(BlendMode.SRC_OVER);
            
            getChildren().addAll(topLabelAnchor, overlapContainer, bottomLabelAnchor);
            
            Platform.runLater(() -> {
                parentContainer.viewportBoundsProperty().addListener((v,o,n) -> {
                    resizeReposition(n);
                });
                
                // Size Left
                Supplier<Bounds> rectBoundsSupplier = () -> {
                    Bounds b = leftBar.getBoundsInParent();
                    //return new BoundingBox(b.getMinX(), b.getMinY(), b.getWidth() - (overlapBar.getWidth() / 2), b.getHeight());
                    return b;
                };
                addMouseEffect(sizeLeft, sizeLeftValue, rectBoundsSupplier, null, null, overlapContainer);
                addBarEffect(leftBar, rectBoundsSupplier, null, overlapContainer);
                Bounds bd = sizeLeft.getBoundsInParent();
                Bounds a = new BoundingBox(bd.getMinX() - 2.5, bd.getMinY(), bd.getWidth() + 5, bd.getHeight());
                addMouseEffect(leftBar, sizeLeftValue, () -> a, null, this, null);
                
                // Overlap All
                Supplier<Bounds> mouseBoundsSupplier = () -> {
                    Bounds b = overlapBar.getBoundsInParent();
                    return new BoundingBox(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight() - rightBar.getBoundsInParent().getHeight());
                };
                Supplier<Bounds> exp = () -> {
                    Bounds b = overlapBar.getBoundsInParent();
                    return new BoundingBox(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
                };
                addMouseEffect(overlapAll, overlapAllValue, () -> overlapBar.getBoundsInParent(), null, null, overlapContainer);
                addBarEffect(overlapBar, () -> exp.get(), mouseBoundsSupplier, overlapContainer);
                bd = overlapAll.getBoundsInParent();
                Supplier<Bounds> newOne = () -> {
                    Bounds bds = overlapAll.getBoundsInParent();
                    return new BoundingBox(bds.getMinX() - 6, bds.getMinY(), bds.getWidth(), bds.getHeight());
                };
                addMouseEffect(overlapBar, overlapAllValue, newOne, mouseBoundsSupplier, this, null);
                
                // Size Right
                rectBoundsSupplier = () -> {
                    Bounds b = rightBar.getBoundsInParent();
                    //return new BoundingBox(overlapBar.getBoundsInLocal().getMaxX(), b.getMinY(), b.getWidth() - (overlapBar.getWidth() / 2), b.getHeight());
                    return b;
                };
                addMouseEffect(sizeRight, sizeRightValue, rectBoundsSupplier, null, null, overlapContainer);
                addBarEffect(rightBar, rectBoundsSupplier, null, overlapContainer);
                Supplier<Bounds> dB = () -> {
                    // The value label never gets updated for screen size changes, so...
                    Text helper = new Text(sizeRightValue.getText());
                    Bounds b = helper.getLayoutBounds();
                    Bounds b2 = sizeRight.getBoundsInParent();
                    Bounds p = topLabelAnchor.getBoundsInLocal();
                    sizeRightValue.resizeRelocate(Math.min(p.getMaxX() - 5 - b.getWidth(), b2.getMinX() + b2.getWidth() / 2 - b.getWidth() / 2 + 5), b2.getMaxY(),
                        b.getWidth() + 5, b.getHeight() + 5);
                    
                    Bounds x = sizeRight.getBoundsInParent();
                    
                    return new BoundingBox(x.getMinX() - 2.5, x.getMinY(), x.getWidth() + 5, x.getHeight());
                };
                addMouseEffect(rightBar, sizeRightValue, dB, null, this, null);
                
                // Overlap LeftRight
                Supplier<Bounds> rectBoundsSupplierLR = () -> {
                    Bounds b = overlapBar.getBoundsInParent();
                    return new BoundingBox(b.getMinX(), b.getMinY(), b.getWidth() / 2 + 1, b.getHeight());
                };
                addMouseEffect(olLeftRight, olLeftRightValue, rectBoundsSupplierLR, null, null, overlapContainer);
                addBarEffect(overlapBar, rectBoundsSupplierLR, () -> leftBar.getBoundsInParent(), overlapContainer);
                dB = () -> {
                    Bounds x = olLeftRight.getBoundsInParent();
                    return new BoundingBox(x.getMinX() - 2.5, x.getMinY(), x.getWidth() + 5, x.getHeight());
                };
                addMouseEffect(overlapBar, olLeftRightValue, dB, () -> leftBar.getBoundsInParent(), bottomLabelAnchor, null);
                
                // Overlap RightLeft
                rectBoundsSupplier = () -> {
                    Bounds b = rightBar.getBoundsInParent();
                    Bounds b2 = overlapBar.getBoundsInParent();
                    return new BoundingBox(b.getMinX(), b2.getMinY(), b2.getWidth() / 2 - 2, b2.getHeight());
                };
                addMouseEffect(olRightLeft, olRightLeftValue, rectBoundsSupplier, null, null, overlapContainer);
                addBarEffect(overlapBar, rectBoundsSupplier, () -> rightBar.getBoundsInParent(), overlapContainer);
                dB = () -> {
                    // The value label never gets updated for screen size changes, so...
                    Text helper = new Text(olRightLeftValue.getText());
                    Bounds b = helper.getLayoutBounds();
                    Bounds b2 = olRightLeft.getBoundsInParent();
                    Bounds p = bottomLabelAnchor.getBoundsInLocal();
                    olRightLeftValue.resizeRelocate(Math.min(p.getMaxX() - b.getWidth() - 5, b2.getMinX() + b2.getWidth() / 2 - b.getWidth() / 2 + 5), b2.getMinY() - b.getHeight() - 5,
                        b.getWidth() + 5, b.getHeight() + 5);
                    
                    Bounds x = olRightLeft.getBoundsInParent();
                    
                    return new BoundingBox(x.getMinX() - 2.5, x.getMinY(), x.getWidth() + 5, x.getHeight());
                };
                addMouseEffect(overlapBar, olRightLeftValue, dB, () -> rightBar.getBoundsInParent(), bottomLabelAnchor, null);
            });
        }
        
        /**
         * Blanks the display
         */
        private void reset() {
            setMetric(CompareDisplay.DUMMY_METRIC);
        }
        
        private void addBarEffect(Node target, Supplier<Bounds> targetBounds, Supplier<Bounds> filter, Group group) {
            Rectangle r = new Rectangle();
            r.setManaged(false);
            r.setFill(Color.TRANSPARENT);
            r.setStroke(Color.DARKRED);
            r.setStrokeWidth(1);
            r.setVisible(false);
            
            group.getChildren().add(r);
            
            HidingTimer timer = getTimer(r, null, "Bar Effect");
            
            barTimers.put(r, timer);
            
            target.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
                if(filter == null || filter.get().contains(new Point2D(e.getX(), e.getY()))) {
                    if(mutuallyExclusiveBarTarget == null) {
                        mutuallyExclusiveBarTarget = r;
                    }else if(mutuallyExclusiveBarTarget != r){
                        barTimers.get(mutuallyExclusiveBarTarget).hide();
                        mutuallyExclusiveBarTarget = r;
                    }
                    
                    r.setVisible(true);
                    
                    Bounds b = targetBounds.get();
                    r.setX(b.getMinX());
                    r.setY(b.getMinY());
                    r.setWidth(b.getWidth());
                    r.setHeight(b.getHeight());
                    
                    // Record the mouse position so that we can "hold" until mouse moves
                    // out of the invoker's target area. We use this to compare the current
                    // and later mouse positions to see that the mouse hasn't moved. 
                    // (see #getTimer(Rectangle))
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    savedMouseOverBarPoint = new Point2D(p.x, p.y);
                    timer.restart();
                }
            });
            
            r.toBack();
        }
        
        /**
         * Adds a mouse-over effect to the target specified to be the invoker, and
         * produces the effect uses the specified target Bounds and mouse Bounds 
         * (if specified).
         * 
         * @param target            the invoker triggering the mouse event
         * @param targetDesc        the value label
         * @param targetBounds      the bounds where the effect is to be displayed
         * @param mouseBounds       the valid bounds of the mouse for triggering
         * @param container         Either container --or-- group is specified but not
         *                          both. This is the container to which to add the effect
         * @param group             Either container --or-- group is specified but not
         *                          both. This is the group to which to add the effect
         */
        private void addMouseEffect(Node target, Node targetDesc, Supplier<Bounds> targetBounds, Supplier<Bounds> mouseBounds, Pane container, Group group) {
            Rectangle r = new Rectangle();
            r.setManaged(false);
            r.setFill(Color.TRANSPARENT);
            r.setStroke(Color.DARKRED);
            r.setStrokeWidth(1);
            r.setVisible(false);
            
            if(group == null) {
                container.getChildren().add(r);
            }else{
                group.getChildren().add(r);
            }
            
            HidingTimer timer = getTimer(r, targetDesc, "MouseEffect");
            
            timers.put(r, timer);
            
            target.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
                if(mouseBounds == null || (mouseBounds != null && mouseBounds.get().contains(new Point2D(e.getX(), e.getY())))) {
                    
                    if(mutuallyExclusiveTarget == null) {
                        mutuallyExclusiveTarget = r;
                    }else if(mutuallyExclusiveTarget != r){
                        timers.get(mutuallyExclusiveTarget).hide();
                        mutuallyExclusiveTarget = r;
                    }
                    
                    r.setVisible(true);
                    if(targetDesc != null) {
                        targetDesc.setVisible(true);
                    }
                    Bounds b = targetBounds.get();
                    r.setX(b.getMinX());
                    r.setY(b.getMinY());
                    r.setWidth(b.getWidth());
                    r.setHeight(b.getHeight());
                    
                    // Record the mouse position so that we can "hold" until mouse moves
                    // out of the invoker's target area. We use this to compare the current
                    // and later mouse positions to see that the mouse hasn't moved. 
                    // (see #getTimer(Rectangle))
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    savedMouseOverPoint = new Point2D(p.x, p.y);
                    timer.restart();
                }
            });
        }
        
        /**
         * A timer which turns off the specified rectangle's visibility after
         * a brief delay.
         * 
         * We use the recorded mouse position so that we can "hold" until mouse moves
         * out of the invoker's target area. We use this to compare the current
         * and later mouse positions to see that the mouse hasn't moved. 
         * (see #getTimer(Rectangle))
         * 
         * @param r     the Rectangle whose visibility is managed by this timer
         * @param desc  the value label
         * @return      a timer which turns off the specified rectangle's visibility
         */
        private HidingTimer getTimer(Rectangle r, Node desc, String name) {
            HidingTimer timer = new HidingTimer(500, r, desc, name);
            timer.setRepeats(false);
            timer.setDelay(500);
            timer.start();
            
            return timer;
        }
        
        /**
         * Timer wrapper that is aware of display-specific contents that it can hide
         * upon request. 
         */
        @SuppressWarnings("serial")
        private class HidingTimer extends javax.swing.Timer {
            private Rectangle r;
            private Node desc;
            
            public HidingTimer(int delay, Rectangle r, Node desc, String name) {
                super(delay, e -> {});
                
                this.r = r;
                this.desc = desc;
                
                addActionListener(e -> {
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    Point2D p2d = new Point2D(p.x, p.y);
                    if(savedMouseOverPoint != null && savedMouseOverPoint.equals(p2d)) {
                        restart();
                    } else if(savedMouseOverBarPoint != null && savedMouseOverBarPoint.equals(p2d)) {
                        restart();
                    } else {
                        hide();
                    }
                });
            }
            
            public void hide() {
                r.setVisible(false);
                if(desc != null) {
                    desc.setVisible(false);
                }
            }
            
        }
        
        private void resizeReposition(Bounds n) {
            if(metric == null) return;
            
            double uiWidth = n.getWidth();
            double dataWidth = metric.getSizeLeft() + metric.getSizeRight();
            double uiLWidth = (metric.getSizeLeft() / dataWidth) * uiWidth;
            double uiRWidth = (metric.getSizeRight() / dataWidth) * uiWidth;
            double uiOLWidth = (metric.getOverlappingLeftRight() * uiLWidth) + (metric.getOverlappingRightLeft() * uiRWidth);
            
            leftBar.setWidth(uiLWidth);
            rightBar.setWidth(uiRWidth);
            rightBar.setX(uiLWidth);
            overlapBar.setWidth(uiOLWidth);
            overlapBar.setX(uiLWidth - (metric.getOverlappingLeftRight() * uiLWidth));
            topLabelAnchor.setMaxWidth(uiWidth);
            bottomLabelAnchor.setMaxWidth(uiWidth);
            
            Text widthHelper = new Text("Overlap All");
            double helperWidth = widthHelper.getLayoutBounds().getWidth();
            overlapAll.relocate(uiWidth / 2 - helperWidth / 2, 0);
            
            sizeLeftValue.setText("( " + metric.getSizeLeft() + " )");
            overlapAllValue.setText("( " + metric.getOverlappingAll() + " )");
            sizeRightValue.setText("( " + metric.getSizeRight() + " )");
            olLeftRightValue.setText("( " + String.format("%.7f", metric.getOverlappingLeftRight()) + " )");
            olRightLeftValue.setText("( " + String.format("%.7f", metric.getOverlappingRightLeft()) + " )");
            Text helper = new Text("( " + metric.getOverlappingAll() + " )");
            Bounds b = helper.getLayoutBounds();
            Bounds b2 = overlapAll.getBoundsInParent();
            overlapAllValue.resizeRelocate(b2.getMinX() + b2.getWidth() / 2 - b.getWidth() / 2 - 5, b2.getMaxY(),
                b.getWidth() + 5, b.getHeight() + 5);
        }
        
        private void setMetric(Metric metric) {
            this.metric = metric;
            resizeReposition(getBoundsInLocal());
            requestLayout();
        }
    }
    
    public static class HorizontalMultiAxisChart extends VBox {
        private Map<String, BarAttributes> barAttributes = new HashMap<>(); 
        private Map<String, Integer> chartIndices = new HashMap<>();
        private List<StackedBarChart<Number, String>> charts = new ArrayList<>();
        private List<Label> legends = new ArrayList<>();
        private List<ValueIndicator> valueLabels = new ArrayList<>();
        private String[] labels;
        
        private CompareDisplay parentContainer;
        private ChangeListener<Bounds> tempListener;
        
        private double mainChartHt;
        
        
        @SuppressWarnings("unchecked")
        public HorizontalMultiAxisChart(CompareDisplay parent, String[] labels) {
            this.labels = labels;
            this.parentContainer = parent;
            
            setSpacing(5);
            getStyleClass().add("compare-display-multiaxis");
            
            AnchorPane header = new AnchorPane();
            header.setPrefHeight(20);
            header.getStyleClass().add("compare-chart-header");
            Polygon arrowHead = new Polygon();
            arrowHead.getPoints().addAll(new Double[] {
               0.0, 0.0, 
               15.0, 7.0,
               0.0, 14.0
            });
            arrowHead.setFill(Color.WHITE);
            Pane arrowBody = new Pane();
            arrowBody.setPrefWidth(200);
            arrowBody.setPrefHeight(10);
            arrowBody.getStyleClass().add("compare-chart-header-arrow-body");
            arrowBody.setManaged(false);
            Label minLabel = new Label("Less Similar");
            minLabel.getStyleClass().add("compare-chart-header-label0");
            Label maxLabel = new Label("More Similar");
            maxLabel.getStyleClass().add("compare-chart-header-label1");
            AnchorPane.setLeftAnchor(minLabel, 5.0);
            AnchorPane.setRightAnchor(maxLabel, 5.0);
            header.getChildren().addAll(minLabel, maxLabel, arrowBody, arrowHead);
            getChildren().add(header);
            
            for(int i = 0;i < 3;i++) {
                final int idx = i;
                NumberAxis na = new NumberAxis(0, 1, 0.1);
                na.setFocusTraversable(false);
                na.setTickLabelFormatter(new StringConverter<Number>() {
                    @Override public String toString(Number object) {
                        return String.format("%.1f", 
                            idx != 0 ? 1.0 - object.doubleValue() : object.doubleValue());
                    }
                    @Override public Number fromString(String string) { return 0; }
                });
                
                CategoryAxis ca = new CategoryAxis(FXCollections.observableArrayList());
                ca.setFocusTraversable(false);
                ca.setTickLabelsVisible(false);
                ca.setTickMarkVisible(false);
                ca.setStartMargin(0);
                ca.setEndMargin(-5);
                
                StackedBarChart<Number, String> bc = new StackedBarChart<>(na, ca);
                bc.setFocusTraversable(false);
                bc.getStyleClass().add("compare-display-chart" + i);
                bc.setManaged(false);
                if(i == 0) {
                    List<String> l = Arrays.asList(Arrays.copyOf(labels, labels.length));
                    Collections.reverse(l);
                    ca.setCategories(FXCollections.<String> observableArrayList(l));
                } else {
                    bc.setVerticalGridLinesVisible(false);
                    bc.setHorizontalGridLinesVisible(false);
                    bc.setVerticalZeroLineVisible(false);
                    ca.setVisible(false);
                    ca.setOpacity(0.0);
                }
                bc.setTitle(null);
                
                Label name = new Label(labels[i]);
                name.setManaged(false);
                name.getStyleClass().add("compare-chart-legend" + i);
                getChildren().add(name);
                
                legends.add(name);
                
                barAttributes.put(labels[i], new BarAttributes().setDescending(i != 0 && i != 3));
                
                Polygon valuePointer = new Polygon();
                valuePointer.setManaged(false);
                valuePointer.getPoints().addAll(
                    new Double[] {
                        0.0, 0.0, 
                        2.5, 5.0,
                        -2.5, 5.0
                    });
                ValueIndicator valueLabel = new ValueIndicator(valuePointer);
                valueLabel.setManaged(false);
                valueLabel.getStyleClass().add("compare-chart-value-label");
                valueLabel.value = (i == 0 ? 2d : i == 1 ? 47d : i == 2 ? 92d : 137d);
                valueLabel.pointer = valuePointer;
                barAttributes.get(labels[i]).valueLabel = valueLabel;
                valueLabels.add(valueLabel);
                getChildren().addAll(valueLabel, valuePointer);
                                
                charts.add(bc);
            }
            
            chartIndices.put(this.labels[0], 0);
            chartIndices.put(this.labels[1], 1);
            chartIndices.put(this.labels[2], 2);
            
            XYChart.Series<Number, String> series = new XYChart.Series<>();
            XYChart.Data<Number, String> d0 = new XYChart.Data<>(0, this.labels[0]);
            XYChart.Data<Number, String> d1 = new XYChart.Data<>(0, this.labels[1]);
            XYChart.Data<Number, String> d2 = new XYChart.Data<>(0, this.labels[2]);
            series.getData().addAll(d0, d1, d2);
            series.getData().stream()
                .forEach(d -> d.nodeProperty().addListener((v,o,n) -> configureLabelForData(d.getYValue(), d)));
                    
            charts.get(0).getData().add(series);
            charts.get(0).heightProperty().addListener((v,o,n) -> {
                StackedBarChart<Number, String> mainChart = charts.get(0);
                CategoryAxis cAxis = (CategoryAxis)mainChart.getYAxis();
                Platform.runLater(() -> setMaxCategoryWidth(mainChart, cAxis, 20, 10));
            });
            parent.layoutBoundsProperty().addListener((v1,o1,n1) -> {
                double margins = 10;
                double w = n1.getWidth() - margins;
                double labelWidth = 150;
                mainChartHt = charts.get(0).getBoundsInLocal().getMaxY();
                
                charts.get(2).resizeRelocate(0, 185.0, w, 50); 
                charts.get(2).layout();
                legends.get(2).resizeRelocate(w / 2 - labelWidth / 2, mainChartHt + 115, labelWidth, 17);
                
                charts.get(1).resizeRelocate(0, 140.0, w, 50);
                charts.get(1).layout();
                legends.get(1).resizeRelocate(w / 2 - labelWidth / 2, mainChartHt + 70, labelWidth, 17);
                
                charts.get(0).resizeRelocate(0, 25.0, w, 120); 
                charts.get(0).layout();
                legends.get(0).resizeRelocate(w / 2 - labelWidth / 2, mainChartHt + 25, labelWidth, 17);
                
                double width = w + 10;
                double x = width / 2 - 200 / 2;
                arrowBody.resizeRelocate(x, 7, 200, 6);
                arrowBody.toFront();
                arrowHead.relocate(x + 198, 3);
                header.setPrefWidth(parentContainer.viewportBoundsProperty().get().getWidth());
                header.setMaxWidth(parentContainer.viewportBoundsProperty().get().getWidth());
            });
            Platform.runLater(() -> {
                parentContainer.viewportBoundsProperty().addListener(tempListener = (v,o,n) -> {
                    header.setPrefWidth(parentContainer.getBoundsInLocal().getWidth());
                    header.setMaxWidth(parentContainer.getBoundsInLocal().getWidth());
                    parentContainer.viewportBoundsProperty.removeListener(tempListener);
                });
            });
            
            List<StackedBarChart<Number, String>> l = new ArrayList<>(charts);
            Collections.reverse(l);
            getChildren().addAll(l);
        }
        
        private void reset() {
            if(!barAttributes.isEmpty()) {
                barAttributes.keySet().forEach(k -> setCategoryData(k, barAttributes.get(k).resetValue));
            }
        }
        
        private EventHandler<MouseEvent> getValueLabelHandler(Node node, Rectangle r, String key, int index) {
            javax.swing.Timer timer = new javax.swing.Timer(500, e -> {
                r.setVisible(false);
                barAttributes.get(key).valueLabel.setBackground(
                    new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.5), new CornerRadii(10), null)));
            });
            timer.setRepeats(false);
            timer.setDelay(500);
            timer.start();
            
            return (MouseEvent e) -> {
                if(e.getY() < 0  || e.getY() > 10) return;
                
                Point2D p = new Point2D(e.getX() + 10, index == 0 ? 125.0 : index == 1 ? 170 : 220.0);
                Bounds pb = barAttributes.get(key).valueLabel.getBoundsInParent();
                if(pb.contains(p)) {
                    Bounds b = node.getBoundsInParent();
                    r.setWidth(b.getWidth() - 5); 
                    r.setHeight(b.getHeight() - 5);
                    r.setVisible(true);
                    charts.get(index).requestLayout();
                    barAttributes.get(key).valueLabel.setBackground(
                        new Background(new BackgroundFill(Color.rgb(49, 109, 160), new CornerRadii(10), null)));
                    
                    timer.restart();
                }else{
                    r.setVisible(false);
                    barAttributes.get(key).valueLabel.setTextFill(Color.WHITE);
                    barAttributes.get(key).valueLabel.setBackground(
                        new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.5), new CornerRadii(10), null)));
                }
            };
        }
        
        private void configureLabelForData(String key, XYChart.Data<Number, String> data) {
            final Node node = data.getNode();
            final Text dataText = new Text(data.getXValue() + "");
            dataText.setFill(Color.BLACK);
            dataText.setVisible(false);
            barAttributes.get(key).setValueText(dataText);
            Rectangle r = new Rectangle();
            r.setManaged(false);
            r.setFill(Color.TRANSPARENT);
            r.setStroke(Color.rgb(237, 93, 37));
            r.setStrokeWidth(1);
            charts.get(chartIndices.get(key)).getXAxis().addEventHandler(
                MouseEvent.MOUSE_MOVED, getValueLabelHandler(node, r, key, chartIndices.get(key)));
            
            node.parentProperty().addListener((v,o,n) -> {
///////////////////////////////////////////////////////////////////////////////////////
//      Method for setting the value text in the bar itself. (save just in case)     //
///////////////////////////////////////////////////////////////////////////////////////                
                Group parentGroup = (Group)n;
//                parentGroup.getChildren().add(dataText);
                parentGroup.getChildren().add(r);
            });
            
            node.boundsInParentProperty().addListener((v,o,n) -> {
///////////////////////////////////////////////////////////////////////////////////////
//      Method for setting the value text in the bar itself. (save just in case)     //
///////////////////////////////////////////////////////////////////////////////////////
//                double w = dataText.getLayoutBounds().getWidth();
//                double labelLoc = n.getMaxX() + 5;
//                double xPos = Math.max(5, labelLoc - w - 10);
//                dataText.setLayoutX(Math.round(xPos));
//                dataText.setLayoutY(Math.round(n.getMinY() + 15));
                
                final BarAttributes bAttrs = barAttributes.get(key);
                String s = String.format("%.4f", bAttrs.value);
                Text t = new Text(s);
                double lw = t.getLayoutBounds().getWidth() + 10;
                bAttrs.valueLabel.setText(s);
                bAttrs.valueLabel.resizeRelocate(n.getMaxX() - (lw / 2) + 2, 
                    mainChartHt + (double)bAttrs.valueLabel.value - 2, lw, 12);
                bAttrs.valueLabel.pointer.relocate(
                    n.getMaxX(), mainChartHt + (double)bAttrs.valueLabel.value - 7);
                bAttrs.valueLabel.pointer.toFront();
                r.resizeRelocate(n.getMinX(), n.getMinY(), n.getWidth(), n.getHeight());
            });
        }
        
        private void setCategoryData(String label, double v) {
            // Invert the value if the axis is descending
            final BarAttributes bAttrs = barAttributes.get(label);
            final double value = barAttributes.get(label).isDescending ? 
                1.0 - v : label.indexOf("eight") != -1 ? v / 100d : v;
            charts.get(0).getData().get(0).getData().stream()
                .filter(s -> s.getYValue().equals(label))
                .findFirst()
                .ifPresent(d -> { 
                    d.setXValue(value);
                    if(v != bAttrs.resetValue) {
                        bAttrs.valueLabel.setVisible(true);
                        bAttrs.valueText.setText(String.format("%.8f", v));
                    }else{
                        bAttrs.valueLabel.setVisible(false);
                    }
                });
        }
        
        @SuppressWarnings("unused")
        private XYChart.Data<Number, String> getCategoryData(String label) {
            return charts.get(0).getData().get(0).getData().stream()
                .filter(s -> s.getYValue().equals(label))
                .findFirst()
                .orElse(null);
        }
        
        private void setMaxCategoryWidth(StackedBarChart<Number, String> sbc, CategoryAxis xAxis, double maxCategoryWidth, double minCategoryGap) {
            double catSpace = xAxis.getCategorySpacing();
            sbc.setCategoryGap(catSpace - Math.min(maxCategoryWidth, catSpace - minCategoryGap));
        }
       
        private class ValueIndicator extends Label {
            private double value;
            private Shape pointer;
            private ValueIndicator(Shape pointer) {
                pointer.visibleProperty().bind(this.visibleProperty());
            }
        }
        
        private class BarAttributes {
            private ValueIndicator valueLabel;
            private Text valueText;
            private boolean isDescending;
            private double resetValue;
            private double value;
            
            private BarAttributes setValueText(Text text) {
                this.valueText = text;
                this.valueText.textProperty().addListener((v,o,n) -> value = Double.parseDouble(n));
                return this;
            }

            private BarAttributes setDescending(boolean b) {
                this.isDescending = b;
                this.resetValue = isDescending ? 1.0 : 0;
                return this;
            }
        }
    }
    
    /**
     * Adds the "flip-over" info invoking button to this view.
     */
    private void addInfoButton() {
        Window w = WindowService.getInstance().windowFor(this);
        ObjectProperty<Point2D> infoLoc = new SimpleObjectProperty<>();
        w.layoutBoundsProperty().addListener((v,o,n) -> {
            infoLoc.set(new Point2D(n.getWidth() - 35, 5));
        });
        getChildren().add(WindowService.createInfoButton(w, this, infoLoc));
    }
}
