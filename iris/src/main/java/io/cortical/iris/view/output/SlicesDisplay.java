package io.cortical.iris.view.output;

import java.util.List;

import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.Window;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Output display under the "slices" tab which shows the semantic slices
 * of a given text supplied by the "Text" tab of an {@link InputWindow}.
 * 
 * @author cogmission
 * @see InputWindow
 * @see View
 */
public class SlicesDisplay extends LabelledRadiusPane implements View {
    
    public final ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();
    
    private ScrollPane slicesScroll;
    private SliceDisplay sliceDisplay;
    
    private Region backPanel;

    
    /**
     * Constructs a new {@SlicesDisplay} view.
     * @param label
     * @param spec
     */
    public SlicesDisplay(String label, NewBG spec) {
        super(label, spec);
        
        setVisible(false);
        setUserData(ViewType.SLICES);
        setManaged(false);
        
        VBox display = new VBox(10);
        
        Text description = new Text("Displays the semantic \"slices\" of the text input.");
        description.setManaged(false);
        description.setFocusTraversable(false);
        description.setStyle("-fx-font-size: 16; -fx-font-style: italic; -fx-font-weight: bold;");
        getChildren().add(description);
        
        slicesScroll = new ScrollPane();
        slicesScroll.getStyleClass().add("token-scroll");
        slicesScroll.setPadding(new Insets(10, 10, 5, 5));
        slicesScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        slicesScroll.setManaged(false);
        display.getChildren().add(slicesScroll);
        getChildren().add(display);
        
        layoutBoundsProperty().addListener((v,o,n) -> {
            description.relocate((n.getWidth() / 2) - (description.getLayoutBounds().getWidth() / 2), 5);
            
            slicesScroll.relocate(15, 70);
            slicesScroll.resize(n.getWidth() - 20, n.getHeight() - 80);
        });
        
        sliceDisplay = new SliceDisplay();
        slicesScroll.setContent(sliceDisplay);
        
        Platform.runLater(() -> addInfoButton());
    }
    
    /**
     * Implemented by {@code View} subclasses to handle an error
     * 
     * @param	context		the error information container
     */
    @Override
    public void processRequestError(RequestErrorContext context) {}
    
    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        sliceDisplay.clear();
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
            backPanel = new SlicesBackPanel(10).getScroll();
        }
        
        return backPanel;
    }
    
    /**
     * Called by the server messaging mechanism to populate this view.
     * @param slices
     */
    public void setResponseSlices(List<io.cortical.retina.model.Text> slices) {
        SliceText.IDX = 1;
        sliceDisplay.clear();
        slices.stream().forEach(sl -> sliceDisplay.addSliceText(new SliceText(sl)));
    }
    
    /**
     * Internal container node which houses the display nodes for the slice view.
     */
    private class SliceDisplay extends VBox {
        /**
         * Constructs a new {@code SliceDisplay}
         */
        public SliceDisplay() {
            super(10);
            
            SlicesDisplay.this.layoutBoundsProperty().addListener((v,o,n) -> {
                getChildren().stream().forEach(c -> ((Wrappable)c).layoutWidth(n.getWidth() - 40));
            });
        }
        
        /**
         * Adds the specified {@code SliceText} to this {@code SliceDisplay}
         * @param sliceText the {@link SliceText} object ot add.
         */
        public void addSliceText(SliceText sliceText) {
            getChildren().add(sliceText);
        }
        
        /**
         * Instructs this {@code SliceDisplay} to "clear" its content.
         */
        public void clear() {
            getChildren().removeAll(getChildren());
            SliceText.IDX = 1;
        }
    }
    
    /**
     * Interface implemented by all occupants of the {@code SlicesDisplay} for the
     * purposes of receiving width adjust commands
     */
    private interface Wrappable {
        public void layoutWidth(double width);
    }
    
    /**
     * An individual display node which displays the view of a single Slice.
     */
    private static class SliceText extends VBox implements Wrappable {
        private static int IDX = 1;
        
        private Text text;
        
        private int sliceIdx;
        
        public SliceText(io.cortical.retina.model.Text modelText) {
            this.text = new Text(modelText.getText());
            getStyleClass().add("original-text");
            sliceIdx = IDX++;
            
            getChildren().addAll(getIndexedDisplay(), this.text);
        }
        
        private Pane getIndexedDisplay() {
            Pane indexedDisplay = new Pane();
            indexedDisplay.setLayoutX(0);
            indexedDisplay.setLayoutY(0);
            indexedDisplay.getStyleClass().add("slice-display");
            indexedDisplay.setManaged(false);
                        
            Text text = new Text("Slice " + sliceIdx);
            text.getStyleClass().add("slice-display-text");
            text.setFont(Font.font("Helvetica", FontWeight.NORMAL, 11));
            text.setTextOrigin(VPos.CENTER);
            text.xProperty().bind(indexedDisplay.widthProperty().divide(2).subtract(text.getLayoutBounds().getWidth() / 2.0));
            text.yProperty().bind(indexedDisplay.heightProperty().divide(2));
            indexedDisplay.resize(text.getLayoutBounds().getWidth() + 20, text.getLayoutBounds().getHeight() * 1.5); 
            
            indexedDisplay.getChildren().add(text);
            
            return indexedDisplay;
        }
        
        public void layoutWidth(double width) {
            text.setWrappingWidth(width);
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
