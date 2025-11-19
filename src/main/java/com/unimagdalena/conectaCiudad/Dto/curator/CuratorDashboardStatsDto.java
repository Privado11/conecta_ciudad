package com.unimagdalena.conectaCiudad.Dto.curator;

public record CuratorDashboardStatsDto(
    Long assignedProjects,
    Long pendingReview,
    Long inReview,
    Long completedThisMonth,
    Double averageReviewTime,
    Long overdueProjects,
    Double approvalRate,
    Double onTimeRate
) {}
