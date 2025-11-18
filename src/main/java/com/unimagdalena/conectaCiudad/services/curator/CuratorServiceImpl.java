package com.unimagdalena.conectaCiudad.services.curator;


import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ProjectActionType;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CuratorServiceImpl implements CuratorService{

    private final ReviewRepository reviewRepository;
    private final ProjectRepository projectRepository;
    private final AuditHelper auditHelper;
    private final ProjectMapper projectMapper;


    @Override
    public ProjectDto addObservations(Long projectId, Long curatorId, String notes, Long accessId) {
        try {
            Project project = findProjectById(projectId);
            Review review = getProjectReview(projectId);
            validateCuratorAccess(review, curatorId);

            if (!project.getStatus().canBeReviewed()) {
                throw new BadRequestException(
                        "No se pueden agregar observaciones. El proyecto debe estar en revisión. Estado actual: " +
                                project.getStatus().getDisplayName()
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
                    "Observaciones agregadas al proyecto '" + project.getName() + "'",
                    EntityType.REVIEW,
                    review.getId(),
                    ActionResult.SUCCESS,
                    metadata
            );

            return projectMapper.toDto(project);

        } catch (Exception e) {
            auditHelper.logFailure(
                    ProjectActionType.PROJECT_OBSERVATIONS_ADDED.name(),
                    "Error al agregar observaciones al proyecto " + projectId,
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
                        "No se pueden aprobar el proyecto. El proyecto debe estar en revisión. Estado actual: " +
                                project.getStatus().getDisplayName()
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
                    String.format("Proyecto '%s' aprobado y listo para publicar. Votación: %s - %s",
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
                    "Error al aprobar proyecto " + projectId,
                    e.getMessage()
            );
            throw e;
        }
    }

    private void validateCuratorAccess(Review review, Long curatorId) {
        if (review.getCurator() == null || !Objects.equals(review.getCurator().getId(), curatorId)) {
            throw new AccessDeniedException("Solo el curador asignado puede realizar esta acción");
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
            throw new BadRequestException("Las fechas de votación son obligatorias");
        }

        LocalDate today = LocalDate.now();

        if (votingStart.isBefore(today)) {
            throw new BadRequestException("La fecha de inicio de votación debe ser futura");
        }

        if (votingEnd.isBefore(votingStart)) {
            throw new BadRequestException("La fecha de fin de votación debe ser posterior a la de inicio");
        }

        if (!votingEnd.isBefore(project.getStartAt())) {
            throw new BadRequestException(
                    String.format(
                            "La votación debe finalizar antes del inicio del proyecto. " +
                                    "Votación termina: %s, Proyecto inicia: %s",
                            votingEnd,
                            project.getStartAt()
                    )
            );
        }

        long bufferDays = java.time.temporal.ChronoUnit.DAYS.between(votingEnd, project.getStartAt());
        if (bufferDays < 2) {
            throw new BadRequestException(
                    "Debe haber al menos 2 días entre el fin de la votación y el inicio del proyecto"
            );
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
}
