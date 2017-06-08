package io.cortical.iris.view.input.expression;

import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;


public class ParenthesisBubble extends OperatorBubble implements Bubble {
    private Operator symbol = Operator.L_PRN;
    private Shape shape;
    
    public ParenthesisBubble(Operator p) {
        super(p);
        
        setText(p == Operator.L_PRN ? p.toDisplay() + " " : " " + p.toDisplay());
        
        if(p != Operator.L_PRN && p != Operator.R_PRN) {
            throw new IllegalArgumentException("Operator type must be L_PAREN or R_PAREN");
        }
        
        getStyleClass().setAll("parenthesis");
        
        this.symbol = p;
        
        shape = getBubbleShape();
        setClip(shape);
        setShape(shape);
        
        setPrefWidth(30);
        setPrefHeight(30);
        setMaxHeight(30);
        
        setFocusTraversable(false);
        
        layoutBoundsProperty().addListener((v, o, n) -> {
            shape = getBubbleShape();
            setClip(shape);
            setShape(shape);
        });
        
    }
    
    public Shape getBubbleShape() {
        Polygon p = new Polygon();
        double x = getWidth() / 2;
        double y = 0;
        double lry = getHeight() / 2;
        p.getPoints().addAll(new Double[]{
            x, y,
            0.0, lry,
            x, getHeight(),
            getWidth(), lry,
            x, y});
        return p;
    }

    @Override
    public Type getType() {
        return symbol == Operator.L_PRN ? Bubble.Type.LPAREN : Bubble.Type.RPAREN;
    }
    
    @Override
    public String toString() {
        return symbol.toDisplay();
    }
    
    
}
