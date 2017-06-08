package io.cortical.iris;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Api Key storage abstraction for ease of loading and storing of the api key
 * so that the user doesn't have to enter it in if they've already done it.
 * 
 * @author cogmission
 * @see ApplicationService#loadApiKey()
 */
public class KeyCache {
    private static final String CACHE = System.getProperty("user.home") + File.separator + ".apikeycache";
    private static String key;
    
    
    
    public KeyCache(String key) {
        KeyCache.key = key;
    }
    
    /**
     * Stores the api key to a default file location in the user's home directory.
     * @return  this KeyCache
     * @throws IOException
     */
    public KeyCache store() throws IOException {
        Files.write(new File(CACHE).toPath(), key.getBytes(), 
            new StandardOpenOption[] { StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING });
        return this;
    }
    
    /**
     * Static load of this {@code KeyCache}'s key from file storage.
     * 
     * @param cache     the a new KeyCache with an empty key
     * @return  an instance of KeyCache
     * @throws IOException
     */
    private static String load() throws IOException {
        if(key == null) {
            File keyFile = new File(CACHE);
            if(keyFile.exists()) {
                key = new String(Files.readAllBytes(new File(CACHE).toPath()));
            }
        }
        
        return key;
    }
    
    public static String getKey() {
        try { 
            key = key == null ? load() : key;
            if(key != null) {
                new KeyCache(key);
            }
        }catch(Exception e) { e.printStackTrace(); }
        
        return key;
    }
}
