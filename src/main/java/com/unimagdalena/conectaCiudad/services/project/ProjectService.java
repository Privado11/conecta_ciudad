package com.unimagdalena.conectaCiudad.services.project;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

public interface ProjectService {
    ProjectDto findById(Long id);


    List<ProjectDto> findReadyToPublish();
    Statistics<ProjectDto> getGlobalStatistics();
}