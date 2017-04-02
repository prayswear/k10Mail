package com.c0124.k9.c0124.exception;

public class TokenNotFound extends SCPGPException {

    public TokenNotFound(String message, String email) {
        super(message);
        this.email = email;
    }


    public String getEmail() {
        return email;
    }


    private static final long serialVersionUID = -4867854132771144525L;
    private final String email; 

}
