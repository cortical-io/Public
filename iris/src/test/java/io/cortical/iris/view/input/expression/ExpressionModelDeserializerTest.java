package io.cortical.iris.view.input.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import io.cortical.iris.view.input.expression.ExpressionModelDeserializer.IndexType;
import io.cortical.retina.model.ExpressionFactory.ExpressionModel;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Model;
import io.cortical.retina.model.Term;
import io.cortical.retina.model.Text;

public class ExpressionModelDeserializerTest {
    
    private String jsonString =
      "{"+
        "\"term\" : \"porsche\"," +
        "\"df\" : 3.135354965114297E-4," +
        "\"score\" : 167.0," +
        "\"pos_types\" : [ \"NOUN\" ]" +
      "}";

    private String expressionString = 
            "{\n"+
                "\t\"sub\": [\n" +
                   "\t\t{\n"+
                        "\t\t\t\"term\": \"jaguar\"\n"+
                   "\t\t},\n"+
                   "\t\t{\n" +
                        "\t\t\t\"positions\" : [ 2, 3, 4, 5, 6 ]\n"+
                   "\t\t}\n"+
                "\t]\n"+
            "}";
    
    private String textString =
            "{" +
                "\"text\" : \"Seeing that you don't have a choice to saying extends String here, I'd say this is a bug in Eclipse.\"" +
            "}";
    
    private String fingerprintString = 
              "{\n" + 
                "\"fingerprint\": {" +
                    "\"positions\": [" +
                      "319,\n" +
                      "1788,\n" +
                      "2383,\n" +
                      "2512,\n" +
                      "2513,\n" +
                      "2639,\n" +
                      "2682,\n" +
                      "2767\n" +
                   "]\n" +
                 "}\n" +
               "}\n";
    
    private String positionsString = 
            "\"positions\": [" +
                      "319,\n" +
                      "1788,\n" +
                      "2383,\n" +
                      "2512,\n" +
                      "2513,\n" +
                      "2639,\n" +
                      "2682,\n" +
                      "2767\n" +
                   "]\n";
    
    private String complexExpressionString = 
              "{\n" +
                "\t\"xor\" : [ {\n" +
                  "\t\t\"or\" : [ {\n" +
                    "\t\t\t\"term\" : \"car\"\n" +
                  "\t\t}, {\n" +
                    "\t\t\t\"term\" : \"truck\"\n" +
                  "\t\t} ]\n" +
                "\t}, {\n" +
                  "\t\t\"sub\" : [ {\n" +
                    "\t\t\t\"term\" : \"vehicle\"\n" +
                  "\t\t}, {\n" +
                    "\t\t\t\"term\" : \"train\"\n" +
                  "\t\t} ]\n" +
                "\t} ]\n" +
              "}\n";
    
    private String nestedExpressionString = 
        "{" +
                "\"sub\" : [" +
                        "{" +
                                "\"and\" : [" +
                                        "{" +
                                                "\"xor\" : [" +
                                                        "{" +
                                                                "\"term\" : \"car\"" +
                                                        "}," +
                                                        "{" +
                                                                "\"term\" : \"truck\"" +
                                                        "}" +
                                                "]" +
                                        "}," +
                                        "{" +
                                                "\"term\" : \"vehicle\"" +
                                        "}" +
                                "]" +
                        "}," +
                        "{" +
                                "\"term\" : \"bus\"" +
                        "}" +
                "]" +
        "}";
                
    
    
