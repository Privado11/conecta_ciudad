package com.unimagdalena.conectaCiudad.Dto.review;

import java.util.List;

public record ReviewHistoryQueueDto(
    List<ReviewHistoryDto> reviews,
    HistoryStatistics statistics
) {
    
    public record HistoryStatistics(
        Integer total,
        Long approved,
        Long returned,
        Long rejected,
        Long resubmissions,
        Double averageDaysToComplete,
        Long completedOnTime,
        Long completedOverdue
    ) {}
    
    public static ReviewHistoryQueueDto from(List<ReviewHistoryDto> reviews) {
        long approvedCount = reviews.stream()
                .filter(r -> "APROBADO".equals(r.getReviewOutcome()))
                .count();

        long returnedCount = reviews.stream()
                .filter(r -> "DEVUELTO".equals(r.getReviewOutcome()))
                .count();

        long rejectedCount = reviews.stream()
                .filter(r -> "RECHAZADO".equals(r.getReviewOutcome()))
                .count();

        long resubmissionCount = reviews.stream()
                .filter(ReviewHistoryDto::isResubmission)
                .count();

        double averageDays = reviews.stream()
                .filter(r -> r.daysToComplete() != null)
                .mapToLong(ReviewHistoryDto::daysToComplete)
                .average()
                .orElse(0.0);

        long onTimeCount = reviews.stream()
                .filter(r -> r.wasOverdue() != null && !r.wasOverdue())
                .count();

        long overdueCount = reviews.stream()
                .filter(r -> r.wasOverdue() != null && r.wasOverdue())
                .count();

        HistoryStatistics statistics = new HistoryStatistics(
                reviews.size(),
                approvedCount,
                returnedCount,
                rejectedCount,
                resubmissionCount,
                Math.round(averageDays * 10.0) / 10.0,
                onTimeCount,
                overdueCount
        );

        return new ReviewHistoryQueueDto(reviews, statistics);
    }
}