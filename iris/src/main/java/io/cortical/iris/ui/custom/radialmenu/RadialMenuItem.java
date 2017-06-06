/**
 * RadialMenuItem.java
 *
 * Copyright (c) 2011-2015, JFXtras
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the organization nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.cortical.iris.ui.custom.radialmenu;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;

/**
 * This class has been modified from the original to include more functionality,
 * get rid of some problems (such as use of deprecated APIs) and complete 
 * functionality started but not yet finished. This documentation is also added 
 * as the original contained no documentation.
 * 
 * @author jfxtras
 * @author cogmission
 *
 */
public class RadialMenuItem extends Group implements ChangeListener<Object> {

    protected DoubleProperty startAngle = new SimpleDoubleProperty();

    /** Angle size in degrees which each menuitem occupies */ 
    protected double menuSize;

    protected DoubleProperty innerRadius = new SimpleDoubleProperty();

    protected DoubleProperty radius = new SimpleDoubleProperty();

    protected DoubleProperty offset = new SimpleDoubleProperty();

    protected ObjectProperty<Paint> backgroundMouseOnColor = new SimpleObjectProperty<Paint>();

    protected ObjectProperty<Paint> backgroundColor = new SimpleObjectProperty<Paint>();

    protected BooleanProperty backgroundVisible = new SimpleBooleanProperty(
                    true);

    protected BooleanProperty strokeVisible = new SimpleBooleanProperty(true);

    protected BooleanProperty clockwise = new SimpleBooleanProperty();

    protected ObjectProperty<Paint> strokeColor = new SimpleObjectProperty<Paint>();

    protected ObjectProperty<Paint> strokeMouseOnColor = new SimpleObjectProperty<Paint>();
    
    protected ObjectProperty<Paint> textColor = new SimpleObjectProperty<Paint>();
    
    protected ObjectProperty<Paint> textMouseOnColor = new SimpleObjectProperty<Paint>();
    
    protected ObjectProperty<Paint> disabledFillColor = new SimpleObjectProperty<Paint>();
    
    protected ObjectProperty<Paint> disabledTextColor = new SimpleObjectProperty<Paint>();
    
    protected BooleanProperty selectedProperty = new SimpleBooleanProperty();
    
    protected ObjectProperty<Paint> selectedFillColor = new SimpleObjectProperty<Paint>();
    
    protected ObjectProperty<Paint> selectedTextColor = new SimpleObjectProperty<Paint>();
    
    protected ObjectProperty<Font> textFont = new SimpleObjectProperty<Font>();
    
    protected ObjectProperty<Graphic> graphicProperty = new SimpleObjectProperty<Graphic>();
    
    protected ObjectProperty<MouseEvent> mouseHoverProperty = new SimpleObjectProperty<>();
    
    protected MoveTo moveTo;

    protected ArcTo arcToInner;

    protected ArcTo arcTo;

    protected LineTo lineTo;

    protected LineTo lineTo2;

    protected double innerStartX;

    protected double innerStartY;

    protected double innerEndX;

    protected double innerEndY;

    protected boolean innerSweep;

    protected double startX;

    protected double startY;

    protected double endX;

    protected double endY;

    protected boolean sweep;

    protected double graphicX;

    protected double graphicY;
    
    protected double textX;
    
    protected double textY;

    protected double translateX;

    protected double translateY;

    protected boolean mouseOn = false;

    protected Path path;

    protected Graphic graphic;

