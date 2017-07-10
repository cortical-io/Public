package io.cortical.iris.view.input.expression;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.cortical.iris.ServerMessageService;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.Message;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.iris.ui.custom.widget.bubble.Entry;
import io.cortical.iris.ui.custom.widget.bubble.WordBubble;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.view.input.expression.ExpressionWordBubbleContainer.Mode;
import io.cortical.iris.window.Window;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.model.ExpressionFactory;
import io.cortical.retina.model.ExpressionFactory.ExpressionModel;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Model;
import io.cortical.retina.model.Term;
import io.cortical.retina.model.Text;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Pair;

/**
 * Used to create {@link Model}s from a list of {@link Bubble}s.
 * @author cogmission
 *
 */
public class BubbleExpressionBuilder {
    private Pattern EXPRESSION_PATTERN = Pattern.compile("((?:\\()*\\s*(?:[a-zA-Z]+)+\\s*(?:\\))*\\s*(?:\\&{1}|\\|{1}|\\!{1}|\\^{1})?\\s*)+");
    private Pattern EXCLUDE_RULE = Pattern.compile("\\)+\\s*[a-zA-Z]+|[a-zA-Z]+\\s*\\(+");
    private Pattern TEXT_PATTERN = Pattern.compile("((?:[a-zA-Z]+\\s+){2,}[a-zA-Z]+)");
    
    private Deque<Object> stack = new ArrayDeque<>();
    
    private Window window;
    
    /**
     * Returns an {@link ExpressionModel} created from the specified {@link Operator}
     * and array of {@link Model}s.
     * 
     * @param op        the {@link Operator} indicating which expression type to return.
     * @param models    the operands
     * @return
     */
    private ExpressionModel getExpressionOp(Operator op, Model... models) {
        switch(op) {
            case AND:
                return ExpressionFactory.and(models);
            case OR:
                return ExpressionFactory.or(models);
            case NOT:
                return ExpressionFactory.sub(models);
            case XOR:
                return ExpressionFactory.xor(models);
            default:
                throw new IllegalStateException("Deep problem with parsing - incorrect operator type!");      
        }
    }
    
    /**
     * Sets the identifying {@link Window}
     * @param w
     */
    public void setWindow(Window w) {
        this.window = w;
    }
    
    /**
     * Returns a {@link Term} if the specified text is only one word,
     * otherwise a {@link Text} object is returned.
     * 
     * @param s
     * @return
     */
    private Model getTermOrText(String s) {
        if(s.trim().indexOf(" ") == -1) {
            return new Term(s);
        } else if(s.split("[\\s]+").length == 2) {
        
            ServerRequest req = new ServerRequest();
            req.setTermString(s);
            req.setInputViewType(ViewType.TOKEN);
            req.setPayload(new Payload(new Pair<UUID, Message>(window.getWindowID(), new Message(ViewType.TERM, null))));
            FullClient currentClient = WindowService.getInstance().clientRetinaFor(window);
            req.setRetinaClient(currentClient);
            
            List<String> tokens = ServerMessageService.getInstance().routeCompoundTermLookup(req);
            
            return tokens.size() == 1 ? new Term(s) : new Text(s);
        }
        
        return new Text(s);
    }
    
    /**
     * Executes a balance check on the specified list of Bubble types to be sure that
     * there are whole pairs of parenthesis in the list.
     * @param bubbles       the list of {@link Bubble}s encapsulating terms, text and parenthesis
     * @throws IllegalArgumentException when a parenthesis without an opposing parenthesis exists
     */
    public void checkParens(FilteredBubbleList bubbles) throws IllegalArgumentException {
        int semaphore = 0;
        for(Bubble b : bubbles) {
            if(b.getType() == Bubble.Type.LPAREN) {
                ++semaphore;
            }else if(b.getType() == Bubble.Type.RPAREN) {
                --semaphore;
            }
        }
        if(semaphore != 0) {
            throw new IllegalArgumentException("Unbalanced parenthesis in bubble list.");
        }
    }
    
    /**
     * Returns the {@link ExpressionModel} or {@link Model} representing
     * the expression indicated by the specified list of {@link Bubble}s.
     * 
     * @param bubbles       the list of bubbles organized as a sequence of expression parts. 
     * @return
     * @throws  IllegalArgumentException    if there are unbalanced parenthesis
     */
    public Model parseExpression(FilteredBubbleList bubbles) throws IllegalArgumentException {
        checkParens(bubbles);
        
        return doParse(bubbles);
    }
    
