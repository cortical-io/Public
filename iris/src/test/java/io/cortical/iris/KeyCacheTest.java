package io.cortical.iris;

import static org.junit.Assert.*;

import org.junit.Test;


public class KeyCacheTest {

    @Test
    public void testKeyCacheStorage() {
        String apiKey = "d059e560-1372-11e5-a409-7159d0ac8188";
        KeyCache cache = new KeyCache(apiKey);
        assertNotNull(cache);
        
        try {
            cache.store();
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
        
        try {
            String storedKey = KeyCache.getKey();
            assertNotNull(storedKey);
            assertEquals(apiKey, storedKey);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

}
