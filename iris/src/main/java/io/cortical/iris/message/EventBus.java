package io.cortical.iris.message;

import java.util.regex.Pattern;

import org.bushe.swing.event.EventSubscriber;
import org.bushe.swing.event.EventTopicSubscriber;
import org.bushe.swing.event.ThreadSafeEventService;

/**
 * Custom {@link org.bushe.swing.event.EventBus} subclass which simplifies 
 * some of the semantics and is specifically suited for use within Iris.
 * 
 * @author cogmission
 *
 * @param <T>
 */
public class EventBus<T> extends ThreadSafeEventService {
    
    private static EventBus<Payload> INSTANCE = new EventBus<>();
    
    static {
        INSTANCE.setDefaultCacheSizePerClassOrTopic(0);
    }
    
    
    /**
     * Private singleton constructor
     */
    private EventBus() {}
    
    /**
     * Returns the singleton event bus instance.
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <P extends Payload> EventBus<P> get() {
        return (EventBus<P>)INSTANCE;
    }
    
    /**
     * Subscribes the specified {@link EventTopicSubscriber} to the topic specified.
     * @param topic     the topic for which the subscriber will be notified
     * @param e         the subscriber
     * @return  true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean subscribeTo(String topic, EventTopicSubscriber<T> e) {
        return super.subscribeStrongly(topic, e);
    }
    
    /**
     * Unsubscribes the specified {@link EventTopicSubscriber} to the topic specified.
     * @param topic
     * @param e
     * @return
     */
    public boolean unsubscribeTo(String topic, EventTopicSubscriber<T> e) {
        return super.unsubscribe(topic, e);
    }
    
    /**
     * Subscribes the specified {@link EventTopicSubscriber} to the topic specified,
     * using a weak reference.
     * @param topic             the topic for which the subscriber will be notified
     * @param e                 the subscriber
     * @return      true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean subscribeWeaklyTo(String topic, EventTopicSubscriber<T> e) {
        return super.subscribe(topic, e);
    }
    
    /**
     * Subscribes the specified {@link EventTopicSubscriber} to any topic whose description
     * matches the {@link Pattern} specified.
     * 
     * @param topicPattern      the pattern to match
     * @param e                 the subscriber
     * @return  true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean subscribeTo(Pattern topicPattern, EventTopicSubscriber<T> e) {
        return super.subscribeStrongly(topicPattern, e);
    }
    
    /**
     * Unsubscribes the specified {@link EventTopicSubscriber} to any topic whose description
     * matches the {@link Pattern} specified.
     * 
     * @param topicPattern      the pattern to match
     * @param e                 the subscriber
     * @return  true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean unsubscribeTo(Pattern topicPattern, EventTopicSubscriber<T> e) {
        return super.unsubscribe(topicPattern, e);
    }
    
    /**
     * Subscribes the specified {@link EventTopicSubscriber} to any topic whose description
     * matches the {@link Pattern} specified. The subscriber will subscribe weakly and the 
     * subscriber will eventually be garbage collected.
     * @param topicPattern      the pattern to match    
     * @param e                 the subscriber
     * @return true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean subscribeWeaklyTo(Pattern topicPattern, EventTopicSubscriber<T> e) {
        return super.subscribe(topicPattern, e);
    }
    
    /**
     * Subscribes the specified {@link EventSubscriber} to any event propagated by the specified
     * class or any subclass of that class.
     * @param clazz     the class or subclass therein to subscribe to.
     * @param e         the subscriber
     * @return  true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean subscribeTo(Class<?> clazz, EventSubscriber<T> e) {
        return super.subscribeStrongly(clazz, e);
    }
    
    /**
     * Unsubscribes the specified {@link EventSubscriber} to any event propagated by the specified
     * class or any subclass of that class.
     * @param clazz     the class or subclass therein to subscribe to.
     * @param e         the subscriber
     * @return  true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean unsubscribeTo(Class<?> clazz, EventSubscriber<T> e) {
        return super.unsubscribe(clazz, e);
    }
    
    /**
     * Subscribes the specified {@link EventSubscriber} to any event propagated by the specified
     * class exactly (only).
     * @param clazz     the class or subclass therein to subscribe to.
     * @param e         the subscriber
     * @return  true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean subscribeExactlyTo(Class<?> clazz, EventSubscriber<T> e) {
        return super.subscribeExactlyStrongly(clazz, e);
    }
    
    /**
     * Unsubscribes the specified {@link EventSubscriber} to any event propagated by the specified
     * class exactly (only).
     * @param clazz     the class or subclass therein to subscribe to.
     * @param e         the subscriber
     * @return  true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean unsubscribeExactlyTo(Class<?> clazz, EventSubscriber<T> e) {
        return super.unsubscribeExactly(clazz, e);
    }
    
    /**
     * Subscribes the specified {@link EventSubscriber} to any event propagated by the specified
     * class or its subclasses, using a weak subscriber which may be garbage collected.
     * @param clazz     the class or subclass therein to subscribe to.
     * @param e         the subscriber
     * @return  true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean subscribeWeaklyTo(Class<?> clazz, EventSubscriber<T> e) {
        return super.subscribe(clazz, e);
    }
    
    /**
     * Subscribes the specified {@link EventSubscriber} to any event propagated by the specified
     * class or exactly, using a weak subscriber which may be garbage collected.
     * @param clazz     the class or subclass therein to subscribe to.
     * @param e         the subscriber
     * @return  true if the subscriber was subscribed successfully, false otherwise
     */
    public boolean subscribeExactlyWeaklyTo(Class<?> clazz, EventSubscriber<T> e) {
        return super.subscribeExactly(clazz, e);
    }
    
    /**
     * Broadcasts the specified &#60;T&#62; to all registered subscribers.
     * @param t     the type to propagate.
     */
    public void broadcast(T t) {
        super.publish(t);
    }
    
    /**
     * Broadcasts the specified &#60;T&#62; to all registered subscribers of the 
     * specified topic.
     * 
     * @param topic     the topic subject for whose subscribers are being notified
     * @param t         the payload type being propagated
     */
    public void broadcast(String topic, T t) {
        super.publish(topic, t);
    }
}
