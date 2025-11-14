package com.unimagdalena.conectaCiudad.services.project;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.ProjectActionType;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ProjectMapper projectMapper;
    private final AuditHelper auditHelper;
    private final UserMapper userMapper;

    
    @Override
    public ProjectDto findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        ProjectDto dto = projectMapper.toDto(project);
        return attachReview(dto);
    }

    @Override
    public List<ProjectDto> findAll() {
        return projectRepository.findAll()
                .stream()
                .map(projectMapper::toDto)
                .map(this::attachReview)
                .toList();
    }

    @Override
    public List<ProjectDto> findByNameContainingIgnoreCase(String name) {
        return projectRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(projectMapper::toDto)
                .map(this::attachReview)
                .toList();
    }

    @Override
    public List<ProjectDto> findByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status)
                .stream()
                .map(projectMapper::toDto)
                .map(this::attachReview)
                .toList();
    }

    @Override
    public List<ProjectDto> findByCreatorId(Long creatorId) {
        return projectRepository.findByCreatorId(creatorId)
                .stream()
                .map(projectMapper::toDto)
                .map(this::attachReview)
                .toList();
    }

    @Override
    public ProjectDto saveProject(ProjectSaveDto projectSaveDto, Long creatorId, Long accessId) {
        try {
            validateProjectDates(projectSaveDto.startAt(), projectSaveDto.endAt());
            
            User creator = findUserById(creatorId);
            validateUserIsLeader(creator);
            
            Project project = projectMapper.toEntity(projectSaveDto);
            project.setCreator(creator);
            project.setStatus(ProjectStatus.PENDIENTE);
            Project savedProject = projectRepository.save(project);

            assignCuratorIfAvailable(savedProject);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectName", savedProject.getName());
            metadata.put("budget", savedProject.getBudgets());
            metadata.put("status", savedProject.getStatus().name());
            
            auditHelper.logComplete(
                ProjectActionType.PROJECT_CREATED.name(),
                "Proyecto '" + savedProject.getName() + "' creado con ID " + savedProject.getId(),
                EntityType.PROJECT,
                savedProject.getId(),
                ActionResult.SUCCESS,
                metadata
            );

            return attachReview(projectMapper.toDto(savedProject));
            
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
            validateProjectDates(projectSaveDto.startAt(), projectSaveDto.endAt());

            String oldName = existingProject.getName();
            String oldObjectives = existingProject.getObjectives();
            String oldBeneficiaryPopulations = existingProject.getBeneficiaryPopulations();
            String oldBudgets = existingProject.getBudgets();
            ProjectStatus oldStatus = existingProject.getStatus();
            LocalDateTime oldStartAt = existingProject.getStartAt();
            LocalDateTime oldEndAt = existingProject.getEndAt();
            
            updateProjectFields(existingProject, projectSaveDto);
            Project updatedProject = projectRepository.save(existingProject);
    
            Map<String, Object> metadata = new HashMap<>();
            
            if (!oldName.equals(updatedProject.getName())) {
                metadata.put("oldName", oldName);
                metadata.put("newName", updatedProject.getName());
            }
            
            if (!Objects.equals(oldObjectives, updatedProject.getObjectives())) {
                metadata.put("oldObjectives", oldObjectives != null ? oldObjectives : "sin valor");
                metadata.put("newObjectives", updatedProject.getObjectives() != null ? updatedProject.getObjectives() : "sin valor");
            }
            
            if (!Objects.equals(oldBeneficiaryPopulations, updatedProject.getBeneficiaryPopulations())) {
                metadata.put("oldBeneficiaryPopulations", oldBeneficiaryPopulations != null ? oldBeneficiaryPopulations : "sin valor");
                metadata.put("newBeneficiaryPopulations", updatedProject.getBeneficiaryPopulations() != null ? updatedProject.getBeneficiaryPopulations() : "sin valor");
            }
            
            if (!Objects.equals(oldBudgets, updatedProject.getBudgets())) {
                metadata.put("oldBudgets", oldBudgets != null ? oldBudgets : "sin valor");
                metadata.put("newBudgets", updatedProject.getBudgets() != null ? updatedProject.getBudgets() : "sin valor");
            }
            
            if (!oldStatus.equals(updatedProject.getStatus())) {
                metadata.put("oldStatus", oldStatus.name());
                metadata.put("newStatus", updatedProject.getStatus().name());
            }
            
            if (!Objects.equals(oldStartAt, updatedProject.getStartAt())) {
                metadata.put("oldStartAt", oldStartAt != null ? oldStartAt.toString() : "sin valor");
                metadata.put("newStartAt", updatedProject.getStartAt() != null ? updatedProject.getStartAt().toString() : "sin valor");
            }
            
            if (!Objects.equals(oldEndAt, updatedProject.getEndAt())) {
                metadata.put("oldEndAt", oldEndAt != null ? oldEndAt.toString() : "sin valor");
                metadata.put("newEndAt", updatedProject.getEndAt() != null ? updatedProject.getEndAt().toString() : "sin valor");
            }
            
            auditHelper.logComplete(
                ProjectActionType.PROJECT_UPDATED.name(),
                metadata.isEmpty() 
                    ? "Proyecto '" + oldName + "' actualizado sin cambios efectivos"
                    : "Proyecto '" + oldName + "' actualizado",
                EntityType.PROJECT,
                id,
                ActionResult.SUCCESS,
                metadata
            );
    
            return attachReview(projectMapper.toDto(updatedProject));
            
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
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    
           
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectId", project.getId());
            metadata.put("projectName", project.getName());
            metadata.put("status", project.getStatus().name());
            metadata.put("creatorId", project.getCreator() != null ? project.getCreator().getId() : null);
    
          
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
    

    private ProjectDto attachReview(ProjectDto dto) {
        if (dto == null || dto.id() == null) return dto;
        List<Review> reviews = reviewRepository.findByProjectId(dto.id());
        if (reviews.isEmpty()) {
            return dto;
        }
        User curator = reviews.get(0).getCurator();
        Review review = reviews.get(0);
        if (curator == null) {
            return new ProjectDto(
                dto.id(), dto.name(), dto.objectives(), dto.beneficiaryPopulations(), dto.budgets(),
                dto.startAt(), dto.endAt(),dto.createdAt(), dto.status(), dto.creator(), null,
                review.getNotes(), review.getDueAt(), review.getReviewedAt()
            );
        }
        return new ProjectDto(
            dto.id(),
            dto.name(),
            dto.objectives(),
            dto.beneficiaryPopulations(),
            dto.budgets(),
            dto.startAt(),
            dto.endAt(),
            dto.createdAt(),
            dto.status(),
            dto.creator(),
            userMapper.toDto(curator),
            review.getNotes(),
            review.getDueAt(),
            review.getReviewedAt()
        );
    }

    @Override
    public ProjectDto addObservations(Long projectId, Long curatorId, String notes, Long accessId) {
        try {
            Project project = findProjectById(projectId);
            Review review = getProjectReview(projectId);
            validateCuratorAccess(review, curatorId);

            review.setNotes(notes);
            review.setReviewedAt(OffsetDateTime.now());
            reviewRepository.save(review);
            
            ProjectStatus oldStatus = project.getStatus();
            project.setStatus(ProjectStatus.OBSERVACIONES);
            projectRepository.save(project);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectId", projectId);
            metadata.put("oldStatus", oldStatus.name());
            metadata.put("newStatus", ProjectStatus.OBSERVACIONES.name());
            metadata.put("notesLength", notes != null ? notes.length() : 0);

            auditHelper.logComplete(
                ProjectActionType.PROJECT_OBSERVATIONS_ADDED.name(),
                "Observaciones agregadas al proyecto '" + project.getName() + "'",
                EntityType.REVIEW,
                review.getId(),
                ActionResult.SUCCESS,
                metadata
            );

            return attachReview(projectMapper.toDto(project));
            
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
    public ProjectDto approveProject(Long projectId, Long curatorId, Long accessId) {
        try {
            Project project = findProjectById(projectId);
            Review review = getProjectReview(projectId);
            validateCuratorAccess(review, curatorId);

            review.setReviewedAt(OffsetDateTime.now());
            reviewRepository.save(review);
            
            ProjectStatus oldStatus = project.getStatus();
            project.setStatus(ProjectStatus.LISTO_PARA_PUBLICAR);
            projectRepository.save(project);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("projectName", project.getName());
            metadata.put("oldStatus", oldStatus.name());
            metadata.put("newStatus", ProjectStatus.LISTO_PARA_PUBLICAR.name());
            metadata.put("reviewDuration", 
                java.time.Duration.between(review.getStartAt(), LocalDateTime.now()).toDays());

            auditHelper.logComplete(
                ProjectActionType.PROJECT_APPROVED.name(),
                "Proyecto '" + project.getName() + "' aprobado y listo para publicar",
                EntityType.PROJECT,
                projectId,
                ActionResult.SUCCESS,
                metadata
            );

            return attachReview(projectMapper.toDto(project));
            
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
            .map(this::attachReview)
            .toList();
    }

    @Override
    public List<ProjectDto> findReadyToPublish() {
        return projectRepository.findByStatus(ProjectStatus.LISTO_PARA_PUBLICAR)
            .stream()
            .map(projectMapper::toDto)
            .map(this::attachReview)
            .toList();
    }
    
    @Override
    public ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId, Long accessId) {
        try {
            Project project = findProjectById(projectId);
            User newCurator = findUserById(curatorId);
            
            validateUserIsCurator(newCurator);
            validateCuratorNotCreator(newCurator, project.getCreator());

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

    private void validateProjectDates(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la de inicio");
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
            throw new IllegalArgumentException("El usuario no tiene rol CURATOR");
        }
    }

    private void validateCuratorNotCreator(User curator, User creator) {
        if (Objects.equals(curator.getId(), creator.getId())) {
            throw new IllegalArgumentException("El creador no puede ser curador de su propio proyecto");
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
        project.setBudgets(dto.budgets());
        project.setStartAt(dto.startAt());
        project.setEndAt(dto.endAt());
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

}
