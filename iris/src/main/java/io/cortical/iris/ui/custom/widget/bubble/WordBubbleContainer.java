package io.cortical.iris.ui.custom.widget.bubble;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.cortical.iris.RetinaClientFactory;
import io.cortical.iris.ServerMessageService;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Message;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.view.input.expression.ExpressionWordBubbleContainer;
import io.cortical.iris.view.input.expression.FingerprintBubble;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Term;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.util.Pair;

/**
 * Smart container that is able to display {@link Bubble}s around words
 * in a {@link FlowPane} display. This version is <b>non-editable</b>.
 * 
 * @author cogmission
 */
public class WordBubbleContainer extends FlowPane {
    protected Map<String, Pair<Term, Color>> queries = new HashMap<>();
    
    protected int bubbleInsertionIdx = 0;
    protected int focusTraversalIdx = 0;

    /**
     * Constructs a new {@code OutputWordBubbleContainer}
     */
    public WordBubbleContainer() {
        getStyleClass().add("bubble-container");
        setVgap(0);
        setHgap(3);
    }
    
    /**
     * Adds a new {@link Entry} containing a {@link WordBubble} with
     * a String "term", to this container. 
     * @param term
     * @return  an {@link Entry}
     */
    public Entry<WordBubble> addTerm(String term) {
        Entry<WordBubble> e = createTerm(term);
        getChildren().add(bubbleInsertionIdx, e);
        
        return e;
    }
    
    /**
     * Returns and {@link Entry} containing a {@link WordBubble} with
     * listeners added, but not added to the container.
     * @param term
     * @return  an {@link Entry}
     */
    public Entry<WordBubble> createTerm(String term) {
        Entry<WordBubble> e = new Entry<>(new WordBubble(term));
        e.getBubble().selectedProperty().addListener(getTermSelectionListener(e, term));
        e.setAlignment(Pos.CENTER);
        
        return e;
    }
    
    public ChangeListener<? super Boolean> getFingerprintSelectionListener(Entry<FingerprintBubble> e) {
        return (v,o,n) -> {
            e.getBubble().getStyleClass().setAll(n ? "fingerprint-bubble-selected" : "fingerprint-bubble");
        };
    }
    
    public ChangeListener<? super Boolean> getTermSelectionListener(Entry<WordBubble> e, String term) {
        return (v,o,n) -> {
            
            e.getBubble().getStyleClass().setAll(n ? "bubble-selected" : "bubble");
            
            InputWindow inWindow = (InputWindow)WindowService.getInstance().windowFor(this);
            UUID inUUUID = inWindow.getWindowID();
            List<OutputWindow> l = WindowService.getInstance().getConnectedOutputWindows(inUUUID);
            
            if(l.isEmpty()) return;
            
            OutputWindow w = l.get(0);
            if(n) {
                ServerRequest req = new ServerRequest();
                req.setTermString(term);
                req.setPayload(new Pair<UUID, Message>(w.getWindowID(), new Message(ViewType.TERM, null)));
                req.setWindow(l.get(0));
                FullClient currentClient = WindowService.getInstance().clientRetinaFor(inWindow);
                req.setRetinaClient(currentClient);
                String retinaName = RetinaClientFactory.getRetinaName(currentClient);
                req.setExtendedClient(RetinaClientFactory.getExtendedClient(retinaName));
                req.setRetinaLanguage(retinaName);
                req.setInputViewType(ViewType.TERM);
                
                ServerMessageService.getInstance().routeTermLookup(req, t -> {
                    queries.put(t.getTerm(), new Pair<Term, Color>(t, Color.rgb(237, 93, 37)));
                    Pair<Term, Color> pair = new Pair<>(t, Color.rgb(237, 93, 37));
                    Payload payload = new Payload(pair);
                    EventBus.get().broadcast(BusEvent.FINGERPRINT_DISPLAY_ADD.subj() + w.getWindowID().toString(), payload);
                });
            }else{
                Pair<Term, Color> cachedPair = queries.get(term.toLowerCase());
                if(cachedPair == null || cachedPair.getKey() == null) return;
                
                Pair<Fingerprint, Color> pair = new Pair<>(cachedPair.getKey().getFingerprint(), Color.rgb(237, 93, 37));
                Payload payload = new Payload(pair);
                EventBus.get().broadcast(BusEvent.FINGERPRINT_DISPLAY_REMOVE_BY_COLOR.subj() + w.getWindowID().toString(), payload);
            }
        };
    }
    
