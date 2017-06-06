package io.cortical.iris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.cortical.iris.message.ClipboardMessage;
import io.cortical.iris.ui.custom.widget.bubble.Bubble;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.model.ExpressionFactory;
import io.cortical.retina.model.ExpressionFactory.ExpressionModel;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Term;
import io.cortical.retina.rest.ApiException;
import javafx.util.Pair;
import rx.observers.TestObserver;
import rx.subjects.ReplaySubject;

public class DragNDropTest {
    public static final String ENGLISH_SERVER = "http://api.cortical.io/rest";
    public static final String ENGLISH_RETINA = "en_associative";
    
    private Pattern POS_PATTERN = Pattern.compile("([^0-9]{0}[\\d]+[\\d\\,\\s]*[^0-9]{0})");
    
    private String TEST_STRING = 
            "{\n"+
                "\t\"sub\": [\n" +
                   "\t\t{\n"+
                        "\t\t\t\"term\": \"jaguar\"\n"+
                   "\t\t},\n"+
                   "\t\t{\n" +
                        "\t\t\t\"positions\" : [ , 2, 3, 4, 5, \n6 ]\n"+
                   "\t\t}\n"+
                "\t]\n"+
            "}";

    @Test
    public void testPositionsPatternMatch() {
        Matcher m2 = POS_PATTERN.matcher(TEST_STRING);
        String expected = "2,3,4,5,6";
        assertEquals(1, m2.groupCount());
        assertTrue(m2.find());
        assertEquals(expected, m2.group(0).replaceAll("[\\s]*", ""));
        
        // Test sloppy input
        String testString = "\"positio:\" 1, 3, 4, 5, 6";
        m2 = POS_PATTERN.matcher(testString);
        expected = "1,3,4,5,6";
        assertEquals(1, m2.groupCount());
        assertTrue(m2.find());
        assertEquals(expected, m2.group(0).replaceAll("[\\s]*", ""));
        
        // Complex Expression
        String complex =
        "{"+
          "\"or\" : [ {"+
            "\"xor\" : [ {"+
              "\"term\" : \"car\""+
            "}, {"+
              "\"term\" : \"bus\""+
            "} ]"+
          "}, {"+
              "\"positions\" : [ , 2, 3, 4, 5, 6 ]"+
          "} ]"+
        "}";
        m2 = POS_PATTERN.matcher(complex);
        expected = "2,3,4,5,6";
        assertEquals(1, m2.groupCount());
        assertTrue(m2.find());
        assertEquals(expected, m2.group(0).replaceAll("[\\s]*", ""));
        
        // Complex Expression with positions in middle
        String complex2 =
        "{"+
          "\"or\" : [ {"+
            "\"xor\" : [ {"+
              "\"positions\" : [ , 2, 3, 4, 5, 6 ]"+
            "}, {"+
              "\"term\" : \"bus\""+
            "} ]"+
          "}, {"+
            "\"term\" : \"car\""+
          "} ]"+
        "}";
        m2 = POS_PATTERN.matcher(complex2);
        expected = "2,3,4,5,6";
        assertEquals(1, m2.groupCount());
        assertTrue(m2.find());
        assertEquals(expected, m2.group(0).replaceAll("[\\s]*", ""));
        
        // Complex Expression with two positions
        String complex3 =
        "{"+
          "\"or\" : [ {"+
            "\"xor\" : [ {"+
              "\"positions\" : [ , 2, 3, 4, 5, 6 ]"+
            "}, {"+
              "\"term\" : \"bus\""+
            "} ]"+
          "}, {"+
            "\"positions\" : [ , 7, 8, 9, 10, 11 ]"+
          "} ]"+
        "}";
        m2 = POS_PATTERN.matcher(complex3);
        expected = "2,3,4,5,6";
        assertEquals(1, m2.groupCount());
        assertTrue(m2.find());
        assertEquals(expected, m2.group(0).replaceAll("[\\s]*", ""));
        
        assertTrue(m2.find());
        assertEquals(1, m2.groupCount());
        expected = "7,8,9,10,11";
        assertEquals(expected, m2.group(0).replaceAll("[\\s]*", ""));
        
        // Test more simple
        String simple = "[ , 7, 8, 9, 10, 11 ]";
        m2 = POS_PATTERN.matcher(simple);
        expected = "7,8,9,10,11";
        assertEquals(1, m2.groupCount());
        assertTrue(m2.find());
        assertEquals(expected, m2.group(0).replaceAll("[\\s]*", ""));
        
        // Test even more simple
        simple = ", 7, 8, 9";
        m2 = POS_PATTERN.matcher(simple);
        expected = "7,8,9";
        assertEquals(1, m2.groupCount());
        assertTrue(m2.find());
        assertEquals(expected, m2.group(0).replaceAll("[\\s]*", ""));
        
        // Test simplest
        simple = "7";
        m2 = POS_PATTERN.matcher(simple);
        expected = "7";
        assertEquals(1, m2.groupCount());
        assertTrue(m2.find());
        assertEquals(expected, m2.group(0).replaceAll("[\\s]*", ""));
    }
    
    
    @Ignore
    public void testExpressionsWithPositions() {
        Matcher m = POS_PATTERN.matcher(TEST_STRING);
        m.find();
        String pos = m.group(0).replaceAll("[\\s]*", "");
        
        Fingerprint fp = new Fingerprint(getIntArray(pos));
        
        ExpressionModel model = ExpressionFactory.xor(new Term("jaguar"), fp);
        
        FullClient client = new FullClient("d059e560-1372-11e5-a409-7159d0ac8188", ENGLISH_SERVER, ENGLISH_RETINA);
        
        Fingerprint retVal = null;
        
        try {
            retVal = client.getFingerprintForExpression(model);
            assertTrue(retVal != null && retVal.getPositions().length > 4);
        } catch(ApiException a) {
            a.printStackTrace();
        } catch(JsonProcessingException j) {
            j.printStackTrace();
        }
        
        System.out.println(Arrays.stream(retVal.getPositions()).boxed().collect(Collectors.toList()) + "\n");
    }
    
    @Test
    public void testGetClipboardTransformer() {
        Pair<List<Bubble.Type>, List<String>> content = new Pair<List<Bubble.Type>, List<String>>(new ArrayList<Bubble.Type>(), new ArrayList<String>());
        Pair<List<Bubble.Type>, List<String>> content2 = new Pair<List<Bubble.Type>, List<String>>(new ArrayList<Bubble.Type>(), new ArrayList<String>());
        ClipboardMessage message = new ClipboardMessage(content);
        ClipboardMessage message2 = new ClipboardMessage(content2);
        
        ReplaySubject<ClipboardMessage> subject = ReplaySubject.createWithSize(1);
        TestObserver<ClipboardMessage> observer = new TestObserver<ClipboardMessage>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) {
                System.out.println("Got OnError of: " + e.getMessage());
            }
            @Override public void onNext(ClipboardMessage m) {
                System.out.println("Got OnNext of: " + m.getContent());
                super.onNext(m);
            }
        };
        
        subject.onNext(message);
        subject.onNext(message2);
//        subject.onCompleted();
//        subject.onError(new IllegalArgumentException("ErrorMessage"));
        
        subject.subscribe(observer);
        
        List<ClipboardMessage> list = new ArrayList<>(Arrays.asList(message2));
        observer.assertReceivedOnNext(list);
    }
    
    public int[] getIntArray(String posString) {
        return Arrays.stream(posString.trim().split(",")).mapToInt(Integer::parseInt).toArray();
    }

}
