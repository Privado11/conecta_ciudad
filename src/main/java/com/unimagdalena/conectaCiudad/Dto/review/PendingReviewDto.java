package com.unimagdalena.conectaCiudad.Dto.review;

import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

public record PendingReviewDto(
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
    OffsetDateTime dueAt,
    OffsetDateTime reviewedAt,
    String currentNotes,
    
    Long daysUntilDue,
    Boolean isOverdue,
    Boolean isDueSoon,
    Long daysInReview,
    Boolean isResubmission,
    
    OffsetDateTime projectCreatedAt,
    Long daysSinceCreation
) {
    
    public static Boolean calculateIsDueSoon(Long daysUntilDue) {
        return daysUntilDue != null && daysUntilDue >= 0 && daysUntilDue <= 2;
    }
    
    public static Boolean calculateIsOverdue(Long daysUntilDue) {
        return daysUntilDue != null && daysUntilDue < 0;
    }

    public String getPriorityLevel() {
        if (isOverdue) return "CRÍTICA";
        if (isDueSoon) return "ALTA";
        if (daysUntilDue <= 4) return "MEDIA";
        return "NORMAL";
    }

    public String getStatusMessage() {
        if (dueAt == null) return "Sin fecha de vencimiento";
    
        OffsetDateTime now = OffsetDateTime.now();
    
        long totalHours = ChronoUnit.HOURS.between(now, dueAt);
        long days = totalHours / 24;
        long hours = totalHours % 24;
    
        if (totalHours < 0) {
            long overdueHours = Math.abs(totalHours);
            long overdueDays = overdueHours / 24;
            long overdueRemainingHours = overdueHours % 24;
    
            if (overdueDays > 0 && overdueRemainingHours == 0) {
                return String.format("Vencida hace %d días", overdueDays);
            }
    
            if (overdueDays == 0) {
                return String.format("Vencida hace %d horas", overdueRemainingHours);
            }
    
            return String.format("Vencida hace %d días y %d horas",
                    overdueDays, overdueRemainingHours);
        }
    
        if (days == 0) {
            if (hours > 0) {
                return String.format("Vence en %d horas", hours);
            } else {
                return "Vence en menos de 1 hora";
            }
        }
    
        if (days == 1) {
            if (hours == 0) {
                return "Vence en 1 día";
            }
            return String.format("Vence en 1 día y %d horas", hours);
        }
    
        if (hours == 0) {
            return String.format("Vence en %d días", days);
        }
        return String.format("Vence en %d días y %d horas", days, hours);
    }
    

}