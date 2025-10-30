package com.unimagdalena.conectaCiudad.Dto.project;

import java.time.LocalDateTime;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

public record ProjectDto(
    Long id,
    String name,
    String objectives,
    String beneficiaryPopulations,
    String budgets,
    LocalDateTime startAt,
    LocalDateTime endAt,
    ProjectStatus status,
    UserDto creator,
    UserDto curator,
    String reviewNotes,
    LocalDateTime reviewDueAt,
    LocalDateTime reviewedAt
) {}
