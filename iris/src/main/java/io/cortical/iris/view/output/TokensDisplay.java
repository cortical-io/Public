package io.cortical.iris.view.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.scene.control.skin.TableHeaderRow;

import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.persistence.ConfigHandler;
import io.cortical.iris.persistence.WindowConfig;
import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import io.cortical.iris.ui.util.POSUiTag;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.retina.core.PosTag;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Callback;


public class TokensDisplay extends LabelledRadiusPane implements View {
    protected static final Logger LOGGER = LoggerFactory.getLogger(TokensDisplay.class);
    private static ConfigHandler<? super WindowConfig> configHandler;
    
    public final ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();
    private BooleanProperty dirtyProperty = new SimpleBooleanProperty(false);
    private OccurrenceProperty autoSendProperty = new OccurrenceProperty();
    private boolean dirtyFlagBlocked;
    private boolean disableSendQuery;
    private ObservableList<POS> root;
    
    private VBox display;
    
    private ToggleButton showHidePosTags;
    
    private Region backPanel;
    private StackPane tableContainer;
    private TableView<POS> table;
    private ScrollPane tokenScroll;
    private TokenDisplay tokenDisplay;
    private Pagination pagination;
    private CheckBox selectAll;
   
    private Polygon showHideArrow;
    
    private boolean isFirstLayout = true;
    
    

    public TokensDisplay(String label, NewBG spec) {
        super(label, spec);
        
        setVisible(false);
        setUserData(ViewType.TOKENS);
        setManaged(false);
        
        display = new VBox(10);
        
        tableContainer = new StackPane();
        tableContainer.getStyleClass().add("pos-table");
        tableContainer.setPadding(new Insets(10, 10, 10, 10));
        
        table = createPOSTable();
        table.setVisible(true);
        pagination = new Pagination(root.size() / 4 + 1, 0);
        pagination.getStyleClass().addAll("tokens-pagination");
        pagination.setMaxHeight(175);
        pagination.setPageFactory(this::createPage);
        pagination.getStyleClass().add(Pagination.STYLE_CLASS_BULLET);
        tableContainer.getChildren().add(pagination);
        tableContainer.setManaged(false);
        
        tokenScroll = new ScrollPane();
        tokenScroll.getStyleClass().add("token-scroll");
        tokenScroll.setPadding(new Insets(10, 10, 5, 5));
        tokenScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        tokenScroll.setManaged(false);
        display.getChildren().add(tokenScroll);
        
        showHidePosTags = createShowHideTagsButton();
        showHidePosTags.relocate(15, pagination.getLayoutBounds().getMaxY() + 15);
        showHidePosTags.resize(180, 25);
        display.getChildren().add(showHidePosTags);
        
        selectAll = new CheckBox();
        selectAll.setVisible(false);
        selectAll.setFocusTraversable(false);
        selectAll.setManaged(false);
        selectAll.selectedProperty().addListener((v,o,n) -> {
            dirtyFlagBlocked = true;
            for(POS pos : root) {
                pos.setSelected(n);
            }
            
            // disableSendQuery flag shuts down query when simply resetting the screen
            if(!disableSendQuery) {
                dirtyProperty.set(n);
                autoSendProperty.set();
            }
            dirtyFlagBlocked = false;
        });
        selectAll.setSelected(true);
        display.getStyleClass().add("pos-select-all-checkbox");
        display.getChildren().add(selectAll);
        
        layoutBoundsProperty().addListener((v,o,n) -> {
            layoutForModeChange();
        });
        
        getChildren().add(display);
        
        Platform.runLater(() -> {
            addInfoButton();
            addAutoSendHandler();
        });
    }
    
