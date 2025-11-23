package com.unimagdalena.conectaCiudad.Dto.voting;

import java.time.OffsetDateTime;

public record VoteResultDto(
        Long projectId,
        String projectName,
        Long votesInFavor,
        Long votesAgainst,
        Long totalVotes,
        Double approvalPercentage,
        String finalResult,  
        OffsetDateTime closedAt
) {
}
