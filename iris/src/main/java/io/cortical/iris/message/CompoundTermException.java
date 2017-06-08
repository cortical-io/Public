package io.cortical.iris.message;

import io.cortical.retina.rest.ApiException;

public class CompoundTermException extends ApiException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public CompoundTermException(int errorcode, String message) {
        super(errorcode, message);
    }
}
