package io.cortical.iris.window;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import fxpresso.tidbit.ui.Flyout;
import io.cortical.iris.WindowService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class InputSelector extends ToggleButton {
    private Shape arrow;
    
    private Flyout inputFlyout;
    
    private BooleanProperty flyoutShownProperty = new SimpleBooleanProperty();
    
    private WindowListView windowListView;
    
    private WindowGroup windowGroup = new WindowGroup();
    
    private WindowLogic logic;
    
    private ObservableList<UUID> inputWindows = getApplicationInputWindowIDs();
    
    private ObjectProperty<Change<? extends UUID>> selectionProperty = new SimpleObjectProperty<Change<? extends UUID>>();
    
    
    private static final String OFF_STYLE_STRING = "" +
        "-fx-background-color: -fx-shadow-highlight-color, -fx-outer-border, -fx-body-color;" +
        "-fx-background-insets: 0 0 -1 0, 0, 1, 20;" +
        "-fx-background-radius: 3px, 3px, 2px, 1px;" +
        "-fx-padding: 0 5 0 5;" +
        "-fx-text-fill: -fx-text-base-color;" +
        "-fx-alignment: CENTER;" +
        "-fx-content-display: LEFT;" +
        "-fx-opacity: 0.4;";
    
    private static final String SELECTED_STYLE_STRING = "" +
        "-fx-background-color:" +
            "rgb(82, 154, 158)," +
            "rgb(115, 219, 223)," +
            "linear-gradient(to top, rgb(92,183,186) 0%, rgb(82, 154, 158) 100%);" +
        "-fx-background-insets: 0, 1, 2 1 1 1;" +
        "-fx-background-radius: 4 4 4 4, 3 3 3 3, 3 3 3 3;" +
        "-fx-padding: 0 5 0 5;" +
        "-fx-text-fill: white;" +
        "-fx-alignment: CENTER;" +
        "-fx-content-display: LEFT;";
    
    private static final String SECONDARY_STYLE_STRING = "" +
        "-fx-background-color: -fx-shadow-highlight-color, -fx-outer-border, -fx-body-color;" +
        "-fx-background-insets: 0 0 -1 0, 0, 1, 20;" +
        "-fx-background-radius: 3px, 3px, 2px, 1px;" +
        "-fx-padding: 0 5 0 5;" +
        "-fx-text-fill: -fx-text-base-color;" +
        "-fx-alignment: CENTER;" +
        "-fx-content-display: LEFT;";
    
    
    /**
     * Constructs a new {@code InputSelector}
     */
    public InputSelector() {
        setGraphic(arrow = getArrowImage());
        
        setFocusTraversable(false);
        
        selectedProperty().addListener((v, o, n) -> {
            if(isSelected()) {
                getStyleClass().setAll("input-selector-pressed");
                arrow.getStyleClass().setAll("input-selector-icon-pressed");
            }else{
                getStyleClass().setAll("input-selector");
                arrow.getStyleClass().setAll("input-selector-icon");
            }
        });
        
        addEventHandler(MouseEvent.MOUSE_ENTERED, m -> {
            arrow.getStyleClass().setAll("input-selector-icon-hover");
        });
        
        addEventHandler(MouseEvent.MOUSE_EXITED, m -> {
            if(isSelected()) {
                arrow.getStyleClass().setAll("input-selector-icon-pressed");
            }else{
                arrow.getStyleClass().setAll("input-selector-icon");
            }
        });
        
        getStyleClass().setAll("input-selector");
        
        inputFlyout = createFlyout();
        
        logic = new WindowLogic(this, windowListView, inputWindows);
        
        setTooltip(new Tooltip("Connect InputWindows"));
    }
    
    /**
     * Disconnects the {@link WindowLogic}'s InputWindow list listener.
     */
    public void disconnect() {
        logic.disconnect();
        WindowService.getInstance().inputWindowTitleChangeProperty().removeListener(windowListView.titleChangeListener);
        WindowService.getInstance().inputWindowColorIDChangeProperty().removeListener(windowListView.idColorChangeListener);
    }
    
    /**
     * Returns a {@link WindowGroup} object which emits events
     * when {@link Window}s are added, selected as primary and
     * unselected.
     * 
     * @return  the {@link WindowGroup}-ing
     */
    public WindowGroup getWindowGroup() {
        return windowGroup;
    }
    
    /**
     * Selects or de-selects the {@link InputWindow} specified by the indicated
     * {@link UUID}, depending on the value of the boolean flag passed in.
     * 
     * @param uuid      the id of the InputWindow to select or de-select.
     * @param b         if true, the InputWindow should be selected, if false it 
     *                  should be "de-selected".
     */
    public void selectInputWindow(UUID uuid, boolean b) {
        windowListView.getCells().stream()
            .filter(c -> c.uuid.equals(uuid))
            .forEach(c -> c.checkBox.setSelected(b));
    }
    
    /**
     * Returns the UUIDs of the selected {@link InputWindow}s
     * @return  the UUIDs of the selected {@link InputWindow}s
     */
    public List<UUID> getInputSelection() {
        return windowListView.getSelectedCells().stream()
            .map(cell -> cell.uuid)
            .collect(Collectors.toList());
    }
    
    /**
     * The actual displayed node.
     * @return
     */
    public Flyout getFlyout() {
        return inputFlyout;
    }
    
    /**
     * Returns the {@link Window} designated as "primary" 
     * or null if one is not selected.
     * @return
     */
    public Window getPrimaryWindow() {
        return windowGroup.getPrimaryWindow();
    }
    
    /**
     * Returns the {@link Window} designated as "secondary"
     * or null if one is not selected.
     * @return
     */
    public Window getSecondaryWindow() {
        return windowGroup.getSecondaryWindow();
    }
    
    /**
     * Returns the UUID of the primary input.
     * @return  the UUID of the primary input.
     */
    public UUID getPrimaryInputSelection() {
        if(windowGroup.getPrimaryWindow() != null) {
            return windowGroup.getPrimaryWindow().getWindowID();
        }
        return null;
    }
    
    /**
     * Returns the UUID of the secondary input.
     * @return  the UUID of the secondary input.
     */
    public UUID getSecondaryInputSelection() {
        if(windowGroup.getSecondaryWindow() != null) {
            return windowGroup.getSecondaryWindow().getWindowID();
        }
        return null;
    }
    
    /**
     * Returns the observable list of UUIDs of the input windows
     * existing within the app. 
     * @return
     */
    public ObservableList<UUID> getInputWindowList() {
        return inputWindows;
    }
    
    /**
     * Returns the property which emits {@link Change} objects describing
     * the change in state from/to selected/deselected.
     * @return  a {@code Change}
     */
    public ObjectProperty<Change<? extends UUID>> selectionProperty() {
        return selectionProperty;
    }
    
    /**
     * Dismisses the flyout if needed and makes sure the state of
     * this {@code InputSelector} is "unselected".
     * 
     * NOTE: This is called automatically in {@link Window#dismissInputSelector()}
     */
    public void reset() {
        inputFlyout.dismiss();
        selectedProperty().set(false);
    }
    
    /**
     * Creates and returns a {@link Flyout}
     * @return  a new {@link Flyout}
     */
    private Flyout createFlyout() {
        setOnAction(e -> {
            if(isSelected()) {
                inputFlyout.flyout();
                flyoutShownProperty.set(true);
            }else{
                inputFlyout.dismiss();
                flyoutShownProperty.set(false);
            }
        });
        
        Flyout retVal = new Flyout(this, createInputSelection());
        retVal.setFlyoutStyle("-fx-background-color: rgb(49, 109, 160, 0.7);-fx-background-radius: 0 0 5 5;");
        
        return retVal;
    }
    
    /**
     * Queries the {@link WindowService} to obtain a reference to the
     * live list of {@link InputWindow}s.
     * @return  live list of {@link InputWindow}s.
     */
    private ObservableList<UUID> getApplicationInputWindowIDs() {
        return WindowService.getInstance().inputWindowListProperty().get();
    }
    
    /**
     * Creates and returns the main {@link Flyout}
     * contents shown when the flyout moves into its
     * "shown" position.
     * 
     * @return  VBox
     */
    private VBox createInputSelection() {
        VBox gp = new VBox();
        gp.setPadding(new Insets(5, 5, 5, 5));
        gp.setSpacing(5);
        
        String prompt = "Choose input window(s):";
        Label l = new Label(prompt);
        l.setFont(Font.font(l.getFont().getFamily(), 12));
        l.setTextFill(Color.WHITE);
        
        Text t = new Text(prompt);
        t.fontProperty().set(l.getFont());
        double width = t.getLayoutBounds().getWidth();
        setMinWidth(width);
        
        windowListView = new WindowListView();
        
        gp.getChildren().addAll(l, windowListView);
        
        return gp;
    }
    
    /**
     * Creates the custom {@link GroupedToggleButton} and its associated
     * state and behavior.
     * 
     * @return  a new {@code GroupedToggleButton}
     */
    private GroupedToggleButton createPrimaryToggle() {
        final GroupedToggleButton b = new GroupedToggleButton("Unselected");
        b.setToggleGroup(null);
        b.setPrefSize(80, 12);
        b.setStyle(OFF_STYLE_STRING);
        b.setFont(Font.font(12d));
        b.selectedProperty().addListener((v,o,n) -> {
            if(n) {
                windowListView.setPrimary(windowListView.getCellForToggle(b));
            } else {
                windowListView.setSecondary(windowListView.getCellForToggle(b));
            }
        });
        b.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if(b.isSelected() || !b.isGrouped()) {
                e.consume();
            }
        });
        b.setFocusTraversable(false);
        return b;
    }
    
    /**
     * Returns the arrow image used to indicate this input selector.
     * @return  the arrow shape
     */
    private Shape getArrowImage() {
        Rectangle r = new Rectangle();
        r.setX(6);
        r.setY(0);
        r.setWidth(8);
        r.setHeight(15);
        
        Polygon arrowHead = new Polygon();
        arrowHead.getPoints().addAll(new Double[] {
           0.0, 15.0,
           20.0, 15.0,
           10.0, 25.0,
           0.0, 15.0
        });
        
        Shape shape = Path.union(r, arrowHead);
        shape.getStyleClass().setAll("input-selector-icon");
        shape.setScaleX(0.7);
        shape.setScaleY(0.5);
        shape.setTranslateX(2.0);
        
        shape.setFocusTraversable(false);
        return shape;
    }
    
    /**
     * Creates and returns the corresponding {@link Change} object used 
     * in conjunction with the {@link #selectionProperty} for additions.
     * @param uuid  changed {@link UUID} 
     * @return  newly constructed {@code Change} containing the specified {@link UUID}
     */
    private static Change<UUID> createAddedChange(UUID uuid) {
        ListChangeListener.Change<UUID> c = new ListChangeListener.Change<UUID>(FXCollections.<UUID>singletonObservableList(uuid)) {
            @Override public boolean next() { return !getList().isEmpty(); }
            @Override public void reset() {}
            @Override public int getFrom() { return 0; }
            @Override public int getTo() { return 1; }
            @Override public boolean wasAdded() { return true; }
            @Override public boolean wasRemoved() { return false; }
            @Override public List<UUID> getRemoved() { return Collections.emptyList(); }
            @Override protected int[] getPermutation() { return new int[0]; }
        };
        return c;
    }
    
    /**
     * Creates and returns the corresponding {@link Change} object used 
     * in conjunction with the {@link #selectionProperty} for removals.
     * @param uuid  changed {@link UUID} 
     * @return  newly constructed {@code Change} containing the specified {@link UUID}
     */
    private static Change<UUID> createRemovedChange(UUID uuid) {
        ListChangeListener.Change<UUID> c = new ListChangeListener.Change<UUID>(FXCollections.<UUID>singletonObservableList(uuid)) {
            @Override public boolean next() { return !getList().isEmpty(); }
            @Override public void reset() {}
            @Override public int getFrom() { return 0; }
            @Override public int getTo() { return 1; }
            @Override public boolean wasAdded() { return false; }
            @Override public boolean wasRemoved() { return true; }
            @Override public List<UUID> getRemoved() { return getList(); }
            @Override public int getRemovedSize() { return 1; }
            @Override protected int[] getPermutation() { return new int[0]; }
        };
        return c;
    }
    
    
    //////////////////////////////////////////////////
    //               Inner Classes                  //
    //////////////////////////////////////////////////
    
    /**
     * Contains all the selection logic
     */
    private static class WindowLogic {
        private InputSelector selector;
        
        private WindowListView windowListView;
        
        private ToggleGroup primaryToggles = new ToggleGroup();
        
        private ListChangeListener<? super UUID> windowListListener;
        
        ObservableList<UUID> windowList;
        
        public WindowLogic(InputSelector selector, WindowListView listView, ObservableList<UUID> windowList) {
            this.selector = selector;
            this.windowListView = listView;
            this.windowList = windowList;
            
            windowList.addListener(windowListListener = (Change<? extends UUID> c) -> {
                c.next();
                if(c.wasRemoved()) {
                    windowRemoved(c);
                } else if(c.wasAdded()) {
                    windowAdded(c);
                }            
            });
            
            windowList.stream().forEach(c -> change(createAddedChange(c)));
            
            primaryToggles.selectedToggleProperty().addListener((v,o,n) -> {
                if(n != null) {
                    selector.windowGroup.primaryWindowProperty().set(
                        WindowService.getInstance().windowFor(
                            windowListView.getCellForToggle((GroupedToggleButton)n).uuid));
                } 
            });
        }
        
        /**
         * Disconnects the logic's listener from the applications Window List.
         */
        private void disconnect() {
            if(windowListListener != null) {
                windowList.removeListener(windowListListener);
            }
        }
        
        /**
         * Forwards the {@link Change} by routing it to its correct handler.
         * @param c     the Change object
         */
        private void change(Change<? extends UUID> c) {
            c.next();
            if(c.wasRemoved()) {
                windowRemoved(c);
            } else if(c.wasAdded()) {
                windowAdded(c);
            }  
        }
        
        /**
         * Handler for window additions which updates the cells in the {@link WindowListView}.
         * @param c     the {@link Change} object
         */
        public void windowAdded(Change<? extends UUID> c) {
            UUID u = c.getAddedSubList().get(0);
            windowListView.addCell(u);
            windowListView.requestLayout();
        }
        
        /**
         * Handler for window removals which updates the cells in the {@link WindowListView}.
         * @param c     the {@link Change} object
         */
        public void windowRemoved(Change<? extends UUID> c) {
            UUID u = c.getRemoved().get(0);
            windowListView.removeCell(u);
            windowListView.requestLayout();
        }
        
        /**
         * Changes the Cell appearance of the "primary" toggle
         * to reflect being grouped if selected and all selecteds
         * are < 2.
         * @param cell  the Cell to attempt grouping
         * @return  true if was grouped, false if not
         */
        public boolean selectCell(Cell cell) {
            int numSelected = (int)windowListView.getCells().stream()
                .filter(c -> c.checkBox.isSelected()) 
                .count();
            
            if(numSelected > 2) {
                cell.checkBox.setSelected(false);
                cell.primaryToggle.setToggleGroup(null);
                cell.primaryToggle.setGrouped(false);
                return false;
            }
            
            if(!primaryToggles.getToggles().contains(cell.primaryToggle)) {
                primaryToggles.getToggles().add(cell.primaryToggle);
                selector.windowGroup.addWindow(WindowService.getInstance().windowFor(cell.uuid));
                cell.primaryToggle.setGrouped(true);
            }
            
            if(numSelected == 1) {
                windowListView.setPrimary(cell);
                primaryToggles.selectToggle(cell.primaryToggle);
            }
            
            return true;
        }
        
        /**
         * Ungroups the "primary" toggle of the Cell 
         * specified and changes its appearance.
         * @param cell  The Cell to ungroup
         * @return false always
         */
        public boolean deSelectCell(Cell cell) {
            boolean removedPrimary = false;
            if(primaryToggles.getSelectedToggle() == cell.primaryToggle) {
                primaryToggles.selectToggle(null);
                removedPrimary = true;
            }
            cell.primaryToggle.setToggleGroup(null);
            cell.primaryToggle.setGrouped(false);
            selector.windowGroup.removeWindow(WindowService.getInstance().windowFor(cell.uuid));
            removedPrimary |= primaryToggles.getToggles().remove(cell.primaryToggle);
            
            if(removedPrimary) {
                if(!primaryToggles.getToggles().isEmpty()) {
                    Cell c = windowListView.getCellForToggle((GroupedToggleButton)primaryToggles.getToggles().get(0));
                    windowListView.setPrimary(c);
                    primaryToggles.selectToggle(c.primaryToggle);
                }
            }
            
            return false;
        }
    }
        
    /**
     * Main container view of the list of windows.
     * @return  ScrollPane
     */
    private class WindowListView extends StackPane {
        private VBox vBox;
        
        private ObservableList<Node> cells;
        
        private Label selecteds;
        
        private ChangeListener<Boolean> initialListener;
        private ChangeListener<String> titleChangeListener;
        private ChangeListener<Paint> idColorChangeListener;
        
        /**
         * Constructs the {@code WindowListView}
         */
        public WindowListView() {
            VBox container = new VBox(2);
            container.setStyle("-fx-background-color: transparent;-fx-padding: 0 5 0 0;");
            
            vBox = new VBox(3);
            vBox.setStyle("-fx-background-color: transparent;");
            cells = vBox.getChildren();
            vBox.layoutBoundsProperty().addListener((v, o, n) -> {
                javafx.stage.Window w = inputFlyout.getFlyoutContainer().getScene().getWindow();
                if(w != null) {
                    Platform.runLater(() -> {
                        double width = vBox.getChildren()
                            .stream()
                            .map(c -> (Cell)c)
                            .mapToDouble(c -> c.computeWidth())
                            .max()
                            .orElseGet(() -> InputSelector.this.getMinWidth());
                        w.setWidth(width + 40);
                        w.setHeight(59 + (cells.size() * 22) + (cells.size() * 3));
                        selecteds.setPrefWidth(inputFlyout.getPopup().getWidth() - 10);
                    });
                }
            });
            
            final String emptyStyle = "-fx-background-color: rgb(49, 109, 160);" +
                            "-fx-text-fill: silver;" +
                            "-fx-background-radius: 5;" +
                            "-fx-padding: 0 5 0 5;" +
                            "-fx-font-size: 14;" +
                            "-fx-font-weight: bold;";
            
            final String nonEmptyStyle = "-fx-background-color: rgb(49, 109, 160);" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 5;" +
                            "-fx-padding: 0 5 0 5;" +
                            "-fx-font-size: 12;" +
                            "-fx-font-weight: normal;";
            
            selecteds = new Label("Selected Windows");
            selecteds.setStyle(emptyStyle);
            selecteds.setAlignment(Pos.CENTER);
            selecteds.setPrefHeight(22);
            selecteds.setMinHeight(22);
            selecteds.textProperty().addListener((v,o,n) -> {
                selecteds.setAlignment(n.equals("Selected Windows") ? Pos.CENTER : Pos.CENTER_LEFT);
                selecteds.setStyle(n.equals("Selected Windows") ? emptyStyle : nonEmptyStyle);
                selecteds.setText(n == null || n.isEmpty() ? "Selected Windows" : selecteds.getText());
            });
            
            Pane spacer = new Pane();
            spacer.setPrefWidth(5);
            spacer.setMinHeight(3);
            spacer.setMaxHeight(2);
            
            Pane spacer2 = new Pane();
            spacer.setPrefWidth(5);
            spacer.setMinHeight(4);
            spacer.setMaxHeight(4);
            
            container.getChildren().addAll(spacer, selecteds, spacer2, vBox);
            
            setPrefHeight(42);
            
            getChildren().add(container);
            
            // Make the width of the label listing selected windows track the 
            // flyout window width
            flyoutShownProperty.addListener(initialListener = (v,o,n) -> {
                flyoutShownProperty.removeListener(initialListener);
                Platform.runLater(() -> {
                    selecteds.setMinWidth(inputFlyout.getPopup().getWidth() - 10);
                });
            });
            
            // Update the window title list for input window title changes
            WindowService.getInstance().inputWindowTitleChangeProperty().addListener(titleChangeListener = (v,o,n) -> {
                updateInputWindowProperties();
            });
            
            // Update the window title list for input window color id changes
            WindowService.getInstance().inputWindowColorIDChangeProperty().addListener(idColorChangeListener = (v,o,n) -> {
                updateInputWindowProperties();
            });
        }
        
        /**
         * Adds a Cell which represents the {@link InputWindow} whose ID
         * matches that specified
         * 
         * @param id    the id of the InputWindow which will have a Cell 
         *              created for it.
         */
        int fauxNum; // Var used for testing only!
        public void addCell(UUID id) {
            // Maintain this "hook" which indicates use within the InputTester
            // as opposed to normal use within the application
            Window w = WindowService.getInstance().windowFor(id);
            Cell cell = w == null ?
                new Cell(id, cells.size() == 2 ? "Window hippopotamus therein: " + (fauxNum++) : "Window " + (fauxNum++)) :
                    new Cell(id, WindowService.getInstance().windowFor(id).getTitle());
                
            ((InputWindow)w).getTitleBar().titleSetProperty().addListener((v,o,n) -> {
                showSelecteds();
            });
            
            cell.rectangle.setFill(w.getTitleBar().getColorIDTab().getPaint());
            
            cells.add(cell);
            cell.checkBox.selectedProperty().addListener((v,o,n) -> {
                boolean b = n ? logic.selectCell(cell) : logic.deSelectCell(cell);
                
                selectionProperty.set(b ? createAddedChange(cell.uuid) : createRemovedChange(cell.uuid));
                
                if(b || !n) {
                    showSelecteds();
                }
            });
        }
        
        /**
         * Removes the Cell who's InputWindow has the corresponding 
         * UUID
         * @param id    the UUID of the InputWindow who's cell is removed
         */
        public void removeCell(UUID id) {
            logic.deSelectCell((Cell)windowListView.cells.stream()
                .filter(c -> ((Cell)c).uuid == id) 
                .findFirst()
                .get());
            
            int indexToRemove = IntStream.range(0, cells.size())
                .filter(i -> ((Cell)cells.get(i)).uuid == id)
                .findFirst()
                .getAsInt();
            
            cells.remove(indexToRemove);  
            
            showSelecteds();
        }
        
        /**
         * Sets the text of the selecteds label
         */
        public void showSelecteds() {
            String selectStr = getSelectedCells().stream()
                .map(c -> c.titleLabel.getText())
                .collect(Collectors.joining(", "));
            selecteds.setText(selectStr); 
        }
        
        /**
         * Called to implement user changes to an {@link InputWindow}'s 
         * title or color id.
         */
        public void updateInputWindowProperties() {
            cells.stream()
                .map(c -> (Cell)c)
                .forEach(c -> {
                    InputWindow iw = (InputWindow)WindowService.getInstance().windowFor(c.uuid);
                    c.titleLabel.setText(iw.getTitle());
                    c.rectangle.setFill(iw.getTitleBar().getColorIDTab().getPaint());
                });
        }
        
        /**
         * Returns the {@link Cell} containing the specified toggle button.
         * @param g     the toggle button whose parent Cell is returned.
         * @return  the parent Cell containing the specified toggle
         */
        public Cell getCellForToggle(GroupedToggleButton g) {
            return getCells().stream().filter(c -> c.primaryToggle == g).findFirst().orElse(null);
        }
        
        /**
         * Returns all the {@link Cell}s representing all InputWindows
         * @return  the list of all Cells representing all InputWindows
         */
        public List<Cell> getCells() {
            return cells.stream().map(n -> (Cell)n).collect(Collectors.toList());
        }
        
        /**
         * Returns a list of those {@link Cell}s which are selected
         * @return  a list of selected Cells
         */
        public List<Cell> getSelectedCells() {
            return cells.stream()
                .map(n -> (Cell)n)
                .filter(c -> c.checkBox.isSelected())
                .collect(Collectors.toList());
        }
        
        /**
         * Sets the {@link Cell} marked as the primary.
         * @param cell  the visual slot for showing the primary input
         */
        public void setPrimary(Cell cell) {
            cell.setPrimary();
        }
        
        /**
         * Sets the "primary" toggle to be active but not selected
         * @param cell  the cell containing the primary toggle
         */
        public void setSecondary(Cell cell) {
            cell.setSecondary();
        }
    }
    
    /**
     * A single row of widgets and label information 
     * corresponding to an {@link InputWindow}
     */
    private class Cell extends HBox {
        private Label titleLabel;
        private CheckBox checkBox;
        private Rectangle rectangle;
        private GroupedToggleButton primaryToggle;
        private UUID uuid;
        
        public Cell(UUID uuid, String title) {
            super(5);
            
            this.uuid = uuid;
            
            this.checkBox = new CheckBox();
            this.checkBox.setFont(Font.font(10d));
            
            this.rectangle = new Rectangle(15, 15);
            this.rectangle.setFill(ColorIDTab.DEFAULT_ID_COLOR);
            
            this.primaryToggle = createPrimaryToggle();
            
            titleLabel = new Label(title);
            titleLabel.setFont(Font.font(titleLabel.getFont().getFamily(), 12));
            titleLabel.setTextFill(Color.BLACK);
            
            Text t = new Text(title);
            t.fontProperty().set(titleLabel.getFont());
            double width = t.getLayoutBounds().getWidth();
            titleLabel.setMinWidth(width);
            
            getChildren().addAll(checkBox, rectangle, primaryToggle, titleLabel);
            
            setPrefHeight(22);
            setMinHeight(22);
            
            setStyle(
                "-fx-padding: 0 5 0 5;" +
                "-fx-background-radius: 5;" +
                "-fx-background-color:derive(rgb(49, 109, 160, 0.7), 80%);");
            
            setAlignment(Pos.CENTER_LEFT);
            
            setFocusTraversable(false);
        }
        
        public double computeWidth() {
            return 30 + primaryToggle.getWidth() + titleLabel.getMinWidth();
        }
        
        /**
         * Sets the GroupToggleButton to be in the primary state
         */
        public void setPrimary() {
            primaryToggle.setPrimary();
        }
        
        /**
         * Sets the GroupToggleButton to be in the grouped or secondary state
         */
        public void setSecondary() {
            primaryToggle.setSecondary();
        }
    }
    
    /**
     * Custom {@link ToggleButton} that contains 3 states: selected, unselected, grouped.
     * "Grouped" means that the cell containing the ToggleButton is selected among other
     * cells which are selected; and does not refer to the ToggleButton itself being
     * selected.
     */
    public class GroupedToggleButton extends ToggleButton {
        private BooleanProperty groupedProperty = new SimpleBooleanProperty();
        public GroupedToggleButton(String title) {
            super(title);
            groupedProperty.set(false);            
            setFocusTraversable(false);
        }
        
        /**
         * Bound to the cell's selection (CheckBox) property so that this
         * ToggleButton becomes grouped when the cell's checkbox is selected.
         * @return  the BooleanProperty indicating the "grouped" state.
         */
        public BooleanProperty groupedProperty() {
            return groupedProperty;
        }
        
        /**
         * Externally sets this GroupedToggleButton as being grouped.
         * @param b true if grouped, false if not grouped.
         */
        public void setGrouped(boolean b) {
            groupedProperty.set(b);
            
            //Here selected refers to being selected among the primary toggle 
            //group's items (primary button), not the cell selection
            if(b) {
                setSecondary();
            }
            else {
                setUnselected();
            }
        }
        
        /**
         * Sets the GroupToggleButton to be in the primary state
         */
        public void setPrimary() {
            groupedProperty.set(true);
            setStyle(SELECTED_STYLE_STRING);
            setText("Primary");
            Tooltip tt = new Tooltip("Indicates primary view.");
            tt.setStyle("-fx-background-color: rgb(49, 109, 160, .5);");
            setTooltip(tt);
        }
        
        /**
         * Sets the GroupToggleButton to be in the grouped or secondary state
         */
        public void setSecondary() {
            groupedProperty.set(true);
            setStyle(SECONDARY_STYLE_STRING);
            setText("Compare To");
            Tooltip tt = new Tooltip("Click to make primary.");
            tt.setStyle("-fx-background-color: rgb(49, 109, 160, .5);");
            setTooltip(tt);
        }
        
        /**
         * Sets the GroupToggleButton to be in the "ungrouped" state
         */
        public void setUnselected() {
            setStyle(OFF_STYLE_STRING);
            setText("Unselected");
        }
        
        /**
         * Gives the toggle button a disabled appearance, meaning
         * not part of a selection group
         */
        public void setOff() {
            groupedProperty.set(false);
            setStyle(OFF_STYLE_STRING);
        }
        
        /**
         * Returns a flag indicating whether or not this ToggleButton is
         * grouped or not.
         * @return  true if grouped, false if not grouped.
         */
        public boolean isGrouped() {
            return groupedProperty.get();
        }
    }
}
