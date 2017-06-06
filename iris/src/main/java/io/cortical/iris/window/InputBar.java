package io.cortical.iris.window;

import java.util.ArrayList;
import java.util.List;

import io.cortical.iris.ui.custom.cornermenu.CornerMenu;
import io.cortical.iris.ui.custom.cornermenu.CornerMenu.Location;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.util.Pair;

/**
 * Part of the header apparatus residing under the 
 * {@link TitleBar} which allows selection of the input 
 * types and displays the relevant input widgets for each type.
 * 
 * @author cogmission
 * @see TitleBar
 */
public class InputBar extends Pane implements Resizable {
    public static final int HEIGHT = 20;
    public static final int MIN_WIDTH = 350;
    
    private ReadOnlyObjectWrapper<String> selectedToggleProperty = new ReadOnlyObjectWrapper<>();
    
    private String[] buttonNames = { "Expression", "Text", "Document", "Url", ""};
    private ToggleGroup toggleGroup;
    
    private Group socialImgGroup;
    private Shape socialIcon;
    private TogglePane socialIconPane;
    private Toggle selectedToggle;
    
    private CornerMenu cornerMenu;
    
    private ImageCursor wImage;
    
    private InputWindow inputWindow;
    
    private List<Toggle> temporarilyDisabledToggles = new ArrayList<>();
    
        
    
    /**
     * Constructs a new {@code InputBar}
     */
    public InputBar(InputWindow iw) {
        this.inputWindow = iw;
        
        setMinWidth(MIN_WIDTH);
        
        HBox box = new HBox();
        box.setPrefHeight(HEIGHT);
       
        configureButtons(box);      
        
        getStyleClass().setAll("input-bar");
        
        Platform.runLater(() -> configureTempDisabledToggles());
        
        getChildren().add(box);
    }
    
    /**
     * Returns a property which is set when this {@link InputBar} has a new toggle
     * selected.
     * @return
     */
    public ReadOnlyObjectProperty<String> selectedToggleProperty() {
        return selectedToggleProperty.getReadOnlyProperty();
    }
    
    /**
     * 
     * @param toggleIndex
     */
    public void selectToggle(int toggleIndex) {
        toggleGroup.selectToggle(toggleGroup.getToggles().get(toggleIndex));
    }
    
    /**
     * Returns the height of this {@code InputBar}
     */
    @Override
    public double computeHeight() {
        return getHeight();
    }
    
    /**
     * Configures this {@code InputBar}'s buttons.
     * @param box
     */
    private void configureButtons(HBox box) {
        toggleGroup = new ToggleGroup();
        int idx = 0;
        for(String name : buttonNames) {
            ButtonBase btn = null;
            if(idx < 4) {
                btn = new ToggleButton(name);
                ToggleButton toggle = (ToggleButton)btn;
                if(idx < 2) { // <--- NOTE: TEMPORARY UNTIL OTHERS ARE ENABLED
                    btn.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
                        if(!((ToggleButton)toggle).isSelected()) {
                            toggle.setSelected(true);
                            e.consume();
                        }
                    });
                } else {
                    temporarilyDisabledToggles.add((Toggle)btn);
                }
                btn.setFocusTraversable(false);
                box.getChildren().add(btn);
                btn.setPrefHeight(box.getPrefHeight());
                btn.setMaxWidth(Double.POSITIVE_INFINITY);
                HBox.setHgrow(btn, Priority.ALWAYS);
            }
            
            // Left border on left-most button only
            if(idx == 0) { btn.setId("left-toggle-button"); }
            
