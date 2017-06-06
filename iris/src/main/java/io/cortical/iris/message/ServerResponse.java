package io.cortical.iris.message;

import java.util.List;

import io.cortical.iris.view.ViewType;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.model.Context;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Metric;
import io.cortical.retina.model.Term;
import io.cortical.retina.model.Text;

public class ServerResponse extends Payload {
    protected List<Term> terms;
    protected Fingerprint fingerPrint;
    protected List<Context> contexts;
    protected List<String> keywords;
    protected List<String> tokens;
    protected List<String> sentences;
    protected List<Text> slices;
    protected Metric metric;
    protected ViewType inputType;
    protected ServerRequest request;
    protected FullClient client;
    
    protected boolean wasCached;
    
    
    /**
     * Constructs a new {@code ServerResponse}
     * @param request
     * @param type
     * @param terms
     * @param fingerPrint
     * @param contexts
     * @param keywords
     * @param tokens
     * @param slices
     * @param sentences
     */
    public ServerResponse(ServerRequest request, ViewType type, List<Term> terms, Fingerprint fingerPrint, List<Context> contexts, 
        List<String> keywords, List<String> tokens, List<Text> slices, List<String> sentences, Metric metric) {
        super();
        this.request = request;
        this.inputType = type;
        this.terms = terms;
        this.fingerPrint = fingerPrint;
        this.contexts = contexts;
        this.keywords = keywords;
        this.tokens = tokens;
        this.slices = slices;
        this.sentences = sentences;
        this.metric = metric;
    }
    
    /**
     * Limited access constructor used by the {@link CompareResponse}.
     */
    protected ServerResponse() {}
    
    /**
     * Sets a flag indicating whether the response was from cache
     * @param b true if response was from cache, false if not.
     */
    public void setCached(boolean b) {
        this.wasCached = b;
    }
    
    /**
     * Returns the flag indicating whether the response was from cache
     * @return
     */
    public boolean wasCached() {
        return wasCached;
    }
    
    /**
     * @return the request
     */
    public ServerRequest getRequest() {
        return request;
    }

    
    /**
     * @param request the request to set
     */
    public void setRequest(ServerRequest request) {
        this.request = request;
    }
    
    /**
     * Sets the language detection result
     * @return
     */
    public FullClient getDetectedClient() {
        return client;
    }
    
    /**
     * Sets the language detection result
     * @param f
     */
    public void setDetectedClient(FullClient f) {
        this.client = f;
    }

    /**
     * @return the terms
     */
    public List<Term> getTerms() {
        return terms;
    }
    
    /**
     * @param terms the terms to set
     */
    public void setTerms(List<Term> terms) {
        this.terms = terms;
    }
    
    /**
     * @return the fingerPrint
     */
    public Fingerprint getFingerPrint() {
        return fingerPrint;
    }
    
    /**
     * @param fingerPrint the fingerPrint to set
     */
    public void setFingerPrint(Fingerprint fingerPrint) {
        this.fingerPrint = fingerPrint;
    }
    
    /**
     * @return the contexts
     */
    public List<Context> getContexts() {
        return contexts;
    }
    
    /**
     * @param contexts the contexts to set
     */
    public void setContexts(List<Context> contexts) {
        this.contexts = contexts;
    }

    
    /**
     * @return the keywords
     */
    public List<String> getKeywords() {
        return keywords;
    }

    
    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    
    /**
     * @return the tokens
     */
    public List<String> getTokens() {
        return tokens;
    }

    
    /**
     * @param tokens the tokens to set
     */
    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    
    /**
     * @return the slices
     */
    public List<Text> getSlices() {
        return slices;
    }

    
    /**
     * @param slices the slices to set
     */
    public void setSlices(List<Text> slices) {
        this.slices = slices;
    }
    
    /**
     * Returns the tokenized full sentences
     * @return
     */
    public List<String> getSentences() {
        return sentences;
    }
    
    /**
     * Sets the tokenized string that represents full sentences.
     * @param sents
     */
    public void setSentences(List<String> sents) {
        this.sentences = sents;
    }
    
    /**
     * Returns the {@link Metric} retrieved from the last compare call
     * @return the Metric from a comparison if made
     */
    public Metric getMetric() {
        return metric;
    }
    
    /**
     * Sets the current Metric object from compare results
     * @param metric    the results of the last compare
     */
    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    /**
     * @return the inputType
     */
    public ViewType getInputType() {
        return inputType;
    }

    
    /**
     * @param inputType the inputType to set
     */
    public void setInputType(ViewType inputType) {
        this.inputType = inputType;
    }
}
