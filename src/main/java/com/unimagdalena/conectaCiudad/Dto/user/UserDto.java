package com.unimagdalena.conectaCiudad.Dto.user;

import java.util.List;

public record UserDto(
    Long id,
    String name,
    String nationalId,
    String email,
    String phone,
    List<String> roles
) {}


