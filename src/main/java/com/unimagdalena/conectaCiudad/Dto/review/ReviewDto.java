package com.unimagdalena.conectaCiudad.Dto.review;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import java.time.OffsetDateTime;

public record ReviewDto(
    Long id,
    UserDto curator,
    String notes,
    OffsetDateTime startAt,
    OffsetDateTime dueAt,
    OffsetDateTime reviewedAt
) {}
