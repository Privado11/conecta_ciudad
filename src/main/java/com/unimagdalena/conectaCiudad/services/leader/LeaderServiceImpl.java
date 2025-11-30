package com.unimagdalena.conectaCiudad.services.leader;

import com.unimagdalena.conectaCiudad.Dto.leader.ProjectVotingResultDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingResultsDto;
import com.unimagdalena.conectaCiudad.clients.VotingClient;
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
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderServiceImpl implements LeaderService{

    private final AuditHelper auditHelper;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ReviewRepository reviewRepository;
    private final VotingClient votingClient;

    @Override
    public ProjectDto createProject(ProjectSaveDto projectSaveDto, Long creatorId, Long accessId) {
        try {
            validateProjectDates(projectSaveDto.startAt(), projectSaveDto.endAt());

            validateStartDate(projectSaveDto.startAt(), 20);


            User creator = findUserById(creatorId);

            Project project = projectMapper.toEntity(projectSaveDto);
            project.setCreator(creator);
            project.setStatus(ProjectStatus.DRAFT);
            Project savedProject = projectRepository.save(project);


            Map<String, Object> metadata = buildProjectMetadata(savedProject);
            metadata.put("budget", savedProject.getBudget());

            auditHelper.logComplete(
                    ProjectActionType.PROJECT_CREATED.name(),
                    "Project '" + savedProject.getName() + "' created with ID " + savedProject.getId(),
                    EntityType.PROJECT,
                    savedProject.getId(),
                    ActionResult.SUCCESS,
                    metadata
            );

            return projectMapper.toDto(savedProject);

        } catch (Exception e) {
            auditHelper.logFailure(
                    ProjectActionType.PROJECT_CREATED.name(),
                    "Failed attempt to create project",
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public ProjectDto updateProject(Long id, ProjectSaveDto projectSaveDto, Long creatorId, Long accessId) {
        try {
            Project existingProject = findProjectById(id);
            validateProjectOwnership(existingProject, creatorId);

            if (!existingProject.getStatus().isEditable()) {
                throw new BadRequestException(
                        ErrorCode.PROJECT_NOT_EDITABLE,
                        Map.of("currentStatus", existingProject.getStatus().name())
                );
            }

            validateProjectDates(projectSaveDto.startAt(), projectSaveDto.endAt());

            Map<String, Object> changes = buildChangeMetadata(existingProject, projectSaveDto);

            updateProjectFields(existingProject, projectSaveDto);
            Project updatedProject = projectRepository.save(existingProject);

            auditHelper.logComplete(
                    ProjectActionType.PROJECT_UPDATED.name(),
                    changes.isEmpty()
                            ? "Project '" + updatedProject.getName() + "' updated with no effective changes"
                            : "Project '" + updatedProject.getName() + "' updated",
                    EntityType.PROJECT,
                    id,
                    ActionResult.SUCCESS,
                    changes
            );

            return projectMapper.toDto(updatedProject);

        } catch (Exception e) {
            auditHelper.logFailure(
                    ProjectActionType.PROJECT_UPDATED.name(),
                    "Error updating project " + id,
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public void deleteProject(Long id) {
        try {
            Project project = findProjectById(id);
            Map<String, Object> metadata = buildProjectMetadata(project);
            if (project.getCreator() != null) {
                metadata.put("creatorId", project.getCreator().getId());
            }

            projectRepository.delete(project);

            auditHelper.logComplete(
                    ProjectActionType.PROJECT_DELETED.name(),
                    "Project '" + project.getName() + "' deleted successfully",
                    EntityType.PROJECT,
                    project.getId(),
                    ActionResult.SUCCESS,
                    metadata
            );

        } catch (Exception e) {
            auditHelper.logFailure(
                    ProjectActionType.PROJECT_DELETED.name(),
                    "Error deleting project with ID " + id,
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public ProjectDto submitForReview(Long projectId, Long creatorId, Long accessId) {
        try {
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


            ProjectStatus oldStatus = project.getStatus();

            if (review.getCurator() != null) {
                project.setStatus(ProjectStatus.IN_REVIEW);
            } else {
                project.setStatus(ProjectStatus.PENDING_REVIEW);
            }

            Project updatedProject = projectRepository.save(project);


            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectId", projectId);
            metadata.put("projectName", project.getName());
            metadata.put("oldStatus", oldStatus.name());
            metadata.put("newStatus", ProjectStatus.PENDING_REVIEW.name());
            metadata.put("curatorId", review.getCurator().getId());
            metadata.put("curatorName", review.getCurator().getName());
            metadata.put("isResubmission", oldStatus == ProjectStatus.RETURNED_WITH_OBSERVATIONS);

            auditHelper.logComplete(
                    ProjectActionType.PROJECT_SUBMITTED_FOR_REVIEW.name(),
                    String.format("Project '%s' submitted for review - Curator: %s",
                            project.getName(),
                            review.getCurator().getName()),
                    EntityType.PROJECT,
                    projectId,
                    ActionResult.SUCCESS,
                    metadata
            );

            return projectMapper.toDto(updatedProject);

        } catch (Exception e) {
            auditHelper.logFailure(
                    ProjectActionType.PROJECT_SUBMITTED_FOR_REVIEW.name(),
                    "Error submitting project " + projectId + " for review",
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    public List<ProjectDto> getMyProjects(Long creatorId) {
        List<Project> projects = projectRepository.findByCreatorId(creatorId);
        return projects.stream()
                .map(projectMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectVotingResultDto> getMyClosedVotingResults(Long creatorId, String token) {
        List<Project> closedProjects = projectRepository.findByCreatorIdAndStatus(
                creatorId,
                ProjectStatus.VOTING_CLOSED
        );

        return closedProjects.stream()
                .map(project -> {
                    VotingResultsDto results = votingClient.getProjectVotingResults(
                            project.getId(),
                            token
                    );

                    long votesInFavor = results != null ? results.votesInFavor() : 0L;
                    long votesAgainst = results != null ? results.votesAgainst() : 0L;
                    long totalVotes = votesInFavor + votesAgainst;
                    double approvalPercentage = totalVotes > 0 ? (votesInFavor * 100.0 / totalVotes) : 0.0;
                    String finalResult = approvalPercentage > 50.0 ? "APPROVED" : "REJECTED";

                    return new ProjectVotingResultDto(
                            project.getId(),
                            project.getName(),
                            project.getDescription(),
                            project.getObjectives(),
                            project.getBeneficiaryPopulations(),
                            project.getBudget(),
                            project.getStartAt(),
                            project.getEndAt(),
                            project.getVotingStartAt(),
                            project.getVotingEndAt(),
                            project.getUpdatedAt(),
                            votesInFavor,
                            votesAgainst,
                            totalVotes,
                            approvalPercentage,
                            finalResult
                    );
                })
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

    private Map<String, Object> buildProjectMetadata(Project project) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectId", project.getId());
        metadata.put("projectName", project.getName());
        metadata.put("status", project.getStatus().name());
        return metadata;
    }

    private Map<String, Object> buildChangeMetadata(Project existing, ProjectSaveDto dto) {
        Map<String, Object> changes = new HashMap<>();

        if (!existing.getName().equals(dto.name())) {
            changes.put("oldName", existing.getName());
            changes.put("newName", dto.name());
        }

        if (!Objects.equals(existing.getObjectives(), dto.objectives())) {
            changes.put("oldObjectives", valueOrDefault(existing.getObjectives()));
            changes.put("newObjectives", valueOrDefault(dto.objectives()));
        }

        if (!Objects.equals(existing.getBeneficiaryPopulations(), dto.beneficiaryPopulations())) {
            changes.put("oldBeneficiaryPopulations", valueOrDefault(existing.getBeneficiaryPopulations()));
            changes.put("newBeneficiaryPopulations", valueOrDefault(dto.beneficiaryPopulations()));
        }

        if (!Objects.equals(existing.getBudget(), dto.budget())) {
            changes.put("oldBudget", valueOrDefault(existing.getBudget()));
            changes.put("newBudget", valueOrDefault(dto.budget()));
        }

        if (!Objects.equals(existing.getStartAt(), dto.startAt())) {
            changes.put("oldStartAt", existing.getStartAt() != null ? existing.getStartAt().toString() : null);
            changes.put("newStartAt", dto.startAt() != null ? dto.startAt().toString() : null);
        }

        if (!Objects.equals(existing.getEndAt(), dto.endAt())) {
            changes.put("oldEndAt", existing.getEndAt() != null ? existing.getEndAt().toString() : null);
            changes.put("newEndAt", dto.endAt() != null ? dto.endAt().toString() : null);
        }

        return changes;
    }

    private String valueOrDefault(String value) {
        return value != null ? value : null;
    }

    private Object valueOrDefault(BigDecimal value) {
        return value != null ? value : null;
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
                reviewRepository.save(review);

                auditHelper.logEntity(
                        ProjectActionType.CURATOR_ASSIGNED.name(),
                        "Curator " + chosenCurator.getName() + " automatically assigned to project " + project.getName(),
                        EntityType.PROJECT,
                        project.getId()
                );
            }
        }
    }
}
