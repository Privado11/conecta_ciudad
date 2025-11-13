package com.unimagdalena.conectaCiudad.Dto.permission;

public record PermissionDto(
    Long id,
    String code,
    String description,
    boolean isCritical
) {}
