package io.cortical.iris.view.output.compare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.cortical.iris.WindowService;
import io.cortical.iris.ui.Overlay;
import io.cortical.iris.ui.util.DragAssistant;
import io.cortical.iris.view.output.CompareDisplay;
import io.cortical.iris.view.output.CompareDisplay.Comparison;
import io.cortical.iris.window.Window;
import io.cortical.iris.window.WindowGroup;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Triple Fingerprint display showing comparison fingerprints together with
 * overlayed view of comparison with different colors representing "left"
 * and "right" fingerprints positions.
 * 
 * @author cogmission
 * @see ImpressionPane
 * @see CompareDisplay
 */
public class CompareImpressionDisplay extends StackPane {
    private ImpressionPane primaryImpression;
    private ImpressionPane compareImpression;
    private ImpressionPane secondaryImpression;
    
    private int[] primarySDR;
    private int[] secondarySDR;
    private int[] compareSDR;
    
    /** The queue of delayed Comparison Metric updates. */
    private List<Runnable> commandQueue = new ArrayList<>();
    
    HBox impressions;
    
    private Comparison lastComparison;
    
    private Rectangle overlapBar;
    private Rectangle overlapBarFill;
    private HBox overlapMetricSelection;
    private ToggleGroup overlapMetricButtonGroup;
    private String selectedMetric;
    private String[] buttonTexts = { "Raw", "Cosine Similarity", "Euclidean Distance", "Jaccard Distance" };
    private StringProperty selectedMetricProperty;
    private StringProperty selectedMetricDescProperty = new SimpleStringProperty();
    
    private Label overlapBarDesc;
    private IntegerProperty overlapPercent = new SimpleIntegerProperty(0);
    
    private Timeline tl = new Timeline();
    private Interpolator interpolator = Interpolator.EASE_OUT;
    
    private WindowGroup windowGroup;
    