    protected Label text;
    
    
    /**
     * Constructs a new {@code RadialMenu} with a default menu size
     * of 45 degrees (i.e. 4 menu items). see {@link #menuSize}
     */
    public RadialMenuItem() {
        this.menuSize = 45;
        this.innerRadius.addListener(this);
        this.radius.addListener(this);
        this.offset.addListener(this);
        this.backgroundVisible.addListener(this);
        this.strokeVisible.addListener(this);
        this.clockwise.addListener(this);
        this.backgroundColor.addListener(this);
        this.strokeColor.addListener(this);
        this.backgroundMouseOnColor.addListener(this);
        this.strokeMouseOnColor.addListener(this);
        this.startAngle.addListener(this);
        this.textColor.addListener(this);
        this.textMouseOnColor.addListener(this);
        this.textFont.addListener(this);
        this.disabledFillColor.addListener(this);
        this.disabledTextColor.addListener(this);
        this.selectedFillColor.addListener(this);
        this.selectedTextColor.addListener(this);

        this.path = new Path();
        this.moveTo = new MoveTo();
        this.arcToInner = new ArcTo();
        this.arcTo = new ArcTo();
        this.lineTo = new LineTo();
        this.lineTo2 = new LineTo();

        this.path.getElements().add(this.moveTo);
        this.path.getElements().add(this.arcToInner);
        this.path.getElements().add(this.lineTo);
        this.path.getElements().add(this.arcTo);
        this.path.getElements().add(this.lineTo2);

        this.getChildren().add(this.path);

        this.setOnMouseEntered(new EventHandler<MouseEvent>() {

            @Override
            public void handle(final MouseEvent arg0) {
                RadialMenuItem.this.mouseOn = true;
                mouseHoverProperty.set(arg0);
                RadialMenuItem.this.redraw();
            }
        });

        this.setOnMouseExited(new EventHandler<MouseEvent>() {

            @Override
            public void handle(final MouseEvent arg0) {
                RadialMenuItem.this.mouseOn = false;
                mouseHoverProperty.set(arg0);
                RadialMenuItem.this.redraw();
            }
        });

    }

    /**
     * Constructs a new {@code RadialMenuItem}
     * 
     * @param menuSize      Angle in degrees, which each menu item occupies
     * @param graphic       A graphic to display
     */
    public RadialMenuItem(final double menuSize, final Graphic graphic) {

        this();

        this.menuSize = menuSize;
        this.graphic = graphic;
        if (this.graphic != null)
            this.getChildren().add((Node)this.graphic);
        
        this.graphicProperty.set(graphic);
        
        this.graphicProperty.addListener(this);
        this.disableProperty().addListener(this);
        this.selectedProperty.addListener(this);

        this.redraw();
    }

    /**
     * Constructs a new {@code RadialMenuItem}
     * 
     * @param menuSize      Angle in degrees, which each menu item occupies
     * @param graphic       A graphic to display
     * @param actionHandler A callback to be performed upon mouse interaction
     */
    public RadialMenuItem(final double menuSize, final Graphic graphic,
        final EventHandler<ActionEvent> actionHandler) {

        this(menuSize, graphic);
        this.addEventHandler(MouseEvent.MOUSE_CLICKED,
                        new EventHandler<MouseEvent>() {

            @Override
            public void handle(final MouseEvent paramT) {
                actionHandler.handle(new ActionEvent(
                                paramT.getSource(), paramT.getTarget()));
            }
        });
        this.redraw();
    }

    /**
     * Constructs a new {@code RadialMenuItem}
     * 
     * @param menuSize          Angle in degrees, which each menu item occupies
     * @param text              Text to be displayed
     * @param graphic           A node used as a graphic to display
     * @param disabledBGFill    Paint used for disabled {@code RadialMenuItem}s.
     * @param disabledTextFill  Paint used for disabled {@code RadialMenuItem}'s text.
     * @param selectedFill      Paint used for selected {@code RadialMenuItem}s.
     * @param selectedTextFill  Paint used for selected {@code RadialMenuItem}'s text.
     */
    public RadialMenuItem(final double menuSize, final String text,
        final Graphic graphic, Paint disabledBGFill, Paint disabledTextFill, Paint selectedFill, Paint selectedTextFill) {

        this(menuSize, graphic);
        
        this.disabledFillColor.set(disabledBGFill);
        this.disabledTextColor.set(disabledTextFill);
        this.selectedFillColor.set(selectedFill);
        this.selectedTextColor.set(selectedTextFill);
        
        this.disableProperty().addListener(this);
        this.selectedProperty.addListener(this);
        
        if(text != null) {
            this.text = new Label(text);
            this.text.setOpacity(1.0);
        }
        
        getChildren().add(this.text);
        this.redraw();
    }
    