    /**
     * Returns the {@link ConfigHandler} instance used to initialize
     * this {@code TokenDisplay}'s {@link PosTag}s settings following
     * deserialization of an {@link OutputWindow}.
     * @return
     */
    public static final ConfigHandler<? super WindowConfig> getChainHandler() {
        if(configHandler == null) {
            configHandler = config -> {
                LOGGER.debug("Executing TokensDisplay chain of responsibility handler");
                Platform.runLater(() -> {
                    if(config.getPrimaryViewType() == ViewType.TEXT) {
                        if(config.getPosTags() != null) {
                            OutputWindow thisWindow = (OutputWindow)WindowService.getInstance().windowFor(config.getWindowID());
                            thisWindow.getViewArea().getTokensDisplay().setSelectedPosTags(config.getPosTags());
                            LOGGER.debug("configure: selecting POSTags = " + config.getPosTags());
                        }
                    } 
                    config.advanceNotificationChain();
                });
            };
        }
        
        return configHandler;
    }
    
    /**
     * Fires the select all checkbox selection which unselects
     * the types.
     */
    public void clearPosTags() {
        selectAll.setSelected(false);
    }
    
    /**
     * Sets the selected PosTags following a deserialization process.
     * @param tags
     */
    public void setSelectedPosTags(Set<PosTag> tags) {
        // Return if they're all selected as in the default
        if(tags != null && tags.size() == 15) return;
        
        if(!showHidePosTags.isSelected()) {
            showHidePosTags.fire();
        }
        
        clearPosTags();
        
        tags.forEach(t -> {
            root.stream()
                .filter(pos -> pos.getTag().tag().equals(t))
                .findFirst()
                .ifPresent(p -> {
                    p.setSelected(true);
                });
        });
    }
    
    /**
     * Implemented by {@code View} subclasses to handle an error
     * 
     * @param	context		the error information container
     */
    @Override
    public void processRequestError(RequestErrorContext context) {}
    
