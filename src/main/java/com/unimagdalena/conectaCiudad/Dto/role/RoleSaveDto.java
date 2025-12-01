package com.unimagdalena.conectaCiudad.Dto.role;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record RoleSaveDto(
    @NotBlank
    String name,

    @NotEmpty
    List<String> permissions 
) {}
