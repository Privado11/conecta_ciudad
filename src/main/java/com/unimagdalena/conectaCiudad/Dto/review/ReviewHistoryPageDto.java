package com.unimagdalena.conectaCiudad.Dto.review;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewHistoryPageDto {
    
    public static PagedResponse<ReviewHistoryDto> from(
            Page<ReviewHistoryDto> page,
            List<ReviewHistoryDto> allReviews) {
        
        Map<String, Object> metrics = calculateMetrics(allReviews);
        
        Statistics<ReviewHistoryDto> statistics = new Statistics<>(
            allReviews.size(),
            metrics
        );
        
        return new PagedResponse<>(page, statistics);
    }
    
    private static Map<String, Object> calculateMetrics(List<ReviewHistoryDto> reviews) {
        Map<String, Object> metrics = new HashMap<>();
        
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
        
        metrics.put("approved", approvedCount);
        metrics.put("returned", returnedCount);
        metrics.put("rejected", rejectedCount);
        metrics.put("resubmissions", resubmissionCount);
        metrics.put("averageDaysToComplete", Math.round(averageDays * 10.0) / 10.0);
        metrics.put("completedOnTime", onTimeCount);
        metrics.put("completedOverdue", overdueCount);

        if (!reviews.isEmpty()) {
            metrics.put("approvalRate", Math.round((approvedCount * 100.0 / reviews.size()) * 10.0) / 10.0);
            metrics.put("returnRate", Math.round((returnedCount * 100.0 / reviews.size()) * 10.0) / 10.0);
            metrics.put("onTimeRate", Math.round((onTimeCount * 100.0 / reviews.size()) * 10.0) / 10.0);
        }
        
        return metrics;
    }
}