package io.cortical.iris.ui.custom.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import io.cortical.iris.ui.custom.property.Occurrence;
import io.cortical.iris.ui.custom.property.OccurrenceListener;
import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import javafx.beans.value.ObservableValue;


/**
 * Tests the functionality of the {@link OccurrenceProperty}
 * @author cogmission
 *
 */
public class OccurrencePropertyTest {
    boolean eventPropagated = false;
    boolean eventPropagated2 = false;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testAddListener() {
        eventPropagated = false;
        
        OccurrenceProperty property = new OccurrenceProperty();
        OccurrenceListener ol = (v,o,n) -> {
            assertNotNull(n);
            assertTrue(n.isPresent());
            assertEquals(1, ((Occurrence)n.get()).sequence());
            eventPropagated = true;
        };
        property.addListener(ol);
        
        property.set();
        
        assertTrue(eventPropagated);
        
        /////////   Test can add anonymous listener as well as Lambda //////////
        
        property = new OccurrenceProperty();
        eventPropagated = false;
        OccurrenceListener ol2 = new OccurrenceListener() {
            @Override
            public void occurred(ObservableValue observable, Optional oldValue, Optional newValue) {
                assertNotNull(newValue);
                assertTrue(newValue.isPresent());
                assertEquals(1, ((Occurrence)newValue.get()).sequence());
                eventPropagated = true;
            }
        };
        property.addListener(ol2);
        property.set();
        
        assertTrue(eventPropagated);
    }
    
    @Test
    public void testSet_NoArg() {
        OccurrenceProperty property = new OccurrenceProperty();
        property.addListener((v,o,n) -> {
            assertNotNull(n);
            assertTrue(n.isPresent());
            assertEquals(1, n.get().sequence());
        });
        
        property.set();
    }
    
    @Test
    public void testSetValue() {
        OccurrenceProperty property = new OccurrenceProperty();
        property.addListener((v,o,n) -> {
            assertNotNull(n);
            assertTrue(n.isPresent());
        });
        
        property.set(Optional.of(() -> { return property.next(); }));
        property.setValue(Optional.of(() -> { return property.next(); }));
    }
    
    @Test
    public void testSet_increments() {
        OccurrenceProperty property = new OccurrenceProperty();
        property.set(); // sequence = 1
        
        property.addListener((v,o,n) -> {
            assertNotNull(n);
            assertTrue(n.isPresent());
            assertEquals(2, n.get().sequence()); // Now equals 2
        });
        
        property.set(); // should force increment of sequence
    }
    
    @Test
    public void testSetWorksWithOccurrenceSubclasses() {
        //////////////////////////
        //        Anonymous     //
        //////////////////////////
        OccurrenceProperty property = new OccurrenceProperty();
        property.addListener((v,o,n) -> {
            assertNotNull(n);
            assertTrue(n.isPresent());
            assertEquals(50, n.get().sequence());
        });
        
        Occurrence oc = new Occurrence() {
            @Override
            public int sequence() {
                return 50;
            }
        };
        
        property.set(Optional.of(oc));
        
        //////////////////////////
        //        Subclass      //
        //////////////////////////
        class OccurrenceChild implements Occurrence {
            @Override
            public int sequence() {
                return 100;
            }
            public String getString() {
                return "the string";
            }
        }
        property = new OccurrenceProperty();
        property.addListener((v,o,n) -> {
            assertNotNull(n);
            assertTrue(n.isPresent());
            assertEquals(100, n.get().sequence());
            assertEquals("the string", ((OccurrenceChild)n.get()).getString());
        });
        
        OccurrenceChild child = new OccurrenceChild();
        property.set(Optional.of(child));
    }
    
    @Test
    public void testRemoveListener() {
        eventPropagated2 = false;
        
        OccurrenceProperty property = new OccurrenceProperty();
        OccurrenceListener<Optional<Occurrence>> ol = (v,o,n) -> {
            assertNotNull(n);
            assertTrue(n.isPresent());
            eventPropagated2 = true;
        };
        property.addListener(ol);
        
        property.set();
        assertTrue(eventPropagated2);    
        eventPropagated2 = false;
        property.set();
        assertTrue(eventPropagated2);
        
        eventPropagated2 = false;
        property.removeListener(ol);
        property.set();
        assertTrue(!eventPropagated2);
        
    }

}
