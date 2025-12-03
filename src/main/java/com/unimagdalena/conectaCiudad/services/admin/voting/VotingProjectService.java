package com.unimagdalena.conectaCiudad.services.admin.voting;

import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;

import java.util.List;


public interface VotingProjectService {


    List<VotingProjectDto> getAllVotingProjects(String token);

    List<VotingProjectDto> getOpenVotingProjects(String token);

    List<VotingProjectDto> getClosedVotingProjects(String token);
}