    /**
     * Constructs a new {@code RadialMenuItem}
     * 
     * @param menuSize      Angle in degrees, which each menu item occupies
     * @param text          Text to be displayed
     * @param graphic       A graphic to display
     * @param actionHandler A callback to be performed upon mouse interaction
     */
    public RadialMenuItem(final double menuSize, final String text,
        final Graphic graphic, final EventHandler<ActionEvent> actionHandler) {

        this(menuSize, graphic, actionHandler);

        if(text != null) {
            this.text = new Label(text);
            this.text.setOpacity(1.0);
        }
        
        getChildren().add(this.text);
        this.redraw();
    }
    
    /**
     * Sets the selected property to have the specified
     * value.
     * @param b
     */
    public void setSelected(boolean b) {
        this.selectedProperty.set(b);
    }
    
    /**
     * Returns a flag indicating whether this {@code RadialMenuItem}
     * is selected or not.
     * 
     * @return
     */
    public boolean isSelected() {
        return selectedProperty.get();
    }
    
    /**
     * Returns the property granting selection state. 
     * @return
     */
    public BooleanProperty selectedProperty() {
        return this.selectedProperty;
    }
    
    /**
     * Returns the property which is set when the mouse either
     * enters or exits this RadialMenuItem.
     */
    public ObjectProperty<MouseEvent> mouseHoverProperty() {
        return mouseHoverProperty;
    }
    
    /**
     * Returns the property containing the graphic if any exists.
     * @return
     */
    ObjectProperty<Graphic> graphicProperty() {
        return this.graphicProperty;
    }
    
    DoubleProperty innerRadiusProperty() {
        return this.innerRadius;
    }

    DoubleProperty radiusProperty() {
        return this.radius;
    }

    DoubleProperty offsetProperty() {
        return this.offset;
    }

    ObjectProperty<Paint> backgroundMouseOnColorProperty() {
        return this.backgroundMouseOnColor;
    }

    public ObjectProperty<Paint> strokeMouseOnColorProperty() {
        return this.strokeMouseOnColor;
    }

    ObjectProperty<Paint> backgroundColorProperty() {
        return this.backgroundColor;
    }

    public BooleanProperty clockwiseProperty() {
        return this.clockwise;
    }

    ObjectProperty<Paint> strokeColorProperty() {
        return this.strokeColor;
    }

    public BooleanProperty strokeVisibleProperty() {
        return this.strokeVisible;
    }
    
    public ObjectProperty<Paint> textColorProperty() {
        return this.textColor;
    }
    
    public ObjectProperty<Paint> textMouseOnColorProperty() {
        return this.textMouseOnColor;
    }
    
    public ObjectProperty<Font> textFontProperty() {
        return this.textFont;
    }

    public BooleanProperty backgroundVisibleProperty() {
        return this.backgroundVisible;
    }
    
    public ObjectProperty<Paint> disabledFillProperty() {
        return this.disabledFillColor;
    }
    
    public ObjectProperty<Paint> disabledTextFillProperty() {
        return this.disabledTextColor;
    }
    
    public ObjectProperty<Paint> selectedFillProperty() {
        return this.selectedFillColor;
    }
    
    public ObjectProperty<Paint> selectedTextFillProperty() {
        return this.selectedTextColor;
    }

    public Graphic getGraphic() {
        return this.graphic;
    }
    
    /**
     * Returns the Path that colors the background
     * @return
     */
    public Path getBackground() {
        return this.path;
    }

