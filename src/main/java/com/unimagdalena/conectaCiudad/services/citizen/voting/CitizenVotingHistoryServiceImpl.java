package com.unimagdalena.conectaCiudad.services.citizen.voting;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.voting.UserVoteHistoryDto;
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
public class CitizenVotingHistoryServiceImpl implements CitizenVotingHistoryService {

    private final ProjectRepository projectRepository;
    private final VoteRepository voteRepository;

    @Override
    public List<UserVoteHistoryDto> getUserVotingHistory(String token) {
        log.debug("Fetching voting history for authenticated citizen");
        
        List<Project> votingProjects = projectRepository.findByStatusIn(
                List.of(ProjectStatus.OPEN_FOR_VOTING, ProjectStatus.VOTING_CLOSED),
                Pageable.unpaged()
        ).getContent();
        
        log.debug("Found {} projects with voting status", votingProjects.size());
        
        List<UserVoteHistoryDto> userVotes = votingProjects.stream()
                .map(project -> {
                    try {
                        return mapToUserVoteHistoryDto(project);
                    } catch (Exception e) {
                        log.warn("Error mapping vote history for project {}: {}", project.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(vote -> vote != null)
                .sorted((v1, v2) -> v2.createdAt().compareTo(v1.createdAt()))
                .collect(Collectors.toList());
        
        log.debug("Found {} votes for citizen", userVotes.size());
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
            long votesFavor = voteRepository.countByProjectIdAndVoteType(project.getId(), VoteType.IN_FAVOR);
            long votesAgst = voteRepository.countByProjectIdAndVoteType(project.getId(), VoteType.AGAINST);
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
}
