package com.unimagdalena.conectaCiudad.exceptions;

import java.util.Map;

import com.unimagdalena.conectaCiudad.enums.ErrorCode;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException {
    
    private final String errorCode = "DUPLICATE_RESOURCE";
    private final String resource;
    private final String field;
    private final Object value;

    public DuplicateResourceException(String resource, String field, Object value) {
        super(String.format("%s already exists with %s: '%s'", resource, field, value));
        this.resource = resource;
        this.field = field;
        this.value = value;
    }

    @Deprecated
    public DuplicateResourceException(String message) {
        super(message);
        this.resource = "Unknown";
        this.field = "unknown";
        this.value = null;
    }
    
    public DuplicateResourceException(ErrorCode voteAlreadyExists, Map<String,Long> of) {
        super(voteAlreadyExists.name());
        this.resource = voteAlreadyExists.name();
        this.field = "id";
        this.value = of.get("id");
    }

    public String getErrorCode() { 
        return errorCode; 
    }
    
    public String getResource() { 
        return resource; 
    }
    
    public String getField() { 
        return field; 
    }
    
    public Object getValue() { 
        return value; 
    }
}
