package com.unimagdalena.conectaCiudad.services.project;

import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectReadyDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectVotingDto;


public interface ProjectService {
    ProjectDto findById(Long id);
    List<ProjectDto> findReadyToPublish();
    List<ProjectReadyDto> findReadyToPublishNotOpen();
    List<ProjectVotingDto> findOpenForVoting(Long citizenId, String token);

}