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

    Page<ProjectDto> findWithFilters(
        String searchTerm,
        ProjectStatus status,
        Long creatorId,
        Long curatorId,
        LocalDate projectStartFrom,
        LocalDate projectStartTo,
        LocalDate projectEndFrom,
        LocalDate projectEndTo,
        LocalDate votingStartFrom,
        LocalDate votingStartTo,
        LocalDate votingEndFrom,
        LocalDate votingEndTo,
        OffsetDateTime createdFrom,
        OffsetDateTime createdTo,
        Pageable pageable
    );
    
    ProjectDto saveProject(ProjectSaveDto projectSaveDto, Long creatorId, Long accessId);
    ProjectDto updateProject(Long id, ProjectSaveDto projectSaveDto, Long creatorId, Long accessId);
    void deleteProject(Long id);
    ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId, Long accessId);
    ProjectDto addObservations(Long projectId, Long curatorId, String notes, Long accessId);
    ProjectDto approveProject(Long projectId, Long curatorId, LocalDate votingStartAt,
    LocalDate votingEndAt,Long accessId);
    List<ProjectDto> findByCurator(Long curatorId, ProjectStatus status);
    List<ProjectDto> findReadyToPublish(); 
    ProjectDto submitForReview(Long projectId, Long creatorId, Long accessId);
    Statistics<ProjectDto> getGlobalStatistics();
}