    private ChangeListener<String> primaryTitleListener;
    private ChangeListener<String> secondaryTitleListener;
    
    
    /**
     * Creates a new {@code CompareImpressionDisplay}
     */
    public CompareImpressionDisplay() {
        primaryImpression = new ImpressionPane("Text 1", "rgb(70, 140, 199)", "rgb(49, 109, 160)", "rgb(26, 73, 94)");
        compareImpression = new ImpressionPane("Combined", "rgb(90, 90, 90)", "rgb(188, 72, 27)", "rgb(237, 93, 37)");
        secondaryImpression = new ImpressionPane("Text 2", "rgb(70, 140, 199)", "rgb(49, 109, 160)", "rgb(47, 189, 252)");
        
        DragAssistant.configureCompareImpressionDisplayDragHandler(primaryImpression.getImpression());
        DragAssistant.configureCompareImpressionDisplayDragHandler(compareImpression.getImpression());
        DragAssistant.configureCompareImpressionDisplayDragHandler(secondaryImpression.getImpression());
        
        overlapBar = new Rectangle();
        overlapBar.setManaged(false);
        overlapBar.setHeight(15);
        Text helper = new Text("100% overlap (based on cosine similarity)");
        helper.setFont(Font.font(14d));
        compareImpression.boundsInParentProperty().addListener((v,o,n) -> {
            Bounds b = impressions.localToParent(n);
            
            overlapBar.setWidth(b.getWidth());
            overlapBar.relocate(b.getMinX(), b.getMinY() - 60);
            overlapBarFill.relocate(b.getMinX(), b.getMinY() - 60);
            overlapBarFill.toFront();
            overlapBar.setFill(Color.TRANSPARENT);
            overlapBar.setStroke(Color.rgb(237, 93, 37));
            double descWidth = helper.getBoundsInLocal().getWidth();
            overlapBarDesc.resizeRelocate(b.getMinX() + ((b.getWidth() / 2) - (descWidth / 2)), b.getMinY() - 40, b.getWidth(), 15);
            overlapMetricSelection.resizeRelocate((b.getMinX() + (b.getWidth() / 2)) - ((b.getWidth() + 55) / 2), b.getMinY() - 100, b.getWidth() + 55, 20);
        });
        
        overlapBarFill = new Rectangle();
        overlapBarFill.setHeight(15);
        overlapBarFill.setFill(Color.rgb(237, 93, 37));
        overlapBarFill.setStroke(Color.rgb(237, 93, 37));
        overlapBarFill.setManaged(false);
        overlapBarFill.setWidth(0);
        
        overlapBarDesc = new Label();
        overlapBarDesc.textProperty().bind(selectedMetricDescProperty);
        overlapBarDesc.setTextFill(Color.rgb(237, 93, 37));
        overlapBarDesc.setFont(Font.font(14d));
        overlapBarDesc.setManaged(false);
        
        overlapMetricSelection = new HBox(10);
        overlapMetricSelection.getStyleClass().addAll("compare-impression-display-metric-buttons");
        overlapMetricSelection.setManaged(false);
        overlapMetricButtonGroup = new ToggleGroup();
        
        ToggleButton[] btns = Arrays.stream(buttonTexts)
            .map(t -> new ToggleButton(t))
            .toArray(ToggleButton[]::new);
        
        Arrays.stream(btns)
            .forEach(b -> {
                overlapMetricButtonGroup.getToggles().add(b);
                overlapMetricSelection.getChildren().add(b);
            });
        
        selectedMetric = buttonTexts[1];
        
        selectedMetricProperty = new SimpleStringProperty(selectedMetric);
        
        overlapPercent.addListener((v,o,n) -> { 
            selectedMetricDescProperty.set(
                overlapPercent.asString("%s%% overlap (based on ").get() + 
                selectedMetricProperty.get() + ")"); 
        });
        selectedMetricProperty.addListener((v,o,n) -> { 
            selectedMetricDescProperty.set(
                overlapPercent.asString("%s%% overlap (based on ").get() + 
                selectedMetricProperty.get() + ")"); 
        });
        
        overlapMetricButtonGroup.selectedToggleProperty().addListener((v,o,n) -> {
            selectedMetric = ((ToggleButton)n).getText();
            selectedMetricProperty.set(selectedMetric);
            if(lastComparison != null) {
                setOverlap(metricForText(selectedMetric));
            }
        });
        
        overlapMetricButtonGroup.selectToggle(btns[1]);
        
        impressions = new HBox(10);
        impressions.getStyleClass().add("compare-impression-display-internal-pane");
        impressions.getChildren().addAll(primaryImpression, compareImpression, secondaryImpression);
        
        addVisiblityHandler();
        
        getChildren().addAll(overlapMetricSelection, overlapBar, overlapBarFill, overlapBarDesc, impressions);
    }
    
    /**
     * Sets the {@link WindowGroup} controller comparison window selection
     * events and their handling.
     * 
     * @param group     the {@code WindowGroup}
     */
    public void initWindowGroup(WindowGroup group) {
        this.windowGroup = group;
        
        primaryTitleListener = (v,o,n) -> {
            primaryImpression.setTitle(n == null ? "None Selected" : n);
            primaryImpression.requestLayout();
        };
        
        secondaryTitleListener = (v,o,n) -> {
            secondaryImpression.setTitle(n == null ? "None Selected" : n);
            secondaryImpression.requestLayout();
        };
        
        Platform.runLater(() -> {
            windowGroup.primaryWindowProperty().addListener(getGroupChangedHandler(true));
            windowGroup.secondaryWindowProperty().addListener(getGroupChangedHandler(false));
        });
        
        primaryImpression.setCompareContext(windowGroup);
        secondaryImpression.setCompareContext(windowGroup);
        compareImpression.setCompareContext(windowGroup);
    }
    
