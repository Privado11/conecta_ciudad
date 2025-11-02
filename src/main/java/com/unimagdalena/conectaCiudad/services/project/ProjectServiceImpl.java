package com.unimagdalena.conectaCiudad.services.project;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.entities.Action;
import com.unimagdalena.conectaCiudad.enums.ProjectActionType;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.repositories.ActionRepository;

@Service
public class ProjectServiceImpl implements ProjectService {
    
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ActionRepository actionRepository;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    @Autowired
    public ProjectServiceImpl(ProjectRepository projectRepository, UserRepository userRepository,
                              ProjectMapper projectMapper, ReviewRepository reviewRepository, UserMapper userMapper,
                              ActionRepository actionRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
        this.reviewRepository = reviewRepository;
        this.userMapper = userMapper;
        this.actionRepository = actionRepository;
    }

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
    public ProjectDto saveProject(ProjectSaveDto projectSaveDto, Long creatorId) {
       
        if (projectSaveDto.startAt() == null || projectSaveDto.endAt() == null) {
            throw new IllegalArgumentException("Both start and end dates are required");
        }
    
        if (projectSaveDto.endAt().isBefore(projectSaveDto.startAt())) {
            throw new IllegalArgumentException("End date must be after the start date");
        }
        
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));
        boolean isLeader = creator.getRoles() != null && creator.getRoles().stream()
                .anyMatch(r -> "LIDER_COMUNITARIO".equalsIgnoreCase(r.getName()));
        if (!isLeader) {
            throw new BadRequestException("Solo un LIDER_COMUNITARIO puede crear proyectos");
        }
        
        Project project = projectMapper.toEntity(projectSaveDto);
        project.setCreator(creator);
        project.setStatus(ProjectStatus.PENDIENTE);

        Project savedProject = projectRepository.save(project);

        
        List<User> potentialCurators = userRepository.findByRoles_NameIgnoreCase("CURATOR");
        potentialCurators.removeIf(u -> Objects.equals(u.getId(), creator.getId()));

        if (!potentialCurators.isEmpty()) {
            User chosenCurator = potentialCurators.stream()
                .min(Comparator.comparingLong(u -> reviewRepository.countByCuratorIdAndReviewedAtIsNull(u.getId())))
                .orElse(null);

            if (chosenCurator != null) {
                Review review = Review.builder()
                    .project(savedProject)
                    .curator(chosenCurator)
                    .dueAt(LocalDateTime.now().plusDays(7))
                    .build();
                reviewRepository.save(review);
                logAction(chosenCurator.getId(), ProjectActionType.CURATOR_ASSIGNED, "Curator asignado al proyecto " + savedProject.getId());
            }
        }

        logAction(creator.getId(), ProjectActionType.PROJECT_CREATED, "Proyecto creado con id " + savedProject.getId());

        return attachReview(projectMapper.toDto(savedProject));
    }

    @Override
public ProjectDto updateProject(Long id, ProjectSaveDto projectSaveDto, Long creatorId) {
    Project existingProject = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    

    if (!Objects.equals(existingProject.getCreator().getId(), creatorId)) {
        throw new AccessDeniedException("Solo el creador del proyecto puede editarlo");
    }
    
   
    if (projectSaveDto.startAt() != null && projectSaveDto.endAt() != null) {
        if (projectSaveDto.endAt().isBefore(projectSaveDto.startAt())) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
    }
    
    
    existingProject.setName(projectSaveDto.name());
    existingProject.setObjectives(projectSaveDto.objectives());
    existingProject.setBeneficiaryPopulations(projectSaveDto.beneficiaryPopulations());
    existingProject.setBudgets(projectSaveDto.budgets());
    existingProject.setStartAt(projectSaveDto.startAt());
    existingProject.setEndAt(projectSaveDto.endAt());
    
    Project updatedProject = projectRepository.save(existingProject);
    
        logAction(creatorId, ProjectActionType.PROJECT_UPDATED, "Proyecto actualizado con id " + id);
    
    return attachReview(projectMapper.toDto(updatedProject));
}

    @Override
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        projectRepository.delete(project);
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
                dto.startAt(), dto.endAt(), dto.status(), dto.creator(), null,
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
            dto.status(),
            dto.creator(),
            userMapper.toDto(curator),
            review.getNotes(),
            review.getDueAt(),
            review.getReviewedAt()
        );
    }

    @Override
    public ProjectDto addObservations(Long projectId, Long curatorId, String notes) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        if (reviews.isEmpty()) {
            throw new ResourceNotFoundException("Review", "projectId", projectId);
        }
        Review review = reviews.get(0);
        if (review.getCurator() == null || !Objects.equals(review.getCurator().getId(), curatorId)) {
            throw new AccessDeniedException("Solo el curador asignado puede registrar observaciones");
        }
        review.setNotes(notes);
        review.setReviewedAt(LocalDateTime.now());
        reviewRepository.save(review);
        project.setStatus(ProjectStatus.OBSERVACIONES);
        projectRepository.save(project);
        logAction(curatorId, ProjectActionType.PROJECT_OBSERVATIONS_ADDED, "Observaciones registradas para proyecto " + projectId);
        return attachReview(projectMapper.toDto(project));
    }

    @Override
    public ProjectDto approveProject(Long projectId, Long curatorId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        if (reviews.isEmpty()) {
            throw new ResourceNotFoundException("Review", "projectId", projectId);
        }
        Review review = reviews.get(0);
        if (review.getCurator() == null || !Objects.equals(review.getCurator().getId(), curatorId)) {
            throw new AccessDeniedException("Solo el curador asignado puede aprobar");
        }
        review.setReviewedAt(LocalDateTime.now());
        reviewRepository.save(review);
        project.setStatus(ProjectStatus.LISTO_PARA_PUBLICAR);
        projectRepository.save(project);
        logAction(curatorId, ProjectActionType.PROJECT_APPROVED, "Proyecto " + projectId + " aprobado (listo para publicar)");
        return attachReview(projectMapper.toDto(project));
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
    public ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        User newCurator = userRepository.findById(curatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", curatorId));

        boolean isCurator = newCurator.getRoles() != null && newCurator.getRoles().stream()
                .anyMatch(r -> "CURATOR".equalsIgnoreCase(r.getName()));
        if (!isCurator) {
            throw new IllegalArgumentException("El usuario no tiene rol CURATOR");
        }
        if (Objects.equals(newCurator.getId(), project.getCreator().getId())) {
            throw new IllegalArgumentException("El creador no puede ser curador de su propio proyecto");
        }

       
        List<Review> reviews = reviewRepository.findByProjectId(projectId);
        Review review;
        if (reviews.isEmpty()) {
            review = Review.builder()
                    .project(project)
                    .curator(newCurator)
                    .dueAt(LocalDateTime.now().plusDays(7))
                    .build();
        } else {
            review = reviews.get(0);
            review.setCurator(newCurator);
        }
        reviewRepository.save(review);
        logAction(adminId, ProjectActionType.CURATOR_REASSIGNED, "Curador reasignado a proyecto " + projectId + " -> usuario " + curatorId);
        return projectMapper.toDto(project);
    }

   
    private void logAction(Long userId, ProjectActionType actionType, String description) {
        if (userId == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        Action action = Action.builder()
            .name(actionType.name())
            .description(description)
            .user(user)
            .build();
        actionRepository.save(action);
    }
}
