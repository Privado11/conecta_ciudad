package com.unimagdalena.conectaCiudad.Dto.project;

import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.entities.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class ProjectVotingMapper {

    private final UserMapper userMapper;

    public ProjectVotingDto toDto(Project project) {
        return new ProjectVotingDto(
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
                calculateVotingInfo(project)
        );
    }

    private ProjectVotingDto.VotingActiveInfo calculateVotingInfo(Project project) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        LocalDate votingStart = project.getVotingStartAt();
        LocalDate votingEnd = project.getVotingEndAt();

        // Si la votación ya cerró, no debería llegar aquí
        if (votingEnd.isBefore(today)) {
            return new ProjectVotingDto.VotingActiveInfo(
                    false,
                    false,
                    -1L,
                    -1L,
                    0L,
                    100.0,
                    "CLOSED",
                    "Votación cerrada"
            );
        }

        long daysRemaining = ChronoUnit.DAYS.between(today, votingEnd);
        long hoursRemaining = ChronoUnit.HOURS.between(now, votingEnd.atTime(23, 59, 59));

        long totalDays = ChronoUnit.DAYS.between(votingStart, votingEnd) + 1;

        long daysPassed = ChronoUnit.DAYS.between(votingStart, today);
        if (daysPassed < 0) daysPassed = 0;

        double progress = (daysPassed * 100.0) / totalDays;
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;

        boolean isExpiringSoon = daysRemaining <= 3;

        String urgencyLevel;
        if (daysRemaining <= 1) {
            urgencyLevel = "CRITICAL";
        } else if (daysRemaining <= 3) {
            urgencyLevel = "HIGH";
        } else {
            urgencyLevel = "NORMAL";
        }

        String statusMessage;
        if (daysRemaining == 0) {
            statusMessage = String.format(
                    "¡ÚLTIMO DÍA! Cierra hoy a las 23:59 (%d horas restantes)",
                    hoursRemaining
            );
        } else if (daysRemaining == 1) {
            statusMessage = String.format(
                    "¡Cierra mañana! (%d horas restantes)",
                    hoursRemaining
            );
        } else if (daysRemaining > 1 && daysRemaining <= 3) {
            statusMessage = String.format(
                    "¡Quedan solo %d días para votar!",
                    daysRemaining
            );
        } else {
            statusMessage = String.format(
                    "Abierta - %d días restantes",
                    daysRemaining
            );
        }

        return new ProjectVotingDto.VotingActiveInfo(
                true,
                isExpiringSoon,
                daysRemaining,
                hoursRemaining,
                totalDays,
                progress,
                urgencyLevel,
                statusMessage
        );
    }
}