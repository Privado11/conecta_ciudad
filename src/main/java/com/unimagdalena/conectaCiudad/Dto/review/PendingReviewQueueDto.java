package com.unimagdalena.conectaCiudad.Dto.review;

import java.util.List;

public record PendingReviewQueueDto(
    List<PendingReviewDto> reviews,
    QueueStatistics statistics
) {
    
    public record QueueStatistics(
        Integer total,
        Long overdue,
        Long dueSoon,
        Long criticalPriority,
        Long highPriority,
        Long resubmissions
    ) {}
    
    public static PendingReviewQueueDto from(List<PendingReviewDto> reviews) {
        long overdueCount = reviews.stream()
                .filter(PendingReviewDto::isOverdue)
                .count();

        long dueSoonCount = reviews.stream()
                .filter(PendingReviewDto::isDueSoon)
                .count();

        long criticalCount = reviews.stream()
                .filter(r -> "CRÍTICA".equals(r.getPriorityLevel()))
                .count();

        long highPriorityCount = reviews.stream()
                .filter(r -> "ALTA".equals(r.getPriorityLevel()))
                .count();

        long resubmissionCount = reviews.stream()
                .filter(PendingReviewDto::isResubmission)
                .count();

        QueueStatistics statistics = new QueueStatistics(
                reviews.size(),
                overdueCount,
                dueSoonCount,
                criticalCount,
                highPriorityCount,
                resubmissionCount
        );

        return new PendingReviewQueueDto(reviews, statistics);
    }
}