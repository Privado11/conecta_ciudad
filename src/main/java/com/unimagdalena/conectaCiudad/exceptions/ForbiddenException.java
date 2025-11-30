package com.unimagdalena.conectaCiudad.exceptions;

import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

@Getter
public class ForbiddenException extends RuntimeException {

    private final String code;
    private final Map<String, Object> parameters;

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.code = errorCode.getCode();
        this.parameters = Collections.emptyMap();
    }

    public ForbiddenException(ErrorCode errorCode, Map<String, Object> parameters) {
        super(errorCode.getCode());
        this.code = errorCode.getCode();
        this.parameters = parameters;
    }
}
