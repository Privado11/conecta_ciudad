package com.unimagdalena.conectaCiudad.Dto.user;

public record CuratorDto(
    UserDto curator,
    Long activeProjects,
    Long completedProjects,
    Long totalProjects
) {}    