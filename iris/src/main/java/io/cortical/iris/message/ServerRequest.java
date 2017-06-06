package io.cortical.iris.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.cortical.iris.ExtendedClient;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.window.OutputWindow;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.core.PosTag;
import io.cortical.retina.core.PosType;
import io.cortical.retina.model.Model;
import rx.Observable;

public class ServerRequest extends Payload {
    private int simTermsStartIndex = 0;
    private int simTermsMaxResults = 10;
    private int contextsStartIndex = 0;
    private int contextsMaxResults = 10;
    private int slicesStartIndex = 0;
    private int slicesMaxResults = 10;
    private int contextID = -1;
  
    private PosType partOfSpeech = PosType.ANY;
    private Set<PosTag> posTags = Collections.emptySet();
    private int position;
    private String termString;
    private boolean doContextLookup;
    
    private double sparsity = 0.02;
    private String text;
    private List<Integer> contextIDs = new ArrayList<>();
    
    protected Model model;
    protected Model secondaryModel;
    protected UUID inputWindowID1;
    protected UUID inputWindowID2;
    
    protected ViewType inputViewType;
    
    protected OutputWindow outputWindow;
    
    protected MergeOrPassthruValve<ServerResponse, Observable<CompareResponse>, ServerResponse> mergeValve;
    
    protected FullClient retinaClient;
    protected ExtendedClient extendedClient;
    protected String retinaLanguage;
    
      
   /**
     * Used to insert a blank comparison in the server request queue as the 
     * last query after a reset, so that re-execution of a new query containing
     * the same parameters will still execute due to its not comparing as the 
     * same with the BLANK inserted as the last query.
     */
    public static final ServerRequest BLANK = new ServerRequest(
        0, 10, -1, PosType.ANY, null, null, 0.02, "BLANK");
    
    
    
    /**
     * Constructs a new {@code ServerRequest}
     */
    public ServerRequest() {}
    
    /**
     * Constructs a new {@code ServerRequest}
     * 
     * @param startIndex                the start index of the results
     * @param maxResults                the maximum number of results to return
     * @param contextID                 the (optional) context id to constrain the results to.
     * @param partOfSpeech              the (optional) part of speech to constrain the results to.
     * @param endpointIndicator         a {@link ViewType} which tells us what kind of query to make.
     * @param model                     the message body
     * @param sparsity                  the sparsity value
     * @param text                      the text
     */
    public ServerRequest(int startIndex, int maxResults, int contextID, PosType partOfSpeech, ViewType endpointIndicator, Model model, double sparsity, String text) {
        super();
        
        this.simTermsStartIndex = startIndex;
        this.simTermsMaxResults = maxResults;
        this.contextsStartIndex = startIndex;
        this.contextsMaxResults = maxResults;
        this.slicesStartIndex = startIndex;
        this.slicesMaxResults = maxResults;
        this.contextID = contextID;
        this.partOfSpeech = partOfSpeech;
        this.inputViewType = endpointIndicator;
        this.model = model;
        this.sparsity = sparsity;
        this.text = text;
    }
    
    /**
     * Sets the {@link FullClient} used to invoke the 
     * server query.
     * 
     * @param client    the {@link FullClient} to use
     */
    public void setRetinaClient(FullClient client) {
        this.retinaClient = client;
    }
    
    FullClient compareClient;
    public void setCompareClient(FullClient client) {
        this.compareClient = client;
    }
    
    public FullClient getCompareClient() {
        return compareClient;
    }
    
    /**
     * Returns the {@link FullClient} which will be used to
     * invoke the server query.
     * 
     * @return  the {@code FullClient}
     */
    public FullClient getRetinaClient() {
        return retinaClient;
    }
    
    /**
     * Sets the {@link ExtendedClient} used to invoke special
     * queries.
     * 
     * @param client    the {@code ExtendedClient}
     */
    public void setExtendedClient(ExtendedClient client) {
        this.extendedClient = client;
    }
    
    /**
     * Returns the {@link ExtendedClient} used to invoke special
     * queries.
     * 
     * @return  the {@code ExtendedClient}
     */
    public ExtendedClient getExtendedClient() {
        return extendedClient;
    }
    
    /**
     * Sets the name used to represent the Retina language.
     * 
     * @param lang      the string name of the Retina
     */
    public void setRetinaLanguage(String lang) {
        this.retinaLanguage = lang;
    }
    
