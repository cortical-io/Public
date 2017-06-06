package io.cortical.iris.ui.custom.property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.event.ChangeEvent;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;


/**
 * As distinguished from a {@link ChangeEvent}, {@code OccurrenceEvent}s express the idea 
 * of something happening, whether it is a change in state; some observable value or not.
 * This avoids the use of binary state objects and having to set them either false, or having
 * to propagate a false change or "null" in order to renew use of the property infrastructure.
 * 
 * Objects of this class delegate to an {@link ObjectProperty} underneath to be able to use
 * the property infrastructure. Because the underlying delegate is organized around changes,
 * this class feeds it an ever-incrementing integer as the "changed" value allowing users of
 * this class to simply call {@link #set()} repeatedly without worrying over a "changed" value.
 * 
 * 
 * @author cogmission
 *
 */
public class OccurrenceProperty {
    private SimpleObjectProperty<Optional<Occurrence>> delegate = new SimpleObjectProperty<>();
    
    private Map<OccurrenceListener<Optional<Occurrence>>, OccurrenceWrapper> listenerMap = new HashMap<>();
    
    private int sequence;
    
    /**
     * The main advantage of this property is this method. Since it is not based on "changes"
     * but simply on occurrences, the methods here can be propagated by simply calling set
     * without having to indicate the change of something.
     */
    public void set() {
        ++sequence;
        delegate.set(Optional.of(() -> { return sequence == Integer.MAX_VALUE ? 0 : sequence; }));
    }
    
    /**
     * This method can be used much like the methods on other properties such as {@link ObjectPropert}.
     * @param optional  an Optional containing an Occurrence.
     */
    public void set(Optional<Occurrence> optional) {
        ++sequence;
        delegate.set(optional);
    }
    
    /**
     * This method can be used much like the methods on other properties such as {@link ObjectPropert}.
     * @param optional  an Optional containing an Occurrence.
     */
    public void setValue(Optional<Occurrence> optional) {
        ++sequence;
        delegate.setValue(optional);
    }
    
    /**
     * Returns the next value to be fed to the underlying delegate as a change.
     * @return the next value to be fed to the underlying delegate as a change.
     */
    int next() {
        return sequence + 1;
    }
    
    /**
     * Can be used to check, if a {@code Property} is bound.
     * @see #bind(javafx.beans.value.ObservableValue)
     *
     * @return {@code true} if the {@code Property} is bound, {@code false}
     *         otherwise
     */
    public boolean isBound() {
        return delegate.isBound();
    }

    /**
     * Create a uni-direction binding for this {@code Property}.
     * <p>
     * Note that JavaFX has all the bind calls implemented through weak listeners. This means the bound property
     * can be garbage collected and stopped from being updated.
     * 
     * @param observable
     *            The observable this {@code Property} should be bound to.
     * @throws NullPointerException
     *             if {@code observable} is {@code null}
     */
    public void bind(final ObservableValue<? extends Optional<Occurrence>> newObservable) {
        delegate.bind(newObservable);
    }
    
    /**
     * Remove the unidirectional binding for this {@code Property}.
     * 
     * If the {@code Property} is not bound, calling this method has no effect.
     * @see #bind(javafx.beans.value.ObservableValue)
     */
    void unbind() {
        delegate.unbind();
    }

    /**
     * Create a bidirectional binding between this {@code Property} and another
     * one.
     * Bidirectional bindings exists independently of unidirectional bindings. So it is possible to
     * add unidirectional binding to a property with bidirectional binding and vice-versa. However, this practice is
     * discouraged.
     * <p>
     * It is possible to have multiple bidirectional bindings of one Property.
     * <p>
     * JavaFX bidirectional binding implementation use weak listeners. This means bidirectional binding does not prevent
     * properties from being garbage collected.
     * 
     * @param other
     *            the other {@code Property}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code other} is {@code this}
     */
    void bindBidirectional(Property<Optional<Occurrence>> other) {
        delegate.bindBidirectional(other);
    }

    /**
     * Remove a bidirectional binding between this {@code Property} and another
     * one.
     * 
     * If no bidirectional binding between the properties exists, calling this
     * method has no effect.
     *
     * It is possible to unbind by a call on the second property. This code will work:
     *
     * <blockquote><pre>
     *     property1.bindBirectional(property2);
     *     property2.unbindBidirectional(property1);
     * </pre></blockquote>
     *
     * @param other
     *            the other {@code Property}
     * @throws NullPointerException
     *             if {@code other} is {@code null}
     * @throws IllegalArgumentException
     *             if {@code other} is {@code this}
     */
    void unbindBidirectional(Property<Optional<Occurrence>> other) {
        delegate.unbindBidirectional(other);
    }
    
    /**
     * adds the specified {@link InvalidationListener}.
     * @param listener  the InvalidationListener to add.
     */
    public void addListener(InvalidationListener listener) {
        delegate.addListener(listener);
    }

    /**
     * Removes the specified {@link InvalidationListener}.
     * @param listener  the listener to remove.
     */
    public void removeListener(InvalidationListener listener) {
        delegate.removeListener(listener);
    }
    
    /**
     * Adds an {@link OccurrenceListener} to receive {@link Occurrence}s 
     * from this property.
     * @param listener  the listener to add.
     */
    public void addListener(OccurrenceListener<Optional<Occurrence>> listener) {
        OccurrenceWrapper wrapper = new OccurrenceWrapper(listener);
        listenerMap.put(listener, wrapper);
        delegate.addListener(wrapper);
    }
    
    /**
     * Removes the specified {@link OccurrenceListener} from the list of listeners
     * to receive new {@link Occurrence} notifications.
     * @param listener  the listener to remove.
     */
    public void removeListener(OccurrenceListener<Optional<Occurrence>> listener) {
        if(!listenerMap.containsKey(listener)) return;
        delegate.removeListener(listenerMap.remove(listener));
    }
    
    /**
     * Wraps an {@link OccurrenceListener} in a {@link ChangeListener} for use with
     * bridging between JavaFX Properties organized around "changes".
     */
    private class OccurrenceWrapper implements ChangeListener<Optional<Occurrence>> {
        private OccurrenceListener<Optional<Occurrence>> wrapped;
        
        private OccurrenceWrapper(OccurrenceListener<Optional<Occurrence>> l) {
            if(l == null) {
                throw new NullPointerException("Can't add a null listener");
            }
            this.wrapped = l;
        }
        
        @Override
        public void changed(
            ObservableValue<? extends Optional<Occurrence>> observable, 
                Optional<Occurrence> oldValue,
                    Optional<Occurrence> newValue) {
            
            wrapped.occurred(observable, oldValue, newValue);
            
        }
        
    }
}
