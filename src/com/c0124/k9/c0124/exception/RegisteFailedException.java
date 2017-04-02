package com.c0124.k9.c0124.exception;

import com.c0124.RegisteResultCodeEnum;

public class RegisteFailedException extends SCPGPException {

    private static final long serialVersionUID = 1006721065270752063L;
    public final RegisteResultCodeEnum code;
    
    public RegisteFailedException(String message, RegisteResultCodeEnum code) {
        super(message);
        this.code = code;
    }

}
