package io.cortical.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

public class ImmutableListCollectorTest {

    @Test
    public void testCollectToImmutable() {
        List<Integer> original = IntStream.range(0, 5).boxed().collect(Collectors.toList());
        List<Integer> immutable = original.stream().collect(ImmutableListCollector.toImmutableList());
        
        assertNotNull(immutable);
        assertEquals(5, immutable.size());
        try {
            immutable.add(5);
            fail("List should be immutable but isn't");
        }catch(Exception e) {
            assertEquals(UnsupportedOperationException.class, e.getClass());
        }
        
        original.add(5);
        assertEquals(6, original.size());
        assertEquals(5, immutable.size());
    }
    
    @Test
    public void testCollectToImmutable_CanSpecifyType() {
        List<Integer> original = IntStream.range(0, 5).boxed().collect(Collectors.toList());
        List<Integer> immutable = original.stream().collect(ImmutableListCollector.toImmutableList(LinkedList::new));
        
        assertNotNull(immutable);
        assertEquals(5, immutable.size());
        try {
            immutable.add(5);
            fail("List should be immutable but isn't");
        }catch(Exception e) {
            assertEquals(UnsupportedOperationException.class, e.getClass());
        }
        
        original.add(5);
        assertEquals(6, original.size());
        assertEquals(5, immutable.size());
    }

}
