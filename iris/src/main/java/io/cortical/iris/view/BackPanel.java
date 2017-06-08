package io.cortical.iris.view;

import io.cortical.util.ImmutableTextFlow;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

public class BackPanel extends VBox {
    
    private ScrollPane scroll;
    
    /**
     * Contructs a new {@code BackPanel}
     * @param spacing   vertical spacing between child nodes
     */
    public BackPanel(double spacing) {
        super(spacing);
        
        setPadding(new Insets(0,0,20,0));
        
        //--- Example code...
//        Text title = new Text("TextDisplay Instructions");
//        title.setFont(Font.font(title.getFont().getFamily(), FontWeight.BOLD, 18));
//        
//        Text instr1 = new Text("\u2022 \tTo be completed later...");
//        instr1.setFont(Font.font(title.getFont().getFamily(), FontWeight.NORMAL, 12));
//        
//        getChildren().addAll(title, instr1);
    }
    
    /**
     * Returns the containing {@link ScrollPane} that is automatically
     * configured.
     * @return
     */
    public ScrollPane getScroll() {
        if(scroll == null) {
            scroll = new ScrollPane(this);
            scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
            scroll.setPadding(new Insets(5,5,5,5));
            
            scroll.viewportBoundsProperty().addListener((v,o,n) -> {
                getChildren().stream()
                .filter(t -> (t instanceof TextFlow) && !(t instanceof ImmutableTextFlow))
                .map(obj -> (TextFlow)obj)
                .forEach(t -> {
                    t.setMaxWidth(n.getWidth() - 5);
                });    
                setMaxWidth(n.getWidth() - 10);
                requestLayout();
            });
        }
        
        return scroll;
    }
}
