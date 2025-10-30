package com.unimagdalena.conectaCiudad.services.project;

import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

public interface ProjectService {
    ProjectDto findById(Long id);
    List<ProjectDto> findAll();
    List<ProjectDto> findByNameContainingIgnoreCase(String name);
    List<ProjectDto> findByStatus(ProjectStatus status);
    List<ProjectDto> findByCreatorId(Long creatorId);
    ProjectDto saveProject(ProjectSaveDto projectSaveDto, Long creatorId);
    ProjectDto updateProject(Long id, ProjectSaveDto projectSaveDto);
    void deleteProject(Long id);
    ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId);
    ProjectDto addObservations(Long projectId, Long curatorId, String notes);
    ProjectDto approveProject(Long projectId, Long curatorId);
    java.util.List<ProjectDto> findByCurator(Long curatorId, ProjectStatus status);
}
