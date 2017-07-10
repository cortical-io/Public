package io.cortical.iris.message;

import io.cortical.iris.window.Window;

/**
 * A container object for transporting events or messages
 * on the message bus.
 * 
 * @author cogmission
 * @param <T>
 */
public class Payload {
    public static final Payload DUMMY_PAYLOAD = new Payload("Dummy");
    
    Object payload;
    
    String description;
    
    Window window;
    
    /**
     * Constructs an empty {@code Payload} object.
     */
    public Payload() {
        this(null);
    }
    
    /**
     * Constructs a {@code Payload} object containing "o"
     * as its payload.
     * 
     * @param o
     */
    public Payload(Object o) {
        this.payload = o;
    }
    
    /**
     * Sets this {@code Payload} object's payload.
     * @param o
     */
    public void setPayload(Object o) {
        this.payload = o;
    }
    
    /**
     * Returns this {@code Payload} object's payload.
     * @return the payload
     */
    public Object getPayload() {
        return payload;
    }
    
    /**
     * Sets the description string.
     * @param desc
     */
    public void setDescription(String desc) {
        this.description = desc;
    }
    
    /**
     * Returns the description string.
     * @return
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets this payload's window
     * @param w
     */
    public void setWindow(Window w) {
        this.window = w;
    }
    
    /**
     * Returns this payload's window
     * @return
     */
    public Window getWindow() {
        return window;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((payload == null) ? 0 : payload.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        Payload other = (Payload)obj;
        if(payload == null) {
            if(other.payload != null)
                return false;
        } else if(!payload.equals(other.payload))
            return false;
        return true;
    }
}