    /**
     * Returns an initialized listener for dynamic title setting of the 
     * implied {@link ImpressionPane} title.
     * 
     * @param isPrimary     flag to designate which handler to initialize and return.
     * @return  the appropriate handler
     */
    private ChangeListener<Window> getGroupChangedHandler(boolean isPrimary) {
        if(isPrimary && windowGroup.primaryWindowProperty().get() != null) {
            primaryImpression.setTitle(windowGroup.primaryWindowProperty().get().getTitleBar().getTitleField().getText());
            windowGroup.primaryWindowProperty().get().getTitleBar().getTitleField().textProperty().removeListener(primaryTitleListener);
            windowGroup.primaryWindowProperty().get().getTitleBar().getTitleField().textProperty().addListener(primaryTitleListener);
        } else if(!isPrimary && windowGroup.secondaryWindowProperty().get() != null) {
            secondaryImpression.setTitle(windowGroup.secondaryWindowProperty().get().getTitleBar().getTitleField().getText());
            windowGroup.secondaryWindowProperty().get().getTitleBar().getTitleField().textProperty().removeListener(secondaryTitleListener);
            windowGroup.secondaryWindowProperty().get().getTitleBar().getTitleField().textProperty().addListener(secondaryTitleListener);
        }
        
        return (v,o,n) -> {
            // Use the "primaryTitleListener" to indicate listener initialization
            if(o != null) {
                o.getTitleBar().getTitleField().textProperty().removeListener(isPrimary ? primaryTitleListener : secondaryTitleListener);
                
                // Set De-Selection Text
                if(isPrimary) {
                    primaryImpression.setTitle("None Selected");
                } else {
                    secondaryImpression.setTitle("None Selected");
                }
            }
            
            if(n != null) {
                if(isPrimary && windowGroup.primaryWindowProperty().get() != null) {
                    primaryImpression.setTitle(windowGroup.primaryWindowProperty().get().getTitleBar().getTitleField().getText());
                } else if(!isPrimary && windowGroup.secondaryWindowProperty().get() != null) {
                    secondaryImpression.setTitle(windowGroup.secondaryWindowProperty().get().getTitleBar().getTitleField().getText());
                }
                
                n.getTitleBar().getTitleField().textProperty().addListener(isPrimary ? primaryTitleListener : secondaryTitleListener);
            }
        };
    }
    
    /**
     * Returns a string containing the name of the currently 
     * selected metric.
     * 
     * @return  currently selected metric description
     */
    public String getSelectedMetric() {
        return selectedMetric;
    }
    
    /**
     * Returns the metric measurement for the specified metric type.
     * 
     * @param text  the metric type mapped to the desired results
     * @return
     */
    private double metricForText(String text) {
        if(lastComparison == null) {
            return 0;
        }
        
        switch(text) {
            case "Raw": {
                long m = Stream.of(
                    Arrays.stream(lastComparison.getFP1().getPositions()), 
                        Arrays.stream(lastComparison.getFP2().getPositions()))
                .flatMapToInt(Function.identity())
                .distinct()
                .count();
                
                return (double)compareSDR.length / ((double)m);
            }
            case "Cosine Similarity" : return lastComparison.getMetric().getCosineSimilarity();
            case "Euclidean Distance" : return Math.max(0, 1.0 - lastComparison.getMetric().getEuclideanDistance());
            case "Jaccard Distance" : return Math.max(0, 1.0 - lastComparison.getMetric().getJaccardDistance());
            default: return lastComparison.getMetric().getCosineSimilarity();
        }
    }
    
    public void clearImpressions() {
        Platform.runLater(() -> { 
            primaryImpression.getImpression().clear();
            compareImpression.getImpression().clear();
            secondaryImpression.getImpression().clear();
        });
    }
    
    /**
     * Adds the visibility property handler which checks the command queue
     * for commands to run when the screen becomes visible.
     */
    private void addVisiblityHandler() {
        Overlay overlay = WindowService.getInstance().getContentPane().getOverlay();
        overlay.visibleProperty().addListener((v,o,n) -> {
            if(n && !commandQueue.isEmpty() && overlay.getDialog() == CompareImpressionDisplay.this) {
                (new Thread(() -> {                    
                    try { Thread.sleep(500); }catch(Exception e) { e.printStackTrace(); }
                    
                    Platform.runLater(() -> {
                        for(Runnable r : commandQueue) {
                            r.run();
                        }
                        commandQueue.clear();
                    });
                })).start();
            } else if(overlay.getDialog() == CompareImpressionDisplay.this) {
                overlapBarFill.setWidth(0);
                
                if(n) { // Can happen if visible and the command queue is empty
                    setOverlap(metricForText(getSelectedMetric()));
                }
            }
        });
    }
    
