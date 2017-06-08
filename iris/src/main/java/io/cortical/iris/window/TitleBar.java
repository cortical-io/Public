package io.cortical.iris.window;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cortical.iris.WindowService;
import io.cortical.iris.persistence.WindowConfig;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * Top or header part of window which allows the editing
 * and display of a given window's name.
 * 
 * @author cogmission
 *
 */
public class TitleBar extends Pane implements Resizable, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(TitleBar.class);

    public enum Type { INPUT, OUTPUT }
    
    public static final int HEIGHT = 25;
    
    private transient TextField titleField;
    
    private transient Point2D mousePressedLoc;
    private transient SimpleObjectProperty<TitleBarState> titleBarStateProperty;
    private transient ReadOnlyObjectWrapper<String> titleSetPropertyWrapper;
    
    private transient ColorIDTab idTab;
    
    private transient WindowControl windowControl;
    
    private transient InputSelector inputSelector;
    
    private transient Type type;
    
    
    ///////////////////////////////////////////
    //          Serialized Fields            //
    ///////////////////////////////////////////
    private transient String title;
    // IDTab Color
    private transient double red, green, blue;
    
    
    public TitleBar() {
        this(Type.INPUT);
    }
    
    /**
     * Creates a new {@code TitleBar}
     */
    public TitleBar(Type type) {
        this.type = type;
        
        setMinHeight(HEIGHT);
        setMaxHeight(HEIGHT);
        
        GridPane box = new GridPane();
        box.setPrefHeight(HEIGHT);
        box.setPadding(new Insets(1, 0, 0, 0));
        layoutBoundsProperty().addListener((v, o, n) -> {
            box.setPrefSize(n.getWidth(), box.getHeight());
        });
        
        HBox icons = getWindowControl();
        
        titleField = getTitleField();
        titleField.setTooltip(new Tooltip("Click to change window name"));
        GridPane.setConstraints(titleField, 0, 0, 1, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);  
        GridPane.setConstraints(icons, 1, 0, 1, 1, HPos.RIGHT, VPos.CENTER, Priority.NEVER, Priority.NEVER);    
        box.getChildren().addAll(titleField, icons);
        
        if(this.type == Type.INPUT) {
            idTab = createIDTab();
            getChildren().addAll(box, idTab);
        }else{
            inputSelector = createInputSelector();
            getChildren().addAll(box, inputSelector.getFlyout());
        }
        
        setFocusTraversable(false);
        
        getStyleClass().addAll("title-bar");
        
        titleSetPropertyWrapper = new ReadOnlyObjectWrapper<>();
        titleBarStateProperty = new SimpleObjectProperty<TitleBarState>();
        
        addEventHandlers();
    }
    
    /**
     * {@inheritDoc}
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(type);
        LOGGER.debug("wrote type: " + type);
        
        // TitleBar
        String title = titleField.getText();
        byte[] titleBytes = title.getBytes();
        out.writeInt(titleBytes.length);
        out.write(titleBytes);
        
        LOGGER.debug("wrote title: " + title);
        
        // IDTab or InputSelector
        Window w = WindowService.getInstance().windowFor(this);
        if(w.isInput()) {
            Color p = (Color)idTab.getPaint();
            out.writeDouble(p.getRed());
            out.writeDouble(p.getGreen());
            out.writeDouble(p.getBlue());
            
            LOGGER.debug("wrote IDColor: " + p.getRed() + "," + p.getGreen() + "," + p.getBlue());
        } else {
            // Write primary OutputWindow parameters
            Window primary = inputSelector.getPrimaryWindow();
            if(primary != null) {
                String serialName = storeSerialFileName(out, w, primary, true);
                LOGGER.debug("wrote primary serial name: " + serialName);
            } else {
                out.writeBoolean(false); // hasPrimary ?
            }
            // Write secondary OutputWindow parameters
            Window secondary = inputSelector.getSecondaryWindow();
            if(secondary != null) {
                String serialName = storeSerialFileName(out, w, secondary, false);
                LOGGER.debug("wrote secondary serial name: " + serialName);
            } else {
                out.writeBoolean(false); // hasSecondary ?
            }
        }
    }
    
    /**
     * {@inheritDoc}
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        type = (Type)in.readObject();
        LOGGER.debug("read type: " + type);
        
        int titleLen = in.readInt();
        byte[] titleBytes = new byte[titleLen];
        in.read(titleBytes);
        title = new String(titleBytes);
        LOGGER.debug("read title: " + title);
        
        if(type == Type.INPUT) {
            red = in.readDouble();
            green = in.readDouble();
            blue = in.readDouble();
            LOGGER.debug("read IDColor: " + red + "," + green + "," + blue);
        } else {
            WindowConfig config = WindowService.getInstance().windowConfigProperty().get();
            boolean hasPrimary = in.readBoolean();
            if(hasPrimary) {
                String primarySerialName = (String)in.readObject();
                config.primarySerialFileName = primarySerialName;
                LOGGER.debug("read primary serial name: " + primarySerialName);
            }
            
            boolean hasSecondary = in.readBoolean();
            if(hasSecondary) {
                String secSerialName = (String)in.readObject();
                config.secondarySerialFileName = secSerialName;
                LOGGER.debug("read secondary serial name: " + secSerialName);
            }
        }
    }
    
    /**
     * Used to "switch" the returned type of the deserialization process from
     * the type {@link TitleBar} to {@link WindowConfig}
     * 
     * @return
     * @throws ObjectStreamException
     */
    private Object readResolve() throws ObjectStreamException {
        LOGGER.debug("TitleBar.readResolve() called");
        WindowConfig config = WindowService.getInstance().windowConfigProperty().get();
        config.title = title;
        config.idColor = Color.color(red, green, blue);
        return config;
    }
    
    /**
     * Creates the standard filename for the connected window's comparison
     * type; then writes the serial name to the stream, after which it 
     * stores the serial name in the current global {@link WindowConfig}
     * being populated.
     * 
     * @param out               the {@link OutputStream} used for serialization
     * @param w                 this {@link Window}
     * @param connected         the connected window if this window is an {@link OutputWindow}
     * @param isPrimary         indicates whether the "connected" window is primary or secondary.
     * @return
     * @throws IOException
     */
    private String storeSerialFileName(java.io.ObjectOutputStream out, Window w, Window connected, boolean isPrimary) 
        throws IOException {
        String serialName = isPrimary ? "primary_" : "secondary_";
        serialName = serialName + w.getTitleField().getText() + "_" + 
            w.getWindowID().toString().substring(0, 4) + "_" + 
                connected.getWindowID().toString().substring(0, 4);
        out.writeBoolean(true); // hasPrimary ?
        out.writeObject(serialName);
        
        WindowConfig config = WindowService.getInstance().windowConfigProperty().get();
        if(isPrimary) {
            config.primarySerialFileName = serialName;
            config.primaryWindow = connected;
        } else {
            config.secondarySerialFileName = serialName;
            config.secondaryWindow = connected;
        }
        
        return serialName;
    }
    
    /**
     * Returns the ObjectProperty containing state of the TitleBar 
     * @return  the {@link #titleBarStateProperty}
     */
    public ObjectProperty<TitleBarState> titleBarStateProperty() {
        return titleBarStateProperty;
    }
    
    /**
     * Returns a read only property which is set when the title is changed.
     * @return  the property emitting title change events
     */
    public ReadOnlyObjectProperty<String> titleSetProperty() {
        return titleSetPropertyWrapper.getReadOnlyProperty();
    }
    
    /**
     * Creates and returns the {@link TextField} used to display
     * and edit the title
     * @return  the TextField containing the title
     */
    public TextField getTitleField() {
        if(titleField == null) {
            titleField = new TextField();
            titleField.setFocusTraversable(false);
            titleField.setPromptText("Enter input name...");
            titleField.setAlignment(Pos.CENTER);
            titleField.focusedProperty().addListener((v,o,n) -> {
                titleField.setStyle(n ? "-fx-text-fill: darkred;" : "-fx-text-fill: -fx-text-color;");
            });
            
            adjustTitleFieldWidth();
        }
        return titleField;
    }
    
    /**
     * Programmatically sets the title and propagates the associated
     * events for the rest of the application.
     * @param newTitle  the new title to set
     */
    public void setTitleWithEvent(String newTitle) {
        titleField.setText(newTitle);
        titleSetPropertyWrapper.set(newTitle);
        adjustTitleFieldWidth();
    }
    
    /**
     * Returns the {@link ColorIDTab} used to add a color identifier
     * to a window.
     * 
     * @return
     */
    public ColorIDTab getColorIDTab() {
        return idTab;
    }
    
    /**
     * Returns the {@link InputSelector}
     * @return
     */
    public InputSelector getInputSelector() {
        return inputSelector;
    }
    
    /**
     * Returns the height of this {@code TitleBar}
     */
    @Override
    public double computeHeight() {
        return HEIGHT;
    }
    
    /**
     * Returns this {@code TitleBar}'s {@link WindowControl}. It is
     * created here if it doesn't already exist.
     * 
     * @return
     */
    public WindowControl getWindowControl() {
        if(windowControl == null) {
            windowControl = new WindowControl();
        }
        return windowControl;
    }
    
    /**
     * Creates the {@link ColorIDTab} used to add a color identifier
     * to the enclosing window.
     * 
     * @return
     */
    private ColorIDTab createIDTab() {
        return idTab = new ColorIDTab();
    }
    
    /**
     * Creates and returns a new {@link InputSelector}
     * @return
     */
    private InputSelector createInputSelector() {
        inputSelector = new InputSelector();
        inputSelector.resizeRelocate(1, 1, 25, 24);
        inputSelector.setManaged(false);
        
        return inputSelector;
    }
    
    /**
     * Dynamically adjusts the size of the transparent 
     * text field to the size of the user specified
     * text plus a margin amount.
     */
    private void adjustTitleFieldWidth() {
        Text t = new Text(titleField.getText() == null || titleField.getText().isEmpty() ? 
            titleField.getPromptText() : titleField.getText());
        double w = Math.max(50, t.getLayoutBounds().getWidth() + 30);
        titleField.setMaxWidth(w);
        titleField.setPrefWidth(w);
    }
    
    /**
     * Publishes the change in the drag state through the 
     * {@link #titleBarStateProperty}
     * @param start     the location of the mouse when first pressed
     * @param e         the drag mouse event
     */
    private void dragTitleBar(Point2D start, MouseEvent e) {
        titleBarStateProperty.setValue(new TitleBarState(start, e));
    }
    
    /**
     * Add Key and Mouse event handlers
     */
    private void addEventHandlers() {
        addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            KeyCode code = e.getCode();
            if(code.equals(KeyCode.ENTER)) {
                if(titleField.getText() == null || titleField.getText().isEmpty()) {
                    titleField.setPromptText("Enter input name...");
                }
                
                titleSetPropertyWrapper.set(titleField.getText());
                
                TitleBar.this.requestFocus();
                titleField.setStyle("-fx-text-fill: -fx-text-color");
                
                adjustTitleFieldWidth();
            }
        });
        
        addEventHandler(MouseEvent.ANY, (MouseEvent e) -> {
            if(e.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
                this.mousePressedLoc = new Point2D(e.getX(), e.getY());
                TitleBar.this.requestFocus();
            }else if(e.getEventType().equals(MouseEvent.MOUSE_DRAGGED)) {
                dragTitleBar(mousePressedLoc, e);
            }else if(e.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
                if(!windowControl.getBoundsInParent().contains(mousePressedLoc)) {
                    titleBarStateProperty.setValue(new TitleBarState(mousePressedLoc, e, true));
                }
            }
        });
        
        titleField.textProperty().addListener((v,o,n) -> {
            Window w = WindowService.getInstance().windowFor(this);
            if(w != null) {
                w.getTitleField().setText(n);
            }
        });
    }

}
