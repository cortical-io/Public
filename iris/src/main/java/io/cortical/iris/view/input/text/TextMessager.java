package io.cortical.iris.view.input.text;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.cortical.retina.model.Model;
import io.cortical.retina.model.Term;
import io.cortical.retina.model.Text;


public class TextMessager {
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
        }
        return new Text(s);
    }
    
    /**
     * Creates the Json message capable of being sent to the server.
     * @param text      the text to transform into json
     * @return          the json Model
     * @throws JsonProcessingException  if there is a problem parsing the text
     */
    public Model createMessage(String text) throws JsonProcessingException {
        return getTermOrText(text);
    }
}
