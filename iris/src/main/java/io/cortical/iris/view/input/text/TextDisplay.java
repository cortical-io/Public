package io.cortical.iris.view.input.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

import org.bushe.swing.event.EventTopicSubscriber;

//import org.fxmisc.richtext.StyledTextArea;

import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.iris.RetinaClientFactory;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Message;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.message.ServerResponse;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.custom.richtext.ParStyle;
import io.cortical.iris.ui.custom.richtext.RichTextArea;
import io.cortical.iris.ui.custom.richtext.StyledTextArea;
import io.cortical.iris.ui.custom.richtext.TextStyle;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.view.WindowContext;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.Resizable;
import io.cortical.iris.window.Window;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.model.Model;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;
import javafx.util.StringConverter;

/**
 * The display node representing the "Text" tab of an {@link InputWindow}'s 
 * UI. Contains the Nodes used to create text oriented queries and functionality 
 * interacting with the text Retina endpoint.
 * 
 * @author cogmission
 */
public class TextDisplay extends Group implements Resizable, View, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public final ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();
    public final ObjectProperty<Payload> languageLookupProperty = new SimpleObjectProperty<>();
    
    private ComboBox<FullClient> inputSpecificRetinaChoices;
    
    private EventTopicSubscriber<Payload> languageDetectionSubscriber;
    
    @SuppressWarnings("unused")
    private WindowContext context;
    
    private LabelledRadiusPane inputArea;
    private LabelledRadiusPane serverMessageArea;
    
    private Label instrLabel;
    private Label warningLabel;
    private TextArea messageText;
    private LocalTextArea inputText;
    private Text helper;
    
    private Model model;
    
    private TextMessager messager;
    
    private AnchorPane anchor;
    
    private ChangeListener<? super Boolean> vizListener;    
    
    private Region backPanel;
    
    
    
    /**
     * Constructs a new {@code TextDisplay} object.
     * @param context   a {@link WindowContext} allowing access to certain
     *                  window-centric features.
     */
    public TextDisplay(WindowContext context) {
        this.context = context;
        
        setManaged(false);
        
        helper = new Text();
        
        messager = new TextMessager();
        
        anchor = new AnchorPane();
        
        VBox vBox = new VBox(5);
        
        inputArea = new LabelledRadiusPane("Input", LabelledRadiusPane.NewBG.GREEN);
        inputArea.setLabelFont(Font.font("Questrial", FontWeight.BOLD, 12));
        inputArea.setTabTextHeightRatio(1.5);
        inputArea.getTab().getStyleClass().setAll("expr-radius-tab");
        inputArea.getStyleClass().setAll("expr-input-area");
        inputArea.setMaxHeight(Double.MAX_VALUE);
        inputArea.setMinHeight(150);
        
        warningLabel = new Label(LocalTextArea.LANG_DETECT_ERROR_TEXT);
        warningLabel.setManaged(false);
        warningLabel.setVisible(false);
        warningLabel.setFont(Font.font("Questrial", FontWeight.BOLD, 11));
        warningLabel.setTextAlignment(TextAlignment.LEFT);
        warningLabel.setTextFill(Color.rgb(237, 93, 37));//rgb(49, 109, 160));
        warningLabel.setPrefSize(150, 30);
        
        instrLabel = new Label("Copy/Paste your text, then click “validate”");
        instrLabel.setFont(Font.font("Questrial", FontWeight.BOLD, 10));
        instrLabel.setManaged(false);
        inputArea.getTab().widthProperty().addListener((v,o,n) -> {
            instrLabel.resizeRelocate(n.doubleValue() + 5, 0, 200, 20);
            warningLabel.resizeRelocate(n.doubleValue() + 195, 0, 250, 30);
        });
        inputArea.getChildren().addAll(instrLabel, warningLabel);
        
        inputText = createTextInput();
        inputText.readyProperty().addListener(createReadyListener());
        inputArea.getChildren().add(inputText);
        
        serverMessageArea = new LabelledRadiusPane("Message Preview", LabelledRadiusPane.NewBG.GREEN);
        serverMessageArea.setLabelFont(Font.font("Questrial", FontWeight.BOLD, 12));
        serverMessageArea.setTabTextHeightRatio(1.5);
        serverMessageArea.getTab().getStyleClass().setAll("expr-radius-tab");
        serverMessageArea.setMinHeight(95);
        
        context.getThumb().thumbStateProperty().addListener((v,o,n) -> {
            Platform.runLater(() -> {
                Window w = WindowService.getInstance().windowFor(TextDisplay.this);
                inputArea.setPrefHeight(w.getHeight() - 92 - serverMessageArea.getBoundsInLocal().getHeight());
            });
        });
        
        messageText = createMessageInput();
        
        serverMessageArea.getChildren().add(messageText);
        
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
        
        messageProperty.addListener((v,o,n) -> {
            System.out.println("TEXT DISPLAY message property listener... " + n);
            EventBus.get().broadcast(
                BusEvent.SERVER_MESSAGE_REQUEST_CREATED.subj() + 
                    WindowService.getInstance().windowFor(this).getWindowID(), n);
        });
        
        languageLookupProperty.addListener((v,o,n) -> {
            // Setting n == null is necessary to reset the change property
            if(n == null) return;
            
            (new Thread() {
                public void run() {
                    UUID windowId = WindowService.getInstance().windowFor(TextDisplay.this).getWindowID();
                    
                    EventBus.get().subscribeTo(BusEvent.LANGUAGE_DETECTION_RESPONSE.subj() + 
                        windowId, languageDetectionSubscriber = getLanguageDetectionSubscriber());
                    
                    EventBus.get().broadcast(
                        BusEvent.LANGUAGE_DETECTION_REQUEST.subj() + windowId, n);
                }
            }).start();
        });
        
        inputText.getApplyButton().setOnAction(e -> {
            Platform.runLater(() -> {
                inputText.apply();
                
                Window w = WindowService.getInstance().windowFor(TextDisplay.this);
                UUID windowId = w.getWindowID();
              
                Payload request = new Payload(new Pair<UUID, Message>(windowId, new Message(ViewType.TEXT, model)));
                request.setWindow(w);
                messageProperty.set(request);
            });
        });
        
        Platform.runLater(() -> {
            Window w = WindowService.getInstance().windowFor(this);
            ObjectProperty<Point2D> infoLoc = new SimpleObjectProperty<>();
            w.layoutBoundsProperty().addListener((v,o,n) -> {
                infoLoc.set(new Point2D(n.getWidth() - 28, 8));
            });
            getChildren().add(WindowService.createInfoButton(w, this, infoLoc));
        });
        
    }
    
    /**
     * Overridden to enable specific configuration of elements within this {@code TextDisplay}
     * view, following deserialization of a {@link WindowConfig} - in order to restore the
     * entire deserialization state.
     * 
     * @param   config      the {@code WindowConfig} used to restore the deserialization state.
     */
    public void configure(WindowConfig config) {
        reset();
        if(!config.textInput.isEmpty() && !config.textInput.trim().equals(LocalTextArea.PROMPT_TEXT)) {
            Platform.runLater(() -> {
                inputText.setText(config.textInput);
                inputText.apply();
            });
        } else {
            if(!visibleProperty().get()) {
                visibleProperty().addListener(vizListener = (v,o,n) -> {
                    Platform.runLater(() -> {
                        inputText.setPromptState(false);
                        inputText.setPromptState(true);
                        inputText.getStyledArea().visibleProperty().removeListener(vizListener);
                    });
                });
            } else {
                Platform.runLater(() -> {
                    inputText.setPromptState(false);
                    inputText.setPromptState(true);
                });
            }
            
        }
    }
    
    /**
     * {@inheritDoc}
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        String input = inputText.textProperty().get();
        out.writeObject(input == null ? "" : input.trim().equals(LocalTextArea.PROMPT_TEXT) ? "" : input);
    }
    
    /**
     * {@inheritDoc}
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        WindowConfig config = WindowService.getInstance().windowConfigProperty().get();
        String input = (String)in.readObject();
        config.textInput = input;
    }
    
    /**
     * Reacts to language detection response by propagating the client selection for
     * the language which was detected, and then invoking the server lookup for the
     * specified text.
     * 
     * @return
     */
    private EventTopicSubscriber<Payload> getLanguageDetectionSubscriber() {
        return (s,p) -> {
            Window w = WindowService.getInstance().windowFor(TextDisplay.this);
            UUID windowId = w.getWindowID();
            
            EventBus.get().unsubscribeTo(BusEvent.LANGUAGE_DETECTION_RESPONSE.subj() + windowId, languageDetectionSubscriber);
            
            ServerResponse response = (ServerResponse)p;
            Platform.runLater(() -> {
                WindowService.getInstance().windowTitleFor(w).selectRetina(response.getDetectedClient());
                
                inputSpecificRetinaChoices.getSelectionModel().select(response.getDetectedClient());
                
                if(model == null) {
                    try {
                        Model model = messager.createMessage(inputText.textProperty().get());
                        setTextModel(model);
                        messageText.setText(model.toJson());
                    }catch(Exception e) {
                        return;
                    }
                }
            
                Payload request = new Payload(new Pair<UUID, Message>(windowId, new Message(ViewType.TEXT, model)));
                request.setWindow(w);
                messageProperty.set(request);
            });
        };
    }
    
    /**
     * Implemented by {@code View} subclasses to handle an error
     * 
     * @param	context		the error information container
     */
    @Override
    public void processRequestError(RequestErrorContext context) {}
    
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
     * Creates and returns a derivative of a {@link RichTextArea}.
     * @return  a subclass of {@link RichTextArea} with special local functionality.
     */
    public LocalTextArea createTextInput() {
        LocalTextArea area = new LocalTextArea(true);
        area.setFocusTraversable(false);
        area.prefWidthProperty().bind(inputArea.widthProperty().subtract(10));
        area.prefHeightProperty().bind(inputArea.heightProperty().subtract(40));
        area.setLayoutX(5);
        area.setLayoutY(inputArea.labelHeightProperty().get() + 10);
        return area; 
    }
    
    /**
     * Implements {@link Resizable#computeHeight()}
     */
    @Override
    public double computeHeight() {
        return inputArea.getHeight() + serverMessageArea.getHeight() + 15;
    }
    
    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        inputText.clear();
        messageText.clear();
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
            backPanel = new TextBackPanel(10).getScroll();
        }
        
        return backPanel;
    }
    
    /**
     * Stores the most recent model generated from user text.
     * @param m
     */
    private void setTextModel(Model m) {
        this.model = m;
    }
    
    /**
     * Creates the text area which displays the JSON sent to the server.
     * @return  the text area which displays the JSON sent to the server.
     */
    private TextArea createMessageInput() {
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setFocusTraversable(false);
        textArea.prefWidthProperty().bind(serverMessageArea.widthProperty().subtract(10));
        textArea.setLayoutX(5);
        textArea.setLayoutY(serverMessageArea.labelHeightProperty().get());
        textArea.setPrefHeight(65);
        serverMessageArea.heightProperty().addListener((v,o,n) -> {
            textArea.setPrefHeight(Math.max(65, n.doubleValue() - 30));
        });
        
        textArea.textProperty().addListener((v,o,n) -> {
            // Do not send query of empty string
            if(n == null || n.isEmpty()) {
                setTextModel(null);
            }
        });
        
        return textArea;
    }
    
    
    
    /**
     * Creates the listener which responds to a signal from the {@link RichTextArea}
     * indicating that text has been inserted and is ready for processing.
     * @return  the ready listener
     */
    public ChangeListener<String> createReadyListener() {
        return (v,o,n) -> {
            try {
                // Don't respond for empty text
                if(n.isEmpty()) {
                    messageText.setText(null);
                    return;
                }
                
                inputText.textProperty().set(n);
                Model model = messager.createMessage(n);
                setTextModel(model);
                messageText.setText(model.toJson());
            }catch(Exception e) {
                e.printStackTrace();
            }
        };
    }
    
    /**
     * Overridden to allow custom widget handling for pasting to send
     * query to the rest server.
     */
    class LocalTextArea extends RichTextArea {
        private static final String PROMPT_TEXT = 
            "Enter text here in any of the languages: \nArabic, Chinese, Danish, English, French, German, or Spanish,\n" +
            "and the language will be automatically detected.\nNote: 10 words, (or 40-50) characters minimum to activate auto-language-detection.";
        private static final String LANG_DETECT_ERROR_TEXT = "Inputted text is too short for automatic language\ndetection. " +
            "Please select from drop-down-menu.";
        
        private Label applyLabel;
        // Only allow language lookups if text has changed by more than 10 characters
        private IntegerProperty lengthThresholdDetectProperty = new SimpleIntegerProperty();
        
        public LocalTextArea(boolean includeControls) {
            super(includeControls);
            
            lengthProperty().addListener((v,o,n) -> {
                String text = area.getText();
                if(text != null && (text.split("\\s").length < 10 && (n > 0 && text.length() < 50))) {
                    warningLabel.setVisible(true);
                } else if(text != null && !text.isEmpty() && !text.trim().equals(PROMPT_TEXT) && 
                    Math.abs(lengthThresholdDetectProperty.get() - text.length()) > 10) {
                    
                    warningLabel.setVisible(false);
                    ServerRequest r = new ServerRequest();
                    r.setText(text);
                    lengthThresholdDetectProperty.set(text.length());
                    languageLookupProperty.set(r);
                    languageLookupProperty.set(null);
                }else if(text == null || text.isEmpty()) {
                    System.out.println("TEXT DISPLAY : TEXT DOT IS EMPTY");
                    InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(this);
                    EventBus.get().broadcast(BusEvent.INPUT_EVENT_INPUT_CLEARED.subj() + iw.getWindowID(), Payload.DUMMY_PAYLOAD);
                    messageProperty.set(null);
                }
            });
            
            setFocusTraversable(false);
        }
        
        @Override
        public StyledTextArea<ParStyle, TextStyle> createTextArea(boolean includeControls) {
            StyledTextArea<ParStyle, TextStyle> area = new StyledTextArea<ParStyle, TextStyle>(
                ParStyle.EMPTY, ( paragraph, style) -> paragraph.setStyle(style.toCss()),
                    TextStyle.EMPTY.updateFontSize(12).updateFontFamily("Questrial-Regular").updateTextColor(Color.BLACK),
                        ( text, style) -> text.setStyle(style.toCss())) {
                
                @Override
                public void paste() {
                    super.paste();
                    Platform.runLater(() -> apply());
                }
            };
            area.setWrapText(true);
            area.setStyleCodecs(ParStyle.CODEC, TextStyle.CODEC);
            area.getStyleClass().addAll(includeControls ? "rich-text-area" : "rich-text-area-no-controls");
            return area;
        }
        
        @Override
        protected HBox createPanel1() {
            double min = 26;
            area.wrapTextProperty().set(true);
            
            Button undoBtn = createButton("undo", area::undo);
            undoBtn.setMinWidth(min);
            undoBtn.setTooltip(new Tooltip("undo"));
            
            Button redoBtn = createButton("redo", area::redo);
            redoBtn.setMinWidth(min);
            redoBtn.setTooltip(new Tooltip("redo"));
            
            Button cutBtn = createButton("cut", area::cut);
            cutBtn.setMinWidth(min);
            cutBtn.setTooltip(new Tooltip("cut"));
            
            Button copyBtn = createButton("copy", area::copy);
            copyBtn.setMinWidth(min);
            copyBtn.setTooltip(new Tooltip("copy"));
            
            Button pasteBtn = createPasteButton("paste", area::paste);
            pasteBtn.setMinWidth(min);
            pasteBtn.setTooltip(new Tooltip("paste"));
            
            applyBtn = createButton("apply-unset", this::apply);
            applyBtn.setPrefHeight(25);
            applyBtn.setMaxHeight(25);
            applyBtn.setText("validate");
            applyBtn.setDisable(true);
            applyBtn.setMinWidth(90);
            applyBtn.setTooltip(new Tooltip("validate"));
            applyBtn.addEventHandler(MouseEvent.MOUSE_ENTERED, (MouseEvent m) -> {
                if(applyBtn.isDisabled()) return;
                applyLabel.getStyleClass().removeAll("apply-set");
                applyLabel.getStyleClass().addAll("apply-set-hover");
            });
            applyBtn.addEventHandler(MouseEvent.MOUSE_EXITED, (MouseEvent m) -> {
                if(applyBtn.isDisabled()) return;
                applyLabel.getStyleClass().removeAll("apply-set-hover");
                applyLabel.getStyleClass().addAll("apply-set");
            });
                        
            applyLabel = new Label();
            applyLabel.getStyleClass().addAll("apply-unset");
            applyLabel.setPrefSize(30, 18);
            applyBtn.setGraphic(applyLabel);
            applyBtn.setGraphicTextGap(-5);
            applyBtn.setContentDisplay(ContentDisplay.LEFT);
            
            sizeCombo = new ComboBox<>(FXCollections.observableArrayList(5, 6, 7, 8, 9, 10, 11, 12, 13, 
                14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72));
            sizeCombo.getSelectionModel().select(Integer.valueOf(12));
            sizeCombo.setMinWidth(65);
            sizeCombo.setValue(12);
            sizeCombo.setDisable(false);
            sizeCombo.setOnAction(evt -> {
                updateFontSize(sizeCombo.getValue());
            });
            sizeCombo.setTooltip(new Tooltip("set font size"));
            
            BooleanBinding selectionEmpty = new BooleanBinding() {
                { bind(area.selectionProperty()); }

                @Override
                protected boolean computeValue() {
                    return area.getSelection().getLength() == 0 || (area.getText() != null && area.getText().trim().equals(promptString));
                }
            };
            
            BooleanBinding textEmpty = new BooleanBinding() {
                { bind(area.lengthProperty()); }

                @Override
                protected boolean computeValue() {
                    return area.getText() == null || area.getText().trim().length() == 0 ||
                        (area.getText() != null && area.getText().trim().equals(promptString));
                }
            };
            
            undoBtn.disableProperty().bind(Bindings.not(area.undoAvailableProperty()).or(textEmpty));
            redoBtn.disableProperty().bind(Bindings.not(area.redoAvailableProperty()).or(textEmpty));

            cutBtn.disableProperty().bind(selectionEmpty);
            copyBtn.disableProperty().bind(selectionEmpty);
            
            textEmpty.addListener((v,o,n) -> {
                if(n) {
                    setTextModel(null);
                    messageText.setText(null);
                }
            });
            
            Platform.runLater(() -> {
                InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(this);
                iw.layoutBoundsProperty().addListener((v,o,n) -> {
                    inputSpecificRetinaChoices.relocate(Math.max(306, 306 + n.getWidth() - 537), 0);
                });
            });
            
            inputSpecificRetinaChoices = new ComboBox<>();
            inputSpecificRetinaChoices.setManaged(false);
            inputSpecificRetinaChoices.relocate(306, 0);
            inputSpecificRetinaChoices.getStyleClass().add("text-display-lang-detect");
            inputSpecificRetinaChoices.setDisable(true);
            inputSpecificRetinaChoices.disableProperty().bind(warningLabel.visibleProperty().not());
            inputSpecificRetinaChoices.setFocusTraversable(false);
            inputSpecificRetinaChoices.resize(200, 25);
            inputSpecificRetinaChoices.getItems().addAll(RetinaClientFactory.getRetinaClients());
            inputSpecificRetinaChoices.setPromptText("Select Language");
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
                Window w = WindowService.getInstance().windowFor(TextDisplay.this);
                WindowService.getInstance().windowTitleFor(w).selectRetina(n);
            });
            
            area.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                promptState.set(false);
            });
            
            area.focusedProperty().addListener((v,o,n) -> {
                if(!n) {
                    String text = area.getText();
                    if(text == null || text.trim().isEmpty()) {
                        promptState.set(true);
                    }
                }
            });
                    
            area.beingUpdatedProperty().addListener(createUpdatePropertyListener());
            
            HBox panel1 = new HBox(3.0);
            panel1.getChildren().addAll(
                applyBtn, undoBtn, redoBtn, cutBtn, copyBtn, pasteBtn, sizeCombo, inputSpecificRetinaChoices);
            
            area.setFocusTraversable(false);
            applyBtn.setFocusTraversable(false);
            undoBtn.setFocusTraversable(false);
            redoBtn.setFocusTraversable(false);
            cutBtn.setFocusTraversable(false);
            copyBtn.setFocusTraversable(false);
            pasteBtn.setFocusTraversable(false);
            sizeCombo.setFocusTraversable(false);      
            
            setPromptString(PROMPT_TEXT);
                        
            return panel1;
        }
        
        protected Button createPasteButton(String styleClass, Runnable action) {
            Button button = new Button();
            button.getStyleClass().add(styleClass);
            button.setOnAction(evt -> {
                promptState.set(false);
                action.run();
                area.requestFocus();
                Platform.runLater(() -> {
                    (new Thread(() -> {
                        try { Thread.sleep(500); } catch(Exception e) { e.printStackTrace(); }
                        
                        Platform.runLater(() -> {
                            apply();
                        });
                    })).start();
                });
            });
            button.setPrefWidth(20);
            button.setPrefHeight(20);
            return button;
        }
        
        /**
         * Enables or Disables the send button, altering its CSS to display
         * either look.
         * @param b
         */
        public void setSendEnabled(boolean b) {
            if(b) {
                applyBtn.getStyleClass().removeAll("apply-unset");
                applyBtn.getStyleClass().addAll("apply-set");
                applyBtn.setDisable(false);
                applyLabel.getStyleClass().removeAll("apply-unset");
                applyLabel.getStyleClass().addAll("apply-set");
                applyBtn.setPrefHeight(24);
                applyBtn.setMaxHeight(24);
            }else{
                applyBtn.getStyleClass().removeAll("apply-set");
                applyBtn.getStyleClass().addAll("apply-unset");
                applyBtn.setDisable(true);
                applyLabel.getStyleClass().removeAll("apply-set");
                applyLabel.getStyleClass().addAll("apply-unset");
                applyBtn.setPrefHeight(25);
                applyBtn.setMaxHeight(25);
            }
        }
        
        /**
         * Sets the prompt state property controlling
         * display of the prompt text.
         * 
         * @param b     the prompt state
         */
        public void setPromptState(boolean b) {
            promptState.set(b);
        }
        
    }
}