    /**
     * Returns the {@link ExpressionModel} or {@link Model} representing
     * the expression indicated by the specified list of {@link Bubble}s.
     * 
     * @param bubbles       the list of bubbles organized as a sequence of expression parts. 
     * @return
     */
    public Model doParse(FilteredBubbleList bubbles) {
        if(bubbles.isEmpty()) {
            Object o = stack.pop();
            stack.clear();
            return (Model)o;
        }
        
        Bubble b = bubbles.remove(0);
        switch(b.getType()) {
            case WORD: {
                if(stack.size() < 2) {
                    stack.push(getTermOrText(b.getText()));
                }else{
                    stack.push(getExpressionOp((Operator)stack.pop(), (Model)stack.pop(), getTermOrText(b.getText())));
                }
                return doParse(bubbles);
            }
            case LPAREN: {
                // Save the stack then empty it to begin nested recursion if necessary.
                Deque<Object> temp = null;
                if(stack.size() > 0) {
                    temp = new ArrayDeque<>(stack);
                    stack.clear();
                }
                
                // Do nested recursion
                Model exp = doParse(bubbles);
                
                if((stack = temp == null ? stack : temp).size() < 2) {
                    stack.push(exp);
                }else{
                    exp = getExpressionOp((Operator)stack.pop(), (Model)stack.pop(), exp);
                    stack.push(exp);
                }
                return doParse(bubbles);
            }
            case RPAREN: {
                return (Model)stack.pop();
            }
            case OPERATOR: {
                stack.push(b.getOperator());
                return doParse(bubbles);
            }
            case FINGERPRINT: {
                if(stack.size() < 2) {
                    stack.push(new Fingerprint(((FingerprintBubble)b).getPositions()));
                }else{
                    stack.push(
                        getExpressionOp
                            ((Operator)stack.pop(), 
                                (Model)stack.pop(), 
                                    new Fingerprint(((FingerprintBubble)b).getPositions())));
                }
                return doParse(bubbles);
            }
            case FIELD: {
                throw new IllegalArgumentException("Bubble list should not contain a field");
            }
        }
        return null;
    }
    
    /**
     * Uses the class name of the specified Model to discern
     * the type of {@link Operator} to return.
     * 
     * @param expressionModel   One of... ({@link AndExpression}, {@link OrExpression} etc.
     * @return
     */
    private Operator getOperator(Model expressionModel) {
        if(!(expressionModel instanceof ExpressionModel)) {
            throw new IllegalArgumentException("\"expressionModel\" must be of type ExpressionModel!");
        }
        
        Class<?> clazz = expressionModel.getClass();
        String fieldName = clazz.getSimpleName().substring(0, clazz.getSimpleName().indexOf("Expression")).toLowerCase();
        String modelArrayFieldName = fieldName.equals("sub") ? "not" : fieldName;
        return Arrays.stream(Operator.values()).filter(op -> op.toString().toLowerCase().equals(modelArrayFieldName)).findFirst().orElse(null);
    }
    
    /**
     * Uses reflection to return the private Model array field of an
     * {@link ExpressionModel}
     * 
     * @param model
     * @return
     */
    private Model[] extractExpressionModels(Model model) {
        Class<?> clazz = model.getClass();
        String modelArrayFieldName = clazz.getSimpleName().substring(0, clazz.getSimpleName().indexOf("Expression")).toLowerCase();
        Model[] retVal = null;
        try {
            Field field = clazz.getDeclaredField(modelArrayFieldName);
            field.setAccessible(true);
            retVal = (Model[])field.get(model);
        }catch(Exception e) { e.printStackTrace(); }
        
        return retVal;
    }
    
