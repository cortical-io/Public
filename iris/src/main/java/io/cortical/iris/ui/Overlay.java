package io.cortical.iris.ui;

import static io.cortical.iris.message.BusEvent.OVERLAY_DISMISS_MODAL_DIALOG;
import static io.cortical.iris.message.BusEvent.OVERLAY_SHOW_MODAL_DIALOG;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fxpresso.tidbit.ui.Flyout;
import io.cortical.iris.ApplicationService;
import io.cortical.iris.ExtendedClient;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.EventBus;
import io.cortical.retina.client.FullClient;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import rx.Observable;
import rx.Subscriber;

/**
 * Partially opaque node which sits atop of the application to prevent
 * interaction with it while forcing the user to interact with specific
 * widgets in a modal like dialog treatment. 
 * 
 * This Overlay is dual purpose. It acts as a barrier to entry, forcing
 * API key entry; and it also acts as a helper to show modal style dialogs
 * preventing app use before acting on dialog contents.
 * 
 * <b>NOTE:</b> This is a example of how to show a modal dialog over the entire
 * application.
 * <pre>
 * TO CREATE THE DIALOG (SHOWING OK AND CANCEL BUTTONS):
 * 
 * private Pane createInputToggleDialog() {
 *     StackPane dialog = new StackPane();
 *     dialog.getStyleClass().add("input-toggle-dialog");
 *     dialog.resize(405, 100);
 *     dialog.setPrefSize(405, 100);
 *      
 *     Label l = new Label("Changing tabs will clear results in the output window.");
 *     l.setFont(Font.font("Questrial-Regular", 16));
 *     l.setTextFill(Color.WHITE);
 *     l.setManaged(false);
 *     l.resizeRelocate(15, 5, 380, 50);
 *     Label l2 = new Label("Is this ok?");
 *     l2.setFont(Font.font("Questrial-Regular", 14));
 *     l2.setTextFill(Color.WHITE);
 *     l2.setManaged(false);
 *     l2.resizeRelocate(15, 35, 360, 50);
 *     Button ok = new Button("Ok");
 *     ok.getStyleClass().addAll("input-toggle-dialog");
 *     ok.setPrefSize(60,  25);
 *     ok.setOnAction(e -> {
 *         EventBus.get().broadcast(OVERLAY_DISMISS_MODAL_DIALOG.subj(), null);
 *         Toggle t = toggleGroup.getSelectedToggle();
 *         selectedToggle = t;
 *         if(t instanceof ToggleButton) {
 *             selectedToggleProperty.set(((ToggleButton)selectedToggle).getText());
 *             toggleGroup.selectToggle(selectedToggle);
 *         }else{
 *             selectedToggleProperty.set(((TogglePane)t).getText());
 *             toggleGroup.selectToggle(selectedToggle);
 *         }
 *     });
 *     Button cancel = new Button("Cancel");
 *     cancel.getStyleClass().addAll("input-toggle-dialog");
 *     cancel.setPrefSize(70, 25);
 *     cancel.setOnAction(e -> {
 *         if(selectedToggle instanceof ToggleButton) {
 *             selectedToggleProperty.set(((ToggleButton)selectedToggle).getText());
 *             toggleGroup.selectToggle(selectedToggle);
 *         }else{
 *             selectedToggleProperty.set(((TogglePane)selectedToggle).getText());
 *             toggleGroup.selectToggle(selectedToggle);
 *         }
 *         EventBus.get().broadcast(OVERLAY_DISMISS_MODAL_DIALOG.subj(), null);
 *     });
 *     HBox hBox = new HBox(ok, cancel);
 *     hBox.resize(155, 30);
 *     hBox.setSpacing(15);
 *     hBox.setManaged(false);
 *     hBox.relocate(250, 63);
 *     dialog.getChildren().addAll(l, l2, hBox);
 *     
 *     return dialog;
 * }
 * </pre>
 * <pre>
 * TO SHOW IT:
 * 
 * public void showTabChangeWarning(Pane dialog) {
 *     Payload p = new Payload(dialog);
 *     EventBus.get().broadcast(OVERLAY_SHOW_MODAL_DIALOG.subj(), p);
 * }
 * </pre> 
 * 
 * @author cogmission
 *
 */
