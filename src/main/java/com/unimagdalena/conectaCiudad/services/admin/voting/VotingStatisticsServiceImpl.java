package com.unimagdalena.conectaCiudad.services.admin.voting;

import com.unimagdalena.conectaCiudad.Dto.voting.UserVotingStatsDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingStatsDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.services.admin.voting.VotingProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VotingStatisticsServiceImpl implements VotingStatisticsService {

    private final VotingProjectService votingProjectService;
    private final ProjectRepository projectRepository;

    @Override
    public VotingStatsDto getVotingStatistics(String token) {
        log.debug("Calculating voting statistics");
        
        List<VotingProjectDto> allProjects = votingProjectService.getAllVotingProjects(token);
        
        long totalVotations = allProjects.size();
        long openVotations = allProjects.stream()
                .filter(p -> "OPEN".equals(p.votingStatus()))
                .count();
        long closedVotations = allProjects.stream()
                .filter(p -> "CLOSED".equals(p.votingStatus()))
                .count();
        
        long totalVotesCast = allProjects.stream()
                .mapToLong(p -> p.totalVotes() != null ? p.totalVotes() : 0L)
                .sum();
        
        double averageParticipationRate = allProjects.stream()
                .filter(p -> p.participationRate() != null)
                .mapToDouble(VotingProjectDto::participationRate)
                .average()
                .orElse(0.0);
        
        long approvedCount = allProjects.stream()
                .filter(p -> "APPROVED".equals(p.finalResult()))
                .count();
        
        long rejectedCount = allProjects.stream()
                .filter(p -> "REJECTED".equals(p.finalResult()))
                .count();
        
        double approvalRate = closedVotations > 0 ? (approvedCount * 100.0 / closedVotations) : 0.0;
        double rejectionRate = closedVotations > 0 ? (rejectedCount * 100.0 / closedVotations) : 0.0;
        
        double averageVotesPerProject = totalVotations > 0 ? (totalVotesCast * 1.0 / totalVotations) : 0.0;
        
        return new VotingStatsDto(
                totalVotations,
                openVotations,
                closedVotations,
                totalVotesCast,
                averageParticipationRate,
                approvalRate,
                rejectionRate,
                averageVotesPerProject
        );
    }

    @Override
    public UserVotingStatsDto getUserVotingStats(String token) {
        log.debug("Calculating voting statistics for authenticated user");
        
        List<Project> closedProjects = projectRepository.findByStatus(ProjectStatus.VOTING_CLOSED, 
                Pageable.unpaged()).getContent();
        
        log.debug("Found {} closed voting projects", closedProjects.size());
        
        long totalVotes = 0;
        long votesInFavor = 0;
        long votesAgainst = 0;
        
        double participationRate = closedProjects.size() > 0 
                ? (totalVotes * 1.0 / closedProjects.size()) 
                : 0.0;
        
        log.debug("User stats - Total: {}, In Favor: {}, Against: {}, Participation: {}", 
                totalVotes, votesInFavor, votesAgainst, participationRate);
        
        return new UserVotingStatsDto(
                totalVotes,
                votesInFavor,
                votesAgainst,
                participationRate
        );
    }
}
