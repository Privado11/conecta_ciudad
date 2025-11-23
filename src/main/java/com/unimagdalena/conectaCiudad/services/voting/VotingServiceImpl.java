package com.unimagdalena.conectaCiudad.services.voting;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.voting.UserVoteHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VoteDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingResultsDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingStatsDto;
import com.unimagdalena.conectaCiudad.clients.VotingClient;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VotingServiceImpl implements VotingService {

    private final ProjectRepository projectRepository;
    private final VotingClient votingClient;

    @Override
    public List<VotingProjectDto> getAllVotingProjects(String token) {
        log.debug("Fetching all voting projects");
        
        List<Project> openProjects = projectRepository.findByStatus(ProjectStatus.OPEN_FOR_VOTING, 
                Pageable.unpaged()).getContent();
        List<Project> closedProjects = projectRepository.findByStatus(ProjectStatus.VOTING_CLOSED, 
                Pageable.unpaged()).getContent();

        List<VotingProjectDto> allProjects = openProjects.stream()
                .sorted((p1, p2) -> {
                    if (p1.getVotingEndAt() == null) return 1;
                    if (p2.getVotingEndAt() == null) return -1;
                    return p1.getVotingEndAt().compareTo(p2.getVotingEndAt());
                })
                .map(p -> mapToVotingProjectDto(p, "OPEN", token))
                .collect(Collectors.toList());

        allProjects.addAll(closedProjects.stream()
                .sorted((p1, p2) -> {
                    if (p1.getVotingEndAt() == null) return 1;
                    if (p2.getVotingEndAt() == null) return -1;
                    return p2.getVotingEndAt().compareTo(p1.getVotingEndAt());
                })
                .map(p -> mapToVotingProjectDto(p, "CLOSED", token))
                .collect(Collectors.toList()));

        log.debug("Found {} total voting projects", allProjects.size());
        return allProjects;
    }

    @Override
    public VotingStatsDto getVotingStatistics(String token) {
        log.debug("Calculating voting statistics");
        
        List<VotingProjectDto> allProjects = getAllVotingProjects(token);
        
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
    public List<VotingProjectDto> getOpenVotingProjects(String token) {
        log.debug("Fetching open voting projects");
        
        List<Project> openProjects = projectRepository.findByStatus(ProjectStatus.OPEN_FOR_VOTING, 
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        
      
        return openProjects.stream()
                .sorted((p1, p2) -> {
                    if (p1.getVotingEndAt() == null) return 1;
                    if (p2.getVotingEndAt() == null) return -1;
                    return p1.getVotingEndAt().compareTo(p2.getVotingEndAt());
                })
                .map(p -> mapToVotingProjectDto(p, "OPEN", token))
                .collect(Collectors.toList());
    }

    @Override
    public List<VotingProjectDto> getClosedVotingProjects(String token) {
        log.debug("Fetching closed voting projects");
        
        List<Project> closedProjects = projectRepository.findByStatus(ProjectStatus.VOTING_CLOSED, 
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        
       
        return closedProjects.stream()
                .sorted((p1, p2) -> {
                    if (p1.getVotingEndAt() == null) return 1;
                    if (p2.getVotingEndAt() == null) return -1;
                    return p2.getVotingEndAt().compareTo(p1.getVotingEndAt());
                })
                .map(p -> mapToVotingProjectDto(p, "CLOSED", token))
                .collect(Collectors.toList());
    }

    private VotingProjectDto mapToVotingProjectDto(Project project, String votingStatus, String token) {
 
        VotingResultsDto votingResults = getVotingResults(project.getId(), token);
        
        long votesInFavor = votingResults != null ? votingResults.votesInFavor() : 0L;
        long votesAgainst = votingResults != null ? votingResults.votesAgainst() : 0L;
        long totalVotes = votesInFavor + votesAgainst;
        
        double participationRate = totalVotes > 0 ? (totalVotes * 100.0 / 5000.0) : 0.0;
        
        double approvalPercentage = totalVotes > 0 ? (votesInFavor * 100.0 / totalVotes) : 0.0;
        
        Long daysRemaining = null;
        Long hoursRemaining = null;
        String urgencyLevel = null;
        
        if ("OPEN".equals(votingStatus) && project.getVotingEndAt() != null) {
            LocalDate now = LocalDate.now();
            LocalDate endDate = project.getVotingEndAt();
            
            long totalDays = Duration.between(now.atStartOfDay(), endDate.atStartOfDay()).toDays();
            daysRemaining = Math.max(0, totalDays);
            
            if (daysRemaining == 0) {
                long totalHours = Duration.between(now.atStartOfDay(), endDate.atStartOfDay()).toHours();
                hoursRemaining = Math.max(0, totalHours);
            }
            
            if (daysRemaining <= 2) {
                urgencyLevel = "CRITICAL";
            } else if (daysRemaining <= 7) {
                urgencyLevel = "HIGH";
            } else {
                urgencyLevel = "NORMAL";
            }
        }
        
        String finalResult = null;
        OffsetDateTime closedAt = null;
        
        if ("CLOSED".equals(votingStatus)) {
            
            finalResult = approvalPercentage > 50.0 ? "APPROVED" : "REJECTED";
            closedAt = project.getUpdatedAt(); 
        }
        
        UserDto creatorDto = new UserDto(
                project.getCreator().getId(),
                project.getCreator().getName(),
                project.getCreator().getNationalId(),
                project.getCreator().getEmail(),
                project.getCreator().getPhone(),
                project.getCreator().getCreatedAt(),
                project.getCreator().getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()),
                project.getCreator().getActive(),
                null  
        );
        
        return new VotingProjectDto(
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
                creatorDto,
                project.getVersion(),
                votingStatus,
                votesInFavor,
                votesAgainst,
                totalVotes,
                participationRate,
                daysRemaining,
                hoursRemaining,
                urgencyLevel,
                finalResult,
                closedAt,
                approvalPercentage
        );
    }

    private VotingResultsDto getVotingResults(Long projectId, String token) {
        try {
            VotingResultsDto results = votingClient.getProjectVotingResults(projectId, token);
            
            if (results != null) {
                log.debug("Obtenidos resultados reales para proyecto {}: {} a favor, {} en contra", 
                        projectId, results.votesInFavor(), results.votesAgainst());
                return results;
            }
            
            log.debug("No se encontraron resultados para proyecto {}", projectId);
            return null;
        } catch (Exception e) {
            log.warn("Failed to get voting results for project {}: {}", projectId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<UserVoteHistoryDto> getUserVotingHistory(String token) {
        log.debug("Fetching voting history for authenticated user");
        

        List<Project> votingProjects = projectRepository.findByStatusIn(
                List.of(ProjectStatus.OPEN_FOR_VOTING, ProjectStatus.VOTING_CLOSED),
                Pageable.unpaged()
        ).getContent();
        
        log.debug("Found {} projects with voting status", votingProjects.size());
        
  
        List<UserVoteHistoryDto> userVotes = votingProjects.stream()
                .map(project -> {
                    try {
                       
                        VoteDto vote = votingClient.getUserVoteForProject(project.getId(), token);
                        
                        if (vote != null) {
                          
                            VotingResultsDto results = getVotingResults(project.getId(), token);
                            
                            long votesInFavor = results != null ? results.votesInFavor() : 0L;
                            long votesAgainst = results != null ? results.votesAgainst() : 0L;
                            long totalVotes = votesInFavor + votesAgainst;
                            
                            double approvalPercentage = totalVotes > 0 ? (votesInFavor * 100.0 / totalVotes) : 0.0;
                            
                           
                            String votingStatus = project.getStatus() == ProjectStatus.OPEN_FOR_VOTING ? "OPEN" : "CLOSED";
                            
                            
                            String finalResult = null;
                            if ("CLOSED".equals(votingStatus)) {
                                finalResult = approvalPercentage > 50.0 ? "APPROVED" : "REJECTED";
                            }
                            
                            return new UserVoteHistoryDto(
                                    vote.id(),
                                    project.getId(),
                                    project.getName(),
                                    project.getDescription(),
                                    project.getVotingStartAt(),
                                    project.getVotingEndAt(),
                                    vote.fechaHora(),
                                    vote.decision(),
                                    vote.hashVerificacion(),
                                    project.getStatus(),
                                    votingStatus,
                                    finalResult,
                                    totalVotes,
                                    votesInFavor,
                                    votesAgainst,
                                    approvalPercentage
                            );
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("Error processing vote for project {}: {}", project.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(vote -> vote != null)
                .sorted((v1, v2) -> v2.voteDate().compareTo(v1.voteDate())) 
                .collect(Collectors.toList());
        
        log.debug("Found {} votes for user", userVotes.size());
        return userVotes;
    }
}

