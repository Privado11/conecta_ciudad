package com.unimagdalena.conectaCiudad.services.scheduled;

import com.unimagdalena.conectaCiudad.entities.Project;
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


    @Scheduled(cron = "0 01 0 * * *")
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

            auditHelper.logComplete(
                    "VOTING_AUTO_CLOSE",
                    "El sistema cerró automáticamente la votación del proyecto",
                    com.unimagdalena.conectaCiudad.enums.EntityType.PROJECT,
                    p.getId(),
                    com.unimagdalena.conectaCiudad.enums.ActionResult.SUCCESS,
                    metadata
            );
        }

        projectRepository.saveAll(expiredProjects);

        log.info("⚙️ Se marcaron automáticamente {} votaciones como cerradas.", expiredProjects.size());
    }
}