    /**
     * Returns the name used to represent the Retina language.
     * @return
     */
    public String getRetinaLanguage() {
        return retinaLanguage;
    }
    
    /**
     * Sets the relevant {@link OutputWindow}'s merge valve on this request.
     * @param mergeValve
     */
    public void setMergeValve(MergeOrPassthruValve<ServerResponse, Observable<CompareResponse>, ServerResponse> mergeValve) {
        this.mergeValve = mergeValve;
    }
   
    /**
     * Returns the relevant {@link OutputWindow}'s merge valve.
     * @return
     */
    public MergeOrPassthruValve<ServerResponse, Observable<CompareResponse>, ServerResponse> getMergeValve() {
        return mergeValve;
    }
    
    /**
     * Returns the OutputWindow
     * @return
     */
    public OutputWindow getOutputWindow() {
        return outputWindow;
    }
    
    /**
     * Sets the OutputWindow
     * @param w
     */
    public void setOutputWindow(OutputWindow w) {
        this.outputWindow = w;
    }
    
    /**
     * Returns the String to lookup
     * @return
     */
    public String getTermString() {
        return termString;
    }
    
    /**
     * Sets the term String to lookup
     * @param term
     */
    public void setTermString(String term) {
        this.termString = term;
    }
    
    /**
     * Returns the list of Context ids.
     * @return
     */
    public List<Integer> getContexts() {
        return contextIDs;
    }
    
    /**
     * Sets the list of Context ids.
     * @param l
     */
    public void setContexts(List<Integer> l) {
        this.contextIDs = l;
    }
    
    /**
     * Returns the flag which stores whether a context search 
     * should be performed.
     * @return
     */
    public boolean lookupContexts() {
        return doContextLookup;
    }
    
    /**
     * Sets the flag which stores whether a context search 
     * should be performed. 
     * @param b
     */
    public void setLookupContexts(boolean b) {
        this.doContextLookup = b;
    }

    /**
     * @return the startIndex
     */
    public int getSimTermsStartIndex() {
        return simTermsStartIndex;
    }
    
    /**
     * @param startIndex the startIndex to set
     */
    public void setSimTermsStartIndex(int startIndex) {
        this.simTermsStartIndex = startIndex;
    }
    
    /**
     * @return the maxResults
     */
    public int getSimTermsMaxResults() {
        return simTermsMaxResults;
    }
    
    /**
     * @param maxResults the maxResults to set
     */
    public void setSimTermsMaxResults(int maxResults) {
        this.simTermsMaxResults = maxResults;
    }
    
    /**
     * @return the startIndex
     */
    public int getContextsStartIndex() {
        return contextsStartIndex;
    }
    
    /**
     * @param startIndex the startIndex to set
     */
    public void setContextsStartIndex(int startIndex) {
        this.contextsStartIndex = startIndex;
    }
    
    /**
     * @return the maxResults
     */
    public int getContextsMaxResults() {
        return contextsMaxResults;
    }
    
    /**
     * @param maxResults the maxResults to set
     */
    public void setContextsMaxResults(int maxResults) {
        this.contextsMaxResults = maxResults;
    }
    
    /**
     * @return the startIndex
     */
    public int getSlicesStartIndex() {
        return slicesStartIndex;
    }
    
    /**
     * @param startIndex the startIndex to set
     */
    public void setSlicesStartIndex(int startIndex) {
        this.slicesStartIndex = startIndex;
    }
    
    /**
     * @return the maxResults
     */
    public int getSlicesMaxResults() {
        return slicesMaxResults;
    }
    
    /**
     * @param maxResults the maxResults to set
     */
    public void setSlicesMaxResults(int maxResults) {
        this.slicesMaxResults = maxResults;
    }
    
    /**
     * @return the contextID
     */
    public int getContextID() {
        return contextID;
    }
    
    /**
     * @param contextID the contextID to set
     */
    public void setContextID(int contextID) {
        this.contextID = contextID;
    }
    
    /**
     * @return the partOfSpeech
     */
    public PosType getPartOfSpeech() {
        return partOfSpeech;
    }
    
    /**
     * @param partOfSpeech the partOfSpeech to set
     */
    public void setPartOfSpeech(PosType partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }
    
