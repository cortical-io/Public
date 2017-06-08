package io.cortical.iris.window;

import io.cortical.iris.view.View;
import javafx.scene.layout.Region;

/**
 * Contract promises to return a Region implementation for
 * back panel purposes such as an info panel for a particular
 * view.
 * 
 * @author cogmission
 * @see View
 * @see Window#getOrCreateBackPanel()
 */
public interface BackPanelSupplier {
    /**
     * Contract promises to return a Region implementation for
     * back panel purposes such as an info panel for a particular
     * view.
     * 
     * @return  a back panel
     */
    public Region getOrCreateBackPanel();
}
