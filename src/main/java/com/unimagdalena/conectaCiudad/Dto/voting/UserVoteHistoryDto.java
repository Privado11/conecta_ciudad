package com.unimagdalena.conectaCiudad.Dto.voting;

import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserVoteHistoryDto(
        Long voteId,
        Long projectId,
        String projectName,
        String projectDescription,
        LocalDate votingStartAt,
        LocalDate votingEndAt,
        LocalDateTime voteDate,
        Boolean voteDecision,
        String hashVerificacion,
        ProjectStatus projectStatus,
        String votingStatus,
        String finalResult,
        Long totalVotes,
        Long votesInFavor,
        Long votesAgainst,
        Double approvalPercentage
) {}
