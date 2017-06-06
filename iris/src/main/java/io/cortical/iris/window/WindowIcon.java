package io.cortical.iris.window;

import java.util.ArrayList;
import java.util.List;

import io.cortical.iris.WindowService;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.util.Duration;


public class WindowIcon extends Pane {
    private Window reference;
    private Text reusableText = new Text();
    private Rectangle fill;
    private Label iconLabel;
    private Pane colorId;
    private Shape arrow;
    
    private Timeline tl = new Timeline();
    private Interpolator interpolator = Interpolator.SPLINE(0.5, 0.1, 0.1, 0.5);
   
    private List<Runnable> animationQueue = new ArrayList<>();
    
    
    
    public WindowIcon(Window w) {
        this.reference = w;
        
        setManaged(false);
        setFocusTraversable(false);
        getStyleClass().add("window-icon");
        
        colorId = new Pane();
        colorId.setManaged(false);
        if(w.isInput()) {
            colorId.setBackground(new Background(new BackgroundFill(
                w.getTitleBar().getColorIDTab().colorIDProperty().get(), new CornerRadii(10d, 0d, 0d, 10d, false), null)));
            
            w.getTitleBar().getColorIDTab().colorIDProperty().addListener((v,o,n) -> {
                colorId.setBackground(new Background(new BackgroundFill(n, new CornerRadii(10d, 0d, 0d, 10d, false), null)));
            });
        }
        
        String text = WindowService.getInstance().windowTitleFor(reference).getWindowName();
        
        iconLabel = createIconLabel(text);
        
        w.getTitleBar().titleSetProperty().addListener((v,o,n) -> {
            reusableText.setText(n);
            Bounds b = reusableText.getLayoutBounds();
            resize(24 + b.getWidth(), 25);
            setPrefSize(24 + b.getWidth(), 25d);
            
            // Fill & Arrow are created statically with the width in mind - so they have to be
            // recreated when the title forces a width change.
            getChildren().removeAll(colorId, iconLabel, fill, arrow);
            getChildren().addAll(colorId, iconLabel = createIconLabel(n), fill = createFill(), arrow = createArrow());
        });
        
        layoutBoundsProperty().addListener((v,o,n) -> {
            colorId.resizeRelocate(0, 0, 10, getHeight());
            iconLabel.resizeRelocate(
                15, 0, 100, getHeight());
        });
        
        reusableText.setText(text);
        Bounds bnds = reusableText.getLayoutBounds();
        resizeRelocate(w.getLayoutX(), w.getLayoutY(), 24 + bnds.getWidth(), 25);
        setPrefSize(24 + bnds.getWidth(), 25d);
        
        fill = createFill();
        
        arrow = createArrow();
        
        fill.setVisible(false);
        arrow.setVisible(false);
        
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            doPlay(fill, arrow, false);
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            doPlay(fill, arrow, true);
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            w.deIconize();
        });
        
        getChildren().addAll(colorId, iconLabel, fill, arrow);
    }
    
    /**
     * Returns this {@code WindowIcon}'s name Label.
     * @return  this {@code WindowIcon}'s name Label.
     */
    public Label getIconLabel() {
        return iconLabel;
    }
    
    /**
     * Returns this {@code WindowIcon} to its normal state 
     * (non-mouse over state).
     */
    public void setNormalState() {
        fill.setOpacity(0.0);
        arrow.setOpacity(0.0);
        fill.setVisible(false);
        arrow.setVisible(false);
    }
    
    private Label createIconLabel(String text) {
        Label iconLabel = new Label(text);
        iconLabel.setPadding(new Insets(0, 0, 2, 0));
        iconLabel.setTextFill(Color.BLACK);
        iconLabel.setManaged(false);
        
        reusableText.setText(text);
        Bounds bnds = reusableText.getLayoutBounds();
        iconLabel.resize(bnds.getWidth(), iconLabel.getHeight());
        iconLabel.setPrefSize(bnds.getWidth(), iconLabel.getHeight());
        
        return iconLabel;
    }
    
    private Rectangle createFill() {
        Rectangle fill = new Rectangle(2, 2, getWidth() - 4, getHeight() - 4);
        fill.setManaged(false);
        fill.setArcHeight(20);
        fill.setArcWidth(20);
        fill.setFill(Color.rgb(0, 0, 0, 0.9));
        
        return fill;
    }
    
    private Shape createArrow() {
        Polygon arrowHead = new Polygon();
        arrowHead.setManaged(false);
        arrowHead.getPoints().addAll(new Double[]{
            0.0, 0.0,
            -5.0, 5.0,
            5.0, 5.0,
            0.0, 0.0});
        arrowHead.setFill(Color.WHITE);
        
        Rectangle arrowBody = new Rectangle(0, 5, 5, 10);
        arrowBody.setManaged(false);
        arrowBody.setX(-2.5);
        arrowBody.setY(5);
        arrowBody.setFill(Color.WHITE);
        
        Shape shape = Shape.union(arrowHead, arrowBody);
        shape.layoutXProperty().bind(widthProperty().divide(2).subtract(2.5));
        shape.setFill(Color.WHITE);
        shape.setLayoutY(5);
        shape.toFront();
        
        return shape;
    }
    
    private void doPlay(Rectangle overlay, Shape arrow, boolean isReverse) {
        if(tl.getStatus() == Animation.Status.RUNNING) {
            if(animationQueue.size() > 0) animationQueue.clear();
            animationQueue.add(() -> { doPlay(overlay, arrow, isReverse); });
            return;
        }
        
        overlay.setVisible(true);
        arrow.setVisible(true);
        DoubleProperty overlayOpacity = new SimpleDoubleProperty(isReverse ? 1.0 : 0.0);
        DoubleProperty arrowOpacity = new SimpleDoubleProperty(isReverse ? 1.0 : 0.0);
        
        overlay.opacityProperty().bind(overlayOpacity);
        arrow.opacityProperty().bind(arrowOpacity);
        
        double end1 = isReverse ? 0.0 : 1.0;
        double end2 = isReverse ? 0.0 : 1.0;
        
        KeyValue keyValue = new KeyValue(overlayOpacity, end1, interpolator);
        KeyValue keyValue2 = new KeyValue(arrowOpacity, end2, interpolator);
        // create a keyFrame with duration 500ms
        KeyFrame keyFrame = new KeyFrame(Duration.millis(250), keyValue, keyValue2);
        // erase last keyframes: forward & reverse have different frames
        tl.getKeyFrames().clear();
        // add the keyframe to the timeline
        tl.getKeyFrames().add(keyFrame);
        // remove binding above after animation is finished
        tl.setOnFinished((e) -> {
            Platform.runLater(() -> {
                overlay.opacityProperty().unbind();
                arrow.opacityProperty().unbind();
                if(isReverse) {
                    overlay.setVisible(false);
                    arrow.setVisible(false);
                }
                if(animationQueue.size() > 0) {
                    animationQueue.remove(0).run();
                }
            });
        });
        
        tl.play();
    }
}
