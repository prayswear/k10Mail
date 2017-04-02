package com.c0124.k9.c0124.exception;

import com.c0124.PublishResultEnum;

public class PublishPublicKeyFailedException extends SCPGPException {

    public PublishPublicKeyFailedException(String message, PublishResultEnum code) {
        super(message);
        this.code = code;
    }

    public PublishResultEnum getCode() {
        return code;
    }

    private final PublishResultEnum code;
    private static final long serialVersionUID = 5674448924663014472L;

}
