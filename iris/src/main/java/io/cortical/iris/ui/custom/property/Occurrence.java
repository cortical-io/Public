package io.cortical.iris.ui.custom.property;

/**
 * Interface to be used with {@link OccurrenceProperty} event propagation.
*/
@FunctionalInterface
public interface Occurrence {
    /**
     * Returns the index of the current occurrence of the
     * event which propagates this {@code State}.
     * @return  the current occurrence index
     */
    public int sequence();
}