    public void setStartAngle(final double angle) {
        this.startAngle.set(angle);
    }

    public DoubleProperty startAngleProperty() {
        return this.startAngle;
    }

    public void setGraphic(final Graphic graphic) {
        if (this.graphic != null) {
            this.getChildren().remove(this.graphic);
        }
        this.graphic = graphic;
        if (this.graphic != null) {
            this.getChildren().add((Node)graphic);
        }
        this.redraw();
    }

    public void setText(final String text) {
        if(this.text != null) {
            Label t = (Label)getChildren().get(getChildren().indexOf(this.text));
            if(t != null) t.setText(text);
        }else{
            this.text = new Label(text);
            getChildren().add(this.text);
        }
        this.redraw();
    }

    public String getText() {
        if(this.text == null) {
            if(this.graphic != null) {
                return ((Graphic)graphic).getText();
            }
            return null;
        }
        
        return text.getText();
    }

    protected void redraw() {

        this.path.setStroke(this.strokeVisible.get() ? 
            (this.mouseOn && this.strokeMouseOnColor.get() != null ? 
                this.strokeMouseOnColor.get() : this.strokeColor.get()) : 
                    Color.TRANSPARENT);

        if(this.text != null) {
            this.text.setFont(textFont.get());
            this.text.setTextFill(this.mouseOn ? this.textMouseOnColor.get() : this.textColor.get());
            if(this.isDisable()) {
                this.text.setTextFill(isSelected() ? selectedTextColor.get() : disabledTextColor.get());
                // Can be disabled AND selected
                this.path.setFill(this.disabledFillColor.get());
            }else if(this.isSelected()) {
                this.text.setTextFill(selectedTextColor.get());
                this.path.setFill(this.selectedFillColor.get());
            }
        }

        this.computeCoordinates();

        this.update();

    }

    protected void update() {
        final double innerRadiusValue = this.innerRadius.get();
        final double radiusValue = this.radius.get();

        this.moveTo.setX(this.innerStartX + this.translateX);
        this.moveTo.setY(this.innerStartY + this.translateY);

        this.arcToInner.setX(this.innerEndX + this.translateX);
        this.arcToInner.setY(this.innerEndY + this.translateY);
        this.arcToInner.setSweepFlag(this.innerSweep);
        this.arcToInner.setRadiusX(innerRadiusValue);
        this.arcToInner.setRadiusY(innerRadiusValue);

        this.lineTo.setX(this.startX + this.translateX);
        this.lineTo.setY(this.startY + this.translateY);

        this.arcTo.setX(this.endX + this.translateX);
        this.arcTo.setY(this.endY + this.translateY);
        this.arcTo.setSweepFlag(this.sweep);

        this.arcTo.setRadiusX(radiusValue);
        this.arcTo.setRadiusY(radiusValue);

        this.lineTo2.setX(this.innerStartX + this.translateX);
        this.lineTo2.setY(this.innerStartY + this.translateY);

        if (this.graphic != null) {
            ((Node)this.graphic).setTranslateX(this.graphicX + this.translateX);
            ((Node)this.graphic).setTranslateY(this.graphicY + this.translateY);
        }
        
        if(this.text != null) {
            this.text.setTranslateX(this.textX + this.translateX);
            this.text.setTranslateY(this.textY + this.translateY);
        }

        // this.translateXProperty().set(this.translateX);
        // this.translateYProperty().set(this.translateY);
    }