    /**
     * Adds the handler which sends a query request upon local parameter changing.
     */
    public void addAutoSendHandler() {
        autoSendProperty.addListener((v,o,n) -> {
            OutputWindow ow = (OutputWindow)WindowService.getInstance().windowFor(this);
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_RE_EXECUTE_LAST_QUERY.subj() + ow.getWindowID(), new Payload());
        });
    }
    
    /**
     * Sometimes layouts required a bit of "extra" handling upon first layout (i.e.
     * in order to get property listeners to fire and set their values). This flag
     * indicates whether the view has been initially laid out or not.
     */
    @Override
    public boolean isFirstLayout() {
        if(isFirstLayout) {
            isFirstLayout = false;
            return true;
        }
        return isFirstLayout;
    }
        
    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        tokenScroll.setContent(null);
        resetPosTags();
    }

    /**
     * Returns the property which is updated with a new {@link Payload} upon
     * message model creation.
     * @return  the message property
     */
    @Override
    public ObjectProperty<Payload> messageProperty() {
        return messageProperty;
    }
    
    /**
     * Returns the property which indicates that a new search criterion
     * has been selected.
     * @return
     */
    public BooleanProperty dirtyProperty() {
        return dirtyProperty;
    }
    
    /**
     * Returns the set of selected {@link PosTag}s.
     * @return
     */
    public Set<PosTag> getSelectedPOSTags() {
        return ((Stream<POS>)root.stream())
            .filter(pos -> pos.isSelected())
            .map(pt -> pt.getTag().tag())
            .collect(Collectors.toSet());
    }
    
    /**
     * Returns or creates if null, the panel used to display
     * information on the "back" of this view's {@link Window}
     * @return
     */
    @Override
    public Region getOrCreateBackPanel() {
        if(backPanel == null) {
            backPanel = new TokensBackPanel(10).getScroll();
        }
        
        return backPanel;
    }
    
    /**
     * Creates and returns the {@link TableView} used to show the context information.
     * @return  the context TableView
     */
    @SuppressWarnings("unchecked")
    public TableView<POS> createPOSTable() {
        TableColumn<POS, Boolean> selectedCol = new TableColumn<>();
        selectedCol.setSortable(false);
        selectedCol.setResizable(false);
        selectedCol.setMinWidth(35);
        selectedCol.setMaxWidth(35);
        selectedCol.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectedCol.setCellFactory(new Callback<TableColumn<POS, Boolean>, TableCell<POS, Boolean>>() {
            public TableCell<POS, Boolean> call(TableColumn<POS, Boolean> p) {
                CheckBoxTableCell<POS, Boolean> cell = new CheckBoxTableCell<POS, Boolean>();
                cell.setAlignment(Pos.TOP_CENTER);
                return cell;
            }
        });
        selectedCol.setEditable(true);
        
        TableColumn<POS, String> idCol = new TableColumn<>("Tag");
        idCol.setSortable(false);
        idCol.setResizable(false);
        idCol.setMinWidth(70);
        idCol.setMaxWidth(70);
        idCol.setCellValueFactory(new PropertyValueFactory<>("label"));
        idCol.setCellFactory(new Callback<TableColumn<POS, String>, TableCell<POS, String>>() {
            public TableCell<POS, String> call(TableColumn<POS, String> p) {
                TableCell<POS, String> cell = new TableCell<POS, String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        if(empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(item);
                        }
                    }
                };
                cell.setAlignment(Pos.TOP_CENTER);
                return cell;
            }
        });
        
        TableColumn<POS, String> descCol = new TableColumn<>("Description");
        descCol.setSortable(false);
        descCol.setResizable(true);
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setCellFactory(new Callback<TableColumn<POS, String>, TableCell<POS, String>>() {
            public TableCell<POS, String> call(TableColumn<POS, String> p) {
                TableCell<POS, String> cell = new TableCell<POS, String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        if(empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(item);
                        }
                    }
                };
                cell.setAlignment(Pos.TOP_CENTER);
                return cell;
            }
        });
        
        root = createPOSItems(); 
        TableView<POS> table = new TableView<>(root);
        table.setFocusTraversable(false);
        table.setTableMenuButtonVisible(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(25);
        table.setPrefHeight(table.getFixedCellSize() * 4 + 30);
        table.setMaxHeight(table.getFixedCellSize() * 4 + 30);
        table.getSelectionModel().setCellSelectionEnabled(false);
        table.getColumns().addAll(selectedCol, idCol, descCol);
        
        // The only way to disable column reordering?
        // http://stackoverflow.com/questions/22202782/how-to-prevent-tableview-from-doing-tablecolumn-re-order-in-javafx-8
        table.widthProperty().addListener((v,o,n) -> {
            TableHeaderRow header = (TableHeaderRow) table.lookup("TableHeaderRow");
            header.reorderingProperty().addListener((v2,o2,n2) -> {
                header.setReordering(false);
            });
        });
        
        return table;
    }
    
    private Node createPage(int pageIndex) {
        int fromIndex = pageIndex * 4;
        int toIndex = Math.min(fromIndex + 4, root.size());
        table.setItems(FXCollections.observableArrayList(root.subList(fromIndex, toIndex)));
        
        return new BorderPane(table);
    }
    
    public ObservableList<POS> createPOSItems() {
        ObservableList<POS> list = FXCollections.observableArrayList();
        List<POS> l = Arrays.stream(POSUiTag.values()).map(tag -> new POS(tag)).collect(Collectors.toList());
        list.addAll(l);
        return list;
    }
    
    public class POS {
        private BooleanProperty selected = new SimpleBooleanProperty(false);
        private SimpleStringProperty label = new SimpleStringProperty();
        private SimpleStringProperty description = new SimpleStringProperty();
        private POSUiTag tag;
        
        private CheckBox checkBox;
        private ChangeListener<Boolean> selectListener;
        
        public POS(POSUiTag tag) {
            this.tag = tag;
            this.label.set(tag.name());
            this.description.set(tag.description());
            
            selected.addListener(selectListener = (v,o,n) -> {
                // Flag change
                if(!dirtyFlagBlocked && !getSelectedPOSTags().isEmpty()) {
                    dirtyProperty.set(true);
                    autoSendProperty.set();
                }
                
                if(!n) {
                    selected.removeListener(selectListener);
                    selectAll.setSelected(false);
                    selected.addListener(selectListener);
                }
            });
        }
        
        public BooleanProperty selectedProperty() { return selected; }
        
        /**
         * Returns a flag indicating whether the specified 
         * context is selected
         * @return
         */
        public boolean isSelected() { return selected.get(); }

        /**
         * Sets the row given by this Context to have a selected context
         * label.
         * @param b
         */
        public void setSelected(boolean b) { this.selected.set(b); }
        
        /**
         * Returns the label property
         * @return
         */
        public String getLabel() { return this.label.get(); }
        
        /**
         * Returns the StringProperty containing a POS Tag's description.
         * @return
         */
        public String getDescription() { return description.get(); }
        
        /**
         * Returns the POSUiTag this POS represents.
         * @return
         */
        public POSUiTag getTag() { return tag; }
        
        /**
         * Sets this Context's CheckBox
         * @param cb
         */
        public void setCheckBox(CheckBox cb) { this.checkBox = cb; }
        
        /**
         * De-selects all checkboxes.
         */
        @SuppressWarnings("unused")
        private void deselectAll() {
            root.stream()
                .filter(pos -> !pos.equals(this) && pos.checkBox != null)
                .forEach(pos -> {
                    pos.checkBox.setSelected(false);   
                });
        }
    }
    
    /**
     * Custom TableCell implementation for using boolean values within
     * a Table.
     * 
     * @param <S>
     * @param <T>
     */
    public static class CheckBoxTableCell<S, T> extends TableCell<S, T> {
        private final CheckBox checkBox;
        private ObservableValue<T> ov;

        public CheckBoxTableCell() {
            this.checkBox = new CheckBox();
            this.checkBox.setFocusTraversable(false);
            this.checkBox.setAlignment(Pos.CENTER);
            setAlignment(Pos.CENTER);
            setGraphic(checkBox);
        }  

        @Override public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            
            POS tag = (POS)tableRowProperty().get().getItem();
            if (empty || tag == null || tag.getLabel().isEmpty()) {
                setText(null);
                setGraphic(null);
            } else {
                tag.setCheckBox(checkBox);
                setGraphic(checkBox);

                if (ov instanceof BooleanProperty) {
                    checkBox.selectedProperty().unbindBidirectional((BooleanProperty) ov);
                }

                ov = getTableColumn().getCellObservableValue(getIndex());

                if (ov instanceof BooleanProperty) {
                    checkBox.selectedProperty().bindBidirectional((BooleanProperty) ov);
                }
            }

        }
    }
    
    private static TokenDisplay createTokenDisplay(String text, List<String> sentenceTokens, List<String> tokens ) {
        List<String> l = new ArrayList<>();
        int mark = 0;
        int idx = 0;
        while(idx < sentenceTokens.size()) {
            String[] sa = patchHyphens(sentenceTokens.get(idx)).split("[\\s]*\\,[\\s]*");
            mark = text.toLowerCase().indexOf(sa[0], mark);
            int end = -1;
            String sentence = idx == sentenceTokens.size() - 1 ? 
                text.substring(mark) :
                    text.substring(mark, end = (text.toLowerCase().indexOf(sa[sa.length - 1], mark) + sa[sa.length - 1].length()) );
            mark = end;
            l.add(sentence);
            idx++;
        }
        
        TokenDisplay td = new TokenDisplay();
        for(int i = 0;i < l.size();i++) {
            td.addTokenLine(new TokenLine(l.get(i), tokens.get(i)));
        }
        
        return td;
    }
    
    /**
     * Fixes a Bug in the API where hyphenated terms have a space inserted
     * after the hyphen.
     * 
     * @param s
     * @return
     */
    private static String patchHyphens(String s) {
        Pattern hyphenError = Pattern.compile("[.+{1}[^\\s]]+([\\-]{1}[\\s]){1}.+{1}");
        String retVal = s;
        Matcher m = hyphenError.matcher(s);
        while(m.find()) {
            retVal = retVal.replace(m.group(0), m.group(0).replace(" ", ""));
        }
        
        return retVal;
    }
    
    public static void main(String[] args) {
        patchHyphens("al[   - jazari]- help");
    }
    
    /**
     * Returns a flag indicating whether the token display internal view node
     * has been created. It usually doesn't get created until {@link #setSentences(String)} 
     * is called.
     * @return
     */
    public boolean tokenDisplayConfigured() {
        return tokenDisplay != null;
    }
    
    /**
     * Populates this view with the initial "source" sentences. The view
     * is later updated with the server response via the method {@link TokenDisplay#update(List)}.
     * 
     * @param text              the initial text the user typed
     * @param sentences    the tokenized full sentence. (needed to display tokens next to their 
     *                          original sentences).
     * @param tokens            the POS tokens
     */
    public void setSentencesAndTokens(String text, List<String> sentences, List<String> tokens) {
        tokenDisplay = createTokenDisplay(text, sentences, tokens);
        tokenScroll.setContent(tokenDisplay);
        Platform.runLater(() -> {
            OutputWindow ow = (OutputWindow)WindowService.getInstance().windowFor(this);
            ow.dragThumb(1, 1);
            ow.requestLayout();
        });
    }
    
    /**
     * Populates this view with the List of token strings received from the server.
     * @param l
     */
    public void setTokens(List<String> l) {
        if(tokenDisplay == null) {
            System.out.println("repopulate");
        }
        tokenDisplay.update(l);
    }
    
    private static class TokenLine extends VBox {
        private static int IDX = 1;
        
        private Text sentence = new Text("");
        private Text tokens = new Text("");
        
        private Pane idxDisplay;
        private int lineIdx;
        
        private VBox original;
        
        public TokenLine(String sentence, String tokens) {
            super(15);
            this.sentence.setText(sentence);
            
            original = new VBox(5);
            Text originalSentence = new Text("Original Sentence");
            originalSentence.setStyle("-fx-font-size: 12; -fx-font-style: italic; -fx-font-weight: bold;");
            original.getChildren().addAll(originalSentence, this.sentence);
            
            this.tokens.setText(tokens);
            this.tokens.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-fill: rgb(49, 109, 160);");
            getStyleClass().add("tokens-display");
            
            lineIdx = IDX++;
            idxDisplay = getIndexDisplay();
            
            getChildren().addAll(this.idxDisplay, this.tokens, original);
        }
        
        public void setWrappingWidth(double width) {
            sentence.setWrappingWidth(width);
            tokens.setWrappingWidth(width);
        }
        
        public void setTokens(String s) {
            tokens.setText(s);
            getChildren().clear();
            getChildren().addAll(this.idxDisplay, this.tokens, original);
            System.out.println("tk layout bounds: " + tokens.getLayoutBounds());
        }
        
        private Pane getIndexDisplay() {
            Pane indexDisplay = new Pane();
            indexDisplay.setLayoutX(0);
            indexDisplay.setLayoutY(0);
            indexDisplay.getStyleClass().add("indexed-display");
            indexDisplay.setManaged(false);
                        
            Text label = new Text("Sentence " + lineIdx);
            label.getStyleClass().add("indexed-display-text");
            label.setFont(Font.font("Helvetica", FontWeight.NORMAL, 11));
            label.setTextOrigin(VPos.CENTER);
            label.xProperty().bind(indexDisplay.widthProperty().divide(2).subtract(label.getLayoutBounds().getWidth() / 2.0));
            label.yProperty().bind(indexDisplay.heightProperty().divide(2));
            indexDisplay.resize(label.getLayoutBounds().getWidth() + 20, label.getLayoutBounds().getHeight() * 1.5); 
            
            indexDisplay.getChildren().add(label);
            
            return indexDisplay;
        }
    }
    
    private static class TokenDisplay extends VBox {
        public TokenDisplay() {
            super(10);
            TokenLine.IDX = 1;
        }
        
        public void addTokenLine(TokenLine tl) {
            getChildren().add(tl);
        }
        
        public List<TokenLine> getLines() {
            return getChildren()
                .stream()
                .map(n -> (TokenLine)n)
                .collect(Collectors.toList());
        }
        
        public void update(List<String> l) {
            Platform.runLater(() -> {
                for(int i = 0;i < getChildren().size();i++) {
                    if(i >= l.size()) break;
                    ((TokenLine)getChildren().get(i)).setTokens(l.get(i));
                }
                
                this.requestFocus();
                this.requestLayout();
            });
        }
    }
    
    private ToggleButton createShowHideTagsButton() {
        showHideArrow = new Polygon();
        showHideArrow.getPoints().addAll(new Double[]{
            3.0,0.0,
            6.0, 6.0,
            0.0, 6.0,
            3.0, 0.0 });
        showHideArrow.setFill(Color.WHITE);
        showHidePosTags = new ToggleButton("Show POS tags to filter tokens", showHideArrow);
        showHidePosTags.setSelected(false);
        showHidePosTags.setManaged(false);
        showHidePosTags.setFocusTraversable(false);
        showHidePosTags.getStyleClass().add("showhidepostags-button");
        showHidePosTags.setOnAction(e -> {
            showHidePosTags.setText(showHidePosTags.isSelected() ? "Hide POS Tags" : "Show POS tags to filter tokens");
            showHideArrow.rotateProperty().set(showHidePosTags.isSelected() ? 0 : 180);
            if(showHidePosTags.isSelected()) {
                showPosTags();
                selectAll.setVisible(true);
                selectAll.relocate(22, 58);
            }else{
                hidePosTags();
                selectAll.setVisible(false);
            }
            
            requestLayout();
        });
        
        return showHidePosTags;
    }
    
    private void layoutForModeChange() {
        Bounds n = getLayoutBounds();
        tableContainer.resize(n.getWidth() - 10, table.getFixedCellSize() * 5 + 85);
        tableContainer.relocate(5, labelHeightProperty().get());
        tokenScroll.relocate(15, showHidePosTags.isSelected() ? 220 : 70);
        tokenScroll.resize(tableContainer.getWidth() - 20, showHidePosTags.isSelected() ? n.getHeight() - 230 : n.getHeight() - 80);
        
        showHidePosTags.relocate(15, showHidePosTags.isSelected() ? 184 : labelHeightProperty().get() + 15);
        if(tokenDisplay != null) {
            tokenDisplay.getLines().stream().forEach(tl -> tl.setWrappingWidth(n.getWidth() - 60));
        }
    }
    
    private void hidePosTags() {
        display.getChildren().remove(tableContainer);
        requestLayout();
        Platform.runLater(() -> {
            layoutForModeChange();
        });
    }
    
    private void showPosTags() {
        display.getChildren().add(0, tableContainer);
        requestLayout();
        Platform.runLater(() -> {
            layoutForModeChange();
        });
    }
    
    /**
     * Called during {@link #reset()} to select the screens default state
     * and reset to the initial state avoiding any query invocations.
     */
    private void resetPosTags() {
        if(showHidePosTags.getText().indexOf("Hide") != -1) {
            disableSendQuery = true;
            showHidePosTags.fire();
            selectAll.setSelected(true);
            selectAll.setVisible(false);
            disableSendQuery = false;
        }
    }
    
    /**
     * Adds the "flip-over" info invoking button to this view.
     */
    private void addInfoButton() {
        Window w = WindowService.getInstance().windowFor(this);
        ObjectProperty<Point2D> infoLoc = new SimpleObjectProperty<>();
        w.layoutBoundsProperty().addListener((v,o,n) -> {
            infoLoc.set(new Point2D(n.getWidth() - 35, 5));
        });
        getChildren().add(WindowService.createInfoButton(w, this, infoLoc));
    }
}
