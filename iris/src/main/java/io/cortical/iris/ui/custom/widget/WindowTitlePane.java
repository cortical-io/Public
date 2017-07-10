package io.cortical.iris.ui.custom.widget;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import fxpresso.tidbit.ui.Flyout;
import fxpresso.tidbit.ui.Flyout.Side;
import io.cortical.fx.webstyle.Impression;
import io.cortical.iris.ApplicationService;
import io.cortical.iris.RetinaClientFactory;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.persistence.ConfigHandler;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.WindowBox;
import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import io.cortical.iris.ui.custom.rangeslider.RangeSlider;
import io.cortical.iris.ui.util.DragMode;
import io.cortical.iris.ui.util.SnapshotAssistant;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.view.input.expression.Operator;
import io.cortical.iris.view.output.CompareDisplay;
import io.cortical.iris.view.output.FingerprintDisplay;
import io.cortical.iris.window.ColorIDTab;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.retina.client.FullClient;
import io.cortical.util.ConfigurableIndentor;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Pair;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;


/**
 * An extension of {@link TitlePane} which has GUI attributes relevant
 * to the ControlPane window display of input and output window attributes.
 */
public class WindowTitlePane extends TitledPane implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(WindowTitlePane.class);

    public enum MaxResultsType { 
        SIMTERMS, CONTEXTS, SLICES, SPACEHOLDER;
        static Map<ViewType, MaxResultsType> map = new HashMap<>();
        static {
            map.put(ViewType.SIMILAR_TERMS, SIMTERMS);
            map.put(ViewType.CONTEXTS, CONTEXTS);
            map.put(ViewType.SLICES, SLICES);
        }
        static final Label SPACE_SAVER = new Label("More window attributes...");
        public static MaxResultsType typeFor(ViewType viewType) {
            return viewType == null || map.get(viewType) == null ? 
                MaxResultsType.SPACEHOLDER : map.get(viewType);
        }
    };
    
    transient WindowBox parentContainer;
    transient Label windowNameLabel = new Label("");
    transient Label windowLocationLabel = new Label("");
    transient BooleanProperty dirtyProperty = new SimpleBooleanProperty();
    
    transient Line titleColorID;
    
    transient UUID referredWindowID;
    
    transient CheckBox checkBox = new CheckBox("Visible");
    
    transient VBox attributePane = new VBox(3);
    
    private transient Window window;
    
    private transient ConfigHandler<? super WindowConfig> configHandler;
    
    private transient IntegerProperty simTermsResultStartIndexProperty = new SimpleIntegerProperty();
    private transient IntegerProperty simTermsMaxResultsProperty = new SimpleIntegerProperty();
    private transient IntegerProperty contextsResultStartIndexProperty = new SimpleIntegerProperty();
    private transient IntegerProperty contextsMaxResultsProperty = new SimpleIntegerProperty();
    private transient IntegerProperty slicesResultStartIndexProperty = new SimpleIntegerProperty();
    private transient IntegerProperty slicesMaxResultsProperty = new SimpleIntegerProperty();
    private transient BooleanProperty compareColorFollowsWindowProperty = new SimpleBooleanProperty(false);
    private transient OccurrenceProperty windowTitleAutoSendProperty = new OccurrenceProperty();
    private transient StringProperty selectedRetinaNameProperty = new SimpleStringProperty("English (Assoc.)");
    private transient ObjectProperty<Operator> selectedDefaultOperatorProperty = new SimpleObjectProperty<>(Operator.AND);
    private transient IntegerProperty selectedDefaultIndentationProperty = new SimpleIntegerProperty(4);
    private transient ObjectProperty<DragMode> clipboardDragModeProperty = new SimpleObjectProperty<>();
    
    private transient Map<IntegerProperty, RangeSlider> propertyRebindMap = new HashMap<>();
           
    private transient Node last;
    private transient StackPane simTermsMaxAdjuster;
    private transient StackPane contextsMaxAdjuster;
    private transient StackPane slicesMaxAdjuster;
    private transient StackPane defaultOperatorPane;
    private transient StackPane defaultIndentationPane;
    private transient StackPane clipboardDragModePane;
    private transient StackPane fingerprintSnapshotPane;
    private transient StackPane compareSnapshotPane;
    
    private transient ComboBox<FullClient> inputSpecificRetinaChoices;
    private transient ComboBox<Operator> defaultOperatorChoices;
    private transient ComboBox<Integer> defaultIndentationChoices;
    
    private transient Flyout exampleFlyout;
    private transient Flyout exampleFlyout2;
    private transient ConfigurableIndentor indentor;
    
    
    /**
     * Creates a new {@code WindowTitlePane}
     * 
     * @param parent        the node container determining ui placement
     * @param w             the associated {@link Window}
     * @param windowNum     the 
     */
    public WindowTitlePane(WindowBox parent, Window w, int windowNum) {
        this.parentContainer = parent;
        this.window = w;
        this.referredWindowID = w.getWindowID();
        
        setPadding(new Insets(0,0,0,0));
        
        setMaxWidth(170);
        setPrefWidth(170);
        
        w.getTitleBar().getTitleField().textProperty().addListener((v,o,n) -> {
            if(n != null && !n.isEmpty()) {
                setWindowName(n);
            }else{
                setWindowName("Window " + windowNum);
            }
        });
        
        if(w.isInput()) {
            w.getTitleBar().getColorIDTab().colorIDProperty().addListener((v,o,n) -> {
                windowLocationLabel.setVisible(n == null || n.equals(ColorIDTab.DEFAULT_ID_COLOR));
            });
        }
        setWindowName("Window " + windowNum);
        
        w.layoutXProperty().addListener((v,o,n) -> {
            windowLocationLabel.setText("[" + String.format("%d",n.longValue()) + ", " + String.format("%d",(long)w.getLayoutY()) + "]");
        });
        w.layoutYProperty().addListener((v,o,n) -> {
            windowLocationLabel.setText("[" + String.format("%d",(long)w.getLayoutX()) + ", " + String.format("%d", n.longValue()) + "]");
        });
        
        w.selectedProperty().addListener((v,o,n) -> {
            if(n) {
                windowNameLabel.getStyleClass().removeAll("accordion-name");
                windowNameLabel.getStyleClass().addAll("accordion-name-selected");
                setExpanded(true);
                parentContainer.setExpandedPane(this);
                parentContainer.layoutBoxes();
            }else{
                windowNameLabel.getStyleClass().removeAll("accordion-name-selected");
                windowNameLabel.getStyleClass().addAll("accordion-name");
                setExpanded(false);
                parentContainer.setExpandedPane(null);
                parentContainer.layoutBoxes();
            }
        });
        
        addEventHandler(MouseEvent.MOUSE_CLICKED, m -> {
            w.selectedProperty().set(true);
        });
        
        windowNameLabel.getStyleClass().add("accordion-name-selected");
        windowNameLabel.setPrefSize(130, 25);
        windowNameLabel.setAlignment(Pos.CENTER_LEFT);
        windowNameLabel.setFocusTraversable(false);
        windowLocationLabel.setVisible(false);
        windowLocationLabel.getStyleClass().add("accordion-location");
        windowLocationLabel.setPrefSize(90, 25);
        windowLocationLabel.setAlignment(Pos.CENTER_RIGHT);
        windowLocationLabel.setFocusTraversable(false);
        checkBox.setPrefSize(90, 15);
        checkBox.setSelected(true);
        checkBox.setDisable(true);
        checkBox.getStyleClass().add("accordion-visible-check");
        checkBox.setAlignment(Pos.CENTER_RIGHT);
        checkBox.setFocusTraversable(false);
        checkBox.selectedProperty().bindBidirectional(w.visibleProperty());
        w.visibleProperty().addListener((v,o,n) -> {
            checkBox.setDisable(n);
        });
        
        if(w.isInput()) {
            titleColorID = new Line(3, 6, 3, 67);
            titleColorID.strokeProperty().bind(w.getTitleBar().getColorIDTab().colorIDProperty());
            titleColorID.strokeWidthProperty().set(4);
            titleColorID.setManaged(false);
        }
        
        Label spacer = new Label("");
        spacer.setPrefSize(90, 20);
        
        setFocusTraversable(false);
        GridPane gp = new GridPane();
        gp.setPrefHeight(60);
        gp.setPrefWidth(150);
        GridPane.setConstraints(windowNameLabel, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(windowLocationLabel, 1, 0, 1, 1, HPos.RIGHT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(spacer, 1, 1, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(checkBox, 1, 2, 1, 1, HPos.RIGHT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        setGraphic(gp);
        gp.getChildren().addAll(windowNameLabel, windowLocationLabel, spacer, checkBox);
        
        Text[] maxes = { new Text("Max"), new Text("Max"), new Text("Max") };
        Arrays.stream(maxes).forEach(t -> { t.setFont(Font.font("Questrial-Regular", 12)); t.setFocusTraversable(false); });
        Text[] returns = { new Text("to return"), new Text("to return"), new Text("to return") };
        Arrays.stream(returns).forEach(t -> { t.setFont(Font.font("Questrial-Regular", 12)); t.setFocusTraversable(false); });
        Text[] descs = { new Text(" Similar Terms "), new Text(" Contexts "), new Text(" Slices ") };
        Arrays.stream(descs)
            .forEach(
                t -> { 
                    t.setFill(Color.rgb(92,183,186)); 
                    t.setFont(Font.font("Questrial-Regular", 12)); 
                    t.setFocusTraversable(false); 
                });
        simTermsMaxAdjuster = getAdjusterAttributePane(
            new TextFlow(maxes[0], descs[0], returns[0]), 
                simTermsResultStartIndexProperty, 
                    simTermsMaxResultsProperty);
        contextsMaxAdjuster = getAdjusterAttributePane(
            new TextFlow(maxes[1], descs[1], returns[1]),
                contextsResultStartIndexProperty,
                    contextsMaxResultsProperty);
        slicesMaxAdjuster = getAdjusterAttributePane(
            new TextFlow(maxes[2], descs[2], returns[2]),
                slicesResultStartIndexProperty,
                    slicesMaxResultsProperty);
        
        attributePane.setPrefWidth(190);
        attributePane.getStyleClass().add("window-title-attribute-contents");
        attributePane.getChildren().add(getWindowCoordinateAttributePane(windowLocationLabel));
                    
        if(!w.isInput()) {
            addAutoSendPropertyHandler();
            
            attributePane.getChildren().add(getShowCompareInfoButtonPane());
            fingerprintSnapshotPane = getFingerprintSnapshotPane();
            compareSnapshotPane = getComparisonSnapshotPane();
            
            ((OutputWindow)w).getViewArea().selectedViewProperty().addListener((v,o,n) -> {
                attributePane.getChildren().remove(fingerprintSnapshotPane);
                attributePane.getChildren().remove(compareSnapshotPane);
                if(n == ViewType.FINGERPRINT) {
                    attributePane.getChildren().add(fingerprintSnapshotPane);
                }else if(n == ViewType.COMPARE){
                    attributePane.getChildren().add(compareSnapshotPane);
                }
            });
            
        }else{
            attributePane.getChildren().addAll(
                getRetinaSelectionAttributePane(), 
                getCompareColorAttributePane(),
                defaultOperatorPane = getDefaultOperatorPane(),
                defaultIndentationPane = getDefaultIndentationPane(),
                clipboardDragModePane = getClipboardDragModePane());
            
            ((InputWindow)w).getViewArea().selectedViewProperty().addListener((v,o,n) -> {
                if(n == ViewType.EXPRESSION) {
                    attributePane.getChildren().add(defaultOperatorPane);
                    attributePane.getChildren().add(defaultIndentationPane);
                    attributePane.getChildren().add(clipboardDragModePane);
                }else{
                    attributePane.getChildren().remove(defaultOperatorPane);
                    attributePane.getChildren().remove(defaultIndentationPane);
                    attributePane.getChildren().remove(clipboardDragModePane);
                }
            });
            
            selectedDefaultOperatorProperty.set(ApplicationService.getInstance().getDefaultOperatorProperty().get());
            ApplicationService.getInstance().getDefaultOperatorProperty().bindBidirectional(selectedDefaultOperatorProperty);
            selectedDefaultOperatorProperty.addListener((v,o,n) -> {
                defaultOperatorChoices.getSelectionModel().select(n);
            });
            
            selectedDefaultIndentationProperty.set(ApplicationService.getInstance().getDefaultIndentationProperty().get());
            ApplicationService.getInstance().getDefaultIndentationProperty().bindBidirectional(selectedDefaultIndentationProperty);
            selectedDefaultIndentationProperty.addListener((v,o,n) -> {
                defaultIndentationChoices.getSelectionModel().select((Integer)n.intValue());
            });
        }
        
        setContent(attributePane);
    }
    
    /**
     * {@inheritDoc}
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if(!window.isInput()) {
            out.writeInt(getResultStartIndexProperty(MaxResultsType.SIMTERMS).get());
            out.writeInt(getMaxResultsProperty(MaxResultsType.SIMTERMS).get());
            out.writeInt(getResultStartIndexProperty(MaxResultsType.CONTEXTS).get());
            out.writeInt(getMaxResultsProperty(MaxResultsType.CONTEXTS).get());
            out.writeInt(getResultStartIndexProperty(MaxResultsType.SLICES).get());
            out.writeInt(getMaxResultsProperty(MaxResultsType.SLICES).get());
            LOGGER.debug("write: \t\n" +
                "simTerms start: " + getResultStartIndexProperty(MaxResultsType.SIMTERMS).get() +
                "\nsimTerms   max: " + getMaxResultsProperty(MaxResultsType.SIMTERMS).get() +
                "\ncontexts start: " + getResultStartIndexProperty(MaxResultsType.CONTEXTS).get() +
                "\ncontexts   max: " + getMaxResultsProperty(MaxResultsType.CONTEXTS).get() +
                "\nslices   start: " + getResultStartIndexProperty(MaxResultsType.SLICES).get() +
                "\nslices     max: " + getMaxResultsProperty(MaxResultsType.SLICES).get());
        } 
    }
    
    /**
     * {@inheritDoc}
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        WindowConfig config = WindowService.getInstance().windowConfigProperty().get();
        if(!config.isInput()) {
            config.simTermsStartIdx = in.readInt();
            config.simTermsMaxIdx = in.readInt();
            config.contextsStartIdx = in.readInt();
            config.contextsMaxIdx = in.readInt();
            config.slicesStartIdx = in.readInt();
            config.slicesMaxIdx = in.readInt();
            LOGGER.debug("read: \t\n" +
                "simTerms start: " + config.simTermsStartIdx +
                "\nsimTerms   max: " + config.simTermsMaxIdx +
                "\ncontexts start: " + config.contextsStartIdx +
                "\ncontexts   max: " + config.contextsMaxIdx +
                "\nslices   start: " + config.slicesStartIdx +
                "\nslices     max: " + config.slicesMaxIdx);
        } 
    }
    
    /**
     * Returns the {@link ConfigHandler} instance used to initialize
     * this {@code WindowTitlePane}'s min/max settings following
     * deserialization of an {@link OutputWindow}.
     * @return
     */
    public final ConfigHandler<? super WindowConfig> getChainHandler() {
        if(configHandler == null) {
            configHandler = config -> {
                LOGGER.debug("Executing WindowTitlePane chain of responsibility handler");
                Arrays.stream(MaxResultsType.values())
                    .filter(t -> t != MaxResultsType.SPACEHOLDER)
                    .forEach(t -> {
                        RangeSlider rs = propertyRebindMap.get(getMaxResultsProperty(t));
                        rs.highValueProperty().set(config.getResultMaxIndex(t));
                        rs.lowValueProperty().set(config.getResultStartIndex(t));
                    });
                
                config.advanceNotificationChain();
            };
        }
        
        return configHandler;
    }
    
    /**
     * Returns the property which contains the current operation mode
     * of the drag pad.
     * @return
     */
    public ObjectProperty<DragMode> clipboardDragModeProperty() {
        return clipboardDragModeProperty;
    }
    
    /**
     * Returns the property set by the user determining the number of
     * spaces to use as the indentation for json printing.
     * @return
     */
    public IntegerProperty defaultIndentationProperty() {
        return selectedDefaultIndentationProperty;
    }
    
    /**
     * Selects the specified {@link FullClient} from the list
     * of clients.
     * 
     * @param client    the {@code FullClient} to select
     */
    public void selectRetina(FullClient client) {
        inputSpecificRetinaChoices.getSelectionModel().select(client);
    }
    
    /**
     * Returns the currently selected Retina client.
     * 
     * @return  the {@link FullClient} currently selected.
     */
    public FullClient getSelectedRetina() {
        return inputSpecificRetinaChoices.getValue();
    }
    
    /**
     * Returns a boolean flag indicating whether the user has chosen to have
     * comparison color for the associated {@link InputWindow} use the color
     * ID of the window or the preset color.
     * 
     * @return  true if the comparison color should be the same as the Window id
     * color, false if not.
     */
    public BooleanProperty compareColorFollowsWindowProperty() {
        return compareColorFollowsWindowProperty;
    }
    
    /**
     * Returns the property which returns the name of the currently selected
     * Retina Client type.
     * 
     * @return
     */
    public StringProperty selectedRetinaNameProperty() {
        return selectedRetinaNameProperty;
    }
    
    /**
     * Returns the property which contains the user's selected default 
     * expression operator.
     * 
     * @return
     */
    public ObjectProperty<Operator> defaultExpressionOperatorProperty() {
        return selectedDefaultOperatorProperty;
    }
    
    /**
     * Called after view transition to set the corresponding max results
     * control for the selected view type.
     * @param type  {@link MaxResultsType}
     */
    public void setMaxResultsControl(MaxResultsType type) {
        switch(type) {
            case SIMTERMS: setMaxAdjuster(simTermsMaxAdjuster); break;
            case CONTEXTS: setMaxAdjuster(contextsMaxAdjuster); break;
            case SLICES: setMaxAdjuster(slicesMaxAdjuster); break;    
            case SPACEHOLDER: setMaxAdjuster(null); break;
        }
    }
    
    /**
     * Sets the default {@link Operator} on the default operator choice box.
     * @param op
     */
    public void setDefaultOperator(Operator op) {
        defaultOperatorChoices.getSelectionModel().select(op);
    }
    
    /**
     * Replaces the current node shown in place of a "max results adjuster"
     * with the specified node.
     * 
     * @param adjuster  a {@link GridPane} containing max results adjustment widgets.
     */
    public void setMaxAdjuster(Node adjuster) {
        attributePane.getChildren().remove(last);
        if(adjuster != null) {
            attributePane.getChildren().add(last = adjuster);
        }
        requestLayout();
    }
    
    /**
     * Returns the property for dirty state handling.
     * @return
     */
    public BooleanProperty dirtyProperty() {
        return dirtyProperty;
    }
    
    /**
     * Returns the property that flags a request to auto-send a 
     * query update.
     * @return
     */
    public OccurrenceProperty autoSendProperty() {
        return windowTitleAutoSendProperty;
    }
    
    /**
     * Returns the property containing the start index reference
     * for the index of where to begin gathering query results.
     * @return
     */
    public IntegerProperty getResultStartIndexProperty(MaxResultsType type) {
        switch(type) {
            case SIMTERMS: return simTermsResultStartIndexProperty;
            case CONTEXTS: return contextsResultStartIndexProperty;
            case SLICES: return slicesResultStartIndexProperty;
            default:
        }
        return null;
    }
    
    /**
     * Returns the property containing the count of maximum results
     * to be returned.
     * @return
     */
    public IntegerProperty getMaxResultsProperty(MaxResultsType type) {
        switch(type) {
            case SIMTERMS: return simTermsMaxResultsProperty;
            case CONTEXTS: return contextsMaxResultsProperty;
            case SLICES: return slicesMaxResultsProperty;
            default:
        }
        return null;
    }
    
    public void setWindowName(String name) {
        windowNameLabel.setText(name);
    }
    
    public String getWindowName() {
        return windowNameLabel.getText();
    }
    
    public UUID getReferredWindowID() {
        return referredWindowID;
    }
    
    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new WindowTitlePaneSkin(this, titleColorID);
    }
    
    /**
     * Adds the auto-send property handler
     */
    private void addAutoSendPropertyHandler() {
        windowTitleAutoSendProperty.addListener((v,o,n) -> {
            OutputWindow ow = (OutputWindow)WindowService.getInstance().windowFor(referredWindowID);
            UUID selectedInput = ow.getInputSelector().getPrimaryInputSelection();
            if(selectedInput != null) {
                EventBus.get().broadcast(BusEvent.INPUT_EVENT_NEW_EXPRESSION_STATE.subj() + selectedInput, new Payload());
            }
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_RE_EXECUTE_LAST_QUERY.subj() + referredWindowID, new Payload());
        });
    }
    
    private StackPane getAdjusterAttributePane(TextFlow desc, IntegerProperty startInt, IntegerProperty maxInt) {
        StackPane radiusVisualGroup1 = new StackPane();
        radiusVisualGroup1.setPrefSize(190, 60);
        radiusVisualGroup1.getStyleClass().add("radius-visual-group");
        
        GridPane attributePane = new GridPane();
        attributePane.getStyleClass().add("out-atts-range");
        attributePane.prefWidthProperty().bind(WindowTitlePane.this.widthProperty());
        attributePane.getColumnConstraints().add(new ColumnConstraints(14));
        attributePane.getColumnConstraints().add(new ColumnConstraints(15));
        attributePane.getColumnConstraints().add(new ColumnConstraints(3));
        attributePane.getColumnConstraints().add(new ColumnConstraints(33));
        attributePane.getColumnConstraints().add(new ColumnConstraints(34));
        attributePane.getColumnConstraints().add(new ColumnConstraints(33));
        attributePane.getColumnConstraints().add(new ColumnConstraints(3));
        attributePane.getColumnConstraints().add(new ColumnConstraints(15));
        attributePane.getColumnConstraints().add(new ColumnConstraints(14));
        
        desc.setTextAlignment(TextAlignment.CENTER);
        desc.setFocusTraversable(false);
        
        Label left = new Label("Start Index");
        left.setFont(Font.font(9));
        left.setFocusTraversable(false);
        TextField start = new TextField();
        start.setAlignment(Pos.BASELINE_RIGHT);
        start.setFocusTraversable(false);
        Label right = new Label("End Index");
        right.setFont(Font.font(9));
        right.setFocusTraversable(false);
        TextField end = new TextField();
        end.setAlignment(Pos.BASELINE_RIGHT);
        end.setFocusTraversable(false);
        
        //Struts to space between textfields and the rangeslider
        Region lStrut = new Region();
        lStrut.setMaxWidth(3);
        lStrut.setPrefWidth(3);
        Region rStrut = new Region();
        rStrut.setMaxWidth(3);
        rStrut.setPrefWidth(3);
       
        RangeSlider hSlider = new RangeSlider(0, 100, 0, 10);
        hSlider.setFocusTraversable(false);
        hSlider.setShowTickMarks(true);
        hSlider.setShowTickLabels(true);
        hSlider.setBlockIncrement(10);
        hSlider.setMajorTickUnit(20);
        hSlider.snapToTicksProperty().set(true);
        hSlider.setSnapToTicks(true);
        
        NumberFormat nf = NumberFormat.getIntegerInstance();
        nf.setParseIntegerOnly(true);
        NumberStringConverter converter = new NumberStringConverter(nf);
        start.textProperty().bindBidirectional(hSlider.lowValueProperty(), converter);
        end.textProperty().bindBidirectional(hSlider.highValueProperty(), converter);
        startInt.bind(hSlider.lowValueProperty());
        maxInt.bind(hSlider.highValueProperty().subtract(hSlider.lowValueProperty()));
        
        // Mapping to be used later during deserialization to set the source 
        // slider values of bound properties which cannot be altered due to
        // their being "bound"
        propertyRebindMap.put(startInt, hSlider);
        propertyRebindMap.put(maxInt, hSlider);
        
        startInt.addListener((v,o,n) -> { 
            dirtyProperty.set(true);
            if(hSlider.isHighValueChanging() || hSlider.isLowValueChanging()) return;
            windowTitleAutoSendProperty.set(); 
        });
        maxInt.addListener((v,o,n) -> { 
            dirtyProperty.set(true); 
            if(hSlider.isHighValueChanging() || hSlider.isLowValueChanging()) return;
            windowTitleAutoSendProperty.set(); 
        });
        
        GridPane.setConstraints(desc, 0, 0, 9, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(hSlider, 3, 2, 3, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(lStrut, 2, 2, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(rStrut, 6, 2, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(left, 0, 1, 4, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);
        GridPane.setConstraints(right, 5, 1, 4, 1, HPos.RIGHT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);
        GridPane.setConstraints(start, 0, 2, 2, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);
        GridPane.setConstraints(end, 7, 2, 2, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);
        
        attributePane.getChildren().addAll(desc, left, right, start, hSlider, end);
        
        radiusVisualGroup1.getChildren().add(attributePane);
        
        return radiusVisualGroup1;
    }
    
    private StackPane getWindowCoordinateAttributePane(Label locLabel) {
        StackPane radiusVisualGroup = new StackPane();
        radiusVisualGroup.setPrefSize(190, 25);
        radiusVisualGroup.getStyleClass().add("radius-visual-group");
        
        CheckBox coordsBox = new CheckBox("Show Window Coordinates");
        coordsBox.setFocusTraversable(false);
        coordsBox.setPrefWidth(180);
        coordsBox.setPrefHeight(15);
        coordsBox.setMaxHeight(15);
        
        coordsBox.selectedProperty().addListener((v,o,n) -> {
            windowLocationLabel.setText("[" + String.format("%d",(long)window.getLayoutX()) + ", " + String.format("%d", (long)window.getLayoutY()) + "]");
            locLabel.setVisible(n);
        });
        
        radiusVisualGroup.getChildren().add(coordsBox);
        
        return radiusVisualGroup;
    }
    
    /**
     * Creates and returns the Pane containing the per-window Retina
     * selection combo box.
     * 
     * @param locLabel      
     * @return
     */
    private StackPane getRetinaSelectionAttributePane() {
        StackPane radiusVisualGroup = new StackPane();
        radiusVisualGroup.setPrefSize(190, 40);
        radiusVisualGroup.getStyleClass().add("radius-visual-group");
        
        VBox visGroupBox = new VBox(3);
        visGroupBox.setPrefWidth(190);
        visGroupBox.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        
        Label retinaChoiceLabel = new Label("Select Input-specific Retina:");
        retinaChoiceLabel.getStyleClass().add("radius-visual-group-label");
        retinaChoiceLabel.setPrefWidth(190);
        retinaChoiceLabel.setPrefHeight(10);
        
        inputSpecificRetinaChoices = new ComboBox<>();
        inputSpecificRetinaChoices.getStyleClass().add("window-title-retina-choice-box");
        inputSpecificRetinaChoices.setFocusTraversable(false);
        inputSpecificRetinaChoices.setPrefSize(170, 15);
        inputSpecificRetinaChoices.getItems().addAll(RetinaClientFactory.getRetinaClients());
        inputSpecificRetinaChoices.setPromptText("Choose Retina");
        inputSpecificRetinaChoices.setConverter(new StringConverter<FullClient>() {
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
        inputSpecificRetinaChoices.getSelectionModel().selectedItemProperty().addListener((v,o,n) -> {
            selectedRetinaNameProperty.set(RetinaClientFactory.getRetinaName(n));
        });
        
        visGroupBox.getChildren().addAll(retinaChoiceLabel, inputSpecificRetinaChoices);
        
        radiusVisualGroup.getChildren().add(visGroupBox);
        
        return radiusVisualGroup;
    }
    
    /**
     * Creates a pane which toggles the option to use window
     * id colors as comparison colors.
     * 
     * @return
     */
    private StackPane getCompareColorAttributePane() {
        StackPane radiusVisualGroup = new StackPane();
        radiusVisualGroup.setPrefSize(190, 25);
        radiusVisualGroup.getStyleClass().add("radius-visual-group");
        
        CheckBox useWinColorBox = new CheckBox("Use ID Color in Compare");
        useWinColorBox.setFocusTraversable(false);
        useWinColorBox.setPrefWidth(180);
        useWinColorBox.setPrefHeight(15);
        useWinColorBox.setMaxHeight(15);
        
        useWinColorBox.selectedProperty().addListener((v,o,n) -> {
            compareColorFollowsWindowProperty.set(n);
        });
        
        radiusVisualGroup.getChildren().add(useWinColorBox);
        
        return radiusVisualGroup;
    }
    
    private StackPane getDefaultOperatorPane() {
        StackPane radiusVisualGroup = new StackPane();
        radiusVisualGroup.setPrefSize(190, 40);
        radiusVisualGroup.getStyleClass().add("radius-visual-group");
        
        VBox visGroupBox = new VBox(3);
        visGroupBox.setPrefWidth(190);
        visGroupBox.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        
        Label operatorChoiceLabel = new Label("Default Expression Operator:");
        operatorChoiceLabel.getStyleClass().add("radius-visual-group-label");
        operatorChoiceLabel.setPrefWidth(190);
        operatorChoiceLabel.setPrefHeight(10);
        
        defaultOperatorChoices = new ComboBox<>();
        defaultOperatorChoices.getStyleClass().add("window-title-retina-choice-box");
        defaultOperatorChoices.setFocusTraversable(false);
        defaultOperatorChoices.setPrefSize(170, 15);
        defaultOperatorChoices.getItems().addAll(
            Arrays.stream(Operator.values())
                .filter(op -> op != Operator.L_PRN && op != Operator.R_PRN)
                .toArray(Operator[]::new));                
        defaultOperatorChoices.setPromptText("Choose Default Operator");
        defaultOperatorChoices.setConverter(new StringConverter<Operator>() {
            @Override public String toString(Operator object) {
                return "Operator:    " + object.toString();
            }
            @Override public Operator fromString(String string) {
                return Operator.typeFor(string);
            }
        });
        defaultOperatorChoices.getSelectionModel().selectedItemProperty().addListener((v,o,n) -> {
            selectedDefaultOperatorProperty.set(n);
        });
        Label globalWarning = new Label("This is a GLOBAL setting.");
        globalWarning.getStyleClass().add("radius-visual-group-label-small");
        globalWarning.setPrefWidth(190);
        globalWarning.setPrefHeight(6);
        
        visGroupBox.getChildren().addAll(operatorChoiceLabel, defaultOperatorChoices, globalWarning);
        
        radiusVisualGroup.getChildren().add(visGroupBox);
        
        Platform.runLater(() -> defaultOperatorChoices.getSelectionModel().select(
            ApplicationService.getInstance().getDefaultOperatorProperty().get()));
        
        return radiusVisualGroup;
    }
    
    private StackPane getDefaultIndentationPane() {
        StackPane radiusVisualGroup = new StackPane();
        radiusVisualGroup.setPrefSize(190, 40);
        radiusVisualGroup.getStyleClass().add("radius-visual-group");
        
        VBox visGroupBox = new VBox(3);
        visGroupBox.setPrefWidth(190);
        visGroupBox.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        
        Label jsonIndentationLabel = new Label("Default JSON Indentation:");
        jsonIndentationLabel.getStyleClass().add("radius-visual-group-label");
        jsonIndentationLabel.setPrefWidth(190);
        jsonIndentationLabel.setPrefHeight(10);
        
        defaultIndentationChoices = new ComboBox<>();
        defaultIndentationChoices.getStyleClass().add("window-title-retina-choice-box");
        defaultIndentationChoices.setFocusTraversable(false);
        defaultIndentationChoices.setPrefSize(170, 15);
        defaultIndentationChoices.getItems().addAll(1, 2, 4, 6, 8, 10, 12);                
        defaultIndentationChoices.setPromptText("Choose Default Operator");
        defaultIndentationChoices.setConverter(new StringConverter<Integer>() {
            @Override public String toString(Integer object) {
                return "Indent:  " + object.toString() + " spaces";
            }
            @Override public Integer fromString(String string) {
                string = string.substring(string.indexOf(" ")).trim();
                string = string.substring(0, string.indexOf(" ")).trim();
                return Integer.valueOf(string);
            }
        });
        defaultIndentationChoices.getSelectionModel().selectedItemProperty().addListener((v,o,n) -> {
            selectedDefaultIndentationProperty.set(n);
        });
        Label globalWarning = new Label("This is a GLOBAL setting.");
        globalWarning.getStyleClass().add("radius-visual-group-label-small");
        globalWarning.setPrefWidth(190);
        globalWarning.setPrefHeight(6);
        
        visGroupBox.getChildren().addAll(jsonIndentationLabel, defaultIndentationChoices, globalWarning);
        
        radiusVisualGroup.getChildren().add(visGroupBox);
        
        Platform.runLater(() -> defaultIndentationChoices.getSelectionModel().select(
            Integer.valueOf(ApplicationService.getInstance().getDefaultIndentationProperty().get())));
        
        return radiusVisualGroup;
    }
    
    private StackPane getShowCompareInfoButtonPane() {
        StackPane radiusVisualGroup = new StackPane();
        radiusVisualGroup.setPrefSize(190, 35);
        radiusVisualGroup.getStyleClass().add("radius-visual-group");
        
        VBox visGroupBox = new VBox(3);
        visGroupBox.setPrefWidth(190);
        visGroupBox.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        
        CheckBox showInfoBox = new CheckBox("Show Compare Info Button");
        showInfoBox.setFocusTraversable(false);
        showInfoBox.setPrefWidth(180);
        showInfoBox.setPrefHeight(15);
        showInfoBox.setMaxHeight(15);
        
        Label globalWarning = new Label("This is a GLOBAL setting.");
        globalWarning.getStyleClass().add("radius-visual-group-label-small");
        globalWarning.setPrefWidth(190);
        globalWarning.setPrefHeight(6);
        
        showInfoBox.selectedProperty().bindBidirectional(WindowService.getInstance().compareInfoButtonVisibleProperty());
        
        visGroupBox.getChildren().addAll(showInfoBox, globalWarning);
        
        radiusVisualGroup.getChildren().add(visGroupBox);
        
        return radiusVisualGroup;
    }
    
    private StackPane getClipboardDragModePane() {
        StackPane radiusVisualGroup = new StackPane();
        radiusVisualGroup.setPrefSize(190, 40);
        radiusVisualGroup.getStyleClass().add("radius-visual-group");
        
        VBox visGroupBox = new VBox(3);
        visGroupBox.getStyleClass().addAll("window-title-drag-mode-toggle");
        visGroupBox.setPrefWidth(190);
        visGroupBox.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        
        HBox labels = new HBox(20);
        Label dragModeLabel = new Label("Drag Mode:");
        dragModeLabel.getStyleClass().add("radius-visual-group-label");
        dragModeLabel.setPrefWidth(100);
        dragModeLabel.setPrefHeight(10);
        
        Label dragInstructions = new Label("Alt-D to Activate");
        dragInstructions.getStyleClass().add("radius-visual-group-label-medium");
        dragInstructions.setPrefWidth(100);
        dragInstructions.setPrefHeight(10);
        labels.getChildren().addAll(dragModeLabel, dragInstructions);
                
        HBox toggleButtonBox = new HBox(5);
        
        ToggleButton freehand = new ToggleButton(DragMode.FREEHAND.toDisplay());
        freehand.setFocusTraversable(false);
        freehand.setPrefWidth(92.5);
        freehand.setPrefHeight(20);
        ToggleButton json = new ToggleButton(DragMode.TERM_OR_JSON.toDisplay());
        json.setFocusTraversable(false);
        json.setSelected(true);
        json.setPrefWidth(92.5);
        json.setPrefHeight(20);
        ToggleGroup group = new ToggleGroup();
        group.getToggles().addAll(freehand, json);
        group.selectedToggleProperty().addListener((v,o,n) -> {
            clipboardDragModeProperty.set(n == json ? DragMode.TERM_OR_JSON : DragMode.FREEHAND);
        });
        toggleButtonBox.getChildren().addAll(freehand, json);
        
        Button example = new Button("Show example");
        example.setOnAction(e -> {
            Flyout actionable = group.getSelectedToggle() == freehand ? exampleFlyout : exampleFlyout2;
            if(!actionable.flyoutShowing()) {
                actionable.flyout();
                example.setText("Hide example");
            } else {
                actionable.dismiss();
                example.setText("Show example");
            }
        });
        example.setFocusTraversable(false);
        example.setPrefWidth(190);
        example.setPrefHeight(20);
        
        exampleFlyout = getFreeHandExample(example);
        exampleFlyout2 = getJsonExample(example);
        
        visGroupBox.getChildren().addAll(labels, toggleButtonBox, example, exampleFlyout, exampleFlyout2);
        
        radiusVisualGroup.getChildren().add(visGroupBox);
        
        Platform.runLater(() -> group.selectToggle(json));
                
        return radiusVisualGroup;
    }
    
    private Flyout getFreeHandExample(Button trigger) {
        VBox ex1 = new VBox(5);
        ex1.setPrefHeight(100);
        ex1.setStyle("-fx-background-color: transparent;-fx-padding: 5 10 25 10;");
        
        Text title = new Text("Freehand");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setFill(Color.WHITE);
        
        TextFlow titleFlow = new TextFlow(title);
        titleFlow.setTextAlignment(TextAlignment.CENTER);
        
        Text simple = new Text("Simple:\n");
        simple.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        simple.setFill(Color.WHITE);
        
        Text freehandExample = new Text("\nApple ! Fruit\n\n\n");
        freehandExample.setFont(Font.font("Questrial", FontWeight.NORMAL, 14));
        freehandExample.setFill(Color.WHITE);
        
        Text complex = new Text("With Parenthesis:\t\t\t");
        complex.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        complex.setFill(Color.WHITE);
        
        Button copyTermButton = new Button("Copy");
        addButtonEventStyling(copyTermButton, 10);
        copyTermButton.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString("Fox | (Rabbit ^ (Rodent & Squirrel))");
            Clipboard.getSystemClipboard().setContent(content);
        });
        
        TextFlow complexTitleFlow = new TextFlow(complex, copyTermButton);
        
        Text freehandExample2 = new Text("\n\nFox | (Rabbit ^ (Rodent & Squirrel))\n\n");
        freehandExample2.setFont(Font.font("Questrial", FontWeight.NORMAL, 14));
        freehandExample2.setFill(Color.WHITE);
        
        TextFlow tf = new TextFlow(simple, freehandExample, complexTitleFlow, freehandExample2);
        tf.setTextAlignment(TextAlignment.LEFT);
        tf.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        
        Button close = new Button("Ok");
        close.setFocusTraversable(false);
        close.setManaged(false);
        close.setOnAction(e -> { exampleFlyout.dismiss(); trigger.setText("Show example"); });
        close.setFont(Font.font("Questrial", FontWeight.NORMAL, 12));
        String normalStyle = "-fx-background-color: rgb(237, 93, 37); -fx-background-radius: 5; -fx-text-fill: white; -fx-padding: 2 10 2 10; -fx-font-family: \"Questrial\"; -fx-font-size: 10";
        close.setStyle(normalStyle);
        close.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            close.setStyle("-fx-background-color: rgb(237, 93, 37); -fx-background-radius: 5; -fx-text-fill: white; -fx-padding: 2 10 2 10; -fx-font-family: \"Questrial\"; -fx-font-size: 14");
        });
        close.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            close.setStyle(normalStyle);
        });
        close.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            close.setStyle("-fx-background-color: linear-gradient(to bottom, rgb(188, 72, 27) 0%, rgb(255, 134, 68) 100%);" + normalStyle.substring(normalStyle.indexOf(";") + 1));
        });
        close.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> close.setStyle(normalStyle));
        
        close.resize(100, 20);
        close.setAlignment(Pos.BASELINE_CENTER);
        ex1.layoutBoundsProperty().addListener((v,o,n) -> {
            close.relocate((n.getWidth() / 2 - close.getWidth() / 2), n.getHeight() - 25);
        });
        
        ex1.getChildren().addAll(titleFlow, tf, close);
        
        Flyout exampleFlyout = new Flyout(trigger, ex1);
        exampleFlyout.setFlyoutStyle("-fx-background-color: rgb(49, 109, 160, .8); -fx-background-radius: 5 5 5 5;");
        exampleFlyout.setFlyoutSide(Side.TOP);
        
        return exampleFlyout;
    }
    
    private Flyout getJsonExample(Button trigger) {
        VBox ex1 = new VBox(5);
        ex1.setPrefHeight(100);
        ex1.setStyle("-fx-background-color: transparent;-fx-padding: 5 10 25 10;");
        
        Text title = new Text("Term or Json");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setFill(Color.WHITE);
        
        TextFlow titleFlow = new TextFlow(title);
        titleFlow.setTextAlignment(TextAlignment.CENTER);
        
        Text simple = new Text("Simple Term:\n");
        simple.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        simple.setFill(Color.WHITE);
        
        Text t = new Text("jaguar\n\n");
        t.setFont(Font.font("Questrial", FontWeight.NORMAL, 14));
        t.setFill(Color.WHITE);
        
        Text jsonTermTitle = new Text("JSON Term:\t\t\t\t");
        jsonTermTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        jsonTermTitle.setFill(Color.WHITE);
        
        Button copyTermButton = new Button("Copy");
        addButtonEventStyling(copyTermButton, 10);
        copyTermButton.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString("{ \"term\": \"jaguar\" }");
            Clipboard.getSystemClipboard().setContent(content);
        });
        
        TextFlow termTitleFlow = new TextFlow(jsonTermTitle, copyTermButton);
        
        String json = "{ \"term\": \"jaguar\" }";
        Text termJson = new Text("\n" + prettyPrint(json) + "\n\n");
        ApplicationService.getInstance().getDefaultIndentationProperty().addListener((v,o,n) -> {
            termJson.setText("\n" + prettyPrint(json) + "\n\n");
        });
        termJson.setFont(Font.font("Questrial", FontWeight.NORMAL, 14));
        termJson.setFill(Color.WHITE);
        
        Text complexTitle = new Text("JSON Expression:\t\t\t");
        complexTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        complexTitle.setFill(Color.WHITE);
        
        String exJson = "{ \"and\":[{" +
                "\"term\":\"Rabbit\"" +
              "},{" +
                "\"term\":\"Rodent\"" +
               "}]" +
              " }";
        
        Button copyComplexButton = new Button("Copy");
        addButtonEventStyling(copyComplexButton, 10);
        copyComplexButton.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(exJson);
            Clipboard.getSystemClipboard().setContent(content);
        });
        
        TextFlow complexTitleFlow = new TextFlow(complexTitle, copyComplexButton);
        
        Text expressionJson = new Text("\n" + prettyPrint(exJson) + "\n\n");
        ApplicationService.getInstance().getDefaultIndentationProperty().addListener((v,o,n) -> {
            expressionJson.setText("\n" + prettyPrint(exJson) + "\n\n");
        });
        expressionJson.setFont(Font.font("Questrial", FontWeight.NORMAL, 14));
        expressionJson.setFill(Color.WHITE);
        
        TextFlow tf = new TextFlow(simple, t, termTitleFlow, termJson, complexTitleFlow, expressionJson);
        tf.setTextAlignment(TextAlignment.LEFT);
        tf.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        
        Button close = new Button("Ok");
        close.setFocusTraversable(false);
        close.setManaged(false);
        close.setOnAction(e -> { exampleFlyout2.dismiss(); trigger.setText("Show example"); });
        addButtonEventStyling(close, 12);
        
        close.resize(100, 20);
        close.setAlignment(Pos.BASELINE_CENTER);
        ex1.layoutBoundsProperty().addListener((v,o,n) -> {
            close.relocate((n.getWidth() / 2 - close.getWidth() / 2), n.getHeight() - 25);
        });
        
        ex1.getChildren().addAll(titleFlow, tf, close);
        
        Flyout exampleFlyout = new Flyout(trigger, ex1);
        exampleFlyout.setFlyoutStyle("-fx-background-color: rgb(49, 109, 160, .8); -fx-background-radius: 5 5 5 5;");
        exampleFlyout.setFlyoutSide(Side.TOP);
        
        return exampleFlyout;
    }
    
    private void addButtonEventStyling(Button button, int fontSize) {
        button.setFont(Font.font("Questrial", FontWeight.NORMAL, fontSize));
        String normalStyle = "-fx-background-color: rgb(237, 93, 37); -fx-background-radius: 5; -fx-text-fill: white; -fx-padding: 2 10 2 10; -fx-font-family: \"Questrial\"; -fx-font-size: " + fontSize;
        button.setStyle(normalStyle);
        button.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            button.setStyle("-fx-background-color: rgb(237, 93, 37); -fx-background-radius: 5; -fx-text-fill: white; -fx-padding: 2 10 2 10; -fx-font-family: \"Questrial\"; -fx-font-size: " + (fontSize + 2));
        });
        button.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            button.setStyle(normalStyle);
        });
        button.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            button.setStyle("-fx-background-color: linear-gradient(to bottom, rgb(188, 72, 27) 0%, rgb(255, 134, 68) 100%);" + normalStyle.substring(normalStyle.indexOf(";") + 1));
        });
        button.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> button.setStyle(normalStyle));
    }

    private String prettyPrint(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object jsonObj = mapper.readValue(json, Object.class);
            indentor = new ConfigurableIndentor(
                ApplicationService.getInstance().getDefaultIndentationProperty().get());
            indentor.indentationProperty().bind(ApplicationService.getInstance().getDefaultIndentationProperty());
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
            printer.indentObjectsWith(indentor);
            printer.indentArraysWith(indentor);
            json = mapper.writer(printer).writeValueAsString(jsonObj);
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        return json;
    }
    
    private StackPane getFingerprintSnapshotPane() {
        StackPane radiusVisualGroup = new StackPane();
        radiusVisualGroup.setPrefSize(190, 35);
        radiusVisualGroup.getStyleClass().add("radius-visual-group");
        
        VBox visGroupBox = new VBox(3);
        visGroupBox.getStyleClass().add("window-title-drag-mode-toggle");
        visGroupBox.setPrefWidth(190);
        visGroupBox.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        
        Label snapshotLabel = new Label("Snapshot PNG:  Fingerprint");
        snapshotLabel.getStyleClass().add("radius-visual-group-label");
        snapshotLabel.setPrefWidth(190);
        snapshotLabel.setPrefHeight(10);
        
        Button snapshotButton = new Button("Snapshot");
        snapshotButton.setFocusTraversable(false);
        snapshotButton.setPrefWidth(190);
        snapshotButton.setPrefHeight(15);
        snapshotButton.setOnAction(e -> {
            View view = ((OutputWindow)window).getViewArea().getSelectedView();
            FingerprintDisplay display = (FingerprintDisplay)view;
            Impression i = display.getImpression();
            Image image = SnapshotAssistant.snapshot(i, Color.WHITE);
            System.out.println("Got fingerprint image = " + image);
            Window.SNAPSHOT_FUNCTION.accept(window.getWindowID(), new Pair<Image, String>(image, "Fingerprint Display"));
        });
        
        visGroupBox.getChildren().addAll(snapshotLabel, snapshotButton);
        
        radiusVisualGroup.getChildren().add(visGroupBox);
        
        return radiusVisualGroup;
    }
    
    private StackPane getComparisonSnapshotPane() {
        StackPane radiusVisualGroup = new StackPane();
        radiusVisualGroup.setPrefSize(190, 35);
        radiusVisualGroup.getStyleClass().add("radius-visual-group");
        
        VBox visGroupBox = new VBox(3);
        visGroupBox.getStyleClass().add("window-title-drag-mode-toggle");
        visGroupBox.setPrefWidth(190);
        visGroupBox.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        
        Label snapshotLabel = new Label("Snapshot PNG:  Compare Display");
        snapshotLabel.getStyleClass().add("radius-visual-group-label");
        snapshotLabel.setPrefWidth(190);
        snapshotLabel.setPrefHeight(10);
        
        Button snapshotButton = new Button("Snapshot");
        snapshotButton.setFocusTraversable(false);
        snapshotButton.setPrefWidth(190);
        snapshotButton.setPrefHeight(15);
        snapshotButton.setOnAction(e -> {
            View view = ((OutputWindow)window).getViewArea().getSelectedView();
            CompareDisplay display = (CompareDisplay)view;
            Node node = display.getDisplayedNode();
            Image image = SnapshotAssistant.snapshot(node, Color.WHITE);
            String description = (node instanceof Impression) ? "Comparison Fingerprint" : "Metrics Display";
            
            Window.SNAPSHOT_FUNCTION.accept(window.getWindowID(), new Pair<Image, String>(image, description));
        });
        
        visGroupBox.getChildren().addAll(snapshotLabel, snapshotButton);
        
        radiusVisualGroup.getChildren().add(visGroupBox);
        
        return radiusVisualGroup;
    }
}
