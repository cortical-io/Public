package io.cortical.iris.view.output;

import java.util.Collections;
import java.util.List;

import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.ui.custom.richtext.RichTextArea;
import io.cortical.iris.ui.custom.widget.bubble.Tag;
import io.cortical.iris.ui.util.Contrast;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.window.Window;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;


public class KeywordsDisplay extends LabelledRadiusPane implements View {
    public final ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();
    public final BooleanProperty dirtyProperty = new SimpleBooleanProperty();
    
    private VBox container;
    
    private RichTextArea area;
    private FlowPane tags;
    
    private boolean isFirstLayout = true;
    
    private Region backPanel;
    
    
    

    public KeywordsDisplay(String label, NewBG spec) {
        super(label, spec);
        
        setVisible(false);
        setUserData(ViewType.KEYWORDS);
        setManaged(false);
        
        container = new VBox(5);
        container.setLayoutY(labelHeightProperty().add(5).get());
        container.getChildren().add(tags = createKeywordsList(Collections.emptyList()));
        container.getChildren().add(area = createOutputTextArea());
        container.prefWidthProperty().bind(widthProperty().subtract(10));
        container.prefHeightProperty().bind(heightProperty().subtract(labelHeightProperty().add(10)));
        container.setLayoutX(5);
        container.layoutBoundsProperty().addListener((v,o,n) -> {
            area.prefHeightProperty().set(n.getHeight() - tags.getLayoutBounds().getHeight() - 10);
        });
        VBox.setMargin(area, new Insets(0,5,0,5));
        
        getChildren().add(container);
        
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
     * Returns a flag indicating whether the layout is the first time.
     * @return
     */
    public boolean isFirstLayout() {
        if(isFirstLayout) {
            isFirstLayout = false;
            return true;
        }
        
        return isFirstLayout;
    }
    
    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        area.clear();
        tags.getChildren().clear();
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
            backPanel = new KeywordsBackPanel(10).getScroll();
        }
        
        return backPanel;
    }
    
    public FlowPane createKeywordsList(List<String> keyWords) {
        FlowPane pane = new FlowPane(5.0, 10.0);
        pane.setPadding(new Insets(5,5,5,5));
        keyWords.stream().forEach(t -> pane.getChildren().add(new Tag(t)));
        return pane;
    }
    
    /**
     * Creates the {@link RichTextArea} used to display the keywords
     * of the given text.
     * @return
     */
    public RichTextArea createOutputTextArea() {
        RichTextArea area = new RichTextArea(false);
        area.getStyleClass().addAll("keywords-text-no-controls-area");
        area.getStyledArea().setOpaqueInsets(new Insets(5,5,5,5));
        area.setFocusTraversable(false);
        area.prefWidthProperty().bind(widthProperty().subtract(10));
        area.setLayoutX(5);
        return area; 
    }

    /**
     * Sets the text and list of keywords and their style colors.
     * 
     * @param text          the body of text from which keywords are identified
     * @param keyWords      the keyword list
     * @param c             the background color highlight for the keywords
     */
    public void setKeywords(String text, List<String> keyWords, Color c) {
        area.setText(text);
        keyWords.stream().forEach(kw -> area.setStyle(kw, Contrast.get(c).color(), c));
        tags = createKeywordsList(keyWords);
        container.getChildren().remove(0);
        container.getChildren().add(0, tags);
        tags.prefWidthProperty().bind(container.widthProperty());
        Platform.runLater(() -> {
            area.getStyledArea().getVirtualFlow().scrollYToPixel(0);
            area.resize(container.getWidth() - 10, container.getHeight() - tags.getLayoutBounds().getHeight() - 10);
            requestLayout();
        });
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
