package io.cortical.iris.ui;

import io.cortical.iris.ui.custom.widget.ControlAccordion;
import io.cortical.iris.ui.custom.widget.ControlAccordionSkin;
import io.cortical.iris.ui.custom.widget.WindowTitlePane;
import io.cortical.iris.ui.util.DragAssistant;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;


public class WindowBox extends VBox {
    private Accordion display;
    private VBox windowButtons;
    private ContentPane contentPane;
    
    private BooleanProperty clipDetectorVisibleProperty = new SimpleBooleanProperty(false);
    private BooleanProperty clipDetectedProperty = new SimpleBooleanProperty(false);
    
    
    /**
     * Constructs a new {@code WindowBox}
     * @param title
     * @param pane
     */
    public WindowBox(String title, ContentPane pane) {
        this.contentPane = pane;
        
        setMinWidth(200);
        setPrefWidth(200);
        
        Pane styledSpacer = new Pane();
        styledSpacer.setPrefSize(200, 30);
        styledSpacer.setMaxHeight(30);
        styledSpacer.setMinHeight(30);
        styledSpacer.setMinWidth(200);
        styledSpacer.getStyleClass().setAll("control-pane-accordian-spacer");
        
        Label l = new Label(title);
        l.prefWidthProperty().bind(styledSpacer.widthProperty());
        l.prefHeightProperty().bind(styledSpacer.heightProperty());
        l.setTextAlignment(TextAlignment.CENTER);
        l.setAlignment(Pos.BASELINE_CENTER);
        l.getStyleClass().add("control-pane-spacer-label");
        StackPane vb = new StackPane();
        vb.getChildren().add(l);
        vb.setAlignment(Pos.CENTER);
        styledSpacer.getChildren().add(vb);
        
        // Add Clip Detector functionality if this is an input WindowBox
        if(title.indexOf("Input") != -1) {
            Button clipDragger = createButton("paste");
            clipDragger.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> getScene().setCursor(Cursor.HAND));
            clipDragger.addEventHandler(MouseEvent.MOUSE_EXITED, e -> getScene().setCursor(Cursor.DEFAULT));
            clipDragger.setTooltip(new Tooltip("Clipboard Detector"));
            clipDragger.setFocusTraversable(false);
            clipDragger.setManaged(false);
            clipDragger.resize(20, 20);
            clipDragger.visibleProperty().bind(clipDetectorVisibleProperty);
            clipDragger.disableProperty().bind(clipDetectedProperty.not());
            DragAssistant.configureDragHandler(clipDragger);
            styledSpacer.layoutBoundsProperty().addListener((v,o,n) -> {
                clipDragger.relocate(n.getMaxX() - clipDragger.getWidth() - 5, (n.getHeight() - clipDragger.getHeight()) / 2);
            });
            styledSpacer.getChildren().add(clipDragger);
        }
        
        getChildren().add(styledSpacer);
        
        display = new ControlAccordion();
        display.setMaxWidth(190);
        display.setPrefWidth(190);
        display.getStyleClass().addAll(title.equals("Input Windows") ? "control-pane-accordian-in" : "control-pane-accordian-out");
        display.setFocusTraversable(false);
        windowButtons = new VBox();
        windowButtons.getStyleClass().setAll("control-pane-spacer-box");
        windowButtons.getChildren().add(display);
        
        getChildren().add(windowButtons);
    }
    
    /**
     * Creates a generic button on behalf of the caller and
     * returns it.
     * 
     * @param styleClass        the style class to apply
     * @return
     */
    protected Button createButton(String styleClass) {
        Button button = new Button();
        button.getStyleClass().add(styleClass);
        button.setPrefWidth(20);
        button.setPrefHeight(20);
        return button;
    }
    
    /**
     * Returns the clip detector button's visibility property.
     * @return
     */
    public BooleanProperty clipDetectorVisibleProperty() {
        return clipDetectorVisibleProperty;
    }
    
    /**
     * To be set true when the application detects a new clip available.
     * @return
     */
    public BooleanProperty clipDetectedProperty() {
        return clipDetectedProperty;
    }
    
    public void setExpandedPane(WindowTitlePane wt) {
        ((ControlAccordionSkin)display.getSkin()).setExpanded(wt);
    }
    
    public ObservableList<TitledPane> getPanes() {
        return display.getPanes();
    }
    
    public void layoutBoxes() {
        contentPane.getControlPane().layoutBoxes();
    }
}
