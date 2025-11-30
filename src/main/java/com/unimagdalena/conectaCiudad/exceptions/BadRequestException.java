package com.unimagdalena.conectaCiudad.exceptions;

import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class BadRequestException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final Map<String, Object> parameters;
    
    public BadRequestException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.parameters = new HashMap<>();
    }
    
    public BadRequestException(ErrorCode errorCode, Map<String, Object> parameters) {
        super(errorCode.getCode() + " - " + parameters);
        this.errorCode = errorCode;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }
    
    @Deprecated
    public BadRequestException(String message) {
        super(message);
        this.errorCode = ErrorCode.UNKNOWN_ERROR;
        this.parameters = new HashMap<>();
    }
    
    public BadRequestException(ErrorCode errorCode, String logMessage, Map<String, Object> parameters) {
        super(logMessage);
        this.errorCode = errorCode;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }
    
    public String getCode() {
        return errorCode != null ? errorCode.getCode() : ErrorCode.UNKNOWN_ERROR.getCode();
    }
}

