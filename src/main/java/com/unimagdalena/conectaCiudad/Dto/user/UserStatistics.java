package com.unimagdalena.conectaCiudad.Dto.user;

public record UserStatistics(
    long total,
    long active,
    long inactive
) {}