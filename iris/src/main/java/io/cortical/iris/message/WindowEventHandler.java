package io.cortical.iris.message;

import io.cortical.iris.window.Window;
import javafx.event.Event;

@FunctionalInterface
public interface WindowEventHandler {
    public abstract void handle(Event e, Window w);
}
