package com.unimagdalena.conectaCiudad.services.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.ProjectActionType;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;
import com.unimagdalena.conectaCiudad.specifications.ProjectSpecifications;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ProjectMapper projectMapper;
    private final AuditHelper auditHelper;


    public ProjectDto findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return projectMapper.toDto(project);
    }
    
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

    validateDateRange(projectStartFrom, projectStartTo, "inicio del proyecto");
    validateDateRange(projectEndFrom, projectEndTo, "fin del proyecto");
    validateDateRange(votingStartFrom, votingStartTo, "inicio de votación");
    validateDateRange(votingEndFrom, votingEndTo, "fin de votación");
    validateDateRange(createdFrom, createdTo, "creación");
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
    public Statistics<ProjectDto> getGlobalStatistics() {

    List<Project> all = projectRepository.findAll();

    Map<ProjectStatus, Long> countByStatus = new EnumMap<>(ProjectStatus.class);

    for (ProjectStatus s : ProjectStatus.values()) {
        long count = all.stream()
                .filter(p -> p.getStatus() == s)
                .count();
        countByStatus.put(s, count);
    }

    long total = all.size();

    long createdToday = all.stream()
            .filter(p -> p.getCreatedAt().isAfter(
                    OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset())
            )).count();

    return new Statistics<>(
        total,
        Map.of(
            "total", total,
            "pending", countByStatus.getOrDefault(ProjectStatus.PENDING_REVIEW, 0L),
            "inReview", countByStatus.getOrDefault(ProjectStatus.IN_REVIEW, 0L),
            "withObservations", countByStatus.getOrDefault(ProjectStatus.RETURNED_WITH_OBSERVATIONS, 0L),
            "readyToPublish", countByStatus.getOrDefault(ProjectStatus.READY_TO_PUBLISH, 0L),
            "published", countByStatus.getOrDefault(ProjectStatus.PUBLISHED, 0L),
            "votingClosed", countByStatus.getOrDefault(ProjectStatus.VOTING_CLOSED, 0L),

            "inProgress",
            countByStatus.getOrDefault(ProjectStatus.PENDING_REVIEW, 0L)
            + countByStatus.getOrDefault(ProjectStatus.IN_REVIEW, 0L)
            + countByStatus.getOrDefault(ProjectStatus.RETURNED_WITH_OBSERVATIONS, 0L),


            "completed",
                countByStatus.getOrDefault(ProjectStatus.READY_TO_PUBLISH, 0L)
                + countByStatus.getOrDefault(ProjectStatus.PUBLISHED, 0L)
                + countByStatus.getOrDefault(ProjectStatus.VOTING_CLOSED, 0L),

            "createdToday", createdToday
        )
    );
}


    @Override
    public ProjectDto saveProject(ProjectSaveDto projectSaveDto, Long creatorId, Long accessId) {
        try {
            validateProjectDates(projectSaveDto.startAt(), projectSaveDto.endAt());
            
            User creator = findUserById(creatorId);
            validateUserIsLeader(creator);
            
            Project project = projectMapper.toEntity(projectSaveDto);
            project.setCreator(creator);
            project.setStatus(ProjectStatus.DRAFT);
            Project savedProject = projectRepository.save(project);


            Map<String, Object> metadata = buildProjectMetadata(savedProject);
            metadata.put("budget", savedProject.getBudget());
            
            auditHelper.logComplete(
                ProjectActionType.PROJECT_CREATED.name(),
                "Proyecto '" + savedProject.getName() + "' creado con ID " + savedProject.getId(),
                EntityType.PROJECT,
                savedProject.getId(),
                ActionResult.SUCCESS,
                metadata
            );

            return projectMapper.toDto(savedProject);
            
        } catch (Exception e) {
            auditHelper.logFailure(
                ProjectActionType.PROJECT_CREATED.name(),
                "Intento fallido de crear proyecto",
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
                    "El proyecto no puede ser editado en el estado: " + 
                    existingProject.getStatus().getDisplayName()
                );
            }
            
            validateProjectDates(projectSaveDto.startAt(), projectSaveDto.endAt());
    
            Map<String, Object> changes = buildChangeMetadata(existingProject, projectSaveDto);
            
            updateProjectFields(existingProject, projectSaveDto);
            Project updatedProject = projectRepository.save(existingProject);
    
            auditHelper.logComplete(
                ProjectActionType.PROJECT_UPDATED.name(),
                changes.isEmpty() 
                    ? "Proyecto '" + updatedProject.getName() + "' actualizado sin cambios efectivos"
                    : "Proyecto '" + updatedProject.getName() + "' actualizado",
                EntityType.PROJECT,
                id,
                ActionResult.SUCCESS,
                changes
            );
    
            return projectMapper.toDto(updatedProject);
            
        } catch (Exception e) {
            auditHelper.logFailure(
                ProjectActionType.PROJECT_UPDATED.name(),
                "Error al actualizar proyecto " + id,
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
                "Proyecto '" + project.getName() + "' eliminado correctamente",
                EntityType.PROJECT,
                project.getId(),
                ActionResult.SUCCESS,
                metadata
            );
    
        } catch (Exception e) {
            auditHelper.logFailure(
                ProjectActionType.PROJECT_DELETED.name(),
                "Error al eliminar proyecto con ID " + id,
                e.getMessage()
            );
            throw e;
        }
    }
    

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
    public List<ProjectDto> findReadyToPublish() {
        return projectRepository.findByStatus(ProjectStatus.READY_TO_PUBLISH, Pageable.unpaged())
            .stream()
            .map(projectMapper::toDto)
            .toList();
    }
    
    @Override
    public ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId, Long accessId) {
        try {
            Project project = findProjectById(projectId);
            User newCurator = findUserById(curatorId);
            
            validateUserIsCurator(newCurator);
            validateCuratorNotCreator(newCurator, project.getCreator());

            if (!project.getStatus().canBeReviewed()) {
                throw new BadRequestException(
                    "No se pueden reasignar el curador. El proyecto debe estar en revisión. Estado actual: " + 
                project.getStatus().getDisplayName()
                );
            }

            List<Review> reviews = reviewRepository.findByProjectId(projectId);
            Review review = getOrCreateReview(reviews, project, newCurator);
            
            User oldCurator = review.getCurator();
            review.setCurator(newCurator);
            reviewRepository.save(review);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectName", project.getName());
            metadata.put("projectId", projectId);
            metadata.put("oldCuratorId", oldCurator != null ? oldCurator.getId() : null);
            metadata.put("oldCuratorName", oldCurator != null ? oldCurator.getName() : "ninguno");
            metadata.put("newCuratorId", newCurator.getId());
            metadata.put("newCuratorName", newCurator.getName());

            auditHelper.logComplete(
                ProjectActionType.CURATOR_REASSIGNED.name(),
                String.format("Curador reasignado en proyecto '%s': %s → %s",
                    project.getName(),
                    oldCurator != null ? oldCurator.getName() : "ninguno",
                    newCurator.getName()),
                EntityType.REVIEW,
                review.getId(),
                ActionResult.SUCCESS,
                metadata
            );

            return projectMapper.toDto(project);
            
        } catch (Exception e) {
            auditHelper.logFailure(
                ProjectActionType.CURATOR_REASSIGNED.name(),
                "Error al reasignar curador al proyecto " + projectId,
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
        
            if (!project.getStatus().canBeSubmitted()) {
                throw new BadRequestException(
                    "El proyecto no puede ser enviado a revisión desde el estado: " + 
                    project.getStatus().getDisplayName()
                );
            }
        
            List<Review> reviews = reviewRepository.findByProjectId(projectId);
            Review review;

            if (reviews.isEmpty() || reviews.get(0).getCurator() == null) {
                assignCuratorIfAvailable(project);
                
                reviews = reviewRepository.findByProjectId(projectId);
                if (reviews.isEmpty()) {
                    throw new BadRequestException(
                        "No hay curadores disponibles para asignar al proyecto"
                    );
                }
                review = reviews.get(0);
            } else {
                review = reviews.get(0);
                if (project.getStatus() == ProjectStatus.RETURNED_WITH_OBSERVATIONS) {
                    review.setStartAt(OffsetDateTime.now());
                    review.setDueAt(OffsetDateTime.now().plusDays(7));
                    review.setReviewedAt(null); 
                    reviewRepository.save(review);
                }
            }
            
        
            ProjectStatus oldStatus = project.getStatus();
            project.setStatus(ProjectStatus.PENDING_REVIEW);
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
            String.format("Proyecto '%s' enviado a revisión - Curador: %s",
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
            "Error al enviar proyecto " + projectId + " a revisión",
            e.getMessage()
        );
        throw e;
    }
}

private void validateProjectDates(LocalDate startAt, LocalDate endAt) {
    if (startAt == null || endAt == null) {
        throw new BadRequestException("Las fechas de inicio y fin son obligatorias");
    }
    if (endAt.isBefore(startAt)) {
        throw new BadRequestException("La fecha de fin debe ser posterior a la de inicio");
    }
}

private void validateDateRange(LocalDate from, LocalDate to, String fieldName) {
    if (from != null && to != null && to.isBefore(from)) {
        throw new BadRequestException(
            String.format("La fecha '%s hasta' debe ser posterior a la fecha 'desde'", fieldName)
        );
    }
}

private void validateDateRange(OffsetDateTime from, OffsetDateTime to, String fieldName) {
    if (from != null && to != null && to.isBefore(from)) {
        throw new BadRequestException(
            String.format("La fecha '%s hasta' debe ser posterior a la fecha 'desde'", fieldName)
        );
    }
}

    private void validateUserIsLeader(User user) {
        boolean isLeader = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "LIDER_COMUNITARIO".equalsIgnoreCase(r.getName()));
        if (!isLeader) {
            throw new BadRequestException("Solo un LIDER_COMUNITARIO puede crear proyectos");
        }
    }

    private void validateUserIsCurator(User user) {
        boolean isCurator = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "CURATOR".equalsIgnoreCase(r.getName()));
        if (!isCurator) {
            throw new BadRequestException("El usuario no tiene rol CURATOR");
        }
    }

    private void validateCuratorNotCreator(User curator, User creator) {
        if (Objects.equals(curator.getId(), creator.getId())) {
            throw new BadRequestException("El creador no puede ser curador de su propio proyecto");
        }
    }

    private void validateProjectOwnership(Project project, Long userId) {
        if (!Objects.equals(project.getCreator().getId(), userId)) {
            throw new AccessDeniedException("Solo el creador del proyecto puede editarlo");
        }
    }

    private void validateCuratorAccess(Review review, Long curatorId) {
        if (review.getCurator() == null || !Objects.equals(review.getCurator().getId(), curatorId)) {
            throw new AccessDeniedException("Solo el curador asignado puede realizar esta acción");
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
            changes.put("oldStartAt", existing.getStartAt() != null ? existing.getStartAt().toString() : "sin valor");
            changes.put("newStartAt", dto.startAt() != null ? dto.startAt().toString() : "sin valor");
        }
        
        if (!Objects.equals(existing.getEndAt(), dto.endAt())) {
            changes.put("oldEndAt", existing.getEndAt() != null ? existing.getEndAt().toString() : "sin valor");
            changes.put("newEndAt", dto.endAt() != null ? dto.endAt().toString() : "sin valor");
        }
        
        return changes;
    }

    private String valueOrDefault(String value) {
        return value != null ? value : "sin valor";
    }

    private Object valueOrDefault(BigDecimal value) {
        return value != null ? value : "sin valor";
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
                    .dueAt(OffsetDateTime.now().plusDays(7))
                    .build();
                reviewRepository.save(review);
                
                auditHelper.logEntity(
                    ProjectActionType.CURATOR_ASSIGNED.name(),
                    "Curador " + chosenCurator.getName() + " asignado automáticamente al proyecto " + project.getName(),
                    EntityType.PROJECT,
                    project.getId()
                );
            }
        }
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
    

}
