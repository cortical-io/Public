package io.cortical.iris.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.cortical.iris.RetinaClientFactory;
import io.cortical.iris.WindowService;
import io.cortical.iris.ui.custom.widget.WindowTitlePane;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.retina.client.FullClient;
import javafx.animation.Animation.Status;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;


/**
 * The left section of the main application window which contains the
 * control buttons and features for the application.
 */
public class ControlPane extends VBox {
    private static final int CONTROL_BAR_V_SPACE = 10;
    
    private ContentPane contentPane;
    
    private List<WindowTitlePane> inputWindowPanes = new ArrayList<>();
    private List<WindowTitlePane> outputWindowPanes = new ArrayList<>();
    private WindowBox inputBox;
    private WindowBox outputBox;
    
    private ComboBox<FullClient> retinaChoices;
    private Button addInputWindowButton;
    private Button addOutputWindowButton;
    
    
    
    /**
     * Creates the application's {@code ControlPane}
     * @param spacing
     */
    public ControlPane(ContentPane pane) {
        super(CONTROL_BAR_V_SPACE);
        
        this.contentPane = pane;
        
        setPadding(new Insets(0, 0, 0, 0));
        setMaxWidth(200);
        setMinWidth(200);
        setFocusTraversable(false);
        getChildren().add(createControlPaneHeader());
            
        // Create the Input and Output Window Boxes containing the Window Accordions
        getChildren().add(createAccordionDisplay());
    }
    
    /**
     * Returns the input {@link WindowBox}
     * @return
     */
    public WindowBox getInputWindowBox() {
        return inputBox;
    }
    
    /**
     * Factory method to create and return a new {@link WindowTitle}.
     * 
     * @param parent        the parent {@link WindowBox}
     * @param iw            the {@link Window} the title pane is associated with.
     * @param windowNum     the index of the {@link Window}
     * @return  the newly created {@link WindowTitle}
     */
    private WindowTitlePane createWindowTitlePane(WindowBox parent, Window iw, int windowNum) {
        WindowTitlePane wt = new WindowTitlePane(parent, iw, windowNum);
        return wt;
    }
    
    /**
     * Adds an {@link InputWindow}'s {@link WindowTitlePane} to the control pane.
     * @param iw            the InputWindow represented by the newly added WindowTitlePane
     * @param windowNum     the window's number in order of instantiation
     */
    public void addInputWindowTitle(InputWindow iw, int windowNum) {
        WindowTitlePane wt = createWindowTitlePane(inputBox, iw, windowNum);
        wt.selectRetina(getDefaultRetina());
        iw.destroyProperty().addListener((v,o,n) -> { removeInputWindowTitle(wt); });
        inputWindowPanes.add(wt);
        inputBox.getPanes().add(wt);
        inputBox.getPanes().addListener((Change<? extends Object> c) -> {
            requestLayout();
        });
    }
    
    /**
     * Removes the specified {@link WindowTitlePane}
     * @param wt    the WindowTitlePane to remove
     */
    public void removeInputWindowTitle(WindowTitlePane wt) {
        inputWindowPanes.remove(wt);
        inputBox.getPanes().remove(wt);
    }
    
    /**
     * Adds an {@link OutputWindow}'s {@link WindowTitlePane} to the control pane.
     * @param iw            the OutputWindow represented by the newly added WindowTitlePane
     * @param windowNum     the window's number in order of instantiation
     */
    public void addOutputWindowTitle(OutputWindow ow, int windowNum) {
        WindowTitlePane wt = createWindowTitlePane(outputBox, ow, windowNum);
        ow.destroyProperty().addListener((v,o,n) -> { removeOutputWindowTitle(wt); });
        outputWindowPanes.add(wt);
        outputBox.getPanes().add(wt);
    }
    
    /**
     * Removes the specified {@link WindowTitle}
     * @param wt    the WindowTitle to remove
     */
    public void removeOutputWindowTitle(WindowTitlePane wt) {
        outputWindowPanes.remove(wt);
        outputBox.getPanes().remove(wt);
    }
    
    /**
     * Returns a list of all the currently existing {@link WindowTitle}s
     * of the type specified by the type of the {@link Window} passed in.
     * The returned list will either be the list of input WindowTitles or
     * output WindowTitles depending on the type of the specified Window.
     * 
     * @param w     used to determine which list to return, inputs or outputs.
     * @return      a list of input or output WindowTitles
     */
    public List<WindowTitlePane> getWindowPanesForType(Window w) {
        if(w.isInput()) {
            return inputWindowPanes;
        }
        return outputWindowPanes;
    }
    
