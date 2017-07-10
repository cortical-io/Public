package io.cortical.iris.window;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fxpresso.tidbit.ui.Flyout;
import io.cortical.iris.ApplicationService;
import io.cortical.iris.ApplicationService.ApplicationState;
import io.cortical.iris.RetinaClientFactory;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.persistence.ConfigHandler;
import io.cortical.iris.persistence.Persistable;
import io.cortical.iris.persistence.Persistence;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.WindowPane;
import io.cortical.iris.ui.custom.widget.WindowTitlePane;
import io.cortical.iris.view.InputViewArea;
import io.cortical.iris.view.OutputViewArea;
import io.cortical.iris.view.ViewArea;
import io.cortical.iris.view.WindowContext;
import io.cortical.iris.view.output.SimilarTermsDisplay;
import io.cortical.iris.view.output.TokensDisplay;
import io.cortical.retina.client.FullClient;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import javafx.util.Pair;

/**
 * A {@code Window} within IRIS is a derivative of {@link Pane} which has 
 * controls and behaviors similar to a formal Window, but operates internally
 * such as an MDI would or a JInternalFrame would in a "Swing" application. 
 */
public abstract class Window extends Pane implements WindowContext, Persistable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(Window.class);
    
    /** Function which returns an EventHandler lambda for saving WindowConfigs */
    public static final Function<Window, EventHandler<ActionEvent>> SAVE_FUNCTION =  w -> e -> {
        Payload p = new Payload(w);
        EventBus.get().broadcast(BusEvent.APPLICATION_WINDOW_SAVE_PROMPT.subj() + w.getWindowID(), p);
    };
    /** Function which returns an EventHandler lambda for loading WindowConfigs */
    public static final Function<Window, EventHandler<ActionEvent>> LOAD_FUNCTION =  w -> e -> {
        Payload p = new Payload(w);
        EventBus.get().broadcast(BusEvent.APPLICATION_WINDOW_LOAD_PROMPT.subj() + w.getWindowID(), p);
    };
    /** Function which returns an EventHandler lambda for deleting WindowConfigs */
    public static final Function<Window, EventHandler<ActionEvent>> DELETE_FUNCTION =  w -> e -> {
        Payload p = new Payload(w);
        EventBus.get().broadcast(BusEvent.APPLICATION_WINDOW_DELETE_PROMPT.subj(), p);
    };
    /** Function which returns an EventHandler lambda for snapshotting nodes. */
    public static final BiConsumer<UUID, Pair<Image, String>> SNAPSHOT_FUNCTION =  (uuid, pair) -> {
        Payload p = new Payload(pair.getKey());
        p.setDescription(pair.getValue());
        EventBus.get().broadcast(BusEvent.APPLICATION_NODE_SAVE_SNAPSHOT.subj() + uuid, p);
    };
    
    public enum Type { INPUT, OUTPUT };

    public transient double MIN_WIDTH = 200;
    public transient double MIN_HEIGHT = 151;
    transient double INI_WIDTH = 508;
    transient double INI_HEIGHT = 151;

    protected transient Flyout warnNotifier;
    protected transient Flyout errorNotifier;
    protected transient Flyout promptNotifier;
    protected transient WarnDialog warnDialog;
    protected transient ErrorDialog errorDialog;
    protected transient PromptDialog promptDialog;
    
    protected transient Pane windowContent;
    protected transient Pane backContent;

    protected TitleBar titleBar;
    protected transient InputBar inputBar;
    protected transient StatusBar statusBar;
    protected transient ViewArea contentArea;
    protected transient Text title;
    protected transient WindowController controller;

    protected transient ChangeListener<Boolean> visibilityListener;

    protected transient WindowIcon windowIcon;

    protected transient UUID windowID;

    protected transient ReadOnlyObjectWrapper<String> messageProperty;

    protected transient BooleanProperty destroyProperty = new SimpleBooleanProperty();

    protected transient BooleanProperty selectedProperty = new SimpleBooleanProperty();

    private transient Timeline tl = new Timeline();
    private transient Timeline flash;
    private transient Timeline antiflash;
    private transient Interpolator interpolator = Interpolator.SPLINE(0.5, 0.1, 0.1, 0.5);
    protected transient boolean isBackShowing;

    protected transient BackPanelSupplier backPanelSupplier;
    protected transient ChangeListener<? super Bounds> backContentListener;

    

    /**
     * Creates a new {@code Window}
     */
    public Window() {
        windowID = UUID.randomUUID();

        messageProperty = new ReadOnlyObjectWrapper<>();
        
        selectedProperty.addListener((v,o,n) -> {
            invokeWindowSelectionCSS(n);
            if(n) {
                toFront();
            }else if(!isInput()) {
                dismissInputSelector();
            }
        });
        
        ////////////////////////////////////////////
        //      Handles Warning Msg From Bus      //
        ////////////////////////////////////////////
        EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_REQUEST_WARNING.subj() + windowID, (s,p) -> {
            if(warnNotifier == null) {
                warnNotifier = createWarnNotifier();
            }
            
            Platform.runLater(() -> {
                showWarnDialog(((RequestErrorContext)p.getPayload()).getMessage());
                System.out.println("message: " + ((RequestErrorContext)p.getPayload()).getMessage());
            });
        });

        ////////////////////////////////////////////
        //       Handles Error Msg From Bus       //
        ////////////////////////////////////////////
        EventBus.get().subscribeTo(BusEvent.SERVER_MESSAGE_REQUEST_ERROR.subj() + windowID, (s,p) -> {
            if(errorNotifier == null) {
                errorNotifier = createErrorNotifier();
            }
            
            Platform.runLater(() -> {
                showErrorDialog(((RequestErrorContext)p.getPayload()).getMessage());
            });
            
            if(isInput()) {
                ((InputWindow)this).getViewArea().getSelectedView().processRequestError((RequestErrorContext)p.getPayload());
            }else{
                ((OutputWindow)this).getViewArea().getSelectedView().processRequestError((RequestErrorContext)p.getPayload());
            }
        });
        
        ////////////////////////////////////////////
        //      Handles Prompt Msg From Bus       //
        ////////////////////////////////////////////
        EventBus.get().subscribeTo(BusEvent.APPLICATION_WINDOW_MESSAGE_PROMPT.subj() + windowID, (s,p) -> {
            if(promptNotifier == null) {
                promptNotifier = createPromptNotifier();
            }
            
            Platform.runLater(() -> {
                showPromptDialog(((RequestErrorContext)p.getPayload()).getMessage());
            });
        });
        
        /////////////////////////
        //  Window ID Flashing //
        /////////////////////////
        antiflash = new Timeline(
            new KeyFrame(Duration.seconds(0.7), e -> {
                invokeWindowSelectionCSS(false);
            }),

            new KeyFrame(Duration.seconds(0.2), e -> {
                invokeWindowSelectionCSS(true);
            })
        );
        
        antiflash.setCycleCount(5);
        
        flash = new Timeline(
            new KeyFrame(Duration.seconds(0.2), e -> {
                invokeWindowSelectionCSS(true);
            }),

            new KeyFrame(Duration.seconds(0.7), e -> {
                invokeWindowSelectionCSS(false);
            })
        );
            
        flash.setCycleCount(5);
   
    }
    
    /**
     * {@inheritDoc}
     */
    public void serialize(WindowConfig config) {
        ApplicationService.getInstance().privilegedStateChange(ApplicationState.SERIALIZING);
        
        if(!isInput()) {
            WindowService.getInstance().setOutputWindowConfig(config);
        }
        
        Persistence.get().write(config, this);
        
        // Serialize connected InputWindows for this OutputWindow
        Map<String, String[]> outputConfigMap = ApplicationService.getInstance().checkConfigMapping();
        if(!isInput() && config.getPrimarySerialFileName() != null) {
            config.getPrimaryWindow().serialize(new WindowConfig(config.getPrimarySerialFileName()));
            String[] ins = new String[] { config.getPrimarySerialFileName(), null };
            outputConfigMap.put(config.getFileName(), ins);
            if(config.getSecondarySerialFileName() != null) {
                config.getSecondaryWindow().serialize(new WindowConfig(config.getSecondarySerialFileName()));
                ins[1] = config.getSecondarySerialFileName();
                outputConfigMap.put(config.getFileName(), ins);
            }
        } else if(!isInput()) {
            outputConfigMap.put(config.getFileName(), new String[] { null, null });
        }
        ApplicationService.getInstance().persistConfigMap();
        
        Platform.runLater(() -> {
            ApplicationService.getInstance().privilegedStateChange(ApplicationState.NORMAL);
        });
    }
    
    /**
     * {@inheritDoc}
     */
    public void deSerialize(WindowConfig config) {
        ApplicationService.getInstance().privilegedStateChange(ApplicationState.DESERIALIZING);
        Persistence.get().read(config);
        Platform.runLater(() -> {
            ApplicationService.getInstance().privilegedStateChange(ApplicationState.NORMAL);
        });
    }
    
    /**
     * Restores the state of a saved window configuration, 
     * into this {@link Window}
     * @param config
     */
    public void configure(WindowConfig config) {
        ApplicationService.getInstance().privilegedStateChange(ApplicationState.CONFIGURING);
        
        Platform.runLater(() -> {
            final Pane p = WindowService.getInstance().getContentPane().getWindowPane();
            Bounds b = config.getBounds(); 
            relocate(
                Math.max(0, Math.min(p.getWidth() - b.getWidth(), b.getMinX())), 
                Math.max(0, Math.min(p.getHeight() - b.getHeight(), b.getMinY())));
            
            resize(b.getWidth(), b.getHeight());
            
            getTitleBar().setTitleWithEvent(config.getTitle());
            
            if(isInput()) {
                getTitleBar().getColorIDTab().colorIDProperty().set(config.getIDColor());
                contentArea.configure(config);
                WindowTitlePane wtp = WindowService.getInstance().windowTitleFor(this);
                wtp.selectRetina(config.getSelectedRetina());
            } else {
                InputSelector is = ((OutputWindow)this).getInputSelector();
                if(config.getPrimarySerialFileName() != null) {
                    Platform.runLater(() -> {
                        //Un-select all connected InputWindows
                        is.getInputSelection().stream().forEach(uuid -> is.selectInputWindow(uuid, false));
                        
                        InputWindow iw = WindowService.getInstance().addInputWindow();
                        WindowService.getInstance().configureWindow(iw, new WindowConfig(config.getPrimarySerialFileName()));
                        
                        is.selectInputWindow(iw.getWindowID(), true);
                   });
                }
                if(config.getSecondarySerialFileName() != null) {
                    Platform.runLater(() -> {
                        InputWindow iw = WindowService.getInstance().addInputWindow();
                        WindowService.getInstance().configureWindow(iw, new WindowConfig(config.getSecondarySerialFileName()));
                    
                        is.selectInputWindow(iw.getWindowID(), true);
                    });
                }
                
                contentArea.configure(config);
            }
            
            getController().nudgeLayout(this, 4);
            
            requestLayout();
            
            Platform.runLater(() -> {
                ApplicationService.getInstance().privilegedStateChange(ApplicationState.CONFIGURING);
            });
        });
    }
    
    /**
     * {@inheritDoc}
     * @param out
     * @throws IOException
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // Input or Output Window?
        out.writeBoolean(isInput());
        LOGGER.debug("wrote boolean isInput ? " + isInput());
        
        // Window dimensions and location
        Bounds b = getBoundsInParent();
        out.writeDouble(b.getMinX());
        out.writeDouble(b.getMinY());
        out.writeDouble(b.getWidth());
        out.writeDouble(b.getHeight());
        LOGGER.debug("wrote window bounds: " + b.getMinX() + "," + b.getMinY() +
            b.getWidth() + "," + b.getHeight());
       
        out.writeObject(titleBar);
        out.writeObject(contentArea);
        
        WindowTitlePane wtp = WindowService.getInstance().windowTitleFor(this);
        if(isInput()) {
            out.writeObject(wtp.selectedRetinaNameProperty().get());
        }
        
        out.writeObject(wtp);
        LOGGER.debug("wrote WindowTitlePane");
    }
    
    /**
     * {@inheritDoc}
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        WindowConfig config = WindowService.getInstance().windowConfigProperty().get();
        
        boolean isInput = in.readBoolean();
        config.isInput = isInput;
        LOGGER.debug("read boolean isInput ? " + isInput);
        
        if(!isInput) {
            initNotificationChain(config);
        }
        
        double x = in.readDouble();
        double y = in.readDouble();
        double w = in.readDouble();
        double h = in.readDouble();
        config.bounds = new BoundingBox(x, y, w, h);
        LOGGER.debug("read window bounds: " + x + "," + y + "," + w + "," + h);
        
        in.readObject(); // titleBar
        in.readObject(); // contentArea
        LOGGER.debug("read title -> " + config.getTitle());
        if(isInput) {
            String selectedRetinaType = (String)in.readObject();
            config.selectedRetina = (FullClient)RetinaClientFactory.getClient(selectedRetinaType);
            LOGGER.debug("read selected client: " + selectedRetinaType);
        }
        
        in.readObject(); // WindowTitlePane
        LOGGER.debug("read WindowTitlePane");
        
        config.setLoaded(true);
    }
    
    /**
     * Called to add a chain of handlers whose {@link ConfigHandler#handle(WindowConfig)} method
     * will be called - each following a short delay so that resulting layout 
     * can "settle". 
     * @param config
     */
    private void initNotificationChain(WindowConfig config) {
        config.addNotificationChainHandler(0, WindowService.getInstance().windowTitleFor(
            config.getCurrentWindow()).getChainHandler());
        config.addNotificationChainHandler(1, OutputViewArea.getChainHandler());
        config.addNotificationChainHandler(2, SimilarTermsDisplay.getChainHandler());
        config.addNotificationChainHandler(3, TokensDisplay.getChainHandler());
    }
    
    /**
     * Disconnects listeners and handlers and frees resources
     * upon this {@code Window} closing.
     */
    protected abstract void releaseResourcesForWindowClose();
    
    /**
     * Instructs this {@link Window} to flash its borders
     */
    public void flash() {
        if(flash.getStatus() == Animation.Status.RUNNING ||
            antiflash.getStatus() == Animation.Status.RUNNING) return;
        
        if(selectedProperty.get()) {
            antiflash.play();
            antiflash.setOnFinished(e -> {
                invokeWindowSelectionCSS(true);
            });
        }else{
            flash.play();
            flash.setOnFinished(e -> {
                invokeWindowSelectionCSS(false);
            });
        }
    }
    
    /**
     * Instructs this {@link Window} to stop flashing its borders
     */
    public void stopFlash() {
        flash.stop();
        antiflash.stop();
        invokeWindowSelectionCSS(selectedProperty.get());
    }
    
    private void invokeWindowSelectionCSS(boolean isSelected) {
        if(isSelected) {
            if(isInput()) {
                getTitleBar().getColorIDTab().getStyleClass().removeAll("id-tab");
                getTitleBar().getColorIDTab().getStyleClass().addAll("id-tab-selected");
            }
            getTitleBar().getStyleClass().removeAll("title-bar");
            getTitleBar().getStyleClass().addAll("title-bar-selected");
            getStyleClass().removeAll("app-window");
            getStyleClass().add("app-window-selected");
        }else{
            if(isInput()) {
                getTitleBar().getColorIDTab().getStyleClass().removeAll("id-tab-selected");
                getTitleBar().getColorIDTab().getStyleClass().addAll("id-tab");
            } 
            getTitleBar().getStyleClass().removeAll("title-bar-selected");
            getTitleBar().getStyleClass().addAll("title-bar");
            getStyleClass().removeAll("app-window-selected");
            getStyleClass().add("app-window");
        }
    }
    
    /**
     * Creates the {@link Flyout} which contains the decorations needed to
     * present a warning dialog to the user.
     * 
     * @return  a {@code Flyout} used to present warnings
     */
    private Flyout createWarnNotifier() {
        // Create invisible "anchor" for flyout
        Rectangle r = new Rectangle(0, 0, 5, 1);
        r.setManaged(false);
        r.setVisible(false);
        r.toBack();

        warnDialog = createWarnDialog();
        Flyout retVal = new Flyout(r, warnDialog);
        retVal.setFlyoutStyle("-fx-background-color: rgb(0,0,0,0.7);-fx-background-radius: 5 5 5 5;");
        getChildren().add(retVal);
        return retVal;
    }

    /**
     * Creates the {@link Flyout} which contains the decorations needed to
     * present an error dialog to the user.
     * 
     * @return  a {@code Flyout} used to present errors
     */
    private Flyout createErrorNotifier() {
        // Create invisible "anchor" for flyout
        Rectangle r = new Rectangle(0, 0, 5, 1);
        r.setManaged(false);
        r.setVisible(false);
        r.toBack();

        errorDialog = createErrorDialog();
        Flyout retVal = new Flyout(r, errorDialog);
        retVal.setFlyoutStyle("-fx-background-color: rgb(0,0,0,0.7);-fx-background-radius: 5 5 5 5;");
        getChildren().add(retVal);
        return retVal;
    }
    
    /**
     * Creates the {@link Flyout} which contains the decorations needed to
     * present a prompt dialog to the user.
     * 
     * @return  a {@code Flyout} used to present user prompts
     */
    private Flyout createPromptNotifier() {
        // Create invisible "anchor" for flyout
        Rectangle r = new Rectangle(0, 0, 5, 1);
        r.setManaged(false);
        r.setVisible(false);
        r.toBack();

        promptDialog = createPromptDialog();
        Flyout retVal = new Flyout(r, promptDialog);
        retVal.setFlyoutStyle("-fx-background-color: rgb(0,0,0,0.7);-fx-background-radius: 5 5 5 5;");
        getChildren().add(retVal);
        return retVal;
    }
    
    /**
     * The radiused styled container which shows the warning controls and information.
     * @return  the decorated Pane showing the warning information.
     */
    private WarnDialog createWarnDialog() {
        WarnDialog dialog = new WarnDialog();
        
        return dialog;
    }

    /**
     * Instructs the Window to show its warning dialog to
     * present the specified message to the user.
     * 
     * @param message   the new warning message.
     */
    public void showWarnDialog(String message) {
        warnDialog.resize(getWidth(), getHeight());
        warnDialog.setPrefSize(getWidth(), getHeight());
        warnDialog.setMessage(message);
        warnDialog.requestLayout();
        Platform.runLater(() -> {
            warnNotifier.flyout();
        });
    }

    /**
     * The radiused styled container which shows the warning controls and information.
     * @return  the decorated Pane showing the warning information.
     */
    private ErrorDialog createErrorDialog() {
        ErrorDialog dialog = new ErrorDialog();

        return dialog;
    }

    /**
     * Instructs the Window to show its error dialog to
     * present the specified message to the user.
     * 
     * @param message   the new error message.
     */
    public void showErrorDialog(String message) {
        errorDialog.resize(getWidth(), getHeight());
        errorDialog.setPrefSize(getWidth(), getHeight());
        errorDialog.setMessage(message);
        errorDialog.requestLayout();
        Platform.runLater(() -> {
            errorNotifier.flyout();
        });
    }
    
    /**
     * The radiused styled container which shows the prompt controls and information.
     * @return  the decorated Pane showing the prompt information.
     */
    private PromptDialog createPromptDialog() {
        PromptDialog dialog = new PromptDialog();
        
        return dialog;
    }

    /**
     * Instructs the Window to show its prompt dialog to
     * present the specified message to the user.
     * 
     * @param message   the new prompt message.
     */
    public void showPromptDialog(String message) {
        promptDialog.resize(getWidth(), getHeight());
        promptDialog.setPrefSize(getWidth(), getHeight());
        promptDialog.setMessage(message);
        promptDialog.requestLayout();
        Platform.runLater(() -> {
            promptNotifier.flyout();
        });
    }
    
    /**
     * Represents the radiused pane centered within the translucent shade 
     * drop-down which contains the error text.
     */
    private class ErrorDialog extends StackPane {
        Label label;
        VBox vBox;
        Button okButton;

        static final double ERROR_BOX_MIN_HEIGHT = 81;

        public ErrorDialog() {
            setPadding(new Insets(30, 30, 30, 30));

            vBox = new VBox(20);
            vBox.setBackground(new Background(new BackgroundFill(Color.rgb(188, 72, 27), new CornerRadii(10), null)));
            vBox.setPadding(new Insets(10, 10, 10, 10));
            vBox.setBorder(
                new Border(
                    new BorderStroke[] { 
                        new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1)) 
                    }
                )
            );
            vBox.setPrefWidth(300);
            vBox.setMaxWidth(300);
            vBox.setMinHeight(ERROR_BOX_MIN_HEIGHT);
            vBox.getStyleClass().add("window-error-dialog");

            label = new Label("");
            label.setWrapText(true);
            label.setTextAlignment(TextAlignment.JUSTIFY);
            label.setTextFill(Color.WHITE);

            okButton = new Button("Ok");
            okButton.setManaged(false);
            okButton.resize(80, 20);
            okButton.setTextFill(Color.WHITE);
            okButton.setFont(Font.font(10f));
            okButton.setFocusTraversable(false);
            okButton.setOnAction(e -> {
                errorNotifier.dismiss();
            });
            Stop[] stops = new Stop[] { new Stop(0, Color.rgb(97, 157, 206)), new Stop(1, Color.rgb(49, 109, 160))};
            LinearGradient lg1 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops);
            okButton.setBackground(new Background(new BackgroundFill(lg1, new CornerRadii(5), null)));
            okButton.setBorder(
                new Border(
                    new BorderStroke[] { 
                        new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1)) 
                    }
                )
            );
            okButton.setOnMouseEntered(e -> {
                Stop[] stops2 = new Stop[] { new Stop(1, Color.rgb(97, 157, 206)), new Stop(0, Color.rgb(49, 109, 160))};
                LinearGradient lg2 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops2);
                okButton.setBackground(new Background(new BackgroundFill(lg2, new CornerRadii(5), null)));
            });
            okButton.setOnMouseExited(e -> {
                Stop[] stops2 = new Stop[] { new Stop(0, Color.rgb(97, 157, 206)), new Stop(1, Color.rgb(49, 109, 160))};
                LinearGradient lg2 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops2);
                okButton.setBackground(new Background(new BackgroundFill(lg2, new CornerRadii(5), null)));
            });

            vBox.getChildren().addAll(label, okButton);

            getChildren().add(vBox);
            setPrefSize(400, 200);
        }
        
        public void setMessage(String message) {
            Text text = new Text(message);
            text.setWrappingWidth(300);
            Bounds b = text.getLayoutBounds();
            label.setText(message);
            label.setPrefWidth(280);
            vBox.setPrefHeight(Math.max(ERROR_BOX_MIN_HEIGHT, b.getHeight() + 50));
            vBox.resize(300, Math.max(ERROR_BOX_MIN_HEIGHT, b.getHeight() + 50));
            vBox.setMaxHeight(Math.max(ERROR_BOX_MIN_HEIGHT, b.getHeight() + 50));
            okButton.relocate(vBox.getWidth() - 80 - 10, vBox.getHeight() - 20 - 10);

            vBox.requestLayout();
        }
    }
    
    /**
     * 汽車
     * Represents the radiused pane centered within the translucent shade 
     * drop-down which contains the warning text.
     */
    private class WarnDialog extends StackPane {
        Label label;
        VBox vBox;
        Button okButton;

        static final double ERROR_BOX_MIN_HEIGHT = 81;

        public WarnDialog() {
            setPadding(new Insets(30, 30, 30, 30));

            vBox = new VBox(20);
            vBox.setBackground(new Background(new BackgroundFill(Color.rgb(92,183,186), new CornerRadii(10), null)));
            vBox.setPadding(new Insets(10, 10, 10, 10));
            vBox.setBorder(
                new Border(
                    new BorderStroke[] { 
                        new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1)) 
                    }
                )
            );
            vBox.setPrefWidth(300);
            vBox.setMaxWidth(300);
            vBox.setMinHeight(ERROR_BOX_MIN_HEIGHT);
            vBox.getStyleClass().add("window-error-dialog");

            label = new Label("");
            label.setWrapText(true);
            label.setTextAlignment(TextAlignment.JUSTIFY);
            label.setTextFill(Color.WHITE);

            okButton = new Button("Ok");
            okButton.setManaged(false);
            okButton.resize(80, 20);
            okButton.setTextFill(Color.WHITE);
            okButton.setFont(Font.font(10f));
            okButton.setFocusTraversable(false);
            okButton.setOnAction(e -> {
                warnNotifier.dismiss();
            });
            Stop[] stops = new Stop[] { new Stop(0, Color.rgb(97, 157, 206)), new Stop(1, Color.rgb(49, 109, 160))};
            LinearGradient lg1 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops);
            okButton.setBackground(new Background(new BackgroundFill(lg1, new CornerRadii(5), null)));
            okButton.setBorder(
                new Border(
                    new BorderStroke[] { 
                        new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1)) 
                    }
                )
            );
            okButton.setOnMouseEntered(e -> {
                Stop[] stops2 = new Stop[] { new Stop(1, Color.rgb(97, 157, 206)), new Stop(0, Color.rgb(49, 109, 160))};
                LinearGradient lg2 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops2);
                okButton.setBackground(new Background(new BackgroundFill(lg2, new CornerRadii(5), null)));
            });
            okButton.setOnMouseExited(e -> {
                Stop[] stops2 = new Stop[] { new Stop(0, Color.rgb(97, 157, 206)), new Stop(1, Color.rgb(49, 109, 160))};
                LinearGradient lg2 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops2);
                okButton.setBackground(new Background(new BackgroundFill(lg2, new CornerRadii(5), null)));
            });

            vBox.getChildren().addAll(label, okButton);

            getChildren().add(vBox);
            setPrefSize(400, 200);
        }
        
        public void setMessage(String message) {
            Text text = new Text(message);
            text.setWrappingWidth(300);
            Bounds b = text.getLayoutBounds();
            label.setText("Warn: " + message);
            label.setPrefWidth(280);
            vBox.setPrefHeight(Math.max(ERROR_BOX_MIN_HEIGHT, b.getHeight() + 50));
            vBox.resize(300, Math.max(ERROR_BOX_MIN_HEIGHT, b.getHeight() + 50));
            vBox.setMaxHeight(Math.max(ERROR_BOX_MIN_HEIGHT, b.getHeight() + 50));
            okButton.relocate(vBox.getWidth() - 80 - 10, vBox.getHeight() - 20 - 10);

            vBox.requestLayout();
        }
    }
    
    /**
     * 汽車
     * Represents the radiused pane centered within the translucent shade 
     * drop-down which contains the Prompt text.
     */
    private class PromptDialog extends StackPane {
        Label label;
        VBox vBox;
        HBox buttonBox;
        Button okButton;
        Button cancelButton;

        static final double ERROR_BOX_MIN_HEIGHT = 81;

        public PromptDialog() {
            setPadding(new Insets(30, 30, 30, 30));

            vBox = new VBox(20);
            vBox.setBackground(new Background(new BackgroundFill(Color.rgb(70, 140, 199), new CornerRadii(10), null)));
            vBox.setPadding(new Insets(10, 10, 10, 10));
            vBox.setBorder(
                new Border(
                    new BorderStroke[] { 
                        new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(1)) 
                    }
                )
            );
            vBox.setPrefWidth(300);
            vBox.setMaxWidth(300);
            vBox.setMinHeight(ERROR_BOX_MIN_HEIGHT);
            vBox.getStyleClass().add("window-error-dialog");

            label = new Label("");
            label.setWrapText(true);
            label.setTextAlignment(TextAlignment.JUSTIFY);
            label.setTextFill(Color.WHITE);
            
            buttonBox = new HBox(5);
            buttonBox.setManaged(false);
            buttonBox.resize(165, 20);

            okButton = new Button("Ok");
            okButton.resize(80, 20);
            okButton.setPrefSize(80, 20);
            okButton.setTextFill(Color.WHITE);
            okButton.setFont(Font.font(10f));
            okButton.setFocusTraversable(false);
            okButton.setOnAction(e -> {
                promptNotifier.dismiss();
            });
            Stop[] stops = new Stop[] { new Stop(0, Color.rgb(97, 157, 206)), new Stop(1, Color.rgb(49, 109, 160))};
            LinearGradient lg1 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops);
            okButton.setBackground(new Background(new BackgroundFill(lg1, new CornerRadii(5), null)));
            okButton.setBorder(
                new Border(
                    new BorderStroke[] { 
                        new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1)) 
                    }
                )
            );
            okButton.setOnMouseEntered(e -> {
                Stop[] stops2 = new Stop[] { new Stop(1, Color.rgb(97, 157, 206)), new Stop(0, Color.rgb(49, 109, 160))};
                LinearGradient lg2 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops2);
                okButton.setBackground(new Background(new BackgroundFill(lg2, new CornerRadii(5), null)));
            });
            okButton.setOnMouseExited(e -> {
                Stop[] stops2 = new Stop[] { new Stop(0, Color.rgb(97, 157, 206)), new Stop(1, Color.rgb(49, 109, 160))};
                LinearGradient lg2 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops2);
                okButton.setBackground(new Background(new BackgroundFill(lg2, new CornerRadii(5), null)));
            });
            
            cancelButton = new Button("Cancel");
            cancelButton.setFocusTraversable(false);
            cancelButton.resize(80, 20);
            cancelButton.setPrefSize(80, 20);
            cancelButton.setTextFill(Color.WHITE);
            cancelButton.setFont(Font.font(10f));
            cancelButton.setOnAction(e -> {
                promptNotifier.dismiss();
            });
            Stop[] stops3 = new Stop[] { new Stop(0, Color.rgb(97, 157, 206)), new Stop(1, Color.rgb(49, 109, 160))};
            LinearGradient lg3 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops3);
            cancelButton.setBackground(new Background(new BackgroundFill(lg3, new CornerRadii(5), null)));
            cancelButton.setBorder(
                new Border(
                    new BorderStroke[] { 
                        new BorderStroke(Color.WHITE, BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1)) 
                    }
                )
            );
            cancelButton.setOnMouseEntered(e -> {
                Stop[] stops4 = new Stop[] { new Stop(1, Color.rgb(97, 157, 206)), new Stop(0, Color.rgb(49, 109, 160))};
                LinearGradient lg4 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops4);
                cancelButton.setBackground(new Background(new BackgroundFill(lg4, new CornerRadii(5), null)));
            });
            cancelButton.setOnMouseExited(e -> {
                Stop[] stops4 = new Stop[] { new Stop(0, Color.rgb(97, 157, 206)), new Stop(1, Color.rgb(49, 109, 160))};
                LinearGradient lg4 = new LinearGradient(0, 0, 0, 20, false, CycleMethod.NO_CYCLE, stops4);
                cancelButton.setBackground(new Background(new BackgroundFill(lg4, new CornerRadii(5), null)));
            });
            
            buttonBox.getChildren().addAll(okButton, cancelButton);

            vBox.getChildren().addAll(label, buttonBox);

            getChildren().add(vBox);
            setPrefSize(400, 200);
        }
        
        public void setMessage(String message) {
            Text text = new Text(message);
            text.setWrappingWidth(300);
            Bounds b = text.getLayoutBounds();
            label.setText("Warn: " + message);
            label.setPrefWidth(280);
            vBox.setPrefHeight(Math.max(ERROR_BOX_MIN_HEIGHT, b.getHeight() + 50));
            vBox.resize(300, Math.max(ERROR_BOX_MIN_HEIGHT, b.getHeight() + 50));
            vBox.setMaxHeight(Math.max(ERROR_BOX_MIN_HEIGHT, b.getHeight() + 50));
            buttonBox.relocate(vBox.getWidth() - 165 - 10, vBox.getHeight() - 20 - 10);

            vBox.requestLayout();
        }
    }

    /**
     * DO NOT USE!
     * 
     * Private constructor for creating Windows for testing
     * @param i
     */
    private Window(int i) { windowID = UUID.randomUUID(); }

    /**
     * Returns a bare Window object which can be used to test Window grouping
     * logic without the need for creating visual elements.
     * @return  a raw Window object to be only used for test purposes.
     */
    public static Window windowForTest() {
        return new Window(0) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                this.windowID = UUID.randomUUID();
            }
            
            @Override
            public void releaseResourcesForWindowClose() {}

            @Override
            public Region getRegion() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Parent getBackContent() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Parent getFrontContent() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean isInput() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public StatusBar getStatusBar() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Thumb getThumb() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public TitleBar getTitleBar() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Type getType() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Dimension2D computeMinSize() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    public abstract Parent getBackContent();

    public abstract Parent getFrontContent();

    /**
     * Rotates this window 180deg about the Y-Axis
     */
    public void flip() {
        animateFlip();
    }

    /**
     * Called after {@link #flip()} to show the back side
     * contents.
     */
    public void showBack() {
        isBackShowing = true;
    }

    /**
     * Called after {@link #flip()} to show the front side
     * contents.
     */
    public void showFront() {
        isBackShowing = false;
    }

    /**
     * Returns a flag indicating whether the front or back
     * of this window is currently showing.
     * @return
     */
    public boolean isBackShowing() {
        return isBackShowing;
    }

    /**
     * Sets the id of this {@code Window}
     * @param id
     */
    public void setWindowID(UUID id) {
        this.windowID = id;
    }

    /**
     * Returns the id of this {@code Window}
     * @return
     */
    public UUID getWindowID() {
        return windowID;
    }

    /**
     * Returns the property which stores the flag indicating whether
     * this {@code Window} is selected or not.
     * 
     * @return  this Window's selected property
     */
    public BooleanProperty selectedProperty() {
        return selectedProperty;
    }

    /**
     * Returns the property set when this Window is destroyed
     * @return
     */
    public BooleanProperty destroyProperty() {
        return destroyProperty;
    }

    /**
     * Returns a flag indicating whether this {@code WindowContext} 
     * refers to an {@link InputWindow} or {@link OutputWindow}.
     * 
     * @return
     */
    @Override
    public abstract boolean isInput();

    /**
     * Returns a property set with changes to a given window's output.
     * @return
     */
    public ReadOnlyObjectProperty<String> messageProperty() {
        return messageProperty.getReadOnlyProperty();
    }

    /**
     * Convenience method to resize the target window by computing its 
     * minimum height. 
     */
    public void resizeWindow() {
        Platform.runLater(() -> {
            Dimension2D dims = computeMinSize();

            resize(getWidth(), dims.getHeight());
            requestLayout();
        });
    }

    /**
     * Proxies to the {@link Node#resize(double, double)} method after 
     * performing any local state handling necessary.
     * 
     * @param w     the width to resize to.
     * @param h     the height to resize to.
     */
    public void resizeWindow(double w, double h) {
        resize(w, h);
    }

    /**
     * Returns this {@code Window}'s {@link StatusBar}
     * @return
     */
    public abstract StatusBar getStatusBar();

    /**
     * Returns the {@link Thumb} used to drag-resize this 
     * window.
     * 
     * @return
     */
    public abstract Thumb getThumb();

    /**
     * Returns this {@code Window}'s {@link TitleBar}
     * @return  this {@code Window}'s {@link TitleBar}
     */
    public abstract TitleBar getTitleBar();

    /** 
     * Returns this {code Window}'s type
     * @return
     */
    public abstract Type getType();

    /**
     * Called by the {@link TitleBar} to set the title in this Window
     * upon changes in its title field
     * @return
     */
    public Text getTitleField() {
        if(title == null) {
            title = new Text();
        }
        return title;
    }

    /**
     * Returns the user specified window title or a location string.
     * @return
     */
    public String getTitle() {
        return (title == null || title.getText().isEmpty()) ? 
                "[x=" + String.format("%.0f", getLayoutX()) + ",y=" + String.format("%.0f", getLayoutY()) + "] " :
                    title.getText();
    }

    /**
     * Computes and returns the minimum size of this window as a function
     * of the min size required by the different parts of its contents
     * which are:
     * <ol>
     * <li>{@link TitleBar}</li>
     * <li>{@link InputBar}</li>
     * <li>{@link InputViewArea}</li>
     * <li>{@link StatusBar}</li>
     * </ol>
     * 
     * This calculation is used for drag-resizing and dynamic
     * auto-expansion/contraction of widgets during user entry.
     *  
     * @return  the computed minimum size.
     */
    public abstract Dimension2D computeMinSize();

    /**
     * Stores this {@code Window}'s {@link WindowController}
     * @param controller
     */
    public void setController(WindowController controller) {
        this.controller = controller;
    }

    /**
     * Returns this {@code Window}'s {@link WindowController}
     * @return
     */
    public WindowController getController() {
        return controller;
    }

    /**
     * Returns the control widget for this {@code Window}
     * @return
     */
    public WindowControl getWindowControl() {
        return titleBar.getWindowControl();
    }

    /**
     * Convenience method to programmatically drag a {@link Window}'s {@link Thumb}
     * @param xAmount       the amount to drag the Thumb in the x direction
     * @param yAmount       the amount to drag the Thumb in the y direction
     */
    public void dragThumb(double xAmount, double yAmount) {
        Point2D p = new Point2D(getThumb().getLayoutX(), getThumb().getLayoutY());
        getThumb().dragThumb(p, p.getX() + xAmount, p.getY() + yAmount);
    }

    /**
     * Swaps the window for the representative {@link WindowIcon} and
     * invokes the transitions necessary.
     */
    ChangeListener<Bounds> tempIconizeListener;
    public void iconize() {
        windowIcon = new WindowIcon(this);

        windowIcon.setNormalState();

        WindowPane windowPane = (WindowPane)getParent();
        windowIcon.relocate(getLayoutX(), getLayoutY());
        windowPane.getChildren().add(windowIcon);
        setVisible(false);

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage img = windowIcon.snapshot(sp, null);
        ImageView ghost = new ImageView(img);
        ghost.setManaged(false);
        ghost.setLayoutX(windowIcon.getLayoutX());
        ghost.setLayoutY(windowIcon.getLayoutY());

        windowPane.getChildren().remove(windowIcon);
        windowPane.getChildren().add(ghost);

        windowIcon.setManaged(true);
        windowPane.getIconArea().getChildren().add(windowIcon);

        windowIcon.layoutBoundsProperty().addListener(tempIconizeListener = (v,o,n) -> {
            Platform.runLater(() -> {
                animateIcon(ghost, windowIcon);

                // Pre-setup the last step in the deIconize process.
                if(visibilityListener == null) {
                    visibilityListener = (v2,o2,n2) -> {
                        if(n2) {
                            windowPane.getIconArea().getChildren().remove(windowIcon);
                        }
                    };
                    visibleProperty().addListener(visibilityListener);
                }

                windowIcon.layoutBoundsProperty().removeListener(tempIconizeListener);
            });
        });

        windowPane.getIconArea().requestLayout();
    }

    /**
     * Called to begin the setup necessary to establish icon state
     * then calls {@link #animateIconUp(ImageView, Node)} to continue the
     * de-iconization process.
     */
    public void deIconize() {
        if(windowIcon == null) {
            return;
        }

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage img = windowIcon.snapshot(sp, null);
        ImageView ghost = new ImageView(img);
        ghost.setManaged(false);
        WindowPane windowPane = (WindowPane)getParent();
        double x1 = windowIcon.layoutXProperty().get();
        double x2 = windowPane.getIconArea().localToParent(windowIcon.getBoundsInLocal()).getMinY();
        ghost.setLayoutX(x1);
        ghost.setLayoutY(x2);

        windowPane.getChildren().add(ghost);
        windowPane.getIconArea().getChildren().remove(windowIcon);

        animateIconUp(ghost, this);
    }

    /**
     * Stores the specified {@link BackPanelSupplier}
     * @param supplier
     */
    public void setBackPanelSupplier(BackPanelSupplier supplier) {
        this.backPanelSupplier = supplier;
    }

    /**
     * Installs the panel used to display the info UI following a 
     * flip animation.
     * @param node
     */
    public void setBackPanel(Region node) {
        if(backContent.getChildren().size() > 1) {
            backContent.getChildren().remove(1);
        }

        if(backContentListener != null) {
            backContent.layoutBoundsProperty().removeListener(backContentListener);
        }

        node.setManaged(false);
        backContent.layoutBoundsProperty().addListener(backContentListener = (v,o,n) -> {
            node.resize(n.getWidth() - 10, n.getHeight() - 45);
            node.relocate(Math.max(5, (n.getWidth() / 2) - (node.getWidth() / 2)),
                    Math.max(40, (n.getHeight() / 2) - (node.getHeight() / 2)));
        });

        backContent.getChildren().add(node);
        backContent.requestLayout();
    }
    
    /**
     * Animates the movement of an "iconized" window to the bottom
     * icon area.
     * @param icon      the icon to animate
     * @param target    the node within which the icon is being animated
     */
    private void animateIcon(ImageView icon, Node target) {
        if(tl.getStatus() == Animation.Status.RUNNING) return;

        DoubleProperty x = new SimpleDoubleProperty(icon.getLayoutX());
        DoubleProperty y = new SimpleDoubleProperty(icon.getLayoutY());
        x.addListener((v,o,n) -> icon.relocate(n.doubleValue(), y.doubleValue()));
        y.addListener((v,o,n) -> icon.relocate(x.doubleValue(), n.doubleValue()));

        Point2D p = ((WindowPane)getParent()).getIconArea().localToParent(target.layoutXProperty().get(), target.layoutYProperty().get());            

        double end1 = p.getX();
        double end2 = p.getY();

        KeyValue keyValue = new KeyValue(x, end1, interpolator);
        KeyValue keyValue2 = new KeyValue(y, end2, interpolator);
        // create a keyFrame with duration 500ms
        KeyFrame keyFrame = new KeyFrame(Duration.millis(750), keyValue, keyValue2);
        // erase last keyframes: forward & reverse have different frames
        tl.getKeyFrames().clear();
        // add the keyframe to the timeline
        tl.getKeyFrames().add(keyFrame);
        // remove binding above after animation is finished
        tl.setOnFinished((e) -> {
            Platform.runLater(() -> {
                icon.setVisible(false);
            });
        });

        tl.play();
    }

    /**
     * Animates the movement of an "iconized" window to its previous
     * full view location.
     * @param icon      the icon to animate
     * @param target    the node within which the icon is being animated
     */
    private void animateIconUp(ImageView icon, Node target) {
        if(tl.getStatus() == Animation.Status.RUNNING) return;

        DoubleProperty x = new SimpleDoubleProperty(icon.getLayoutX());
        DoubleProperty y = new SimpleDoubleProperty(icon.getLayoutY());
        x.addListener((v,o,n) -> icon.relocate(n.doubleValue(), y.doubleValue()));
        y.addListener((v,o,n) -> icon.relocate(x.doubleValue(), n.doubleValue()));

        double end1 = target.getLayoutX();
        double end2 = target.getLayoutY();

        KeyValue keyValue = new KeyValue(x, end1, interpolator);
        KeyValue keyValue2 = new KeyValue(y, end2, interpolator);
        // create a keyFrame with duration 500ms
        KeyFrame keyFrame = new KeyFrame(Duration.millis(750), keyValue, keyValue2);
        // erase last keyframes: forward & reverse have different frames
        tl.getKeyFrames().clear();
        // add the keyframe to the timeline
        tl.getKeyFrames().add(keyFrame);
        // remove binding above after animation is finished
        tl.setOnFinished((e) -> {
            Platform.runLater(() -> {
                target.setVisible(true);
                icon.setVisible(false);
                ((WindowPane)getParent()).getChildren().remove(icon);
            });
        });

        tl.play();
    }

    /**
     * Animates the flipping of a window to reveal its back side.
     */
    private void animateFlip() {
        Parent incoming = isBackShowing() ? getFrontContent() : getBackContent();
        Parent outgoing = isBackShowing() ? getBackContent() : getFrontContent();
        this.setRotationAxis(Rotate.Y_AXIS);

        DoubleProperty yIn = new SimpleDoubleProperty(0);
        DoubleProperty yOut = new SimpleDoubleProperty(1.0);
        DoubleProperty win = new SimpleDoubleProperty(rotateProperty().get());

        yIn.addListener((v,o,n) -> incoming.opacityProperty().set(n.doubleValue())); 
        yOut.addListener((v,o,n) -> outgoing.opacityProperty().set(n.doubleValue()));
        win.addListener((v,o,n) -> this.rotateProperty().set(n.doubleValue()));

        double midIn = 0;
        double midOut = 90;
        double endOut = 180;

        KeyValue keyValue = new KeyValue(yIn, 0.5, Interpolator.EASE_IN);
        KeyValue keyValue2 = new KeyValue(yOut, 0.5, Interpolator.EASE_IN);
        KeyValue keyValue3 = new KeyValue(win, midOut, Interpolator.EASE_IN);
        // create a keyFrame with duration 500ms
        KeyFrame keyFrame = new KeyFrame(Duration.millis(375), keyValue, keyValue2, keyValue3);
        // erase last keyframes: forward & reverse have different frames
        tl.getKeyFrames().clear();
        // add the keyframe to the timeline
        tl.getKeyFrames().add(keyFrame);
        // remove binding above after animation is finished
        tl.setOnFinished((e) -> {
            Platform.runLater(() -> {
                if(isBackShowing()) {
                    showFront();
                }else{
                    showBack();
                }

                tl.setOnFinished(null);

                KeyValue endValue1 = new KeyValue(yIn, 1.0, Interpolator.EASE_OUT);
                KeyValue endValue2 = new KeyValue(yOut, isBackShowing() ? 0 : 0.1, Interpolator.EASE_OUT);
                KeyValue endValue3 = new KeyValue(win, isBackShowing() ? midIn : endOut, Interpolator.EASE_OUT);
                // create a keyFrame with duration 500ms
                KeyFrame endFrame = new KeyFrame(Duration.millis(375), endValue1, endValue2, endValue3);
                // erase last keyframes: forward & reverse have different frames
                tl.getKeyFrames().clear();
                // add the keyframe to the timeline
                tl.getKeyFrames().add(endFrame);
                tl.setOnFinished((x) -> {
                    isBackShowing = !isBackShowing;
                });
                tl.play();
            });
            // slight resize after half of flip ensures resize listeners get called
            // to make back/front side visible.
            getController().nudgeLayout(this, 1);
        });

        tl.play();
    }

    /**
     * Dismiss the InputSelector flyout when another window is selected
     */
    public void dismissInputSelector() {
        getTitleBar().getInputSelector().reset();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((windowID == null) ? 0 : windowID.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        Window other = (Window)obj;
        if(windowID == null) {
            if(other.windowID != null)
                return false;
        } else if(!windowID.equals(other.windowID))
            return false;
        return true;
    }

}
