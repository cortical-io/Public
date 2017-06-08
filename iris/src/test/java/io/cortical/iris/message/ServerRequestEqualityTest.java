package io.cortical.iris.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import io.cortical.iris.message.Message;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.view.ViewType;
import io.cortical.retina.core.PosTag;
import io.cortical.retina.core.PosType;
import io.cortical.retina.model.Model;
import io.cortical.retina.model.Term;
import javafx.util.Pair;


public class ServerRequestEqualityTest {

    @Test
    public void testRawEquality() {
        ServerRequest req = new ServerRequest();
        ServerRequest req2 = new ServerRequest();
        assertEquals(req, req2);
    }
    
    @Test
    public void testTermEquality() {
        UUID uuid = UUID.randomUUID();
        ViewType viewType = ViewType.EXPRESSION;
        String termStr = "bird";
        
        ServerRequest req = createRequest(uuid, viewType, new Term(termStr));
        ServerRequest req2 = createRequest(uuid, viewType, new Term(termStr));
        assertEquals(req, req2);
        
        // Now vary the term slightly
        req2 = createRequest(UUID.randomUUID(), viewType, new Term(termStr)); // by uuid
        assertNotEquals(req, req2);
        
        req2 = createRequest(uuid, ViewType.CLASSIFY, new Term(termStr)); // by ViewType
        assertNotEquals(req, req2);
        
        req2 = createRequest(uuid, viewType, new Term("bird ")); // vary term (add space)
        assertNotEquals(req, req2); 
        
        req2 = createRequest(uuid, viewType, new Term("eagle")); // vary term
        assertNotEquals(req, req2);
        
        // Now set it back and reconfirm equality
        req2 = createRequest(uuid, viewType, new Term("bird")); // vary term
        assertEquals(req, req2);
    }
    
    @Test
    public void testRangeVariables() {
        UUID uuid = UUID.randomUUID();
        ViewType viewType = ViewType.EXPRESSION;
        String termStr = "bird";
        
        ServerRequest req = createRequest(uuid, viewType, new Term(termStr));
        ServerRequest req2 = createRequest(uuid, viewType, new Term(termStr));
        assertEquals(req, req2);
        
        // simTermsMaxResults
        assertEquals(10, req.getSimTermsMaxResults());
        assertEquals(10, req2.getSimTermsMaxResults());
        assertEquals(req, req2);
        req2.setSimTermsMaxResults(100);
        assertNotEquals(req, req2);
        
        // contextsMaxResults
        req = createRequest(uuid, viewType, new Term(termStr));
        req2 = createRequest(uuid, viewType, new Term(termStr));
        assertEquals(10, req.getContextsMaxResults());
        assertEquals(10, req2.getContextsMaxResults());
        assertEquals(req, req2);
        req2.setContextsMaxResults(100);
        assertNotEquals(req, req2);
        
        // slicesMaxResults
        req = createRequest(uuid, viewType, new Term(termStr));
        req2 = createRequest(uuid, viewType, new Term(termStr));
        assertEquals(10, req.getSlicesMaxResults());
        assertEquals(10, req2.getSlicesMaxResults());
        assertEquals(req, req2);
        req2.setSlicesMaxResults(100);
        assertNotEquals(req, req2);
        
        /////////////
        
        // simTermsStartIndex
        req = createRequest(uuid, viewType, new Term(termStr));
        req2 = createRequest(uuid, viewType, new Term(termStr));
        assertEquals(0, req.getSimTermsStartIndex());
        assertEquals(0, req2.getSimTermsStartIndex());
        assertEquals(req, req2);
        req2.setSimTermsStartIndex(1);
        assertNotEquals(req, req2);
        
        // contextsStartIndex
        req = createRequest(uuid, viewType, new Term(termStr));
        req2 = createRequest(uuid, viewType, new Term(termStr));
        assertEquals(0, req.getContextsStartIndex());
        assertEquals(0, req2.getContextsStartIndex());
        assertEquals(req, req2);
        req2.setContextsStartIndex(1);
        assertNotEquals(req, req2);
        
        // simTermsStartIndex
        req = createRequest(uuid, viewType, new Term(termStr));
        req2 = createRequest(uuid, viewType, new Term(termStr));
        assertEquals(0, req.getSlicesStartIndex());
        assertEquals(0, req2.getSlicesStartIndex());
        assertEquals(req, req2);
        req2.setSlicesStartIndex(1);
        assertNotEquals(req, req2);
    }
    
    @Test
    public void testPosTags() {
        UUID uuid = UUID.randomUUID();
        ViewType viewType = ViewType.EXPRESSION;
        String termStr = "bird";
        
        ServerRequest req = createRequest(uuid, viewType, new Term(termStr));
        ServerRequest req2 = createRequest(uuid, viewType, new Term(termStr));
        assertEquals(req, req2);
        
        Set<PosTag> posTags = Stream.of(PosTag.JJ, PosTag.CW, PosTag.NN)
            .collect(Collectors.toSet());
        req.setPosTags(posTags);
        posTags = Stream.of(PosTag.CW, PosTag.JJ, PosTag.NN)
            .collect(Collectors.toSet()); // Same thing different order
        req2.setPosTags(posTags);
        assertEquals(req, req2);
        
        posTags = Stream.of(PosTag.CW, PosTag.VB, PosTag.NN)
            .collect(Collectors.toSet()); // diff set
        req2.setPosTags(posTags);
        assertNotEquals(req, req2);
    }
    
    @Test
    public void testPartOfSpeechTypes() {
        UUID uuid = UUID.randomUUID();
        ViewType viewType = ViewType.EXPRESSION;
        String termStr = "bird";
        
        ServerRequest req = createRequest(uuid, viewType, new Term(termStr));
        ServerRequest req2 = createRequest(uuid, viewType, new Term(termStr));
        assertEquals(req, req2);
        
        req.setPartOfSpeech(PosType.ADJECTIVE);
        req2.setPartOfSpeech(PosType.ADJECTIVE);
        assertEquals(req, req2);
        
        req.setPartOfSpeech(PosType.ADJECTIVE);
        req2.setPartOfSpeech(PosType.ANY);
        assertNotEquals(req, req2);
    }

    private ServerRequest createRequest(UUID uuid, ViewType viewType, Model model) {
        Payload payload = new Payload(
            new Pair<UUID, Message>(
                uuid, 
                new Message(viewType, model)));
        
        ServerRequest req = new ServerRequest();
        req.setPayload(payload);
        
        return req;
    }
}
