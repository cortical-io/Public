package io.cortical.iris.view.input.expression;

import io.cortical.iris.WindowService;
import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;


public class ExpressionField extends TextField implements Bubble {
    public static final String DEFAULT_PROMPT = "Enter a word or expression";
    
    private static final KeyCombination KEY_DRAG_TRAP = KeyCodeCombination.valueOf("Alt+D");
    
    private ObjectProperty<Long> emptyFieldProperty = new SimpleObjectProperty<>();
    private ObjectProperty<String> termEntryProperty = new SimpleObjectProperty<>();
    private ObjectProperty<String> keyReleasedProperty = new SimpleObjectProperty<>();
    private ObjectProperty<String> keyPressedProperty = new SimpleObjectProperty<>();
    private BooleanProperty isEditingProperty = new SimpleBooleanProperty();
    private OccurrenceProperty becomingEmptyProperty = new OccurrenceProperty();
    
    private String interimPrompt = "";
    
    private KeyCode lastCode;
    
    
    /**
     * Constructs a new {@code ExpressionField}
     */
    public ExpressionField() {
        setPrefWidth(10);
        setMinWidth(Control.USE_PREF_SIZE);
        
        setFocusTraversable(false);
        
        // Set instead of add here to override caspian.css
        getStyleClass().setAll("bubbletext");
        
        promptTextProperty().addListener((v,o,n) -> { 
            if(getText().isEmpty()) {
                sizeForEdit(getPromptText());
            }
        });
        
        addKeyHandlers();
        addMouseHandler();
    }
    
    /**
     * Sets the default prompt text: "Enter a word or expression".
     */
    public void setDefaultPrompt() {
        promptTextProperty().set(DEFAULT_PROMPT);
    }
    
    /**
     * Sets the prompt string used when there is at least 1 term already
     * defined. In the case of 0 terms the prompt text used is the {@link #DEFAULT_PROMPT} 
     * @param promptText
     */
    public void setInterimPrompt(String promptText) {
        this.interimPrompt = promptText;
    }
    
    /**
     * Returns the interim prompt text configured.
     * @return
     */
    public String getInterimPrompt() {
        return this.interimPrompt;
    }
    
    /**
     * Calls {@link #setPromptText(String)} using the previously configured
     * interim prompt text.
     */
    public void useInterimPrompt() {
        super.setPromptText(interimPrompt);
    }
    
    /**
     * Returns this {@link Bubble}'s "type" field.
     * @return
     */
    @Override
    public Type getType() {
        return Bubble.Type.FIELD;
    }
    
    /**
     * Returns a property that is modified when editing
     * starts or stops.
     * @return
     */
    public BooleanProperty isEditingProperty() {
        return isEditingProperty;
    }
    
    /**
     * Returns the property which specifies the input
     * field's empty state.
     * 
     * @return
     */
    public ObjectProperty<Long> emptyFieldProperty() {
        return emptyFieldProperty;
    }
    
    /**
     * Returns the property which specifies whether a 
     * term has been entered or not.
     * @return
     */
    public ObjectProperty<String> termEntryProperty() {
        return termEntryProperty;
    }
    
    /**
     * Returns the property which specifies a press of
     * a key.
     * 
     * @return
     */
    public ObjectProperty<String> keyPressedProperty() {
        return keyPressedProperty;
    }
    
    /**
     * Returns the property which specifies a release of
     * a key.
     * 
     * @return
     */
    public ObjectProperty<String> keyReleasedProperty() {
        return keyReleasedProperty;
    }
    
    /**
     * Returns the property set when the editor is becoming empty.
     * @return
     */
    public OccurrenceProperty becomingEmptyProperty() {
        return becomingEmptyProperty;
    }
    
    /**
     * Resizes the input field to suit the length of the 
     * changed text.
     * 
     * @param term
     */
    public void sizeForEdit(String term) {
        double width = 0;
        if(term.trim().isEmpty()) {
            width = new Text(interimPrompt).getLayoutBounds().getWidth();
        }else{
            width = new Text(term).getLayoutBounds().getWidth();
        }
        
        setPrefSize(Math.max(10, width), getHeight());
        resizeRelocate(0, 10, Math.max(10, width), getHeight());
    }
    
    /**
     * Returns the {@link KeyCode} of the last key typed.
     * @return
     */
    public KeyCode getLastKeyTyped() {
        return lastCode;
    }
    
