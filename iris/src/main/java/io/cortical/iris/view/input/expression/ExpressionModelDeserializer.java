package io.cortical.iris.view.input.expression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.cortical.retina.model.ExpressionFactory;
import io.cortical.retina.model.ExpressionFactory.ExpressionModel;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Model;
import io.cortical.retina.model.Term;
import io.cortical.retina.model.Text;

/**
 * A Jackson {@link StdDeserializer} used as an argument to a standard
 * deserialization call when deseerializing an {@link ExpressionModel}. For example:
 * <pre>
 *      ObjectMapper mpr = new ObjectMapper();
 *      SimpleModule module = new SimpleModule();
 *      module.addDeserializer(ExpressionModel.class, new ExpressionModelDeserializer());
 *      mpr.registerModule(module);
 *               
 *      ExpressionModel readValue = mpr.readValue(json, ExpressionModel.class);
 * </pre> 
 * 
 */
public class ExpressionModelDeserializer extends StdDeserializer<ExpressionModel> {
    
    public enum IndexType { TERM, TEXT, AND, OR, SUB, XOR, FP, POS };
    
    private static final Pattern POS_PATTERN = Pattern.compile("([^0-9]{0}[\\d]+[\\d\\,\\s]*[^0-9]{0})");
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private Set<String> typeSet = new HashSet<>();
    
    
    /**
     * Constructs a new {@code ExpressionModelDeserializer}
     */
    public ExpressionModelDeserializer() { 
        this(null); 
    } 
 
    /**
     * Constructs a new {@code ExpressionModelDeserializer}
     * @param vc
     */
    public ExpressionModelDeserializer(Class<?> vc) { 
        super(vc); 
        
        typeSet.add("or");
        typeSet.add("sub");
        typeSet.add("xor");
        typeSet.add("and");
    }
 
    /**
     * Main functional method called by the Jackson library framework
     * when deserializing an {@link ExpressionModel}.
     */
    @Override
    public ExpressionModel deserialize(JsonParser jp, DeserializationContext ctxt) 
      throws IOException, JsonProcessingException {
        
        JsonNode node = jp.getCodec().readTree(jp);
        String expressionType = node.fieldNames().next();
        ArrayNode n = (ArrayNode)node.elements().next();
        List<Model> exprModels = new ArrayList<>();
        
        for(Iterator<JsonNode> it = n.elements(); it.hasNext();) {
            
            JsonNode child = it.next();
            String childName = child.fieldNames().next();
            
            if(typeSet.contains(childName)) {
                Model m = ExpressionModelDeserializer.narrow(child.toString());
                exprModels.add(m);
                continue;
            }else{
                ObjectMapper mapper = new ObjectMapper();
                Class<? extends Model> type = null;
                if(childName.equals("term")) {
                    type = Term.class;
                }else if(childName.equals("text")) {
                    type = Text.class;
                }else if(childName.equals("positions") || childName.equals("fingerprint")) {
                    Fingerprint fp = ExpressionModelDeserializer.extractFingerprint(child.toString());
                    exprModels.add(fp);
                    continue;
                }
                
                JsonParser p = child.traverse();
                Model m = mapper.readValue(p, type);
                exprModels.add(m);
            }
        }
        
        ExpressionModel retVal = null;
        if(expressionType.equals("or")) {
            retVal = ExpressionFactory.or(exprModels.toArray(new Model[exprModels.size()]));
        }else if(expressionType.equals("and")) {
            retVal = ExpressionFactory.and(exprModels.toArray(new Model[exprModels.size()]));
        }else if(expressionType.equals("xor")) {
            retVal = ExpressionFactory.xor(exprModels.toArray(new Model[exprModels.size()]));
        }else if(expressionType.equals("sub")) {
            retVal = ExpressionFactory.sub(exprModels.toArray(new Model[exprModels.size()]));
        }
        
        return retVal;
    }
    
    /**
     * Receives a {@link Model} and returns a {@link Fingerprint} object.
     * @param m
     * @return
     * @throws JsonProcessingException
     */
    public static Fingerprint extractFingerprint(Model m) throws JsonProcessingException {
        return extractFingerprint(m.toJson());
    }
    
    /**
     * Receives a JSON String and returns a {@link Fingerprint} object 
     * of the internally specified fingerprint or positions object within
     * the String.
     * 
     * @param json
     * @return
     */
    public static Fingerprint extractFingerprint(String json) {
        int[] pos = extractPositionsArray(json);        
        Fingerprint fp = new Fingerprint(pos);
        return fp;
    }
    
