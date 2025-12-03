package com.unimagdalena.conectaCiudad.services.admin.project;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface ProjectAdminService {

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

    ProjectDto reassignCurator(Long projectId, Long curatorId, Long adminId, Long accessId);
}
