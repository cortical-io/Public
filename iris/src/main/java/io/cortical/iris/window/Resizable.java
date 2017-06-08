package io.cortical.iris.window;

/**
 * <p>
 * Indicates Nodes that resize themselves upon user interaction.
 * </p><p>
 * Implemented by Nodes which need to report changes in size
 * attributes required by the dynamic addition or removal of elements to
 * a given Node.
 * </p>
 * @author cogmission
 *
 */
public interface Resizable {
    /**
     * Returns the required height of this
     * Node which may vary from the preferred or
     * min/max and/or may be unavailable due to the use of
     * "unmanaged" nodes.
     * 
     * @return
     */
    public double computeHeight();
}
