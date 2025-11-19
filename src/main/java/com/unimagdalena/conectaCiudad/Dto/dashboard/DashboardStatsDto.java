package com.unimagdalena.conectaCiudad.Dto.dashboard;

public record DashboardStatsDto(
    Long totalUsers,
    Long activeUsers,
    Long totalProjects,
    Long activeVotaciones,
    Double participationRate,
    Long newUsersThisMonth
) {}
