package com.unimagdalena.conectaCiudad.Dto.project;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectSaveDto(

    @NotBlank(message = "Project name is required")
    @Size(max = 50, message = "Project name must not exceed 50 characters")
    String name,

    @NotBlank(message = "Objectives are required")
    @Size(max = 150, message = "Objectives must not exceed 150 characters")
    String objectives,

    @NotBlank(message = "Beneficiary populations are required")
    @Size(max = 250, message = "Beneficiary populations must not exceed 250 characters")
    String beneficiaryPopulations,

    @NotBlank(message = "Budgets information is required")
    @Size(max = 150, message = "Budgets must not exceed 150 characters")
    String budgets,

    @NotNull(message = "Start date is required")
    LocalDateTime startAt,

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    LocalDateTime endAt

) {}
