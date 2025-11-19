package com.unimagdalena.conectaCiudad.services.project;

import java.util.List;

import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;


    public ProjectDto findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return projectMapper.toDto(project);
    }

    @Override
    public List<ProjectDto> findReadyToPublish() {
        return projectRepository.findByStatus(ProjectStatus.READY_TO_PUBLISH, Pageable.unpaged())
            .stream()
            .map(projectMapper::toDto)
            .toList();
    }
}