    /**
     * Takes in any kind of {@link Model} and returns a {@link Pair} containing
     * two equal sized lists: a list of {@link Bubble.Type}s; and a list of display
     * strings which a {@link Bubble} of a given type would display.
     * 
     * From these lists, a {@link Node} list of {@link Entry}s can be constructed
     * which is capable of being directly inserted into the view of an
     * {@link ExpressionWordBubbleContainer}. (see {@link #buildEntryList(ExpressionWordBubbleContainer, List, List)})
     *  
     * @param model     a Cortical.io {@link Model} object (i.e. Term, Text, ExpressionModel etc.)
     * @param pair      {@link Pair} object containing the 2 lists.
     * @param index     Recursive control variable which designates the index of an ExpressionModel (0 or 1) where
     *                  the specified Model resides in its parent Model array (the ExpressionModel itself).
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Pair<List<Bubble.Type>, List<String>>> T doParse(Model model, T pair, int index) {
        if(pair == null) {
            index = 0;
            pair = (T)new Pair<List<Bubble.Type>, List<String>>(new ArrayList<Bubble.Type>(), new ArrayList<String>()); 
            // First time through so clear everything
            stack.clear();
        }
        
        if(model instanceof ExpressionModel) {
            Model[] models = extractExpressionModels(model);
            
            // If index == 1 add a Parenthesis
            if(index == 1) {
                pair.getKey().add(Bubble.Type.LPAREN);
                pair.getValue().add(Operator.L_PRN.toDisplay());
            }
            
            doParse(models[0], pair, 0);
            pair.getKey().add(Bubble.Type.OPERATOR);
            pair.getValue().add(getOperator(model).toDisplay());
            doParse(models[1], pair, 1);
            if(index == 1) {
                pair.getKey().add(Bubble.Type.RPAREN);
                pair.getValue().add(Operator.R_PRN.toDisplay());
            }
        } else if (model instanceof Term) {
            pair.getKey().add(Bubble.Type.WORD);
            pair.getValue().add(((Term)model).getTerm());
        } else if(model instanceof Fingerprint) {
            pair.getKey().add(Bubble.Type.FINGERPRINT);
            pair.getValue().add(ExpressionModelDeserializer.toStringifiedArray(((Fingerprint)model).getPositions()));
        }
        
        return pair;
    }
    
    public String validateFreehand(String freehand) {
        String retVal = "";
        try {
            validateExpression(freehand);
        }catch(Exception e) {
            retVal = e.getMessage();
        }
        
        return retVal;
    }
    
    public boolean isValidFreehand(String freehand) {
        try {
            validateExpression(freehand);
        }catch(Exception e) {
            return false;
        }
        
        return true;
    }
    
    public Pair<List<Bubble.Type>, List<String>> doParse(String freehand) throws IllegalArgumentException {
        validateFreehand(freehand);
        
        
        Pair<List<Bubble.Type>, List<String>> retVal = new Pair<List<Bubble.Type>, List<String>>(
            new ArrayList<Bubble.Type>(), new ArrayList<String>());
        
        List<Bubble.Type> types = retVal.getKey();
        List<String> texts = retVal.getValue();
        
        StringBuilder buffer = new StringBuilder();
        freehand.chars().mapToObj(c -> (char)c).forEach(c -> {
            switch(c) {
                case '(': {
                    if(buffer.length() > 0) addBuffer(buffer, retVal);
                    types.add(Bubble.Type.LPAREN); texts.add("("); 
                    break;
                }
                case ')': {
                    if(buffer.length() > 0) addBuffer(buffer, retVal);
                    types.add(Bubble.Type.RPAREN); texts.add(")"); 
                    break;
                }
                case '!': {
                    if(buffer.length() > 0) addBuffer(buffer, retVal);
                    types.add(Bubble.Type.OPERATOR); texts.add("!"); 
                    break;
                }
                case '^': { 
                    if(buffer.length() > 0) addBuffer(buffer, retVal);
                    types.add(Bubble.Type.OPERATOR); texts.add("^"); 
                    break;
                }
                case '&': {
                    if(buffer.length() > 0) addBuffer(buffer, retVal);
                    types.add(Bubble.Type.OPERATOR); texts.add("&"); 
                    break;
                }
                case '|': {
                    if(buffer.length() > 0) addBuffer(buffer, retVal);
                    types.add(Bubble.Type.OPERATOR); texts.add("|"); 
                    break;
                }
                case ' ':
                default : {
                    buffer.append(Character.toString(c));
                    break;
                }
            }
        });
        
        if(buffer.length() > 0) {
            addBuffer(buffer, retVal);
        }
        
        return retVal;
    }
    
    private void addBuffer(StringBuilder buffer, Pair<List<Bubble.Type>, List<String>> pair) {
        if(buffer.toString().trim().length() < 1) return;
        pair.getKey().add(Bubble.Type.WORD);
        pair.getValue().add(buffer.toString().trim());
        buffer.setLength(0);
    }
    
    /**
     * Returns a list of {@link Entry}s suitable for insertion into the child-node 
     * list of children of the {@link ExpressionWordBubbleContainer}.
     * 
     * @param container         current {@link ExpressionWordBubbleContainer}
     * @param bubbleTypes       list of {@link Bubble.Type}s to restore
     * @param bubbleSymbols     list of specific text or symbols of each bubble.
     * @return
     */
    public List<Entry<?>> buildEntryList(ExpressionWordBubbleContainer container, List<Bubble.Type> bubbleTypes, List<String> bubbleSymbols) {
        List<Entry<?>> entries = new ArrayList<>();
        for(int i = 0;i < bubbleTypes.size();i++) {
            Bubble.Type t = bubbleTypes.get(i);
            String symbol = bubbleSymbols.get(i);
            Entry<?> b = null;
            switch(t) {
                case FIELD: continue;
                case WORD: {
                    Entry<WordBubble> e = new Entry<>(new WordBubble(symbol));
                    e.getBubble().selectedProperty().addListener(container.getTermSelectionListener(e, symbol));
                    e.getBubble().addEventHandler(MouseEvent.MOUSE_CLICKED, container.getTermDoubleClickListener(e));
                    b = e;
                    break;
                }
                case OPERATOR: {
                    OperatorBubble ob = new OperatorBubble(Operator.typeFor(symbol));
                    Entry<OperatorBubble> e = new Entry<>(ob);
                    ob.addEventHandler(MouseEvent.MOUSE_PRESSED, (MouseEvent m) -> {
                        // Prevent operator edit if already in edit mode.
                        if(container.getMode() == Mode.EDIT) return;
                        
                        container.displayOperatorChoice(e.getBubble());
                    });
                    b = e;
                    break;
                }
                case LPAREN:
                case RPAREN: {
                    ParenthesisBubble pb = new ParenthesisBubble(Operator.typeFor(symbol));
                    Entry<ParenthesisBubble> e = new Entry<>(pb);
                    pb.selectedProperty().addListener(container.getParenthesisSelectionListener(pb, e));
                    b = e;
                    break;
                }
                case FINGERPRINT: {
                    FingerprintBubble fb = new FingerprintBubble(ExpressionModelDeserializer.getIntArray(symbol));
                    fb.addEventHandler(MouseEvent.ANY, e -> {
                        if(e.getEventType().equals(MouseEvent.MOUSE_PRESSED) && e.isSecondaryButtonDown()) {
                            fb.showPopup(e);
                        } else if(e.getEventType().equals(MouseEvent.MOUSE_RELEASED)) {
                            fb.hidePopup();
                        }
                    });
                    Entry<FingerprintBubble> e = new Entry<>(fb);
                    e.getBubble().selectedProperty().addListener(container.getFingerprintSelectionListener(e));
                    b = e;
                    break;
                }
            }
            
            b.setAlignment(Pos.CENTER);
            b.addEventHandler(KeyEvent.KEY_RELEASED, container.getNavigationKeyListener());
            b.addEventHandler(KeyEvent.KEY_RELEASED, container.getControlKeyListener());
            entries.add(b);
        }
        
        return entries;
    }
    
