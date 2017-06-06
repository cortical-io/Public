package io.cortical.iris.view.input.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.Timer;

import com.sun.javafx.robot.FXRobot;
import com.sun.javafx.robot.FXRobotFactory;

import io.cortical.iris.WindowService;
import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import io.cortical.iris.ui.custom.radialmenu.RadialGraphic;
import io.cortical.iris.ui.custom.radialmenu.RadialMenu;
import io.cortical.iris.ui.custom.radialmenu.RadialMenuItem;
import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.iris.ui.custom.widget.bubble.Bubble.Type;
import io.cortical.iris.ui.custom.widget.bubble.Entry;
import io.cortical.iris.ui.custom.widget.bubble.WordBubble;
import io.cortical.iris.ui.custom.widget.bubble.WordBubbleContainer;
import io.cortical.iris.ui.util.DragAssistant;
import io.cortical.iris.view.MenuRequest;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Smart container that manages layout, navigation, and editing
 * of custom stylized {@link Bubble}s containing expression terms.
 * 
 * This is an editable version of the {@link WordBubbleContainer}
 * 
 * @author cogmission
 * @see WordBubbleContainer
 */
public class ExpressionWordBubbleContainer extends WordBubbleContainer {
    
    public enum Mode { EDIT, ENTRY, RANDOM_ENTRY }
    
    private static final Color NORMAL_COLOR = Color.rgb(41, 132, 191, 0.5);
    private static final Color NORMAL_TEXT = Color.WHITE;
    
    private static final Color SELECTED_BG = Color.rgb(41, 132, 191);
    private static final Color SELECTED_TEXT = Color.rgb(235,107,38);
    
    private static final Color OUTLINE_FILL = SELECTED_BG;
    
    private static final KeyCombination KEY_TAB = KeyCodeCombination.valueOf("TAB");
    private static final KeyCombination KEY_SHIFT_TAB = KeyCodeCombination.valueOf("Shift+TAB");
    private static final KeyCombination KEY_L_ARROW = KeyCodeCombination.valueOf("Left");
    private static final KeyCombination KEY_R_ARROW = KeyCodeCombination.valueOf("Right");
    private static final KeyCombination KEY_ENTER = KeyCodeCombination.valueOf("ENTER");
    private static final KeyCombination KEY_DELETE = KeyCodeCombination.valueOf("DELETE");
    private static final KeyCombination KEY_BACKSPACE = KeyCodeCombination.valueOf("BACKSPACE");
    
    private static final String INTERIM_PROMPT = "Add term ...";
    
    private static final Entry<WordBubble> COMPARE_WORD = new Entry<>(new WordBubble("DUMMY"));
    
    private ObjectProperty<Entry<?>> selectionIndicatorProperty = new SimpleObjectProperty<>();
    private ObjectProperty<MenuRequest> menuRequestProperty = new SimpleObjectProperty<>();
    private ObjectProperty<Change<? extends Node>> expressionProperty = new SimpleObjectProperty<>();
    private OccurrenceProperty invalidStateProperty = new OccurrenceProperty();
        
    private Change<? extends Node> currChange;
    
    private Mode mode = Mode.ENTRY;
    
    private ExpressionField inputField;
    
    private Entry<ExpressionField> inputEntry;
    
    private EventHandler<KeyEvent> navHandler;
    
    private EntryEdit currentEdit;
    
    private RadialMenu radialMenu;
    
    private OperatorBubble selectedOperatorBubble;
    
    private RadialMenuItem selectedMenuItem;
    
    private String currentTerm;
    
    private boolean editorInGap = true;
    
    private List<Integer> randomEditLocations;

    private boolean cycleStop;
    
    private ObjectProperty<Operator> defaultOperatorProperty = new SimpleObjectProperty<>();
    
    private double result = Double.MAX_VALUE;
    private int index = -1;
    
    boolean clear = true;
    private Timer cursorTimer;
    
    
    
    
    /**
     * Constructs a new {@code WordBubbleContainer}
     */
    public ExpressionWordBubbleContainer() {
        // The editor is the only bubble added at initialization time
        addEditor();
        // Create and add the RadialMenu
        addRadialMenu();
        // Add the listener which stores bubble edits
        addStoreChangeListener();
        
        selectionIndicatorProperty().addListener(getSelectionChangeListener());
        
        getStyleClass().add("bubble-container");
        setVgap(0);
        setHgap(3);
        
        DragAssistant.configureDropHandler(this);
        
        cursorTimer = new javax.swing.Timer(200, e -> {
            clear = true;
        });
        cursorTimer.setRepeats(false);
        cursorTimer.start();
        
        // Do layout pulse after adding all the constructs.
        Platform.runLater(() -> {
            requestLayout();
        });
    }
    
    /**
     * Returns this container's default operator property.
     * 
     * @return
     */
    public ObjectProperty<Operator> defaultExpressionOperatorProperty() {
        return defaultOperatorProperty;
    }
    
    /**
     * Returns the flag indicating whether the editor is currently located
     * in the middle of bubbles.
     * @return
     */
    public boolean getEditorInGap() {
        return editorInGap;
    }
    
    /**
     * Returns this {@code WordBubbleContainer}'s {@link ExpressionField}
     * @return
     */
    public ExpressionField getExpressionField() {
        return inputField;
    }
    
    /**
     * Returns this {@code WordBubbleContainer}'s ExpressionField {@link Entry}
     * @return
     */
    public Entry<ExpressionField> getExpressionEntry() {
        return inputEntry;
    }
    
