package com.unimagdalena.conectaCiudad.Dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

public record ProjectVotingDto(
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
    VotingActiveInfo votingInfo
) {
    public record VotingActiveInfo(
        boolean isOpen,
        boolean isExpiringSoon,
        Long daysRemaining,
        Long hoursRemaining,
        Long totalVotingDays,
        Double progressPercentage,
        String urgencyLevel, 
        String statusMessage
    ) {}
}