    protected void computeCoordinates() {
        final double innerRadiusValue = this.innerRadius.get();
        final double startAngleValue = this.startAngle.get();

        final double graphicAngle = startAngleValue + (this.menuSize / 2.0);
        final double radiusValue = this.radius.get();

        final double graphicRadius = innerRadiusValue
                        + (radiusValue - innerRadiusValue) / 2.0;

        final double offsetValue = this.offset.get();

        if (!this.clockwise.get()) {
            this.innerStartX = innerRadiusValue
                            * Math.cos(Math.toRadians(startAngleValue));
            this.innerStartY = -innerRadiusValue
                            * Math.sin(Math.toRadians(startAngleValue));
            this.innerEndX = innerRadiusValue
                            * Math.cos(Math.toRadians(startAngleValue + this.menuSize));
            this.innerEndY = -innerRadiusValue
                            * Math.sin(Math.toRadians(startAngleValue + this.menuSize));

            this.innerSweep = false;

            this.startX = radiusValue
                            * Math.cos(Math.toRadians(startAngleValue + this.menuSize));
            this.startY = -radiusValue
                            * Math.sin(Math.toRadians(startAngleValue + this.menuSize));
            this.endX = radiusValue * Math.cos(Math.toRadians(startAngleValue));
            this.endY = -radiusValue
                            * Math.sin(Math.toRadians(startAngleValue));

            this.sweep = true;

            if (this.graphic != null) {
                this.graphicX = graphicRadius
                                * Math.cos(Math.toRadians(graphicAngle))
                                - ((Node)this.graphic).getBoundsInParent().getWidth() / 2.0;
                this.graphicY = -graphicRadius
                                * Math.sin(Math.toRadians(graphicAngle))
                                - ((Node)this.graphic).getBoundsInParent().getHeight() / 2.0;

            }
            
            if(this.text != null) {
                this.textX = graphicRadius
                                * Math.cos(Math.toRadians(graphicAngle))
                                - this.text.getBoundsInParent().getWidth() / 2.0;
                this.textY = -graphicRadius
                                * Math.sin(Math.toRadians(graphicAngle))
                                - this.text.getBoundsInParent().getHeight() / 2.0; 
            }
            
            this.translateX = offsetValue
                            * Math.cos(Math.toRadians(startAngleValue
                                            + (this.menuSize / 2.0)));
            this.translateY = -offsetValue
                            * Math.sin(Math.toRadians(startAngleValue
                                            + (this.menuSize / 2.0)));

        } else if (this.clockwise.get()) {
            this.innerStartX = innerRadiusValue
                            * Math.cos(Math.toRadians(startAngleValue));
            this.innerStartY = innerRadiusValue
                            * Math.sin(Math.toRadians(startAngleValue));
            this.innerEndX = innerRadiusValue
                            * Math.cos(Math.toRadians(startAngleValue + this.menuSize));
            this.innerEndY = innerRadiusValue
                            * Math.sin(Math.toRadians(startAngleValue + this.menuSize));

            this.innerSweep = true;

            this.startX = radiusValue
                            * Math.cos(Math.toRadians(startAngleValue + this.menuSize));
            this.startY = radiusValue
                            * Math.sin(Math.toRadians(startAngleValue + this.menuSize));
            this.endX = radiusValue * Math.cos(Math.toRadians(startAngleValue));
            this.endY = radiusValue * Math.sin(Math.toRadians(startAngleValue));

            this.sweep = false;

            if (this.graphic != null) {
                this.graphicX = graphicRadius
                                * Math.cos(Math.toRadians(graphicAngle))
                                - ((Node)this.graphic).getBoundsInParent().getWidth() / 2.0;
                this.graphicY = graphicRadius
                                * Math.sin(Math.toRadians(graphicAngle))
                                - ((Node)this.graphic).getBoundsInParent().getHeight() / 2.0;

            }

            this.translateX = offsetValue
                            * Math.cos(Math.toRadians(startAngleValue
                                            + (this.menuSize / 2.0)));
            this.translateY = offsetValue
                            * Math.sin(Math.toRadians(startAngleValue
                                            + (this.menuSize / 2.0)));
        }

    }

    public double getMenuSize() {
        return this.menuSize;
    }

    @Override
    public void changed(final ObservableValue<? extends Object> arg0,
        final Object arg1, final Object arg2) {
        this.redraw();

    }

}