    /**
     * Clears the cache of terms to their Fingerprint/Color pair.
     * 
     * called by {@link ExpressionWordBubbleContainer#reset()}
     */
    public void clearQueryCache() {
        queries.clear();
    }
    
    /**
     * Returns the focus traversal index.
     * @return
     */
    public int getFocusTraversalIndex() {
        return focusTraversalIdx;
    }
    
    /**
     * Sets the focus traversal index.
     * @param index
     */
    public void setFocusTraversalIndex(int index) {
        this.focusTraversalIdx = index;
    }
    
    /**
     * Returns the bubble insertion index.
     * @return
     */
    public int getBubbleInsertionIndex() {
        return bubbleInsertionIdx;
    }
    
    /**
     * Sets the bubble insertion index
     * @param index
     */
    public void setBubbleInsertionIndex(int index) {
        this.bubbleInsertionIdx = index;
    }

    /**
     * Increments the index pointing to a bubble
     * insertion point.
     */
    public void incBubbleInsertionIdx() {
        if(focusTraversalIdx == bubbleInsertionIdx) {
            focusTraversalIdx = (bubbleInsertionIdx += 1);
        }else{
            bubbleInsertionIdx++;
        }
    }

    /**
     * Decrements the index pointing to a bubble
     * insertion point.
     */
    protected void decBubbleInsertionIdx() {
        if(focusTraversalIdx == bubbleInsertionIdx) {
            focusTraversalIdx = (bubbleInsertionIdx -= 1);
        }else{
            bubbleInsertionIdx--;
        }
    }

    /**
     * Increments the index pointing to the {@link Entry}
     * currently being focused.
     */
    protected void incFocusTraversalIdx() {
        focusTraversalIdx += 1;
        if(focusTraversalIdx == getChildren().size()) {
            focusTraversalIdx = 0;
        }
    }

    /**
     * Decrements the index pointing to the {@link Entry}
     * currently being focused.
     */
    protected void decFocusTraversalIdx() {
        focusTraversalIdx -= 1;
        if(focusTraversalIdx < 0) {
            focusTraversalIdx = getChildren().size() - 1;
        }
    }

    /**
     * Returns a flag indicating whether the terms exist
     * in the current display state.
     * @return
     */
    protected boolean containsTerms() {
        return getChildren().size() > 1;
    }

    /**
     * Manages focus of the contained {@link Entry}s.
     * 
     * @param entryIndex
     */
    protected void selectEntry(int entryIndex) {
        int len = getChildren().size();
        for(int i = 0;i < len;i++) {
            ((Entry<?>)getChildren().get(i)).setSelected(i == entryIndex);
        }
        
        ((Entry<?>)getChildren().get(entryIndex)).requestFocus();
    }

    /**
     * Sets all Entries to unselected state
     */
    protected void deSelectAllEntries() {
        int len = getChildren().size();
        for(int i = 0;i < len;i++) {
            ((Entry<?>)getChildren().get(i)).setSelected(false);
        }
    }

    /**
     * Returns the {@link Entry} residing at the focus traversal index.
     * @return
     */
    protected Entry<?> getSelectedEntry() {
        return (Entry<?>)getChildren().get(focusTraversalIdx);
    }

    /**
     * Returns a flag indicating whether the focus traversal index is at the end of the 
     * expression or not.
     * @return  editor at end flag
     */
    protected boolean focusAtEnd() {
        return focusTraversalIdx == getChildren().size() - 1;
    }
    
    /**
     * Returns a flag indicating whether the focus traversal index is at the beginning of the 
     * expression or not.
     * @return  editor at beginning flag
     */
    protected boolean focusAtBeginning() {
        return focusTraversalIdx == 0;
    }

    /**
     * Returns a flag indicating whether the container currently
     * has terms inserted in it ({@link Entry}s other than the "editor").
     * @return
     */
    public boolean hasTerms() {
        return (bubbleInsertionIdx) > 0;
    }

    public Bubble getPrecedingBubble() {
        if(focusTraversalIdx == 0) return null;
        return ((Entry<?>)getChildren().get(focusTraversalIdx - 1)).getBubble();
    }

    public Bubble getNextBubble() {
        if(focusTraversalIdx >= getChildren().size() - 1) return null;
        return ((Entry<?>)getChildren().get(focusTraversalIdx + 1)).getBubble();
    }

    /**
     * Returns the {@link Bubble} which was last inserted, or null
     * if there are no terms present.
     * @return
     */
    public Bubble getLastInserted() {
        return !hasTerms() ? null : ((Entry<?>)getChildren().get(bubbleInsertionIdx - 1)).getBubble();
    }
}
