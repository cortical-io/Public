package io.cortical.iris.view.input.expression;

/**
 * Holds the types of allowable punctuation or {@link Operator} that
 * can be used in the UI.
 * 
 * @author cogmission
 */
public enum Operator {
    R_PRN(")"), AND("&"), L_PRN("("), OR("|"), NOT("!"), XOR("^");
    
    private String symbol;
    
    private Operator(String symbol) {
        this.symbol = symbol;
    }
    
    public String toDisplay() {
        return symbol;
    }
    
    public static Operator typeFor(String s) {
        for(Operator o : values()) {
            if(s.equalsIgnoreCase(o.symbol) || s.equalsIgnoreCase(o.toString())) {
                return o;
            }else if(s.equals(")")) {
                return R_PRN;
            }else if(s.equals("(")) {
                return L_PRN;
            }
        }
        return null;
    }
}
