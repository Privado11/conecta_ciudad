package com.unimagdalena.conectaCiudad.Dto.voting;

public record UserVotingStatsDto(
        Long totalVotes,
        Long votesInFavor,
        Long votesAgainst,
        Double participationRate
) {}
