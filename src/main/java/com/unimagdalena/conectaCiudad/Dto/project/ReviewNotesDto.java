package com.unimagdalena.conectaCiudad.Dto.project;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for submitting review notes when approving or rejecting a project.
 */
public record ReviewNotesDto(
    @NotBlank
    String notes
) {}
