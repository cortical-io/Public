package io.cortical.iris.ui.custom.property;

import java.util.Optional;

import javax.swing.event.ChangeEvent;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * As distinguished from a {@link ChangeEvent}, {@code OccurrenceEvent}s express the idea 
 * of something happening, whether it is a change in state or some observable value or not.
 * This avoids the use of binary state objects and having to set them either false, or having
 * to propagate a false change in order to use the property infrastructure.
 * 
 * @author cogmission
 *
 * @param <T> a subclass of {@link Occurrence}
 */
@SuppressWarnings("all")
public interface OccurrenceListener<T extends Optional<Occurrence>> {

    /**
     * This method needs to be provided by an implementation of
     * {@code OccurenceListener}. It is called if the value of an
     * {@link ObservableValue} is set.
     * <p>
     * As distinguished from a {@link ChangeEvent}, {@code OccurrenceEvent}s express the idea 
     * of something happening, whether it is a change in state or some observable value or not.
     * This avoids the use of binary state objects and having to set them either false, or having
     * to propagate a false change in order to use the property infrastructure.
     * <p>
     * In general is is considered bad practice to modify the observed value in
     * this method.
     *
     * @param observable
     *            The {@code ObservableValue} which value changed
     * @param oldValue
     *            The old value
     * @param newValue
     *            The new value
     */
    void occurred(ObservableValue<? extends T> observable, T oldValue, T newValue);
}
