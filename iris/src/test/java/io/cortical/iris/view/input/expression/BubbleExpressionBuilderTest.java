package io.cortical.iris.view.input.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.iris.ui.custom.widget.bubble.Bubble.Type;
import io.cortical.retina.model.Model;
import javafx.util.Pair;


public class BubbleExpressionBuilderTest {
    
    @Test
    public void testGetExpression() {
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble term1 = makeWordBubble("Fox");
        bubbles.add(term1);
        
        String expected = "{" +
            "\"term\":\"Fox\"," +
            "\"df\":0.0," +
            "\"score\":0.0" +
          "}";
        Model model = parser.parseExpression(bubbles);
        try {
            System.out.println(model.toJson());
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetExpressionSingleOp() {
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);
        
        String expected = "{" +
          "\"and\":[{" +
            "\"term\":\"Fox\"," +
            "\"df\":0.0," +
            "\"score\":0.0" +
          "},{" +
            "\"term\":\"Rabbit\"," +
            "\"df\":0.0," +
            "\"score\":0.0" +
          "}]" +
        "}";
        Model model = parser.parseExpression(bubbles);
        try {
            //System.out.println(model.toJson());
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testGetExpressionWithSingleParens() {
        // (e1 && e2)
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);                         
        Bubble paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(0, paren);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);             
        
        String expected = "{" +
            "\"and\":[{" +
              "\"term\":\"Fox\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "},{" +
              "\"term\":\"Rabbit\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "}]" +
          "}";
        Model model = parser.parseExpression(bubbles);                           
        try {                                                                              
            //System.out.println(model.toJson());                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));          
        }catch(Exception e) {                                                              
            e.printStackTrace();                                                           
        }
    }
    
    @Test
    public void testGetExpressionWithDoubleParens() {
        // ((e1 && e2))
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);                         
        Bubble paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(0, paren);
        bubbles.add(0, paren);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);
        bubbles.add(paren);
        
        String expected = "{" +
            "\"and\":[{" +
              "\"term\":\"Fox\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "},{" +
              "\"term\":\"Rabbit\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "}]" +
          "}";
        Model model = parser.parseExpression(bubbles);                           
        try {                                                                              
            //System.out.println(model.toJson());                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));          
        }catch(Exception e) {                                                              
            e.printStackTrace();                                                           
        }
    }
    
    @Test
    public void testGetExpressionProgressiveNesting() {
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);                         
        Bubble paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(0, paren);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);            
        Bubble op3 = makeOpBubble(Operator.AND);                                                                         
        Bubble term4 = makeWordBubble("bug");                                                                            
        bubbles.add(op3);                                                                            
        bubbles.add(term4);
        Bubble op4 = makeOpBubble(Operator.AND);
        Bubble term5 = makeWordBubble("beatle");
        bubbles.add(op4);                                                                            
        bubbles.add(term5);
        
        // Next add end expression e1 && e2 && e3 && e4, where e1 plus e2 plus e3 resolve to single expression           
        // which is then "anded" to e4.
        String expected = "{" +
          "\"and\":[{" +
            "\"and\":[{" +
              "\"and\":[{" +
                "\"term\":\"Fox\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "},{" +
                "\"term\":\"Rabbit\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "}]" +
            "},{" +
              "\"term\":\"bug\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "}]" +
          "},{" +
            "\"term\":\"beatle\"," +
            "\"df\":0.0," +
            "\"score\":0.0" +
          "}]" +
        "}";
        
        Model model = parser.parseExpression(bubbles);                                                         
        try {                                                                                                            
            //System.out.println(model.toJson());                                                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));                                  
        }catch(Exception e) {                                                                                            
            e.printStackTrace();                                                                                         
        } 
    }
    
    public void testGetExpression2ParenGroups() {
        // Next add two parens to make (e1 && e2) && (e3 && e4), where e1 plus e2 are "anded" with e3 plus e4.           
        // The result should nest differently.
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);                         
        Bubble paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(0, paren);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);   
        paren = makeParenBubble(Bubble.Type.RPAREN);                                                                     
        bubbles.add(paren);                                                                                           
        paren = makeParenBubble(Bubble.Type.LPAREN);                                                                     
        bubbles.add(6, paren);       
        
        String expected = "{" +
          "\"and\":[{" +
            "\"and\":[{" + 
              "\"term\":\"Fox\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "},{" +
              "\"term\":\"Rabbit\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "}]" +
          "},{" +
            "\"and\":[{" +
              "\"term\":\"bug\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "},{" + 
              "\"term\":\"beatle\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "}]" +
          "}]" +
        "}";
                                                                                            
        Model model = parser.parseExpression(bubbles);                                                         
        try {                                                                                                            
            //System.out.println(model.toJson());                                                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));                                  
        }catch(Exception e) {                                                                                            
            e.printStackTrace();                                                                                         
        }                                                                                                                
    }
    
    @Test
    public void testGetExpressionSingleThenGroup() {
        //(e5 && (e6 && e7))
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble paren = makeParenBubble(Type.LPAREN);
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        bubbles.add(paren);
        bubbles.add(term1);
        bubbles.add(op);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(paren);
        bubbles.add(term2);
        Bubble op2 = makeOpBubble(Operator.AND);
        Bubble term3 = makeWordBubble("Rodent");
        bubbles.add(op2);
        bubbles.add(term3);
        paren = makeParenBubble(Type.RPAREN);
        bubbles.add(paren);
        bubbles.add(paren);
        
        String expected = "{" +
          "\"and\":[{" +
            "\"term\":\"Fox\"," +
            "\"df\":0.0," +
            "\"score\":0.0" +
          "},{" +
            "\"and\":[{" +
              "\"term\":\"Rabbit\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "},{" +
              "\"term\":\"Rodent\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "}]" +
          "}]" +
        "}";
        
        Model model = parser.parseExpression(bubbles);                                                         
        try {                                                                                                            
            System.out.println(model.toJson());                                                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));                                  
        }catch(Exception e) {                                                                                            
            e.printStackTrace();                                                                                         
        } 
    }
    
    @Test
    public void testGetExpression2ParenGroupsWithExtraneousParens() {
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        //((e1 && e2) && (e3 && e4))
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(paren);
        Bubble term3 = makeWordBubble("Rodent");
        bubbles.add(term3);
        bubbles.add(op);
        Bubble term4 = makeWordBubble("Squirrel");
        bubbles.add(term4);
        paren = makeParenBubble(Type.RPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        
        String expected = "{" +
            "\"and\":[{" +
              "\"and\":[{" + 
                "\"term\":\"Fox\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "},{" +
                "\"term\":\"Rabbit\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "}]" +
            "},{" +
              "\"and\":[{" +
                "\"term\":\"Rodent\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "},{" + 
                "\"term\":\"Squirrel\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "}]" +
            "}]" +
          "}";

        Model model = parser.parseExpression(bubbles);                                                         
        try {                                                                                                            
            System.out.println(model.toJson());                                                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));                                  
        }catch(Exception e) {                                                                                            
            e.printStackTrace();                                                                                         
        } 
    }
    
    @Test
    public void testGetExpression2ParenGroupsWithMoreExtraneousParens() {
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        //(((e1 && e2) && (e3 && e4)))
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(paren);
        Bubble term3 = makeWordBubble("Rodent");
        bubbles.add(term3);
        bubbles.add(op);
        Bubble term4 = makeWordBubble("Squirrel");
        bubbles.add(term4);
        paren = makeParenBubble(Type.RPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        
        String expected = "{" +
            "\"and\":[{" +
              "\"and\":[{" + 
                "\"term\":\"Fox\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "},{" +
                "\"term\":\"Rabbit\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "}]" +
            "},{" +
              "\"and\":[{" +
                "\"term\":\"Rodent\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "},{" + 
                "\"term\":\"Squirrel\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "}]" +
            "}]" +
          "}";

        Model model = parser.parseExpression(bubbles);                                                         
        try {                                                                                                            
            System.out.println(model.toJson());                                                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));                                  
        }catch(Exception e) {                                                                                            
            e.printStackTrace();                                                                                         
        } 
    }
    
    @Test
    public void testGetExpressionComplexGrouping() {
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        //((e1 && e2) && (e3 && e4)) && (e5 && (e6 && e7))     
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(paren);
        Bubble term3 = makeWordBubble("Rodent");
        bubbles.add(term3);
        bubbles.add(op);
        Bubble term4 = makeWordBubble("Squirrel");
        bubbles.add(term4);
        paren = makeParenBubble(Type.RPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);
        Bubble term5 = makeWordBubble("volkswagen");
        bubbles.add(paren);
        bubbles.add(term5);
        bubbles.add(op);
        bubbles.add(paren);
        Bubble term6 = makeWordBubble("bug");
        bubbles.add(term6);
        bubbles.add(op);
        Bubble term7 = makeWordBubble("plane");
        bubbles.add(term7);
        paren = makeParenBubble(Type.RPAREN);
        bubbles.add(paren);
        bubbles.add(paren);
        
        String expected = "{" +
              "\"and\":[{" +
                "\"and\":[{" +
                  "\"and\":[{" +
                    "\"term\":\"Fox\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "},{" +
                    "\"term\":\"Rabbit\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "}]" +
                "},{" +
                  "\"and\":[{" +
                    "\"term\":\"Rodent\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "},{" +
                    "\"term\":\"Squirrel\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "}]" +
                "}]" +
              "},{" + 
                "\"and\":[{" +
                  "\"term\":\"volkswagen\"," +
                  "\"df\":0.0," +
                  "\"score\":0.0" +
                "},{" +
                  "\"and\":[{" +
                    "\"term\":\"bug\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "},{" +
                    "\"term\":\"plane\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "}]" +
                "}]" +
              "}]" +
            "}";
        
        Model model = parser.parseExpression(bubbles);                                                         
        try {                                                                                                            
            System.out.println(model.toJson());                                                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));                                  
        }catch(Exception e) {                                                                                            
            e.printStackTrace();                                                                                         
        } 
    }
    
    @Test
    public void testGetExpressionComplexGrouping_ExtraneousParens() {
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        //(((e1 && e2) && (e3 && e4)) && (e5 && (e6 && e7)))     
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble paren = makeParenBubble(Type.LPAREN);     
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(paren);
        Bubble term3 = makeWordBubble("Rodent");
        bubbles.add(term3);
        bubbles.add(op);
        Bubble term4 = makeWordBubble("Squirrel");
        bubbles.add(term4);
        paren = makeParenBubble(Type.RPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);
        Bubble term5 = makeWordBubble("volkswagen");
        bubbles.add(paren);
        bubbles.add(term5);
        bubbles.add(op);
        bubbles.add(paren);
        Bubble term6 = makeWordBubble("bug");
        bubbles.add(term6);
        bubbles.add(op);
        Bubble term7 = makeWordBubble("plane");
        bubbles.add(term7);
        paren = makeParenBubble(Type.RPAREN);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        
        String expected = "{" +
              "\"and\":[{" +
                "\"and\":[{" +
                  "\"and\":[{" +
                    "\"term\":\"Fox\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "},{" +
                    "\"term\":\"Rabbit\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "}]" +
                "},{" +
                  "\"and\":[{" +
                    "\"term\":\"Rodent\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "},{" +
                    "\"term\":\"Squirrel\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "}]" +
                "}]" +
              "},{" + 
                "\"and\":[{" +
                  "\"term\":\"volkswagen\"," +
                  "\"df\":0.0," +
                  "\"score\":0.0" +
                "},{" +
                  "\"and\":[{" +
                    "\"term\":\"bug\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "},{" +
                    "\"term\":\"plane\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "}]" +
                "}]" +
              "}]" +
            "}";
        
        Model model = parser.parseExpression(bubbles);                                                         
        try {                                                                                                            
            System.out.println(model.toJson());                                                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));                                  
        }catch(Exception e) {                                                                                            
            e.printStackTrace();                                                                                         
        } 
    }
    
    @Test
    public void testGetExpressionComplexGrouping_DeeplyExtraneousParens() {
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        //((((e1 && e2)) && ((e3 && e4))) && ((e5 && ((e6 && e7)))))     
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble paren = makeParenBubble(Type.LPAREN); 
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term3 = makeWordBubble("Rodent");
        bubbles.add(term3);
        bubbles.add(op);
        Bubble term4 = makeWordBubble("Squirrel");
        bubbles.add(term4);
        paren = makeParenBubble(Type.RPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);
        Bubble term5 = makeWordBubble("volkswagen");
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(term5);
        bubbles.add(op);
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term6 = makeWordBubble("bug");
        bubbles.add(term6);
        bubbles.add(op);
        Bubble term7 = makeWordBubble("plane");
        bubbles.add(term7);
        paren = makeParenBubble(Type.RPAREN);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        
        String expected = "{" +
              "\"and\":[{" +
                "\"and\":[{" +
                  "\"and\":[{" +
                    "\"term\":\"Fox\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "},{" +
                    "\"term\":\"Rabbit\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "}]" +
                "},{" +
                  "\"and\":[{" +
                    "\"term\":\"Rodent\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "},{" +
                    "\"term\":\"Squirrel\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "}]" +
                "}]" +
              "},{" + 
                "\"and\":[{" +
                  "\"term\":\"volkswagen\"," +
                  "\"df\":0.0," +
                  "\"score\":0.0" +
                "},{" +
                  "\"and\":[{" +
                    "\"term\":\"bug\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "},{" +
                    "\"term\":\"plane\"," +
                    "\"df\":0.0," +
                    "\"score\":0.0" +
                  "}]" +
                "}]" +
              "}]" +
            "}";
        
        Model model = parser.parseExpression(bubbles);                                                         
        try {                                                                                                            
            System.out.println(model.toJson());                                                                          
            assertEquals(expected, model.toJson().replaceAll("[\\s\\t\\r\\n]*", ""));                                  
        }catch(Exception e) {                                                                                            
            e.printStackTrace();                                                                                         
        } 
    }
    
    @Test
    public void testCheckParens() {
        BubbleExpressionBuilder parser = new BubbleExpressionBuilder();
        
        //((((e1 && e2)) && ((e3 && e4))) && ((e5 && ((e6 && e7)))))     
        FilteredBubbleList bubbles = new FilteredBubbleList();
        Bubble paren = makeParenBubble(Type.LPAREN); 
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term1 = makeWordBubble("Fox");
        Bubble op = makeOpBubble(Operator.AND);
        Bubble term2 = makeWordBubble("Rabbit");
        bubbles.add(term1);
        bubbles.add(op);
        bubbles.add(term2);
        paren = makeParenBubble(Type.RPAREN);                                              
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);                                       
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term3 = makeWordBubble("Rodent");
        bubbles.add(term3);
        bubbles.add(op);
        Bubble term4 = makeWordBubble("Squirrel");
        bubbles.add(term4);
        paren = makeParenBubble(Type.RPAREN);                                       
        bubbles.add(paren);
