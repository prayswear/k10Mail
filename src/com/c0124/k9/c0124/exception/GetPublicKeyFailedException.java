package com.c0124.k9.c0124.exception;


public class GetPublicKeyFailedException extends SCPGPException {
    private static final long serialVersionUID = 864141243739755680L;
    public final com.c0124.GetPublicKeyResultEnum errorCode; 
    public GetPublicKeyFailedException(String message, com.c0124.GetPublicKeyResultEnum errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
