package com.unimagdalena.conectaCiudad.services.curator.review;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.events.ReviewCompletedEvent;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ForbiddenException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CuratorReviewServiceImpl implements CuratorReviewService {

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProjectMapper projectMapper;

    @Override
    public ProjectDto addObservations(Long projectId, Long curatorId, String notes, Long accessId) {
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

        project.setStatus(ProjectStatus.RETURNED_WITH_OBSERVATIONS);
        projectRepository.save(project);

        User curator = findUserById(curatorId);
        eventPublisher.publishEvent(new ReviewCompletedEvent(this, review, ProjectStatus.RETURNED_WITH_OBSERVATIONS, curator));

        return projectMapper.toDto(project);
    }

    @Override
    public ProjectDto approveProject(
            Long projectId,
            Long curatorId,
            LocalDate votingStartAt,
            LocalDate votingEndAt,
            Long accessId
    ) {
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

        project.setStatus(ProjectStatus.READY_TO_PUBLISH);
        project.setVotingStartAt(votingStartAt);
        project.setVotingEndAt(votingEndAt);
        projectRepository.save(project);

        User curator = findUserById(curatorId);
        eventPublisher.publishEvent(new ReviewCompletedEvent(this, review, ProjectStatus.READY_TO_PUBLISH, curator));

        return projectMapper.toDto(project);
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


    private void validateCuratorAccess(Review review, Long curatorId) {
        if (review.getCurator() == null || !Objects.equals(review.getCurator().getId(), curatorId)) {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED_NOT_ASSIGNED_CURATOR);
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
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
