package com.unimagdalena.conectaCiudad.Dto.dashboard;

public record UserRoleDistributionDto(
    String role,
    Long count,
    String color
) {}
