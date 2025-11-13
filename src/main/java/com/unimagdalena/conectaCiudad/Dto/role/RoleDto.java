package com.unimagdalena.conectaCiudad.Dto.role;

import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.permission.PermissionDto;

public record RoleDto(
    Long id,
    String name,
    List<PermissionDto> permissions
) {}