public class Overlay extends Pane {
    private static final Logger LOG = LoggerFactory.getLogger(Overlay.class);
    
    private Label response;
    private Label instr;
    private Flyout enterApiFlyout;
    private VBox apiKeyPane;
    private Hyperlink get;
    private Hyperlink enter;
    private TextField apiTextField;
    private boolean apiEntered;
    private Pane currentDialog;
    private ProgressIndicator progress;
    
    private ContentPane contentPane;
    
    private StackPane closeButton;
     
    
    /**
     * Constructs a new instance of the {@code Overlay}
     */
    public Overlay() {
        setVisible(false);
        setStyle("-fx-background-color: transparent;");
        contentPane = WindowService.getInstance().getContentPane();
        prefWidthProperty().bind(contentPane.getLogoBackground().widthProperty());
        prefHeightProperty().bind(contentPane.getLogoBackground().heightProperty());
        
        addEventHandler(MouseEvent.ANY, e -> {
            if(apiKeyPane.getBoundsInLocal().contains(e.getX(), e.getY())) {
                Bounds b1 = enter.localToScene(enter.getBoundsInLocal());
                Bounds b2 = get.localToScene(get.getBoundsInLocal());
                if(b1.contains(e.getX(), e.getY())) {
                    if(e.getEventType().equals(MouseEvent.MOUSE_CLICKED)) {
                        enter.fire();
                    }
                    getScene().setCursor(Cursor.CLOSED_HAND);
                }else if(b2.contains(e.getX(), e.getY())) {
                    if(e.getEventType().equals(MouseEvent.MOUSE_CLICKED)) {
                        get.fire();
                    }
                    getScene().setCursor(Cursor.CLOSED_HAND);
                }else{
                    getScene().setCursor(Cursor.DEFAULT);
                }
            }
        });
        
        instr = new Label("\t\t    Click \"Enter\" to add your API Key \n " +
            "\t\t\t\t\t   --or--\nClick \"Get\" to get a new API Key (opens system browser)");
        instr.setTextFill(Color.WHITE);
        instr.setFont(Font.font(16));
        instr.setManaged(false);
        instr.setFocusTraversable(false);
        getChildren().add(instr);
        
        response = new Label("Test");
        response.setWrapText(true);
        response.setTextFill(Color.WHITE);
        response.setVisible(false);
        response.setFont(Font.font(20));
        response.setManaged(false);
        response.setFocusTraversable(false);
        getChildren().add(response);
        
        progress = new ProgressIndicator();
        progress.resize(125, 125);
        progress.setManaged(false);
        progress.setVisible(false);
        progress.setFocusTraversable(false);
        getChildren().add(progress);
        
        apiKeyPane = (VBox)createApiKeyControlPane();
        apiKeyPane.setManaged(false);
        getChildren().add(apiKeyPane);
        
        createEnterAPIFlyout();
        
        // This Overlay is dual purpose. It acts as a barrier to entry, forcing
        // API key entry; and it also acts as a helper to show modal style dialogs
        // preventing app use before acting on dialog contents. The method below
        // adds the necessary handlers which listen for the conditions under which
        // the dialog is shown.
        addDialogServiceToEventBus();
        
        visibleProperty().addListener((v,o,n) -> {
            if(n && !apiEntered) {
                redrawOverlayContent();
            }
        });
        
        getChildren().add(closeButton = createCloseButton());
        
        // Do the final application entry logic
        checkApiKey();
    }
    
