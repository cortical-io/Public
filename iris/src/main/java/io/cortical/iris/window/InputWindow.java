package io.cortical.iris.window;


import java.io.IOException;
import java.io.Serializable;

import io.cortical.iris.WindowService;
import io.cortical.iris.ui.custom.radialmenu.RadialMenu;
import io.cortical.iris.view.InputViewArea;
import io.cortical.iris.view.WindowContext;
import io.cortical.iris.view.input.expression.ExpressionDisplay;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;


/**
 * Draggable window used for entering input which will be 
 * processed by a given Retina.
 * 
 * @author cogmission
 *
 */
public class InputWindow extends Window implements WindowContext {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private transient AnchorPane windowContent;
    
    private transient RadialMenu showingRadialMenu;
    private transient StackPane overlay;
    
    
    /**
     * Constructs a new {@code InputWindow}
     */
    public InputWindow() {
        FXMLLoader fxmlLoader = new FXMLLoader(InputWindow.class.getClassLoader().getResource("InputWindow.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        
        setManaged(false);
        setFocusTraversable(false);
        
        MIN_WIDTH = INI_WIDTH = 535;
        MIN_HEIGHT = INI_HEIGHT = 277;
        
        setMinSize(MIN_WIDTH, MIN_HEIGHT);
        resize(INI_WIDTH, INI_HEIGHT);
        
        getStyleClass().add("app-window");
        
        getChildren().addAll(windowContent = createContents(), overlay = createOverlay());
        
        backContent.prefHeightProperty().bind(heightProperty());
        backContent.prefWidthProperty().bind(widthProperty());
        
        windowContent.prefHeightProperty().bind(heightProperty());
        windowContent.prefWidthProperty().bind(widthProperty());
    }
    
    /**
     * Computes and returns the minimum size of this window as a function
     * of the min size required by the different parts of its contents
     * which are:
     * <ol>
     * <li>{@link TitleBar}</li>
     * <li>{@link InputBar}</li>
     * <li>{@link InputViewArea}</li>
     * <li>{@link StatusBar}</li>
     * </ol>
     * 
     * This calculation is used for drag-resizing and dynamic
     * auto-expansion/contraction of widgets during user entry.
     *  
     * @return  the computed minimum size.
     */
    public Dimension2D computeMinSize() {
        double minWidth = Math.max(MIN_WIDTH, titleBar.getMinWidth());
        minWidth = Math.max(minWidth, statusBar.getMinWidth());
        
        double minHeight = titleBar.computeHeight() + inputBar.computeHeight() +
            contentArea.computeHeight() + statusBar.computeHeight();
        
        return new Dimension2D(minWidth, Math.max(MIN_HEIGHT, minHeight));
    }
    
    /**
     * Returns true indicating that this is an {@code InputWindow}
     */
    @Override
    public boolean isInput() {
        return true;
    }
    
    /**
     * Returns this {@code InputWindow}'s {@link InputBar}
     * @return      this {@code InputWindow}'s {@link InputBar}
     */
    public InputBar getInputBar() {
        return inputBar;
    }
    
    /**
     * Returns the {@link Thumb} used to drag-resize this 
     * window.
     * 
     * @return
     */
    @Override
    public Thumb getThumb() {
        return statusBar.getThumb();
    }
    
    /**
     * Returns this {@code InputWindow}'s {@link TitleBar}
     * @return  this {@code InputWindow}'s {@link TitleBar}
     */
    @Override
    public TitleBar getTitleBar() {
        return titleBar;
    }
    
    /**
     * Returns this {@code InputWindow}'s {@link InputViewArea}
     * @return
     */
    public InputViewArea getViewArea() {
        return (InputViewArea)contentArea;
    }
    
    /**
     * Returns this {@code InputWindow}'s {@link StatusBar}
     * @return
     */
    @Override
    public StatusBar getStatusBar() {
        return statusBar;
    }
    
    /** 
     * Returns this {code Window}'s type
     * @return
     */
    @Override
    public Type getType() {
        return Type.INPUT;
    }
    
    /**
     * {@link WindowContext} implementation which returns the Region to
     * resize.
     */
    @Override
    public Region getRegion() {
        return contentArea;
    }
    
    @Override
    public Parent getBackContent() {
        return backContent;
    }
    
    @Override
    public Parent getFrontContent() {
        return windowContent;
    }
    
    /**
     * Creates and returns the {@link TitleBar}
     * @return
     */
    private TitleBar createTitleBar() {
        return new TitleBar();
    }
    
    /**
     * Creates and returns the {@link InputBar}
     * @return
     */
    private InputBar createInputBar() {
        return new InputBar(this);
    }
    
    /**
     * Creates and returns this {@code InputWindow}'s 
     * {@link InputViewArea}
     * 
     * @return
     */
    private InputViewArea createContentArea() {
        return new InputViewArea(this);
    }
    
    /**
     * Creates and returns the {@link StatusBar}
     * @return
     */
    private StatusBar createStatusBar() {
        return new StatusBar(this);
    }
    
    /**
     * Adds this {@code InputWindow}'s control apparatus.
     */
    private AnchorPane createContents() {
        windowContent = new AnchorPane();
        windowContent.getStyleClass().addAll("window-content-pane");
        windowContent.setFocusTraversable(false);
        
        VBox sections = new VBox();
        
        titleBar = createTitleBar();
        titleBar.prefWidthProperty().bind(widthProperty());
        titleBar.titleSetProperty().addListener((ChangeListener<String> & Serializable) (v,o,n) -> { 
            if(title == null) {
                title = new Text();
            }
            title.setText(n); 
        });
        
        inputBar = createInputBar();
        inputBar.prefWidthProperty().bind(widthProperty());
        
        statusBar = createStatusBar();
        statusBar.prefWidthProperty().bind(widthProperty());
        
        contentArea = createContentArea();
        contentArea.prefWidthProperty().bind(widthProperty());
        
        sections.getChildren().addAll(titleBar, inputBar, contentArea);
        
        windowContent.getChildren().addAll(sections, statusBar);
        
        AnchorPane.setTopAnchor(sections, 0d);
        AnchorPane.setBottomAnchor(statusBar, 0d);
        AnchorPane.setLeftAnchor(statusBar, 1d);
        AnchorPane.setRightAnchor(statusBar, 1d);
        
        backContent = new StackPane();
        Button infoButton = new Button();
        infoButton.getStyleClass().addAll("info-button");
        infoButton.setPrefSize(41d, 32d);
        infoButton.resize(41d, 32d);
        infoButton.setManaged(false);
        infoButton.setFocusTraversable(false);
        ImageView infoView = new ImageView(new Image(this.getClass().getClassLoader().getResourceAsStream("return.png"), 41d, 32d, true, true));
        infoButton.setGraphic(infoView);        
        infoButton.setOnAction(e -> {
            Platform.runLater(() -> {
                WindowService.getInstance().windowFor(this).flip();
            });
        });
        
        layoutBoundsProperty().addListener((v,o,n) -> {
            infoButton.relocate(n.getWidth() - 45, 5);
        });
        backContent.getChildren().add(infoButton);
        backContent.setRotationAxis(Rotate.Y_AXIS);
        backContent.setRotate(180);
        
        return windowContent;
    }
    
    /**
     * Displays the Radial operator menu on behalf of the {@link ExpressionDisplay}
     * 
     * @param source
     * @param menu
     * @param srcX
     * @param srcY
     * @param showOverlay
     * @param focalShape
     */
    public void showExpressionDisplayRadialMenu(Node source, RadialMenu menu, double srcX, double srcY, boolean showOverlay, Shape focalShape) {
        Platform.runLater(() -> {
            showingRadialMenu = menu;
            
            // Remove the menu if we enter here before removing the menu
            if(getChildren().contains(menu)) {
                getChildren().remove(menu);
            }
            
            Bounds trans = source.localToParent(source.getBoundsInLocal());
            trans = contentArea.localToParent(trans);
            trans = new BoundingBox(trans.getMinX() + srcX, trans.getMinY() + srcY, trans.getWidth(), trans.getHeight());
            
            getChildren().add(menu);
            menu.setTranslateX(trans.getMinX() + ((trans.getMaxX() - trans.getMinX())/2));
            menu.setTranslateY(trans.getMinY() + ((trans.getMaxY() - trans.getMinY())/2));
            menu.setVisible(true);
            menu.showRadialMenu();
            
            if(showOverlay) {
                Rectangle r = new Rectangle(0, 0, getWidth(), getHeight());
                
                Shape shape = r;
                if(focalShape != null) {
                    if(focalShape instanceof Circle) {
                        ((Circle)focalShape).setCenterX(menu.getTranslateX());
                        ((Circle)focalShape).setCenterY(menu.getTranslateY());
                    }
                    
                    shape = (Path)Shape.subtract(r, focalShape);
                    
                    overlay.getChildren().clear();
                    overlay.getChildren().add(shape);
                }
                
                shape.setFill(Color.rgb(0, 0, 0, 0.8));
               
                showOverlay();
            }
            
            menu.toFront();
        });
    }
    
    /**
     * Makes the overlay visible.
     */
    public void showOverlay() {
        overlay.resizeRelocate(0, 0, getWidth(), getHeight());
        overlay.setVisible(true);
        overlay.toFront();
    }
    
    /**
     * Hides the overlay.
     */
    public void hideOverlay() {
        overlay.setVisible(false);
    }
    
    /**
     * Hides the radial operator menu.
     * 
     * @param menu
     */
    public void hideExpressionDisplayRadialMenu(RadialMenu menu) {
        menu.hideRadialMenu();
        menu.setVisible(false);
        getChildren().remove(menu);
        hideOverlay();
        showingRadialMenu = null;
    }
    
    /**
     * Creates and returns the partly transparent overlay used to
     * contain display nodes on a layer above the main view.
     * 
     * @return  a transparent node the size of this {@code Iris}
     */
    public StackPane createOverlay() {
        StackPane overlay = new StackPane();
        overlay.setBackground(new Background(new BackgroundFill(Color.rgb(50, 50, 50, 0.0), null, null)));
        overlay.setVisible(false);
        overlay.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent e) -> {
            if(showingRadialMenu != null) {
                hideExpressionDisplayRadialMenu(showingRadialMenu);
            }
        });
        layoutBoundsProperty().addListener((v,o,n) -> {
            overlay.resize(n.getWidth(), n.getHeight());
        });
        return overlay;
    }
    
    @Override
    public void showBack() {
        Platform.runLater(() -> {
            getChildren().add(backContent);
        });
    }
    
    @Override
    public void showFront() {
        Platform.runLater(() -> {
            getChildren().remove(3);
        });
    }
}
