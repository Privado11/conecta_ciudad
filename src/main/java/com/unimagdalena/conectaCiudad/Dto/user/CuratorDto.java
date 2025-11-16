package com.unimagdalena.conectaCiudad.Dto.user;

public record CuratorDto(
    UserDto user,
    Long activeProjects,
    Long completedProjects,
    Long totalProjects
) {}    