    /**
     * Returns a flag indicating whether the {@link KeyCode} specified
     * is a control, navigation or meta key.
     * 
     * @param code
     * @return  true if so, false if not.
     */
    public boolean isNavOrControlOrMetaKey(KeyCode code) {
        return ! (!code.equals(KeyCode.ENTER) && 
                  !code.equals(KeyCode.SHIFT) && 
                  !code.equals(KeyCode.TAB) &&
                  !code.equals(KeyCode.UP) &&
                  !code.equals(KeyCode.DOWN) &&
                  !code.equals(KeyCode.ALT) &&
                  !code.equals(KeyCode.COMMAND) &&
                  !code.equals(KeyCode.CANCEL) &&
                  !code.equals(KeyCode.CLEAR) &
                  !code.equals(KeyCode.HOME) &&
                  !code.equals(KeyCode.ESCAPE) &&
                  !code.equals(KeyCode.PAGE_UP) &&
                  !code.equals(KeyCode.WINDOWS) &&
                  !code.equals(KeyCode.PAGE_DOWN));
    }
    
    /**
     * Adds the handlers for key events. These handlers update
     * specific properties which other controllers listen to in
     * order to modify the state of the user interface. The relevant
     * properties are:
     * <ul>
     *      <li>emptyFieldProperty - set when backspace or delete have been pressed
     *          and the field is empty afterward.</li>
     *      <li>keyReleasedProperty - set when a key is released</li>
     *      <li>termEntryProperty - set when there is a new term entered</li>
     * </ul>
     *        
     */
    private void addKeyHandlers() {
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            KeyCode code = e.getCode();
            
            if((code.equals(KeyCode.DELETE) || code.equals(KeyCode.BACK_SPACE)) && 
                getText().trim().length() == 0) {
                System.out.println("GOT EMPTY AFTER DELETE");
                emptyFieldProperty.set(System.currentTimeMillis());
                isEditingProperty.set(false);
            } else if(!isNavOrControlOrMetaKey(code)) {
                isEditingProperty.set(true);
            } 
            
            keyPressedProperty.set(getText());
            
            Platform.runLater(() -> {
                requestLayout();
            });
        });
        
        addEventHandler(KeyEvent.KEY_RELEASED, e -> {
            KeyCode code = lastCode = e.getCode();
            
            if(KEY_DRAG_TRAP.match(e)) {
                Platform.runLater(() -> {
                    if(getText() != null && getText().length() > 0) {
                        if(getText().length() == 1) {
                            clear();
                        } else {
                            setText(getText().substring(0, getText().length() - 1));
                        }
                    }                    
                });
                e.consume();
                return;
            }
            
            if(code.equals(KeyCode.DELETE) || code.equals(KeyCode.BACK_SPACE)) {
                if(getText().trim().length() == 0) {
                    becomingEmptyProperty.set();
                }
            } else if(code.equals(KeyCode.ENTER)) {

                Platform.runLater(() -> WindowService.getInstance().windowFor(this).resizeWindow());
                
                isEditingProperty.set(false);
                
                if(getText().trim().isEmpty()) {
                    keyReleasedProperty.set("empty");
                    requestLayout();
                    
                    return;
                }
                
                String text = getText().trim();
                setText("");
                sizeForEdit("");
                termEntryProperty.set(text);
                termEntryProperty.set(null);
            }else if(!getText().trim().isEmpty() && !e.isAltDown()){
                sizeForEdit(getText());
            }
            
            Platform.runLater(() -> {
                requestLayout();
            });
            keyReleasedProperty.set("edit");
        });
    }
    
    int i = 0;
    private void addMouseHandler() {
        final MenuItem paste = new MenuItem("Paste");
        
        ContextMenu contextMenu = new ContextMenu() {
            @Override public void show(Node anchor, Side side, double dx, double dy) {
                System.out.println("trapped show(n, ns, dx, dy)");
                super.show(anchor, side, dx, dy);
            }
            
            @Override public void show(Node anchor, double screenX, double screenY) {
                System.out.println("trapped show(n, dx, dy)");
                paste.setText("Paste " + (i++));
                super.show(anchor, screenX, screenY);
            }
            
            @Override public void hide() {
                System.out.println("trapped hide()");
                paste.setText("Working...");
                (new Thread(() -> {
                    try { Thread.sleep(3000); }catch(Exception e) { e.printStackTrace(); }
                    Platform.runLater(() -> {paste.setText("Paste");super.hide();});
                })).start();
            }
        };
        
        contextMenu.getStyleClass().add("bubble-text-context");
        contextMenu.getItems().addAll(paste);
        paste.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("handling paste...");
            }
        });
        
        //paste.setDisable(true);
       
        setContextMenu(contextMenu);
    }
}
