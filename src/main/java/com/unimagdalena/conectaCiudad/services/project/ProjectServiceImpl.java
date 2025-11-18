package com.unimagdalena.conectaCiudad.services.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectMapper;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Review;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.ProjectActionType;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.ReviewRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;
import com.unimagdalena.conectaCiudad.specifications.ProjectSpecifications;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    
    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final ProjectMapper projectMapper;


    public ProjectDto findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return projectMapper.toDto(project);
    }


    @Override
    public Statistics<ProjectDto> getGlobalStatistics() {

    List<Project> all = projectRepository.findAll();

    Map<ProjectStatus, Long> countByStatus = new EnumMap<>(ProjectStatus.class);

    for (ProjectStatus s : ProjectStatus.values()) {
        long count = all.stream()
                .filter(p -> p.getStatus() == s)
                .count();
        countByStatus.put(s, count);
    }

    long total = all.size();

    long createdToday = all.stream()
            .filter(p -> p.getCreatedAt().isAfter(
                    OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset())
            )).count();

    return new Statistics<>(
        total,
        Map.of(
            "total", total,
            "pending", countByStatus.getOrDefault(ProjectStatus.PENDING_REVIEW, 0L),
            "inReview", countByStatus.getOrDefault(ProjectStatus.IN_REVIEW, 0L),
            "withObservations", countByStatus.getOrDefault(ProjectStatus.RETURNED_WITH_OBSERVATIONS, 0L),
            "readyToPublish", countByStatus.getOrDefault(ProjectStatus.READY_TO_PUBLISH, 0L),
            "published", countByStatus.getOrDefault(ProjectStatus.PUBLISHED, 0L),
            "votingClosed", countByStatus.getOrDefault(ProjectStatus.VOTING_CLOSED, 0L),

            "inProgress",
            countByStatus.getOrDefault(ProjectStatus.PENDING_REVIEW, 0L)
            + countByStatus.getOrDefault(ProjectStatus.IN_REVIEW, 0L)
            + countByStatus.getOrDefault(ProjectStatus.RETURNED_WITH_OBSERVATIONS, 0L),


            "completed",
                countByStatus.getOrDefault(ProjectStatus.READY_TO_PUBLISH, 0L)
                + countByStatus.getOrDefault(ProjectStatus.PUBLISHED, 0L)
                + countByStatus.getOrDefault(ProjectStatus.VOTING_CLOSED, 0L),

            "createdToday", createdToday
        )
    );
}

    @Override
    public List<ProjectDto> findReadyToPublish() {
        return projectRepository.findByStatus(ProjectStatus.READY_TO_PUBLISH, Pageable.unpaged())
            .stream()
            .map(projectMapper::toDto)
            .toList();
    }
}
