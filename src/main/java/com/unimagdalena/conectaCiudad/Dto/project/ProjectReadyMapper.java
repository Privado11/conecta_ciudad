package com.unimagdalena.conectaCiudad.Dto.project;

import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.entities.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class ProjectReadyMapper {

    private final UserMapper userMapper;

    public ProjectReadyDto toDto(Project project) {
        return new ProjectReadyDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getObjectives(),
                project.getBeneficiaryPopulations(),
                project.getBudget(),
                project.getStartAt(),
                project.getEndAt(),
                project.getVotingStartAt(),
                project.getVotingEndAt(),
                project.getCreatedAt(),
                project.getStatus(),
                userMapper.toDto(project.getCreator()),
                project.getVersion(),
                calculateScheduleInfo(project)
        );
    }

    private ProjectReadyDto.VotingScheduleInfo calculateScheduleInfo(Project project) {
        LocalDate today = LocalDate.now();

        if (project.getVotingStartAt() == null || project.getVotingEndAt() == null) {
            return new ProjectReadyDto.VotingScheduleInfo(
                    false,
                    null,
                    null,
                    "Sin fechas de votación programadas"
            );
        }

        long daysUntilStart = ChronoUnit.DAYS.between(today, project.getVotingStartAt());
        long votingDuration = ChronoUnit.DAYS.between(
                project.getVotingStartAt(),
                project.getVotingEndAt()
        ) + 1;

        String scheduleStatus;

        if (daysUntilStart < 0) {
            scheduleStatus = "La votación ya inició";
        } else if (daysUntilStart == 0) {
            scheduleStatus = String.format(
                    "La votación inicia HOY (duración: %d días)",
                    votingDuration
            );
        } else {
            scheduleStatus = String.format(
                    "Votación programada para el %s (inicia en %d días, duración: %d días)",
                    project.getVotingStartAt(),
                    daysUntilStart,
                    votingDuration
            );
        }

        return new ProjectReadyDto.VotingScheduleInfo(
                true,
                daysUntilStart,
                votingDuration,
                scheduleStatus
        );
    }
}