    @Test
    public void testExtractPositionsArray() {
        String simple = "7, 8, 9";
        int[] expected = { 7, 8, 9 };
        assertTrue(Arrays.equals(expected, ExpressionModelDeserializer.extractPositionsArray(simple)));
        
        simple = ", 7, 8, 9";
        assertTrue(Arrays.equals(expected, ExpressionModelDeserializer.extractPositionsArray(simple)));
        
        simple = ",, 7, 8, 9,";
        assertTrue(Arrays.equals(expected, ExpressionModelDeserializer.extractPositionsArray(simple)));
        
        // Negative Test
        simple = "7, 0, 9";
        int[] notExpected = expected;
        expected = new int[] { 7, 0, 9 };
        assertFalse(Arrays.equals(notExpected, ExpressionModelDeserializer.extractPositionsArray(simple)));
        assertTrue(Arrays.equals(expected, ExpressionModelDeserializer.extractPositionsArray(simple)));
        
        simple = "7";
        expected = new int[] { 7 };
        assertTrue(Arrays.equals(expected, ExpressionModelDeserializer.extractPositionsArray(simple)));
        
        simple = "\"positions\":[6,7,319,406,805,1788,2230]";
        System.out.println("line = " + Arrays.toString(ExpressionModelDeserializer.extractPositionsArray(simple)));
    }
    
    @Test
    public void testParseExpression() {
        System.out.println("Complex: \n" + complexExpressionString);
    }
    
    @Test
    public void testIsJsonString() {
        try {
            assertTrue(ExpressionModelDeserializer.isJsonString(jsonString));
        }catch(Exception e) {
            fail();
        }
        
        try {
            String failString = 
                    "\"pos_types\" : [ \"NOUN\" ]" +
                            "}";
            assertTrue(ExpressionModelDeserializer.isJsonString(failString));
            fail();
        }catch(Exception ignore) {}
    }
    
    @Test
    public void testGetLowestIndex() {
        ExpressionModelDeserializer.IndexType found = null;
        found = ExpressionModelDeserializer.getLowestIndex(jsonString);
        assertEquals(IndexType.TERM, found);
        
        found = ExpressionModelDeserializer.getLowestIndex(textString);
        assertEquals(IndexType.TEXT, found);
        
        found = ExpressionModelDeserializer.getLowestIndex(expressionString);
        assertEquals(IndexType.SUB, found);
        
        found = ExpressionModelDeserializer.getLowestIndex(fingerprintString);
        assertEquals(IndexType.FP, found);
        
        found = ExpressionModelDeserializer.getLowestIndex(positionsString);
        assertEquals(IndexType.POS, found);
    }
    
    @Test
    public void testNarrow() {
        Model model = null;
        
        // Term
        try {
            model = ExpressionModelDeserializer.narrow(jsonString);
            assertEquals(Term.class, model.getClass());
        }catch(Exception e) {
            fail();
        }
        
        // Text
        try {
            model = ExpressionModelDeserializer.narrow(textString);
            assertEquals(Text.class, model.getClass());
        }catch(Exception e) {
            fail();
        }
        
        // Expression
        try {
            model = ExpressionModelDeserializer.narrow(expressionString);
            assertTrue(ExpressionModel.class.isAssignableFrom(model.getClass()));
            System.out.println(model.toJson());
        }catch(Exception e) {
            fail();
        }
        
        // Complex Expression
        try {
            model = ExpressionModelDeserializer.narrow(complexExpressionString);
            assertTrue(ExpressionModel.class.isAssignableFrom(model.getClass()));
            System.out.println(model.toJson());
        }catch(Exception e) {
            fail();
        }
        
        // Fingerprint
        try {
            model = ExpressionModelDeserializer.narrow(fingerprintString);
            assertEquals(Fingerprint.class, model.getClass());
        }catch(Exception e) {
            fail();
        }
        
        // Positions
        try {
            model = ExpressionModelDeserializer.narrow(positionsString);
            assertEquals(Fingerprint.class, model.getClass());
        }catch(Exception e) {
            fail();
        }
        
        // Nested complex
        try {
            model = ExpressionModelDeserializer.narrow(nestedExpressionString);
            assertTrue(ExpressionModel.class.isAssignableFrom(model.getClass()));
        }catch(Exception e) {
            fail();
        }
    }

}
