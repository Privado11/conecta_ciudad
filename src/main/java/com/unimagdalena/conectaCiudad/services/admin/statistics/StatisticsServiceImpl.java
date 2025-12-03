package com.unimagdalena.conectaCiudad.services.admin.statistics;

import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final ProjectRepository projectRepository;

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
}
