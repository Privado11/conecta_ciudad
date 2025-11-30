package com.unimagdalena.conectaCiudad.services.curator;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewQueueDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryFilterDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryPageDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.enums.ProjectActionType;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ForbiddenException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;
import com.unimagdalena.conectaCiudad.specifications.ReviewSpecifications;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CuratorServiceImpl implements CuratorService {

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final AuditHelper auditHelper;
    private final ProjectMapper projectMapper;

    @Override
    public ProjectDto addObservations(Long projectId, Long curatorId, String notes, Long accessId) {
        try {

            if(notes.length() < 10){
                throw new BadRequestException(
                        ErrorCode.REVIEW_OBSERVATIONS_TOO_SHORT,
                        Map.of("minLength", 10)
                );
            }

            Project project = findProjectById(projectId);
            Review review = getProjectReview(projectId);
            validateCuratorAccess(review, curatorId);

            if (!project.getStatus().canBeReviewed()) {
                throw new BadRequestException(
                        ErrorCode.REVIEW_NOT_REVIEWABLE,
                        Map.of("currentStatus", project.getStatus().name())
                );
            }

            review.setNotes(notes);
            review.setReviewedAt(OffsetDateTime.now());
            reviewRepository.save(review);

            ProjectStatus oldStatus = project.getStatus();
            project.setStatus(ProjectStatus.RETURNED_WITH_OBSERVATIONS);
            projectRepository.save(project);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectId", projectId);
            metadata.put("oldStatus", oldStatus.name());
            metadata.put("newStatus", ProjectStatus.RETURNED_WITH_OBSERVATIONS.name());
            metadata.put("notesLength", notes != null ? notes.length() : 0);

            auditHelper.logComplete(
                    ProjectActionType.PROJECT_OBSERVATIONS_ADDED.name(),
                    "Observations added to project '" + project.getName() + "'",
                    EntityType.REVIEW,
                    review.getId(),
                    ActionResult.SUCCESS,
                    metadata
            );

            return projectMapper.toDto(project);

        } catch (Exception e) {
            auditHelper.logFailure(
                    ProjectActionType.PROJECT_OBSERVATIONS_ADDED.name(),
                    "Error adding observations to project " + projectId,
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public ProjectDto approveProject(
            Long projectId,
            Long curatorId,
            LocalDate votingStartAt,
            LocalDate votingEndAt,
            Long accessId
    ) {
        try {
            Project project = findProjectById(projectId);
            Review review = getProjectReview(projectId);
            validateCuratorAccess(review, curatorId);

            validateVotingDates(votingStartAt, votingEndAt, project);

            if (!project.getStatus().canBeReviewed()) {
                throw new BadRequestException(
                        ErrorCode.REVIEW_NOT_APPROVABLE,
                        Map.of("currentStatus", project.getStatus().name())
                );
            }

            review.setReviewedAt(OffsetDateTime.now());
            reviewRepository.save(review);

            ProjectStatus oldStatus = project.getStatus();
            project.setStatus(ProjectStatus.READY_TO_PUBLISH);

            project.setVotingStartAt(votingStartAt);
            project.setVotingEndAt(votingEndAt);

            projectRepository.save(project);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectName", project.getName());
            metadata.put("oldStatus", oldStatus.name());
            metadata.put("newStatus", ProjectStatus.READY_TO_PUBLISH.name());
            metadata.put("votingStartAt", votingStartAt.toString());
            metadata.put("votingEndAt", votingEndAt.toString());
            metadata.put("votingDurationDays",
                    java.time.Duration.between(
                            votingStartAt.atStartOfDay(),
                            votingEndAt.atStartOfDay()
                    ).toDays());
            metadata.put("reviewDuration",
                    java.time.Duration.between(
                            review.getStartAt().toLocalDate().atStartOfDay(),
                            OffsetDateTime.now().toLocalDate().atStartOfDay()
                    ).toDays());

            auditHelper.logComplete(
                    ProjectActionType.PROJECT_APPROVED.name(),
                    String.format("Project '%s' approved and ready to publish. Voting: %s - %s",
                            project.getName(),
                            votingStartAt,
                            votingEndAt),
                    EntityType.PROJECT,
                    projectId,
                    ActionResult.SUCCESS,
                    metadata
            );

            return projectMapper.toDto(project);

        } catch (Exception e) {
            auditHelper.logFailure(
                    ProjectActionType.PROJECT_APPROVED.name(),
                    "Error approving project " + projectId,
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public List<ProjectDto> findByCurator(Long curatorId, ProjectStatus status) {
        List<Review> reviews = reviewRepository.findByCuratorId(curatorId);
        return reviews.stream()
                .map(Review::getProject)
                .filter(Objects::nonNull)
                .filter(p -> status == null || p.getStatus() == status)
                .map(projectMapper::toDto)
                .toList();
    }

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

    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
    }

    private Review getProjectReview(Long projectId) {
        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        if (reviews.isEmpty()) {
            throw new ResourceNotFoundException("Review", "projectId", projectId);
        }
        return reviews.get(0);
    }

    private void validateVotingDates(LocalDate votingStart, LocalDate votingEnd, Project project) {

        if (votingStart == null || votingEnd == null) {
            throw new BadRequestException(ErrorCode.VOTING_DATES_REQUIRED);
        }
    
        LocalDate today = LocalDate.now();
    
        if (!votingStart.isAfter(today)) {
            throw new BadRequestException(ErrorCode.VOTING_START_NOT_FUTURE);
        }
    
        if (!votingEnd.isAfter(votingStart)) {
            throw new BadRequestException(ErrorCode.VOTING_END_BEFORE_START);
        }
    

        if (!votingEnd.isBefore(project.getStartAt())) {
            throw new BadRequestException(
                    ErrorCode.VOTING_END_AFTER_PROJECT_START,
                    Map.of(
                            "votingEnd", votingEnd.toString(),
                            "projectStart", project.getStartAt().toString()
                    )
            );
        }
    
        long bufferDays = ChronoUnit.DAYS.between(votingEnd, project.getStartAt());
        if (bufferDays < 3) {
            throw new BadRequestException(
                    ErrorCode.VOTING_BUFFER_TOO_SHORT,
                    Map.of(
                            "currentDays", bufferDays,
                            "requiredDays", 3
                    )
            );
        }
    
        long votingDurationDays = ChronoUnit.DAYS.between(votingStart, votingEnd);
        if (votingDurationDays < 3) {
            throw new BadRequestException(
                    ErrorCode.VOTING_DURATION_TOO_SHORT,
                    Map.of(
                            "currentDays", votingDurationDays,
                            "requiredDays", 3
                    )
            );
        }
    }
}    
    
