package io.cortical.util;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.NopIndenter;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class ConfigurableIndentor extends NopIndenter {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final static String SYS_LF;
    static {
        String lf = null;
        try {
            lf = System.getProperty("line.separator");
        } catch (Throwable t) { } // access exception?
        SYS_LF = (lf == null) ? "\n" : lf;
    }

    final static int SPACE_COUNT = 64;
    final static char[] SPACES = new char[SPACE_COUNT];
    static {
        Arrays.fill(SPACES, ' ');
    }
    
    public static int DEFAULT_SPACES = 4;

    /**
     * Linefeed used; default value is the platform-specific linefeed.
     */
    protected final String _lf;
    
    protected int _numSpaces = DEFAULT_SPACES;
    
    protected IntegerProperty indentationProperty = new SimpleIntegerProperty(_numSpaces);
    
    
    
    /**
     * Constructs a new {@code ConfigurableIndentor}
     * @param numSpaces
     */
    public ConfigurableIndentor(int numSpaces) { this(SYS_LF, numSpaces); }
    
    /**
     * @since 2.3
     */
    public ConfigurableIndentor(String lf, int numSpaces) {
        _lf = lf;
        _numSpaces = numSpaces;
        indentationProperty.addListener((v,o,n) -> {
            _numSpaces = n.intValue();
        });
    }

    /**
     * "Mutant factory" method that will return an instance that uses
     * specified String as linefeed.
     * 
     * @since 2.3
     */
    public ConfigurableIndentor withLinefeed(String lf)
    {
        if (lf.equals(_lf)) {
            return this;
        }
        return new ConfigurableIndentor(lf, _numSpaces);
    }
    
    /**
     * Returns the property governing the indentation.
     * @return
     */
    public IntegerProperty indentationProperty() {
        return indentationProperty;
    }
    
    public static void setDefaultSpaces(int numSpaces) {
        DEFAULT_SPACES = numSpaces;
    }
    
    @Override
    public boolean isInline() { return false; }

    @Override
    public void writeIndentation(JsonGenerator jg, int level)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw(_lf);
        if (level > 0) { // should we err on negative values (as there's some flaw?)
            level *= _numSpaces; // 2 spaces per level
            while (level > SPACE_COUNT) { // should never happen but...
                jg.writeRaw(SPACES, 0, SPACE_COUNT); 
                level -= SPACES.length;
            }
            jg.writeRaw(SPACES, 0, level);
        }
    }
}
