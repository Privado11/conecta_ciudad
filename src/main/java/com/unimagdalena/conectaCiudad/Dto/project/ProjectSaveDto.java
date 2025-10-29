package com.unimagdalena.conectaCiudad.Dto.project;

import java.time.LocalDateTime;


public record ProjectSaveDto(
    String name,
    String objectives,
    String beneficiaryPopulations,
    String budgets,
    LocalDateTime startAt,
    LocalDateTime endAt,
    Long creatorId
) {}
