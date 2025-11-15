package com.unimagdalena.conectaCiudad.Dto.project;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;

public record ProjectApprovalDto(
    @NotNull(message = "La fecha de inicio de votación es obligatoria")
    @Future(message = "La fecha de inicio de votación debe ser futura")
    LocalDate votingStartAt,
    
    @NotNull(message = "La fecha de fin de votación es obligatoria")
    @Future(message = "La fecha de fin de votación debe ser futura")
    LocalDate votingEndAt
) {}