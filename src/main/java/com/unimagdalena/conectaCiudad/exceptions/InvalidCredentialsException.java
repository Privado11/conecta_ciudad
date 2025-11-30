package com.unimagdalena.conectaCiudad.exceptions;

import lombok.Getter;

@Getter
public class InvalidCredentialsException extends RuntimeException {
    
    private final String errorCode = "INVALID_CREDENTIALS";
    
    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
