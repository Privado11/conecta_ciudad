package com.unimagdalena.conectaCiudad.services.leader.project;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;

import java.util.List;

public interface LeaderProjectService {

    ProjectDto createProject(ProjectSaveDto projectSaveDto, Long creatorId, Long accessId);

  
    ProjectDto updateProject(Long id, ProjectSaveDto projectSaveDto, Long creatorId, Long accessId);

   
    void deleteProject(Long id);

   
    ProjectDto submitForReview(Long projectId, Long creatorId, Long accessId);

    
    List<ProjectDto> getMyProjects(Long creatorId);
}
