package com.unimagdalena.conectaCiudad.services.voting;

import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingStatsDto;

import java.util.List;

public interface VotingService {

   
    List<VotingProjectDto> getAllVotingProjects(String token);

 
    VotingStatsDto getVotingStatistics(String token);

 
    List<VotingProjectDto> getOpenVotingProjects(String token);


    List<VotingProjectDto> getClosedVotingProjects(String token);
}
