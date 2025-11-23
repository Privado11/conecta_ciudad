package com.unimagdalena.conectaCiudad.Dto.leader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ProjectVotingResultDto(
        Long projectId,
        String projectName,
        String description,
        String objectives,
        String beneficiaryPopulations,
        BigDecimal budget,
        LocalDate projectStartAt,
        LocalDate projectEndAt,
        LocalDate votingStartAt,
        LocalDate votingEndAt,
        OffsetDateTime closedAt,
        Long votesInFavor,
        Long votesAgainst,
        Long totalVotes,
        Double approvalPercentage,
        String finalResult
) {}
