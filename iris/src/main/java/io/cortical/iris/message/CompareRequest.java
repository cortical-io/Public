package io.cortical.iris.message;

import java.util.UUID;

import io.cortical.iris.view.ViewType;
import io.cortical.retina.model.Model;

public class CompareRequest extends ServerRequest {
    private ViewType secondaryEndpointIndicator;
    
    /**
     * Sets the UUID of the primary input window
     * @param windowId
     */
    public void setPrimaryInputWindowID(UUID windowId) {
        this.inputWindowID1 = windowId;
    }
    
    /**
     * Returns the UUID of the primary input window
     * @return
     */
    public UUID getPrimaryInputWindowID() {
        return inputWindowID1;
    }
    
    /**
     * Sets the UUID of the secondary input window
     * @param windowId
     */
    public void setSecondaryInputWindowID(UUID windowId) {
        this.inputWindowID2 = windowId;
    }
    
    /**
     * Returns the UUID of the secondary input window
     * @return
     */
    public UUID getSecondaryInputWindowID() {
        return inputWindowID2;
    }
    
    /**
     * Sets the {@link ViewType} of the primary {@link InputWindow}'s view
     * which indicates the type of Model the input is.
     * @param type  a {@link ViewType}
     */
    public void setPrimaryEndpointIndicator(ViewType type) {
        this.inputViewType = type;
    }
    
    /**
     * Returns the {@link ViewType} of the {@link InputWindow}'s view
     * which indicates the type of Model the input is.
     * @return the view type of the primary input window
     */
    public ViewType getPrimaryEndpointIndicator() {
        return inputViewType;
    }
    

    /**
     * Sets the {@link ViewType} of the secondary {@link InputWindow}'s view
     * which indicates the type of Model the input is.
     * @param type  a {@link ViewType}
     */
    public void setSecondaryEndpointIndicator(ViewType type) {
        this.secondaryEndpointIndicator = type;
    }
    
    /**
     * Returns the {@link ViewType} of the {@link InputWindow}'s view
     * which indicates the type of Model the input is.
     * @return the view type of the primary input window
     */
    public ViewType getSecondaryEndpointIndicator() {
        return secondaryEndpointIndicator;
    }
    
    /**
     * Sets the primary {@link Model} object
     * @param model object which contains the primary input
     */
    public void setPrimaryModel(Model model) {
        this.model = model;
    }
    
    /**
     * Returns the primary {@link Model} object
     * @return model object which contains the primary input
     */
    public Model getPrimaryModel() {
        return this.model;
    }
    
    /**
     * Sets the secondary {@link Model} object
     * @param model object which contains the secondary input
     */
    public void setSecondaryModel(Model model) {
        this.secondaryModel = model;
    }
    
    /**
     * Returns the primary {@link Model} object
     * @return model object which contains the primary input
     */
    public Model getSecondaryModel() {
        return this.secondaryModel;
    }
}