    /**
     * @return the input {@link ViewType}
     */
    public ViewType getInputViewType() {
        return inputViewType;
    }
    
    /**
     * @param endpointIndicator the input {@link ViewType} to set
     */
    public void setInputViewType(ViewType inputViewType) {
        this.inputViewType = inputViewType;
    }

    /**
     * @return the model
     */
    public Model getModel() {
        return model;
    }

    /**
     * @param model the {@link Model} to set
     */
    public void setModel(Model model) {
        this.model = model;
    }

    
    /**
     * @return the sparsity
     */
    public double getSparsity() {
        return sparsity;
    }

    
    /**
     * @param sparsity the sparsity to set
     */
    public void setSparsity(double sparsity) {
        this.sparsity = sparsity;
    }

    
    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    
    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Sets the {@link Set} of {@link PosTag}s to be used
     * to filter the query.
     * @param tags
     */
    public void setPosTags(Set<PosTag> tags) {
        this.posTags = tags;
    }
    
    /**
     * Returns the {@link Set} of {@link PosTags} used to
     * filter the query.
     * @return
     */
    public Set<PosTag> getPosTags() {
        return posTags;
    }
    
    /**
     * Sets the position for the tooltip lookup
     * @param position
     */
    public void setPosition(int position) {
        this.position = position;
    }
    
    /**
     * Returns the position used for the tooltip lookup
     * @return
     */
    public int getPosition() {
        return position;
    }
    
    /**
     * Sets the {@link Model} containing the secondary input
     * @param model
     */
    public void setSecondaryModel(Model model) {
        this.secondaryModel = model;
    }
    
    /**
     * Returns the {@link Model} containing the secondary input
     * @return
     */
    public Model getSecondaryModel() {
        return secondaryModel;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + contextID;
        result = prime * result + ((outputWindow == null) ? 0 : outputWindow.getWindowID().hashCode());
        result = prime * result + ((termString == null) ? 0 : termString.hashCode());
        result = prime * result + ((inputViewType == null) ? 0 : inputViewType.hashCode());
        result = prime * result + simTermsMaxResults;
        result = prime * result + contextsMaxResults;
        result = prime * result + slicesMaxResults;
        result = prime * result + ((payload == null) ? 0 : payload.hashCode());
        result = prime * result + ((partOfSpeech == null) ? 0 : partOfSpeech.hashCode());
        result = prime * result + ((posTags == null) ? 0 : posTags.hashCode());
        long temp;
        temp = Double.doubleToLongBits(sparsity);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + simTermsStartIndex;
        result = prime * result + contextsStartIndex;
        result = prime * result + slicesStartIndex;
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + ((secondaryModel == null) ? 0 : secondaryModel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServerRequest other = (ServerRequest) obj;
        if (contextID != other.contextID)
            return false;
        if (inputViewType != other.inputViewType)
            return false;
        if (simTermsMaxResults != other.simTermsMaxResults)
            return false;
        if (contextsMaxResults != other.contextsMaxResults)
            return false;
        if (slicesMaxResults != other.slicesMaxResults)
            return false;
        if (payload == null) {
            if (other.payload != null)
                return false;
        } else if (!payload.equals(other.payload))
            return false;
        if (partOfSpeech != other.partOfSpeech)
            return false;
        if (outputWindow == null) {
            if (other.outputWindow != null)
                return false;
        } else if (!outputWindow.getWindowID().equals(other.outputWindow.getWindowID()))
            return false;
        if (posTags == null) {
            if (other.posTags != null)
                return false;
        } else if (!posTags.equals(other.posTags))
            return false;
        if (Double.doubleToLongBits(sparsity) != Double.doubleToLongBits(other.sparsity))
            return false;
        if (simTermsStartIndex != other.simTermsStartIndex)
            return false;
        if (contextsStartIndex != other.contextsStartIndex)
            return false;
        if (slicesStartIndex != other.slicesStartIndex)
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        if (termString == null) {
            if (other.termString != null)
                return false;
        } else if (!termString.equals(other.termString))
            return false;
        
        try {
            if(secondaryModel == null) {
                if(other.secondaryModel != null)
                    return false;
            } else if (!secondaryModel.toJson().equals(other.secondaryModel.toJson()))
                return false;
        }catch(Exception e) {
            return false;
        }
        return true;
    }

    
}
