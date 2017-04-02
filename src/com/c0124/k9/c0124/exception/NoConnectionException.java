package com.c0124.k9.c0124.exception;


public class NoConnectionException extends SCPGPException {
    private static final long serialVersionUID = 8375987542074297632L;
    public final long timestamp;

    public NoConnectionException(String message, long timestamp) {
        super(message);
        this.timestamp = timestamp;
    }
}
