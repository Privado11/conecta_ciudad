package com.unimagdalena.conectaCiudad.Dto.user;

public record ChangePasswordDto(
    String oldPassword,
    String newPassword
) {}
