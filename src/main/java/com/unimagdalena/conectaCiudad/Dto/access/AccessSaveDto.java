package com.unimagdalena.conectaCiudad.Dto.access;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for saving access log entries.
 * Records user login attempts and access information.
 */
public record AccessSaveDto(
    @NotNull
    Long userId,
    
    @NotBlank
    String ipAddress,
    
    String userAgent,
    String location,
    
    @NotNull
    Boolean success
) {}
