package io.cortical.iris.window;

import static io.cortical.iris.message.BusEvent.WINDOW_CLOSE_REQUEST;
import static io.cortical.iris.message.BusEvent.WINDOW_HIDE_REQUEST;

import io.cortical.iris.WindowService;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.stage.Popup;


/**
 * Bottom bar area of a window which can display
 * status text and other window controls.
 * 
 * @author cogmission
 *
 */
public class StatusBar extends Pane implements Resizable {
    public static final int HEIGHT = 20;
    
    /** The width of menuButton, loadButton, saveButton */
    private static final double ALL_CHILDREN_EXCEPT_LABEL_WIDTH = 149; 
    
    private Button loadButton;
    private Button saveButton;
    private ToggleButton menuButton;
    private Pane statusPane;
    
    private Thumb thumb;
    /** Thumb upper triangle with no grooves */
    private Pane thumbApparatus;
    
    private ContextMenu popup;
    
    private Window parentWindow;
    
    private GridPane componentBox;
    
    private ObjectProperty<String> statusProperty = new SimpleObjectProperty<>();
    
    
    
    /**
     * Constructs a new {@code StatusBar}
     * @param iw    the {@link InputWindow} to which this StatusBar is attached.
     */
    public StatusBar(Window w) {
        this.parentWindow = w;
        
        popup = createPopup();
        
        setFocusTraversable(false);
        
        componentBox = new GridPane();
        componentBox.prefWidthProperty().bind(widthProperty());
        componentBox.setPrefHeight(HEIGHT);
        
        // This was added to keep this status bar's width from exceeding window width 
        w.widthProperty().addListener((v,o,n) -> {
            resize(getWidth(), n.doubleValue());
            statusPane.setMaxWidth(w.getWidth() - ALL_CHILDREN_EXCEPT_LABEL_WIDTH);
        });
        
        menuButton = getMenuButton();
        statusPane = getStatusPane();
        loadButton = getLoadButton();
        saveButton = getSaveButton();
        thumbApparatus = getThumbApparatus();
        
        GridPane.setConstraints(menuButton, 0, 0, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(statusPane, 1, 0, 1, 1, HPos.CENTER, VPos.CENTER, Priority.SOMETIMES, Priority.NEVER);
        GridPane.setConstraints(loadButton, 2, 0, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(saveButton, 3, 0, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        GridPane.setConstraints(thumbApparatus, 4, 0, 1, 1, HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        
        componentBox.getChildren().addAll(menuButton, statusPane, loadButton, saveButton, thumbApparatus);
        
        getChildren().addAll(componentBox);
        
        getStyleClass().addAll("status-bar");
    }
    
    /**
     * Displays this {@code StatusBar}'s popup menu 
     */
    public void showWindowMenu() {
        popup.show(getScene().getWindow());
        
        double sBarLoc = getBoundsInParent().getMaxY();
        double iwLoc = parentWindow.getBoundsInParent().getMinY();
        Point2D p = menuButton.localToScene(new Point2D(0, parentWindow.getHeight()));
        Point2D appWinLoc = new Point2D(getScene().getWindow().getX(), getScene().getWindow().getY());                    
        popup.setX(appWinLoc.getX() + p.getX() + 2);
        popup.setY(appWinLoc.getY() + iwLoc + sBarLoc - popup.getHeight() + (getHeight() * 2) + 90); 
    }
    
    /**
     * Hides this {@code StatusBar}'s popup menu 
     */
    public void hideWindowMenu() {
        popup.hide();
        menuButton.setSelected(false);
    }
    
    /**
     * Returns the "load button".
     * @return  the "load button"
     */
    public Button getLoadButton() {
        if(loadButton == null) {
            Button b = new Button("Load");
            b.setTooltip(new Tooltip("Load a new window configuration"));
            b.setOnAction(Window.LOAD_FUNCTION.apply(parentWindow));
            b.setFocusTraversable(false);
            b.getStyleClass().setAll("status-bar-button");
            b.setMaxSize(54, 19);
            b.setMinSize(54, 19);
            loadButton = b;
        }
        return loadButton;
    }
    
    /**
     * Returns the "save button"
     * 
     * @return  the "save button"
     */
    public Button getSaveButton() {
        if(saveButton == null) {
            Button b = new Button("Save");
            b.setTooltip(new Tooltip("Save this window's configuration"));
            b.setOnAction(Window.SAVE_FUNCTION.apply(parentWindow));
            b.setFocusTraversable(false);
            b.getStyleClass().setAll("status-bar-button-last");
            b.setMaxSize(54, 19);
            b.setMinSize(54, 19);
            saveButton = b;
        }
        return saveButton;
    }
    
    /**
     * Returns the button which is not a real "MenuButton",
     * but a {@link Button} which triggers the showing of a 
     * {@link Popup}
     * 
     * @return  the popup trigger button
     */
    public ToggleButton getMenuButton() {
        if(menuButton == null) {
            ToggleButton b = new ToggleButton("^");
            b.setTooltip(new Tooltip("Show window menu"));
            b.setFocusTraversable(false);
            b.getStyleClass().setAll("status-bar-menubutton");
            menuButton = b;
            menuButton.addEventHandler(MouseEvent.MOUSE_CLICKED, m -> {
                activateMenu(menuButton.isSelected());
            });
        }
        return menuButton;
    }
    
    /**
     * Activates/Deactivates (shows/hides) the menu.
     * @param becomeActive      true if the menu should be shown, false if not.
     */
    public void activateMenu(boolean becomeActive) {
        if(becomeActive) {
            showWindowMenu();
        }else{
            hideWindowMenu();
        }
    }
    
    public boolean menuActivated() {
        return popup.isShowing();
    }
    
    /**
     * Returns the Pane used to display a status text
     * @return
     */
    public Pane getStatusPane() {
        if(statusPane == null) {
            Pane p = new Pane();
            p.setFocusTraversable(false);
            p.getStyleClass().setAll("status-bar-statuspane");
            statusPane = p;
            
            Platform.runLater(() -> {
                Window w = WindowService.getInstance().windowFor(StatusBar.this);
                prefWidthProperty().bind(w.widthProperty());
            });
            
            final double RN_LEN = 80;
            
            HBox compoundStatusLabel = new HBox(3);
            compoundStatusLabel.layoutXProperty().set(-1d);
            
            Label retinaName = null;
            if(parentWindow.isInput()) {
                retinaName = new Label();
                retinaName.setTooltip(new Tooltip("Selected Retina Language"));
                retinaName.getStyleClass().setAll("app-window-status-retina-name");
                retinaName.setText("English (Synon.)");
                retinaName.setAlignment(Pos.BASELINE_LEFT);
                retinaName.setFocusTraversable(false);
                retinaName.prefWidthProperty().set(RN_LEN);
                retinaName.prefHeightProperty().bind(statusPane.heightProperty());
                
                final Label l = retinaName;
                retinaName.textProperty().addListener((v,o,n) -> {
                    Text t = new Text(n);
                    t.setFont(l.getFont());
                    l.prefWidthProperty().set(t.getLayoutBounds().getWidth() + 10);
                });
                
                Platform.runLater(() -> {
                   l.textProperty().bind(WindowService.getInstance().windowTitleFor(parentWindow).selectedRetinaNameProperty()); 
                });
            }
            
            Label statusText = new Label();
            statusText.getStyleClass().setAll(parentWindow.isInput() ? "app-window-status" : "app-window-status-out");
            statusText.setAlignment(Pos.BASELINE_LEFT);
            statusText.setFocusTraversable(false);
            statusText.textProperty().bind(statusProperty());
            statusText.prefWidthProperty().bind(statusPane.widthProperty().subtract(parentWindow.isInput() ? RN_LEN : 0));
            statusText.layoutXProperty().set(0d);
            statusProperty.set("Status: OK");
            
            if(parentWindow.isInput()) {
                compoundStatusLabel.getChildren().addAll(retinaName, statusText);
            } else {
                compoundStatusLabel.getChildren().add(statusText);
            }
            p.getChildren().add(compoundStatusLabel);
        }
        return statusPane;
    }
    
    /**
     * Returns the property containing status text messages.
     * @return
     */
    public ObjectProperty<String> statusProperty() {
        return statusProperty;
    }
    
    /**
     * Creates and returns a new {@link Thumb}
     * @return
     */
    public Thumb getThumb() {
        if(thumb == null) {
            Thumb corner = new Thumb();
            corner.setFocusTraversable(false);
            thumb = corner;
        }
        return thumb;
    }
    
    /**
     * Returns the height of this {@code StatusBar}
     */
    @Override
    public double computeHeight() {
        return HEIGHT;
    }
    
    /**
     * Creates and returns the Pane assembly holding the {@link Thumb}
     * and the triangle containing its fill area.
     * 
     * @return
     */
    private Pane getThumbApparatus() {
        if(thumbApparatus == null) {
            Pane p = new Pane();
            p.setFocusTraversable(false);
            p.getChildren().addAll(getThumbFill(), getThumb());
            thumbApparatus = p;
        }
        
        return thumbApparatus;
    }
    
    /**
     * Returns the shape used to display the upper triangle
     * portion of the thumb at the right corner of the display.
     * 
     * @return  the triangular thumb
     */
    private Polygon getThumbFill() {
        Polygon triangle = new Polygon();
        triangle.setFocusTraversable(false);
        triangle.getPoints().addAll(new Double[]{ 1.0, 1.0, 0.5, 19.5, 24.5, 1.0 });
        triangle.getStyleClass().add("thumb-fill");
        return triangle;
    }
    
    
    
    /**
     * Creates and returns the popup menu used to display window
     * options.
     * @return
     */
    private ContextMenu createPopup() {
        ContextMenu m = new ContextMenu();
        m.getStyleClass().add("status-bar-menu");
        m.showingProperty().addListener((v,o,n) -> { if(!n) hideWindowMenu(); });
        m.addEventHandler(MouseEvent.MOUSE_CLICKED, mouse -> {
            if(m.isShowing()) {
                hideWindowMenu();
            }
        });
        
        String type = parentWindow.isInput() ? "Input Window" : "Output Window";
        
        MenuItem item3 = new MenuItem("Hide Window");
        MenuItem item4 = new MenuItem("Close Window");
        MenuItem item9 = new MenuItem("Close All");
        MenuItem item5 = new MenuItem("Minimize Window");
        
        SeparatorMenuItem sep = new SeparatorMenuItem();
        
        MenuItem item = new MenuItem("Load " + type + " Config...");
        item.setOnAction(Window.LOAD_FUNCTION.apply(parentWindow));
        
        MenuItem item2 = new MenuItem("Save Window Config...");
        item2.setOnAction(Window.SAVE_FUNCTION.apply(parentWindow));
        
        MenuItem item10 = new MenuItem("Delete " + type + " Config...");
        item10.setOnAction(Window.DELETE_FUNCTION.apply(parentWindow));
        
        SeparatorMenuItem sep2 = new SeparatorMenuItem();
        
        CheckMenuItem item6 = new CheckMenuItem("Highlight Connected Input Window(s)");
        CheckMenuItem item7 = new CheckMenuItem("Show Coordinates in Selector");
        
        SeparatorMenuItem sep3 = new SeparatorMenuItem();
        
        MenuItem item8 = new MenuItem("Show Window Log Messages...");
                
        m.getItems().addAll(item3, item4, item9, item5, sep, item, item2, item10, sep2);
        if(!parentWindow.isInput()) {
            m.getItems().addAll(item6, item7, sep3);
        }
        m.getItems().addAll(item8);
        
        item.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.ALT_DOWN));
        
        item2.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN));
        
        item3.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.ALT_DOWN));
        item3.setOnAction(e -> sendRequest(WINDOW_HIDE_REQUEST.subj(), e));
        
        item4.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.ALT_DOWN));
        item4.setOnAction(e -> sendRequest(WINDOW_CLOSE_REQUEST.subj(), e));
        
        item9.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN));
        
        item5.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.ALT_DOWN));
        
        
        return m;
    }
    
    /**
     * Sends the action request specified to the bus.
     * @param request
     * @param e
     */
    private void sendRequest(String request, ActionEvent e) {
        Payload p = new Payload(e);
        p.setWindow(parentWindow);
        EventBus.get().broadcast(request, p);
    }
}
