package com.unimagdalena.conectaCiudad.Dto.voting;

import com.unimagdalena.conectaCiudad.enums.VoteType;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new vote in the local database.
 * Used when a citizen votes on a project.
 */
public record VoteSaveDto(
    @NotNull
    Long projectId,
    
    @NotNull
    VoteType voteType
) {}
