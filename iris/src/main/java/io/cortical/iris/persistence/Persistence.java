package io.cortical.iris.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.nustaq.serialization.FSTConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Persistence {
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(Persistence.class);
    
    private static final Persistence INSTANCE = new Persistence();
    
    /** Use of Fast Serialize https://github.com/RuedigerMoeller/fast-serialization */
    private transient FSTConfiguration fastSerialConfig = FSTConfiguration.createDefaultConfiguration();
    
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private Lock writeMonitor = rwl.writeLock();
    private Lock readMonitor = rwl.readLock();
    
    
    /**
     * Returns the final singleton instance of this {@code Persistence} class.
     * @return
     */
    public static Persistence get() {
        return INSTANCE;
    }
    
    /**
     * Main public interface method for persisting IRIS entities.
     * 
     * @param config
     * @param instance
     * @return
     */
    public <T extends Serializable> byte[] write(WindowConfig config, T instance) {
        LOGGER.info("Persistence write(T, " + config.getFileName() + ") called ...");
        
        byte[] bytes = serialize(instance);
        
        try {
            writeFile(config, bytes, config.getOpenOptions());
        }catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        return bytes;
    }
    
    /**
     * Writes the file contained in "bytes" to disk using the {@link WindowConfig} 
     * specified which in turn specifies file name and location specifics.
     *          
     * @param config            the SerialConfig to use for file name and location        
     * @param bytes             the bytes to write
     * @throws IOException      if there is a problem writing the file
     */
    void writeFile(WindowConfig config, byte[] bytes) throws IOException {
        writeFile(config, bytes, config.getOpenOptions());
    }
  
    /**
     * Writes the file specified by "fileName" using the pre-configured location 
     * specified by the {@link WindowConfig}.
     *   
     * @param config            the SerialConfig to use for file name and location  
     * @param bytes             the content to write
     * @param options           the file handling rules to use
     * @throws IOException      if there is a problem writing the file
     */
    void writeFile(WindowConfig config, byte[] bytes, StandardOpenOption... options) throws IOException {
        try {
            //writeMonitor.lock();
            Path path = ensurePathExists(config, config.getFileName()).toPath();
            Files.write(path, bytes, options);
        } catch(Exception e) {
           throw e;
        } finally {
            //writeMonitor.unlock();
        }
    }
    
    /**
     * Reifies a {@link Persistable} from the specified file in the location
     * configured by the {@link WindowConfig} passed in at construction time, using
     * the file name specified.
     * 
     * @param fileName  the name of the file from which to get the serialized object.
     * @return  the reified type &lt;R&gt;
     */
    public <R extends Serializable> R read(WindowConfig config) {
        LOGGER.debug("Persistence reify(" + config.getFileName() + ") called ...");
        
        R retVal;
        try {
            byte[] bytes = readFile(config, config.getFileName());
            System.out.println("bytes len = " + bytes.length);
            retVal = deSerialize(bytes);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return retVal;
    }
    
    /**
     * Reads the file located at the path specified and returns the content
     * in the form of a byte array.
     * 
     * @param config            the {@link WindowConfig}
     * @param filePath          the fully qualified file path
     * 
     * @return a byte array containing the object
     * @throws IOException  if there is a problem reading the file
     */
    byte[] readFile(WindowConfig config, String filePath) throws IOException {
        Path path = testFileExists(config, filePath).toPath();
        return readFile(path);
    }
    
    /**
     * Reads the file located at the path specified and returns the content
     * in the form of a byte array.
     * 
     * @param path          the {@link Path} object
     * @return  a byte array containing the object
     * @throws IOException  if there is a problem reading the file
     */
    byte[] readFile(Path path) throws IOException {
        
        byte[] bytes = null;
        try {
            //readMonitor.lock();
            bytes = Files.readAllBytes(path);
        } finally {
            //readMonitor.unlock();
        }
        
        return bytes;
    }

    /**
     * <p>
     * For Writing:</p>
     * Creates the serialization file.
     * 
     * @param config            the {@link SC} specifying the file storage file.\
     * @param fileName          the name of the file to retrieve
     * 
     * @return the File object
     * @throws IOException      if there is a problem locating the specified directories or 
     *                          creating the file.
     */
    File ensurePathExists(WindowConfig config, String fileName) throws IOException {
        File serializedFile = null;
    
        try {
            writeMonitor.lock();
            
            String path = System.getProperty("user.home") + File.separator + config.getFileDir();
            File customDir = new File(path);
            // Make sure container directory exists
            customDir.mkdirs();

            // Check to make sure the fileName doesn't already include the full path.
            serializedFile = new File(fileName.indexOf(customDir.getAbsolutePath()) != -1 ?
                fileName : customDir.getAbsolutePath() + File.separator +  fileName);
            if(!serializedFile.exists()) {
                serializedFile.createNewFile();
            }
        }catch(Exception io) {
            throw io;
        }finally{
            writeMonitor.unlock();
        }
        
        return serializedFile;
    }
    
    /**
     * <p>
     * For Reading:</p>
     * Returns the File corresponding to "fileName" if this framework is successful in locating
     * the specified file, otherwise it throws an {@link IOException}.
     * 
     * @param fileName          the name of the file to search for.
     * @return  the File if the operation is successful, otherwise an exception is thrown
     * @throws IOException      if the specified file is not found, or there's a problem loading it.
     */
    File testFileExists(WindowConfig config, String fileName) throws IOException, FileNotFoundException {
        try {
            readMonitor.lock();
            
            String path = System.getProperty("user.home") + File.separator + config.getFileDir();
            File customDir = new File(path);
            // Make sure container directory exists
            customDir.mkdirs();
            
            File serializedFile = new File(fileName.indexOf(customDir.getAbsolutePath()) != -1 ?
                fileName : customDir.getAbsolutePath() + File.separator +  fileName);
            if(!serializedFile.exists()) {
                throw new FileNotFoundException("File \"" + fileName + "\" was not found.");
            }
            
            return serializedFile;
        }catch(IOException io) {
            throw io;
        }finally{
            readMonitor.unlock();
        }
    }
    
    /**
     * Serializes the specified {@link Persistable} to a byte array
     * then returns it.
     * @param instance  the instance of Persistable to serialize
     * @return  the byte array
     */
    public <T extends Serializable> byte[] serialize(T instance) {
        byte[] bytes = null;
        try {
            bytes = fastSerialConfig.asByteArray(instance);
        } catch(Exception e) {
            bytes = null;
            throw new RuntimeException(e);
        }
        
        return bytes;
    }
    
    /**
     * Deserializes the specified Class type from the specified byte array
     * 
     * @param bytes     the byte array containing the object to be deserialized
     * @return  the deserialized object of type &lt;T&gt;
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T deSerialize(byte[] bytes) {
        return  (T)fastSerialConfig.asObject(bytes);
    }
}
