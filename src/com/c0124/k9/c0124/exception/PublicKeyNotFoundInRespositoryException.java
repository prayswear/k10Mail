package com.c0124.k9.c0124.exception;

import java.util.Set;


public class PublicKeyNotFoundInRespositoryException extends SCPGPException {
    private static final long serialVersionUID = 6550270277501117557L;
    public final Set<String> missingPublicKeyEmail;
    
    public PublicKeyNotFoundInRespositoryException(String message, Set<String> missingPublicKeyEmail) {
        super(message);
        this.missingPublicKeyEmail = missingPublicKeyEmail;
    }
}
