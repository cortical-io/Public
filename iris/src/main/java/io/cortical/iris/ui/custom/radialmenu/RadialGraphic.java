package io.cortical.iris.ui.custom.radialmenu;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Contains the label and symbol graphic used in {@link RadialMenuItem}s
 * 
 * @author cogmission
 */
public class RadialGraphic extends VBox implements Graphic {
    private String text;
    private Label textLabel;
    private Label symbolLabel;
    private Pane symbolPane;
    
    private boolean isSelected;
    private boolean isDisabled;
    
    public RadialGraphic(String text, String symbol) {
        this.text = text;
        
        textLabel = new Label(text);
        symbolLabel = new Label(symbol);
        textLabel.setOpacity(1.0);
        symbolLabel.setOpacity(1.0);
        
        symbolPane = new Pane();
        symbolPane.setMaxSize(20, 20);
        symbolPane.getChildren().add(symbolLabel);
        symbolPane.getStyleClass().add("radial-menu-graphic");
        symbolPane.layoutBoundsProperty().addListener((v,o,n) -> {
            symbolLabel.setLayoutX((n.getWidth() / 2) - (new Text(symbol).getLayoutBounds().getWidth() / 2));
        });
        
        setAlignment(Pos.CENTER);
        
        disabledProperty().addListener((v,o,n) -> {
            this.isDisabled = n;
            
            symbolPane.getStyleClass().removeAll("radial-menu-graphic-disabled", "radial-menu-graphic-selected", "radial-menu-graphic");
            symbolPane.getStyleClass().add(n ? "radial-menu-graphic-disabled" : "radial-menu-graphic");
            textLabel.getStyleClass().removeAll("radial-menu-text-disabled", "radial-menu-text-selected", "radial-menu-text");
            textLabel.getStyleClass().add(n ? "radial-menu-text-disabled" : "radial-menu-text");
            symbolLabel.getStyleClass().removeAll("radial-menu-symbol-disabled", "radial-menu-symbol-selected", "radial-menu-symbol");
            symbolLabel.getStyleClass().add(n ? "radial-menu-symbol-disabled" : "radial-menu-symbol");
        });
        
        setFocusTraversable(false);
        
        getChildren().addAll(textLabel, symbolPane);
    }
    
    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setFont(Font f) {
        textLabel.setFont(f);
        symbolLabel.setFont(f);
    }

    @Override
    public void setTextFill(Paint p) {
        textLabel.setTextFill(p);
        symbolLabel.setTextFill(p);
    }
    
    @Override
    public void setSelected(boolean b) {
        textLabel.getStyleClass().removeAll("radial-menu-text-selected", "radial-menu-text");
        if(isDisabled) return;
        
        textLabel.getStyleClass().add(b ? "radial-menu-text-selected" : "radial-menu-text");
        symbolLabel.getStyleClass().removeAll("radial-menu-symbol-disabled", "radial-menu-symbol-selected", "radial-menu-symbol");
        symbolLabel.getStyleClass().add(b ? "radial-menu-symbol-selected" : "radial-menu-symbol");
        if(!isDisabled()) {
            symbolPane.getStyleClass().removeAll("radial-menu-graphic-selected", "radial-menu-graphic-disabled", "radial-menu-graphic");
            symbolPane.getStyleClass().add(b ? "radial-menu-graphic-selected" : "radial-menu-graphic");
        }
    }

    @Override
    public boolean isSelected() {
        return isSelected;
    }
}
