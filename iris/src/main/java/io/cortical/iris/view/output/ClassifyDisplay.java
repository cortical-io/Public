package io.cortical.iris.view.output;

import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.view.View;
import io.cortical.iris.view.ViewType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;


public class ClassifyDisplay extends LabelledRadiusPane implements View {
    
    public final ObjectProperty<Payload> messageProperty = new SimpleObjectProperty<>();

    public ClassifyDisplay(String label, NewBG spec) {
        super(label, spec);
        
        setVisible(false);
        setUserData(ViewType.SIMILAR_TERMS);
        setManaged(false);
    }

    /**
     * Called on the previously selected {@link ViewType} when another {@link ViewType}
     * is selected to have focus input. This function is responsible for emptying all 
     * user input, and returning the {@code View} to its initial state.
     */
    @Override
    public void reset() {
        
    }
    
    /**
     * Returns the property which is updated with a new {@link Payload} upon
     * message model creation.
     * @return  the message property
     */
    @Override
    public ObjectProperty<Payload> messageProperty() {
        return messageProperty;
    }
    
    @Override
    public Region getOrCreateBackPanel() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Implemented by {@code View} subclasses to handle an error
     * 
     * @param	context		the error information container
     */
    @Override
    public void processRequestError(RequestErrorContext context) {}
}