    /**
     * Returns the count of OutputWindows currently showing in the app.
     * This method is used for auto-connection of OutputWindows to InputWindows
     * during InputWindow construction. (see {@link WindowService#addInputWindow()})
     * @return
     */
    public int getOutputWindowCount() {
        return outputWindowPanes.size();
    }
    
    /**
     * Returns the {@link ComboBox} used to display the various Retina
     * style choices.
     * @return  the Retina choice {@code ComboBox}
     */
    public ComboBox<FullClient> getRetinaChoices() {
        return retinaChoices;
    }
    
    
    /**
     * Called to translate the specified node to the right 15 pixels
     * @param t     the {@link TranslateTransition}
     * @param n     the Node 
     */
    private void animIn(TranslateTransition t, Node n) {
        if(t.getStatus() != Status.RUNNING) {
            t.setFromX(n.getBoundsInParent().getMinX());
            t.setToX(15);
            t.play();
        }else if(t.getToX() == 0) {
            // This is here so that when the mouse re-enters, the node will immediately
            // go back to the focused position.
            t.stop();
            t.setFromX(n.getBoundsInParent().getMinX());
            t.setToX(15);
            t.play();
        }
    }
    
    /**
     * Called to translate the specified node to the left 15 pixels to
     * its origin position.
     * 
     * @param t     the {@link TranslateTransition}
     * @param n     the Node 
     */
    private void animOut(TranslateTransition t, Node n) {
        if(t.getStatus() != Status.RUNNING) {
            t.setFromX(n.getBoundsInParent().getMinX());
            t.setToX(0);
            t.play();
        }else{
            // This is here so that the node won't get stuck in the focused position
            // when the mouse has already exited.
            (new Thread(() -> {
                try { Thread.sleep(250); } catch(Exception e) { e.printStackTrace(); }
                Platform.runLater(() -> animOut(t, n));
            })).start();
        }
    }
    
    /**
     * Creates a new {@link TranslateTransition} used for a button mouse-over
     * effect.
     *  
     * @param target    the Node the transition will apply to.
     * @return  the TranslateTransition
     */
    private TranslateTransition getTransition(Node target) {
        TranslateTransition translateTransition =
            new TranslateTransition(Duration.millis(250), target);
        translateTransition.setCycleCount(1);
        translateTransition.setAutoReverse(false);
        return translateTransition;
    }
    
    /**
     * Adds the handler for Retina selection changes. This listener should be 
     * added following the initialization of the Retina choice to the zero'th
     * entry (otherwise an event will be propagated that isn't valid).
     */
    public void addRetinaChoiceListener(ChangeListener<? super FullClient> listener) {
        retinaChoices.valueProperty().addListener(listener);
    }
    
    /**
     * Returns the currently selected Retina client.
     * 
     * @return  the currently selected Retina client
     */
    public FullClient getDefaultRetina() {
        return retinaChoices.getValue();
    }
    
    /**
     * Called to simultaneously layout the accordion display containers.
     */
    public void layoutBoxes() {
        inputBox.requestLayout();
        outputBox.requestLayout();
    }
    
