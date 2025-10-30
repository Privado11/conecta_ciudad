package com.unimagdalena.conectaCiudad.Dto.project;

import jakarta.validation.constraints.NotBlank;

public record ReviewNotesDto(
    @NotBlank(message = "Notes are required")
    String notes
) {}


