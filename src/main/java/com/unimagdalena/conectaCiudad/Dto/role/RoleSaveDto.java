package com.unimagdalena.conectaCiudad.Dto.role;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record RoleSaveDto(
    @NotBlank(message = "El nombre del rol es obligatorio")
    String name,

    @NotEmpty(message = "Debe asignar al menos un permiso al rol")
    List<String> permissions 
) {}
