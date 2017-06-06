/**
 * RadialMenu.java
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

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Base Menu class for displaying circular menus whose items are sectional
 * and whose sub menu items can be displayed in expanding concentric rings.
 * 
 * This class has been modified from the original to include more functionality,
 * get rid of some problems (such as use of deprecated APIs) and complete 
 * functionality started but not yet finished. This documentation is also added 
 * as the original contained no documentation.
 * 
 * @author jfxtras
 * @author cogmission
 *
 */
public class RadialMenu extends Group implements EventHandler<MouseEvent>,
ChangeListener<Object> {

    public enum CenterVisibility {
        ALWAYS, WITH_MENU, NEVER
    }

    protected List<RadialMenuItem> items = new ArrayList<RadialMenuItem>();
    protected DoubleProperty innerRadius;
    protected DoubleProperty radius;
    protected DoubleProperty offset;
    protected DoubleProperty initialAngle;
    protected ObjectProperty<Paint> backgroundFill;
    protected ObjectProperty<Paint> backgroundMouseOnFill;
    protected ObjectProperty<Paint> strokeMouseOnFill;
    protected ObjectProperty<Paint> strokeFill;
    protected ObjectProperty<Paint> textMouseOnFill;
    protected ObjectProperty<Paint> textFill;
    protected ObjectProperty<Font> textFont;
    protected BooleanProperty clockwise;
    protected BooleanProperty backgroundVisible;
    protected BooleanProperty strokeVisible;
    protected ObjectProperty<CenterVisibility> centerVisibility;
    protected ObjectProperty<Node> centerGraphic;
    protected Circle centerStrokeShape;
    protected Group centerGroup;
    protected Group itemGroup;
    private boolean mouseOn = false;
    private double lastInitialAngleValue;
    private double lastOffsetValue;
    /** Amount of rotation during menu showing */
    private double initialTwist;

    public Paint getBackgroundFill() {
        return this.backgroundFill.get();
    }

    public void setBackgroundFill(final Paint backgroundFill) {
        this.backgroundFill.set(backgroundFill);
    }

    public ObjectProperty<Paint> backgroundFillProperty() {
        return this.backgroundFill;
    }

    public Paint getBackgroundMouseOnFill() {
        return this.backgroundMouseOnFill.get();
    }

    public void setBackgroundMouseOnFill(final Paint backgroundMouseOnFill) {
        this.backgroundMouseOnFill.set(backgroundMouseOnFill);
    }

    public ObjectProperty<Paint> backgroundMouseOnFillProperty() {
        return this.backgroundMouseOnFill;
    }

    public Paint getStrokeMouseOnFill() {
        return this.strokeMouseOnFill.get();
    }

    public void setStrokeMouseOnFill(final Paint backgroundMouseOnFill) {
        this.strokeMouseOnFill.set(backgroundMouseOnFill);
    }

    public ObjectProperty<Paint> strokeMouseOnFillProperty() {
        return this.strokeMouseOnFill;
    }

    public Paint getStrokeFill() {
        return this.strokeFill.get();
    }

    public void setStrokeFill(final Paint strokeFill) {
        this.strokeFill.set(strokeFill);
    }

    public ObjectProperty<Paint> strokeFillProperty() {
        return this.strokeFill;
    }

    public void setTextFill(Paint textFill) {
        this.textFill.set(textFill);
    }
    public ObjectProperty<Paint> textFillProperty() {
        return this.textFill;
    }

    public void setTextMouseOnFill(Paint textMouseOnFill) {
        this.textMouseOnFill.set(textMouseOnFill);
    }

    public ObjectProperty<Paint> textMouseOnFillProperty() {
        return this.textMouseOnFill;
    }

    public void setTextFont(Font textFont) {
        this.textFont.set(textFont);
    }
    public ObjectProperty<Font> textFontProperty() {
        return this.textFont;
    }

    public Node getCenterGraphic() {
        return this.centerGraphic.get();
    }
    
    /**
     * Sets the amount of rotation when showing menu
     * @param degrees   the rotational amount in degrees
     */
    public void setInitialTwist(double degrees) {
        this.initialTwist = degrees;
    }
    
    /**
     * Returns the amount of rotation when showing menu
     * @return  the rotational amount in degrees
     */
    public double getInitialTwist() {
        return initialTwist;
    }

    public void setCenterGraphic(final Node graphic) {
        if (this.centerGraphic.get() != null) {
            this.centerGroup.getChildren().remove(this.centerGraphic.get());
        }
        if (graphic != null) {
            this.centerGroup.getChildren().add(graphic);
        }
        this.centerGraphic.set(graphic);
    }

    public ObjectProperty<Node> centerGraphicProperty() {
        return this.centerGraphic;
    }

    public double getInitialAngle() {
        return this.initialAngle.get();
    }

    public DoubleProperty initialAngleProperty() {
        return this.initialAngle;
    }

    public double getInnerRadius() {
        return this.innerRadius.get();
    }

    public DoubleProperty innerRadiusProperty() {
        return this.innerRadius;
    }

    public double getRadius() {
        return this.radius.get();
    }

    public DoubleProperty radiusProperty() {
        return this.radius;
    }

    public double getOffset() {
        return this.offset.get();
    }

    public DoubleProperty offsetProperty() {
        return this.offset;
    }

    public boolean isClockwise() {
        return this.clockwise.get();
    }

    public BooleanProperty clockwiseProperty() {
        return this.clockwise;
    }

    public boolean isBackgroundVisible() {
        return this.backgroundVisible.get();
    }

    public BooleanProperty backgroundVisibleProperty() {
        return this.backgroundVisible;
    }

    public BooleanProperty strokeVisibleProperty() {
        return this.strokeVisible;
    }

    public boolean isStrokeVisible() {
        return this.strokeVisible.get();
    }

    public ObjectProperty<CenterVisibility> centerVisibilityProperty() {
        return this.centerVisibility;
    }

    public CenterVisibility getCenterVisibility() {
        return this.centerVisibility.get();
    }

    public void setCenterVisibility(final CenterVisibility visibility) {
        this.centerVisibility.set(visibility);
    }

    public RadialMenu() {

    }

    public RadialMenu(final double initialAngle, final double innerRadius,
        final double radius, final double offset, final Paint bgFill,
        final Paint bgMouseOnFill, final Paint strokeFill,
        final Paint strokeMouseOnFill, final boolean clockwise,
        final CenterVisibility centerVisibility, final Node centerGraphic) {

        this(initialAngle, innerRadius, radius, offset, bgFill, bgMouseOnFill, strokeFill, 
                        strokeMouseOnFill, Color.BLACK, Color.BLACK, new Text().getFont(), clockwise, centerVisibility, centerGraphic);
    }

    public RadialMenu(final double initialAngle, final double innerRadius,
        final double radius, final double offset, final Paint bgFill,
        final Paint bgMouseOnFill, final Paint strokeFill,
        final Paint strokeMouseOnFill, final Paint textFill, final Paint textMouseOnFill, final Font textFont, final boolean clockwise,
        final CenterVisibility centerVisibility, final Node centerGraphic) {
        this.itemGroup = new Group();
        this.getChildren().add(this.itemGroup);

        this.initialAngle = new SimpleDoubleProperty(initialAngle);
        this.initialAngle.addListener(new ChangeListener<Number>() {

            @Override
            public void changed(
                final ObservableValue<? extends Number> paramObservableValue,
                final Number paramT1, final Number paramT2) {
                RadialMenu.this.setInitialAngle(paramObservableValue.getValue()
                                .doubleValue());
            }
        });

        this.innerRadius = new SimpleDoubleProperty(innerRadius);
        this.strokeFill = new SimpleObjectProperty<Paint>(strokeFill);
        this.strokeFill.addListener(this);

        this.radius = new SimpleDoubleProperty(radius);
        this.offset = new SimpleDoubleProperty(offset);
        this.clockwise = new SimpleBooleanProperty(clockwise);
        this.backgroundFill = new SimpleObjectProperty<Paint>(bgFill);
        this.backgroundFill.addListener(this);
        this.backgroundMouseOnFill = new SimpleObjectProperty<Paint>(
                        bgMouseOnFill);
        this.backgroundMouseOnFill.addListener(this);
        this.strokeMouseOnFill = new SimpleObjectProperty<Paint>(
                        strokeMouseOnFill);
        this.strokeMouseOnFill.addListener(this);
        this.strokeVisible = new SimpleBooleanProperty(true);
        this.backgroundVisible = new SimpleBooleanProperty(true);

        this.textFill = new SimpleObjectProperty<>(textFill);
        this.textMouseOnFill = new SimpleObjectProperty<>(textMouseOnFill);
        this.textFont = new SimpleObjectProperty<>(textFont);

        this.centerVisibility = new SimpleObjectProperty<CenterVisibility>(
                        centerVisibility);

        this.centerStrokeShape = new Circle();
        this.centerStrokeShape.setRadius(innerRadius);
        this.centerStrokeShape.setStroke(strokeFill);
        this.centerStrokeShape.setFill(bgFill);
        this.centerStrokeShape.radiusProperty()
        .bind(this.innerRadiusProperty());
        this.centerVisibility.addListener(this);

        this.strokeVisible.addListener(this);
        this.backgroundVisible.addListener(this);

        this.centerGroup = new Group();
        this.centerGroup.setOnMouseDragEntered(me -> {
            RadialMenu.this.mouseOn = true;
            RadialMenu.this.redraw();
        });
        this.centerGroup.setOnMouseExited(me -> {
            RadialMenu.this.mouseOn = false;
            RadialMenu.this.redraw();
        });
        this.centerGroup.setOnMouseClicked(me -> {
            final boolean visible = RadialMenu.this.itemGroup.isVisible();
            if(visible) {
                RadialMenu.this.hideRadialMenu();
            } else {
                RadialMenu.this.showRadialMenu();
            }

            me.consume();
        });
        this.centerGroup.getChildren().add(this.centerStrokeShape);
        this.centerGroup.visibleProperty().set(
            this.centerVisibility.get() == CenterVisibility.ALWAYS ||
                this.centerVisibility.get() == CenterVisibility.WITH_MENU);

        this.getChildren().add(this.centerGroup);
        this.centerGraphic = new SimpleObjectProperty<Node>(centerGraphic);
        this.setCenterGraphic(centerGraphic);

        this.saveStateBeforeAnimation();
    }

    public void setOnMenuItemMouseClicked(
        final EventHandler<? super MouseEvent> paramEventHandler) {
        for (final RadialMenuItem item : this.items) {
            item.setOnMouseClicked(paramEventHandler);
        }
    }

    public void setInitialAngle(final double angle) {
        this.initialAngle.set(angle);

        double angleOffset = this.initialAngle.get();
        for (final RadialMenuItem item : this.items) {
            item.setStartAngle(angleOffset);
            angleOffset = angleOffset + item.getMenuSize();
        }
    }

    public void setInnerRadius(final double radius) {
        this.innerRadius.set(radius);

    }

    public void setRadius(final double radius) {
        this.radius.set(radius);

    }
    
    public Shape getCenterStrokeShape() {
        return this.centerStrokeShape;
    }

    public void setOffset(final double offset) {
        this.offset.set(offset);
    }

    public void setBackgroundVisible(final boolean visible) {
        this.backgroundVisible.set(visible);

    }

    public void setStrokeVisible(final boolean visible) {
        this.strokeVisible.set(visible);

    }

    public void setBackgroundColor(final Paint color) {
        this.backgroundFill.set(color);
    }

    public void setBackgroundMouseOnColor(final Paint color) {
        this.backgroundMouseOnFill.set(color);
    }

    public void setStrokeMouseOnColor(final Paint color) {
        this.strokeMouseOnFill.set(color);
    }

    public void setStrokeColor(final Paint color) {
        this.strokeFill.set(color);
    }

    public void setClockwise(final boolean clockwise) {
        this.clockwise.set(clockwise);
    }

    public void addMenuItem(final RadialMenuItem item) {
        item.visibleProperty().bind(this.visibleProperty());
        item.backgroundColorProperty().bind(this.backgroundFill);
        item.backgroundMouseOnColorProperty().bind(this.backgroundMouseOnFill);
        item.strokeMouseOnColorProperty().bind(this.strokeMouseOnFill);
        item.innerRadiusProperty().bind(this.innerRadius);
        item.radiusProperty().bind(this.radius);
        item.offsetProperty().bind(this.offset);
        item.strokeColorProperty().bind(this.strokeFill);
        item.clockwiseProperty().bind(this.clockwise);
        item.backgroundVisibleProperty().bind(this.backgroundVisible);
        item.strokeVisibleProperty().bind(this.strokeVisible);
        item.textColorProperty().bind(this.textFill);
        item.textMouseOnColorProperty().bind(this.textMouseOnFill);
        item.textFontProperty().bind(this.textFontProperty());
        this.items.add(item);
        this.itemGroup.getChildren().add(this.itemGroup.getChildren().size(),
                        item);
        double angleOffset = this.initialAngle.get();
        for (final RadialMenuItem it : this.items) {
            it.setStartAngle(angleOffset);
            angleOffset = angleOffset + item.getMenuSize();
        }
        item.setOnMouseClicked(this);
    }

    public void removeMenuItem(final RadialMenuItem item) {
        this.items.remove(item);
        this.itemGroup.getChildren().remove(item);
        item.visibleProperty().unbind();
        item.backgroundColorProperty().unbind();
        item.backgroundMouseOnColorProperty().unbind();
        item.innerRadiusProperty().unbind();
        item.radiusProperty().unbind();
        item.offsetProperty().unbind();
        item.strokeColorProperty().unbind();
        item.clockwiseProperty().unbind();
        item.backgroundVisibleProperty().unbind();
        item.strokeVisibleProperty().unbind();
        item.removeEventHandler(MouseEvent.MOUSE_CLICKED, this);
    }

    public void removeMenuItem(final int itemIndex) {
        final RadialMenuItem item = this.items.get(itemIndex);
        this.removeMenuItem(item);
    }
    
    public RadialMenuItem getMenuItem(String text) {
        for(RadialMenuItem item : items) {
            if(item.getText().equals(text)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Resets all menu items to a non-disabled state
     */
    public void resetDisabledItems() {
        for(RadialMenuItem item : items) {
            item.setDisable(false);
            if(item.getGraphic() != null) {
                item.getGraphic().setDisable(false);
            }
        }
    }
    
    /**
     * Sets the specified item's disabled state to true after
     * setting all items to false
     * @param item
     */
    public void setDisabledMenuItem(RadialMenuItem item) {
        resetDisabledItems();
        item.setDisable(true);
        if(item.getGraphic() != null) {
            item.getGraphic().setDisable(true);
        }
    }
    
    /**
     * Returns the index of the specified {@link RadialMenuItem}
     * 
     * @param item
     * @return
     */
    public int indexOfItem(RadialMenuItem item) {
        for(int i = 0;i < items.size();i++) {
            RadialMenuItem it = items.get(i);
            if(it.equals(item)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Returns the {@link RadialMenuItem} whose index is one less than
     * the specified index. If the specified index is zero, then the last
     * item is returned.
     * 
     * @param index
     * @return
     */
    public RadialMenuItem getPreviousItem(int index) {
        return index == 0 ? items.get(items.size() - 1) : items.get(index - 1);
    }
    
    /**
     * Returns the {@link RadialMenuItem} whose index is one greater than
     * the specified index. If the specified index is the last item's index,
     * the zero'th index is then returned.
     * 
     * @param index
     * @return
     */
    public RadialMenuItem getNextItem(int index) {
        return index == items.size() - 1 ? items.get(0) : items.get(index + 1);
    }
    
    /**
     * Sets the specified {@link RadialMenuItem} to be selected
     * @param item
     */
    public void setSelectedMenuItem(RadialMenuItem item) {
        resetSelectedItems();
        item.setSelected(true);
        if(item.getGraphic() != null) {
            item.getGraphic().setSelected(true);
        }
    }
    
    /**
     * Sets the specified items to be disabled.
     * @param items
     */
    public void setDisabledMenuItems(RadialMenuItem... items) {
        for(RadialMenuItem item : items) {
            item.setDisable(true);
            if(item.getGraphic() != null) {
                item.getGraphic().setDisable(true);
            }
        }
    }
    
    /**
     * Returns the list of {@link RadialMenuItem}s
     * @return
     */
    public List<RadialMenuItem> getItems() {
        return items;
    }
    
    /**
     * Deselects all menu items
     */
    public void resetSelectedItems() {
        for(RadialMenuItem it : this.items) {
            it.setSelected(false);
            if(it.getGraphic() != null) {
                it.getGraphic().setSelected(false);
            }
        }
    }

    @Override
    public void handle(final MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            final RadialMenuItem item = (RadialMenuItem) event.getSource();
            item.setSelected(!item.isSelected());
            for (final RadialMenuItem it : this.items) {
                if (it != item) {
                    it.setSelected(false);
                }
            }
            if (!item.isSelected()) {
                this.hideRadialMenu();
            }
            event.consume();
        }
    }

    public void hideRadialMenu() {
        this.saveStateBeforeAnimation();

        final List<Animation> anim = new ArrayList<Animation>();

        FadeTransition fadeItemGroup = new FadeTransition(Duration.millis(300), this.itemGroup);
        fadeItemGroup.setFromValue(1);
        fadeItemGroup.setToValue(0);
        fadeItemGroup.setOnFinished(ae -> { RadialMenu.this.itemGroup.setVisible(false); });

        anim.add(fadeItemGroup);

        if (this.centerVisibility.get() == CenterVisibility.WITH_MENU) {
            FadeTransition fadeCenter = new FadeTransition(Duration.millis(300), this.centerGroup);
            fadeCenter.setFromValue(1);
            fadeCenter.setToValue(0);
            fadeCenter.setOnFinished(ae -> { RadialMenu.this.centerGroup.setVisible(false); });
            anim.add(fadeCenter);
        }

        ParallelTransition transition = new ParallelTransition();
        transition.getChildren().addAll(anim);

        transition.play();
    }

    public void showRadialMenu() {
        final List<Animation> anim = new ArrayList<Animation>();

        FadeTransition fade = new FadeTransition(Duration.millis(400), this.itemGroup);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        anim.add(fade);

        final Animation offset = new Timeline(new KeyFrame(Duration.ZERO,
                        new KeyValue(this.offsetProperty(), 0)), new KeyFrame(
                                        Duration.millis(300), new KeyValue(this.offsetProperty(),
                                                        this.lastOffsetValue)));
        anim.add(offset);

        final Animation angle = new Timeline(new KeyFrame(Duration.ZERO,
                        new KeyValue(this.initialAngleProperty(),
                                        this.lastInitialAngleValue  + initialTwist)), new KeyFrame(
                                                        Duration.millis(300), new KeyValue(this.initialAngleProperty(),
                                                                        this.lastInitialAngleValue)));
        anim.add(angle);

        if (this.centerVisibility.get() == CenterVisibility.WITH_MENU) {
            FadeTransition fadeCenter = new FadeTransition(Duration.millis(300), this.centerGroup);
            fadeCenter.setFromValue(0);
            fadeCenter.setToValue(1);
            anim.add(fadeCenter);

//             final Animation radius = new Timeline(new KeyFrame(Duration.ZERO,
//             new KeyValue(this.innerRadiusProperty(), 0)), new KeyFrame(
//             Duration.millis(300), new KeyValue(
//             this.innerRadiusProperty(),
//             this.lastInnerRadiusValue)));
//             anim.add(radius);

            RadialMenu.this.centerGroup.setVisible(true);
        }

        ParallelTransition transition = new ParallelTransition();
        transition.getChildren().addAll(anim);

        this.itemGroup.setVisible(true);
        transition.play();
    }

    private void saveStateBeforeAnimation() {
        this.lastInitialAngleValue = this.initialAngle.get();
        this.lastOffsetValue = this.offset.get();
    }

    @Override
    public void changed(final ObservableValue<? extends Object> arg0,
        final Object arg1, final Object arg2) {
        this.redraw();
    }

    private void redraw() {
        if (this.centerVisibility.get() == CenterVisibility.NEVER) {
            this.centerGroup.visibleProperty().set(false);
        } else if (this.centerVisibility.get() == CenterVisibility.ALWAYS) {
            this.centerGroup.visibleProperty().set(true);
        } else {
            this.centerGroup.visibleProperty().set(this.itemGroup.isVisible());
        }

        this.centerStrokeShape.setFill(this.backgroundVisible.get() ? 
            (this.mouseOn && this.backgroundMouseOnFill.get() != null ? 
                this.backgroundMouseOnFill.get() : this.backgroundFill.get()) : Color.TRANSPARENT);
        
        this.centerStrokeShape.setStroke(this.strokeVisible.get() ?
            (this.mouseOn && this.strokeMouseOnFill.get() != null ? 
                this.strokeMouseOnFill.get() : this.strokeFill.get()) : Color.TRANSPARENT);
    }
}
