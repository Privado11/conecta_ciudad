package com.unimagdalena.conectaCiudad.services.leader.voting;

import com.unimagdalena.conectaCiudad.Dto.leader.ProjectVotingResultDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.enums.VoteType;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class LeaderVotingServiceImpl implements LeaderVotingService {

    private final ProjectRepository projectRepository;
    private final VoteRepository voteRepository;

    @Override
    public List<ProjectVotingResultDto> getMyClosedVotingResults(Long creatorId, String token) {
        List<Project> closedProjects = projectRepository.findByCreatorIdAndStatus(
                creatorId,
                ProjectStatus.VOTING_CLOSED
        );

        return closedProjects.stream()
                .map(project -> {
                    long votesInFavor = voteRepository.countByProjectIdAndVoteType(
                            project.getId(), VoteType.IN_FAVOR);
                    long votesAgainst = voteRepository.countByProjectIdAndVoteType(
                            project.getId(), VoteType.AGAINST);
                    long totalVotes = votesInFavor + votesAgainst;
                    double approvalPercentage = totalVotes > 0 ? (votesInFavor * 100.0 / totalVotes) : 0.0;
                    String finalResult = approvalPercentage > 50.0 ? "APPROVED" : "REJECTED";

                    return new ProjectVotingResultDto(
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
                            project.getUpdatedAt(),
                            votesInFavor,
                            votesAgainst,
                            totalVotes,
                            approvalPercentage,
                            finalResult
                    );
                })
                .collect(Collectors.toList());
    }
}
