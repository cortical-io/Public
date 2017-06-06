package io.cortical.iris.message;

import java.util.List;

import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.retina.model.Model;
import javafx.util.Pair;

public class ClipboardMessage extends Payload {
    private Pair<List<Bubble.Type>, List<String>> content;
    private Model result;
    
    public ClipboardMessage(Pair<List<Bubble.Type>, List<String>> content) {
        this(content, null);
    }
    
    public ClipboardMessage(Pair<List<Bubble.Type>, List<String>> content, Model result) {
        this.content = content;
        this.result = result;
    }
    
    /**
     * @return the content
     */
    public Pair<List<Bubble.Type>, List<String>> getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(Pair<List<Bubble.Type>, List<String>> content) {
        this.content = content;
    }

    /**
     * @return the result
     */
    public Model getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(Model result) {
        this.result = result;
    }

    
}