    public StackPane createCloseButton() {
        StackPane sp = new StackPane();
        sp.setManaged(false);
        sp.resize(100, 120);
        layoutBoundsProperty().addListener((v,o,n) -> {
            sp.relocate(n.getWidth() - 100, 0);
        });
        
        Rectangle r = new Rectangle(30, 30);
        r.setFill(Color.TRANSPARENT);
        r.setManaged(false);
        r.setStroke(Color.GRAY);
        r.relocate(35, 35);
        
        Line l1 = new Line(35, 35, 65, 65);
        l1.setManaged(false);
        l1.setStroke(Color.GRAY);
        Line l2 = new Line(35, 65, 65, 35);
        l2.setStroke(Color.GRAY);
        l2.setManaged(false);
        
        Label closeLabel = new Label("Close");
        closeLabel.setVisible(false);
        closeLabel.setFont(Font.font(14));
        closeLabel.setTextFill(Color.WHITE);
        closeLabel.setManaged(false);
        Text helper = new Text("Close");
        helper.fontProperty().bind(closeLabel.fontProperty());
        double w = helper.getBoundsInLocal().getWidth();
        double h = helper.getBoundsInLocal().getHeight();
        closeLabel.resizeRelocate(50 - (w / 2), 65 + h + 5, w, h);
        
        sp.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { 
            closeLabel.setVisible(true);
            r.setStroke(Color.WHITE);
            l1.setStroke(Color.WHITE);
            l2.setStroke(Color.WHITE);
            sp.setCursor(Cursor.HAND);
        } );
        sp.addEventHandler(MouseEvent.MOUSE_EXITED, e -> { 
            closeLabel.setVisible(false); 
            r.setStroke(Color.GRAY);
            l1.setStroke(Color.GRAY);
            l2.setStroke(Color.GRAY);
            sp.setCursor(Cursor.DEFAULT);
        });
        
        sp.setOnMouseClicked(e -> dismissDialog());
        
        sp.getChildren().addAll(r, l1, l2, closeLabel);
        
