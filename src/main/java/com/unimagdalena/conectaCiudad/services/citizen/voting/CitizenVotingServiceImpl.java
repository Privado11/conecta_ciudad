package com.unimagdalena.conectaCiudad.services.citizen.voting;

import com.unimagdalena.conectaCiudad.Dto.voting.VoteResponseDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VoteSaveDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.entities.Vote;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.enums.UserActionType;
import com.unimagdalena.conectaCiudad.events.ActionFailedEvent;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.DuplicateResourceException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.repositories.VoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenVotingServiceImpl implements CitizenVotingService {

    private final VoteRepository voteRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public VoteResponseDto submitVote(VoteSaveDto voteSaveDto, Long voterId, Long accessId) {
        log.info("Citizen {} submitting vote for project {}", voterId, voteSaveDto.projectId());

        try {
            Project project = findProjectById(voteSaveDto.projectId());
            validateProjectIsOpenForVoting(project);

            User voter = findUserById(voterId);

            if (voteRepository.existsByProjectIdAndVoterId(voteSaveDto.projectId(), voterId)) {
                throw new DuplicateResourceException(
                        ErrorCode.VOTE_ALREADY_EXISTS,
                        Map.of(
                                "projectId", voteSaveDto.projectId(),
                                "voterId", voterId
                        )
                );
            }

            Vote vote = Vote.builder()
                    .project(project)
                    .voter(voter)
                    .voteType(voteSaveDto.voteType())
                    .build();

            Vote savedVote = voteRepository.save(vote);

            log.info("Vote submitted successfully: {} for project {} by user {}", 
                    savedVote.getVoteType(), project.getId(), voter.getId());

            return mapToVoteResponseDto(savedVote);

        } catch (Exception e) {
            eventPublisher.publishEvent(new ActionFailedEvent(
                    this,
                    UserActionType.CITIZEN_VOTE.name(),
                    "Error submitting vote",
                    e,
                    EntityType.PROJECT,
                    voteSaveDto.projectId(),
                    findUserById(voterId)
            ));
            throw e;
        }
    }

    @Override
    public boolean hasUserVoted(Long projectId, Long voterId) {
        return voteRepository.existsByProjectIdAndVoterId(projectId, voterId);
    }

    @Override
    public VoteResponseDto getUserVoteForProject(Long projectId, Long voterId) {
        Optional<Vote> voteOpt = voteRepository.findByProjectIdAndVoterId(projectId, voterId);
        return voteOpt.map(this::mapToVoteResponseDto).orElse(null);
    }


    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private void validateProjectIsOpenForVoting(Project project) {
        if (project.getStatus() != ProjectStatus.OPEN_FOR_VOTING) {
            throw new BadRequestException(
                    ErrorCode.PROJECT_NOT_OPEN_FOR_VOTING,
                    Map.of("currentStatus", project.getStatus().name())
            );
        }

    
        LocalDate today = LocalDate.now();
        
        if (project.getVotingStartAt() == null || project.getVotingEndAt() == null) {
            throw new BadRequestException(ErrorCode.VOTING_DATES_NOT_SET);
        }

        if (today.isBefore(project.getVotingStartAt())) {
            throw new BadRequestException(
                    ErrorCode.VOTING_NOT_STARTED,
                    Map.of("votingStartDate", project.getVotingStartAt().toString())
            );
        }

        if (today.isAfter(project.getVotingEndAt())) {
            throw new BadRequestException(
                    ErrorCode.VOTING_ALREADY_ENDED,
                    Map.of("votingEndDate", project.getVotingEndAt().toString())
            );
        }
    }

    private VoteResponseDto mapToVoteResponseDto(Vote vote) {
        return new VoteResponseDto(
                vote.getId(),
                vote.getProject().getId(),
                vote.getProject().getName(),
                vote.getVoter().getId(),
                vote.getVoter().getName(),
                vote.getVoteType(),
                vote.getVotedAt()
        );
    }
}
