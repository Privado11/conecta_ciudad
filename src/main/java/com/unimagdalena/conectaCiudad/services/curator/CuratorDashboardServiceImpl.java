package com.unimagdalena.conectaCiudad.services.curator;

import com.unimagdalena.conectaCiudad.Dto.curator.*;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.repositories.ActionRepository;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuratorDashboardServiceImpl implements CuratorDashboardService {

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final ActionRepository actionRepository;

    private static final Map<String, String> STATUS_COLORS = Map.of(
        "PENDING_REVIEW", "#fbbf24",
        "IN_REVIEW", "#60a5fa",
        "READY_TO_PUBLISH", "#a78bfa",
        "PUBLISHED", "#34d399",
        "RETURNED_WITH_OBSERVATIONS", "#f97316"
    );

    @Override
    public CuratorDashboardStatsDto getCuratorDashboardStats(Long curatorId) {
        long assignedProjects = reviewRepository.countByCuratorId(curatorId);
        long pendingReview = reviewRepository.countByCuratorIdAndReviewedAtIsNull(curatorId);
        long completedThisMonth = reviewRepository.countCompletedThisMonth(curatorId);
        long overdueProjects = reviewRepository.countOverdueReviews(curatorId);
        
        // Calculate in review (pending but not overdue)
        long inReview = pendingReview - overdueProjects;
        
        // Average review time
        Double avgTime = reviewRepository.getAverageReviewTimeInDays(curatorId);
        double averageReviewTime = avgTime != null ? Math.round(avgTime * 10.0) / 10.0 : 0.0;
        
        // Calculate approval rate and on-time rate
        long totalCompleted = reviewRepository.countByCuratorIdAndReviewedAtIsNotNull(curatorId);
        
        // Get approved projects (status READY_TO_PUBLISH or PUBLISHED)
        List<Review> completedReviews = reviewRepository.findByCuratorIdAndReviewedAtIsNotNull(curatorId);
        long approvedCount = completedReviews.stream()
            .filter(r -> {
                ProjectStatus status = r.getProject().getStatus();
                return status == ProjectStatus.READY_TO_PUBLISH || status == ProjectStatus.PUBLISHED;
            })
            .count();
        
        double approvalRate = totalCompleted > 0 
            ? Math.round((approvedCount * 100.0 / totalCompleted) * 10.0) / 10.0 
            : 0.0;
        
        // Calculate on-time rate (completed before due date)
        long onTimeCount = completedReviews.stream()
            .filter(r -> r.getReviewedAt() != null && r.getReviewedAt().isBefore(r.getDueAt()))
            .count();
        
        double onTimeRate = totalCompleted > 0 
            ? Math.round((onTimeCount * 100.0 / totalCompleted) * 10.0) / 10.0 
            : 0.0;

        return new CuratorDashboardStatsDto(
            assignedProjects,
            pendingReview,
            inReview,
            completedThisMonth,
            averageReviewTime,
            overdueProjects,
            approvalRate,
            onTimeRate
        );
    }

    @Override
    public List<CuratorProjectStatusDataDto> getCuratorProjectStatusDistribution(Long curatorId) {
        List<Review> reviews = reviewRepository.findByCuratorId(curatorId);
        
        Map<ProjectStatus, Long> statusCounts = reviews.stream()
            .collect(Collectors.groupingBy(
                r -> r.getProject().getStatus(),
                Collectors.counting()
            ));
        
        return statusCounts.entrySet().stream()
            .map(entry -> {
                String statusName = entry.getKey().name();
                String color = STATUS_COLORS.getOrDefault(statusName, "#6b7280");
                return new CuratorProjectStatusDataDto(statusName, entry.getValue(), color);
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<CuratorReviewTrendDataDto> getCuratorReviewTrend(Long curatorId) {
        OffsetDateTime sixMonthsAgo = OffsetDateTime.now().minusMonths(6);
        List<Object[]> monthlyData = reviewRepository.countReviewsByMonth(curatorId, sixMonthsAgo);
        
        // Get all completed reviews for detailed breakdown
        List<Review> completedReviews = reviewRepository.findByCuratorIdAndReviewedAtIsNotNull(curatorId);
        
        // Group by month and status
        Map<String, Map<String, Long>> monthlyBreakdown = new HashMap<>();
        
        for (Review review : completedReviews) {
            if (review.getReviewedAt() != null && review.getReviewedAt().isAfter(sixMonthsAgo)) {
                String monthKey = review.getReviewedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                ProjectStatus status = review.getProject().getStatus();
                
                monthlyBreakdown.putIfAbsent(monthKey, new HashMap<>());
                Map<String, Long> statusCounts = monthlyBreakdown.get(monthKey);
                
                if (status == ProjectStatus.READY_TO_PUBLISH || status == ProjectStatus.PUBLISHED) {
                    statusCounts.merge("approved", 1L, Long::sum);
                } else if (status == ProjectStatus.RETURNED_WITH_OBSERVATIONS) {
                    statusCounts.merge("returned", 1L, Long::sum);
                } else if (status == ProjectStatus.REJECTED) {
                    statusCounts.merge("rejected", 1L, Long::sum);
                }
            }
        }
        
        // Create list for last 6 months
        List<CuratorReviewTrendDataDto> trend = new ArrayList<>();
        OffsetDateTime current = OffsetDateTime.now();
        
        for (int i = 5; i >= 0; i--) {
            OffsetDateTime monthDate = current.minusMonths(i);
            String monthName = monthDate.getMonth()
                .getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            String monthKey = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            Map<String, Long> statusCounts = monthlyBreakdown.getOrDefault(monthKey, new HashMap<>());
            long reviewed = statusCounts.values().stream().mapToLong(Long::longValue).sum();
            long approved = statusCounts.getOrDefault("approved", 0L);
            long returned = statusCounts.getOrDefault("returned", 0L);
            long rejected = statusCounts.getOrDefault("rejected", 0L);
            
            trend.add(new CuratorReviewTrendDataDto(monthName, reviewed, approved, returned, rejected));
        }
        
        return trend;
    }

    @Override
    public List<UrgentProjectDataDto> getUrgentProjects(Long curatorId, int limit) {
        Page<Review> urgentReviews = reviewRepository.findUrgentReviews(
            curatorId, 
            PageRequest.of(0, limit)
        );
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        OffsetDateTime now = OffsetDateTime.now();
        
        return urgentReviews.getContent().stream()
            .map(review -> {
                Project project = review.getProject();
                long daysInReview = ChronoUnit.DAYS.between(review.getStartAt(), now);
                long daysUntilDue = ChronoUnit.DAYS.between(now, review.getDueAt());
                boolean isOverdue = daysUntilDue < 0;
                
                // Determine priority level
                String priorityLevel;
                if (isOverdue) {
                    priorityLevel = "CRÍTICA";
                } else if (daysUntilDue <= 2) {
                    priorityLevel = "ALTA";
                } else if (daysUntilDue <= 5) {
                    priorityLevel = "MEDIA";
                } else {
                    priorityLevel = "NORMAL";
                }
                
                return new UrgentProjectDataDto(
                    project.getId(),
                    project.getName(),
                    project.getCreator().getName(),
                    review.getStartAt().format(formatter),
                    review.getDueAt().format(formatter),
                    (int) daysUntilDue,
                    isOverdue,
                    priorityLevel,
                    (int) daysInReview
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<CuratorRecentActivityDto> getCuratorRecentActivities(Long curatorId, int limit) {
        // Get recent completed reviews
        List<Review> recentReviews = reviewRepository.findByCuratorIdAndReviewedAtIsNotNull(curatorId);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        return recentReviews.stream()
            .filter(r -> r.getReviewedAt() != null)
            .sorted((r1, r2) -> r2.getReviewedAt().compareTo(r1.getReviewedAt()))
            .limit(limit)
            .map(review -> {
                Project project = review.getProject();
                ProjectStatus status = project.getStatus();
                
                String action;
                String outcome;
                
                if (status == ProjectStatus.READY_TO_PUBLISH || status == ProjectStatus.PUBLISHED) {
                    action = "Aprobó proyecto para votación";
                    outcome = "APROBADO";
                } else if (status == ProjectStatus.RETURNED_WITH_OBSERVATIONS) {
                    action = "Devolvió proyecto con observaciones";
                    outcome = "DEVUELTO";
                } else if (status == ProjectStatus.REJECTED) {
                    action = "Rechazó proyecto por incumplimiento";
                    outcome = "RECHAZADO";
                } else if (status == ProjectStatus.IN_REVIEW) {
                    action = "Inició revisión del proyecto";
                    outcome = "EN_REVISION";
                } else {
                    action = "Revisó proyecto";
                    outcome = "EN_REVISION";
                }
                
                return new CuratorRecentActivityDto(
                    review.getId(),
                    project.getName(),
                    action,
                    review.getReviewedAt().format(formatter),
                    outcome
                );
            })
            .collect(Collectors.toList());
    }
}
