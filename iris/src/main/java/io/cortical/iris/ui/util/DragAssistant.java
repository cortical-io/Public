package io.cortical.iris.ui.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cortical.fx.webstyle.Impression;
import io.cortical.iris.ApplicationService;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.ClipboardMessage;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Message;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.iris.ui.custom.widget.bubble.WordBubble;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.view.input.expression.BubbleExpressionBuilder;
import io.cortical.iris.view.input.expression.ExpressionDisplay;
import io.cortical.iris.view.input.expression.ExpressionModelDeserializer;
import io.cortical.iris.view.input.expression.ExpressionWordBubbleContainer;
import io.cortical.iris.view.input.expression.FingerprintBubble;
import io.cortical.iris.view.output.ContextDisplay.Context;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.retina.model.ExpressionFactory.ExpressionModel;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Model;
import io.cortical.retina.model.Term;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Pair;
import rx.Observable;
import rx.Observer;
import rx.subjects.ReplaySubject;

public class DragAssistant {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragAssistant.class);
    
    public static final DataFormat FINGERPRINT_FORMAT_KEY = new DataFormat("text/fingerprint");
    public static final DataFormat TERM_FORMAT_KEY = new DataFormat("text/term");
    public static final DataFormat TEXT_FORMAT_KEY = new DataFormat("text/text");
    public static final DataFormat EXPRESSION_FORMAT_KEY = new DataFormat("text/expression");
    public static final DataFormat FREEHAND_MODEL_KEY = new DataFormat("text/freehand");
    public static final DataFormat FINGERPRINT_IMAGE_KEY = new DataFormat("image/fingerprint");
    
    private static final ImageView view = new ImageView(new Image(Window.class.getClassLoader().getResourceAsStream("fingerprint.png"), 100d, 100d, true, true));
    
    private static Map<Window, ExpressionWordBubbleContainer> containerMapping = new HashMap<>(); 
    
    private static ReplaySubject<ClipboardMessage> subject;
    
    
    
    
    
    public static void configureDropHandler(ExpressionWordBubbleContainer target) {
        // For case where there are multiple input windows.
        InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(target);
        containerMapping.put(iw, target);
        
        target.setOnDragOver(e -> {
            /* accept it only if it is  not dragged from the same node 
             * and if it has a string data */
            if (e.getGestureSource() != target &&
                    (e.getDragboard().hasContent(EXPRESSION_FORMAT_KEY) || 
                     e.getDragboard().hasContent(FINGERPRINT_FORMAT_KEY) ||
                     e.getDragboard().hasContent(TERM_FORMAT_KEY))) {
                /* allow for moving */
                e.acceptTransferModes(TransferMode.COPY);
                
                int index = target.getInsertionIndex(e.getX(), e.getY());
                if(index == -1) return;
                target.moveCursor(index);
                
                target.getExpressionEntry().cursorVisibleProperty().set(true);
            }            
        });
        
        target.setOnDragDone(e -> {
            target.getExpressionEntry().cursorVisibleProperty().set(false);
        });
        
        target.setOnDragDropped(event -> {
            subject.subscribe(getExpressionDisplayCleanupObserver(event, target));
            
            Observer<ClipboardMessage> o = getClipboardObserver(target);
            subject.subscribe(o);
            
            event.consume();
        });
    }
    
    public static void configureDragHandler(Button source) {
        source.setOnDragDetected(e -> {
            if(e.isSecondaryButtonDown()) return;
            
            /* allow MOVE transfer mode */
            Dragboard db = source.startDragAndDrop(TransferMode.COPY);
            
            String text = null;
            if((text = ApplicationService.getInstance().getClipboardContent()) != null) {
                /* put a string on dragboard */
                ClipboardContent content = new ClipboardContent();
                content.putImage(new Image(Window.class.getClassLoader().getResourceAsStream("results_fingerprint.png"), 100d, 100d, true, true));
                content.put(TERM_FORMAT_KEY, text);
                db.setContent(content);
                
                ((Node) e.getSource()).setCursor(Cursor.HAND);
                
                getDragboardTransformer(db, text);
                
                e.consume();
            }
        });
        
        source.setOnDragDone(e -> {
            ObservableList<UUID> winIDList = WindowService.getInstance().inputWindowListProperty().get();
            for(UUID id : winIDList) {
                InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(id);
                ((ExpressionDisplay)iw.getViewArea().getView(ViewType.EXPRESSION)).getBubbleContainer().getExpressionEntry().cursorVisibleProperty().set(false);
            }
        });
    }
    
    public static void configureDragHandler(WordBubble source) {
        source.setOnDragDetected(e -> {
            if(e.isSecondaryButtonDown()) return;
            
            /* allow MOVE transfer mode */
            Dragboard db = source.startDragAndDrop(TransferMode.COPY);
            
            /* put a string on dragboard */
            ClipboardContent content = new ClipboardContent();
            content.putImage(createTermCursor(source.getTerm(), WindowService.getInstance().windowFor(source)));
            content.put(TERM_FORMAT_KEY, source.getText());
            db.setContent(content);
            
            ((Node) e.getSource()).setCursor(Cursor.HAND);
            
            getDragboardTransformer(db, source.getText());
            
            e.consume();
        });
        
        source.setOnDragDone(e -> {
            ExpressionWordBubbleContainer container = getPrimaryInputContainer(source);
            if(container != null) {
                container.getExpressionEntry().cursorVisibleProperty().set(false);
            }
        });
    }
    
    public static void configureDragHandler(FingerprintBubble source) {
        source.setOnDragDetected(e -> {
            if(e.isSecondaryButtonDown()) return;
            
            /* allow MOVE transfer mode */
            Dragboard db = source.startDragAndDrop(TransferMode.COPY);
            
            /* put a string on dragboard */
            ClipboardContent content = new ClipboardContent();
            content.putImage(new Image(Window.class.getClassLoader().getResourceAsStream("results_fingerprint.png"), 100d, 100d, true, true));
            content.put(TERM_FORMAT_KEY, source.getText());
            db.setContent(content);
            
            ((Node) e.getSource()).setCursor(Cursor.HAND);
            
            e.consume();
        });
        
        source.setOnDragDone(e -> {
            ExpressionWordBubbleContainer container = getPrimaryInputContainer(source);
            if(container != null) {
                container.getExpressionEntry().cursorVisibleProperty().set(false);
            }
        });
    }
    
    public static void configureDragHandler(Impression source) {
        source.addDragDetectBehavior(e -> {
            if(source.isEmpty() || e.isSecondaryButtonDown()) return;
            
            /* Have all Windows showing an ExpressionDisplay, snapshot themselves. */
            snapshotInputWindows();
            
            /* allow MOVE transfer mode */
            Dragboard db = source.startDragAndDrop(TransferMode.COPY);
            
            /* put a string on dragboard */
            ClipboardContent content = new ClipboardContent();
            content.putImage(new Image(Window.class.getClassLoader().getResourceAsStream("results_fingerprint.png"), 100d, 100d, true, true));
            
            // Collect Dense array
            String positionsString = Arrays.stream(source.getSDR()).boxed().map(i -> i.toString()).collect(Collectors.joining(","));
            String[] positionsArray = positionsString.split(",");
            
            // Make Sparse array
            StringBuilder sb = new StringBuilder();
            IntStream.range(0, 128 * 128).filter(i -> !positionsArray[i].equals("0")).forEach(i -> sb.append(i).append(","));
            sb.setLength(sb.length() - 1);
            
            // Store Fingerprint JSON
            String positionsJson = "\"positions\":[" + sb.toString() + "]";
            content.put(FINGERPRINT_FORMAT_KEY, positionsJson);
            db.setContent(content);
            
            getDragboardTransformer(db, positionsJson);
            
            source.setCursor(Cursor.HAND);
            
            e.consume();
        });
        
        source.setOnDragDone(e -> {
            ExpressionWordBubbleContainer container = getPrimaryInputContainer(source);
            if(container != null) {
                container.getExpressionEntry().cursorVisibleProperty().set(false);
            }
        });
    }
    
    static Pane currentDialog;
    public static void configureCompareImpressionDisplayDragHandler(Impression source) {
        source.addDragDetectBehavior(e -> {
            if(source.isEmpty() || e.isSecondaryButtonDown()) return;
            
            /* Have all Windows showing an ExpressionDisplay, snapshot themselves. */
            snapshotInputWindows();
            
            /* allow MOVE transfer mode */
            Dragboard db = source.startDragAndDrop(TransferMode.COPY);
            
            /* put a string on dragboard */
            ClipboardContent content = new ClipboardContent();
            content.putImage(new Image(Window.class.getClassLoader().getResourceAsStream("results_fingerprint.png"), 100d, 100d, true, true));
            
            // Collect Dense array
            String positionsString = Arrays.stream(source.getSDR()).boxed().map(i -> i.toString()).collect(Collectors.joining(","));
            String[] positionsArray = positionsString.split(",");
            
            // Make Sparse array
            StringBuilder sb = new StringBuilder();
            IntStream.range(0, 128 * 128).filter(i -> !positionsArray[i].equals("0")).forEach(i -> sb.append(i).append(","));
            sb.setLength(sb.length() - 1);
            
            // Store Fingerprint JSON
            String positionsJson = "\"positions\":[" + sb.toString() + "]";
            content.put(FINGERPRINT_FORMAT_KEY, positionsJson);
            db.setContent(content);
            
            source.setCursor(Cursor.HAND);
            
            getDragboardTransformer(db, positionsJson);
            
            currentDialog = WindowService.getInstance().getContentPane().getOverlay().getDialog();
            EventBus.get().broadcast(BusEvent.OVERLAY_DISMISS_MODAL_DIALOG.subj(), Payload.DUMMY_PAYLOAD);
            
            e.consume();
        });
        
        // Restore Compare Overlay
        source.setOnDragDone(e -> {
            (new Thread(() -> {
                try { Thread.sleep(700); }catch(Exception ex) { ex.printStackTrace(); }
                Platform.runLater(() -> {
                    ExpressionWordBubbleContainer container = containerMapping.get(WindowService.getInstance().windowFor(source));
                    if(container != null) {
                        container.typeEscape();
                    }
                    
                    Platform.runLater(() -> EventBus.get().broadcast(BusEvent.OVERLAY_SHOW_MODAL_DIALOG.subj(), new Payload(currentDialog)));
                });
            })).start();
            
            ExpressionWordBubbleContainer container = getPrimaryInputContainer(source);
            if(container != null) {
                container.getExpressionEntry().cursorVisibleProperty().set(false);
            }
        });
    }
    
    public static void configureDragHandler(Text source, TreeTableCell<Context, String> cell) {
        source.setOnMouseDragged(e -> {
            if(e.isSecondaryButtonDown()) return;
            
            source.setFont(Font.font("Arial", FontWeight.BOLD, FontPosture.ITALIC, 18));
            
            e.consume();
        });
        source.setOnDragDone(e -> {
            source.setFont(Font.font("Questrial", FontWeight.BOLD, FontPosture.REGULAR, 14));
            if(cell != null) {
                cell.setPrefHeight(cell.getPrefHeight() - 10);
                cell.requestLayout();
            }
            
            ExpressionWordBubbleContainer container = getPrimaryInputContainer(source);
            if(container != null) {
                container.getExpressionEntry().cursorVisibleProperty().set(false);
            }
            
            e.consume();
        });
        source.setOnDragDetected(e -> {
            if(e.isSecondaryButtonDown()) return;
            
            /* allow MOVE transfer mode */
            Dragboard db = source.startDragAndDrop(TransferMode.COPY);
            
            /* put a string on dragboard */
            ClipboardContent content = new ClipboardContent();
            content.putImage(createTermCursor(source.getText(), WindowService.getInstance().windowFor(source)));
            content.put(TERM_FORMAT_KEY,source.getText());
            db.setContent(content);
            
            if(cell != null) {
                cell.setPrefHeight(cell.getPrefHeight() + 10);
                cell.requestLayout();
            }
            
            ((Node) e.getSource()).setCursor(Cursor.HAND);
            
            getDragboardTransformer(db, source.getText());
            
            e.consume();
        });
        source.setOnMouseEntered(e -> {
            source.setFill(Color.rgb(237, 93, 37));
            source.setCursor(Cursor.HAND);
            Timer t = new Timer(3000, se -> {
                source.setFill(Color.BLACK);
                if(source.getParent() != null) {
                    source.getParent().requestLayout();
                }
                source.setCursor(Cursor.DEFAULT);
            });
            t.setRepeats(false);
            t.start();
            if(source.getParent() != null) {
                source.getParent().requestLayout();
            }
        });
        source.setOnMouseExited(e -> {
            source.setFill(Color.BLACK);
            if(source.getParent() != null) {
                source.getParent().requestLayout();
            }
            source.setCursor(Cursor.DEFAULT);
        });
    }
    
    public static Observer<ClipboardMessage> getClipboardObserver(ExpressionWordBubbleContainer target) {
        Observer<ClipboardMessage> observer = new Observer<ClipboardMessage>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) {
                LOGGER.debug("Got OnError of: " + e.getMessage());
                
                Window w = WindowService.getInstance().windowFor(target);
                Payload p = new Payload(new Pair<UUID, Message>(w.getWindowID(), null));
                ServerRequest req = new ServerRequest();
                req.setPayload(p);
                RequestErrorContext rec = new RequestErrorContext(req, null, e, true);
                p.setPayload(rec);
                EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_REQUEST_ERROR.subj() + w.getWindowID(), p);
            }
            @Override public void onNext(ClipboardMessage m) {
                LOGGER.debug("Got OnNext of: " + m.getContent());
                
                WindowConfig config = new WindowConfig(null);
                config.setDoReset(false);
                config.setIsDragNDrop(true);
                config.bubbleInsertionIdx = target.getEditorIndex();
                config.focusTraversalIdx = target.getFocusTraversalIndex();
                config.bubbleList = m.getContent().getKey();
                config.expressionList = m.getContent().getValue();
                
                InputWindow w = (InputWindow)WindowService.getInstance().windowFor(target);
                ExpressionDisplay display = (ExpressionDisplay)w.getViewArea().getView(ViewType.EXPRESSION);
                display.getBubbleContainer().getExpressionEntry().cursorVisibleProperty().set(false);
                display.configure(config);
            }
        };
        
        return observer;
    }
    
    static class SerializableImageContainer implements Serializable {
        /** Serial Version */
        private static final long serialVersionUID = 1L;
        
        private transient Image image;
        public SerializableImageContainer(Image i) {
            this.image = i;
        }
        
        private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
            s.defaultReadObject();
            image = SwingFXUtils.toFXImage(ImageIO.read(s), null);
        }

        private void writeObject(ObjectOutputStream s) throws IOException {
            s.defaultWriteObject();
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", s);
        }
        
        public Image getImage() {
            return image;
        }
    }
    
    public static Observable<ClipboardMessage> getClipboardTransformer(String content) {
        subject = ReplaySubject.createWithSize(1);
        
        doTransform(null, subject, content);
        
        return subject;
    }
    
    private static ClipboardMessage setClipboardContent(Clipboard cb, DataFormat key, Pair<List<Bubble.Type>, List<String>> pair) {
        ClipboardMessage message = new ClipboardMessage(pair);
        ClipboardContent clip = new ClipboardContent();
        clip.put(key, message);
        cb.setContent(clip);
        
        return message;
    }
    
    public static Observable<ClipboardMessage> getDragboardTransformer(Dragboard db, String content) {
        subject = ReplaySubject.createWithSize(1);
        
        doTransform(db, subject, content);
        
        return subject;
    }
    
    private static ClipboardMessage setDragboardContent(Dragboard db, DataFormat key, Pair<List<Bubble.Type>, List<String>> pair) {
        ClipboardMessage message = new ClipboardMessage(pair);
        ClipboardContent clip = new ClipboardContent();
        clip.put(key, message);
        db.setContent(clip);
        
        return message;
    }
    
    private static void doTransform(Dragboard db, ReplaySubject<ClipboardMessage> subject, String content) {
        (new Thread(() -> {
            System.out.println("OnSubscribe running... : " + content);
            if(content != null && !content.isEmpty()) {
                BubbleExpressionBuilder builder = new BubbleExpressionBuilder();
                
                if(content.trim().split("\\s+").length > 2 || content.indexOf(":[") != -1) {
                    boolean isValidJson = false;
                    try {
                        if(ExpressionModelDeserializer.isJsonString(content) || ExpressionModelDeserializer.extractPositionsArray(content) != null) {
                            isValidJson = true;
                        }
                    }catch(Exception ignore) {}
                    
                    if(!isValidJson) {
                        isValidJson = ExpressionModelDeserializer.extractPositionsArray(content) != null;
                    }
                    
                    if(isValidJson) {                                       // JSON
                        LOGGER.debug("Clipboard Transformer processing: JSON");
                        try {
                            Model m = ExpressionModelDeserializer.narrow(content);
                            DataFormat key = null;
                            if(m instanceof ExpressionModel) {
                                key = EXPRESSION_FORMAT_KEY;
                            }else if(m instanceof Fingerprint) {
                                key = FINGERPRINT_FORMAT_KEY;
                            }else if(m instanceof Term) {
                                key = TERM_FORMAT_KEY;
                            }else if(m instanceof io.cortical.retina.model.Text) {
                                key = TEXT_FORMAT_KEY;
                            }else{
                                subject.onError(new IllegalStateException("ExpressionModelDeserializer couldn't narrow the specified JSON: \n" + content));
                                return;
                            }
                            
                            final DataFormat finalKey = key;
                            Platform.runLater(() -> {
                                Pair<List<Bubble.Type>, List<String>> result = builder.doParse(m, null, 0);
                                ClipboardMessage message = db == null ? 
                                    setClipboardContent(Clipboard.getSystemClipboard(), finalKey, result) : 
                                        setDragboardContent(db, finalKey, result);
                                subject.onNext(message);
                            });
                        }catch(Exception e) {
                            subject.onError(e);
                        }
                    } else if(builder.isValidFreehand(content)) {           // Freehand Expression
                        LOGGER.debug("Clipboard Transformer processing: FREEHAND");
                        Platform.runLater(() -> {
                            try {
                                Pair<List<Bubble.Type>, List<String>> pair = builder.doParse(content);
                                ClipboardMessage message = db == null ? 
                                    setClipboardContent(Clipboard.getSystemClipboard(), FREEHAND_MODEL_KEY, pair) : 
                                        setDragboardContent(db, FREEHAND_MODEL_KEY, pair);
                                subject.onNext(message);
                            }catch(Exception e) {
                                subject.onError(e);
                            }
                        });
                    } else {                                                // Default to Text
                        LOGGER.debug("Clipboard Transformer processing: TEXT");
                        Platform.runLater(() -> {
                            String json = "{ \"text\": \"" + content + "\" }";
                            try {
                                Model m = ExpressionModelDeserializer.narrow(json);
                                Pair<List<Bubble.Type>, List<String>> result = builder.doParse(m, null, 0);
                                ClipboardMessage message = db == null ? 
                                    setClipboardContent(Clipboard.getSystemClipboard(), TEXT_FORMAT_KEY, result) : 
                                        setDragboardContent(db, TEXT_FORMAT_KEY, result);
                                subject.onNext(message);
                            }catch(Exception e) {
                                subject.onError(e);
                            }
                        });
                    }
                } else {                                                    // Term
                    LOGGER.debug("Clipboard Transformer processing: TERM");
                    Platform.runLater(() -> {
                        String json = "{ \"term\": \"" + content + "\" }";
                        try {
                            Model m = ExpressionModelDeserializer.narrow(json);
                            Pair<List<Bubble.Type>, List<String>> result = builder.doParse(m, null, 0);
                            ClipboardMessage message = db == null ? 
                                setClipboardContent(Clipboard.getSystemClipboard(), TERM_FORMAT_KEY, result) : 
                                    setDragboardContent(db, TERM_FORMAT_KEY, result);
                            subject.onNext(message);
                        }catch(Exception e) {
                            subject.onError(e);
                        }
                    });
                }
            }
        })).start();
    }
    
    /**
     * Returns an {@link Image} used to display the specified term on top
     * of an image of a fingerprint.
     * 
     * @param term      the term which is being "dragged"   
     * @param region    the parent node of the temporary image.
     * @return
     */
    private static Image createTermCursor(String term, Window region) {
        Text text = new Text(term);
        text.setStyle("-fx-font-size: 20");
        TextFlow flow = new TextFlow(text);
        flow.setPrefSize(100, 100);
        flow.setTextAlignment(TextAlignment.CENTER);
        flow.setManaged(false);
        flow.relocate(45 - (text.getLayoutBounds().getWidth() / 2), 30 - (text.getLayoutBounds().getHeight() / 2));
        
        StackPane pane = new StackPane();
        pane.resize(100, 100);
        pane.getChildren().addAll(view, flow);
        pane.setStyle("-fx-background-color: transparent");
        
        region.getChildren().add(pane);
        
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage img = pane.snapshot(sp, null);
        
        region.getChildren().remove(pane);
        
        return img;
    }
    
    /**
     * Returns an {@link Observer} which will clean up the display 
     * following a successful or unsuccessful drag-n-drop operation.
     * 
     * @param event
     * @param target
     * @return
     */
    public static Observer<ClipboardMessage> getExpressionDisplayCleanupObserver(
        DragEvent event, ExpressionWordBubbleContainer target) {
        
        return new Observer<ClipboardMessage>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable ex) { 
                doExpressionDisplayCleanup(target);
                if(event != null) {
                    event.setDropCompleted(false);
                }
            }
            @Override public void onNext(ClipboardMessage m) {
                if(event != null) {
                    event.setDropCompleted(true);
                }
            }
        };
    }
    
    /**
     * Resets the drop detection position cursor.
     * 
     * @param target
     */
    private static void doExpressionDisplayCleanup(ExpressionWordBubbleContainer target) {
        target.getExpressionEntry().cursorVisibleProperty().set(false);
    }
   
    /**
     * Returns the {@link ExpressionWordBubbleContainer} which resides within the
     * primary {@link InputWindow} connected to the {@link OutputWindow} containing
     * the specified node.
     * 
     * @param outputNode
     * @return
     */
    private static ExpressionWordBubbleContainer getPrimaryInputContainer(Node outputNode) {
        Window w = WindowService.getInstance().windowFor(outputNode);
        if(w == null) return null;
        
        InputWindow iw = null;
        if(!(w instanceof InputWindow)) {
            OutputWindow ow = (OutputWindow)w;
            iw = (InputWindow)ow.getInputSelector().getPrimaryWindow();
        } else {
            iw = (InputWindow)w;
        }
        
        return ((ExpressionDisplay)iw.getViewArea().getView(ViewType.EXPRESSION)).getBubbleContainer();
    }
    
    /**
     * Instructs all {@link InputWindow}s to snapshot
     * themselves if they are showing an ExpressionDisplay.
     * This method executes in a separate thread.
     */
    private static void snapshotInputWindows() {
        (new Thread(() -> {
            WindowService.getInstance().inputWindowListProperty().get().stream()
                .map(uuid -> WindowService.getInstance().windowFor(uuid))
                .forEach(w -> ((InputWindow)w).snapshot());
        })).start();
    }
}
