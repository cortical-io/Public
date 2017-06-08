package io.cortical.iris.view.output;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.robot.FXRobot;
import com.sun.javafx.robot.FXRobotFactory;

import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.persistence.ConfigHandler;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import io.cortical.iris.ui.custom.richtext.RichTextArea;
import io.cortical.iris.ui.custom.widget.bubble.Entry;
import io.cortical.iris.ui.custom.widget.bubble.WordBubble;
import io.cortical.iris.ui.custom.widget.bubble.WordBubbleContainer;
import io.cortical.iris.ui.util.TermLookupAssistant;
import io.cortical.iris.view.OutputViewArea;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.view.output.ContextDisplay.ContextEvent;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.retina.core.PosType;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Term;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import javafx.util.Pair;
import javafx.util.StringConverter;
import rx.Observable;


public class SimilarTermsDisplay extends LabelledRadiusPane implements View {
    protected static final Logger LOGGER = LoggerFactory.getLogger(SimilarTermsDisplay.class);
    
    public final ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();
    
    private static ConfigHandler<? super WindowConfig> configHandler;
    
    private SimTermsBubbleContainer bubbleContainer;
    private HBox mainContainer;
    private HBox posWidget;
    
    private ComboBox<String> posTypes;
    private ListView<Cell> checklist;
    
    private Label contextsLabel;
    private Label termsLabel;
    
    private ChangeListener<Paint> localChangeListener;
    
    private BooleanProperty dirtyProperty = new SimpleBooleanProperty(false);
    private OccurrenceProperty autoSendProperty = new OccurrenceProperty();
    
    private Region backPanel;
    
    
    /**
     * Constructs a new {@code SimilarTermsDisplay} specifying the 
     * tab background type.
     * 
     * @param label     the tab text
     * @param spec      the color background
     */
    public SimilarTermsDisplay(String label, NewBG spec) {
        super(label, spec);
        
        setVisible(false);
        setUserData(ViewType.SIMILAR_TERMS);
        setManaged(false);
        
        mainContainer = createMainContainer();
        
        ScrollPane scroll = createBubbleScroll();
        bubbleContainer = createWordBubbleContainer();
        scroll.setContent(bubbleContainer);
        
        checklist = createContextList();
        
        createLabels();
        
        mainContainer.getChildren().addAll(checklist, scroll);
        
        posWidget = createPOSWidget();
        
        getChildren().addAll(mainContainer, posWidget);    
        
        Platform.runLater(() -> {
            addAutoSendHandler();
            addInfoButton();
        });
    }
    
    /**
     * Implemented by {@code View} subclasses to handle an error
     * 
     * @param	context		the error information container
     */
    @Override
    public void processRequestError(RequestErrorContext context) {}
    
