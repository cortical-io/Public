package io.cortical.iris.view.input.expression;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bushe.swing.event.EventTopicSubscriber;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.javafx.robot.FXRobot;
import com.sun.javafx.robot.FXRobotFactory;

import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.fx.webstyle.SegmentedButtonBar;
import io.cortical.iris.ApplicationService;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Message;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.custom.richtext.RichTextArea;
import io.cortical.iris.ui.custom.widget.WindowTitlePane;
import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.iris.ui.custom.widget.bubble.Entry;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewArea;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.view.WindowContext;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Resizable;
import io.cortical.iris.window.Window;
import io.cortical.retina.model.Model;
import io.cortical.retina.model.Term;
import io.cortical.util.ConfigurableIndentor;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Pair;
import rx.Observable;
import rx.Observer;






/**
 * Displays the "Topic Modeler" type UI which allows entry, selection
 * and combinations of terms.
 * 
 * @author cogmission
 */
public class ExpressionDisplay extends Group implements Resizable, View, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final double SPACING = 5;
    
    public transient final ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();
    
    private transient double lineHeight;
    
    @SuppressWarnings("unused")
    private transient WindowContext context;
    
    private transient LabelledRadiusPane inputArea;
    private transient LabelledRadiusPane serverMessageArea;
    
    private transient ExpressionWordBubbleContainer container;
    
    private transient TextArea messageText;
    private transient RichTextArea fullText;
    
    private transient BubbleExpressionBuilder parser;
    
    private transient Insets padding;
    
    private transient Model model;
    
    private transient Text helper = new Text();
    
    private transient AnchorPane anchor;
    
    private transient SegmentedButtonBar inOutBar;
    
    private transient ToggleGroup jsonToggleGroup;
    
    private transient Region backPanel;
    
    private transient ConfigurableIndentor indentor;
    
    private transient DefaultPrettyPrinter printer;
    
    private transient BubbleExpressionBuilder expressionBuilder = new BubbleExpressionBuilder();
    
    /**
     * Bus subscriber which listens for "halt" messages so that it can "know"
     * when a query is complete, then it checks for an empty display and if 
     * empty, knows to send out a clearance message to any connected {@link OutputWindow}s.
     */
    @SuppressWarnings("unused")
	private transient EventTopicSubscriber<Payload> delayer;
    
    
    
    /**
     * Constructs a new {@code ExpressionDisplay}
     * @param context
     */
    public ExpressionDisplay(WindowContext context) {
        this.context = context;
        
        setManaged(false);
        
        padding = new Insets(5, 5, 5, 5);
        
        anchor = new AnchorPane();
        
        VBox vBox = new VBox(5);
        
        inputArea = new LabelledRadiusPane("Input", LabelledRadiusPane.NewBG.GREEN);
        inputArea.setLabelFont(Font.font("Questrial", FontWeight.BOLD, 12));
        inputArea.setTabTextHeightRatio(1.5);
        inputArea.getTab().getStyleClass().setAll("expr-radius-tab");
        inputArea.getStyleClass().setAll("expr-input-area");
        inputArea.setMaxHeight(Double.MAX_VALUE);
        
        container = createWordBubbleContainer();
        inputArea.getChildren().add(container);
        
        serverMessageArea = new LabelledRadiusPane("Message Preview", LabelledRadiusPane.NewBG.GREEN);
        serverMessageArea.setLabelFont(Font.font("Questrial", FontWeight.BOLD, 12));
        serverMessageArea.setTabTextHeightRatio(1.5);
        serverMessageArea.getTab().getStyleClass().setAll("expr-radius-tab");
        serverMessageArea.setMinHeight(80);
        
        context.getThumb().thumbStateProperty().addListener((v,o,n) -> {
            Platform.runLater(() -> {
                Window w = WindowService.getInstance().windowFor(ExpressionDisplay.this);
                serverMessageArea.setPrefHeight(w.getHeight() - 92 - inputArea.getBoundsInLocal().getHeight());
            });
        });
        
        messageText = createMessageInput();
        fullText = createFullMessageInput();
        
        lineHeight = computeTextHeight(messageText.getFont(), "W", 0);
        
        serverMessageArea.getChildren().add(messageText);
        
        parser = new BubbleExpressionBuilder();
        
        vBox.getChildren().addAll(inputArea, serverMessageArea);
        VBox.setVgrow(serverMessageArea, Priority.ALWAYS);
        
        AnchorPane.setTopAnchor(vBox, 5d);
        AnchorPane.setBottomAnchor(vBox, 5d);
        AnchorPane.setLeftAnchor(vBox, 5d);
        AnchorPane.setRightAnchor(vBox, 5d);
        
        anchor.getChildren().addAll(vBox);
        anchor.prefWidthProperty().bind(context.widthProperty());
        
        VBox.setVgrow(anchor, Priority.ALWAYS);
        
        getChildren().addAll(anchor);
        
        Platform.runLater(() -> {
            InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(context.getThumb());
            
            Window w = WindowService.getInstance().windowFor(this);
            ObjectProperty<Point2D> infoLoc = new SimpleObjectProperty<>();
            w.layoutBoundsProperty().addListener((v,o,n) -> {
                infoLoc.set(new Point2D(n.getWidth() - 28, 8));
            });
            getChildren().add(WindowService.createInfoButton(w, this, infoLoc));
            
            inOutBar = createSegmentedButtonBar();
            inOutBar.setManaged(false);
            inOutBar.setPrefSize(200, 20);
            inOutBar.setMaxSize(200, 20);
            inOutBar.resize(200, 20);
            inOutBar.relocate(w.getWidth() - 210, 5);
            serverMessageArea.layoutBoundsProperty().addListener((v,o,n) -> {
                inOutBar.relocate(n.getWidth() - inOutBar.getWidth() - 5, 5);
            });
            serverMessageArea.getChildren().add(inOutBar);
            
            ExpressionField ef = getExpressionField();
            ef.keyReleasedProperty().addListener((v,o,n) -> { iw.resizeWindow(); });
            getBubbleContainer().expressionProperty().addListener((v,o,n) -> {
                iw.resizeWindow();
            });
            
            getBubbleContainer()
                .menuRequestProperty().addListener(
                    iw.getController().getExpressionDisplayRadialMenuRequestListener(iw));
            
            iw.dragThumb(1, 1);
            Platform.runLater(() -> {
                iw.dragThumb(1, 1);
                ef.requestFocus();
                requestLayout();
            });
            
            parser.setWindow(w);
            
            subscribeDelayedClearer();
            
            configurePrinter(w);
        });
        
        messageProperty.addListener((v,o,n) -> {
        	if(n == null) {
        	    InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(this);
        	    EventBus.get().broadcast(
                    BusEvent.INPUT_EVENT_INPUT_CLEARED.subj() + 
                        iw.getWindowID(), 
                            Payload.DUMMY_PAYLOAD);
        	    iw.getController().nudgeLayout(iw, 1);
        	    return;
        	}
        	
            EventBus.get().broadcast(
                BusEvent.SERVER_MESSAGE_REQUEST_CREATED.subj() + 
                    WindowService.getInstance().windowFor(this).getWindowID(), 
                        n);
        });
    }
    
    /**
     * {@inheritDoc}
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        List<Bubble.Type> bubbles = container.getChildren().stream().map(e -> ((Entry<?>)e).getBubble().getType()).collect(Collectors.toList());
        out.writeObject(bubbles);
        List<String> content = container.getChildren()
            .stream()
            .map(e -> {
                Bubble.Type t = ((Entry<?>)e).getBubble().getType();
                if(t == Bubble.Type.OPERATOR || t.toString().indexOf("PAREN") != -1) {
                    return ((Entry<?>)e).getBubble().getOperator().toDisplay();
                } else if(t == Bubble.Type.FIELD){
                    return t.toString();
                } else if(t == Bubble.Type.WORD) {
                    return ((Entry<?>)e).getBubble().getText();
                } else if(t == Bubble.Type.FINGERPRINT) {
                    return ((FingerprintBubble)((Entry<?>)e).getBubble()).getPositionsString();
                }
                return "NOT_FOUND";
            })
            .collect(Collectors.toList());
        out.writeObject(content);
        
        out.writeInt(container.getBubbleInsertionIndex());
        out.writeInt(container.getFocusTraversalIndex());
        out.writeBoolean(container.getEditorInGap());
    }
    
    /**
     * {@inheritDoc}
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        List<Bubble.Type> bubbles = (List<Bubble.Type>)in.readObject();
        List<String> content = (List<String>)in.readObject();
        
        int bubbleIdx = in.readInt();
        int focusIdx = in.readInt();
        boolean editorInGap = in.readBoolean();
        
        WindowConfig config = WindowService.getInstance().windowConfigProperty().get();
        config.bubbleList = bubbles;
        config.expressionList = content;
        config.bubbleInsertionIdx = bubbleIdx;
        config.focusTraversalIdx = focusIdx;
        config.editorInGap = editorInGap;
    }
    
    /**
     * Adds {@link Entry}s recovered from deserialization of a window.
     * @param entries
     */
    private void addSerializedEntries(List<Entry<?>> entries) {
        for(int i = 0;i < entries.size();i++) {
            container.getChildren().add(i, entries.get(i));
            container.requestLayout();
        }
    }
    
    /**
     * Adds {@link Entry}s recovered during deserialization following
     * a drag-n-drop operation.
     * 
     * @param entries       one or more {@link Entry}s to insert.
     * @param insertIdx     the cursor position at which to do the insertion.
     */
    private void addDragNDropEntries(List<Entry<?>> entries, int insertIdx) {
        container.doDropEntries(entries, insertIdx);
    }
    
    /**
     * Overridden to enable specific configuration of elements within this {@code ExpressionDisplay}
     * view, following deserialization of a {@link WindowConfig} - in order to restore the
     * a particular deserialization state, either from a saved window state or drag-n-drop operation.
     * 
     * @param   config      the {@code WindowConfig} used to restore the deserialization state.
     */
    public void configure(WindowConfig config) {
        if(config.doReset()) {
            reset();        
        }
        
        Platform.runLater(() -> {
            List<Entry<?>> entries = expressionBuilder.buildEntryList(container, config.getBubbleList(), config.getExpressionList());
            if(!config.isDragNDrop()) {
                addSerializedEntries(entries);
            } else {
                addDragNDropEntries(entries, config.getBubbleInsertionIndex());
            }
            
            if(!entries.isEmpty()) {
                container.notifyExpressionChange();
            }
            
            if(!config.isDragNDrop()) {
                int bblIndex = config.getBubbleInsertionIndex();
                int focIndex = config.getFocusTraversalIndex();
                container.setBubbleInsertionIndex(bblIndex);
                container.setFocusTraversalIndex(bblIndex);
                if(bblIndex - focIndex > 0) {
                    recoverCursorIndex(config, bblIndex, focIndex);
                } else {
                    container.typeEnter();
                }
            } else {
                container.typeEnter();
            }
            
            recoverPrompt();
        });
    }
    
    /**
     * Called during deserialization to restore the saved cursor position.
     * 
     * @param config
     * @param bblIndex
     * @param focIndex
     */
    private void recoverCursorIndex(WindowConfig config, int bblIndex, int focIndex) {
        Platform.runLater(() -> {
            container.getExpressionField().requestFocus();
            // Use the robot to position the editor, invoking all listeners along the way.
            FXRobot r = FXRobotFactory.createRobot(getScene());
            if(config.editorInGap()) {
                for(int j = 0;j < bblIndex - focIndex;j++) {
                    r.keyPress(KeyCode.LEFT);
                    r.keyRelease(KeyCode.LEFT);
                }
            } else {
                r.keyPress(KeyCode.SHIFT);
                for(int j = 0;j < bblIndex - focIndex;j++) {
                    r.keyPress(KeyCode.TAB);
                    r.keyRelease(KeyCode.TAB);
                }
                r.keyRelease(KeyCode.SHIFT);
            }
            container.notifyExpressionChange();
        });
    }
    
    /**
     * Called during deserialization to restore the appropriate prompt
     * given the current cursor position.
     */
    private void recoverPrompt() {
        // Make sure the ExpressionField's prompt is correct after deserialization
        Platform.runLater(() -> { 
            container.selectProperPromptText(); 
            container.requestLayout(); 
        });
    }
    
    /**
     * Implemented by {@code View} subclasses to handle an error
     * 
     * @param	context		the error information container
     */
    @Override
    public void processRequestError(RequestErrorContext context) {
        if(container.operatorMenuShowing()) {
            container.hideOperatorMenu();
        }
        
        if(!context.isFromClipboard()) {
            deleteToPreviousWord();
        }
    }
    
    /**
     * Recursively deletes entries in the {@link ExpressionWordBubbleContainer}
     * until it reaches an entry of type {@link Bubble.Type#WORD}.
     */
    public void deleteToPreviousWord() {
        deleteLastEntry();
        
        Window w = WindowService.getInstance().windowFor(this);
        w.requestLayout();
        
        Platform.runLater(() -> {
            if(container.getLastInserted() != null && container.getLastInserted().getType() != Bubble.Type.WORD) {
                deleteLastEntry();
            }
        });
    }
    
    /**
     * Programmatically executes a delete key press to invoke the handlers
     * which manage deletions and last-entry clearances.
     */
    public void deleteLastEntry() {
        container.getExpressionField().requestFocus();
    	FXRobot r = FXRobotFactory.createRobot(getScene());
    	r.keyPress(KeyCode.DELETE);
    	r.keyRelease(KeyCode.DELETE);
    }
    
    /**
     * Returns the height the text will need to be displayed, given the specified
     * font and wrapping width. (use wrapping width of zero for no wrapping width).
     * 
     * @param font
     * @param text
     * @param wrappingWidth
     * @return
     */
    public double computeTextHeight(Font font, String text, double wrappingWidth) {
        helper.setText(text);
        helper.setFont(font);
        helper.setWrappingWidth((int)wrappingWidth);
        return helper.getLayoutBounds().getHeight();
    }
    
    /**
     * Returns the required height of this {@code ExpressionDisplay}
     */
    @Override
    public double computeHeight() {
        double h = inputArea.getHeight() + serverMessageArea.getHeight() +
            SPACING + padding.getTop() + padding.getBottom();
        
        return h;
    }
    
    public void setToMinHeight() {
        inputArea.setPrefHeight(76);
        serverMessageArea.setPrefHeight(80);
    }
    
    /**
     * Returns this {@link ExpressionDisplay}'s {@link ExpressionField}
     * @return
     */
    public ExpressionField getExpressionField() {
        return container.getExpressionField();
    }
   
    /**
     * Returns this {@code WordBubbleContainer}
     * @return
     */
    public ExpressionWordBubbleContainer getBubbleContainer() {
        return container;
    }
    
    /**
     * Added to do UI adjustments following a view tab change.
     */
    @Override
    public void notifyViewChange() {
        Window w = WindowService.getInstance().windowFor(this);
        w.getController().nudgeLayout(w, 2);
        getParent().requestLayout();
    }
    
    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        // Zero out last property state.
        getExpressionField().emptyFieldProperty().set(System.currentTimeMillis());
        
        // Zero out all server message text, clearing status bar status also
        updateMessageText(FilteredBubbleList.emptyList());
        
        // Reset the message area to the default json display
        resetMessageArea();
        
        // Reset the Bubble Display
        container.reset();
        
        messageProperty.set(null);
    }
    
    public void resetMessageArea() {
        jsonToggleGroup.selectToggle(jsonToggleGroup.getToggles().get(0));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Region getOrCreateBackPanel() {
        if(backPanel == null) {
            backPanel = new ExpressionBackPanel(10).getScroll();
        }
        
        return backPanel;
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
     * Configures the "Pretty Printer" for user-determined indentation.
     * 
     * @param w
     */
    private void configurePrinter(Window w) {
        WindowTitlePane titlePane = WindowService.getInstance().windowTitleFor(w);
        
        indentor = new ConfigurableIndentor(4);
        printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indentor);
        printer.indentArraysWith(indentor);
        
        titlePane.defaultIndentationProperty().addListener((v,o,n) -> {
            indentor.indentationProperty().set(n.intValue());
            
            if(model == null) return;
            
            String text = null;
            try {
                text = filterText(model.toJson());
            } catch (Exception e) {
                e.printStackTrace();
                messageText.clear();
            }
            setDisplayDependentMessageText(text);
        });
        
    }
    
    /**
     * Creates and returns the {@link TextArea} used to display the outgoing
     * JSON message.
     * @return
     */
    private TextArea createMessageInput() {
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setFocusTraversable(false);
        textArea.focusedProperty().addListener((v,o,n) -> {
            if(n) {
                getExpressionField().requestFocus();
            }
        });
        textArea.prefWidthProperty().bind(serverMessageArea.widthProperty().subtract(10));
        textArea.setLayoutX(5);
        textArea.setLayoutY(serverMessageArea.labelHeightProperty().get() + 5);
        textArea.setPrefHeight(50);
        serverMessageArea.setMaxHeight(400);
        serverMessageArea.heightProperty().addListener((v,o,n) -> {
            textArea.setPrefHeight(n.doubleValue() - 35);
            textArea.setMaxHeight(n.doubleValue() - 35);
        });
        
        textArea.textProperty().addListener((v,o,n) -> {
            double computedHeightForAddedText = Math.max(50, computeTextHeight(messageText.getFont(), messageText.getText(), 0) + (2 * lineHeight));
            serverMessageArea.setPrefHeight(computedHeightForAddedText + 35);
            Platform.runLater(() -> {
                messageText.setPrefHeight(Math.max(serverMessageArea.getHeight() - 35, computedHeightForAddedText));
            });
            
            if(n != null && !n.isEmpty()) {
                sendMessage();
            }else{
                messageProperty.set(null);
            }
        });
        
        return textArea;
    }
    
    /**
     * Sends the request for a server response by setting the {@link #messageProperty}
     */
    private void sendMessage() {
        Window w = WindowService.getInstance().windowFor(ExpressionDisplay.this);
        Payload request = new Payload(new Pair<UUID, Message>(w.getWindowID(), new Message(ViewType.EXPRESSION, model)));
        request.setWindow(w);
        messageProperty.set(null);
        messageProperty.set(request);
    }
    
    /**
     * Creates and returns the {@link RichTextArea} used to display the full JSON
     * Term or Expression Object's Json fields.
     * @return
     */
    private RichTextArea createFullMessageInput() {
        RichTextArea textArea = new RichTextArea(false);
        textArea.setFocusTraversable(false);
        textArea.focusedProperty().addListener((v,o,n) -> {
            if(n) {
                getExpressionField().requestFocus();
            }
        });
        textArea.prefWidthProperty().bind(serverMessageArea.widthProperty().subtract(10));
        textArea.setLayoutX(5);
        textArea.setLayoutY(serverMessageArea.labelHeightProperty().get() + 5);
        textArea.setPrefHeight(50);
        serverMessageArea.setMaxHeight(400);
        serverMessageArea.heightProperty().addListener((v,o,n) -> {
            textArea.setPrefHeight(n.doubleValue() - 35);
            textArea.setMaxHeight(n.doubleValue() - 35);
        });
        
        textArea.textProperty().addListener((v,o,n) -> {
            double computedHeightForAddedText = n == null ? 50 : Math.max(50, computeTextHeight(messageText.getFont(), n, 0) + (2 * lineHeight) + 50);
            serverMessageArea.setPrefHeight(computedHeightForAddedText + 35);
            Platform.runLater(() -> {
                fullText.setPrefHeight(Math.max(serverMessageArea.getHeight() - 35, computedHeightForAddedText));
            });
        });
        
        return textArea;
    }
    
    /**
     * Creates and returns a new {@link ExpressionWordBubbleContainer}
     * @return
     */
    private ExpressionWordBubbleContainer createWordBubbleContainer() {
        ExpressionWordBubbleContainer wbc = new ExpressionWordBubbleContainer();
        wbc.prefWidthProperty().bind(inputArea.widthProperty().subtract(10));
        wbc.prefWrapLengthProperty().bind(inputArea.widthProperty().subtract(10));
        wbc.layoutYProperty().bind(inputArea.labelHeightProperty());
        wbc.layoutXProperty().set(5);
        
        wbc.invalidStateProperty().addListener((v,o,n) -> {
            Platform.runLater(() -> { 
                updateMessageText(FilteredBubbleList.emptyList());
                WindowService.getInstance().windowFor(this).resizeWindow();
            });
        });
        
        wbc.expressionProperty().addListener((v,o,n) -> {
            Platform.runLater(() -> { 
                if(n == null || container.hasRandomEdits()) {
                    if(n != null) {
                        WindowService.getInstance().statusMessage(this, "Incomplete or Bad Expression.");
                        WindowService.getInstance().resetConnectedOutputWindows(this);
                    }
                    return;
                }
                
                FilteredBubbleList filteredList = container.getFilteredBubbleList(n.getList());
                updateMessageText(filteredList);
                Platform.runLater(() -> WindowService.getInstance().windowFor(this).resizeWindow());
            });
        });
        
        wbc.getExpressionField().emptyFieldProperty().addListener((v,o,n) -> {
            if(n == null || container.hasRandomEdits()) return;
            
            FilteredBubbleList filteredList = container.getFilteredBubbleList(wbc.getChildren());
            updateMessageText(filteredList);
        });
        
        Platform.runLater(() -> {
            Window w = WindowService.getInstance().windowFor(this);
            wbc.defaultExpressionOperatorProperty().bind(
                WindowService.getInstance().windowTitleFor(w).defaultExpressionOperatorProperty());
        });
        
               
        return wbc;
    }
    
    /**
     * Utility method to execute setup necessary to create a {@link SegmentedButtonBar}
     * @return  the created SegmentedButtonBar
     */
    private SegmentedButtonBar createSegmentedButtonBar() {
        SegmentedButtonBar buttonBar1 = new SegmentedButtonBar();
        buttonBar1.getStyleClass().setAll("message-preview-button-bar");
        buttonBar1.setFocusTraversable(false);
        buttonBar1.setAlignment(Pos.CENTER);
        
        ToggleButton[] toggles = createJsonButtons();
        buttonBar1.getChildren().addAll(toggles);
        
        assignToggleGroup(buttonBar1);
        
        return buttonBar1;
    }
    
    /**
     * Buttons used to show Json sent and Json with all object fields.
     * @return
     */
    private ToggleButton[] createJsonButtons() {
        ToggleButton sampleButton = new ToggleButton("Sent Json");
        sampleButton.setUserData("Sent");
        sampleButton.setFocusTraversable(false);
        sampleButton.getStyleClass().addAll("first");
        ToggleButton sampleButton2 = new ToggleButton("All Fields");
        sampleButton2.setUserData("Complete");
        sampleButton2.getStyleClass().addAll("last");
        sampleButton2.setFocusTraversable(false);
        BooleanBinding bb = new BooleanBinding() {
            {
                super.bind(messageText.textProperty());
            }

            @Override
            protected boolean computeValue() {
                return messageText.getText().isEmpty();
            }
        };

        sampleButton2.disableProperty().bind(bb);
        
        return new ToggleButton[] { sampleButton, sampleButton2 };
    }
    
    /**
     * Adds each Toggle in the specified button bar to a new ToggleGroup
     * used for mutual exclusion of toggle buttons.
     * @param bar
     */
    private void assignToggleGroup(SegmentedButtonBar bar) {
        ToggleGroup toggleGroup = jsonToggleGroup = new ToggleGroup();
        bar.getChildren().stream().forEach(b -> toggleGroup.getToggles().add((ToggleButton)b));
        
        ((ToggleButton)toggleGroup.getToggles().get(0)).requestFocus();
        toggleGroup.selectedToggleProperty().addListener((v,o,n) -> {
            if(n != null) { 
                if(n.getUserData().equals("Sent")) {
                    if(serverMessageArea.getChildren().contains(fullText)) {
                        serverMessageArea.getChildren().remove(fullText);
                        serverMessageArea.getChildren().add(messageText);
                        requestLayout();
                    }
                }else{
                    if(serverMessageArea.getChildren().contains(messageText)) {
                        getFullModelString(model).subscribe(new Observer<String>() {
                            @Override public void onCompleted() { System.out.println("Completed"); }
                            @Override public void onError(Throwable e) { System.out.println("Error"); }
                            @Override public void onNext(String fullString) {
                                serverMessageArea.getChildren().remove(messageText);
                                serverMessageArea.getChildren().add(fullText);
                                if(fullString != null) {
                                    fullText.setText(fullString);
                                    String[] lines = fullString.split("\n");
                                    Arrays.stream(lines)
                                        .filter(in -> in.indexOf("df") != -1 || in.indexOf("score") != -1 || in.indexOf("pos_") != -1)
                                        .forEach(line -> {
                                            List<Integer> indices = IntStream.range(0, (int)fullString.chars().count())
                                                .filter(i -> fullString.indexOf(line, i) == i)
                                                .boxed()
                                                .collect(Collectors.toList());
                                            for(int i : indices) {
                                                fullText.setStyle(fullString.indexOf(line, i), fullString.indexOf(line, i) + line.length(), Color.rgb(237, 93, 37), Color.WHITE);
                                            }
                                            
                                        });
                                }
                                requestLayout();
                            }
                        });
                    }
                }
            }
        });
        toggleGroup.selectToggle(toggleGroup.getToggles().get(0));
    }
    
    /**
     * Sets the type of json text according to the view type selected
     * by the user.
     * 
     * @param text
     */
    private void setDisplayDependentMessageText(String text) {
        if(text != null && !text.isEmpty() && 
            (messageText.getText() == null || messageText.getText().isEmpty() || !text.trim().equals(messageText.getText().trim()))) {
            
            Window w = WindowService.getInstance().windowFor(ExpressionDisplay.this);
            Payload request = new Payload();
            EventBus.get().broadcast(BusEvent.INPUT_EVENT_NEW_EXPRESSION_STATE.subj() + w.getWindowID(), request);
        }
        
        messageText.setText(text);
        
        container.typeEnter();
    
        if(serverMessageArea.getChildren().contains(fullText)) {
            getFullModelString(model).subscribe(new Observer<String>() {
                @Override public void onCompleted() { System.out.println("Completed"); }
                @Override public void onError(Throwable e) { System.out.println("Error"); }
                @Override public void onNext(String fullString) {
                    if(fullString != null) {
                        fullText.setText(fullString);
                        Arrays.stream(fullString.split("\n"))
                            .filter(in -> in.indexOf("df") != -1 || in.indexOf("score") != -1 || in.indexOf("pos_") != -1)
                            .forEach(line -> {
                                List<Integer> indices = IntStream.range(0, (int)fullString.chars().count())
                                    .filter(i -> fullString.indexOf(line, i) == i)
                                    .boxed()
                                    .collect(Collectors.toList());
                                for(int i : indices) {
                                    fullText.setStyle(fullString.indexOf(line, i), fullString.indexOf(line, i) + line.length(), Color.rgb(237, 93, 37), Color.WHITE);
                                }
                                
                            });
                    }
                }
            });
        }
        
        
    }
    
    /**
     * Returns the json string showing full model field details.
     * @return
     */
    private Observable<String> getFullModelString(Model model) {
        Observable<String> returnVal = Observable.create(sub -> {
            String retVal = null;
            if(model != null) {
                try {
                    String s = retVal = model.toJson();
                    String[] lines = s.split("\n");
                    String currTerm = null;
                    Term currTermObj = null;
                    StringBuilder sb = new StringBuilder();
                    String lastL = null;
                    for(int i = 0;i < lines.length;i++) {
                        lastL = i > 0 ? lines[i - 1] : null;
                        String l = lines[i];
                        if(l.indexOf("term") != -1) {
                            currTerm = l.substring(l.indexOf("\"", l.indexOf(":")) + 1, l.lastIndexOf("\""));
                            currTermObj = ApplicationService.getInstance().cachingClientProperty().get().lookupFn(WindowService.getInstance().windowFor(this), currTerm.trim()).toBlocking().first();
                        }
                        if(currTerm != null && currTermObj != null) {
                            if(l.indexOf("df") != -1) {
                                l = l.replace("0.0", new BigDecimal(currTermObj.getDf()).setScale(10, RoundingMode.HALF_UP).toString());
                                lines[i] = l;
                            }else if(l.indexOf("score") != -1) {
                                l = l.replace("0.0", "" + currTermObj.getScore());
                                lines[i] = l + ",";
                            }else if(lastL != null && lastL.indexOf("score") != -1) {
                                StringBuilder spaces = new StringBuilder();
                                lastL.chars().filter(c -> c == ' ').forEach(sp -> spaces.append(" "));
                                spaces.setLength(spaces.length() - 2);
                                sb.append(spaces.toString()).append("\"pos_types\": ").append(Arrays.toString(currTermObj.getPosTypes())).append("\n");
                            }
                        }
                        sb.append(lines[i]).append("\n");
                    }
                    
                    retVal = sb.toString();
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }else{
                sub.onError(new NullPointerException("Model was null"));
            }
            
            final String result = retVal;
            Platform.runLater(() -> {
                sub.onNext(result);
                sub.onCompleted();
            });
            
        });
        
        return returnVal;
    }
    
    /**
     * Bus subscriber which listens for "halt" messages so that it can "know"
     * when a query is complete, then it checks for an empty display and if 
     * empty, knows to send out a clearance message to any connected {@link OutputWindow}s.
     */
    private void subscribeDelayedClearer() {
        EventBus.get().subscribeTo(ViewArea.PROGRESS_PATTERN, delayer = (m, p) -> {
            if(isVisible() && m.indexOf("HaltProgress") != -1 && 
                (messageText.getText() == null || messageText.getText().trim().isEmpty())) {
                
                (new Thread(() -> {
                    try { Thread.sleep(2000); }catch(Exception e) {}
                    
                    Platform.runLater(() -> {
                        WindowService.getInstance().clearStatus(this);
                        WindowService.getInstance().resetConnectedOutputWindows(this);
                        
                        Window w = WindowService.getInstance().windowFor(this);
                        w.getController().nudgeLayout(w, 5);
                    });
                })).start();
            }
        });
    }
    
    /**
     * Transform the list of {@link Bubble}s into a parsed {@link Model} and 
     * display it.
     * 
     * @param l
     */
    private void updateMessageText(FilteredBubbleList l) {
        try {
            if(l.isEmpty()) {
                messageText.clear();
                fullText.clear();
                WindowService.getInstance().clearStatus(this);
                WindowService.getInstance().resetConnectedOutputWindows(this);
            }else{
                Model model = parser.parseExpression(l);
                setTextModel(model);
                String text = filterText(model.toJson());
                setDisplayDependentMessageText(text);
                WindowService.getInstance().statusMessage(this, "Expression parsing successful...");
            }
            getParent().requestLayout();
        }catch(Exception e) {
            WindowService.getInstance().statusMessage(this, "Incomplete or Bad Expression.");
            WindowService.getInstance().resetConnectedOutputWindows(this);
        }
    }
    
    /**
     * Remove the "df" and "score" fields from the specified json string.
     * @param json
     * @return
     * @throws Exception
     */
    private String filterText(String json) throws Exception {
        String[] sa = json.split("\n");
        StringBuilder sb = new StringBuilder();
        for(int i = 0;i < sa.length;i++) {
            String s = sa[i];
            s = s.trim();
            if(s.indexOf("df") == -1 && s.indexOf("score") == -1) {
                sb.append(s).append("\n");
            }
        }
        
        // Remove trailing "," from last line before "}"
        sa = sb.toString().split("\n");
        sb.setLength(0);
        for(int i = 0;i < sa.length;i++) {
            String s = sa[i];
            if(i < sa.length - 1 && sa[i + 1].indexOf("}") != -1 && s.endsWith(",")) {
                s = s.substring(0, s.indexOf(","));
            }
            sb.append(s).append("\n");
        }
        
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObj = mapper.readValue(sb.toString(), Object.class);
        json = mapper.writer(printer).writeValueAsString(jsonObj);
         
        return json;
    }
    
    private static String filterTextStatic(String json) throws Exception {
        String[] sa = json.split("\n");
        StringBuilder sb = new StringBuilder();
        for(int i = 0;i < sa.length;i++) {
            String s = sa[i];
            s = s.trim();
            if(s.indexOf("df") == -1 && s.indexOf("score") == -1) {
                sb.append(s).append("\n");
            }
        }
        
        // Remove trailing "," from last line before "}"
        sa = sb.toString().split("\n");
        sb.setLength(0);
        for(int i = 0;i < sa.length;i++) {
            String s = sa[i];
            if(i < sa.length - 1 && sa[i + 1].indexOf("}") != -1 && s.endsWith(",")) {
                s = s.substring(0, s.indexOf(","));
            }
            sb.append(s).append("\n");
        }
        
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObj = mapper.readValue(sb.toString(), Object.class);
        ConfigurableIndentor indentor = new ConfigurableIndentor(4);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indentor);
        printer.indentArraysWith(indentor);
        json = mapper.writer(printer).writeValueAsString(jsonObj);
        
        return json;
    }

    /////////////////////////////////////////////////////
    //                Test Code                        //
    static String exp =  "{\n" +
            "\"or\" : [ {\n"+
            "\"term\" : \"Jaguar\",\n"+
            "\"df\" : 0.0,\n"+
            "\"score\" : 0.0\n"+
          "}, {\n"+
            "\"term\" : \"car\",\n"+
            "\"df\" : 0.0,\n"+
            "\"score\" : 0.0\n"+
          "} ]\n"+
        "}";

    public static void main(String[] args) throws Exception {
        filterTextStatic(exp);
    }
    /////////////////////////////////////////////////////
    
    /**
     * Stores the most recent model generated from user text.
     * @param m
     */
    private void setTextModel(Model m) {
        this.model = m;
    }
}
