package com.unimagdalena.conectaCiudad.services.project;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;

@Service
public class ProjectServiceImpl implements ProjectService {
    
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    @Autowired
    public ProjectServiceImpl(ProjectRepository projectRepository, UserRepository userRepository,
                              ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
    }

    @Override
    public ProjectDto findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return projectMapper.toDto(project);
    }

    @Override
    public List<ProjectDto> findAll() {
        return projectRepository.findAll()
                .stream()
                .map(projectMapper::toDto)
                .toList();
    }

    @Override
    public List<ProjectDto> findByNameContainingIgnoreCase(String name) {
        return projectRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(projectMapper::toDto)
                .toList();
    }

    @Override
    public List<ProjectDto> findByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status)
                .stream()
                .map(projectMapper::toDto)
                .toList();
    }

    @Override
    public List<ProjectDto> findByCreatorId(Long creatorId) {
        return projectRepository.findByCreatorId(creatorId)
                .stream()
                .map(projectMapper::toDto)
                .toList();
    }

    @Override
    public ProjectDto saveProject(ProjectSaveDto projectSaveDto) {
       
        if (Objects.isNull(projectSaveDto)) {
            throw new BadRequestException("Project data cannot be null");
        }
        if (projectSaveDto.name() == null || projectSaveDto.name().trim().isEmpty()) {
            throw new BadRequestException("Project name is required");
        }
        if (projectSaveDto.objectives() == null || projectSaveDto.objectives().trim().isEmpty()) {
            throw new BadRequestException("Project objectives are required");
        }
        if (projectSaveDto.beneficiaryPopulations() == null || projectSaveDto.beneficiaryPopulations().trim().isEmpty()) {
            throw new BadRequestException("Beneficiary populations are required");
        }
        if (projectSaveDto.budgets() == null || projectSaveDto.budgets().trim().isEmpty()) {
            throw new BadRequestException("Project budgets are required");
        }
        if (projectSaveDto.startAt() == null) {
            throw new BadRequestException("Start date is required");
        }
        if (projectSaveDto.endAt() == null) {
            throw new BadRequestException("End date is required");
        }
        if (projectSaveDto.endAt().isBefore(projectSaveDto.startAt())) {
            throw new BadRequestException("End date must be after start date");
        }

        
        if (projectSaveDto.creatorId() == null) {
            throw new BadRequestException("Creator ID is required");
        }
        
        User creator = userRepository.findById(projectSaveDto.creatorId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", projectSaveDto.creatorId()));
        
        Project project = projectMapper.toEntity(projectSaveDto);
        project.setCreator(creator);
        
        
        project.setStatus(ProjectStatus.PENDIENTE);
        

        Project savedProject = projectRepository.save(project);
        return projectMapper.toDto(savedProject);
    }

    @Override
    public ProjectDto updateProject(Long id, ProjectSaveDto projectSaveDto) {
        return projectRepository.findById(id).map(existingProject -> {
            existingProject.setName(projectSaveDto.name());
            existingProject.setObjectives(projectSaveDto.objectives());
            existingProject.setBeneficiaryPopulations(projectSaveDto.beneficiaryPopulations());
            existingProject.setBudgets(projectSaveDto.budgets());
            existingProject.setStartAt(projectSaveDto.startAt());
            existingProject.setEndAt(projectSaveDto.endAt());
            
            return projectRepository.save(existingProject);
        }).map(projectMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    }

    @Override
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        projectRepository.delete(project);
    }
}