    /**
     * Returns a {@link Model} object of the appropriate type depending on 
     * the object expressed by the specified JSON String.
     * 
     * @param json
     * @return
     * @throws JsonParseException
     * @throws IOException
     */
    @SuppressWarnings("all")
    public static <T extends Model, M extends Class<T>> T narrow(String json) throws JsonParseException, IOException {
        try {
            isJsonString(json);
        }catch(Exception e) { 
            IndexType type = getLowestIndex(json);
            if(type != IndexType.POS && type != IndexType.FP) {
                throw e;
            }
        }
        
        ObjectMapper mapper = new ObjectMapper();
        
        Model obj = null;
        M type = null;
        
        switch(getLowestIndex(json)) {
            case TERM: type = (M)Term.class; break;
            case TEXT: type = (M)Text.class; break;
            case XOR: 
            case SUB:
            case OR:
            case AND: { 
                ObjectMapper mpr = new ObjectMapper();
                SimpleModule module = new SimpleModule();
                module.addDeserializer(ExpressionModel.class, new ExpressionModelDeserializer());
                mpr.registerModule(module);
                 
                ExpressionModel readValue = mpr.readValue(json, ExpressionModel.class);
                return (T)readValue;
            }
            case FP: 
            case POS: {
                int[] array = extractPositionsArray(json);
                Fingerprint fp = new Fingerprint(array);
                return (T)fp;
            }
        }
        
        try {
            obj = mapper.readValue(json, type);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        return (T)obj;
    }
    
    /**
     * Returns the outer-most JSON class type.
     * 
     * @param json
     * @return
     */
    public static IndexType getLowestIndex(String json) {
        int term = -1, text = -1, and = -1, or = -1, xor = -1, sub = -1, fp = -1, pos = -1;
        
        term = json.toLowerCase().indexOf("\"term\"");
        text = json.toLowerCase().indexOf("\"text\"");
        or = json.toLowerCase().indexOf("\"or\"");
        and = json.toLowerCase().indexOf("\"and\"");
        xor = json.toLowerCase().indexOf("\"xor\"");
        sub = json.toLowerCase().indexOf("\"sub\"");
        pos = json.toLowerCase().indexOf("\"positions\"");
        fp = json.toLowerCase().indexOf("\"fingerprint\"");
        
        int[] types = { term, text, and, or, sub, xor, fp, pos };
        int least = Integer.MAX_VALUE;
        int i = 0;
        IndexType found = null;
        for(int t : types) {
            if(t == -1) { ++i; continue; }
            if(t < least) {
                least = t;
                found = IndexType.values()[i];
            }
            ++i;
        }

        return found;
    }
    
    /**
     * Returns an integer array from the internally specified position's 
     * array found within the specified JSON String.
     * 
     * @param json      a JSON formatted String containing a \"positions\" array.
     * @return
     */
    public static int[] extractPositionsArray(String json) {
        Matcher m = POS_PATTERN.matcher(json);
        if(m.find()) {
            String arrayString = m.group(0).replaceAll("[\\s]*", "");
            int[] positions = getIntArray(arrayString);
            
            return positions;
        }
        return null;
    }
    
    /**
     * Returns an integer array from a comma-separated String of numbers.
     * @param positionsString
     * @return
     */
    public static int[] getIntArray(String positionsString) {
        return Arrays.stream(positionsString.trim().split(",")).mapToInt(Integer::parseInt).toArray();
    }
    
    /**
     * Returns a comma separated String containing all the positions of the specified int[].
     * @param array
     * @return
     */
    public static String toStringifiedArray(int[] array) {
        return Arrays.stream(array).mapToObj(i -> String.valueOf(i)).collect(Collectors.joining(","));
    }
    
    /**
     * Validates the specified String, returning a flag indicating whether
     * the specified String is valid JSON or not.
     * 
     * @param s     the JSON String to test.
     * @return      true if it is valid JSON, false if not.
     * @throws JsonParseException
     * @throws IOException
     */
    public static boolean isJsonString(String s) throws JsonParseException, IOException {
        boolean valid = false;
        try {
           final JsonParser parser = new ObjectMapper().getFactory().createParser(s);
                 
           while (parser.nextToken() != null) {
           }
           valid = true;
        } catch (JsonParseException jpe) {
           throw jpe;
        } catch (IOException ioe) {
           throw ioe;
        }
        
        return valid;
    }
}
