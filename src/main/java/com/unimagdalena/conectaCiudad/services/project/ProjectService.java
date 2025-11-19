package com.unimagdalena.conectaCiudad.services.project;

import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;


public interface ProjectService {
    ProjectDto findById(Long id);


    List<ProjectDto> findReadyToPublish();

}