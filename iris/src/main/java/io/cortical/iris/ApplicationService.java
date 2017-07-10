package io.cortical.iris;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.bushe.swing.event.EventServiceExistsException;
import org.bushe.swing.event.EventServiceLocator;
import org.bushe.swing.event.EventTopicSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cortical.iris.ApplicationService.Validation.Type;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.persistence.Persistence;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.ContentPane;
import io.cortical.iris.view.InputViewArea;
import io.cortical.iris.view.input.expression.ExpressionDisplay;
import io.cortical.iris.view.input.expression.Operator;
import io.cortical.iris.window.Window;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.model.Term;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import rx.Observable;
import rx.Subscriber;

/**
 * Class which offers application-wide services such as event bus registration,
 * api client creation, client OS host services access, globally scoped utilities, 
 * and API key management.
 * 
 * @see WindowService
 */
public class ApplicationService extends SecurityManager {
    public enum ApplicationState { SERIALIZING, DESERIALIZING, CONFIGURING, NORMAL };
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationService.class);
    
    private static final String CONFIG_MAP = "output_config_mapping.ocm";
    private static final int DEFAULT_THREADPOOL_SIZE = 3;
    private static ApplicationService INSTANCE;
    
    private ReadOnlyObjectWrapper<FullClient> retinaClientProperty;
    private ReadOnlyObjectWrapper<ExtendedClient> extendedClientProperty;
    private ReadOnlyObjectWrapper<CachingClientWrapper> cachingClientProperty;
    private ReadOnlyObjectWrapper<ApplicationState> applicationStateProperty;
    
    private HostServices hostServices;
    
    private KeyCache keyCache;
    
    private Stage primaryStage;
    
    private Preferences userPrefs = Preferences.userNodeForPackage(Iris.class);
    
    private Map<String, String[]> outputConfigMapping;
    
    /** Thread pool access (used by {@link ServerMessageService} */
    private ExecutorService executor;
    
    private ObjectProperty<Operator> defaultOperatorProperty = new SimpleObjectProperty<>(Operator.AND);
    
    private IntegerProperty defaultIndentationProperty = new SimpleIntegerProperty(4);
      
        
    
    
    /**
     * Singleton constructor
     */
    private ApplicationService() {
        retinaClientProperty = new ReadOnlyObjectWrapper<FullClient>();
        extendedClientProperty = new ReadOnlyObjectWrapper<ExtendedClient>();
        cachingClientProperty = new ReadOnlyObjectWrapper<CachingClientWrapper>();
        applicationStateProperty = new ReadOnlyObjectWrapper<ApplicationState>(ApplicationState.NORMAL);
        cachingClientProperty.set(new CachingClientWrapper());
        
        // Register our event bus service
        try {
            EventServiceLocator.setEventService(EventServiceLocator.SERVICE_NAME_EVENT_BUS, EventBus.get());
        } catch(EventServiceExistsException e) {
            e.printStackTrace();
        }
        
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.APPLICATION_WINDOW_SAVE_PROMPT.subj() + "[\\w\\-]+"), getWindowConfigPersistenceHandler(true));
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.APPLICATION_WINDOW_LOAD_PROMPT.subj() + "[\\w\\-]+"), getWindowConfigPersistenceHandler(false));
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.APPLICATION_NODE_SAVE_SNAPSHOT.subj() + "[\\w\\-]+"), getSnapshotPersistenceHandler());
        EventBus.get().subscribeTo(BusEvent.APPLICATION_WINDOW_DELETE_PROMPT.subj(), getOutputConfigDeletionHandler());
        
        executor = Executors.newFixedThreadPool(DEFAULT_THREADPOOL_SIZE, new DaemonThreadFactory());
        
        Platform.runLater(() -> {
            WindowService.getInstance().contentCreatedProperty().addListener((v,o,n) -> {
                if(n) {
                    LOGGER.debug("The application UI is considered to be fully constructed at this point...");
                    
                    if(!ClipboardMonitor.getInstance().isAttached()) {
                        LOGGER.debug("The ClipboardMonitor has not been started, and will now start");
                        startClipboardMonitor();
                    }
                }
            });
        });
    }
    
    /**
     * Returns the singleton instance of {@code ApplicationService}
     * @return  the singleton instance of {@code ApplicationService}
     */
    public static ApplicationService getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ApplicationService();
        }
        return INSTANCE;
    }
    
    /**
     * Returns the Thread pool {@link ExecutorService}.
     * @return
     */
    public ExecutorService getExecutorService() {
        return executor;
    }
    
    /**
     * Sets the {@link HostServices} utility on this {@code ApplicationService} for later use...
     * 
     * @param hostServices
     */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
    
    /**
     * Returns the {@link HostServices}
     * @return  the HostServices
     */
    public HostServices getHostServices() {
        return hostServices;
    }
    
    /**
     * Records and stores a reference to the application's
     * primary {@link Stage}
     * 
     * @param stage
     */
    public void setPrimaryStage(Stage stage) {
        // Do only once
        if(primaryStage == null) {
            this.primaryStage = stage;
            
            primaryStage.setOnCloseRequest(e -> {
                userPrefs.putDouble("stage.x", primaryStage.getX());
                userPrefs.putDouble("stage.y", primaryStage.getY());
                userPrefs.putDouble("stage.width", primaryStage.getWidth());
                userPrefs.putDouble("stage.height", primaryStage.getHeight());
                userPrefs.putBoolean("compareInfo.on", WindowService.getInstance().compareInfoButtonVisibleProperty().get());
                userPrefs.put(
                    "application.defaultOperator", 
                        getDefaultOperatorProperty().get().toString());
                userPrefs.putInt(
                    "application.defaultIndentation", 
                        getDefaultIndentationProperty().get());
            });
        }        
    }
    
    /**
     * Returns a reference to this application's primary {@link Stage}
     * @return  the app's primary {@code Stage}
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Applies the stored stage state (dimensions and location), to the specified
     * {@link Stage} using the defaults provided if necessary.
     * 
     * @param stage         the Stage to apply saved dimension to.
     * @param defaults      the defaults in case no previous state was saved
     */
    public void restorePrimaryStageState(Stage stage, Bounds defaults) {
        stage.setX(userPrefs.getDouble("stage.x", defaults.getMinX()));
        stage.setY(userPrefs.getDouble("stage.y", defaults.getMinY()));
        stage.setWidth(userPrefs.getDouble("stage.width", defaults.getWidth()));
        stage.setHeight(userPrefs.getDouble("stage.height", defaults.getHeight()));
        
        WindowService.getInstance().compareInfoButtonVisibleProperty().set(userPrefs.getBoolean("compareInfo.on", true));
        
        Operator defaultOperator = Operator.typeFor(userPrefs.get("application.defaultOperator", Operator.AND.toString()));
        getDefaultOperatorProperty().set(defaultOperator);
        
        getDefaultIndentationProperty().set(userPrefs.getInt("application.defaultIndentation", 4));
    }
    
    /**
     * Starts the application's {@link ClipboardMonitor} which will respond to a {@link KeyCombination}
     * registered to activate the monitor.
     * 
     * @see ContentPane#ALT_D
     * @see ContentPane#addGlobalKeyListener()
     */
    public void startClipboardMonitor() {
        ClipboardMonitor.getInstance().attach();
        if(ClipboardMonitor.getInstance().isAttached()) {
            LOGGER.debug("The ClipboardMonitor is now listening for user activations...");
        } else {
            LOGGER.warn("The ClipboardMonitor was unable to start!");
        }
    }
    
    /**
     * Returns the last copied String onto the system clipboard.
     * @return
     */
    public String getClipboardContent() {
        return ClipboardMonitor.getInstance().getClipboardContent();
    }
    
    /**
     * Returns the property which stores the user's default operator choice.
     * @return
     */
    public ObjectProperty<Operator> getDefaultOperatorProperty() {
        return defaultOperatorProperty;
    }
    
    /**
     * Returns the global property which sets the default number of spaces
     * to indent json text in the {@link ExpressionDisplay}
     * 
     * @return
     */
    public IntegerProperty getDefaultIndentationProperty() {
        return defaultIndentationProperty;
    }
    
    /**
     * Returns the previously stored api key if one exists.
     * @return
     */
    public String loadApiKey() {
        return KeyCache.getKey();
    }
    
    /**
     * Signals the {@link KeyCache} to store the api key on disk.
     * @param key   the key to store
     */
    public void storeApiKey(String key) {
        try {
            keyCache = new KeyCache(key);
            keyCache.store();
        }catch(Exception e) { e.printStackTrace(); }
    }
    
    /**
     * <p>
     * Returns an rx.Observable containing a read only property containing the configured client or
     * null.
     * </p><p>
     * <b>Note: this method is not guaranteed to return on the client rendering thread! (Please use:
     * {@link Platform#runLater(Runnable)} if calling UI methods following this call)</b>
     * </p>
     * 
     * @param key       an {@link Optional} which is "optional" and will change and set the api key if present,
     *                  otherwise it is assumed the api key has already been set (Optional==null or !{@link Optional#isPresent()}) 
     * 
     * @return  a read only property containing the configured client
     * @throws Exception    if the current api key is invalid or there's a network oriented error.
     */
    public Observable<ReadOnlyObjectProperty<FullClient>> createRetinaClientProperty(Optional<String> key) throws Exception {
        return Observable.create(new Observable.OnSubscribe<ReadOnlyObjectProperty<FullClient>>() {
            @Override
            public void call(Subscriber<? super ReadOnlyObjectProperty<FullClient>> subscriber) {
                // If the key is null or the Optional is empty simply return the retina client property
                // Otherwise, create it; validate it, and return it.
                if(key != null && key.isPresent()) {
                    System.setProperty("apiKey", key.get());
                    retinaClientProperty.set(RetinaClientFactory.getClient("en_associative"));//new FullClient(System.getProperty("apiKey")));
                    
                    (new Thread() {
                        public void run() {
                            validateApiKey(System.getProperty("apiKey")).subscribe(
                                new Subscriber<Validation>() {
                                    @Override public void onCompleted() {}
                                    @Override public void onError(Throwable e) {}
                                    @Override public void onNext(Validation v) {
                                        switch(v.getType()) {
                                            case SUCCESS:
                                                // Forward success notification to the subscriber
                                                subscriber.onNext(retinaClientProperty.getReadOnlyProperty());
                                                subscriber.onCompleted();
                                                break;
                                            case FAILURE:
                                                System.clearProperty("apiKey");
                                                retinaClientProperty.set(null);
                                                subscriber.onError(v.getThrowable());
                                        }
                                    }
                                }
                            );
                        }
                    }).start();
                }else{
                    subscriber.onError(new Exception("Invalid Key Specified"));
                }
            }
        });
    }
    
    /**
     * <p>
     * Returns an rx.Observable containing a read only property containing the configured client or
     * null.
     * </p><p>
     * <b>Note: this method is not guaranteed to return on the client rendering thread! (Please use:
     * {@link Platform#runLater(Runnable)} if calling UI methods following this call)</b>
     * </p>
     * 
     * @param key       an {@link Optional} which is "optional" and will change and set the api key if present,
     *                  otherwise it is assumed the api key has already been set (Optional==null or !{@link Optional#isPresent()}) 
     * 
     * @return  a read only property containing the configured client
     * @throws Exception    if the current api key is invalid or there's a network oriented error.
     */
    public Observable<ReadOnlyObjectProperty<ExtendedClient>> createExtendedClientProperty(Optional<String> key) throws Exception {
        return Observable.create(new Observable.OnSubscribe<ReadOnlyObjectProperty<ExtendedClient>>() {
            @Override
            public void call(Subscriber<? super ReadOnlyObjectProperty<ExtendedClient>> subscriber) {
                // If the key is null or the Optional is empty simply return the retina client property
                // Otherwise, create it; validate it, and return it.
                if(key != null && key.isPresent()) {
                    System.setProperty("apiKey", key.get());
                    extendedClientProperty.set(new ExtendedClient(System.getProperty("apiKey")));
                    
                    (new Thread() {
                        public void run() {
                            validateApiKey(System.getProperty("apiKey")).subscribe(
                                new Subscriber<Validation>() {
                                    @Override public void onCompleted() {}
                                    @Override public void onError(Throwable e) {}
                                    @Override public void onNext(Validation v) {
                                        switch(v.getType()) {
                                            case SUCCESS:
                                                // Forward success notification to the subscriber
                                                subscriber.onNext(extendedClientProperty.getReadOnlyProperty());
                                                subscriber.onCompleted();
                                                break;
                                            case FAILURE:
                                                System.clearProperty("apiKey");
                                                extendedClientProperty.set(null);
                                                subscriber.onError(v.getThrowable());
                                        }
                                    }
                                }
                            );
                        }
                    }).start();
                }else{
                    subscriber.onError(new Exception("Invalid Key Specified"));
                }
            }
        });
    }
    
    /**
     * Returns the full client property.
     * @return  the full client property
     */
    public ReadOnlyObjectProperty<FullClient> retinaClientProperty() {
        return retinaClientProperty.getReadOnlyProperty();
    }
    
    /**
     * Returns the caching client wrapper property
     * @return  the caching client wrapper property
     */
    public ReadOnlyObjectProperty<CachingClientWrapper> cachingClientProperty() {
        return cachingClientProperty.getReadOnlyProperty();
    }
    
    /**
     * WARNING: FOR USE ONLY WITHIN THIS APPLICATION
     * Returns the extended client property.
     * @return
     */
    public ReadOnlyObjectProperty<ExtendedClient> extendedClientProperty() {
        return extendedClientProperty.getReadOnlyProperty();
    }
    
    /**
     * Sets the Retina type of the client used to access the server Retina API
     * @param client 
     */
    public void setApiRetinaClient(FullClient client) {
        retinaClientProperty.set(client);
        extendedClientProperty.set(
            RetinaClientFactory.getExtendedClient(
                RetinaClientFactory.getRetinaName(client)));
    }
    
    /**
     * Returns the property which tracks the current {@link ApplicationState}.
     * ApplicationState changes when serialization/deserialization is taking
     * place.
     * @return
     */
    public ReadOnlyObjectProperty<ApplicationState> applicationStateProperty() {
        return applicationStateProperty.getReadOnlyProperty();
    }
    
    /**
     * Privileged call to change the application state.
     * @param state
     */
    public void privilegedStateChange(ApplicationState state) {
        Class<?> caller = getClassContext()[1];
        LOGGER.debug("ApplicationState change requested by: " + caller.getSimpleName());
        if(!caller.equals(Window.class) && !caller.equals(WindowService.class) && !caller.equals(InputViewArea.class)) {
            throw new IllegalStateException("Illegal method call by unprivileged class: " + caller);
        }
        
        applicationStateProperty.set(state);
    }
    
    /**
     * Returns a {@link Validation} containing the results of the API Key
     * validation.
     * 
     * @param key       the api key string
     * @return  a Validation
     */
    public Observable<Validation> validateApiKey(String key) {
        return Observable.create(new Observable.OnSubscribe<Validation>() {
            @Override
            public void call(Subscriber<? super Validation> subscriber) {
                (new Thread() {
                    public void run() {
                        try {
                            new FullClient(key).getTerms(null, 0, 10, false);
                            // Store in key cache for next app load
                            new KeyCache(key).store();
                            subscriber.onNext(new Validation(null, "Api key accepted.", Type.SUCCESS));
                            subscriber.onCompleted();
                        }catch(Exception e) {
                            subscriber.onNext(new Validation(e, e.getMessage(), Type.FAILURE));
                            subscriber.onCompleted();
                        }
                    }
                }).start();
            }
        });
    }
    
    /**
     * Returns a {@link Validation} containing the results of the attempted {@link ExtendedClient}
     * instantiation and use.
     * 
     * @param key       the api key string
     * @return  a Validation
     */
    public Observable<Validation> validateExtendedApi(String key) {
        return Observable.create(new Observable.OnSubscribe<Validation>() {
            @Override
            public void call(Subscriber<? super Validation> subscriber) {
                (new Thread() {
                    public void run() {
                        try {
                            List<Term> terms = new ExtendedClient(key).getSimilarTermsForPosition(15);
                            if(terms == null || terms.isEmpty()) {
                                subscriber.onNext(new Validation(new Exception("Terms not retrievable"), "Terms not retrieved", Type.FAILURE));
                                subscriber.onCompleted();
                            }else {
                                subscriber.onNext(new Validation(null, "Access to extended api granted.", Type.SUCCESS));
                                subscriber.onCompleted();
                            }
                        }catch(Exception e) {
                            subscriber.onNext(new Validation(e, e.getMessage(), Type.FAILURE));
                            subscriber.onCompleted();
                        }
                    }
                }).start();
            }
        });
    }
    
    /**
     * Immutable container for Validation results.
     */
    public static class Validation {
        public enum Type { SUCCESS, FAILURE };
        
        private Throwable t;
        private String message;
        private Type type;
        
        public Validation(Throwable t, String message, Type type) {
            this.t = t;
            this.message = message;
            this.type = type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Type getType() {
            return type;
        }
        
        public Throwable getThrowable() {
            return t;
        }
    }
    
    /**
     * Makes daemon threads to ease app shutdown.
     */
    private static class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    }
    
    /**
     * Returns the EventBus listener configured to handle serialization/deserialization requests.
     * 
     * @param isSave    flag indicating which mode the "handler" will work in...
     * @return
     */
    private EventTopicSubscriber<Payload> getWindowConfigPersistenceHandler(boolean isSave) {
        return (messageString, requestPayload) -> {
            String route = messageString.substring(messageString.indexOf("_") + 1);
            Window w = WindowService.getInstance().windowFor(UUID.fromString(route));
            
            FileChooser fileChooser = new FileChooser();
            if(isSave) {
                fileChooser.setTitle("Save Configuration As...");
                fileChooser.setInitialFileName(w.getTitleField().getText());
            } else {
                fileChooser.setTitle("Select Window Configuration");
            }
            
            File f = new File(System.getProperty("user.home") + File.separator + ".iris");
            LOGGER.debug("Persistence Handler using home dir: " + System.getProperty("user.home"));
            
            // Make sure IRIS configuration directory exists
            f.mkdirs();
            
            fileChooser.setInitialDirectory(f);
            LOGGER.debug("Persistence Handler using configured save dir: " + f.getAbsolutePath());
            
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(w.isInput() ? "IRIS InputWindow Configurations" : "IRIS OutputWindow Configurations", 
                    w.isInput() ? "*.iwrs" : "*.owrs")
            );
            
            File selectedFile = isSave ? 
                fileChooser.showSaveDialog(w.getScene().getWindow()) : 
                    fileChooser.showOpenDialog(w.getScene().getWindow());

            if (selectedFile != null) {
                String fileName = selectedFile.getName().replaceAll("[\\s]+", "_");
                
                // Make sure the File object used, matches the modified fileName minus spaces.
                File moddedFile = new File(selectedFile.getAbsoluteFile().getParent() + File.separator + fileName);
                selectedFile = moddedFile;
                
                LOGGER.debug("Persistence Handler using user's selected file to " + 
                    (isSave ? "save file: " : "load file: " + selectedFile.getAbsolutePath() + " ; " + fileName));
                
                if(isSave) {
                    // Make sure that any OutputWindow file overwrites delete any
                    // saved connected InputWindow serialization files (which will
                    // have different names) - first, before we overwrite the file!
                    checkConfigMapping();
                    if(outputConfigMapping.containsKey(fileName)) {
                        LOGGER.debug("Caught potential connected file leak - deleting any connected InputWindow files now!");
                        deleteFile(selectedFile);
                    }
                    
                    w.serialize(new WindowConfig(fileName));
                    LOGGER.debug("Persistence Handler now calling serialize()...");
                } else {
                    WindowService.getInstance().configureWindow(w, new WindowConfig(fileName));
                    LOGGER.debug("Persistence Handler now calling configureWindow()...");
                }
            } else {
                LOGGER.debug("Persistence Handler got a null filename - no action is invoked!");
            }
        };
    }
    
    /**
     * Handler which manages prompting the user for a filename and then
     * saving the image file as a png.
     * 
     * @return
     */
    private EventTopicSubscriber<Payload> getSnapshotPersistenceHandler() {
        return (messageString, requestPayload) -> {
            String route = messageString.substring(messageString.indexOf("_") + 1);
            Window w = WindowService.getInstance().windowFor(UUID.fromString(route));
            String description = requestPayload.getDescription();
            Image image = (Image)requestPayload.getPayload();
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save " + description);
            File f = new File(System.getProperty("user.home"));
            fileChooser.setInitialDirectory(f);
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Save " + description + " .png image", "*.png")
            );
            
            File selectedFile = fileChooser.showSaveDialog(w.getScene().getWindow());
            
            if(selectedFile == null) {
                LOGGER.debug("User selected \"Cancel\" and/or file was null, not saving snapshot.");
                return;
            }
            
            // save image (without alpha)
            BufferedImage bufImageARGB = SwingFXUtils.fromFXImage(image, null);
            BufferedImage bufImageRGB = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(), BufferedImage.OPAQUE);

            Graphics2D graphics = bufImageRGB.createGraphics();
            graphics.drawImage(bufImageARGB, 0, 0, null);

            try {
                ImageIO.write(bufImageRGB, "png", selectedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            graphics.dispose();

            LOGGER.debug("Image saved: " + selectedFile.getName());
        };
    }
    
    /**
     * Returns the {@link Map} containing references to the connected input windows from each
     * output window. The map is persisted on disk and if is not found, is created.
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, String[]> checkConfigMapping() {
        File f = new File(System.getProperty("user.home") + File.separator + ".iris");
        LOGGER.debug("Persistence Map Checker using home dir: " + System.getProperty("user.home"));
        
        // Make sure IRIS configuration directory exists
        f.mkdirs();
        
        boolean mapExists = false;
        if(outputConfigMapping == null) {
            WindowConfig config = new WindowConfig(CONFIG_MAP);
            try {
                outputConfigMapping = 
                    (Map<String, String[]>)Persistence.get().read(config);
                mapExists = true;
            } catch(Exception e) {
                LOGGER.debug("Output Config Mapping File Not Found");
            }
        } else {
            mapExists = true;
        }
        
        if(!mapExists) {
            outputConfigMapping = new HashMap<>();
            persistConfigMap();
            LOGGER.debug("Created and saved new Output Config Mapping File");
        }
        LOGGER.debug("Got Output Config Mapping: " + outputConfigMapping);
        return outputConfigMapping;
    }
    
    /**
     * Refreshes the config map on disk
     */
    public void persistConfigMap() {
        WindowConfig config = new WindowConfig(CONFIG_MAP);
        Persistence.get().write(config, (Serializable)outputConfigMapping);
    }
    
    /**
     * Returns the EventBus listener configured to handle deletion requests for saved Window configurations.
     * 
     * @param isSave    flag indicating which mode the "handler" will work in...
     * @return
     */
    private EventTopicSubscriber<Payload> getOutputConfigDeletionHandler() {
        return (messageString, requestPayload) -> {
            Window w = (Window)requestPayload.getPayload();
            outputConfigMapping = checkConfigMapping();
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Delete Window Configuration...");
            
            fileChooser.getExtensionFilters().addAll(w.isInput() ? 
                new FileChooser.ExtensionFilter("IRIS: Open InputWindow Config For Deletion", "*.iwrs") : 
                    new FileChooser.ExtensionFilter("IRIS: Open OutputWindow Config For Deletion", "*.owrs")
            );
            
            File selectedFile = fileChooser.showOpenDialog(WindowService.getInstance().getStage());
            if (selectedFile != null) {
                Pane dialog = createFileDeletionDialog(selectedFile);
                Payload p = new Payload(dialog);
                EventBus.get().broadcast(BusEvent.OVERLAY_SHOW_MODAL_DIALOG.subj(), p);
            }
            
            LOGGER.debug("Config Deletion Handler finished");
        };
    }
    
    /**
     * Deletes the specified saved configuration file from disk.
     * @param selectedFile
     */
    private void deleteFile(File selectedFile) {
        try {
            selectedFile.delete();
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        String name = selectedFile.getName().replaceAll("[\\s]+", "_");
        if(outputConfigMapping.containsKey(name)) {
            String[] inputFileNames = outputConfigMapping.remove(name);
            int i = 0;
            for(String fileName : inputFileNames) {
                if(fileName != null) {
                    String filePath = System.getProperty("user.home") + File.separator + ".iris" + File.separator + fileName;
                    File f = new File(filePath);
                    if(f.exists()) {
                        try {
                            f.delete();
                            LOGGER.debug("Config Deletion Handler deleted " + 
                                (i == 0 ? "primary input config: " : "secondary input config: "+ f.getName()));
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            persistConfigMap();
        }
    }
    
    /**
     * Creates and returns a new file deletion warning dialogd.
     * @param fileToDelete  the file to mention in the dialog as the file
     *                      being deleted.
     * @return
     */
    private Pane createFileDeletionDialog(File fileToDelete) {
        StackPane dialog = new StackPane();
        dialog.getStyleClass().add("input-toggle-dialog");
        dialog.resize(405, 100);
        dialog.setPrefSize(405, 100);
              
        Label l = new Label("Are you sure you want to delete file: " + fileToDelete.getName() + " ?");
        l.setFont(Font.font("Questrial-Regular", 16));
        l.setTextFill(Color.WHITE);
        l.setManaged(false);
        l.resizeRelocate(15, 5, 380, 50);
        Label l2 = new Label("Is this ok?");
        l2.setFont(Font.font("Questrial-Regular", 14));
        l2.setTextFill(Color.WHITE);
        l2.setManaged(false);
        l2.resizeRelocate(15, 35, 360, 50);
        Button ok = new Button("Ok");
        ok.getStyleClass().addAll("input-toggle-dialog");
        ok.setPrefSize(60,  25);
        ok.setOnAction(e -> {
            EventBus.get().broadcast(BusEvent.OVERLAY_DISMISS_MODAL_DIALOG.subj(), null);
            deleteFile(fileToDelete);
        });
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll("input-toggle-dialog");
        cancel.setPrefSize(70, 25);
        cancel.setOnAction(e -> {
            EventBus.get().broadcast(BusEvent.OVERLAY_DISMISS_MODAL_DIALOG.subj(), null);
        });
        HBox hBox = new HBox(ok, cancel);
        hBox.resize(155, 30);
        hBox.setSpacing(15);
        hBox.setManaged(false);
        hBox.relocate(250, 63);
        dialog.getChildren().addAll(l, l2, hBox);
        
        return dialog;
    }
}
