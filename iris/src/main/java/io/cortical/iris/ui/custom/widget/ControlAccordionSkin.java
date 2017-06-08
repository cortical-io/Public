package io.cortical.iris.ui.custom.widget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.javafx.scene.control.skin.AccordionSkin;
import com.sun.javafx.scene.control.skin.TitledPaneSkin;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Accordion;
import javafx.scene.control.Skin;
import javafx.scene.control.TitledPane;
import javafx.scene.shape.Rectangle;

public class ControlAccordionSkin extends AccordionSkin  {

    private TitledPane firstTitledPane;
    private Rectangle clipRect;
    
    // This is used when we definitely want to force a relayout, regardless of
    // whether the height has also changed
    private boolean forceRelayout = false;

    // this is used to request a layout, assuming the height has also changed
    private boolean relayout = false;

    // we record the previous height to know if the current height is different
    private double previousHeight = 0;

    public ControlAccordionSkin(final Accordion accordion) {
        super(accordion);
        accordion.getPanes().addListener((ListChangeListener<TitledPane>) c -> {
            if (firstTitledPane != null) {
                firstTitledPane.getStyleClass().remove("first-titled-pane");
            }
            if (!accordion.getPanes().isEmpty()) {
                firstTitledPane = accordion.getPanes().get(0);
                firstTitledPane.getStyleClass().add("first-titled-pane");
            }
            // TODO there may be a more efficient way to keep these in sync
            getChildren().setAll(accordion.getPanes());
            while (c.next()) {
                removeTitledPaneListeners(c.getRemoved());
                initTitledPaneListeners(c.getAddedSubList());
            }
        });

        if (!accordion.getPanes().isEmpty()) {
            firstTitledPane = accordion.getPanes().get(0);
            firstTitledPane.getStyleClass().add("first-titled-pane");
        }

        clipRect = new Rectangle(accordion.getWidth(), accordion.getHeight());
        getSkinnable().setClip(clipRect);

        initTitledPaneListeners(accordion.getPanes());
        getChildren().setAll(accordion.getPanes());
        getSkinnable().requestLayout();

        registerChangeListener(getSkinnable().widthProperty(), "WIDTH");
        registerChangeListener(getSkinnable().heightProperty(), "HEIGHT");
    }

    @Override
    protected void handleControlPropertyChanged(String property) {
        super.handleControlPropertyChanged(property);
        if ("WIDTH".equals(property)) {
            clipRect.setWidth(getSkinnable().getWidth());
        } else if ("HEIGHT".equals(property)) {
            clipRect.setHeight(getSkinnable().getHeight());
        }
    }
    
    @Override protected void layoutChildren(final double x, double y,
        final double w, final double h) {
        final boolean rebuild = forceRelayout || (relayout && previousHeight != h);
        forceRelayout = false;
        previousHeight = h;
    
        // Compute height of all the collapsed panes
        double collapsedPanesHeight = 0;
        for (TitledPane tp : getSkinnable().getPanes()) {
            if (!tp.equals(expandedPane)) {
                WindowTitlePaneSkin childSkin = (WindowTitlePaneSkin) ((WindowTitlePane)tp).getSkin();
                collapsedPanesHeight += snapSize(childSkin.getWindowTitleRegionSize(w));
            }
        }
        final double maxTitledPaneHeight = h - collapsedPanesHeight;
    
        for (TitledPane tp : getSkinnable().getPanes()) {
            Skin<?> skin = tp.getSkin();
            double ph;
            if (skin instanceof TitledPaneSkin) {
                ((WindowTitlePaneSkin)skin).setMaxWindowTitledPaneHeightForAccordion(maxTitledPaneHeight);
                ph = snapSize(((WindowTitlePaneSkin)skin).getWindowTitledPaneHeightForAccordion());
            } else {
                ph = tp.prefHeight(w);
            }
            tp.resize(w, ph);
    
            boolean needsRelocate = true;
            if (! rebuild && previousPane != null && expandedPane != null) {
                List<TitledPane> panes = getSkinnable().getPanes();
                final int previousPaneIndex = panes.indexOf(previousPane);
                final int expandedPaneIndex = panes.indexOf(expandedPane);
                final int currentPaneIndex  = panes.indexOf(tp);
    
                if (previousPaneIndex < expandedPaneIndex) {
                    // Current expanded pane is after the previous expanded pane..
                    // Only move the panes that are less than or equal to the current expanded.
                    if (currentPaneIndex <= expandedPaneIndex) {
                        tp.relocate(x, y);
                        y += ph;
                        needsRelocate = false;
                    }
                } else if (previousPaneIndex > expandedPaneIndex) {
                    // Previous pane is after the current expanded pane.
                    // Only move the panes that are less than or equal to the previous expanded pane.
                    if (currentPaneIndex <= previousPaneIndex) {
                        tp.relocate(x, y);
                        y += ph;
                        needsRelocate = false;
                    }
                } else {
                    // Previous and current expanded pane are the same.
                    // Since we are expanding and collapsing the same pane we will need to relocate
                    // all the panes.
                    tp.relocate(x, y);
                    y += ph;
                    needsRelocate = false;
                }
            }
    
            if (needsRelocate) {
                tp.relocate(x, y);
                y += ph;
            }
        }
    }


    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return ControlAccordion.DEFAULT_HEIGHT;
    }
    
    public void setExpanded(WindowTitlePane wt) {
        expandedPane = wt;
    }

    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double h = ControlAccordion.DEFAULT_HEIGHT;
        if (expandedPane != null) {
            h = expandedPane.prefHeight(width);
        }
        double sizeMinus1 = Math.max(0, getChildren().size() - 1);
        return h + (sizeMinus1 * ControlAccordion.DEFAULT_HEIGHT);
    }

    private TitledPane expandedPane = null;
    private TitledPane previousPane = null;
    private Map<TitledPane, ChangeListener<Boolean>>listeners = new HashMap<TitledPane, ChangeListener<Boolean>>();

    private void initTitledPaneListeners(List<? extends TitledPane> list) {
        for (final TitledPane tp: list) {
            tp.setExpanded(tp == getSkinnable().getExpandedPane());
            if (tp.isExpanded()) {
                expandedPane = tp;
            }
            ChangeListener<Boolean> changeListener = expandedPropertyListener(tp);
            tp.expandedProperty().addListener(changeListener);
            listeners.put(tp, changeListener);
        }
    }

    private void removeTitledPaneListeners(List<? extends TitledPane> list) {
        for (final TitledPane tp: list) {
            if (listeners.containsKey(tp)) {
                tp.expandedProperty().removeListener(listeners.get(tp));
                listeners.remove(tp);
            }
        }
    }

    private ChangeListener<Boolean> expandedPropertyListener(final TitledPane tp) {
        return (observable, wasExpanded, expanded) -> {
            final Accordion accordion = getSkinnable();
            if (expanded) {
                if (expandedPane != null) {
                    expandedPane.setExpanded(false);
                }
                if (tp != null) {
                    accordion.setExpandedPane(tp);
                }
                expandedPane = accordion.getExpandedPane();
            } else {
                expandedPane = null;
                accordion.setExpandedPane(null);
            }
        };
    }
}
