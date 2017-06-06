package io.cortical.iris.ui.custom.radialmenu;

import javafx.scene.paint.Paint;
import javafx.scene.text.Font;


public interface Graphic {
    /**
     * Returns the graphic's text if any
     * @return
     */
    public String getText();
    /**
     * Returns the font of the graphic 
     * @param f
     */
    public void setFont(Font f);
    /**
     * Sets the fill color 
     * @param p
     */
    public void setTextFill(Paint p);
    /**
     * Invokes the "selected" state or not
     * @param b
     */
    public void setSelected(boolean b);
    /**
     * Invokes the "disabled" state or not
     * @param b
     */
    public void setDisable(boolean b);
    /**
     * Returns a flag indicating whether the menu item is selected
     * @return
     */
    public boolean isSelected();
}
