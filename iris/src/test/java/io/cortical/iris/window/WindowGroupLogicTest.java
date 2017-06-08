package io.cortical.iris.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WindowGroupLogicTest {

    Window pWindow, oldPWindow, sWindow, oldSWindow;
    
    /**
     * Run through a series of state permutations to guarantee functionality
     * of the {@link WindowGroup} exclusion and grouping logic.
     */
    @Test
    public void testPrimarySecondaryLogic() {
        WindowGroup group = new WindowGroup();
        group.setTestMode(true);
        
        group.primaryWindowProperty().addListener((v,o,n) -> {
            oldPWindow = o;
            pWindow = n;
        });
        
        group.secondaryWindowProperty().addListener((v,o,n) -> {
            oldSWindow = o;
            sWindow = n; 
        });
        
        Window w1 = Window.windowForTest();
        Window w2 = Window.windowForTest();
        assertTrue(w1.getWindowID() != null && w2.getWindowID() != null && 
            (!w1.getWindowID().equals(w2.getWindowID())));
        
        // Test that setting the primary window will set the expected 
        // object reference (pWindow)
        group.addWindow(w1);
        assertTrue(group.getPrimaryWindow() == pWindow);
        assertEquals(group.getChildren().size(), 1);
        
        // Assert resetting the same primary window changes nothing
        group.primaryWindowProperty().set(w1);
        assertNull(oldPWindow); // Null because there was no change event sent for same value
        assertEquals(w1, pWindow);
        assertNotEquals(pWindow, sWindow);
        assertNull(oldPWindow);
        
        group.addWindow(w2);
        assertEquals(sWindow, w2);
        assertNull(oldSWindow);
        
        // Now test swap
        group.primaryWindowProperty().set(w2);
        assertEquals(w2, group.primaryWindowProperty().get());
        assertEquals(w1, group.secondaryWindowProperty().get());
        assertEquals(w1, oldPWindow);
        assertEquals(w2, oldSWindow);
        
        // Now test removal 
        group.secondaryWindowProperty().set(null);
        assertEquals(w1, oldSWindow);
        assertNull(group.secondaryWindowProperty().get());
        assertEquals(w2, group.primaryWindowProperty().get()); // Assert primary doesn't change
        
        // Now add the secondary back in and test removal of the primary
        group.secondaryWindowProperty().set(w1);
        assertNull(oldSWindow);
        // Now remove
        group.removeWindow(w2); // Remove primary
        assertNotNull(group.primaryWindowProperty().get());
        assertEquals(w1, group.primaryWindowProperty().get());
        assertNull(group.secondaryWindowProperty().get());
        
        // Test adding now that there's a primary, results in secondary being auto-set to the new addition
        group.addWindow(w2);
        assertNotNull(group.secondaryWindowProperty().get());
        assertEquals(w2, group.secondaryWindowProperty().get());
        assertNull(oldSWindow);
    }

}