        return sp;
    }
    
    /**
     * Returns the dialog set to be shown.
     * @return
     */
    public Pane getDialog() {
        return currentDialog;
    }
    
    /**
     * Adds a Pane to this {@code Overlay} which acts as a modal dialog
     * @param n
     */
    public void showDialog(Pane n) {
        clearDialog();
        n.setManaged(false);
        n.relocate((getWidth() / 2.0) - (n.getWidth() / 2.0), (getHeight() / 2.0) - (n.getHeight() * 4.0 / 5.0));
        getChildren().add(currentDialog = n);
        setVisible(true);
        redrawOverlayContent();
    }
    
    /**
     * Hides this {@code Overlay} and removes the current dialog (pane) 
     * child from the list of children.
     */
    public void dismissDialog() {
        clearDialog();
        setVisible(false);
    }
    
    /**
     * Removes the previously added modal dialog pane from 
     * this {@code Overlay}
     */
    public void clearDialog() {
        if(currentDialog != null) {
            getChildren().remove(currentDialog);
        }
    }
    
    /**
     * Removes the nodes displaying api key information
     * and interactive fields.
     */
    public void clearApiMessage() {
        getChildren().removeAll(instr, response, apiKeyPane);
    }
    
    /**
     * Sets the api response text displayed when there is a 
     * successful or unsuccessful api key entry.
     * @param text
     */
    public void setApiResponse(String text) {
        response.setText(text);
    }
    
    /**
     * Sets the node displaying the api key response from the 
     * server, either visible or invisible.
     * 
     * @param b
     */
    public void setApiResponseVisible(boolean b) {
        response.setVisible(b);
    }
    
    /**
     * Sets the "fill color" of the api key response. Unsuccessful
     * responses are filled in red.
     * 
     * @param c
     */
    public void setApiResponseFill(Color c) {
        response.setTextFill(c);
    }
    
    /**
     * Attempts loading of the api key from cache and executes all successful
     * and unsuccessful downstream logic such as api key retrieval from the 
     * company website or api key entry and validation feedback.
     */
    private void checkApiKey() {
        String key = null;
        if((key = ApplicationService.getInstance().loadApiKey()) != null) {
            apiTextField.setText(key);
            apiTextField.selectAll();
            try {
                submitApiKey(key);
                attachResizeListener();
            }catch(Exception e) {
                Platform.runLater(() -> {
                    setVisible(true);
                    attachResizeListener();
                    positionAndSizeKeyPane();
                });
            }
        }else{
            Platform.runLater(() -> {
                setVisible(true);
                attachResizeListener();
                positionAndSizeKeyPane();
            });
        }
    }
    
    /**
     * Called during window resizes to simultaneously resize this Overlay
     * making sure it is kept in sink with the window it covers.
     */
    private void positionAndSizeKeyPane() {
        apiKeyPane.resize(200, 35);
        apiKeyPane.relocate((getLayoutBounds().getWidth() / 2) - (apiKeyPane.getLayoutBounds().getWidth() / 2),
            (getLayoutBounds().getHeight() / 2) - (apiKeyPane.getLayoutBounds().getHeight() / 2));
    }
    
    /**
     * The 2-link orange "Enter" or "Get" button.
     * 
     * @return
     */
    private Pane createApiKeyControlPane() {
        apiKeyPane = new VBox();
        apiKeyPane.setFocusTraversable(false);
        apiKeyPane.addEventHandler(MouseEvent.MOUSE_EXITED, e -> { getScene().setCursor(Cursor.DEFAULT); });
        apiKeyPane.getStyleClass().add("control-pane-api-button");
        
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("control-pane-api-textflow");
        
        enter = new Hyperlink("Enter");
        enter.getStyleClass().add("overlay-hyperlink");
        enter.setFocusTraversable(false);
        enter.setOnAction(e -> {
            String key = null;
            if((key = ApplicationService.getInstance().loadApiKey()) != null) {
                apiTextField.setText(key);
                apiTextField.selectAll();
            }
            showEnterAPIFlyout(!enterApiFlyout.flyoutShowing());
            ((Hyperlink)e.getSource()).setVisited(false);
        });
        
        Text or = new Text("or");
        or.setFill(Color.WHITE);
        
        get = new Hyperlink("Get");
        get.getStyleClass().add("overlay-hyperlink");
        get.setFocusTraversable(false);
        get.setOnAction(e -> {
            ApplicationService.getInstance().getHostServices().showDocument("http://www.cortical.io/resources_apikey.html");
            ((Hyperlink)e.getSource()).setVisited(false);
        });
        
        Text api = new Text("API Key");
        api.setFill(Color.WHITE);
        
        flow.setManaged(false);
        flow.getChildren().addAll(enter, or, get, api);
        flow.relocate(5, 2);
        
        apiKeyPane.getChildren().add(flow);
        
        return apiKeyPane;
    }
    
    /**
     * Returns the {@link Flyout} widget which displays the text
     * field used to enter an API key.
     * 
     * @return
     */
    public Flyout createEnterAPIFlyout() {
        VBox vb = new VBox(5);
        
        HBox line = new HBox();
        line.setPrefWidth(310);
        
        Label l = new Label("Enter your API key and press ENTER");
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font(14));
        
        Label l2 = new Label("             [x]");
        l2.setAlignment(Pos.BASELINE_RIGHT);
        l2.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            l2.setTextFill(Color.rgb(237, 93, 37));
        });
        l2.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            l2.setTextFill(Color.WHITE);
        });
        l2.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            showEnterAPIFlyout(false);
            l2.setTextFill(Color.WHITE);
        });
        l2.setTextFill(Color.WHITE);
        l2.setFont(Font.font(14));
        
        line.getChildren().addAll(l, l2);
        
        apiTextField = new TextField();
        apiTextField.setOnAction(getApiTextFieldHandler());
        apiTextField.setPrefWidth(300);
        vb.setPadding(new Insets(5, 5, 5, 5));
        vb.getChildren().addAll(line, apiTextField);
        
        enterApiFlyout = new Flyout(apiKeyPane, vb);
        enterApiFlyout.setFlyoutStyle("-fx-background-color: rgb(49, 109, 160, 0.7);-fx-background-radius: 0 0 5 5;");
        return enterApiFlyout;
    }
    
    /**
     * Returns the action handler for the "Enter" key press when the 
     * api text field has focus. The handler executes the api key
     * submission logic.
     *  
     * @return  the handler for the api key textfield action
     */
    private EventHandler<ActionEvent> getApiTextFieldHandler() {
        return e -> {
            try {
                showProgressIndicator();
                
                String apikey = apiTextField.getText().trim();
                // Validate the api key and execute dependent logic.
                submitApiKey(apikey);
            }catch(Exception ex) {
                return;
            }
        };
    }
    
    /**
     * Sends the specified api key String to the server for validation and executes
     * dependent logic for either a successful validation or a failure.
     * 
     * @param apikey
     * @throws Exception
     * @see {@link #getFullClientObservable(String)}
     * @see {@link #createAndSetExtendedClient(String)}
     * @see {@link #getRetinaSubscriber()}
     */
    private void submitApiKey(String apikey) throws Exception {
        Observable<ReadOnlyObjectProperty<FullClient>> retinaObs = 
            getFullClientObservable(apikey);
        
        createAndSetExtendedClient(apikey);
                    
        retinaObs.subscribe(getRetinaSubscriber());
    }
    
    /**
     * Returns the Observable which can be used to obtain a {@link FullClient}.
     * @return  an Observable used to obtain a {@link FullClient}
     * @throws Exception    if the api key specified is bad, or there is a connection problem.
     */
    private Observable<ReadOnlyObjectProperty<FullClient>> getFullClientObservable(String apikey) throws Exception {
        return ApplicationService.getInstance().createRetinaClientProperty(
            Optional.of(apiTextField.getText().trim()));
    }
    
    /**
     * Client creation includes server verification which can fail. This method 
     * handles the failure in a controlled fashion.
     * @param apikey
     */
    private void createAndSetExtendedClient(String apikey) {
        try {
            Observable<ReadOnlyObjectProperty<ExtendedClient>> extendedObs = 
                ApplicationService.getInstance().createExtendedClientProperty(
                    Optional.of(apikey));
            
            extendedObs.subscribe(new Subscriber<ReadOnlyObjectProperty<ExtendedClient>>() {
                @Override public void onCompleted() {}
                @Override public void onError(Throwable e) {
                    //Log unsuccessful extended client creation
                    e.printStackTrace();
                }
                @Override public void onNext(ReadOnlyObjectProperty<ExtendedClient> v) {
                    //Log successful extended client creation
                    LOG.info("ExtendedClient creation successful! " + v.get());
                }
            });
        }catch(Exception ex) {
            return;
        }
    }
    
    /**
     * Positions and displays the progress indicator display.
     */
    private void showProgressIndicator() {
        progress.relocate((getWidth() / 2) - 62.5, ((getHeight() / 2) - 200));
        progress.resize(125, 125);
        progress.setVisible(true);
    }
    
    /**
     * The rx.Observable {@link Subscriber} used to receive the emissions 
     * of a successful API key entry or unsuccessful one.
     * 
     * This subscriber interacts with the display to indicate success or reset
     * the display for more attempts in case of a failure.
     * 
     * @return  the {@link Subscriber} used to react to the entry of an api key
     */
    private Subscriber<ReadOnlyObjectProperty<FullClient>> getRetinaSubscriber() {
        return new Subscriber<ReadOnlyObjectProperty<FullClient>>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) {
                resetApiKeyDisplayForReEntry(e.getMessage());
            }
            @Override public void onNext(ReadOnlyObjectProperty<FullClient> t) {
                Platform.runLater(() -> {
                    showApiEntrySuccessInfo();
                    transitionOverlayToDialogMode();
                });
            }
        };
    }
    
    /**
     * Following an unsuccessful api key entry, show the error received and
     * reset the entry widgets to prepare for the user's correction.
     * 
     * @param failureMessage    the failure description and cause
     */
    private void resetApiKeyDisplayForReEntry(String failureMessage) {
        Platform.runLater(() -> {
            Overlay.this.setVisible(true);
            response.relocate((getWidth() / 2) - 175, apiKeyPane.getLayoutY() - 140);
            showEnterAPIFlyout(true);
            Overlay.this.setApiResponseFill(Color.RED);
            Overlay.this.setApiResponse(failureMessage);
            Overlay.this.setApiResponseVisible(true);
            progress.setVisible(false);
            apiTextField.selectAll();
        });
    }
    
    /**
     * Provide the user with a successful api key entry feedback.
     */
    private void showApiEntrySuccessInfo() {
        Overlay.this.setApiResponseFill(Color.WHITE);
        response.relocate((getWidth() / 2) - 75, apiKeyPane.getLayoutY() - 140);
        Overlay.this.setApiResponse("API key accepted!");
        Overlay.this.setApiResponseVisible(true);
        progress.setVisible(false);
    }
    
    /**
     * Begins the delayed dismissal of the {@code Overlay} and removes
     * the widgets used for api key entry in preparation for re-use as
     * a general overlay for modal dialog messages. 
     */
    private void transitionOverlayToDialogMode() {
        (new Thread() {
            public void run() {
                try { Thread.sleep(500); }catch(Exception e) { e.printStackTrace(); }
                Platform.runLater(() -> {
                    showEnterAPIFlyout(false);
                    Overlay.this.setVisible(false);
                    Overlay.this.setApiResponseVisible(false);
                    Overlay.this.setApiResponse("");
                    Overlay.this.apiEntered = true;
                    Overlay.this.clearApiMessage();
                    
                    ContentPane contentPane = WindowService.getInstance().getContentPane();
                    contentPane.getControlPane().initializeRetinaChoices();
                    
                    // Wait until after the initial selection above to add the listener.
                    contentPane.getControlPane().addRetinaChoiceListener((v,o,n) -> {
                        ApplicationService.getInstance().setApiRetinaClient(n);
                    });
                });
            }
        }).start();
    }
    
    /**
     * If true, animates the flyout to its "shown" position, otherwise
     * it dismisses the flyout.
     * @param b
     */
    private void showEnterAPIFlyout(boolean b) {
        Platform.runLater(() -> {
            if(b) {
                enterApiFlyout.flyout();
            }else{
                enterApiFlyout.dismiss();
            }
        });
    }
    
    /**
     * Create, draw, and install the Overlay and its dependent shapes.
     */
    private void redrawOverlayContent() {
        Rectangle overlayRect = new Rectangle();
        overlayRect.setFill(Color.rgb(0, 0, 0, 0.7d));
        overlayRect.setWidth(getWidth());
        overlayRect.setHeight(getHeight());
        overlayRect.setX(0);
        overlayRect.setY(0);
         
        if(!apiEntered) {
            positionAndSizeKeyPane();
            instr.resizeRelocate((getWidth() / 2) - 210, apiKeyPane.getLayoutY() - 150, 480, 63);
            response.resizeRelocate((getWidth() / 2) - 75, apiKeyPane.getLayoutY() - 150, 400, 200);
            response.toFront();
            
            progress = new ProgressIndicator();
            progress.resize(125, 125);
            progress.relocate((getWidth() / 2) - 62.5, ((getHeight() / 2) - 200));
            progress.setManaged(false);
            progress.setVisible(false);
            progress.setFocusTraversable(false);
            progress.toFront();
        }
        
        if(apiEntered && currentDialog != null) {
            currentDialog.relocate(
                (getWidth() / 2.0) - (currentDialog.getWidth() / 2.0), 
                    (getHeight() / 2.0) - (currentDialog.getHeight() * 4.0 / 5.0));
        }
        
        getChildren().clear();
        getChildren().addAll(!apiEntered ?
            new Node[] { overlayRect, instr, response, apiKeyPane, progress } :
                currentDialog == null ? new Node[] { overlayRect, closeButton } : new Node[] { overlayRect, currentDialog, closeButton });
    }
    
    /**
     * Attaches a listener which resizes this {@copy Overlay} during window resizing.
     */
    private void attachResizeListener() {
        (new Thread() {
            public void run() {
                while(getScene() == null || getScene().getWindow() == null) {
                    try { Thread.sleep(200); }catch(Exception e) { e.printStackTrace(); }
                }
                
                Platform.runLater(() -> {
                    Window w = getScene().getWindow();
                    w.widthProperty().addListener((v,o,n) -> {
                        redrawOverlayContent();
                    });
                    w.heightProperty().addListener((v,o,n) -> {
                        redrawOverlayContent();
                    });
                });
            }
        }).start();
    }
    
    private void addDialogServiceToEventBus() {
        EventBus.get().subscribeTo(Pattern.compile("[\\w]+ModalDialog"), (s, p) -> {
            if(s.equals(OVERLAY_SHOW_MODAL_DIALOG.subj())) {
                showDialog((Pane)p.getPayload());
            }else if(s.equals(OVERLAY_DISMISS_MODAL_DIALOG.subj())) {
                dismissDialog();
            }
        });
    }
}
