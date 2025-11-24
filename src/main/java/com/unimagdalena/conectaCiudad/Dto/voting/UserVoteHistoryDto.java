package com.unimagdalena.conectaCiudad.Dto.voting;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record UserVoteHistoryDto(
        Long id,
        String name,
        String description,
        String objectives,
        String beneficiaryPopulations,
        BigDecimal budget,
        LocalDate startAt,
        LocalDate endAt,
        LocalDate votingStartAt,
        LocalDate votingEndAt,
        OffsetDateTime createdAt,
        ProjectStatus status,
        UserDto creator,
        Long version,


        String votingStatus,
        Long votesInFavor,
        Long votesAgainst,
        Long totalVotes,
        Double participationRate,


        Long daysRemaining,
        Long hoursRemaining,
        String urgencyLevel,

        // For closed votings
        String finalResult,
        OffsetDateTime closedAt,
        Double approvalPercentage
) {}
