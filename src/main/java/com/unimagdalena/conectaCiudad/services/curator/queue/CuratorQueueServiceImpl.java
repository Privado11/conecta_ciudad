package com.unimagdalena.conectaCiudad.services.curator.queue;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.review.*;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ForbiddenException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.specifications.ReviewSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CuratorQueueServiceImpl implements CuratorQueueService {

    private final ReviewRepository reviewRepository;

    @Override
    public PendingReviewQueueDto getPendingReviewQueue(Long curatorId) {
        List<Review> pendingReviews = reviewRepository.findByCuratorIdAndReviewedAtIsNull(curatorId);
        
        OffsetDateTime now = OffsetDateTime.now();
        
        List<PendingReviewDto> reviews = pendingReviews.stream()
                .filter(review -> review.getProject() != null)
                .filter(review -> review.getProject().getStatus() == ProjectStatus.IN_REVIEW)
                .map(review -> buildPendingReviewDto(review, now))
                .sorted(Comparator
                        .comparing(PendingReviewDto::isOverdue).reversed()
                        .thenComparing(PendingReviewDto::daysUntilDue)
                )
                .toList();
        
        return PendingReviewQueueDto.from(reviews);
    }

    @Override
    public PendingReviewDto getPendingReviewDetails(Long projectId, Long curatorId) {
        Review review = getProjectReview(projectId);
        validateCuratorAccess(review, curatorId);
        
        if (review.getReviewedAt() != null) {
            throw new BadRequestException(ErrorCode.REVIEW_ALREADY_COMPLETED);
        }
        
        return buildPendingReviewDto(review, OffsetDateTime.now());
    }

    @Override
    public PagedResponse<ReviewHistoryDto> getReviewHistory(
            Long curatorId, 
            ReviewHistoryFilterDto filters, 
            Pageable pageable) {
        

        Specification<Review> spec = ReviewSpecifications.withHistoryFilters(
            curatorId,
            filters.searchTerm(),
            filters.status(),
            filters.outcome(),
            filters.wasOverdue(),
            filters.isResubmission(),
            filters.reviewedFrom(),
            filters.reviewedTo()
        );
        

        Page<Review> reviewPage = reviewRepository.findAll(spec, pageable);
        
        OffsetDateTime now = OffsetDateTime.now();
     
        Page<ReviewHistoryDto> reviewDtoPage = reviewPage.map(
            review -> buildReviewHistoryDto(review, now)
        );
        
        List<Review> allReviews = reviewRepository.findAll(spec);
        List<ReviewHistoryDto> allReviewDtos = allReviews.stream()
            .map(review -> buildReviewHistoryDto(review, now))
            .toList();
        
        return ReviewHistoryPageDto.from(reviewDtoPage, allReviewDtos);
    }


    private ReviewHistoryDto buildReviewHistoryDto(Review review, OffsetDateTime now) {
        Project project = review.getProject();
        User creator = project.getCreator();
        
        Long daysToComplete = null;
        Boolean wasOverdue = null;
        
        if (review.getReviewedAt() != null) {
            daysToComplete = ChronoUnit.DAYS.between(
                review.getStartAt().toLocalDate(), 
                review.getReviewedAt().toLocalDate()
            );
            wasOverdue = review.getReviewedAt().isAfter(review.getDueAt());
        }
        
        Boolean isResubmission = review.getNotes() != null && !review.getNotes().isEmpty() &&
                                 project.getStatus() != ProjectStatus.RETURNED_WITH_OBSERVATIONS;
        
        return new ReviewHistoryDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getObjectives(),
                project.getBeneficiaryPopulations(),
                project.getBudget(),
                project.getStartAt(),
                project.getEndAt(),
                project.getStatus(),
                
                creator != null ? creator.getId() : null,
                creator != null ? creator.getName() : null,
                creator != null ? creator.getEmail() : null,
                
                review.getId(),
                review.getStartAt(),
                review.getReviewedAt(),
                review.getDueAt(),
                review.getNotes(),
                
                daysToComplete,
                wasOverdue,
                isResubmission,
                
                project.getCreatedAt(),
                
                project.getVotingStartAt(),
                project.getVotingEndAt()
        );
    }

    private PendingReviewDto buildPendingReviewDto(Review review, OffsetDateTime now) {
        Project project = review.getProject();
        User creator = project.getCreator();
        
        Long daysUntilDue = ChronoUnit.DAYS.between(now.toLocalDate(), review.getDueAt().toLocalDate());
        Long daysInReview = ChronoUnit.DAYS.between(review.getStartAt().toLocalDate(), now.toLocalDate());
        Long daysSinceCreation = ChronoUnit.DAYS.between(project.getCreatedAt().toLocalDate(), now.toLocalDate());
        
        Boolean isResubmission = project.getStatus() == ProjectStatus.PENDING_REVIEW && 
                                 review.getNotes() != null && !review.getNotes().isEmpty();
        
        return new PendingReviewDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getObjectives(),
                project.getBeneficiaryPopulations(),
                project.getBudget(),
                project.getStartAt(),
                project.getEndAt(),
                project.getStatus(),
                
                creator != null ? creator.getId() : null,
                creator != null ? creator.getName() : null,
                creator != null ? creator.getEmail() : null,
                
                review.getId(),
                review.getStartAt(),
                review.getDueAt(),
                review.getReviewedAt(),
                review.getNotes(),
                
                daysUntilDue,
                PendingReviewDto.calculateIsOverdue(daysUntilDue),
                PendingReviewDto.calculateIsDueSoon(daysUntilDue),
                daysInReview,
                isResubmission,
                
                project.getCreatedAt(),
                daysSinceCreation
        );
    }

    private void validateCuratorAccess(Review review, Long curatorId) {
        if (review.getCurator() == null || !Objects.equals(review.getCurator().getId(), curatorId)) {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED_NOT_ASSIGNED_CURATOR);
        }
    }

    private Review getProjectReview(Long projectId) {
        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        if (reviews.isEmpty()) {
            throw new ResourceNotFoundException("Review", "projectId", projectId);
        }
        return reviews.get(0);
    }
}
