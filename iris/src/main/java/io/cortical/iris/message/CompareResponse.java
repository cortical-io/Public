package io.cortical.iris.message;

import io.cortical.iris.window.InputWindow;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Metric;
import io.cortical.retina.model.Model;

/**
 * A {@code CompareResponse} consists of the original or primary response and the
 * response from a comparison query - not two independent primary queries.
 * 
 * @author cogmission
 *
 */
public class CompareResponse extends ServerResponse {
    private ServerResponse primaryResponse;
    private ServerRequest secondaryRequest;
    private ServerResponse secondaryResponse;
    private ServerRequest primaryRequest;
    private CompareRequest compareRequest;
    
    /**
     * Created using the {@link ServerResponse} of the primary, and the {@code ServerResponse}
     * of the compare query.
     * 
     * @param primaryResponse
     * @param compareResponse
     */
    public CompareResponse(ServerRequest primaryRequest, CompareRequest compareRequest,
        ServerResponse primaryResponse, Fingerprint secondaryFingerprint, Metric metric) {
        
        this.primaryRequest = primaryRequest;
        this.compareRequest = compareRequest;
        this.primaryResponse = primaryResponse;
        this.metric = metric;
    }
    
    /**
     * Created using the {@link ServerResponse} of the primary, and the {@code ServerResponse}
     * of the compare query.
     * 
     * @param primaryResponse
     * @param compareResponse
     */
    public CompareResponse(ServerRequest primaryRequest, ServerRequest secondaryRequest, CompareRequest compareRequest,
        ServerResponse primaryResponse, ServerResponse secondaryResponse, Metric metric) {
        
        this.primaryRequest = primaryRequest;
        this.secondaryRequest = secondaryRequest;
        this.compareRequest = compareRequest;
        this.primaryResponse = primaryResponse;
        this.secondaryResponse = secondaryResponse;
        this.metric = metric;
    }
    
    /**
     * Returns the request made to retrieve the primary response using
     * the primary input.
     * @return  the primary request
     */
    public ServerRequest getPrimaryRequest() {
        return primaryRequest;
    }
    
    /**
     * Sets the primary request object.
     * @param request
     */
    public void setPrimaryRequest(ServerRequest request) {
        this.primaryRequest = request;
    }
    
    /**
     * Returns the request made to retrieve the secondary.
     * @return  the primary request
     */
    public ServerRequest getSecondaryRequest() {
        return secondaryRequest;
    }
    
    /**
     * Sets the secondary request object.
     * @param request
     */
    public void setSecondaryRequest(ServerRequest request) {
        this.secondaryRequest = request;
    }
    
    /**
     * Returns the request made to retrieve the secondary (compare) response.
     * 
     * @return  the primary request
     */
    public CompareRequest getCompareRequest() {
        return compareRequest;
    }
    
    /**
     * Sets the secondary (compare) request object.
     * @param request
     */
    public void setCompareRequest(CompareRequest request) {
        this.primaryRequest = request;
    }
    
    /**
     * Returns the [@link ServerResponse} from the primary query.
     * @return
     */
    public ServerResponse getPrimaryResponse() {
        return primaryResponse;
    }
    
    /**
     * Sets the primary response if it has to be set after this {@code CompareResponse}
     * is composed.
     * 
     * @param response
     */
    public void setPrimaryResponse(ServerResponse response) {
        this.primaryResponse = response;
    }
    
    /**
     * Returns the [@link ServerResponse} from the primary query.
     * @return
     */
    public ServerResponse getSecondaryResponse() {
        return secondaryResponse;
    }
    
    /**
     * Sets the primary response if it has to be set after this {@code CompareResponse}
     * is composed.
     * 
     * @param response
     */
    public void setSecondaryResponse(ServerResponse response) {
        this.secondaryResponse = response;
    }
    
    /**
     * Returns the {@link Fingerprint} from the primary query.
     * @return
     */
    public Fingerprint getPrimaryFingerprint() {
        return primaryResponse.getFingerPrint();
    }
    
    /**
     * Returns the {@link Fingerprint} of the secondary model.
     * @return
     */
    public Fingerprint getSecondaryFingerprint() {
        return secondaryResponse.getFingerPrint();
    }
    
    /**
     * Returns the {@link Model} for the primary input.
     * @return  the Model object containing the primary {@link InputWindow}'s input.
     */
    public Model getPrimaryModel() {
        return compareRequest.getPrimaryModel();
    }
    
    /**
     * Returns the secondary {@link Model}.
     * @return
     */
    public Model getSecondaryModel() {
        return compareRequest.getSecondaryModel();
    }
}
