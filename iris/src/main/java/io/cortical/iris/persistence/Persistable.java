package io.cortical.iris.persistence;

import java.io.Serializable;

import io.cortical.iris.window.Window;

public interface Persistable extends Serializable {
    /**
     * Main method for saving the configuration of a given {@link Window}.
     * The invoked serialization routine actually stores the properties of
     * a given Window and restores them into a {@link WindowConfig} object
     * which contains all the properties and is subsequently used by 
     * {@link Window#configure(WindowConfig)} to set the properties on a 
     * Window.
     * 
     * @param config    container for a given {@code Window}'s properties
     */
    public void serialize(WindowConfig config);
    /**
     * Main method for restoring the properties of a given {@link Window}.
     * This method retrieves the serialized properties and stores them 
     * within the specified {@link WindowConfig}.
     * 
     * @param config
     */
    public void deSerialize(WindowConfig config);
}
