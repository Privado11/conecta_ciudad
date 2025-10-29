package com.unimagdalena.conectaCiudad.Dto.user;

public record UserDto(
    Long id,
    String name,
    String nationalId,
    String email,
    String phone
) {}


