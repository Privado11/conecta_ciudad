package com.unimagdalena.conectaCiudad.Dto.review;

import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ReviewHistoryDto(
    Long projectId,
    String projectName,
    String description,
    String objectives,
    String beneficiaryPopulations,
    BigDecimal budget,
    LocalDate projectStartAt,
    LocalDate projectEndAt,
    ProjectStatus projectStatus,
    
    Long creatorId,
    String creatorName,
    String creatorEmail,
    
    Long reviewId,
    OffsetDateTime assignedAt,
    OffsetDateTime reviewedAt,
    OffsetDateTime dueAt,
    String notes,
    
    Long daysToComplete,
    Boolean wasOverdue,
    Boolean isResubmission,
    
    OffsetDateTime projectCreatedAt,
    

    LocalDate votingStartAt,
    LocalDate votingEndAt
) {
    
    public String getReviewOutcome() {
        return switch (projectStatus) {
            case RETURNED_WITH_OBSERVATIONS -> "DEVUELTO";
            case READY_TO_PUBLISH -> "APROBADO";
            case REJECTED -> "RECHAZADO";
            default -> "DESCONOCIDO";
        };
    }
    
    public String getStatusDisplayName() {
        return projectStatus.name();
    }
}