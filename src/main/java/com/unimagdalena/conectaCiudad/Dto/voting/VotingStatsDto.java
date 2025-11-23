package com.unimagdalena.conectaCiudad.Dto.voting;

public record VotingStatsDto(
        Long totalVotations,
        Long openVotations,
        Long closedVotations,
        Long totalVotesCast,
        Double averageParticipationRate,
        Double approvalRate,
        Double rejectionRate,
        Double averageVotesPerProject
) {
}
