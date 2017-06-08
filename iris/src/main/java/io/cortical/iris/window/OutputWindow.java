package io.cortical.iris.window;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import io.cortical.iris.WindowService;
import io.cortical.iris.view.OutputViewArea;
import io.cortical.iris.view.ViewArea;
import io.cortical.iris.view.WindowContext;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Dimension2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;

/**
 * Controls the display of selected input processed by the server side Retina API.
 * 
 * @author cogmission
 * @see InputWindow
 */
public class OutputWindow extends Window implements WindowContext {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code OutputWindow}
     */
    public OutputWindow() {
        FXMLLoader fxmlLoader = new FXMLLoader(InputWindow.class.getClassLoader().getResource("OutputWindow.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        
        setManaged(false);
        setFocusTraversable(false);
        
        MIN_WIDTH = 535;
        MIN_HEIGHT = 580;
        INI_WIDTH = MIN_WIDTH;
        INI_HEIGHT = MIN_HEIGHT;
        
        setMinSize(MIN_WIDTH, MIN_HEIGHT);
        resize(INI_WIDTH, INI_HEIGHT);
        
        getStyleClass().add("app-window");
        
        getChildren().addAll(windowContent = createContents());
        
        backContent.prefHeightProperty().bind(heightProperty());
        backContent.prefWidthProperty().bind(widthProperty());
        
        windowContent.prefHeightProperty().bind(heightProperty());
        windowContent.prefWidthProperty().bind(widthProperty());
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
     * Rotates this window 180deg about the Y-Axis
     */
    public void flip() {
        super.flip();
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
            getChildren().remove(0);
            getChildren().remove(0);
            getChildren().add(windowContent);
        });
    }
    
    /**
     * Override to check for Flyout showing - to hide when window is moved.
     */
    @Override
    public void relocate(double x, double y) {
        super.relocate(x, y);
        if(titleBar.getInputSelector().getFlyout().flyoutShowing()) {
            titleBar.getInputSelector().getFlyout().dismiss();
            titleBar.getInputSelector().getFlyout().getPopup().hide();
            titleBar.getInputSelector().setSelected(false);
        }
    }

    @Override
    public Region getRegion() {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * Adds the specified {@link InputWindow} to this {@code OutputWindow}'s list of
     * input windows.
     * 
     * @param w the window to add to the list
     */
    public void autoSelectInputWindow(InputWindow w) {
        getTitleBar().getInputSelector().selectInputWindow(w.getWindowID(), true);
    }
    
    /**
     * Returns a list of UUIDs of the selected {@link InputWindow}s.
     * @return
     */
    public List<UUID> getSelectedInputs() {
        return titleBar.getInputSelector().getInputSelection();
    }
    
    /**
     * Creates and returns the {@link TitleBar}
     * @return
     */
    private TitleBar createTitleBar() {
        TitleBar titleBar = new TitleBar(TitleBar.Type.OUTPUT);
        titleBar.getTitleField().setPromptText("Enter output name...");
        return titleBar;
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
    private Pane createContents() {
        windowContent = new AnchorPane();
        windowContent.getStyleClass().addAll("window-content-pane");
        windowContent.setFocusTraversable(false);
        
        VBox sections = new VBox();
        
        titleBar = createTitleBar();
        titleBar.prefWidthProperty().bind(widthProperty());
        
        statusBar = createStatusBar();
        statusBar.prefWidthProperty().bind(widthProperty());
        
        contentArea = createContentArea();
        contentArea.prefWidthProperty().bind(widthProperty());
        sections.getChildren().addAll(titleBar, contentArea);
        contentArea.prefHeightProperty().bind(sections.heightProperty().subtract(titleBar.heightProperty().subtract(10)));
        
        windowContent.getChildren().addAll(sections, statusBar);
        
        AnchorPane.setTopAnchor(sections, 0d);
        AnchorPane.setBottomAnchor(sections, 20d);
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
     * Returns the central view area of the output window.
     * @return
     */
    private OutputViewArea createContentArea() {
        return new OutputViewArea(this);
    }
    
    /**
     * Returns false indicating that this is an {@code OutputWindow}
     */
    @Override
    public boolean isInput() {
        return false;
    }
    
    /**
     * Returns the main content area of this {@code Window}
     * @return
     */
    public ViewArea getContentArea() {
        return contentArea;
    }
    
    /**
     * Computes and returns the minimum size of this window as a function
     * of the min size required by the different parts of its contents
     * which are:
     * <ol>
     * <li>{@link TitleBar}</li>
     * <li>{@link InputBar}</li>
     * <li>{@link OutputViewArea}</li>
     * <li>{@link StatusBar}</li>
     * </ol>
     * 
     * This calculation is used for drag-resizing and dynamic
     * auto-expansion/contraction of widgets during user entry.
     *  
     * @return  the computed minimum size.
     */
    @Override
    public Dimension2D computeMinSize() {
        boolean isInput = getType() == Type.INPUT;
        
        double minWidth = Math.max(0, titleBar.getMinWidth());
        minWidth = Math.max(minWidth, contentArea.getMinWidth());
        minWidth = Math.max(minWidth, statusBar.getMinWidth());
        
        double minHeight = titleBar.computeHeight() + (isInput ? inputBar.computeHeight() : 0) +
            (isInput ? contentArea.computeHeight() : 0) + statusBar.computeHeight();
        
        return new Dimension2D(Math.max(MIN_WIDTH, minWidth), Math.max(MIN_HEIGHT, minHeight));
    }
    
    /**
     * Returns this {@code OutputWindow}'s {@link InputSelector}
     * @return
     */
    public InputSelector getInputSelector() {
        return titleBar.getInputSelector();
    }
    
    /**
     * Returns this {@code OutputWindow}'s {@link OutputViewArea}
     * @return
     */
    public OutputViewArea getViewArea() {
        return (OutputViewArea)contentArea;
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
        return Type.OUTPUT;
    }
}
    