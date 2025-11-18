package com.unimagdalena.conectaCiudad.services.leader;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;

public interface LeaderService {
    ProjectDto createProject(ProjectSaveDto projectSaveDto, Long creatorId, Long accessId);
    ProjectDto updateProject(Long id, ProjectSaveDto projectSaveDto, Long creatorId, Long accessId);
    void deleteProject(Long id);
    ProjectDto submitForReview(Long projectId, Long creatorId, Long accessId);
}