    /**
     * Checks for 3 or more words in sequence without an operator or parenthesis separating them
     * which would qualify the string as text and not an expression. Expressions can sometimes have
     * two words separated by a space because they can be compound words such as "ice cream".
     * 
     * @param text
     */
    private void checkTextPattern(String text) {
        Matcher m = TEXT_PATTERN.matcher(text);
        if(m.find()) {
            throw new IllegalArgumentException("The text: --> \"" + text + "\" <-- appears to be text and not an expression.");
        }
    }
    
    /**
     * Called internally to do a parenthesis matching validation.
     * 
     * @param expression
     * @throws IllegalArgumentException if a problem is found
     */
    private void checkParenMatches(String expression) {
        class CharLocation {
            char c;
            int loc;
            public CharLocation(char kar, int i) { c = kar; loc = i; }
        }
        char c;
        Stack<CharLocation> stack = new Stack<>();
        for(int i = 0;i < expression.length();i++) {
            c = expression.charAt(i);
            if(c == '(') stack.push(new CharLocation(c, i));
            else if(c == ')') {
                if(stack.isEmpty()) {
                    stack.push(new CharLocation(c, i));
                    break;
                }
                stack.pop();    
            }
        }
        if(!stack.isEmpty()) {
            throw new IllegalArgumentException("Unmatched parenthesis \"" + stack.get(0).c + "\" at index: " + stack.get(0).loc);
        }
    }
    
    
    /**
     * Expects a String in the format of: ((Fox trot) | Rabbit & ((Rodent ! Squirrel)) & (volkswagen ^ (bug & plane))
     * 
     * and throws an {@link IllegalArgumentException} with specific error locations, if a problem is found.
     * 
     * @param expression
     * @throws IllegalArgumentException
     */
    public void validateExpression(String expression) throws IllegalArgumentException {
        checkTextPattern(expression);
        
        checkParenMatches(expression);
        
        Matcher m = EXCLUDE_RULE.matcher(expression);
        if(m.find()) {
            String errorString = null;
            if(m.group(0).indexOf("(") != -1) {
                errorString = "Bad character: \"(\" near index: " + (expression.indexOf(m.group(0)) + m.group(0).length() - 1);  
            } else {
                errorString = "Bad character: \")\" near index: " + expression.indexOf(m.group(0));
            }
            throw new IllegalArgumentException(errorString);
        }
        
        m = EXPRESSION_PATTERN.matcher(expression);
        int expressionLen = expression.length();
        if(m.find()) {
            if(m.group(0).length() != expressionLen) {
                int failIndex = m.start() == 0 ? m.group(0).length() : m.start() - 1;
                System.out.println(m.group(0));
                m.find();
                System.out.println(m.group(0));
                throw new IllegalArgumentException(
                    "Bad character: \"" + expression.substring(failIndex, failIndex + 1) + 
                    "\" near index: " + failIndex);
            }
        }
    }
}
