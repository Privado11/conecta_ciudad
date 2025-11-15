package com.unimagdalena.conectaCiudad.Dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.review.ReviewDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

public record ProjectDto(
    Long id,
    String name,
    String objectives,
    String beneficiaryPopulations,
    BigDecimal budget,
    LocalDate startAt,
    LocalDate endAt,
    LocalDate votingStartAt,
    LocalDate votingEndAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    ProjectStatus status,
    UserDto creator,
    Long version,
    List<ReviewDto> reviews
) {}
