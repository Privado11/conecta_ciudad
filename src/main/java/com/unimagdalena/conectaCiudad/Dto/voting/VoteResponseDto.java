package com.unimagdalena.conectaCiudad.Dto.voting;

import com.unimagdalena.conectaCiudad.enums.VoteType;
import java.time.OffsetDateTime;

/**
 * DTO for vote response.
 * Provides clean vote information without exposing internal entity details.
 */
public record VoteResponseDto(
    Long id,
    Long projectId,
    String projectName,
    Long voterId,
    String voterName,
    VoteType voteType,
    OffsetDateTime votedAt
) {}