    /**
     * Adds the handler which sends a query request upon local parameter changing.
     */
    public void addAutoSendHandler() {
        autoSendProperty.addListener((v,o,n) -> {
            OutputWindow ow = (OutputWindow)WindowService.getInstance().windowFor(this);
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_RE_EXECUTE_LAST_QUERY.subj() + ow.getWindowID(), new Payload());
        });
    }
    
    /**
     * Returns this {@code SimilarTermsDisplay}'s {@link WordBubbleContainer}.
     * @return  this {@code SimilarTermsDisplay}'s {@link WordBubbleContainer}.
     */
    public WordBubbleContainer getBubbleContainer() {
        return bubbleContainer;
    }
    
    /**
     * Adds the specified term to this display.
     * @param term  the string to add
     */
    public void addTerm(Term term) {
        bubbleContainer.addTerm(term);
    }
    
    /**
     * Returns the POSTypes ComboBox
     * @return
     */
    public ComboBox<String> getPOSTypesCombo() {
        return posTypes;
    }
    
    /**
     * Clears this display of all added terms.
     */
    public void clear() {
        bubbleContainer.getChildren().removeAll(bubbleContainer.getChildren());
    }
    
    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        clear();
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
            backPanel = new SimilarTermsBackPanel(10).getScroll();
        }
        
        return backPanel;
    }
    
    /**
     * Returns the property which indicates that a new search criterion
     * has been selected.
     * @return
     */
    public BooleanProperty dirtyProperty() {
        return dirtyProperty;
    }
    
    /**
     * Returns the selected part of speech type.
     * @return
     */
    public PosType getSelectedPOS() {
        String pos = posTypes.getSelectionModel().getSelectedItem();
        PosType type = Arrays.stream(PosType.values())
            .filter(v -> v.toString().toLowerCase().equals(pos.toLowerCase()))
            .findFirst()
            .get();
        return type;
    }
    
    public void setSelectedPOS(PosType type) {
        posTypes.valueProperty().set(type.toString());
    }
    
    /**
     * Returns the {@link ConfigHandler} instance used to initialize
     * this {@code SimiliarTermDisplay}'s {@link PosType} settings following
     * deserialization of an {@link OutputWindow}.
     * @return
     */
    public static final ConfigHandler<? super WindowConfig> getChainHandler() {
        if(configHandler == null) {
            configHandler = config -> {
                LOGGER.debug("Executing SimilarTermsDisplay chain of responsibility handler");
                Platform.runLater(() -> {
                    if(config.getPrimaryViewType() == ViewType.EXPRESSION) {
                        if(config.getPosType() != null) {
                            OutputWindow thisWindow = (OutputWindow)WindowService.getInstance().windowFor(config.getWindowID());
                            thisWindow.getViewArea().getSimilarTermsDisplay().setSelectedPOS(config.getPosType());
                            LOGGER.debug("configure: selecting PosType = " + config.getPosType());
                        }
                    } 
                    config.advanceNotificationChain();
                });
            };
        }
        
        return configHandler;
    }
    
    /**
     * Creates and adds the 2 labels used to describe both the 
     * context list and the similar terms list displays.
     */
    private void createLabels() {
        contextsLabel = new Label("Selected Contexts");
        termsLabel = new Label("Terms");
        Font f = Font.font(termsLabel.getFont().getFamily(), FontWeight.NORMAL, FontPosture.ITALIC, 12);
        contextsLabel.setFont(f);
        termsLabel.setFont(f);
        contextsLabel.setManaged(false);
        termsLabel.setManaged(false);
        checklist.layoutBoundsProperty().addListener((v,o,n) -> {
           contextsLabel.resizeRelocate(n.getMinX(), n.getMinY() - 15, 130, 15);
           termsLabel.resizeRelocate(n.getMaxX() + 5, n.getMinY() - 15, 130, 15);
        });
        mainContainer.getChildren().addAll(contextsLabel, termsLabel);
    }
    
    /**
     * The horizontal box which contains both the context list
     * and the bubble display of similar terms.
     * @return  the HBox container
     */
    private HBox createMainContainer() {
        HBox mainContainer = new HBox(5);
        mainContainer.layoutYProperty().bind(labelHeightProperty().add(25));
        mainContainer.layoutXProperty().set(5);
        mainContainer.prefWidthProperty().bind(widthProperty().subtract(10));
        mainContainer.prefHeightProperty().bind(heightProperty().subtract(labelHeightProperty()).subtract(30));
        
        return mainContainer;
    }
    
    /**
     * Creates the scroll pane which contains the 
     * bubble display
     * @return  the bubble display scroll pane
     */
    private ScrollPane createBubbleScroll() {
        ScrollPane sp = new ScrollPane();
        sp.setHbarPolicy(ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("sim-terms-bubble-display");
        
        return sp;
    }
    
    /**
     * Creates and returns a new {@link WordBubbleContainer}
     * @return  a new {@link WordBubbleContainer}
     */
    private SimTermsBubbleContainer createWordBubbleContainer() {
        SimTermsBubbleContainer bubbleContainer = new SimTermsBubbleContainer();
        bubbleContainer.prefWrapLengthProperty().bind(mainContainer.widthProperty().subtract(160));
       
        return bubbleContainer;
    }
    
    /**
     * Returns the cell factory used to create the context list's 
     * list view cells.
     * 
     * @param getSelectedProperty   the cell entry's selection property
     * @param converter             the converter used to convert the object 
     *                              to a string representation
     * @return  the cell factory
     */
    public static <T> Callback<ListView<T>, ListCell<T>> forListView(
        final Callback<T, ObservableValue<Boolean>> getSelectedProperty, 
        final StringConverter<T> converter) {
        return list -> {
            CheckBoxListCell<T> cblc = new CheckBoxListCell<T>(getSelectedProperty, converter);
            cblc.graphicProperty().addListener((v,o,n) -> {
               if(n != null) {
                   n.setDisable(true);
               }
            });
            cblc.selectedProperty().addListener((v,o,n) -> {
                list.getSelectionModel().select(-1);
            });
            return cblc;
        };
    }
    
    /**
     * Creates the list used to display the selected contexts
     * from the context tab.
     * @return  the context display list
     */
    private ListView<Cell> createContextList() {
        ListView<Cell> checklist = new ListView<>();
        checklist.getStyleClass().add("similar-terms-context-display");
        checklist.setCellFactory(forListView(Cell::selectedProperty, new StringConverter<Cell>() {
            @Override
            public String toString(Cell object) {
                return object.getName();
            }

            @Override
            public Cell fromString(String string) {
                return null;
            }
        }));
        checklist.setPrefWidth(150);
        checklist.prefHeightProperty().bind(mainContainer.heightProperty().subtract(labelHeightProperty()));
        checklist.setMinWidth(150);
        checklist.setMaxWidth(150);
        
        return checklist;
    }
    
    /**
     * Called by the {@link OutputViewArea#createSimilarTermsDisplay()} method
     * to initialize the observation of context changes occurring from
     * the contexts tab.
     * 
     * @param o the Observable used to receive {@link ContextEvent}s.
     */
    public void observeContexts(Observable<ContextEvent> o) {
        o.subscribe(e -> {
            ObservableList<Cell> tasks = FXCollections.observableArrayList(
                e.getContexts().stream().map(c -> {
                    Cell t = new Cell(c.getLabel());
                    t.setSelected(c.isSelected());
                    return t;
                }).collect(Collectors.toList())
            );
            
            checklist.setItems(tasks);
            // makes sure that the list itself can have no selected rows
            // to give it a "read only" feel.
            checklist.getSelectionModel().select(-1);
        });
    }
    
    /**
     * Creates the POS (Part of speech) widget.
     * @return  a new pos widget
     */
    private HBox createPOSWidget() {
        HBox hb = new HBox(5);
        Label l = new Label("POS Type:");
        l.setFocusTraversable(false);
        hb.setAlignment(Pos.BOTTOM_CENTER);
        posTypes = new ComboBox<>();
        posTypes.setFocusTraversable(false);
        posTypes.getItems().addAll("Any", "Noun", "Verb", "Adjective");
        posTypes.getSelectionModel().select(0);
        hb.getChildren().addAll(l, posTypes);
        hb.layoutXProperty().bind(widthProperty().subtract(hb.widthProperty().add(35)));
        hb.setLayoutY(5);
        posTypes.getStyleClass().add("similar-terms-pos-display");
        
        posTypes.valueProperty().addListener((v,o,n) -> {
            dirtyProperty.set(true);
            autoSendProperty.set();
        });
        
        return hb;
    }
    
    /**
     * Abstracts the context list entry items.
     */
    private static class Cell {
        private ReadOnlyStringWrapper name = new ReadOnlyStringWrapper();
        private BooleanProperty selected = new SimpleBooleanProperty(false);

        public Cell(String name) {
            this.name.set(name);
        }

        public String getName() {
            return name.get();
        }
        
        public BooleanProperty selectedProperty() {
            return selected;
        }
        
        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }
    }
    
    /**
     * Local subclass of the WordBubbleContainer to invoke commands to retrieve 
     * terms from the server.
     */
    private class SimTermsBubbleContainer extends WordBubbleContainer {
        private Map<String, Term> terms = new HashMap<>();
        private PopOver popper = new PopOver();
        private BooleanProperty popperEntered = new SimpleBooleanProperty();
        private BooleanProperty colorButtonActive = new SimpleBooleanProperty();
        
        
        private SimTermsBubbleContainer() {
            popper.getRoot().getStyleClass().add("sim-terms-popover");
            popper.setCornerRadius(10);
            popper.setHideOnEscape(true);
            popper.getRoot().setPadding(new Insets(10, 10, 10, 10));
            popper.getRoot().addEventHandler(MouseEvent.MOUSE_ENTERED, m -> {
                popperEntered.set(true);
            });
            popper.getRoot().addEventHandler(MouseEvent.MOUSE_EXITED, m -> {
                if(!colorButtonActive.get()) {
                    popperEntered.set(false);
                    popper.hide();
                }
                popperEntered.set(false);
            });
            popper.showingProperty().addListener((v,o,n) -> {
                if(!n) {
                    popperEntered.set(false);
                    colorButtonActive.set(false);
                }
            });
        }
        
        /**
         * Adds a new {@link Entry} containing a {@link WordBubble} with
         * a String "term", to this container. 
         * @param term
         */
        public Entry<WordBubble> addTerm(Term term) {
            term.setFingerprint(null);
            terms.put(term.getTerm(), term);
            
            VBox highlighter = new VBox(5);
            RichTextArea textArea = new RichTextArea(false);
            
            final ColorPicker colorPicker = new ColorPicker();
            
            Entry<WordBubble> e = new Entry<>(new WordBubble(term.getTerm()));
            
            // This listener is only here to prevent bubbles from becoming discolored
            // or having their colors reset although still selected; when the user clicks
            // within the bubble container but NOT in a bubble.
            e.getBubble().textFillProperty().addListener(localChangeListener = (v,o,n) -> {
                if(e.getBubble().isSelected() && n.equals(Color.BLACK)) {
                    Paint bg = e.getBubble().getBackground().getFills().get(0).getFill();
                    Platform.runLater(() -> {
                        e.getBubble().textFillProperty().removeListener(localChangeListener);
                        assertSpecificColor(e, bg, bg, Color.WHITE);
                        e.getBubble().textFillProperty().addListener(localChangeListener);
                    });
                }
            });
            
            e.getBubble().selectedProperty().addListener((v,o,n) -> {
                if(!n) {
                    assertOriginalColor(e);
                    removeTerm(term.getFingerprint(), e);
                    popper.hide();
                    popperEntered.set(false);
                }else if(!popper.isShowing()){
                    assertPickerValueColor(colorPicker, e);
                    
                    styleAndShowTerm(term, e, highlighter, textArea, colorPicker);
                    
                    if(term.getFingerprint() == null) {
                        routeTermRequest(term, colorPicker.getValue());
                    }else{
                        UUID uuid = WindowService.getInstance().windowFor(this).getWindowID();
                        OutputViewArea.setProgressIndicatorActive(uuid, true);
                        addTermToFingerprint(term, colorPicker.getValue());
                    }
                }
            });
            e.setAlignment(Pos.CENTER);
            
            textArea.setStyle("-fx-background-color: rgb(237, 93, 37); -fx-padding: 5;");
            textArea.setEditable(false);
            textArea.setFocusTraversable(false);
            textArea.setPrefSize(260, 130);
            highlighter.getChildren().add(textArea);
            
            HBox colorSelectionRow = new HBox(5);
            
            colorPicker.setValue(Color.rgb(237, 93, 37));
            colorPicker.valueProperty().addListener((v,o,n) -> {
                if(term.getFingerprint() != null) {
                    removeTerm(term.getFingerprint(), e);
                }
                
                Platform.runLater(() -> {
                    assertPickerValueColor(colorPicker, e);
                    
                    String hex = String.format( "#%02X%02X%02X",
                        (int)(n.getRed() * 255 ),
                        (int)(n.getGreen() * 255 ),
                        (int)(n.getBlue() * 255 ));
                    textArea.setStyle("-fx-background-color: " + hex + "; -fx-padding: 5;");
                   
                    if(term.getFingerprint() == null) {
                        routeTermRequest(term, n);
                    }else{
                        addTermToFingerprint(term, n);
                    }
                    
                    popperEntered.set(false);
                    colorButtonActive.set(false);
                    popper.hide();
                });
            });
            colorPicker.setOnMouseClicked(m -> {
                colorButtonActive.set(!colorButtonActive.get());
            });
                        
            colorSelectionRow.getChildren().addAll(colorPicker);
            highlighter.getChildren().add(colorSelectionRow);
            
            e.getBubble().addEventHandler(MouseEvent.MOUSE_MOVED, ev -> {
                if(e.getBubble().isSelected() && !popper.isShowing()) {
                    styleAndShowTerm(term, e, highlighter, textArea, colorPicker);
                }
            });
            
            e.getBubble().addEventHandler(MouseEvent.MOUSE_EXITED, ev -> {
                if(e.getBubble().isSelected() && popper.isShowing()) {
                    (new Thread(() -> {
                        try { Thread.sleep(500); }catch(Exception ex) { ex.printStackTrace(); }
                        
                        Platform.runLater(() -> {
                            if(!popperEntered.get()) {
                                colorPicker.requestFocus();
                                FXRobot robot = FXRobotFactory.createRobot(getScene());
                                robot.mousePress(MouseButton.PRIMARY);
                                robot.mouseRelease(MouseButton.PRIMARY);
                                robot.mouseClick(MouseButton.PRIMARY, 1);
                                popperEntered.set(false);
                                colorButtonActive.set(false);
                            }
                        });
                    })).start();
                }
            });
            
            getChildren().add(bubbleInsertionIdx, e);
            
            return e;
        }
        
        private void assertSpecificColor(Entry<WordBubble> e, Paint bgColor, Paint borderColor, Paint textFill) {
            CornerRadii radii = new CornerRadii(20);
            e.getBubble().setBackground(new Background(new BackgroundFill(bgColor, radii, null)));
            e.getBubble().setBorder(new Border(new BorderStroke(borderColor, BorderStrokeStyle.SOLID, radii, BorderWidths.DEFAULT)));
            e.getBubble().setTextFill(textFill);
        }
        
        private void assertOriginalColor(Entry<WordBubble> e) {
            CornerRadii radii = new CornerRadii(20);
            e.getBubble().setBackground(new Background(new BackgroundFill(Color.WHITE, radii, null)));
            e.getBubble().setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, radii, BorderWidths.DEFAULT)));
            e.getBubble().setTextFill(Color.BLACK);
        }
        
        /**
         * Sets the bubble to be the same color as the current picker value.
         * @param e     the Entry containing the word bubble to modify
         * @param c     the color to assert
         */
        private void assertPickerValueColor(ColorPicker colorPicker, Entry<WordBubble> e) {
            Color c = colorPicker.getValue();
            CornerRadii radii = new CornerRadii(20);
            e.getBubble().setBackground(new Background(new BackgroundFill(c, radii, null)));
            e.getBubble().setBorder(new Border(new BorderStroke(c, BorderStrokeStyle.SOLID, radii, BorderWidths.DEFAULT)));
            e.getBubble().setTextFill(Color.WHITE);
            e.addUserDefinedColor(c);
            colorPicker.fireEvent(new ActionEvent(colorPicker, colorPicker));
        }

        /**
         * Styles the specified arguments before adding them to the {@link PopOver}
         * node.
         * 
         * @param term
         * @param e
         * @param highlighter
         * @param textArea
         */
        private void styleAndShowTerm(Term term, Entry<WordBubble> e, VBox highlighter, RichTextArea textArea, ColorPicker colorPicker) {
            textArea.clear();
            
            // Remove the term's fingerprint so it's not displayed, then set it back
            Fingerprint fp = term.getFingerprint();
            term.setFingerprint(null);
            try { textArea.setText(term.toJson()); }catch(Exception ex) { ex.printStackTrace(); }
            term.setFingerprint(fp);
            
            Color selectedColor = colorPicker.getValue();
            
            textArea.setCharStyle("{", selectedColor, textArea.getBackgroundColor());
            textArea.setCharStyle("}", selectedColor, textArea.getBackgroundColor());
            textArea.setStyle("\"df\"", selectedColor, textArea.getBackgroundColor(), true);
            textArea.setStyle("\"score\"", selectedColor, textArea.getBackgroundColor(), true);
            textArea.setStyle("\"pos_types\"", selectedColor, textArea.getBackgroundColor(), true);
            textArea.setStyle("\"term\"", selectedColor, textArea.getBackgroundColor(), true);
            popper.setContentNode(highlighter);
            popper.setTitle("Term: " + term.getTerm());
            popper.show(e);
        }
        
        /**
         * Invokes stand alone server call to update the FingerprintDisplay with
         * a new Fingerprint representing the selected Term in this SimilarTermsDisplay.
         * 
         * @param term
         */
        private void routeTermRequest(Term term, Color color) {
            OutputWindow w = (OutputWindow)WindowService.getInstance().windowFor(SimilarTermsDisplay.this);
            ServerRequest request = TermLookupAssistant.prepareRequest(term, null, w);
            request.setWindow(w);
            
            TermLookupAssistant.routeTermRequest(request, t -> {
                Fingerprint fp = t.getFingerprint();
                term.setFingerprint(fp);
                addTermToFingerprint(term, color);
            });
        }
        
        /**
         * Sends a message to the {@link FingerprintDisplay} to have it display the 
         * specified {@link Fingerprint}
         * @param term      the term containing the {@link Fingerprint} to display
         * @param color     the Color in which to display the fingerprint
         */
        private void addTermToFingerprint(Term term, Color color) {
            OutputWindow w = (OutputWindow)WindowService.getInstance().windowFor(SimilarTermsDisplay.this);
            Pair<Term, Color> pair = new Pair<>(term, color);
            Payload payload = new Payload(pair);
            EventBus.get().broadcast(BusEvent.FINGERPRINT_DISPLAY_ADD.subj() + w.getWindowID().toString(), payload);
        }
        
        /**
         * Removes the specified {@link Term}'s {@link Fingerprint} from the
         * {@link FingerprintDisplay}
         * @param term
         */
        private void removeTerm(Fingerprint fp, Entry<WordBubble> e) {
            // If the user "de-selects" the bubble before the term is retrieved from the server
            if(fp == null) {
                return;
            }
            
            Color lastColor = null;
            if((lastColor = e.popLastUserDefinedColor()) == null) {
                return;
            }
            
            OutputWindow w = (OutputWindow)WindowService.getInstance().windowFor(SimilarTermsDisplay.this);
            Pair<Fingerprint, Color> pair = new Pair<>(fp, lastColor);//Color.rgb(237, 93, 37));
            Payload payload = new Payload(pair);
            EventBus.get().broadcast(BusEvent.FINGERPRINT_DISPLAY_REMOVE_BY_COLOR.subj() + w.getWindowID().toString(), payload);
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
