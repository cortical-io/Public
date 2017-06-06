package io.cortical.iris.persistence;

import java.io.Serializable;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.cortical.iris.ui.custom.widget.WindowTitlePane.MaxResultsType;
import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.view.input.expression.Operator;
import io.cortical.iris.view.input.text.TextDisplay;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.core.PosTag;
import io.cortical.retina.core.PosType;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

public class WindowConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Represents the default file-handling options used by the NIO system */
    public static final StandardOpenOption[] DEFAULT_OPTIONS = new StandardOpenOption[] { 
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING 
    };
    
    public static final String DEFAULT_FILE_DIR = ".iris";
    
    private StandardOpenOption[] openOptions = DEFAULT_OPTIONS;
    
    private String fileDir = DEFAULT_FILE_DIR;
    
    private String fileName;
    
    private boolean isLoaded;
    
    
    ////////////////////////////////////
    //      Serialized Fields         //
    ////////////////////////////////////
    public transient Window currentWindow;
    public String title;
    public Bounds bounds;
    
    //---- Input ----
    public Color idColor;
    public ViewType selectedViewType;
    public List<Bubble.Type> bubbleList;
    public List<String> expressionList;
    public int bubbleInsertionIdx;
    public int focusTraversalIdx;
    public boolean editorInGap;
    public boolean isInput;
    public String textInput;
    public FullClient selectedRetina;
    public Operator defaultOperator;
    
    //---- Output ----
    public String primarySerialFileName;
    public String secondarySerialFileName;
    public transient Window primaryWindow;
    public transient Window secondaryWindow;
    public transient int simTermsStartIdx;
    public transient int simTermsMaxIdx;
    public transient int contextsStartIdx;
    public transient int contextsMaxIdx;
    public transient int slicesStartIdx;
    public transient int slicesMaxIdx;
    public transient int selectedContextID;
    public transient Set<PosTag> posTags;
    public transient boolean doReset = true;
    public transient boolean isDragNDrop = false;
    public transient PosType posType;
    public transient ViewType primaryViewType;
    public transient UUID windowID;
    
    private static final long CHAIN_DELAY = 1000; 
    private transient List<ConfigHandler<? super WindowConfig>> notificationChain = new ArrayList<>();
    private transient int notificationChainIdx = 0;
    
    
    /**
     * Contains serialization data required for saving all Window state.
     * @param fileName
     */
    public WindowConfig(String fileName) {
        this.fileName = fileName;
    }
    
    /**
     * Sets the flag indicating whether this config is for the 
     * configuration of a window following a DND operation.
     * @param b
     */
    public void setIsDragNDrop(boolean b) {
        this.isDragNDrop = b;
    }
    
    /**
     * Returns the flag indicating whether this config is for the 
     * configuration of a window following a DND operation.
     * 
     * @return
     */
    public boolean isDragNDrop() {
        return isDragNDrop;
    }
    
    /**
     * Sets the flag indicating whether a "reset" should be issued
     * prior to rebuilding the view.
     * 
     * @param b
     */
    public void setDoReset(boolean b) {
        this.doReset = b;
    }
    
    /**
     * Returns a flag indicating whether a "reset" should be issued
     * prior to rebuilding the view.
     * 
     * @return
     */
    public boolean doReset() {
        return doReset;
    }
    
    /**
     * Returns the user's selected default {@link Operator}
     * 
     * @return
     */
    public Operator getDefaultOperator() {
        return defaultOperator;
    }
    
    /**
     * Returns the reified {@link Window}'s id.
     * @return
     */
    public UUID getWindowID() {
        return windowID;
    }
    
    /**
     * Returns the view type of the primary connected {@link InputWindow}.
     * @return
     */
    public ViewType getPrimaryViewType() {
        return primaryViewType;
    }
    
    /**
     * Returns the {@link Set} of {@link PosTag}s selected for
     * Text queries.
     * @return
     */
    public Set<PosTag> getPosTags() {
        return posTags;
    }
    
    /**
     * For similar terms queries, the POS type filters
     * the terms according to the type.
     * @return
     */
    public PosType getPosType() {
        return posType;
    }
    
    /**
     * Returns the ID of the selected context, or
     * -1 if none exists.
     * @return
     */
    public int getSelectedContextID() {
        return selectedContextID;
    }
    
    /**
     * Used to obtain a reference to the {@link Window} currently being
     * initialized following deserialization.
     * 
     * @return
     */
    public Window getCurrentWindow() {
        return currentWindow;
    }
    
    /**
     * Returns a flag indicating whether the {@link Window} corresponding to
     * this {@code WindowConfig} is an {@link InputWindow} or an {@link OutputWindow}.
     * @return
     */
    public boolean isInput() {
        return isInput;
    }
    
    /**
     * Adds the next handler in the "chain of responsibility" pattern so that
     * initialization events can be handled sequentially without encroaching on
     * each other despite the existence of queued runnables in the handlers.
     * @param h
     */
    public void addNotificationChainHandler(int index, ConfigHandler<? super WindowConfig> h) {
        if(h == null) {
            return;
        }
        notificationChain.add(index, h);
    }
    
    /**
     * Returns the handler residing at the current notification index.
     * @return
     */
    public ConfigHandler<? super WindowConfig> getCurrentHandler() {
        return notificationChain.isEmpty() || notificationChainIdx >= notificationChain.size() ? 
            null : notificationChain.get(notificationChainIdx);
    }
    
    /**
     * Calls {@link ConfigHandler#handle(WindowConfig)} on the current handler
     * then advances the notification chain index to the next handler.
     */
    public void advanceNotificationChain() {
        ConfigHandler<? super WindowConfig> next = getCurrentHandler();
        ++notificationChainIdx;
        if(next != null) {
            (new Thread(() -> {
                try { Thread.sleep(CHAIN_DELAY); }catch(Exception e) { e.printStackTrace(); }
                Platform.runLater(() -> {
                    next.handle(this);
                });
            })).start();
        }
    }
    
    /**
     * Returns the start index in the list of possible results.
     * @param type
     * @return
     */
    public int getResultStartIndex(MaxResultsType type) {
        switch(type) {
            case SIMTERMS: return simTermsStartIdx;
            case CONTEXTS: return contextsStartIdx;
            case SLICES: return slicesStartIdx;
            default: return 0;
        }
    }
    
    /**
     * Returns the max index in the list of possible results.
     * @param type
     * @return
     */
    public int getResultMaxIndex(MaxResultsType type) {
        switch(type) {
            case SIMTERMS: return simTermsMaxIdx;
            case CONTEXTS: return contextsMaxIdx;
            case SLICES: return slicesMaxIdx;
            default: return 10;
        }
    }
    
    /**
     * Returns the primary {@link InputWindow}
     * @return
     */
    public Window getPrimaryWindow() {
        return primaryWindow;
    }
    
    /**
     * Returns the secondary {@link InputWindow}
     * @return
     */
    public Window getSecondaryWindow() {
        return secondaryWindow;
    }
    
    /**
     * Returns the serialized file name of an {@link OutputWindow}'s primary connected
     * {@link InputWindow}.
     * @return
     */
    public String getPrimarySerialFileName() {
        return primarySerialFileName;
    }
    
    /**
     * Returns the serialized file name of an {@link OutputWindow}'s secondary connected
     * {@link InputWindow}.
     * @return
     */
    public String getSecondarySerialFileName() {
        return secondarySerialFileName;
    }
    
    /**
     * Returns the {@link FullClient} which is configured to access the 
     * user's selected Retina type.
     * @return
     */
    public FullClient getSelectedRetina() {
        return selectedRetina;
    }
    
    /**
     * Returns the text inserted by the user into the {@link TextDisplay}
     * @return
     */
    public String getTextInput() {
        return textInput;
    }
    
    /**
     * Returns a flag indicating whether or not this configuration has
     * already been deserialized from the file system.
     * @return
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Sets the flag indicating whether or not this configuration has
     * already been deserialized from the file system.
     * @param   b
     */
    public void setLoaded(boolean b) {
        this.isLoaded = b;
    }
    
    /**
     * Returns the list of {@link Bubble.Type}s
     * @return
     */
    public List<Bubble.Type> getBubbleList() {
        return bubbleList;
    }
    
    /**
     * Returns the list of expression symbols or words.
     * @return
     */
    public List<String> getExpressionList() {
        return expressionList;
    }
    
    /**
     * Returns the bubble insertion index
     * @return
     */
    public int getBubbleInsertionIndex() {
        return bubbleInsertionIdx;
    }
    
    /**
     * Returns the focus traversal index.
     * @return
     */
    public int getFocusTraversalIndex() {
        return focusTraversalIdx;
    }
    
    /**
     * Returns the editor in gap flag
     * @return
     */
    public boolean editorInGap() {
        return editorInGap;
    }
    
    /**
     * Returns the type ({@link ViewType}) of the currently selected view.
     * @return
     */
    public ViewType getViewType() {
        return selectedViewType;
    }
    
    /**
     * Returns the window title
     * @return
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Returns the bounding rectangle of a given {@link Window}
     * @return
     */
    public Bounds getBounds() {
        return bounds;
    }
    
    /**
     * Returns the {@link Color} used as the color id.
     * @return
     */
    public Color getIDColor() {
        return idColor;
    }
    
    /**
     * Returns the name of the directory where IRIS persistence files
     * are written-to/read-from.
     * 
     * @return  the file directory name
     */
    public String getFileDir() {
        return fileDir;
    }
    
    /**
     * Returns the filename which is used to persist a given file or
     * retrieve a given file.
     * 
     * @return  the file name
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Returns the defined array of {@link StandardOpenOption}s used to configure
     * the underlying NIO file io mechanism.
     * @return
     */
    public StandardOpenOption[] getOpenOptions() {
        return openOptions;
    }
}
