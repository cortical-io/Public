package io.cortical.iris;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cortical.fx.webstyle.Impression;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.ContentPane;
import io.cortical.iris.ui.ControlPane;
import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import io.cortical.iris.ui.custom.widget.WindowTitlePane;
import io.cortical.iris.window.BackPanelSupplier;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.iris.window.Window.Type;
import io.cortical.iris.window.WindowController;
import io.cortical.retina.client.FullClient;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

/**
 * Provides window level services such as:
 * <ul>
 *  <li>Locating the containing window for a given node</li>
 *  <li>Providing access to key properties such as:
 *      <ul>
 *          <li>inputWindowProperty - lists all the loaded {@link InputWindow}s</li>
 *          <li>outputWindowProperty - lists all the loaded {@link OutputWindow}s</li>
 *          <li>inputWindowDragStopProperty - Provides location information for OutputWindows observing InputWindows; upon window drag stopping</li>
 *          <li>inputWindowTitleChangeProperty - Notifies listeners of {@link Window} title changes</li>
 *      </ul>
 *  </li>
 * </ul>       
 */
public class WindowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowService.class);
    
    private static final double DEFAULT_OUTPUT_WINDOW_HEIGHT = 570;
    
    private Stage stage;
    
    private ObservableList<Window> inputWindows = FXCollections.observableArrayList();
    private ObservableList<Window> outputWindows = FXCollections.observableArrayList();
    
    private ReadOnlyObjectWrapper<ObservableList<UUID>> inputWindowListProperty;
    private ReadOnlyObjectWrapper<ObservableList<UUID>> outputWindowListProperty;
    private ReadOnlyObjectWrapper<Window> inputWindowDragStopProperty;
    private ReadOnlyObjectWrapper<String> inputWindowTitleChangeProperty;
    private ReadOnlyObjectWrapper<Paint> inputWindowColorIDProperty;
    private ReadOnlyObjectWrapper<Window> selectedWindowProperty;
    private ReadOnlyObjectWrapper<WindowConfig> windowConfigProperty;
    private ReadOnlyBooleanWrapper contentPaneCreatedProperty;
    
    private OccurrenceProperty snapshotClearanceProperty;
    
    private BooleanProperty compareInfoButtonVisibleProperty = new SimpleBooleanProperty(true);
    
    private static WindowService instance; 
    
    private StackPane windowPane;
    private ContentPane contentPane;
    
    private Point2D initialInputLoc;
    private Point2D initialOutputLoc;
    private Point2D defaultInputLoc;
    private Point2D defaultOutputLoc;
    
    private IntegerProperty inputWindowCount;
    private IntegerProperty outputWindowCount;
    
    private IntegerProperty inputWindowCreationCount;
    private IntegerProperty outputWindowCreationCount;
    
    private ObjectProperty<Window> currentFlashingWindow;
    
    /**
     * Private constructor for singleton instance.
     */
    private WindowService() {
        initialInputLoc = new Point2D(10, 10);
        defaultInputLoc = new Point2D(10, 10);
        initialOutputLoc = new Point2D(10, 570);
        defaultOutputLoc = new Point2D(0, 570);
        
        inputWindows.addListener((ListChangeListener.Change<? extends Object> c) -> {
            updateInputLoc();
        });
        
        outputWindows.addListener((ListChangeListener.Change<? extends Object> c) -> {
            updateOutputLoc();
        });
        
        inputWindowListProperty = new ReadOnlyObjectWrapper<>();
        inputWindowListProperty.set(
            FXCollections.observableArrayList(
                inputWindows.stream()
                .map(w -> w.getWindowID())
                .collect(Collectors.toList())));
        
        outputWindowListProperty = new ReadOnlyObjectWrapper<>();
        outputWindowListProperty.set(
            FXCollections.observableArrayList(
                outputWindows.stream()
                .map(w -> w.getWindowID())
                .collect(Collectors.toList())));
        
        inputWindowDragStopProperty = new ReadOnlyObjectWrapper<>();
        
        inputWindowTitleChangeProperty = new ReadOnlyObjectWrapper<>();
        
        inputWindowColorIDProperty = new ReadOnlyObjectWrapper<>();
        
        selectedWindowProperty = new ReadOnlyObjectWrapper<Window>();
        
        inputWindowCount = new SimpleIntegerProperty();
        outputWindowCount = new SimpleIntegerProperty();
        
        inputWindowCreationCount = new SimpleIntegerProperty();
        outputWindowCreationCount = new SimpleIntegerProperty();
        
        currentFlashingWindow = new SimpleObjectProperty<>();
        
        windowConfigProperty = new ReadOnlyObjectWrapper<>();
        
        contentPaneCreatedProperty = new ReadOnlyBooleanWrapper();
        
        snapshotClearanceProperty = new OccurrenceProperty();
        
        snapshotClearanceProperty.addListener((v,o,n) -> {
            LOGGER.debug("Clearing all InputWindow snapshots.");
            inputWindows.stream()
                .map(w -> (InputWindow)w)
                .forEach(w -> w.clearSnapshot());
        });
    }
    
    /**
     * Returns the singleton instance of this {@code WindowService}
     * @return
     */
    public static WindowService getInstance() {
        if(instance == null) {
            instance = new WindowService();
        }
        return instance;
    }
    
    /**
     * Returns the property controlling the visibility of the compare info buttons
     * @return
     */
    public BooleanProperty compareInfoButtonVisibleProperty() {
        return compareInfoButtonVisibleProperty;
    }
    
    /**
     * Returns the read-only property which yields the current {@link WindowConfig}
     * of the {@link Window} being serialized or reified.
     * @return
     */
    public ReadOnlyObjectProperty<WindowConfig> windowConfigProperty() {
        return windowConfigProperty.getReadOnlyProperty();
    }
    
    /**
     * Returns the currently selected Window or the Window with focus.
     * @return
     */
    public Window getSelectedWindow() {
        return selectedWindowProperty.get();
    }
    
    /**
     * Returns a read-only counter which increments every time a new 
     * {@link InputWindow} is created.
     * @return  the InputWindow counter property
     */
    public ReadOnlyIntegerProperty inputWindowCounter() {
        return ReadOnlyIntegerProperty.readOnlyIntegerProperty(inputWindowCount);
    }
    
    /**
     * Returns a read-only counter which increments every time a new 
     * {@link OutputWindow} is created.
     * @return  the OutputWindow counter property
     */
    public ReadOnlyIntegerProperty outputWindowCounter() {
        return ReadOnlyIntegerProperty.readOnlyIntegerProperty(outputWindowCount);
    }
    
    /**
     * Returns the property which is set when the UI's content structure is viable.
     * @return
     */
    public ReadOnlyBooleanProperty contentCreatedProperty() {
        return contentPaneCreatedProperty.getReadOnlyProperty();
    }
    
    /**
     * Returns the property that is set following a drag-n-drop operation
     * involving a Fingerprint {@link Impression}.
     * @return
     */
    public OccurrenceProperty snapshotClearanceProperty() {
        return snapshotClearanceProperty;
    }
    
    /**
     * Sets the {@link Stage} associated with this {@code WindowService}
     * @param stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Returns the {@link Stage} associated with this {@code WindowService}
     * @return
     */
    public Stage getStage() {
        return this.stage;
    }
    
    /**
     * Sets the {@link ContentPane} which is the main application work 
     * surface.
     * 
     * @param contentPane   the main application work surface
     */
    public void setContentPane(ContentPane contentPane) {
        this.contentPane = contentPane;
        this.windowPane = contentPane.getWindowPane();
        Platform.runLater(() -> {
            defaultOutputLoc = new Point2D(10, windowPane.getHeight() - DEFAULT_OUTPUT_WINDOW_HEIGHT - 20);
            initialOutputLoc = new Point2D(10, defaultOutputLoc.getY());
            
            contentPaneCreatedProperty.set(true);
        });
        windowPane.layoutBoundsProperty().addListener((v,o,n) -> {
            defaultOutputLoc = new Point2D(10, windowPane.getHeight() - DEFAULT_OUTPUT_WINDOW_HEIGHT - 20);
        });
    }
    
    /**
     * Returns the main content holder of the application.
     * @return
     */
    public ContentPane getContentPane() {
        return contentPane;
    }
    
    /**
     * Adds a new {@link InputWindow} to the window pane.
     * 
     * @return  the newly added InputWindow
     */
    public InputWindow addInputWindow() {
        InputWindow inputWindow = new InputWindow();
        inputWindow.relocate(initialInputLoc.getX(), initialInputLoc.getY());
        windowPane.getChildren().add(inputWindow);
        
        // Notify all output windows (or any other listener) that an InputWindow has been dragged
        // (important for the OutputWindow's InputWindow-list)
        inputWindow.getTitleBar().titleBarStateProperty().addListener((v,o,n) -> {
            if(n.dragStopped()) {
                inputWindowDragStopProperty.set(inputWindow);
                inputWindowDragStopProperty.set(null);
            }
        });
        
        // Notify all output windows (or any other listener) that an InputWindow's title has changed
        inputWindow.getTitleBar().titleSetProperty().addListener((v,o,n) -> {
            inputWindowTitleChangeProperty.set(n);
        });
        
        // Notify all output windows (or any other listener) that an Inputwindow's color id has changed
        inputWindow.getTitleBar().getColorIDTab().colorIDProperty().addListener((v,o,n) -> {
            inputWindowColorIDProperty.set(n);
        });
        
        inputWindows.add(inputWindow);
        inputWindowListProperty.get().add(inputWindow.getWindowID());
        
        inputWindow.selectedProperty().addListener(getSelectedWindowListener(inputWindow));
        selectWindow(inputWindow);
        
        new WindowController(inputWindow);
        
        inputWindowCreationCount.set(inputWindowCreationCount.get() + 1);
        Platform.runLater(() -> {
            inputWindow.getTitleBar().setTitleWithEvent("Input " + inputWindowCreationCount.get());
        });
        
        // Add the corresponding WindowTitle
        ControlPane controlPane = contentPane.getControlPane();
        controlPane.addInputWindowTitle(inputWindow, WindowService.getInstance().getInputWindowCount());
        
        // Add an OutputWindow if none exist
        if(controlPane.getOutputWindowCount() == 0) {
            WindowService.getInstance().addOutputWindow();
        }
        
        return inputWindow;
    }
    
    /**
     * Adds a new {@link OutputWindow} to the window pane.
     * 
     * @return the newly added OutputWindow
     */
    public OutputWindow addOutputWindow() {
        OutputWindow outputWindow = new OutputWindow();
        outputWindow.relocate(initialOutputLoc.getX(), initialOutputLoc.getY());
        windowPane.getChildren().add(outputWindow);
        
        outputWindows.add(outputWindow);
        outputWindowListProperty.get().add(outputWindow.getWindowID());
        
        outputWindow.selectedProperty().addListener(getSelectedWindowListener(outputWindow));
        selectWindow(outputWindow);
        
        new WindowController(outputWindow);
        
        if(inputWindows.size() == 1 && outputWindows.size() == 1) {
            outputWindow.autoSelectInputWindow((InputWindow)inputWindows.get(0));
            Platform.runLater(() -> EventBus.get().broadcast(
                BusEvent.INPUT_VIEWTYPE_CHANGE_BROADCAST_REQUEST.subj(), new Payload())); 
        }
        
        outputWindowCreationCount.set(outputWindowCreationCount.get() + 1);
        Platform.runLater(() -> {
            outputWindow.getTitleBar().setTitleWithEvent("Output " + outputWindowCreationCount.get());
        });
        
        // Add the corresponding WindowTitle
        ControlPane controlPane = contentPane.getControlPane();
        controlPane.addOutputWindowTitle(outputWindow, WindowService.getInstance().getOutputWindowCount());
        
        return outputWindow;
    }
    
    /**
     * Allows setting of the {@link WindowConfig} for {@link OutputWindow}
     * serialization.
     * @param config
     */
    public void setOutputWindowConfig(WindowConfig config) {
        windowConfigProperty.set(config);
    }
    
    /**
     * 
     * @param w
     * @param config
     */
    public void configureWindow(Window w, WindowConfig config) {
        windowConfigProperty.set(config);
        config.currentWindow = w;
        config.windowID = w.getWindowID();
        if(!config.isLoaded()) {
            w.deSerialize(config);
        }
        
        w.configure(config);
    }
    
    /**
     * Returns a count of the currently active {@link InputWindow}s
     * @return  the current count of {@code InputWindow}s
     */
    public int getInputWindowCount() {
        return inputWindows.size();
    }
    
    /**
     * Returns a count of the currently active {@link OutputWindow}s
     * @return  the current count of {@code OutputWindow}s
     */
    public int getOutputWindowCount() {
        return outputWindows.size();
    }
    
    /**
     * Returns the number of {@link InputWindow}s created.
     * @return the number of {@code InputWindow}s created.
     */
    public IntegerProperty inputWindowCreationCountProperty() {
        return inputWindowCreationCount;
    }
    
    /**
     * Returns the number of {@link OutputWindow}s created.
     * @return the number of {@code OutputWindow}s created.
     */
    public IntegerProperty outputWindowCreationCountProperty() {
        return outputWindowCreationCount;
    }
    
    /**
     * Returns a list of {@link OutputWindow}s connected to the {@link InputWindow}
     * specified by inputWindowUUID
     * 
     * @param inputWindowUUID       the {@link UUID} of the InputWindow to find connected OutputWindows
     *                              for.
     * @return
     */
    public List<OutputWindow> getConnectedOutputWindows(UUID inputWindowUUID) {
        List<OutputWindow> result = 
            outputWindows.stream()
            .filter(w -> w.getTitleBar().getInputSelector().getWindowGroup().getPrimaryWindow().getWindowID().equals(inputWindowUUID))
            .map(w -> (OutputWindow)w)
            .collect(Collectors.toList());
        
        return result;
    }
    
    /**
     * Removes the specified window from the display 
     * getting rid of all references to it - without
     * saving.
     * 
     * @param w
     */
    public void destroyWindow(Window w) {
        if(w.isInput()) {
            resetConnectedOutputWindows((InputWindow)w);
            inputWindowListProperty.get().remove(w.getWindowID());
            inputWindows.remove(w);
        }else{
            outputWindowListProperty.get().remove(w.getWindowID());
            outputWindows.remove(w);
        }
        w.destroyProperty().set(true);
        windowPane.getChildren().remove(w);
    }
    
    /**
     * Uses the parameter "windowOrChildNode" to locate the specified window
     * or the {@link Window} containing the specified node; then updates the 
     * status message text with the specified message.
     * 
     * @param windowOrChildNode     {@link Window} containing the status bar field, or
     *                              a node that can be used to locate the window (i.e. child node)
     * @param statusText            the status message to display
     */
    public void statusMessage(Node windowOrChildNode, String statusText) {
        Window w = windowFor(windowOrChildNode);
        if(w != null) {
            w.getStatusBar().statusProperty().set(statusText);
        }
    }
    
    /**
     * Sets the status message to the default "ok" message.
     * @param windowOrChildNode     {@link Window} containing the status bar field, or
     *                              a node that can be used to locate the window (i.e. child node)
     */
    public void clearStatus(Node windowOrChildNode) {
        Window w = windowFor(windowOrChildNode);
        if(w != null) {
            w.getStatusBar().statusProperty().set("Status: OK");
        }
    }
    
    /**
     * Sends a bus message to all connected {@link OutputWindows} to clear
     * their contents.
     * @param n
     */
    public void resetConnectedOutputWindows(Node n) {
        Window w = WindowService.getInstance().windowFor(n);
        Payload request = new Payload();
        // Blanks the display of any connected {@link OutputWindow}s
        EventBus.get().broadcast(BusEvent.RESET_CONNECTED_OUTPUT_VIEW_REQUEST.subj() + w.getWindowID(), request);
    }
    
    /**
     * Returns the {@link Window} which is the parent of the specified node.
     * @param n     the node for which the parent window is returned.
     * @return      the parent {@code Window}
     */
    public Window windowFor(Node n) {
        Node parent = n;
        while(parent != null) {
            if(Window.class.isAssignableFrom(parent.getClass())) {
                break;
            }
            parent = parent.getParent();
        }
        
        return (Window)parent;
    }
    
    /**
     * Returns the {@link Window} with the specified title or
     * null if none exists with that name.
     * @param title
     * @return
     */
    public Window windowFor(String title) {
        Optional<Window> result = Stream.of(inputWindows.stream(), outputWindows.stream())
            .flatMap(w -> w)
            .filter((Window w) -> { 
                String text = w.getTitleBar().getTitleField().getText();
                System.out.println("xxx" + text + "xxx  =  yyy" + title + "yyy  ? " + text.equals(title));
                return text.equals(title);
            })
            .map(Optional::ofNullable)
            .findFirst()
            .orElse(null);
        
        return result == null ? null : result.get();
    }
    
    /**
     * Returns the {@link Window} with the specified window ID or
     * null if none exists with that ID.
     * @param title
     * @return
     */
    public Window windowFor(UUID id) {
        Optional<Window> result = Stream.of(inputWindows.stream(), outputWindows.stream())
            .flatMap(w -> w)
            .filter((Window w) -> w.getWindowID().equals(id))
            .map(Optional::ofNullable)
            .findFirst()
            .orElse(null);
        
        return result == null ? null : result.get();
    }
    
    /**
     * Returns the representative {@link WindowTitle} for the specified {@link Window}
     * or null if none matches.
     * 
     * @param w     the window for which the WindowTitle will be returned.
     * @return  the window title
     */
    public WindowTitlePane windowTitleFor(Window w) {
        Optional<WindowTitlePane> result = contentPane.getControlPane().getWindowPanesForType(w)
            .stream()
            .filter(wt -> wt.getReferredWindowID().equals(w.getWindowID()))
            .map(Optional::ofNullable)
            .findFirst()
            .orElse(null);
        
        return result == null ? null : result.get();
    }
    
    /**
     * Returns the application's {@link ControlPane}
     * @return
     */
    public ControlPane getControlPane() {
        return contentPane.getControlPane();
    }
    
    /**
     * Returns the Retina client ({@link FullClient}) which corresponds
     * to the specified {@link Window}. 
     * 
     * Note: Window may be either an Input or Output Window
     * 
     * @param w     an {@link InputWindow} or {@link OutputWindow}
     * @return  the selected Retina client.
     */
    public FullClient clientRetinaFor(Window w) {
        WindowTitlePane wt = null;
        
        if(w.getType() == Type.INPUT) {
            wt = windowTitleFor(w);
        } else {
            InputWindow iw = (InputWindow)((OutputWindow)w).getInputSelector().getWindowGroup().getPrimaryWindow();
            wt = windowTitleFor(iw);
        }
        
        return wt.getSelectedRetina();
    }
    
    /**
     * Returns a boolean indicating whether or not the color used
     * in comparison fingerprints is the same as the user's custom
     * color for that {@link InputWindow}.
     * 
     * @param w
     * @return
     */
    public boolean compareColorFollowsWindowFor(InputWindow w) {
        WindowTitlePane wt = windowTitleFor(w);
        return wt.compareColorFollowsWindowProperty().get();
    }
    
    /**
     * Called to invoke identification flashing on the specified {@link Window}
     * 
     * @param w
     */
    public void flashWindow(Window w) {
        if(w == null) return;
        
        if(currentFlashingWindow.get() != null) {
            currentFlashingWindow.get().stopFlash();
        }
        
        currentFlashingWindow.set(w);
        
        w.flash(); 
    }
    
    /**
     * Returns a property set when an {@link InputWindow} is either added or removed
     * from the display
     * @return
     */
    public ReadOnlyObjectProperty<ObservableList<UUID>> inputWindowListProperty() {
        return inputWindowListProperty.getReadOnlyProperty();
    }
    
    /**
     * Returns a property set when an {@link OutputWindow} is either added or removed
     * from the display
     * @return
     */
    public ReadOnlyObjectProperty<ObservableList<UUID>> outputWindowListProperty() {
        return outputWindowListProperty.getReadOnlyProperty();
    }
    
    /**
     * Returns a property set when an {@link InputWindow}'s dragging has stopped
     * @return  the property governing drag location
     */
    public ReadOnlyObjectProperty<Window> inputWindowDragStopProperty() {
        return inputWindowDragStopProperty.getReadOnlyProperty();
    }
    
    /**
     * Returns a property set when an {@link inputWindow}'s title has changed.
     * (important for window persistence and input window listings)
     * @return  the property governing title changes
     */
    public ReadOnlyObjectProperty<String> inputWindowTitleChangeProperty() {
        return inputWindowTitleChangeProperty.getReadOnlyProperty();
    }
    
    /**
     * Returns a property set when an {@link inputWindow}'s color id has changed.
     * @return  the property governing color id changes
     */
    public ReadOnlyObjectProperty<Paint> inputWindowColorIDChangeProperty() {
        return inputWindowColorIDProperty.getReadOnlyProperty();
    }
    
    /**
     * Convenience factory method to create an info button to be used to reveal
     * the info panel using a flip animation.
     * 
     * InfoButtons are created and enabled via the following process:
     * <UL>
     *  <li>Within a particular view's constructor, create the info button:
     *  <pre>
     *   Platform.runLater(() -> {
     *       Window w = WindowService.getInstance().windowFor(this);
     *       ObjectProperty<Point2D> infoLoc = new SimpleObjectProperty();
     *       w.layoutBoundsProperty().addListener((v,o,n) -> {
     *           infoLoc.set(new Point2D(n.getWidth() - 25, 5));
     *       });
     *       getChildren().add(infoButton = WindowService.createInfoButton(w, this, infoLoc)); 
     *   });
     *   </pre>
     *  </li>
     *  <li>Let the view implement {@link BackPanelSupplier}:
     *  <pre>
     *  public Region getOrCreateBackPanel() {
     *      if(backPanel == null) {
     *          VBox pane = new VBox();
     *          ImageView pennView = new ImageView(new Image(this.getClass().getResourceAsStream("penn.png"), 546, 549, true, true));
     *          pane.getChildren().add(pennView);
     *          pane.setPrefSize(546, 549);
     *          ScrollPane scroll = new ScrollPane(pane);
     *          backPanel = scroll;
     *      }
     *   
     *      return backPanel;
     *  }
     *  </pre>
     *  </li>
     *  <li>Make sure the Window subclass uses the "backContent" pane as container for the 
     *      back contents, and "windowContent" variable as container for the front contents
     *      of the window.
     *  </li>
     * </UL>
     *  
     *  
     * 
     * @param w                     The window on which to install the back panel
     * @param supplier              The supplier for creating the back panel
     * @param infoButtonLocator     A property to specifically locate the info button (unmanaged)
     * @return      the new info button
     */
    public static Button createInfoButton(Window w, BackPanelSupplier supplier, ObjectProperty<Point2D> infoButtonLocator) {
        Button infoButton = new Button();
        infoButton.setTooltip(new Tooltip("Show usage help"));
        infoButton.getStyleClass().addAll("info-button");
        infoButton.setPrefSize(20, 20);
        infoButton.resize(20, 20);
        infoButton.setManaged(false);
        infoButton.setFocusTraversable(false);
        ImageView infoView = new ImageView(new Image(Window.class.getClassLoader().getResourceAsStream("info.png"), 20d, 20d, true, true));
        infoButton.setGraphic(infoView);
        
        infoButton.setOnAction(e -> {
            Platform.runLater(() -> {
                w.setBackPanel(supplier.getOrCreateBackPanel());
                w.flip();
            });
        });
        
        w.layoutBoundsProperty().addListener((v,o,n) -> {
            infoButton.relocate(infoButtonLocator.get().getX(), infoButtonLocator.get().getY());
        });
        
        return infoButton;
    }
        
    /**
     * Adjusts the initial location of input windows.
     */
    private void updateInputLoc() {
        initialInputLoc = new Point2D(
            defaultInputLoc.getX() + (20 * inputWindows.size()), 
                defaultInputLoc.getY() + (20 * inputWindows.size()));
    }
    
    /**
     * Adjusts the initial location of output windows.
     */
    private void updateOutputLoc() {
        initialOutputLoc = new Point2D(
            defaultOutputLoc.getX() + (20 * outputWindows.size()), 
                defaultOutputLoc.getY() - (20 * outputWindows.size()));
    }
    
    /**
     * Selects the specified Window and sets the corresponding properties.
     * @param w     the currently selected Window
     */
    private void selectWindow(Window w) {
        if(selectedWindowProperty.get() != null) {
            selectedWindowProperty.get().selectedProperty().set(false);
        }
        w.selectedProperty().set(true);
        selectedWindowProperty.set(w);
    }
    
    /**
     * Returns a new ChangeListener for selected Windows.
     * @return
     */
    private ChangeListener<Boolean> getSelectedWindowListener(Window w) {
        return (v,o,n) -> {
            if(n) {
                selectWindow(w);
            }
        };
    }
    
    
}
