package com.unimagdalena.conectaCiudad.services.leader.voting;

import com.unimagdalena.conectaCiudad.Dto.leader.ProjectVotingResultDto;

import java.util.List;

public interface LeaderVotingService {

    List<ProjectVotingResultDto> getMyClosedVotingResults(Long creatorId, String token);
}
