package com.unimagdalena.conectaCiudad.Dto.user;

import java.time.OffsetDateTime;
import java.util.List;

public record UserDto(
    Long id,
    String name,
    String nationalId,
    String email,
    String phone,
    OffsetDateTime createdAt,
    List<String> roles,
    Boolean active,
    OffsetDateTime lastActionAt
) {}
