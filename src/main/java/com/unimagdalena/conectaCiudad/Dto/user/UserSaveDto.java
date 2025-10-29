package com.unimagdalena.conectaCiudad.Dto.user;


public record UserSaveDto(
    String name,
    String nationalId,
    String email,
    String password,
    String phone
) {}


