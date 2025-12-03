package com.unimagdalena.conectaCiudad.services.leader.project;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.events.*;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderProjectServiceImpl implements LeaderProjectService {

    private final ApplicationEventPublisher eventPublisher;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ReviewRepository reviewRepository;

    @Override
    public ProjectDto createProject(ProjectSaveDto projectSaveDto, Long creatorId, Long accessId) {
        validateProjectDates(projectSaveDto.startAt(), projectSaveDto.endAt());
        validateStartDate(projectSaveDto.startAt(), 20);

        User creator = findUserById(creatorId);

        Project project = projectMapper.toEntity(projectSaveDto);
        project.setCreator(creator);
        project.setStatus(ProjectStatus.DRAFT);
        Project savedProject = projectRepository.save(project);

        eventPublisher.publishEvent(new ProjectCreatedEvent(this, savedProject, creator));

        return projectMapper.toDto(savedProject);
    }

    @Override
    public ProjectDto updateProject(Long id, ProjectSaveDto projectSaveDto, Long creatorId, Long accessId) {
        Project existingProject = findProjectById(id);
        validateProjectOwnership(existingProject, creatorId);

        if (!existingProject.getStatus().isEditable()) {
            throw new BadRequestException(
                    ErrorCode.PROJECT_NOT_EDITABLE,
                    Map.of("currentStatus", existingProject.getStatus().name())
            );
        }

        validateProjectDates(projectSaveDto.startAt(), projectSaveDto.endAt());

        User updater = findUserById(creatorId);
        updateProjectFields(existingProject, projectSaveDto);
        Project updatedProject = projectRepository.save(existingProject);

        eventPublisher.publishEvent(new ProjectUpdatedEvent(this, updatedProject, updater));

        return projectMapper.toDto(updatedProject);
    }

    @Override
    public void deleteProject(Long id) {
        Project project = findProjectById(id);
        String projectName = project.getName();
        User deleter = project.getCreator();
        Long projectId = project.getId();

        projectRepository.delete(project);

        eventPublisher.publishEvent(new ProjectDeletedEvent(this, projectId, projectName, deleter));
    }

    @Override
    public ProjectDto submitForReview(Long projectId, Long creatorId, Long accessId) {
        Project project = findProjectById(projectId);
        validateProjectOwnership(project, creatorId);
        validateStartDate(project.getStartAt(), 10);

        if (!project.getStatus().canBeSubmitted()) {
            throw new BadRequestException(
                    ErrorCode.PROJECT_NOT_SUBMITTABLE,
                    Map.of("currentStatus", project.getStatus().name())
            );
        }

        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        Review review;

        if (reviews.isEmpty() || reviews.get(0).getCurator() == null) {
            assignCuratorIfAvailable(project);

            reviews = reviewRepository.findByProjectId(projectId);
            if (reviews.isEmpty()) {
                Review emptyReview = Review.builder()
                        .project(project)
                        .curator(null)
                        .startAt(null)
                        .dueAt(null)
                        .build();
                reviewRepository.save(emptyReview);
                review = emptyReview;
            } else {
                review = reviews.get(0);
            }
        } else {
            review = reviews.get(0);
            if (project.getStatus() == ProjectStatus.RETURNED_WITH_OBSERVATIONS) {
                review.setStartAt(OffsetDateTime.now());
                review.setDueAt(OffsetDateTime.now().plusDays(5));
                review.setReviewedAt(null);
                reviewRepository.save(review);
            }
        }

        if (review.getCurator() != null) {
            project.setStatus(ProjectStatus.IN_REVIEW);
        } else {
            project.setStatus(ProjectStatus.PENDING_REVIEW);
        }

        Project updatedProject = projectRepository.save(project);
        User submitter = findUserById(creatorId);

        eventPublisher.publishEvent(new ProjectSubmittedEvent(this, updatedProject, submitter));

        return projectMapper.toDto(updatedProject);
    }

    @Override
    public List<ProjectDto> getMyProjects(Long creatorId) {
        List<Project> projects = projectRepository.findByCreatorId(creatorId);
        return projects.stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());
    }


    private void validateProjectDates(LocalDate startAt, LocalDate endAt) {
        if (startAt == null || endAt == null) {
            throw new BadRequestException(ErrorCode.PROJECT_DATES_REQUIRED);
        }
        if (endAt.isBefore(startAt)) {
            throw new BadRequestException(ErrorCode.PROJECT_END_BEFORE_START);
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private void updateProjectFields(Project project, ProjectSaveDto dto) {
        project.setName(dto.name());
        project.setObjectives(dto.objectives());
        project.setBeneficiaryPopulations(dto.beneficiaryPopulations());
        project.setBudget(dto.budget());
        project.setStartAt(dto.startAt());
        project.setEndAt(dto.endAt());
    }

    private void validateStartDate(LocalDate startAt, Integer requiredDays) {
        LocalDate today = LocalDate.now();

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(today, startAt);

        if (daysBetween < requiredDays) {
            throw new BadRequestException(
                    ErrorCode.PROJECT_START_DATE_TOO_SOON,
                    Map.of(
                            "requiredDays", requiredDays,
                            "actualDays", daysBetween
                    )
            );
        }
    }

    private void validateProjectOwnership(Project project, Long userId) {
        if (!Objects.equals(project.getCreator().getId(), userId)) {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED_NOT_PROJECT_OWNER);
        }
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
    }

    private void assignCuratorIfAvailable(Project project) {
        List<User> potentialCurators = userRepository.findByRoles_NameIgnoreCase("CURATOR");
        potentialCurators.removeIf(u -> Objects.equals(u.getId(), project.getCreator().getId()));

        if (!potentialCurators.isEmpty()) {
            User chosenCurator = potentialCurators.stream()
                    .min(Comparator.comparingLong(u ->
                            reviewRepository.countByCuratorIdAndReviewedAtIsNull(u.getId())))
                    .orElse(null);

            if (chosenCurator != null) {
                Review review = Review.builder()
                        .project(project)
                        .curator(chosenCurator)
                        .startAt(OffsetDateTime.now())
                        .dueAt(OffsetDateTime.now().plusDays(5))
                        .build();
                Review savedReview = reviewRepository.save(review);

                eventPublisher.publishEvent(new ReviewAssignedEvent(this, savedReview, project.getCreator()));
            }
        }
    }
}
