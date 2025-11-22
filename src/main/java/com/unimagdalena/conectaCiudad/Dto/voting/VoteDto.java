package com.unimagdalena.conectaCiudad.Dto.voting;

import java.time.LocalDateTime;
import java.util.List;

public record VoteDto(
        Long id,
        LocalDateTime fechaHora,
        Boolean decision,
        String hashVerificacion,
        Long proyectoId,
        Long ciudadanoId,
        List<Long> auditoriasId
) {}