//        bubbles.add(paren);                             <----  Remove arbitrary parenthesis to fail test
        bubbles.add(paren);
        bubbles.add(op);
        paren = makeParenBubble(Type.LPAREN);
        Bubble term5 = makeWordBubble("volkswagen");
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(term5);
        bubbles.add(op);
        bubbles.add(paren);
        bubbles.add(paren);
        Bubble term6 = makeWordBubble("bug");
        bubbles.add(term6);
        bubbles.add(op);
        Bubble term7 = makeWordBubble("plane");
        bubbles.add(term7);
        paren = makeParenBubble(Type.RPAREN);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        bubbles.add(paren);
        
        try {    
            parser.parseExpression(bubbles);                                                         
            fail();                             
        }catch(Exception e) {                                                                                            
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("Unbalanced parenthesis in bubble list.", e.getMessage());
        } 
    }
    
    @Test
    public void testDoParseOfModels() {
        //(e5 && (e6 && e7))
        
        String json = "{" +
            "\"and\":[{" +
              "\"term\":\"Fox\"," +
              "\"df\":0.0," +
              "\"score\":0.0" +
            "},{" +
              "\"and\":[{" +
                "\"term\":\"Rabbit\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "},{" +
                "\"term\":\"Rodent\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "}]" +
            "}]" +
          "}";
        
        try {
            Model m = ExpressionModelDeserializer.narrow(json);
            BubbleExpressionBuilder builder = new BubbleExpressionBuilder();
            Pair<List<Bubble.Type>, List<String>> result = builder.doParse(m, null, 0);
            
            String expectedResultString = "[WORD, OPERATOR, LPAREN, WORD, OPERATOR, WORD, RPAREN]=[Fox, &, (, Rabbit, &, Rodent, )]";
            assertEquals(expectedResultString, result.toString());
       }catch(Exception e) { 
            e.printStackTrace();
        }
        
        
        //((((e1 && e2)) && ((e3 && e4))) && ((e5 && ((e6 && e7)))))
        json = "{" +
            "\"and\":[{" +
              "\"and\":[{" +
                "\"and\":[{" +
                  "\"term\":\"Fox\"," +
                  "\"df\":0.0," +
                  "\"score\":0.0" +
                "},{" +
                  "\"term\":\"Rabbit\"," +
                  "\"df\":0.0," +
                  "\"score\":0.0" +
                "}]" +
              "},{" +
                "\"and\":[{" +
                  "\"term\":\"Rodent\"," +
                  "\"df\":0.0," +
                  "\"score\":0.0" +
                "},{" +
                  "\"term\":\"Squirrel\"," +
                  "\"df\":0.0," +
                  "\"score\":0.0" +
                "}]" +
              "}]" +
            "},{" + 
              "\"and\":[{" +
                "\"term\":\"volkswagen\"," +
                "\"df\":0.0," +
                "\"score\":0.0" +
              "},{" +
                "\"and\":[{" +
                  "\"term\":\"bug\"," +
                  "\"df\":0.0," +
                  "\"score\":0.0" +
                "},{" +
                  "\"term\":\"plane\"," +
                  "\"df\":0.0," +
                  "\"score\":0.0" +
                "}]" +
              "}]" +
            "}]" +
          "}";
        
        try {
            Model m = ExpressionModelDeserializer.narrow(json);
            BubbleExpressionBuilder builder = new BubbleExpressionBuilder();
            Pair<List<Bubble.Type>, List<String>> result = builder.doParse(m, null, 0);
            
            String expectedTypeSequence = "[WORD, OPERATOR, WORD, OPERATOR, LPAREN, WORD, OPERATOR, WORD, RPAREN, " +
                "OPERATOR, LPAREN, WORD, OPERATOR, LPAREN, WORD, OPERATOR, WORD, RPAREN, RPAREN]";
            String expectedBubbleSequence = "[Fox, &, Rabbit, &, (, Rodent, &, Squirrel, ), &, (, volkswagen, &, (, bug, &, plane, ), )]";
            String result1 = result.toString().split("\\=")[0];
            String result2 = result.toString().split("\\=")[1];
            assertEquals(expectedTypeSequence, result1);
            assertEquals(expectedBubbleSequence, result2);
        }catch(Exception e) { 
            e.printStackTrace();
        }
    }
    
    @Test
    public void testBuildBubbleLists() {
        BubbleExpressionBuilder builder = new BubbleExpressionBuilder();
        
        String expression = "(Fox trot) | Rabbit & ((Rodent & Squirrel)) & (volkswagen & (bug & plane))";
        try {
            builder.validateExpression(expression);
        }catch(Exception e) {
            fail();
        }
        
        // Test all operators
        expression = "(Fox trot) | Rabbit ^ ((Rodent & Squirrel)) ! (volkswagen & (bug | plane))";
        try {
            builder.validateExpression(expression);
        }catch(Exception e) {
            fail();
        }
        
        // Test no expression
        expression = "Fox";
        try {
            builder.validateExpression(expression);
        }catch(Exception e) {
            fail();
        }
        
        // Test un matched parenthesis at beginning
        expression = "((Fox trot) | Rabbit ^ ((Rodent & Squirrel)) ! (volkswagen & (bug | plane))";
        String errorMessage = "";
        try {
            builder.validateExpression(expression);
            fail();
        }catch(Exception e) {
            errorMessage = e.getMessage();
        }
        assertEquals("Unmatched parenthesis \"(\" at index: 0", errorMessage);
        
        // Test un matched parenthesis in middle                 v
        expression = "(Fox trot) | Rabbit ^ ((Rodent & Squirrel))) ! (volkswagen & (bug | plane))";
        errorMessage = "";
        try {
            builder.validateExpression(expression);
            fail();
        }catch(Exception e) {
            errorMessage = e.getMessage();
        }
        assertEquals("Unmatched parenthesis \")\" at index: 43", errorMessage);
        
        // Test un matched parenthesis at end                                                   v     
        expression = "(Fox trot) | Rabbit ^ ((Rodent & Squirrel)) ! (volkswagen & (bug | plane)))";
        errorMessage = "";
        try {
            builder.validateExpression(expression);
            fail();
        }catch(Exception e) {
            errorMessage = e.getMessage();
        }
        assertEquals("Unmatched parenthesis \")\" at index: " + (expression.length() - 1), errorMessage);
        
        // Test for text (not an expression)
        expression = "Fox trot rabbit";
        errorMessage = "";
        try {
            builder.validateExpression(expression);
        }catch(Exception e) {
            errorMessage = e.getMessage();
        }
        assertEquals("The text: --> \"Fox trot rabbit\" <-- appears to be text and not an expression.", errorMessage);
        
        // Check for whacky parenthesis     v
        expression = "(Fox trot) | Rabbit ^ )((Rodent & Squirrel)) ! (volkswagen & (bug | plane))";
        errorMessage = "";
        try {
            builder.validateExpression(expression);
        }catch(Exception e) {
            errorMessage = e.getMessage();
        }
        assertEquals("Unmatched parenthesis \")\" at index: 22", errorMessage);
        
        // Check for two operators         
        expression = "(Fox trot) | Rabbit ^^ ((Rodent & Squirrel)) ! (volkswagen & (bug | plane))";
        errorMessage = "";
        try {
            builder.validateExpression(expression);
        }catch(Exception e) {
            errorMessage = e.getMessage();
        }
        assertEquals("Bad character: \"^\" near index: 21", errorMessage);
        
        // Check for operator in wrong place        
        expression = "(Fox trot) | Rabbit ^ ((& Rodent & Squirrel)) ! (volkswagen & (bug | plane))";
        errorMessage = "";
        try {
            builder.validateExpression(expression);
        }catch(Exception e) {
            errorMessage = e.getMessage();
        }
        assertEquals("Bad character: \"(\" near index: 22", errorMessage);
    }
    
    @Test
    public void testDoParseFreehand() {
        BubbleExpressionBuilder builder = new BubbleExpressionBuilder();
        
        //String expression = "(ice cream) | Rabbit & ((Rodent & Squirrel)) & (volkswagen & (bug & plane))";
        String expression = "(ice cream) | Rabbit | ((Rodent & Squirrel)) | (volkswagen | (bug | plane))";
        try {
            Pair<List<Bubble.Type>, List<String>> pair = builder.doParse(expression);
            System.out.println(pair.getKey() + "    -    " + pair.getValue());
        }catch(Exception e) {
            fail();
        }
        
        expression = "(Fox trot) | Rabbit ^ ((& Rodent & Squirrel)) ! (volkswagen & (bug | plane))";
        String errorMessage = "";
        try {
            builder.validateExpression(expression);
        }catch(Exception e) {
            errorMessage = e.getMessage();
        }
        assertEquals("Bad character: \"(\" near index: 22", errorMessage);
    }
    
    private Bubble makeWordBubble(String text) {
        return new Bubble() {

            @Override
            public Type getType() {
                return Bubble.Type.WORD;
            }

            @Override
            public String getText() {
                return text;
            }
        };
    }
    
    private Bubble makeOpBubble(Operator op) {
        return new Bubble() {

            @Override
            public Type getType() {
                return Bubble.Type.OPERATOR;
            }

            @Override
            public String getText() {
                return null;
            }
            
            @Override
            public Operator getOperator() {
                return op;
            }
        };
    }
    
    private Bubble makeParenBubble(Bubble.Type type) {
        return new Bubble() {

            @Override
            public Type getType() {
                return type;
            }

            @Override
            public String getText() {
                return null;
            }
            
         };
    }
}
