package com.unimagdalena.conectaCiudad.services.voting.mapper;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.enums.VoteType;
import com.unimagdalena.conectaCiudad.repositories.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;


@Slf4j
@Component
@RequiredArgsConstructor
public class VotingDtoMapper {

    private final VoteRepository voteRepository;

    public VotingProjectDto mapToVotingProjectDto(Project project, String votingStatus, String token) {
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
        
        UserDto creatorDto = mapUserToDto(project.getCreator());
        
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

    private UserDto mapUserToDto(com.unimagdalena.conectaCiudad.entities.User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getNationalId(),
                user.getEmail(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()),
                user.getActive(),
                null
        );
    }

    public record VotingResults(long votesInFavor, long votesAgainst) {}
}
