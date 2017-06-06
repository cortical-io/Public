package io.cortical.iris.persistence;

import java.util.EventListener;

/**
 * Handler "Chain-of-Responsibility" style sequential 
 * post-deserialization handlers.
 * 
 *
 * @param <T> the event class this handler can handle
 */
@FunctionalInterface
public interface ConfigHandler<T extends WindowConfig> extends EventListener {
    /**
     * Invoked when a specific event of the type for which this handler is
     * registered happens.
     *
     * @param event the event which occurred
     */
    void handle(T config);
}
