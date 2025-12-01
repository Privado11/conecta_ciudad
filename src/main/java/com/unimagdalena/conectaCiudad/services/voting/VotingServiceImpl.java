package com.unimagdalena.conectaCiudad.services.voting;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.voting.UserVoteHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingStatsDto;
import com.unimagdalena.conectaCiudad.Dto.voting.UserVotingStatsDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.enums.VoteType;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.VoteRepository;
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
    private final VoteRepository voteRepository;

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
 
        VotingResults votingResults = getVotingResults(project.getId());
        
        long votesInFavor = votingResults.votesInFavor();
        long votesAgainst = votingResults.votesAgainst();
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

    private VotingResults getVotingResults(Long projectId) {
        long votesInFavor = voteRepository.countByProjectIdAndVoteType(projectId, VoteType.IN_FAVOR);
        long votesAgainst = voteRepository.countByProjectIdAndVoteType(projectId, VoteType.AGAINST);
        
        log.debug("Obtenidos resultados para proyecto {}: {} a favor, {} en contra", 
                projectId, votesInFavor, votesAgainst);
        
        return new VotingResults(votesInFavor, votesAgainst);
    }
    
    // Helper record to hold voting results
    private record VotingResults(long votesInFavor, long votesAgainst) {}

    @Override
    public List<UserVoteHistoryDto> getUserVotingHistory(String token) {
        log.debug("Fetching voting history for authenticated user");
        
        
        List<Project> votingProjects = projectRepository.findByStatusIn(
                List.of(ProjectStatus.OPEN_FOR_VOTING, ProjectStatus.VOTING_CLOSED),
                Pageable.unpaged()
        ).getContent();
        
        log.debug("Found {} projects with voting status", votingProjects.size());
        
        
        // Get the authenticated user ID from the token (assuming it's passed in the context)
        // For now, we'll need to get it from SecurityContext or pass it as parameter
        // This is a simplified version - you may need to adjust based on your auth setup
        
        List<UserVoteHistoryDto> userVotes = votingProjects.stream()
                .map(project -> {
                    try {
                        // Note: You'll need to pass the actual userId instead of extracting from token
                        // This is a placeholder - adjust based on your authentication setup
                        return mapToUserVoteHistoryDto(project);
                    } catch (Exception e) {
                        log.warn("Error mapping vote history for project {}: {}", project.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(vote -> vote != null)
                .sorted((v1, v2) -> v2.createdAt().compareTo(v1.createdAt())) 
                .collect(Collectors.toList());
        
        log.debug("Found {} votes for user", userVotes.size());
        return userVotes;
    }

    private UserVoteHistoryDto mapToUserVoteHistoryDto(Project project) {
      
        String votingStatus = project.getStatus() == ProjectStatus.OPEN_FOR_VOTING ? "OPEN" : "CLOSED";
        
        
        Long votesInFavor = null;
        Long votesAgainst = null;
        Long totalVotes = null;
        Double participationRate = null;
        Double approvalPercentage = null;
        String finalResult = null;
        OffsetDateTime closedAt = null;
        
      
        Long daysRemaining = null;
        Long hoursRemaining = null;
        String urgencyLevel = null;
        
        if ("OPEN".equals(votingStatus)) {
           
            if (project.getVotingEndAt() != null) {
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
        } else {
            
            VotingResults votingResults = getVotingResults(project.getId());
            
            long votesFavor = votingResults.votesInFavor();
            long votesAgst = votingResults.votesAgainst();
            long total = votesFavor + votesAgst;
            
            votesInFavor = votesFavor;
            votesAgainst = votesAgst;
            totalVotes = total;
            participationRate = total > 0 ? (total * 100.0 / 5000.0) : 0.0;
            approvalPercentage = total > 0 ? (votesFavor * 100.0 / total) : 0.0;
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
        
        return new UserVoteHistoryDto(
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

    @Override
    public UserVotingStatsDto getUserVotingStats(String token) {
        log.debug("Calculating voting statistics for authenticated user");
        
        
        List<Project> closedProjects = projectRepository.findByStatus(ProjectStatus.VOTING_CLOSED, 
                Pageable.unpaged()).getContent();
        
        log.debug("Found {} closed voting projects", closedProjects.size());
               // Note: This method needs userId to be passed as parameter
        // For now, returning placeholder stats - adjust based on your auth setup
        // You'll need to modify the service interface to accept userId
        
        long totalVotes = 0;
        long votesInFavor = 0;
        long votesAgainst = 0;
        
        // TODO: Get actual userId from security context or pass as parameter
        // Example implementation (commented out until userId is available):
        // List<Vote> userVotes = voteRepository.findByVoterId(userId);
        // totalVotes = userVotes.size();
        // votesInFavor = userVotes.stream().filter(v -> v.getVoteType() == VoteType.IN_FAVOR).count();
        // votesAgainst = userVotes.stream().filter(v -> v.getVoteType() == VoteType.AGAINST).count();
        
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