            // Unless we're at the end, add regular toggle buttons
            // otherwise add the custom TogglePane
            if(idx < 4) { 
                if(idx < 2) {  // REMOVE THIS TO USE ALL BUTTONS ON THE BAR
                    toggleGroup.getToggles().add((Toggle)btn);
                }
            }else{
                box.getChildren().add(socialIconPane = createSocialIconToggle());
                HBox.setHgrow(socialIconPane, Priority.ALWAYS);
                socialIconPane.setFocusTraversable(false);
                socialIconPane.setPrefHeight(box.getPrefHeight());
                temporarilyDisabledToggles.add(socialIconPane);
                //toggleGroup.getToggles().add((Toggle)socialIconPane); // DON'T ADD THIS TO THE TOGGLE GROUP YET
                socialIconPane.setToggleGroup(toggleGroup);
                socialIconPane.setMinWidth(22);
            }
            ++idx;
        }
        
        // Init by selecting the first toggle
        toggleGroup.selectedToggleProperty().addListener((v,o,n) -> {
            // Only send request once, and not again for toggle veto (reselect of previous toggle)
            if(selectedToggle != null && selectedToggle == n) return;
            // selectedToggle still refers to the previously selected toggle here
            // for use with the "Cancel" option to go back to the previously selected tab.
            if(n != null && (n instanceof ToggleButton)) {
                toggleScreen();
            }
        });
        toggleGroup.selectToggle(selectedToggle = toggleGroup.getToggles().get(0));
        ((ToggleButton)toggleGroup.getToggles().get(0)).requestFocus();
        
        // Make sure the HBox layout follows the width of this widget
        box.prefWidthProperty().bind(widthProperty().subtract(1));
        box.relocate(1, box.getLayoutBounds().getMinY());
        
        // Creates the social icon circular menu
        cornerMenu = createCornerMenu();
        
        // Experimental: creates the custom "Unimplemented" icon
        this.wImage = new ImageCursor(createUnimplementedIconImage(), 0, 0);
    }
    
    public void configureTempDisabledToggles() {
        for(int i = 0;i < 3;i++) {
            if(i == 2) {
                
            }else{
                ToggleButton tb = (ToggleButton)temporarilyDisabledToggles.get(i);
                tb.setOnMouseEntered(e -> {
                    tb.setStyle(
                        "-fx-background-color: " +
                            "linear-gradient(to bottom, ivory 0%, -bar-background 100%);" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-radius: 0;" +
                        "-fx-font-size: 14;" +
                        "-fx-text-fill: darkgray;" +
                        "-fx-border-color: -base-border-color;" +
                        "-fx-border-width: 0 1 1 0;" +
                        "-fx-background-insets: 0 0 1 0;");
                });
                tb.setOnMouseExited(e -> {
                    tb.setStyle(
                        "-fx-background-color: " +
                            "linear-gradient(to bottom, ivory 0%, -bar-background 100%);" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-radius: 0;" +
                        "-fx-font-size: 14;" +
                        "-fx-text-fill: darkgray;" +
                        "-fx-border-color: -base-border-color;" +
                        "-fx-border-width: 0 1 1 0;" +
                        "-fx-background-insets: 0 0 1 0;");
                });
                tb.selectedProperty().addListener((v,o,n) -> {
                    tb.setStyle(
                        "-fx-background-color: " +
                            "linear-gradient(to bottom, ivory 0%, -bar-background 100%);" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-radius: 0;" +
                        "-fx-font-size: 14;" +
                        "-fx-text-fill: darkgray;" +
                        "-fx-border-color: -base-border-color;" +
                        "-fx-border-width: 0 1 1 0;" +
                        "-fx-background-insets: 0 0 1 0;");
                });
                
                // Set it immediately also for first initialization
                tb.setStyle(
                    "-fx-background-color: " +
                        "linear-gradient(to bottom, ivory 0%, -bar-background 100%);" +
                    "-fx-background-radius: 0;" +
                    "-fx-border-radius: 0;" +
                    "-fx-font-size: 14;" +
                    "-fx-text-fill: darkgray;" +
                    "-fx-border-color: -base-border-color;" +
                    "-fx-border-width: 0 1 1 0;" +
                    "-fx-background-insets: 0 0 1 0;");
            }
        }
    }
    
    public void toggleScreen() {
        Toggle t = toggleGroup.getSelectedToggle();
        selectedToggle = t;
        if(t instanceof ToggleButton) {
            selectedToggleProperty.set(((ToggleButton)selectedToggle).getText());
            toggleGroup.selectToggle(selectedToggle);
        }else{
            selectedToggleProperty.set(((TogglePane)t).getText());
            toggleGroup.selectToggle(selectedToggle);
        }
    }
    
    /**
     * Creates the {@link TogglePane} acting as a trigger for the social
     * icons menu.
     * @return
     */
    public TogglePane createSocialIconToggle() {
        TogglePane p = new TogglePane("Social");
        p.resizeRelocate(0, 0, 30, 30);
        p.getStyleClass().addAll("social-button");
        
        Ellipse e = new Ellipse();
        e.setCenterX(20.0f);
        e.setCenterY(10.0f);
        e.setRadiusX(3.0f);
        e.setRadiusY(4.0f);
        
        Rectangle r0 = new Rectangle();
        r0.setX(19.2);
        r0.setY(14);
        r0.setWidth(2);
        r0.setHeight(2);
        
        Rectangle r = new Rectangle();
        r.setX(15);
        r.setY(15);
        r.setWidth(10);
        r.setHeight(10);
        r.setArcWidth(8);
        r.setArcHeight(8);
        
        Rectangle r1 = new Rectangle();
        r1.setX(15);
        r1.setY(17);
        r1.setWidth(10);
        r1.setHeight(10);
        
        socialIcon = Path.union(e, r0);
        socialIcon = Path.union(socialIcon, r);
        socialIcon = Path.union(socialIcon, r1);
        
        Shape si = Path.union(socialIcon, socialIcon);
        Shape si2 = Path.union(socialIcon, socialIcon);
        
        socialIcon.setFill(Color.rgb(37, 98, 136));
        socialIcon.setScaleY(.8);
        socialIcon.setTranslateY(-1);
        
        si.setFill(Color.GRAY);//Color.rgb(37, 98, 136).brighter());
        si.setScaleY(.7);
        si.setTranslateX(-4);
        
        si2.setFill(Color.rgb(200,200,200));//Color.rgb(37, 98, 136).brighter().brighter().brighter().brighter());
        si2.setScaleY(.6);
        si2.setTranslateX(-7);
        si2.setTranslateY(1);
        
        p.addEventHandler(MouseEvent.MOUSE_ENTERED, m -> {
            if(p.isSelected()) return;//social-icon-temp-disabled
            //socialIcon.getStyleClass().setAll("social-icon-selected");
            socialIcon.getStyleClass().setAll("social-icon-temp-disabled");
        });
        p.addEventHandler(MouseEvent.MOUSE_EXITED, m -> {
            if(!p.isSelected() && !p.getLayoutBounds().contains(new Point2D(m.getX(), m.getY()))) {
                //socialIcon.getStyleClass().setAll("social-icon");
                socialIcon.getStyleClass().setAll("social-icon-temp-disabled");
            }
        });
        p.addEventHandler(MouseEvent.MOUSE_CLICKED, m -> {
            // Temporary
            return;
            
//            if(!p.isSelected()) {
//                p.setSelected(true);
//                toggleGroup.selectToggle(p);
//            }
//            cornerMenu.show();
        });
        
        socialImgGroup = new Group();
        socialImgGroup.getChildren().addAll(si2, si, socialIcon);
        
        p.getChildren().addAll(socialImgGroup);
       
        return p;
    }
    
    /**
     * Creates the popout social menu displayed when clicking the 
     * "social" icon.
     * 
     * @return
     */
    private CornerMenu createCornerMenu() {
        // uninstall the current cornerMenu
        if (cornerMenu != null) {
            cornerMenu.autoShowAndHideProperty().unbind();
            cornerMenu.removeFromPane();
            cornerMenu = null;
        }
        
        MenuItem facebookMenuItem = new MenuItem("Facebook", new ImageView(new Image(this.getClass().getClassLoader().getResourceAsStream("social_facebook_button_blue.png"))));
        MenuItem googleMenuItem = new MenuItem("Google", new ImageView(new Image(this.getClass().getClassLoader().getResourceAsStream("social_google_button_blue.png"))));
        MenuItem skypeMenuItem = new MenuItem("Skype", new ImageView(new Image(this.getClass().getClassLoader().getResourceAsStream("social_skype_button_blue.png"))));
        MenuItem twitterMenuItem = new MenuItem("Twitter", new ImageView(new Image(this.getClass().getClassLoader().getResourceAsStream("social_twitter_button_blue.png"))));
        MenuItem linkedInMenuItem = new MenuItem("LinkedIn", new ImageView(
            new Image(this.getClass().getClassLoader().getResource("social_in_button_blue.png").toExternalForm(), 35d, 35d, true, true)));
         
        Platform.runLater(() -> {
            cornerMenu = new CornerMenu(Location.TOP_RIGHT, inputWindow, new Dimension2D(130, 130), false);
            cornerMenu.getItems().addAll(facebookMenuItem, linkedInMenuItem, googleMenuItem, skypeMenuItem, twitterMenuItem);
            cornerMenu.setAutoShowAndHide(false);
            
            setSocialMenuItemHandlers();
        });
        
        
        return cornerMenu;
    }
    
    /**
     * Initializes event handlers for social menu
     * cursor management.
     */
    private void setSocialMenuItemHandlers() {
        for(MenuItem mi : cornerMenu.getItems()) {
            if(!mi.getText().equals("LinkedIn")) {
                mi.setDisable(true);
                continue;
            }
            mi.setOnAction(e -> {
                switch(mi.getText()) {
                    case "LinkedIn" : {
                        cornerMenu.hide();
                    }
                }
            });
            
            mi.getGraphic().addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {getScene().setCursor(Cursor.HAND);});
            mi.getGraphic().addEventHandler(MouseEvent.MOUSE_EXITED, e -> {getScene().setCursor(Cursor.DEFAULT);});
        }
        
        cornerMenu.shownProperty().addListener((v,o,n) -> {
            if(!n) {
                getScene().setCursor(Cursor.DEFAULT);
            }
        });
        
        cornerMenu.getCircularPane().addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            cornerMenu.hide();
            e.consume();
        });
        
        cornerMenu.getCircularPane().addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            Point2D cp = cornerMenu.getCircularPane().localToParent(e.getX(), e.getY());
            Bounds b = socialIcon.localToParent(socialIcon.getLayoutBounds());
            Pair<Boolean, Boolean> dualCondition = isActiveIcon(e);
            if(b.contains(cp) || dualCondition.getValue()) {
                getScene().setCursor(Cursor.HAND);
            }else if(dualCondition.getKey()){
                getScene().setCursor(wImage);
            }else{
                getScene().setCursor(Cursor.DEFAULT);
            }
        });
    }
    
    /**
     * Returns a {@link Pair} whose 'key' indicates whether the icon
     * shape contains the current coordinates of the mouse, and 
     * whose 'value' indicates whether the icon is an enabled icon
     * or not.
     * 
     * @param m     the mouse event supplying the mouse location
     * @return      a {@code Pair} with &lt;iconHit, iconActive&gt;
     */
    private Pair<Boolean, Boolean> isActiveIcon(MouseEvent m) {
        for(MenuItem mi : cornerMenu.getItems()) {
            
            Point2D cp = cornerMenu.getCircularPane().localToScene(m.getX(), m.getY());
            
            if(mi.getGraphic().localToScene(mi.getGraphic().getLayoutBounds()).contains(cp)) {
                if(mi.getText().equals("LinkedIn")) {
                    return new Pair<>(true, true);
                }
                return new Pair<>(true, false);
            }
        }
        
        return new Pair<>(false, false);
    }
    
    /**
     * Creates and returns a custom drawn icon image
     * with a transparent background.
     * @return
     */
    private Image createUnimplementedIconImage() {
        Image i = new Image(getClass().getClassLoader().getResource("red-circle-cross.png").toExternalForm(), 20, 20, true, true);
        ImageView view = new ImageView(i);
        view.setStyle("-fx-background-color: transparent");
        
        Text nyi = new Text("Not");
        nyi.setStyle("-fx-font-size: 8");
        Text nyi2 = new Text(" Yet");
        nyi2.setStyle("-fx-font-size: 8");
        Text nyi3 = new Text("Implemented");
        nyi3.setStyle("-fx-font-size: 8");
        
        VBox vb = new VBox();
        vb.getChildren().addAll(nyi, nyi2, nyi3);
        
        StackPane pane = new StackPane();
        pane.resize(20, 20);
        pane.getChildren().addAll(view, vb);
        pane.setStyle("-fx-background-color: transparent");
        
        getChildren().add(pane);
        
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage img = pane.snapshot(sp, null);
        
        getChildren().remove(pane);
        
        return img;
    }
    
    /**
     * Custom class which converts a {@link StackPane} into a ToggleButton
     */
    class TogglePane extends StackPane implements Toggle {
        ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
        BooleanProperty selectedProperty = new SimpleBooleanProperty();
        String text;
        
        public TogglePane(String text) {
            this.text = text;
        }

        @Override
        public ToggleGroup getToggleGroup() {
            return toggleGroupProperty.get();
        }
        
        public String getText() {
            return text;
        }

        @Override
        public void setToggleGroup(ToggleGroup toggleGroup) {
            this.toggleGroupProperty.set(toggleGroup);
            this.toggleGroupProperty.get().selectedToggleProperty().addListener((v,o,n) -> {
                setSelected(n == this);
            });
        }

        @Override
        public ObjectProperty<ToggleGroup> toggleGroupProperty() {
            return toggleGroupProperty;
        }

        @Override
        public boolean isSelected() {
            return selectedProperty.get();
        }

        @Override
        public void setSelected(boolean selected) {
            if(selected) {
                if(toggleGroupProperty.get().getSelectedToggle() != null) {
                    toggleGroupProperty.get().getSelectedToggle().setSelected(false);
                }
                getStyleClass().setAll("social-button-selected");
                socialIcon.getStyleClass().setAll("social-icon-selected");
            }else{
                getStyleClass().setAll("social-button");
                socialIcon.getStyleClass().setAll("social-icon");
            }
            selectedProperty.set(selected);
        }

        @Override
        public BooleanProperty selectedProperty() {
            return selectedProperty;
        }
        
    }
}
