package com.unimagdalena.conectaCiudad.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    
    private final String errorCode = "RESOURCE_NOT_FOUND";
    private final String resource;
    private final String field;
    private final Object value;

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", resource, field, value));
        this.resource = resource;
        this.field = field;
        this.value = value;
    }

    public String getErrorCode() { return errorCode; }
    public String getResource() { return resource; }
    public String getField() { return field; }
    public Object getValue() { return value; }
}

