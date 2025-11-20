package com.unimagdalena.conectaCiudad.services.scheduled;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VotingSchedulerService {

    private final ProjectRepository projectRepository;
    private final AuditHelper auditHelper;

    @Scheduled(cron = "0 0 0 * * *")
    public void openVotingAtStartDate() {
        LocalDate today = LocalDate.now();

        List<Project> projectsToOpen = projectRepository.findProjectsToOpenVoting(today);

        if (projectsToOpen.isEmpty()) {
            return;
        }

        for (Project project : projectsToOpen) {

            ProjectStatus oldStatus = project.getStatus();
            project.setStatus(ProjectStatus.OPEN_FOR_VOTING);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("oldStatus", oldStatus.toString());
            metadata.put("newStatus", ProjectStatus.OPEN_FOR_VOTING.toString());
            metadata.put("votingStartAt", project.getVotingStartAt().toString());
            metadata.put("autoScheduler", true);

            auditHelper.logSystemAction(
                    "VOTING_AUTO_OPEN",
                    "El sistema abrió automáticamente la votación del proyecto",
                    EntityType.PROJECT,
                    project.getId(),
                    ActionResult.SUCCESS,
                    metadata
            );
        }

        projectRepository.saveAll(projectsToOpen);

        log.info("Se abrieron automáticamente {} votaciones.", projectsToOpen.size());
    }


    @Scheduled(cron = "0 00 0 * * *")
    public void closeExpiredVotations() {
        LocalDate today = LocalDate.now();

        List<Project> expiredProjects = projectRepository.findExpiredVoting(today);

        if (expiredProjects.isEmpty()) {
            return;
        }

        for (Project p : expiredProjects) {

            ProjectStatus oldStatus = p.getStatus();
            p.setStatus(ProjectStatus.VOTING_CLOSED);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("oldStatus", oldStatus.toString());
            metadata.put("newStatus", ProjectStatus.VOTING_CLOSED.toString());
            metadata.put("closedAt", today.toString());
            metadata.put("votingEndAt", p.getVotingEndAt().toString());
            metadata.put("autoScheduler", true);

            auditHelper.logSystemAction(
                    "VOTING_AUTO_CLOSE",
                    "El sistema cerró automáticamente la votación del proyecto",
                    EntityType.PROJECT,
                    p.getId(),
                    ActionResult.SUCCESS,
                    metadata
            );
        }

        projectRepository.saveAll(expiredProjects);

        log.info("Se marcaron automáticamente {} votaciones como cerradas.", expiredProjects.size());
    }
}
