package com.unimagdalena.conectaCiudad.Dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectSaveDto(

    @NotBlank
    @Size(max = 50)
    String name,

    @NotBlank
    String description,

    @NotBlank
    @Size(max = 150)
    String objectives,

    @NotBlank
    @Size(max = 250)
    String beneficiaryPopulations,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal budget,

    @NotNull
    LocalDate startAt,

    @NotNull
    @Future
    LocalDate endAt

) {}
