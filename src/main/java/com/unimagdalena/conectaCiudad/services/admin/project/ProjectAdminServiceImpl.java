package com.unimagdalena.conectaCiudad.services.admin.project;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.events.ReviewAssignedEvent;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.specifications.ProjectSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAdminServiceImpl implements ProjectAdminService {

    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Page<ProjectDto> findWithFilters(
            String searchTerm,
            ProjectStatus status,
            Long creatorId,
            Long curatorId,
            LocalDate projectStartFrom,
            LocalDate projectStartTo,
            LocalDate projectEndFrom,
            LocalDate projectEndTo,
            LocalDate votingStartFrom,
            LocalDate votingStartTo,
            LocalDate votingEndFrom,
            LocalDate votingEndTo,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            Pageable pageable
    ) {

        validateDateRange(projectStartFrom, projectStartTo);
        validateDateRange(projectEndFrom, projectEndTo);
        validateDateRange(votingStartFrom, votingStartTo);
        validateDateRange(votingEndFrom, votingEndTo);
        validateDateRange(createdFrom, createdTo);
        
        String normalizedSearchTerm = (searchTerm != null && !searchTerm.trim().isEmpty())
                ? searchTerm.trim()
                : null;

        Specification<Project> spec = ProjectSpecifications.withFilters(
                normalizedSearchTerm,
                status,
                creatorId,
                curatorId,
                projectStartFrom,
                projectStartTo,
                projectEndFrom,
                projectEndTo,
                votingStartFrom,
                votingStartTo,
                votingEndFrom,
                votingEndTo,
                createdFrom,
                createdTo
        );

        return projectRepository
                .findAll(spec, pageable)
                .map(projectMapper::toDto);
    }

    @Override
    public ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId, Long accessId) {
        Project project = findProjectById(projectId);
        User newCurator = findUserById(curatorId);

        if (!project.getStatus().canBeReviewed()) {
            throw new BadRequestException(
                    ErrorCode.PROJECT_NOT_REVIEWABLE,
                    Map.of("currentStatus", project.getStatus().name())
            );
        }

        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        Review review = getOrCreateReview(reviews, project, newCurator);

        review.setCurator(newCurator);
        Review savedReview = reviewRepository.save(review);

        User admin = findUserById(adminId);
        eventPublisher.publishEvent(new ReviewAssignedEvent(this, savedReview, admin));

        return projectMapper.toDto(project);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
    }

    private Review getOrCreateReview(List<Review> reviews, Project project, User curator) {
        if (reviews.isEmpty()) {
            return Review.builder()
                    .project(project)
                    .curator(curator)
                    .dueAt(OffsetDateTime.now().plusDays(7))
                    .build();
        }
        return reviews.get(0);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
