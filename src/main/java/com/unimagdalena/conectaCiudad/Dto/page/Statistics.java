package com.unimagdalena.conectaCiudad.Dto.page;

import java.util.Map;


public record Statistics<T>(
    long total,
    Map<String, Object> metrics 
) {}
