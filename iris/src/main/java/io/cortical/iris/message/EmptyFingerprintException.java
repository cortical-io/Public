package io.cortical.iris.message;

import io.cortical.retina.rest.ApiException;


/**
 * Thrown when for example, an expression resolves to an empty fingerprint
 * such as "Apple ! Apple".
 */
public class EmptyFingerprintException extends ApiException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code EmptyFingerprintException}
     * @param message
     */
    public EmptyFingerprintException(String message) {
        super(200, message);
    }
}