    /**
     * Pane which houses both the input and output accordion {@link WindowBox}es
     * @return  returns the container for both accordion displays
     */
    private GridPane createAccordionDisplay() {
        GridPane gridPane = new GridPane();
        gridPane.setPrefWidth(190);
        gridPane.setMaxWidth(190);
        
        inputBox = new WindowBox("Input Windows", contentPane);
        TranslateTransition inTransition = getTransition(inputBox);
        inputBox.addEventHandler(MouseEvent.ANY, m -> {
            if(m.getEventType() == MouseEvent.MOUSE_ENTERED) {
                animIn(inTransition, inputBox);
            }else if(m.getEventType() == MouseEvent.MOUSE_EXITED) {
                animOut(inTransition, inputBox);
            }
        });
        
        outputBox = new WindowBox("Output Windows", contentPane);
        TranslateTransition outTransition = getTransition(outputBox);
        outputBox.addEventHandler(MouseEvent.ANY, m -> {
            if(m.getEventType() == MouseEvent.MOUSE_ENTERED) {
                animIn(outTransition, outputBox);
            }else if(m.getEventType() == MouseEvent.MOUSE_EXITED) {
                animOut(outTransition, outputBox);
            }
        });
        
        Pane spacer = new Pane();
        spacer.setPrefHeight(CONTROL_BAR_V_SPACE);
        spacer.setMinHeight(CONTROL_BAR_V_SPACE);
        spacer.setMaxHeight(CONTROL_BAR_V_SPACE);
        spacer.setPrefWidth(CONTROL_BAR_V_SPACE);
        
        GridPane.setConstraints(inputBox, 0, 0, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(spacer, 0, 1, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(outputBox, 0, 2, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        
        gridPane.getChildren().addAll(inputBox, spacer, outputBox);
              
        return gridPane;
    }
    
    /** 
     * Called by the 
     */
    public void initializeRetinaChoices() {
    	retinaChoices.getItems().addAll(RetinaClientFactory.getRetinaClients());
    	retinaChoices.getSelectionModel().select(3);
    	addInputWindowButton.setDisable(false);
    	addOutputWindowButton.setDisable(false);
    }
    
    /**
     * Creates and returns the header of the control pane, which is the
     * smaller pane residing on the left or west of the app ui and contains
     * control elements.
     * 
     * @return  the control pane
     */
    private Pane createControlPaneHeader() {
        VBox vb = new VBox(CONTROL_BAR_V_SPACE);
        vb.setFocusTraversable(false);
        vb.setFocusTraversable(false);
        
        retinaChoices = new ComboBox<>();
        retinaChoices.getStyleClass().add("retina-choice-box");
        retinaChoices.setTooltip(new Tooltip("Affects new InputWindows only."));
        retinaChoices.setFocusTraversable(false);
        retinaChoices.setPrefSize(200, 26);
        
        retinaChoices.setPromptText("Select Retina");
        retinaChoices.setConverter(new StringConverter<FullClient>() {
            @Override public String toString(FullClient object) {
                return "Retina:    " + Arrays.stream(
                    RetinaClientFactory.getRetinaTypes())
                        .filter(t -> RetinaClientFactory.getClient(t) == object)
                        .findAny()
                        .get();
            }
            @Override public FullClient fromString(String string) {
                return RetinaClientFactory.getClient(string);
            }
        });
        
        TranslateTransition retinaTransition = getTransition(retinaChoices);
        retinaChoices.addEventHandler(MouseEvent.ANY, m -> {
            if(m.getEventType() == MouseEvent.MOUSE_ENTERED) {
                animIn(retinaTransition, retinaChoices);
                getScene().setCursor(Cursor.HAND);
            }else if(m.getEventType() == MouseEvent.MOUSE_EXITED) {
                animOut(retinaTransition, retinaChoices);
                getScene().setCursor(Cursor.DEFAULT);
            }
        });
        
        Button b1 = addInputWindowButton = createHeaderAddButton("Add new input window", true);
        b1.setDisable(true);
        b1.setOnAction(e -> {
            getScene().setCursor(Cursor.WAIT);
            
            (new Thread(() -> {
                Platform.runLater(() -> {
                    WindowService.getInstance().addInputWindow();
                    Platform.runLater(() -> getScene().setCursor(Cursor.DEFAULT));
                });
            })).start();
        });
        TranslateTransition b1Transition = getTransition(b1);
        b1.addEventHandler(MouseEvent.ANY, m -> {
            if(m.getEventType() == MouseEvent.MOUSE_ENTERED) {
                animIn(b1Transition, b1);
                getScene().setCursor(Cursor.HAND);
            }else if(m.getEventType() == MouseEvent.MOUSE_EXITED) {
                animOut(b1Transition, b1);
                getScene().setCursor(Cursor.DEFAULT);
            }
        });
        
        Button b2 = addOutputWindowButton = createHeaderAddButton("Add new output window", false);
        b2.setDisable(true);
        b2.setOnAction(e -> {
            getScene().setCursor(Cursor.WAIT);
            
            (new Thread(() -> {
                Platform.runLater(() -> {
                    WindowService.getInstance().addOutputWindow();
                    Platform.runLater(() -> getScene().setCursor(Cursor.DEFAULT));
                });
            })).start();
        });
        TranslateTransition b2Transition = getTransition(b2);
        b2.addEventHandler(MouseEvent.ANY, m -> {
            if(m.getEventType() == MouseEvent.MOUSE_ENTERED) {
                animIn(b2Transition, b2);
                getScene().setCursor(Cursor.HAND);
            }else if(m.getEventType() == MouseEvent.MOUSE_EXITED) {
                animOut(b2Transition, b2);
                getScene().setCursor(Cursor.DEFAULT);
            }
        });
        
        vb.getChildren().addAll(retinaChoices, b1, b2);
        return vb;
    }

    /**
     * Creates the "Add" buttons used to add new Windows and their control
     * elements.
     * 
     * @param text      the text shown on the button
     * @param isInput   whether the button is used to create input or output {@link Window}s
     * @return  the specialized add button
     */
    private Button createHeaderAddButton(String text, boolean isInput) {
        Button b = new Button(text);
        b.setFocusTraversable(false);
        b.setPrefSize(200, 26);
        b.getStyleClass().add(isInput ? 
            "control-pane-header-add-input-button" : "control-pane-header-add-output-button");
        b.setAlignment(Pos.CENTER);
        
        return b;
    }
    
}
