package com.unimagdalena.conectaCiudad.services.curator.review;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

import java.time.LocalDate;
import java.util.List;


public interface CuratorReviewService {


    ProjectDto addObservations(Long projectId, Long curatorId, String notes, Long accessId);


    ProjectDto approveProject(Long projectId, Long curatorId, LocalDate votingStartAt, 
                             LocalDate votingEndAt, Long accessId);


    List<ProjectDto> findByCurator(Long curatorId, ProjectStatus status);
}
