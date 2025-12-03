package com.unimagdalena.conectaCiudad.services.admin.voting;

import com.unimagdalena.conectaCiudad.Dto.voting.UserVotingStatsDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingStatsDto;

public interface VotingStatisticsService {

    VotingStatsDto getVotingStatistics(String token);

    UserVotingStatsDto getUserVotingStats(String token);
}
