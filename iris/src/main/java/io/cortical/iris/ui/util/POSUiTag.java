package io.cortical.iris.ui.util;

import io.cortical.retina.core.PosTag;

public enum POSUiTag {
    JJ(PosTag.JJ, "Adjective"), CD(PosTag.CD, "Cardinal number"), FW(PosTag.FW, "Foreign word"), CW(PosTag.CW, "Connecting Words"), P(PosTag.P, "Particle"), 
    PUNCT(PosTag.PUNCT, "Punctuation"), RB(PosTag.RB, "Adverb"), NN(PosTag.NN, "Noun, singular or mass"), 
    NNS(PosTag.NNS, "Noun, plural"), NNP(PosTag.NNP, "Proper noun, singular"), NNPS(PosTag.NNPS, "Proper noun, plural"), 
    SYM(PosTag.SYM, "Symbol"), MD(PosTag.MD, "Modal"), VB(PosTag.VB, "Verb, base form"), LRB(PosTag.LRB, "Left|Right Bracket");
    
    private String description;
    private PosTag tag;
    
    private POSUiTag(PosTag tag, String description) {
        this.tag = tag;
        this.description = description;
    }
    
    /**
     * Returns the acronym used to represent this POS type
     * @return
     */
    public String acronym() {
        return this == LRB ? "-LRB-" : name();
    }
    
    /**
     * Returns the rest {@link PosTag} enum that corresponds
     * to this {@code POSUiTag}
     * @return
     */
    public PosTag tag() {
        return tag;
    }
    
    /**
     * Returns description for this tag
     * @return
     */
    public String description() {
        return description;
    }
}
