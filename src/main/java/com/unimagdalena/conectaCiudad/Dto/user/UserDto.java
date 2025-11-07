package com.unimagdalena.conectaCiudad.Dto.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserDto(
    Long id,
    String name,
    String nationalId,
    String email,
    String phone,
    LocalDateTime createdAt,
    List<String> roles,
    Boolean active,
    LocalDateTime lastActionAt
) {}