    public void setOverlap(double metric) {
        if(metric == 0) {
            System.out.println("setOverlap received null Comparison object!");
            return;
        }
        
        if(tl.getStatus() == Animation.Status.RUNNING) return;
        
        overlapBarFill.setWidth(0);
        overlapPercent.set(0);
        
        (new Thread(() -> {
            // Insert Delay so animation isn't encumbered by node clean up above
            try { Thread.sleep(500); } catch(Exception e) {}
            
            Platform.runLater(() -> {
                KeyValue keyValue = new KeyValue(
                    overlapBarFill.widthProperty(), 
                        metric * overlapBar.getWidth(), 
                            interpolator);
                KeyValue keyValue2 = new KeyValue(overlapPercent, metric * 100);
                // create a keyFrame with duration 500ms
                KeyFrame keyFrame = new KeyFrame(Duration.millis(1500), keyValue, keyValue2);
                // erase last keyframes: forward & reverse have different frames
                tl.getKeyFrames().clear();
                // add the keyframe to the timeline
                tl.getKeyFrames().add(keyFrame);
                // remove binding above after animation is finished
                tl.setOnFinished((e) -> {
                    Platform.runLater(() -> {
                        
                    });
                });
                
                tl.play();
            });
        })).start();
        
    }
    
    public void populate(Comparison c) {
        lastComparison = c == null ? lastComparison : c;
        
        if(c.getFP1() == null || c.getFP1().getPositions() == null || c.getFP1().getPositions().length < 1 ||
           c.getFP2() == null || c.getFP2().getPositions() == null || c.getFP2().getPositions().length < 1) {
            System.out.println("GOT COMPARISON WITH NO POSITIONS!!!!");
            return;
        }
        
        Overlay overlay = WindowService.getInstance().getContentPane().getOverlay();
        if(!overlay.isVisible() || overlay.getDialog() != this) {
            clearImpressions();
            commandQueue.clear();
            commandQueue.add(() -> {
                Platform.runLater(() -> {
                    setOverlap(metricForText(getSelectedMetric()));
                    this.primarySDR = c.getFP1().getPositions();
                    primaryImpression.setPrimarySDR(primarySDR);
                    setSecondarySDR(c.getFP2().getPositions());
                });
            });
            return;
        }
        
        Platform.runLater(() -> {
            setOverlap(metricForText(getSelectedMetric()));
            this.primarySDR = c.getFP1().getPositions();
            primaryImpression.setPrimarySDR(primarySDR);
            setSecondarySDR(c.getFP2().getPositions());
        });
    }
    
    public void setPrimarySDR(int[] sdr) {
        this.primarySDR = sdr;
        
        primaryImpression.setPrimarySDR(primarySDR);
        
        if(secondarySDR != null) {
            setCompareSDR();
        }
    }
    
    public void setSecondarySDR(int[] sdr) {
        this.secondarySDR = sdr;
        
        secondaryImpression.setSecondarySDR(secondarySDR);
        
        if(primarySDR != null) {
            setCompareSDR();
        }
    }
    
    private void setCompareSDR() {
        if(primarySDR == null || primarySDR.length < 1 || secondarySDR == null || secondarySDR.length < 1) {
            throw new IllegalStateException("Primary or Secondary SDR was null, therfore couldn't populate compare fingerprint!");
        }
        
        Set<Integer> intermediate = Arrays.stream(primarySDR)
            .boxed()
            .collect(Collectors.toSet());
        
        compareSDR = Arrays.stream(secondarySDR)
            .boxed()
            .filter(s -> intermediate.contains(s))
            .mapToInt(i -> i)
            .toArray();
        
        compareImpression.setPrimarySDR(primarySDR);
        compareImpression.setSecondarySDR(secondarySDR);
        compareImpression.setCompareSDR(compareSDR);
    }

    public static void main(String[] args) {
        int[] i = { 0, 1, 2, 3 };
        int[] ii = { 2, 3, 4, 5 };
        long m = Stream.of(
                Arrays.stream(i), 
                    Arrays.stream(ii))
            .flatMapToInt(Function.identity())
            .distinct()
            .count();
        
        Set<Integer> n = Stream.of(
                Arrays.stream(i), 
                    Arrays.stream(ii))
            .flatMapToInt(Function.identity())
            .boxed()
            .distinct()
            .collect(Collectors.toCollection(LinkedHashSet::new));
        
        System.out.println("count = " + m + " == " + n);
    }
   
}
