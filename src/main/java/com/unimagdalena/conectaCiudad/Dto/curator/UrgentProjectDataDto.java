package com.unimagdalena.conectaCiudad.Dto.curator;

public record UrgentProjectDataDto(
    Long projectId,
    String projectName,
    String creatorName,
    String assignedAt,
    String dueAt,
    Integer daysUntilDue,
    Boolean isOverdue,
    String priorityLevel,
    Integer daysInReview
) {}
