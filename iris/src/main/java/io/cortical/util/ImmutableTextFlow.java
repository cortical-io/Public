package io.cortical.util;

import javafx.scene.Node;
import javafx.scene.text.TextFlow;

public class ImmutableTextFlow extends TextFlow {
    public ImmutableTextFlow() { }
    
    public ImmutableTextFlow(Node... children) {
        super(children);
    }
}
