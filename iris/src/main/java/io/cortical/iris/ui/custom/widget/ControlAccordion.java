package io.cortical.iris.ui.custom.widget;

import javafx.scene.control.Accordion;
import javafx.scene.control.Skin;

public class ControlAccordion extends Accordion {
    public static final int DEFAULT_HEIGHT = 72;
    public static final double SPACING = 5;
    
    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        ControlAccordionSkin skin = new ControlAccordionSkin(this);
        return skin;
    }
}