    /**
     * Returns the index of the {@link ExpressionField} (i.e. editor)
     * @return
     */
    public int getEditorIndex() {
         for(int i = 0;i < getChildren().size();i++) {
            Entry<?> e = (Entry<?>)getChildren().get(i);
            if(e.getBubble().getType() == Type.FIELD) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Sets the {@link Mode}s between {@link Mode#EDIT}
     * and {@link Mode#ENTRY}, meaning (respectively) changing 
     * a bubble that already exists, or adding a new bubble.
     * 
     * @param mode  the {@code Mode} to set.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
    /**
     * Returns the {@link Mode}s between {@link Mode#EDIT}
     * and {@link Mode#ENTRY}, meaning (respectively) changing 
     * a bubble that already exists, or adding a new bubble.
     * 
     * @return  the current {@code Mode}.
     */
    public Mode getMode() {
        return mode;
    }
    
    /**
     * Returns the property which is set upon submission of a menu request.
     * @return
     */
    public ObjectProperty<MenuRequest> menuRequestProperty() {
        return menuRequestProperty;
    }
    
    /**
     * Returns a property which is set upon selection of an {@link Entry}
     * @return
     */
    public ObjectProperty<Entry<?>> selectionIndicatorProperty() {
        return selectionIndicatorProperty;
    }
    
    /**
     * Returns a property which is set when the current expression has been
     * modified.
     * @return
     */
    public ObjectProperty<Change<? extends Node>> expressionProperty() {
        return expressionProperty;
    }
    
    /**
     * Returns the property indicating that this container is currently in
     * an invalid entry state.
     * @return
     */
    public OccurrenceProperty invalidStateProperty() {
        return invalidStateProperty;
    }
    
    /**
     * Adds a parenthesis to the display in the form of an {@link ParenthesisBubble}
     * 
     * @param op        the {@link Operator} enum describing the type of bubble to add.
     * @return          the added {@code OperatorBubble}
     */
    public OperatorBubble addParenthesis(ParenthesisBubble pb) {
        Entry<ParenthesisBubble> e = new Entry<>(pb);
        e.setAlignment(Pos.CENTER);
        e.addEventHandler(KeyEvent.KEY_RELEASED, getNavigationKeyListener());
        e.addEventHandler(KeyEvent.KEY_RELEASED, getPunctuationListener());
        getChildren().add(focusTraversalIdx, e);
        
        pb.selectedProperty().addListener(getParenthesisSelectionListener(pb, e));
        
        incBubbleInsertionIdx();
        
        evaluateAllParens();
        
        return pb;
    }
    
    /**
     * Returns the listener or handler invoked when the specified {@link ParenthesisBubble} is selected.
     * @param pb
     * @param e
     * @return
     */
    public ChangeListener<? super Boolean> getParenthesisSelectionListener(ParenthesisBubble pb, Entry<ParenthesisBubble> e) {
        return (v,o,n) -> {
            Entry<ParenthesisBubble> match = findMatch((Entry<ParenthesisBubble>)e);
            setUnmatched(pb);
            if(n) {
                if(match != null) {
                    highlightMatch(pb, match.getBubble());
                }
            }else{
                if(match!= null) {
                    setMatched(pb);
                    setMatched(match.getBubble());
                }
            }
        };
    }
    
    /**
     * Adds an operator to the display in the form of an {@link OperatorBubble}
     * 
     * @param op        the {@link Operator} enum describing the type of bubble to add.
     * @return          the added {@code OperatorBubble}
     */
    public OperatorBubble addOperator(OperatorBubble ob) {
        Entry<OperatorBubble> e = new Entry<>(ob);
        e.setAlignment(Pos.CENTER);
        e.addEventHandler(KeyEvent.KEY_RELEASED, getNavigationKeyListener());
        e.addEventHandler(KeyEvent.KEY_RELEASED, getPunctuationListener());
        getChildren().add(bubbleInsertionIdx, e);
        
        ob.addEventHandler(MouseEvent.MOUSE_PRESSED, (MouseEvent m) -> {
            // Prevent operator edit if already in edit mode.
            if(mode == Mode.EDIT) return;
            
            displayOperatorChoice(e.getBubble());
        });
        
        incBubbleInsertionIdx();
        
        return ob;
    }
    
    /**
     * Creates an {@link OperatorBubble} adding all typical listeners and then
     * returning it.
     * @param op    the {@link Operator} type to add.
     * @return
     */
    public Entry<OperatorBubble> createOperator(Operator op) {
        OperatorBubble ob = new OperatorBubble(op);
        Entry<OperatorBubble> e = new Entry<>(ob);
        e.setAlignment(Pos.CENTER);
        e.addEventHandler(KeyEvent.KEY_RELEASED, getNavigationKeyListener());
        e.addEventHandler(KeyEvent.KEY_RELEASED, getPunctuationListener());
        
        ob.addEventHandler(MouseEvent.MOUSE_PRESSED, (MouseEvent m) -> {
            // Prevent operator edit if already in edit mode.
            if(mode == Mode.EDIT) return;
            
            displayOperatorChoice(e.getBubble());
        });
        
        return e;
    }
    
    /**
     * Adds a new {@link Entry} containing a {@link WordBubble} with
     * a String "term", to this container. 
     * @param term
     */
    public Entry<WordBubble> addTerm(String term) {
        Entry<WordBubble> e = super.addTerm(term);
        e.addEventHandler(KeyEvent.KEY_RELEASED, getNavigationKeyListener());
        e.addEventHandler(KeyEvent.KEY_RELEASED, getPunctuationListener());
        e.getBubble().addEventHandler(MouseEvent.MOUSE_CLICKED, getTermDoubleClickListener(e));
        
        incBubbleInsertionIdx();
        
        return e;
    }
    
    /**
     * Creates a new {@link Entry} containing a {@link WordBubble} with listeners added and
     * a String "term", and returns it without adding it to the container. 
     * @param term
     */
    public Entry<WordBubble> createTerm(String term) {
        Entry<WordBubble> e = super.createTerm(term);
        e.addEventHandler(KeyEvent.KEY_RELEASED, getNavigationKeyListener());
        e.addEventHandler(KeyEvent.KEY_RELEASED, getPunctuationListener());
        e.getBubble().addEventHandler(MouseEvent.MOUSE_CLICKED, getTermDoubleClickListener(e));
        
        return e;
    }
    
    /**
     * Adds the editor ({@link ExpressionField}) to this {@code WordBubbleContainer}
     */
    public void addEditor() {
        inputField = new ExpressionField();
        inputField.setFocusTraversable(false);
        inputField.setPrefHeight(30);
        inputField.setBorder(null);
        inputField.setDefaultPrompt();
        inputField.setInterimPrompt("Add term ...");
        inputField.termEntryProperty().addListener((v,o,n) -> { selectProperPromptText(); });
        inputField.termEntryProperty().addListener(getTermEntryListener());
        inputField.emptyFieldProperty().addListener((v, o, n) -> {
            // Deletes previous bubble when entry text field is empty
            if(getEditorIndex() != getChildren().size() -1) return;
            
            System.out.println("GOT EMPTY FIELD PROPERTY NOTICE: mode = " + mode);
            
            if(bubbleInsertionIdx > 0 && mode != Mode.EDIT) {
                decBubbleInsertionIdx();
                getChildren().remove(bubbleInsertionIdx);
                if(getChildren().size() == 1) {
                    System.out.println("GOT U&");
                    inputField.setDefaultPrompt();
                    inputField.sizeForEdit(ExpressionField.DEFAULT_PROMPT);
                }else{
                    System.out.println("GOT X");
                    inputField.setInterimPrompt("Add term...");
                    inputField.useInterimPrompt();
                    Platform.runLater(() -> inputField.sizeForEdit(inputField.getPromptText()));
                }
                
                evaluateAllParens();
            } 
        });
        inputField.becomingEmptyProperty().addListener((v,o,n) -> {
            if(getChildren().size() == 1) {
                System.out.println("BECOMING GOT U&");
                inputField.setDefaultPrompt();
                inputField.sizeForEdit(ExpressionField.DEFAULT_PROMPT);
            }else{
                System.out.println("BECOMING GOT X");
                inputField.setInterimPrompt("Add term...");
                inputField.useInterimPrompt();
                Platform.runLater(() -> inputField.sizeForEdit(inputField.getPromptText()));
            }
        });
        inputField.becomingEmptyProperty().addListener((v,o,n) -> {
            if(getChildren().size() == 1) {
                inputField.setDefaultPrompt();
                inputField.sizeForEdit(ExpressionField.DEFAULT_PROMPT);
            }else{
                inputField.useInterimPrompt();
                inputField.sizeForEdit(inputField.getPromptText());
            }
        });
        inputField.addEventHandler(KeyEvent.KEY_RELEASED, getNavigationKeyListener());
        inputField.addEventHandler(KeyEvent.KEY_RELEASED, getPunctuationListener());
        
        inputField.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if(editorInMiddle() && !inputField.isNavOrControlOrMetaKey(e.getCode()) && isRepeatingType(COMPARE_WORD)) {
                e.consume();
            }
        });
        
        // The lone occupant of the WordBubbleContainer at initialization
        getChildren().add(inputEntry = new Entry<>(inputField));
    }
    
    /**
     * Adds the {@link RadialMenu} which allows editing of operators.
     */
    private void addRadialMenu() {
        Font f = new Text().getFont();
        f = Font.font(f.getFamily(), FontWeight.BOLD, f.getSize());
        radialMenu = new RadialMenu(0, 15, 80, 5, NORMAL_COLOR, SELECTED_BG, 
            OUTLINE_FILL, OUTLINE_FILL, 
                NORMAL_TEXT, SELECTED_TEXT, f, false, RadialMenu.CenterVisibility.NEVER, null);
        
        int selectFirst = 0;
        for(Operator o : Operator.values()) {
            
            RadialGraphic graphic = new RadialGraphic(o.toString(), o.toDisplay());
            RadialMenuItem item = new RadialMenuItem(60, graphic);
            selectedMenuItem = selectFirst == 0 ? item : selectedMenuItem;
            selectFirst++;
            item.selectedProperty().addListener(getMenuItemSelectionListener(item));
            item.disabledProperty().addListener(getMenuItemDisabledListener(item));
            item.mouseHoverProperty().addListener(getMouseHoverListener(item));
            item.getBackground().getStyleClass().setAll("radial-menu-item");
            graphic.setDisable(false);
            graphic.setSelected(false);
            
            final RadialMenuItem finalItem = item;
            item.addEventHandler(MouseEvent.MOUSE_RELEASED, (MouseEvent e) -> {
                System.out.println("mouse clicked");
                setMode(Mode.ENTRY);
                
                hideOperatorMenu();
                
                if(selectedOperatorBubble != null) {
                    boolean isParen = finalItem.getText().toLowerCase().indexOf("paren") != -1;
                    selectedOperatorBubble.setText(isParen ? o.toDisplay() : finalItem.getText());
                    selectedOperatorBubble.setOperator(Operator.typeFor(isParen ? o.toDisplay() : o.toString()));
                    selectOperatorBubble(null);
                }
                
                getSelectedEntry().requestFocus();

                // Notify listeners that the expression has been changed (i.e. for JSON processing) or sending to server.
                notifyExpressionChange();
            });
            radialMenu.addMenuItem(finalItem);
        }
        
        radialMenu.addEventHandler(KeyEvent.KEY_PRESSED, getRadialMenuKeyListener());
        radialMenu.visibleProperty().addListener((v,o,n) -> {
            if(n) { 
                Platform.runLater(() -> { 
                    radialMenu.requestFocus(); 
                }); 
            }else{
                
            }
        });
        radialMenu.setVisible(false);
        radialMenu.setManaged(false);
    }
    
    /**
     * Sets the required property which invokes the operator menu
     * handler which hides the operator menu.
     */
    public void hideOperatorMenu() {
        menuRequestProperty.set(
            makeMenuRequest(
                MenuRequest.Type.HIDE, null, radialMenu, -1, -1, true, null));
    }
    
    /**
     * Returns a flag indicating whether the operator menu is
     * currently visible or not.
     * @return  true if the menu is currently being shown, false if not.
     */
    public boolean operatorMenuShowing() {
        return radialMenu.visibleProperty().get();
    }
    
    /**
     * Notify listeners that the expression has been changed (i.e. for JSON processing) or sending to server.
     */
    public void notifyExpressionChange() {
        expressionProperty.set(currChange);
        expressionProperty.set(null);
    }
    
    /**
     * Does a search over the entire expression, setting those {@link ParenthesisBubble}s 
     * containing a match to appear as such - likewise those with no "match" will have 
     * their appearance set to the "unmatched" state.
     */
    @SuppressWarnings("unchecked")
    public void evaluateAllParens() {
        List<Node> children = getChildren();
        for(Node n : children) {
            Entry<?> e = (Entry<?>)n;
            if(e.getBubble().getType().toString().indexOf("PAREN") != -1) {
                setUnmatched((ParenthesisBubble)e.getBubble());
                Entry<ParenthesisBubble> match = findMatch((Entry<ParenthesisBubble>)e);
                if(match != null) {
                    setMatched((ParenthesisBubble)e.getBubble());
                    setMatched(match.getBubble());
                }
            }
        }
    }
    
    /**
     * Changes the appearance of the specified {@link ParenthesisBubble} to the "matched" state.
     * @param pb
     */
    public void setMatched(ParenthesisBubble pb) {
        pb.getStyleClass().setAll("parenthesis");
    }
    
    /**
     * Changes the appearance of the specified {@link ParenthesisBubble} to the "unmatched" state.
     * @param pb
     */
    public void setUnmatched(ParenthesisBubble pb) {
        pb.getStyleClass().setAll("parenthesis-unmatched");
    }
    
    /**
     * Changes the appearance of the two specified {@link ParenthesisBubble}s to appear
     * highlighted.
     * 
     * @param source    one of the bubbles to highlight
     * @param target    one of the bubbles to highlight
     */
    public void highlightMatch(ParenthesisBubble source, ParenthesisBubble target) {
        source.getStyleClass().remove("parenthesis-unmatched");
        target.getStyleClass().remove("parenthesis-unmatched");
        source.getStyleClass().setAll("parenthesis-selected");
        target.getStyleClass().setAll("parenthesis-selected");
    }
    
    /**
     * Conducts the appropriate search (either left or right) depending upon the
     * type of {@link ParenthesisBubble} contained by the specified {@link Entry}
     * @param source    the Entry containing the {@link ParenthesisBubble} to find 
     *                  a match for.
     * @return          a match or null.
     */
    public Entry<ParenthesisBubble> findMatch(Entry<ParenthesisBubble> source) {
        Entry<ParenthesisBubble> found = null;
        if(source.getBubble().getType() == Bubble.Type.LPAREN) {
            found = searchParenRight(source);
        }else{
            found = searchParenLeft(source);
        }
        return found;
    }
    
    /**
     * Returns the {@link Entry} containing the matching {@link ParenthesisBubble} which 
     * corresponds to the specified {@code ParenthesisBubble} by searching to the right.
     * @param selected      the {@link ParenthesisBubble} to find a match for.
     * @return  null if a match is not found, otherwise the match
     */
    @SuppressWarnings("unchecked")
    public Entry<ParenthesisBubble> searchParenRight(Entry<ParenthesisBubble> selected) {
        List<Node> children = getChildren();
        int startIdx = children.indexOf(selected);
        int currIdx = startIdx + 1;
        Entry<?> found = null;
        int matchCounter = 1;
        while(true) {
            if(currIdx >= children.size()) break;// currIdx = 0;
            if((found = (Entry<?>)children.get(currIdx)).getBubble().getType() == Bubble.Type.LPAREN) {
                ++matchCounter;
            }
            if((found = (Entry<?>)children.get(currIdx)).getBubble().getType() == Bubble.Type.RPAREN) {
                --matchCounter;
                if(matchCounter < 1) break;
            }
            found = null;
            if(currIdx == startIdx) break;
            ++currIdx;
        }
        
        return (Entry<ParenthesisBubble>)found;
    }
    
    /**
     * Returns the {@link Entry} containing the matching {@link ParenthesisBubble} which 
     * corresponds to the specified {@code ParenthesisBubble} by searching to the left.
     * @param selected      the {@link ParenthesisBubble} to find a match for.
     * @return  null if a match is not found, otherwise the match
     */
    @SuppressWarnings("unchecked")
    public Entry<ParenthesisBubble> searchParenLeft(Entry<ParenthesisBubble> selected) {
        List<Node> children = getChildren();
        int startIdx = children.indexOf(selected);
        int currIdx = startIdx - 1;
        Entry<?> found = null;
        int matchCounter = 1;
        while(true) {
            if(currIdx < 0) break;//currIdx = children.size() - 1;
            if((found = (Entry<?>)children.get(currIdx)).getBubble().getType() == Bubble.Type.RPAREN) {
                ++matchCounter;
            }
            if((found = (Entry<?>)children.get(currIdx)).getBubble().getType() == Bubble.Type.LPAREN) {
                --matchCounter;
                if(matchCounter < 1) break;
            }
            found = null;
            if(currIdx == startIdx) break;
            --currIdx;
        }
        
        return (Entry<ParenthesisBubble>)found;
    }
    
    /**
     * Given a mouse coordinate, returns the index of the closest
     * insertion point.
     * 
     * @param x
     * @param y
     * @return
     */
    public int getInsertionIndex(double x, double y) {
        result = Double.MAX_VALUE;
        index = -1;
        
        ObservableList<Node> ol = getChildren();
        
        IntStream.range(0, ol.size())
            .forEach(i -> {
                double d = 0;
                Bounds parent = ol.get(i).getBoundsInParent();
                if((d = distance(x,y,parent.getMinX(), parent.getMinY() + ((parent.getMaxY() - parent.getMinY()) / 2))) < result - 20) {
                    result = d;
                    index = i;
                }
            });
        
        return index;
    }
    
    /**
     * Quick Pythagorean distance calculation.
     * 
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1)));
    }
    
    /**
     * Returns a listener which responds to events by adding term 
     * entries to the view and which is notified by the underlying
     * {@link ExpressionField} when a term is entered.
     * 
     * @return  a {@link ChangeListener}
     */
    private ChangeListener<String> getTermEntryListener() {
        return (v, o, n) -> {
            if(n == null) return;
            
            boolean rectifyingRandomEdit = hasRandomEdits();
            
            System.out.println("getTermEntryListener: -----> EDITOR IN GAP: " + editorInGap);
            boolean editorInMiddle = hasTerms() && findEditor() < getChildren().size() - 1;
            System.out.println("getTermEntryListener: mode = " + getMode() + ",  rectifyingRandomEdit = " + rectifyingRandomEdit + ", EDITOR IN middle: " + editorInMiddle + ",  isEditingProperty = " + inputField.isEditingProperty().get());
            if(getMode() != Mode.EDIT && !rectifyingRandomEdit && editorInMiddle) {
                return;
            }
            
            if(getMode() == Mode.ENTRY && !rectifyingRandomEdit) {
                doTermEntry(n);
            }else if(getMode() == Mode.EDIT){
                doTermEdit(n);
            }else if(rectifyingRandomEdit) {
                doTermInsertion(n);
                Platform.runLater(() -> typeEnter());
                (new Thread(() -> {
                    try { Thread.sleep(1000); }catch(Exception e) { e.printStackTrace(); }
                    Platform.runLater(() -> typeEnter());
                })).start();
            }
        };
    }
    
    /**
     * Returns a flag indicating whether or not the editor
     * is currently located in the middle of an expression.
     * @return
     */
    private boolean editorInMiddle() {
        return hasTerms() && findEditor() < getChildren().size() - 1;
    }
    
    /**
     * Displays the radial menu appropriate for activation while a {@link OperatorBubble}
     * is focused. 
     * @param ob    the currently focused {@code OperatorBubble}
     */
    void displayOperatorChoice(OperatorBubble ob) {
        System.out.println("got i ==> " + getMode());
        Platform.runLater(() -> {
            Circle c = new Circle(((Circle)radialMenu.getCenterStrokeShape()).getRadius() + 10);
            radialMenu.resetSelectedItems();
            radialMenu.resetDisabledItems();
            
            disableParenthesisRadialMenuItems();
            
            menuRequestProperty.set(
                makeMenuRequest(
                    MenuRequest.Type.SHOW, ob.getParent(), radialMenu, 10, 23, true, c));
            
            selectOperatorBubble(ob);
            selectRadialMenuItem(ob.getText());
            
            inputField.requestFocus();
            
            requestLayout();
        });
    }
    
    /**
     * Displays the radial menu appropriate for activation while the
     * editor is focused.
     * 
     * @param ob    the editor {@link ExpressionField}
     */
    private void displayOperatorChoice(ExpressionField ob) {
        Platform.runLater(() -> {
            Circle c = new Circle(((Circle)radialMenu.getCenterStrokeShape()).getRadius() + 10);
            
            boolean rectifyingRandomEdit = hasRandomEdits();
            
            if(getMode() != Mode.RANDOM_ENTRY && !rectifyingRandomEdit) {
                Bubble bubble = getPrecedingBubble();
                Bubble.Type type = bubble == null ? null : bubble.getType();
                String excludeType = (type == null || type == Bubble.Type.OPERATOR || type == Bubble.Type.LPAREN)  ? "R_PRN" :
                    (type == Bubble.Type.WORD || type == Bubble.Type.RPAREN) ? "L_PRN" : null;
                
                disableParenthesisRadialMenuItems(excludeType);
                
                if(excludeType != null && (excludeType.equals("R_PRN") || excludeType.equals("L_PRN"))) {
                    selectRadialMenuItem(excludeType.equals("R_PRN") ? "L_PRN" : "R_PRN");
                }
            } else {
                disableParenthesisRadialMenuItems();
                
                if(hasRandomEdits()) {
                    Entry<OperatorBubble> entry = createOperator(defaultOperatorProperty.get());
                    entry.getBubble().setText(defaultOperatorProperty.get().toString());
                    getChildren().add(focusTraversalIdx, entry);
                    selectedOperatorBubble = entry.getBubble();
                    incBubbleInsertionIdx();
                    setMode(Mode.ENTRY);
                    
                    requestLayout();
                    Platform.runLater(() -> {
                        menuRequestProperty.set(
                           makeMenuRequest(
                               MenuRequest.Type.SHOW, ob.getParent(), radialMenu, -17, 23, true, c));
                        
                        selectOperatorBubble(entry.getBubble());
                        selectRadialMenuItem(entry.getBubble().getText());
                       
                        focusTraversalIdx = bubbleInsertionIdx;
                        
                        getChildren().add(getChildren().remove(findEditor()));
                        inputField.setInterimPrompt(INTERIM_PROMPT);
                        selectProperPromptText();
                        
                        requestLayout();
                    });
                    
                    return;
                }
            }
            
            menuRequestProperty.set(
                makeMenuRequest(
                    MenuRequest.Type.SHOW, ob.getParent(), radialMenu, 10, 23, true, c));
            
            requestLayout();
        });
    }
    
    public void doDropEntries(List<Entry<?>> entries, int insertIdx) {
        Bubble lastInserted = getLastInserted();
        Bubble.Type type = null;
        if(lastInserted == null) {
            type = Bubble.Type.OPERATOR;
        }else{
            type = lastInserted.getType();
        }
        
        switch(type) {
            case FINGERPRINT:
            case RPAREN:
            case WORD: {
                OperatorBubble ob = new OperatorBubble(defaultOperatorProperty.get());
                addOperator(ob);
                doDropEntries(entries, insertIdx + 1);
                requestLayout();
                              
                new Thread(
                    () ->  {
                        try { Thread.sleep(400); } catch(Exception ignore){}
                        
                        Platform.runLater(() -> {
                            displayOperatorChoice(ob);
                        });
                    }
                ).start();
               
                break;
            }
            case LPAREN:
            case OPERATOR: {
                for(int i = 0, j = insertIdx ;i < entries.size();i++, j++) {
                    getChildren().add(j, entries.get(i));
                    requestLayout();
                    incBubbleInsertionIdx();
                }
                
                break;
            }
            default: {
                return;
            }
        }
    }
    
    /**
     * Creates the {@link WordBubble}, {@link OperatorBubble} or {@link ParenthesisBubble}
     * and executes the insertion procedure.
     * 
     * @param term
     */
    private void doTermEntry(String term) {
        Bubble lastInserted = getLastInserted();
        Bubble.Type type = null;
        if(lastInserted == null) {
            type = Bubble.Type.OPERATOR;
        }else{
            type = lastInserted.getType();
        }
        
        switch(type) {
            case RPAREN:
            case WORD: {
                OperatorBubble ob = new OperatorBubble(defaultOperatorProperty.get());
                addOperator(ob);
                currentTerm = term;
                doTermEntry(currentTerm);
                requestLayout();
                              
                new Thread(
                    () ->  {
                        try { Thread.sleep(100); } catch(Exception ignore){}
                        
                        Platform.runLater(() -> {
                            displayOperatorChoice(ob);
                        });
                    }
                ).start();
               
                break;
            }
            case LPAREN:
            case OPERATOR: {
                addTerm(term.trim().isEmpty() ? currentTerm : term);
                currentTerm = null;
                
                break;
            }
            default: {
                return;
            }
        }
    }
    
    /**
     * Stores the user specified term string into the current
     * {@link EntryEdit}, then calls {@link #endEditEntry(EntryEdit)}
     * with the updated {@code EntryEdit}
     * @param term  the string to insert into the bubble
     */
    private void doTermEdit(String term) {
        EntryEdit edit = getCurrentEdit();
        edit.text = term;
        endEditEntry(edit);
    }
    
    /**
     * Starts term entry for the rectification of random edits.
     * 
     * @param term
     */
    private void doTermInsertion(String term) {
        if(randomEditLocations.contains(focusTraversalIdx)) {
            Entry<WordBubble> e = createTerm(term);
            getChildren().add(focusTraversalIdx, e);
            incBubbleInsertionIdx();
            setDefaultState();
            
            beginEditEntry(e);
        }
    }
    
    /**
     * Sets the correct prompt text which depends on the condition
     * of whether the display contains bubbles or not. If the display
     * contains no bubbles, then a more detailed prompt explaining 
     * what to do, is displayed.
     */
    public void selectProperPromptText() {
        if(containsTerms()) {
            inputField.useInterimPrompt();
        }else{
            inputField.setDefaultPrompt();
            inputField.sizeForEdit(ExpressionField.DEFAULT_PROMPT);
        }
    }
    
    /**
     * Marks the specified {@link OperatorBubble} as selected.
     * @param ob
     */
    private void selectOperatorBubble(OperatorBubble ob) {
        this.selectedOperatorBubble = ob;
    }
    
    /**
     * De-selects all radial menu items, then selects the menu item
     * with the specified text.
     * @param text
     */
    private void selectRadialMenuItem(String text) {
        radialMenu.resetSelectedItems();
        RadialMenuItem item = radialMenu.getMenuItem(text);
        radialMenu.setSelectedMenuItem(item);
        selectedMenuItem = item;
    }
    
    /**
     * Called from the RadialMenu KeyListener (see {@link #getRadialMenuKeyListener()})
     * to cycle through the menu items which are still enabled, in the counter-clockwise
     * direction.
     * 
     * @param text  the text of the menu item representing the current active bubble.
     */
    private void selectNextEnabledMenuItem(String text) {
        int idx = radialMenu.indexOfItem(radialMenu.getMenuItem(text));
        List<RadialMenuItem> items = radialMenu.getItems();
        int len = radialMenu.getItems().size();
        RadialMenuItem item = items.get(idx);
        
        for(int i = idx + 1 < len ? idx + 1 : 0;(item = items.get(i)).isDisabled();i = i + 1 < len ? i + 1 : 0) ;
        
        selectedMenuItem = item;
        radialMenu.setSelectedMenuItem(item);
    }
    
    /**
     * Called from the RadialMenu KeyListener (see {@link #getRadialMenuKeyListener()})
     * to cycle through the menu items which are still enabled, in the clockwise
     * direction.
     * 
     * @param text  the text of the menu item representing the current active bubble.
     */
    private void selectPreviousEnabledMenuItem(String text) {
        int idx = radialMenu.indexOfItem(radialMenu.getMenuItem(text));
        List<RadialMenuItem> items = radialMenu.getItems();
        int len = radialMenu.getItems().size();
        RadialMenuItem item = items.get(idx);
        
        for(int i = idx - 1 > -1 ? idx - 1 : len - 1;(item = items.get(i)).isDisabled();i = i - 1 > -1 ? i - 1 : len - 1) ;
        
        selectedMenuItem = item;
        radialMenu.setSelectedMenuItem(item);
    }
    
    /**
     * Disables the menu items which have parenthesis
     */
    private void disableParenthesisRadialMenuItems() {
        radialMenu.resetDisabledItems();
        radialMenu.resetSelectedItems();
        
        RadialMenuItem item = radialMenu.getMenuItem("L_PRN");
        radialMenu.setDisabledMenuItems(item);
        item = radialMenu.getMenuItem("R_PRN");
        radialMenu.setDisabledMenuItems(item);
    }
    
    /**
     * Enables only those menu items that have parenthesis
     */
    private void disableParenthesisRadialMenuItems(String excludeText) {
        radialMenu.resetDisabledItems();
        radialMenu.resetSelectedItems();
        
        RadialMenuItem[] items = excludeText == null ? 
            new RadialMenuItem[radialMenu.getItems().size() - 2] :
                new RadialMenuItem[radialMenu.getItems().size() - 1];
        
        int idx = 0;
        
        for(RadialMenuItem item : radialMenu.getItems()) {
            if(item.getText().indexOf("_PRN") == -1 || 
                (excludeText != null && item.getText().indexOf(excludeText) != -1)) {
                items[idx++] = item;
            }
        }
        
        radialMenu.setDisabledMenuItems(items);
    }
    
    /**
     * Returns the index of the Editor ({@link ExpressionField}).
     * @return
     */
    private int findEditor() {
        int len = getChildren().size();
        for(int i = 0;i < len;i++) {
            if(((Entry<?>)getChildren().get(i)).getBubble() instanceof ExpressionField) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * "Selects" space between bubbles which means the editor is inserted
     * at the specified index between bubbles.
     * @param focusIndex    the index at which the editor is to be inserted.
     */
    private void selectGap(int focusIndex) {
        deSelectAllEntries();
        
        // Return if already at end since editor is properly located.
        Entry<?> editor = (Entry<?>)getChildren().remove(findEditor());
        ExpressionField entryField = (ExpressionField)editor.getBubble();
        
        getChildren().add(focusIndex, editor);
        
        entryField.setInterimPrompt(" ");
        entryField.useInterimPrompt();
        editor.setSelected(true);
        entryField.requestFocus();
        
        editorInGap = true;
    }
    
    /**
     * Returns a flag indicating whether the cursor's position is between two
     * bubbles of the same type.
     * @param e     the {@link Entry} containing the {@link Bubble} at the focusTraversalIdx.
     * @return
     */
    public boolean isRepeatingType(Entry<?> e) {
        return (getNextBubble() != null && getNextBubble().getType() == e.getBubble().getType()) || 
                (getPrecedingBubble() != null && getPrecedingBubble().getType() == e.getBubble().getType());
    }
    
    /**
     * Places the editor at the index of the specified Entry, placing the bubble's
     * text in the editor for editing and switching the mode to Mode.EDIT - then
     * creating an EntryEdit object which temporarily holds the former bubble until
     * editing is finished and the editor can be swapped out for the former bubble.
     * @param e
     */
    public void beginEditEntry(Entry<?> e) {
        if(focusTraversalIdx == bubbleInsertionIdx || !getChildren().get(focusTraversalIdx).equals(e)) {
            return;
        }
        
        Entry<?> editor = (Entry<?>)getChildren().remove(bubbleInsertionIdx);
        Entry<?> bubble = (Entry<?>)getChildren().remove(focusTraversalIdx);
        
        getChildren().add(focusTraversalIdx, editor);
        
        String text = ((WordBubble)bubble.getBubble()).getText();
        ExpressionField field = (ExpressionField)editor.getBubble();
        field.setText(text);
        field.sizeForEdit(text);
        field.selectAll();
        field.requestFocus();
        
        editor.setSelected(true);
        
        setMode(Mode.EDIT);
        
        currentEdit = new EntryEdit(editor, bubble, text, focusTraversalIdx);
    }
    
    /**
     * Swaps the editor for the bubble it previously replaced. Then places
     * the editor text in the bubble and moves the editor to the end.
     * @param edit
     */
    public void endEditEntry(EntryEdit edit) {
        ((WordBubble)edit.entry.getBubble()).setText(edit.text);
        getChildren().remove(focusTraversalIdx);
        getChildren().add(edit.index, edit.entry);
        getChildren().add(edit.editor);
        edit.editor.setSelected(true);
        edit.entry.setSelected(false);
        ((ExpressionField)edit.editor.getBubble()).requestFocus();
        focusTraversalIdx = bubbleInsertionIdx;
        setMode(Mode.ENTRY);
        currentEdit = null;
    }
    
    /**
     * Returns the {@link EntryEdit} containing the edited changes.
     * @return
     */
    public EntryEdit getCurrentEdit() {
        return currentEdit;
    }
    
    /**
     * Returns editor location and prompts to their default state.
     */
    public void setDefaultState() {
        Entry<?> editor = (Entry<?>)getChildren().remove(findEditor());
        getChildren().add(editor);
        bubbleInsertionIdx = getChildren().size() - 1;
        inputField.setInterimPrompt(INTERIM_PROMPT);
        selectProperPromptText();
    }
    
    /**
     * Returns this widget to its initial state, ready for use.
     */
    public void reset() {
        Entry<?> editor = (Entry<?>)getChildren().remove(findEditor());
        getChildren().clear();
        getChildren().add(editor);
        bubbleInsertionIdx = getChildren().size() - 1;
        focusTraversalIdx = bubbleInsertionIdx;
        setMode(Mode.ENTRY);
        inputField.setInterimPrompt(INTERIM_PROMPT);
        selectProperPromptText();
        
        //Reset the query map of terms and colors
        clearQueryCache();
    }
    
    /**
     * Creates and returns a new {@link MenuRequest}
     * @param source
     * @param context
     * @param menu
     * @param srcX
     * @param srcY
     * @param showOverlay
     * @param focalShape
     * @return
     */
    private MenuRequest makeMenuRequest(MenuRequest.Type type, Node source, RadialMenu menu, double srcX, double srcY, 
        boolean showOverlay, Shape focalShape) {
        
        return MenuRequest.builder()
            .type(type)
            .source(source)
            .menu(menu)
            .srcX(srcX)
            .srcY(srcY)
            .overlay(showOverlay)
            .focalShape(focalShape)
            .build();
    }
    
    /**
     * Returns the ChangeListener which alters the selection state
     * @param item
     * @return
     */
    public ChangeListener<Boolean> getMenuItemSelectionListener(RadialMenuItem item) {
        return (v, o, n) -> {
            ObservableList<String> sc = item.getBackground().getStyleClass();
            if(!item.isDisabled()) {
                sc.removeAll("radial-menu-item-selected", "radial-menu-item");
                sc.add(n ? "radial-menu-item-selected" : "radial-menu-item");
            }
        };
    }
    
    /**
     * Returns the ChangeListener which alters the disabled state
     * @param item
     * @return
     */
    public ChangeListener<Boolean> getMenuItemDisabledListener(RadialMenuItem item) {
        return (v, o, n) -> {
            ObservableList<String> sc = item.getBackground().getStyleClass();
            sc.removeAll("radial-menu-item-disabled", "radial-menu-item");
            item.getBackground().getStyleClass().add(n ? "radial-menu-item-disabled" : "radial-menu-item");
        };
    }
    
    /**
     * Returns the ChangeListener which controls hover color changes
     * @param item
     * @return
     */
    public ChangeListener<MouseEvent> getMouseHoverListener(RadialMenuItem item) {
        return (v, o, n) -> {
            ObservableList<String> sc = item.getBackground().getStyleClass();
            sc.removeAll("radial-menu-item-selected", "radial-menu-item");
            if(n.getEventType() == MouseEvent.MOUSE_ENTERED) {
                radialMenu.setSelectedMenuItem(item);
            }else{
                radialMenu.resetSelectedItems();
            }
        };
    }
    
    /**
     * Returns the ChangeListener which manages Entry selection during focus traversal
     * @return
     */
    public ChangeListener<Entry<?>> getSelectionChangeListener() {
        return (v, o, n) -> {
            // Set to null so that the same setting can be reselected (due to nuance of "change" listener)
            if(n == null) return;
            
            Bubble bubble = n.getBubble();
            switch(bubble.getType()) {
                case OPERATOR: {
                    displayOperatorChoice((OperatorBubble)bubble);
                    setMode(Mode.EDIT);
                    break;
                }
                case WORD: {
                    beginEditEntry(n);
                    break;
                }
                case FIELD: {
                    if(editorSelected()) {
                        if(currentEdit != null || (hasRandomEdits() && getNextBubble().getType() == Bubble.Type.OPERATOR)) {
                            return;
                        }
                        displayOperatorChoice((ExpressionField)bubble);
                        break;
                    }
                }
                case LPAREN:
                case RPAREN: {
                    ParenthesisBubble pb = (ParenthesisBubble)bubble;
                    @SuppressWarnings("unchecked")
                    Entry<ParenthesisBubble> match = findMatch((Entry<ParenthesisBubble>)n);
                    setUnmatched((ParenthesisBubble)bubble);
                    if(match != null) {
                        setMatched(pb);
                        setMatched(match.getBubble());
                    }
                }
                default : break;
            }
        };
    }
    
    /**
     * Added to the RadialMenu to detect arrow key presses for menu navigation
     * via keyboard.
     * 
     * @return      the {@link RadialMenu} key listener.
     */
    private EventHandler<KeyEvent> getRadialMenuKeyListener() {
        return e -> {
            if(selectedMenuItem != null) {
                KeyCode code = e.getCode();
                if(code.equals(KeyCode.UP) || code.equals(KeyCode.LEFT)) {
                    selectNextEnabledMenuItem(selectedMenuItem.getText());
                }else if(code.equals(KeyCode.DOWN) || code.equals(KeyCode.RIGHT)) {
                    selectPreviousEnabledMenuItem(selectedMenuItem.getText());
                }else if(code.equals(KeyCode.ENTER)) {
                    setMode(Mode.ENTRY);
                    
                    hideOperatorMenu();
                    
                    if(selectedOperatorBubble != null) {
                        selectedOperatorBubble.setText(selectedMenuItem.getText());
                        selectedOperatorBubble.setOperator(Operator.typeFor(selectedMenuItem.getText()));
                        selectOperatorBubble(null);
                    }else if(selectedMenuItem.getText().indexOf("_PRN") != -1) {
                        String text = selectedMenuItem.getText();
                        addParenthesis(
                            new ParenthesisBubble(
                                text.indexOf("L") != -1 ? Operator.L_PRN : Operator.R_PRN)); 
                    }
                    
                    Platform.runLater(() -> { getSelectedEntry().requestFocus(); System.out.println("entry = " + getSelectedEntry().getBubble().getType()); });
                    
                    //typeEnter();
                }else if(code.equals(KeyCode.ESCAPE)) {
                    setMode(Mode.ENTRY);
                    
                    hideOperatorMenu();
                    
                    Platform.runLater(() -> ((Entry<?>)getChildren().get(findEditor())).requestFocus());
                }
            }
        };
    }
    
    /**
     * Returns a flag indicating whether or not the editor is currently selected.
     * @return  editor selected flag
     */
    private boolean editorSelected() {
        int idx = findEditor();
        return ((Entry<?>)getChildren().get(idx)).isSelected();
    }
    
    private void selectCurrent() {
        setDefaultState();
        selectEntry(focusTraversalIdx);
        editorInGap = focusTraversalIdx == getChildren().size() - 1;
    }
    
    private void shiftTab() {
        setDefaultState();
        decFocusTraversalIdx();
        selectEntry(focusTraversalIdx);
        editorInGap = focusTraversalIdx == getChildren().size() - 1;
    }
    
    private void tab() {
        setDefaultState();
        if(!editorInGap || focusAtEnd()) {
            incFocusTraversalIdx();
        }
        selectEntry(focusTraversalIdx);
        editorInGap = focusTraversalIdx == getChildren().size() - 1;
    }
    
    public void typeEnter() {
        Platform.runLater(() -> {
            if(cycleStop) {
                cycleStop = false;
                return;
            }
            FXRobot robot = FXRobotFactory.createRobot(getScene());
            robot.keyPress(KeyCode.ENTER);
            robot.keyRelease(KeyCode.ENTER);
            cycleStop = true;
            
            (new Thread(() -> {
                try { Thread.sleep(2000); }catch(Exception e) { e.printStackTrace(); }
                Platform.runLater(() -> cycleStop = false);
            })).start();
        });
    }
    
    public void typeLeft() {
        if(editorSelected()) {
            decFocusTraversalIdx();
        }
        selectGap(focusTraversalIdx);
        if(focusAtEnd()) {
            inputField.setInterimPrompt(INTERIM_PROMPT);
            selectProperPromptText();
        }
        inputField.isEditingProperty().set(false);
        
        Platform.runLater(() -> {
            WindowService.getInstance().windowFor(this).requestLayout();
        });
    }
    
    public void typeRight() {
        incFocusTraversalIdx();
        selectGap(focusTraversalIdx);
        if(focusAtEnd()) {
            inputField.setInterimPrompt(INTERIM_PROMPT);
            selectProperPromptText();
        }
        inputField.isEditingProperty().set(false);
        
        Platform.runLater(() -> {
            WindowService.getInstance().windowFor(this).requestLayout();
        });
    }
    
    public void moveCursor(int index) {
        if(!clear) return;
        
        clear = false;
        
        focusTraversalIdx = index;
        selectGap(focusTraversalIdx);
        if(focusAtEnd()) {
            inputField.setInterimPrompt(INTERIM_PROMPT);
            selectProperPromptText();
        }
        inputField.isEditingProperty().set(false);
        
        Platform.runLater(() -> {
            WindowService.getInstance().windowFor(this).requestLayout();
            cursorTimer.restart();
        });
    }
    
    /**
     * Return the handler for controlling navigation events in this
     * {@code WordBubbleContainer}
     * 
     * @return
     */
    EventHandler<KeyEvent> getNavigationKeyListener() {
        if(navHandler == null) {
            return navHandler = e -> { 
                if(mode == Mode.EDIT) {
                    return;
                }
                
                if(KEY_ENTER.match(e)) {
                    notifyExpressionChange();
                    inputField.isEditingProperty().set(false);
                }else if(KEY_SHIFT_TAB.match(e)) {
                    shiftTab();
                    inputField.isEditingProperty().set(false);
                }else if(KEY_TAB.match(e)){
                    tab();
                    inputField.isEditingProperty().set(false);
                }else if(KEY_L_ARROW.match(e)) {
                    if(editorSelected()) {
                        decFocusTraversalIdx();
                    }
                    selectGap(focusTraversalIdx);
                    if(focusAtEnd()) {
                        inputField.setInterimPrompt(INTERIM_PROMPT);
                        selectProperPromptText();
                    }
                    inputField.isEditingProperty().set(false);
                }else if(KEY_R_ARROW.match(e)) {
                    incFocusTraversalIdx();
                    selectGap(focusTraversalIdx);
                    if(focusAtEnd()) {
                        inputField.setInterimPrompt(INTERIM_PROMPT);
                        selectProperPromptText();
                    }
                    inputField.isEditingProperty().set(false);
                }else if(KEY_BACKSPACE.match(e) || KEY_DELETE.match(e)) {
                    if(focusAtEnd() || !hasTerms() || mode == Mode.EDIT || inputField.isEditingProperty().get()) return;
                    
                    if(focusTraversalIdx == findEditor()) {
                        decFocusTraversalIdx();
                    }
                    
                    decBubbleInsertionIdx();
                    getChildren().remove(focusTraversalIdx);
                    
                    if(focusTraversalIdx == 0) {
                        selectCurrent();
                    }else{
                        shiftTab();
                    }
                    
                    boolean hasRandomEdits = checkRandomEditMode();
                    if(hasRandomEdits) {
                        invalidStateProperty.set();
                    }
                    
                    notifyExpressionChange();
                    inputField.isEditingProperty().set(false);
                } 
                
                Platform.runLater(() -> {
                    WindowService.getInstance().windowFor(this).requestLayout();
                });
            };
        }
        
        return navHandler;
    }
    
    /**
     * Creates a list of indexes > 0 if the location of a random edit
     * has been detected; in which case the index returned will 
     * be the insertion index where an {@link Entry} will need
     * to be inserted to rectify a random edit. Otherwise -1 is
     * returned indicating that edits are "normal" (which means
     * there may still be unmatched parenthesis, but that state is
     * handled elsewhere).
     * 
     * @return  flag indicating whether random edit locations exist,
     *          true if so, false if not.
     */
    public boolean checkRandomEditMode() {
        List<Integer> randomEdits = new ArrayList<>();
        
        Bubble.Type lastType = null;
        
        FilteredBubbleList children = getFilteredBubbleList(getChildrenUnmodifiable());
        
        int len = children.size();
        for(int i = 0;i < len;i++) {
            Bubble.Type type = ((Bubble)children.get(i)).getType();
            if(type == Bubble.Type.FIELD || type  == Bubble.Type.LPAREN || type == Bubble.Type.RPAREN) {
                continue;
            }
            if(type == lastType || (i == 0 && type == Bubble.Type.OPERATOR)) {
                randomEdits.add(i);
            }
            
            lastType = type;
        }
        
        randomEditLocations = randomEdits;
        
        return !randomEditLocations.isEmpty();
    }
    
    /**
     * Returns a boolean flag indicating whether there are random edits locations 
     * and whether the current action location as indicated by {@link #getFocusTraversalIndex()} 
     * is located in a random edit location.
     * @return
     */
    public boolean hasRandomEdits() {
        return checkRandomEditMode();
    }
    
    /**
     * Returns a list of {@link Bubble}s minus the input field bubble.
     * @param l
     * @return
     */
    FilteredBubbleList getFilteredBubbleList(ObservableList<? extends Node> l) {
        return l.stream()
            .map(e -> (Bubble)((Entry<?>)e).getBubble())
            .filter(e -> e.getType() != Bubble.Type.FIELD)
            .collect(new FilteredListCollector());
    }
    
    /**
     * Returns the control key radial menu trigger.
     * @return
     */
    public EventHandler<KeyEvent> getPunctuationListener() {
        return e -> {
            if(e.getCode() == KeyCode.CONTROL) {
                selectionIndicatorProperty.set((Entry<?>)getChildren().get(focusTraversalIdx));
                selectionIndicatorProperty.set(null);
            }
        };
    }
    
    /**
     * Returns the listener controlling the Term double click action.
     * @param   entry      The {@link WordBubble} {@link Entry} to attach the action to.
     * @return
     */
    public EventHandler<MouseEvent> getTermDoubleClickListener(Entry<WordBubble> entry) {
        return e -> {
            if(e.getClickCount() == 2) {
                // Prevent editing if already in edit mode and editing other WordBubble
                if(mode == Mode.EDIT) return;
                
                setDefaultState();
                selectEntry(focusTraversalIdx = getChildren().indexOf(entry));
                editorInGap = focusTraversalIdx == getChildren().size() - 1;
                selectionIndicatorProperty.set((Entry<?>)getChildren().get(focusTraversalIdx));
                System.out.println("set selection to: " + focusTraversalIdx);
                selectionIndicatorProperty.set(null);
            }
        };
    }
    
    /**
     * Adds a handler which stores the last change made to this {@code WordBubbleContainer}'s
     * content. We don't want to react when the change actually happens (an {@link Entry} gets
     * added or removed), because there are changes which occur after that which depend on menu
     * selections etc. So we want to inspect the container after all changes are made.
     */
    public void addStoreChangeListener() {
        getChildren().addListener((Change<? extends Node> ch) -> {
            if(ch.next() && (ch.wasAdded() || ch.wasRemoved())) {
                currChange = ch;
            }
        });
    }
    
    /**
     * Temporarily stores nodes and settings for an edit.
     */
    class EntryEdit {
        Entry<?> editor;
        Entry<?> entry;
        String text;
        int index;
        
        public EntryEdit(Entry<?> editor, Entry<?> entry, String text, int index) {
            this.editor = editor;
            this.entry = entry;
            this.text = text;
            this.index = index;
        }
    }
}
