package com.unimagdalena.conectaCiudad.Dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectSaveDto(

    @NotBlank(message = "Project name is required")
    @Size(max = 50, message = "Project name must not exceed 50 characters")
    String name,

    @NotBlank(message = "Description is required")
    String description,

    @NotBlank(message = "Objectives are required")
    @Size(max = 150, message = "Objectives must not exceed 150 characters")
    String objectives,

    @NotBlank(message = "Beneficiary populations are required")
    @Size(max = 250, message = "Beneficiary populations must not exceed 250 characters")
    String beneficiaryPopulations,

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Budget must be greater than zero")
    BigDecimal budget,

    @NotNull(message = "Start date is required")
    LocalDate startAt,

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    LocalDate endAt

) {}
