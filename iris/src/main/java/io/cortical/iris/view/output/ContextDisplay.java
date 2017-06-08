package io.cortical.iris.view.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import io.cortical.iris.ui.util.DragAssistant;
import io.cortical.iris.ui.util.TermLookupAssistant;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.retina.model.Term;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import rx.Observable;
import rx.Subscriber;


public class ContextDisplay extends LabelledRadiusPane implements View {
    
    public ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();
    private BooleanProperty dirtyProperty = new SimpleBooleanProperty(false);
    private OccurrenceProperty autoSendProperty = new OccurrenceProperty();
    private boolean dirtyFlagBlocked;
    
    private TreeTableView<Context> treeTable;
    private TreeItem<Context> rootItem;
    private StackPane tableView;
    private CheckBox selectAllCheckBox;
    
    private List<Integer> lastSelection = new ArrayList<>();
    
    private Observable<ContextEvent> contextObservable;
    private List<Subscriber<? super ContextEvent>> contextObservers = new ArrayList<>();
    
    private Region backPanel;
    
    
    
    /**
     * Constructs a new {@code ContextDisplay}
     * 
     * @param label     the title used in the radiused tab
     * @param spec      specifies tab colors
     */
    public ContextDisplay(String label, NewBG spec) {
        super(label, spec);

        setVisible(false);
        setUserData(ViewType.CONTEXTS);
        setManaged(false);

        tableView = new StackPane();
        layoutBoundsProperty().addListener((v,o,n) -> {
            tableView.setPrefWidth(n.getWidth());
            tableView.setPrefHeight(n.getHeight() - 30);
            tableView.setLayoutX(0);
            tableView.layoutYProperty().bind(labelHeightProperty().add(5));
        });
        tableView.setPadding(new Insets(10, 10, 10, 10));
        
        treeTable = createContextTable();
        tableView.getStyleClass().addAll("context-display");
        tableView.getChildren().add(treeTable);

        getChildren().add(tableView);
        
        getChildren().add(selectAllCheckBox = createSelectAllCheckBox());
        
        Platform.runLater(() -> {
            addAutoSendHandler();
            addInfoButton();
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
     * Creates the stand-alone {@link CheckBox} which auto selects
     * the check boxes of all rows.
     * @return  the select all check box
     */
    private CheckBox createSelectAllCheckBox() {
        CheckBox cb = new CheckBox();
        cb.setFocusTraversable(false);
        cb.setManaged(false);
        cb.resize(10, 10);
        cb.relocate(22, 75);
        cb.toFront();
        cb.selectedProperty().addListener((v,o,n) -> {
            for(TreeItem<Context> item : rootItem.getChildren()) {
                item.getValue().setSelected(n);
            }
            notifyContextObservers(createContextEvent());
        });
        
        return cb;
    }

    /**
     * Creates and returns the {@link TableView} used to show the context information.
     * @return  the context TableView
     */
    @SuppressWarnings("unchecked")
    public TreeTableView<Context> createContextTable() {
        
        TreeTableColumn<Context, Boolean> selectedCol = new TreeTableColumn<>();
        selectedCol.setSortable(false);
        selectedCol.setResizable(false);
        selectedCol.setMinWidth(40);
        selectedCol.setMaxWidth(40);
        selectedCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("selected"));
        selectedCol.setCellFactory(new Callback<TreeTableColumn<Context, Boolean>, TreeTableCell<Context, Boolean>>() {
            public TreeTableCell<Context, Boolean> call(TreeTableColumn<Context, Boolean> p) {
                CheckBoxTreeTableCell<Context, Boolean> cell = new CheckBoxTreeTableCell<Context, Boolean>();
                cell.setAlignment(Pos.TOP_CENTER);
                return cell;
            }
        });
        selectedCol.setEditable(true);
        
        TreeTableColumn<Context, String> idCol = new TreeTableColumn<>("ID");
        idCol.setSortable(false);
        idCol.setResizable(false);
        idCol.setMinWidth(40);
        idCol.setMaxWidth(40);
        idCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("id"));
        idCol.setCellFactory(new Callback<TreeTableColumn<Context, String>, TreeTableCell<Context, String>>() {
            public TreeTableCell<Context, String> call(TreeTableColumn<Context, String> p) {
                TreeTableCell<Context, String> cell = new TreeTableCell<Context, String>() {
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

        TreeTableColumn<Context, String> contextCol = new TreeTableColumn<>("Context Label");
        contextCol.setSortable(false);
        contextCol.setMinWidth(100);
        contextCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("label"));
        contextCol.setCellFactory(new Callback<TreeTableColumn<Context, String>, TreeTableCell<Context, String>>() {
            public TreeTableCell<Context, String> call(TreeTableColumn<Context, String> p) {
                TreeTableCell<Context, String> cell = new TreeTableCell<Context, String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        if(empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            Text t = new Text(item);
                            t.setFont(Font.font("Questrial", FontWeight.BOLD, FontPosture.REGULAR, 14));
                            DragAssistant.configureDragHandler(t, null);
                            setGraphic(t);
                        }
                    }
                };
                cell.setAlignment(Pos.TOP_CENTER);
                return cell;
            }
        });

        TreeTableColumn<Context, String> similarTermsCol = new TreeTableColumn<>("Similar Terms");
        similarTermsCol.setSortable(false);
        similarTermsCol.setMinWidth(200);
        similarTermsCol.setCellValueFactory(new TreeItemPropertyValueFactory<>("similarTerms"));
        similarTermsCol.setCellFactory(new Callback<TreeTableColumn<Context, String>, TreeTableCell<Context, String>>() {
            public TreeTableCell<Context, String> call(TreeTableColumn<Context, String> p) {
                TreeTableCell<Context, String> cell = new TreeTableCell<>();
                TextFlow textFlow = new TextFlow();
                textFlow.maxWidthProperty().bind(similarTermsCol.widthProperty());
                cell.setGraphic(textFlow);
                cell.itemProperty().addListener((v,o,n) -> {
                    textFlow.getChildren().clear();
                    if(n != null) {
                        if(n.indexOf(",") != -1) {
                            Text sizer = new Text();
                            sizer.setFont(Font.font("Questrial", FontWeight.BOLD, FontPosture.REGULAR, 14));
                            StringBuilder sb = new StringBuilder();
                            Arrays.stream(n.split("[\\,\\s]+")).map(s -> {
                                Text t = new Text(s);
                                t.setFont(Font.font("Questrial", FontWeight.BOLD, FontPosture.REGULAR, 14));
                                DragAssistant.configureDragHandler(t, cell);
                                sb.append(s).append(", ");
                                return t;
                            }).forEach(t -> textFlow.getChildren().addAll(t, new Text(", ")));
                            
                            sizer.setText(sb.toString());
                            sizer.setWrappingWidth(similarTermsCol.widthProperty().get());
                            textFlow.setPrefHeight(sizer.getLayoutBounds().getHeight() + 5);
                        } else {
                            Text t = new Text(n);
                            t.setFont(Font.font("Questrial", FontWeight.BOLD, FontPosture.REGULAR, 14));
                            textFlow.getChildren().add(t);
                            textFlow.setPrefHeight(t.getLayoutBounds().getHeight() + 5);
                        }
                        cell.requestLayout();
                        cell.setPrefHeight(textFlow.prefHeightProperty().get());
                    }
                });
                
                return cell ;
            }
        });
        

        TreeTableColumn<Context, String> titleCol = new TreeTableColumn<>("Choose contexts for similar terms");
        titleCol.setSortable(false);
        titleCol.getColumns().addAll(selectedCol, idCol, contextCol, similarTermsCol);
        
        rootItem = new TreeItem<>(new Context("", "", ""));
        rootItem.setExpanded(true);
        TreeTableView<Context> table = new TreeTableView<>();
        table.setTableMenuButtonVisible(false);
        table.setRoot(rootItem);
        table.setShowRoot(false);
        table.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(titleCol);
        table.prefHeightProperty().bind(heightProperty().subtract(50));
        table.setEditable(true);
        
        return table;
    }
    
    public void selectContext(int contextID) {
        rootItem.getChildren().get(contextID).getValue().setSelected(false);
        rootItem.getChildren().get(contextID).getValue().setSelected(true);
    }
    
    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        rootItem.getChildren().clear();
        selectAllCheckBox.selectedProperty().set(false);
        notifyContextObservers(new ContextEvent(Collections.emptyList(), Collections.emptyList()));
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
     * Returns the list of selected Context IDs
     * @return
     */
    public List<Integer> getSelectedContexts() {
        List<Integer> l = Collections.emptyList();
        if(rootItem != null && rootItem.getChildren().size() > 0) {
            l = IntStream.range(0, rootItem.getChildren().size())
                .filter(i -> ((Context)rootItem.getChildren().get(i).getValue()).isSelected())
                .map(i -> Integer.parseInt(((Context)rootItem.getChildren().get(i).getValue()).getId()))
                .boxed()
                .collect(Collectors.toList());
        }
        
        return l;
    }
    
    /**
     * Returns the number of listed Contexts
     * @return
     */
    public int getNumContexts() {
        return rootItem.getChildren().size();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Region getOrCreateBackPanel() {
        if(backPanel == null) {
            backPanel = new ContextBackPanel(10).getScroll();
        }
        
        return backPanel;
    }
    
    /**
     * Notify all observers of {@link ContextEvent}s of a 
     * change in the state of contexts.
     * 
     * @param event     the {@link ContextEvent} to publish
     */
    public void notifyContextObservers(ContextEvent event) {
        contextObservers.stream().forEach(o -> o.onNext(event));
    }
    
    /**
     * Returns the Observable used to publish context changes to
     * subscribed context observers.
     * 
     * Used to keep the {@link SimilarTermsDisplay} in sync.
     * 
     * @return  the Observable used to publish context changes
     */
    public Observable<ContextEvent> getContextsObservable() {
        if(contextObservable == null) {
            contextObservable = Observable.create(s -> {
                contextObservers.add(s);
                s.onNext(createContextEvent());
            });
        }
        return contextObservable;
    }
    
    /**
     * Creates and returns a new {@link ContextEvent}
     * @return  a new {@code ContextEvent}
     */
    public ContextEvent createContextEvent() {
        List<Integer> selected = getSelectedContexts();
        List<Context> contexts = Collections.emptyList();
        if(rootItem != null && rootItem.getChildren().size() > 0) {
            contexts = rootItem.getChildren().stream().map(c -> c.getValue()).collect(Collectors.toList());
        }
        
        return new ContextEvent(contexts, selected);
    }
    
    /**
     * Storage medium for messages to other displays such
     * as the {@link SimilarTermsDisplay}
     * 
     * @see ContextDisplay#getContextsObservable()
     */
    public class ContextEvent {
        private List<Context> contexts;
        private List<Integer> selectedContexts;
        
        public ContextEvent(List<Context> c, List<Integer> sc) {
            this.contexts = c;
            this.selectedContexts = sc;
        }
        
        public List<Context> getContexts() {
            return contexts;
        }
        
        public List<Integer> getSelectedContexts() {
            return selectedContexts;
        }
    }
    
    public class Context {
        private SimpleBooleanProperty selected;
        private SimpleStringProperty id;
        private SimpleStringProperty label;
        private SimpleStringProperty similarTerms;
        private ChangeListener<Boolean> selectListener;
        private CheckBox checkBox;
        
        
        public Context(String id, String label, String simTerms) {
            this.selected = new SimpleBooleanProperty(false);
            this.id = new SimpleStringProperty(id);
            this.label = new SimpleStringProperty(label);
            this.similarTerms = new SimpleStringProperty(simTerms);
            
            selected.addListener(selectListener = (v,o,n) -> {
                // Flag change
                if(!dirtyFlagBlocked) {
                    dirtyProperty.set(true);
                    Platform.runLater(() -> {
                        List<Integer> currentSelection = getSelectedContexts();
                        if(!currentSelection.isEmpty() && !currentSelection.equals(lastSelection)) {
                            lastSelection = currentSelection;
                            autoSendProperty.set(); 
                            
                            notifyContextObservers(createContextEvent());
                        }
                    });
                }
                
                if(!n) {
                    selected.removeListener(selectListener);
                    selectAllCheckBox.setSelected(false);
                    selected.addListener(selectListener);
                }else{
                    if(!selectAllCheckBox.isSelected()) {
                        selected.removeListener(selectListener);
                        deselectAll();
                        selected.addListener(selectListener);
                    }
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
         * @return the id
         */
        public String getId() { return id.get(); }

        /**
         * @param id the id to set
         */
        public void setId(String id) { this.id.set(id); }

        /**
         * @return the label
         */
        public String getLabel() { return label.get(); }

        /**
         * @param label the term to set
         */
        public void setTerm(String label) { this.label.set(label); }

        /**
         * @return the similar terms
         */
        public String getSimilarTerms() { return similarTerms.get(); }

        /**
         * @param similarTerms the similar terms to set
         */
        public void setSimilarTerms(String fingerprint) { this.similarTerms.set(fingerprint); }
        
        /**
         * Returns the this Context's CheckBox
         * @return
         */
        public CheckBox getCheckBox() { return checkBox; }
        
        /**
         * Sets this Context's CheckBox
         * @param cb
         */
        public void setCheckBox(CheckBox cb) { this.checkBox = cb; }
        
        /**
         * De-selects all checkboxes.
         */
        private void deselectAll() {
            rootItem.getChildren().stream()
                .filter(treeItem -> !treeItem.getValue().equals(this))
                .forEach(treeItem -> {
                    ((Context)treeItem.getValue()).getCheckBox().setSelected(false);   
                });
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((label == null) ? 0 : label.hashCode());
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            Context other = (Context)obj;
            if(!getOuterType().equals(other.getOuterType()))
                return false;
            if(id == null) {
                if(other.id != null)
                    return false;
            } else if(!id.equals(other.id))
                return false;
            if(label == null) {
                if(other.label != null)
                    return false;
            } else if(!label.equals(other.label))
                return false;
            return true;
        }

        private ContextDisplay getOuterType() {
            return ContextDisplay.this;
        }
    }

    public static class CheckBoxTreeTableCell<S, T> extends TreeTableCell<S, T> {
        private final CheckBox checkBox;
        private ObservableValue<T> ov;

        public CheckBoxTreeTableCell() {
            this.checkBox = new CheckBox();
            this.checkBox.setAlignment(Pos.CENTER);
            setAlignment(Pos.CENTER);
            setGraphic(checkBox);
        }  

        @Override public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            
            Context context = (Context)tableRowProperty().get().getItem();
            if (empty || context == null || context.getLabel().isEmpty()) {
                setText(null);
                setGraphic(null);
            } else {
                context.setCheckBox(checkBox);
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
    
    /**
     * Populates this {@code ContextDisplay} with the {@link Context}s from the latest
     * server query.
     * @param contexts
     */
    public void setContexts(List<io.cortical.retina.model.Context> contexts) {
        // Used internally to suppress the dirty property from being set during reloads
        dirtyFlagBlocked = true;
        
        reset();
        
        Platform.runLater(() -> {
            
            rootItem = new TreeItem<>(new Context("", "", ""));
            rootItem.setExpanded(true);
            treeTable.setRoot(rootItem);
            treeTable.setShowRoot(false);
            
            Map<String, List<Term>> similarTermsMap = new HashMap<>(); 
            for(io.cortical.retina.model.Context context : contexts) {
                lookupSimilarTermsForContext(context, contexts, similarTermsMap, rootItem, contexts.size());
            }
        });
    }
    
    /**
     * Executes an asynchronous server call to retrieve similar terms for the context specified.
     * 
     * @param context
     * @param contexts
     * @param similarTermsMap
     * @param rootItem
     * @param querySize
     */
    private void lookupSimilarTermsForContext(io.cortical.retina.model.Context context, List<io.cortical.retina.model.Context> contexts, 
        Map<String, List<Term>> similarTermsMap, TreeItem<Context> rootItem, int querySize) {
        
        TermLookupAssistant.routeSimilarTermsRequest(
            new Term(context.getContextLabel()), 
            WindowService.getInstance().windowFor(this), 
            l -> {
                similarTermsMap.put(context.getContextLabel(), l);
                if(similarTermsMap.size() == querySize) {
                    completeTreeAssembly(contexts, similarTermsMap, rootItem);
                }
            });
    }
    
    /**
     * Completes the construction of the tree data model.
     * 
     * @param contexts
     * @param similarTermsMap
     * @param rootItem
     */
    private void completeTreeAssembly(List<io.cortical.retina.model.Context> contexts, Map<String, List<Term>> similarTermsMap, TreeItem<Context> rootItem) {
        for(io.cortical.retina.model.Context context : contexts) {
            TreeItem<Context> item = new TreeItem<>(new Context("" + context.getContextId(), context.getContextLabel(), "[...]"));
            
            List<Term> terms = similarTermsMap.get(context.getContextLabel());
            String termString = terms.stream()
                .map(t -> t.getTerm())
                .collect(Collectors.joining(", "));
            TreeItem<Context> child = new TreeItem<>(new Context("", "", termString));
            item.getChildren().add(child);
            rootItem.getChildren().add(item);
        }
        
        treeTable.getSelectionModel().clearSelection();
        tableView.requestLayout();
        
        // Always initialize with all Contexts selected.
        Platform.runLater(() -> {
            selectAllCheckBox.selectedProperty().set(true);
            dirtyFlagBlocked = false;
            // Notify context observers of context change
            notifyContextObservers(createContextEvent());
        });
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
