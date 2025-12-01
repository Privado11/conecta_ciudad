package com.unimagdalena.conectaCiudad.services.scheduled;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.events.VotingClosedEvent;
import com.unimagdalena.conectaCiudad.events.VotingOpenedEvent;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VotingSchedulerService {

    private final ProjectRepository projectRepository;
    private final VoteRepository voteRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 0 * * *")
    public void openVotingAtStartDate() {
        LocalDate today = LocalDate.now();

        List<Project> projectsToOpen = projectRepository.findProjectsToOpenVoting(today);

        if (projectsToOpen.isEmpty()) {
            return;
        }

        for (Project project : projectsToOpen) {
            project.setStatus(ProjectStatus.OPEN_FOR_VOTING);
            projectRepository.save(project);
            
            eventPublisher.publishEvent(new VotingOpenedEvent(this, project));
        }

        log.info("Automatically opened {} votings.", projectsToOpen.size());
    }


    @Scheduled(cron = "0 00 0 * * *")
    public void closeExpiredVotations() {
        LocalDate today = LocalDate.now();

        List<Project> expiredProjects = projectRepository.findExpiredVoting(today);

        if (expiredProjects.isEmpty()) {
            return;
        }

        for (Project project : expiredProjects) {
            project.setStatus(ProjectStatus.VOTING_CLOSED);
            projectRepository.save(project);
            
            long totalVotes = voteRepository.countByProjectId(project.getId());
            eventPublisher.publishEvent(new VotingClosedEvent(this, project, totalVotes));
        }

        log.info("Automatically marked {} votings as closed.", expiredProjects.size());
    }
}
