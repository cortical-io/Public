package io.cortical.iris.view.input.expression;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.iris.ui.util.DragAssistant;
import javafx.geometry.Point2D;
import javafx.scene.control.PopupControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;


/**
 * Version of {@link Bubble} used for drag-n-drop operations.
 */
public class FingerprintBubble extends ToggleButton implements Bubble {
    private int[] positions;
    private String positionString;
    private Image source;
    private PopupControl popup;
    private TextFlow flow;
    
    /**
     * Constructs a new {@code FingerprintBubble}
     * @param pos
     */
    public FingerprintBubble(int[] pos) {
        this.positions = pos;
        
        this.positionString = Arrays.stream(pos).mapToObj(i -> String.valueOf(i)).collect(Collectors.joining(","));
        
        getStyleClass().setAll("fingerprint-bubble");
        
        setPrefWidth(50);
        setPrefHeight(30);
        
        setGraphic(new ImageView("fingerprint_exp.png"));
        
        setFocusTraversable(false);
        
        popup = new PopupControl();
        flow = new TextFlow();
        flow.setTextAlignment(TextAlignment.JUSTIFY);
        Text t = new Text("Source Expression\n\n");
        t.setFill(Color.WHITE);
        t.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        flow.getChildren().add(t);
        Text t2 = new Text("Not Available...");
        t2.setFill(Color.rgb(237, 93, 37));
        t2.setFont(Font.font(14));
        flow.getChildren().add(t2);
        StackPane popupRootPane = new StackPane();
        popupRootPane.getChildren().add(flow);
        popupRootPane.setStyle(
            "-fx-background-radius: 5 5 5 5;" +
            "-fx-background-color: rgb(70, 140, 199, 0.8);" +
            "-fx-padding: 10 10 10 10;"
        );
        
        popup.getScene().setRoot(popupRootPane);
        
        DragAssistant.configureDragHandler(this);
    }
    
    /**
     * Returns a comma separated string of integers
     * @return
     */
    public String getPositionsString() {
        return positionString;
    }
    
    /**
     * Returns the fingerprint's positions.
     * @return
     */
    public int[] getPositions() {
        return positions;
    }

    @Override
    public Type getType() {
        return Bubble.Type.FINGERPRINT;
    }
    
    public void showPopup(MouseEvent e) {
        double wx = getScene().getWindow().getX();
        double wy = getScene().getWindow().getY();
        Point2D p = new Point2D(e.getX(), e.getY());
        p = localToScene(p);
        popup.show(getScene().getWindow(), p.getX() + wx + 20, p.getY() + wy + 20);
    }
    
    public void hidePopup() {
        popup.hide();
    }

    /**
     * Sets the JSON which created the source fingerprint for this bubble.
     * @param json
     */
    public void setSourceExpression(Image image) {
        this.source = image;
        if(source != null) {
            ImageView iv = new ImageView(image) {
                public double getBaselineOffset() {
                    return 5;
                }
            };
            flow.getChildren().remove(1);
            flow.getChildren().add(iv);
        }
    }
    
    /**
     * Returns the JSON which created the source fingerprint for this bubble.
     * @return
     */
    public Image getSourceExpression() {
        return source;
    }
}
