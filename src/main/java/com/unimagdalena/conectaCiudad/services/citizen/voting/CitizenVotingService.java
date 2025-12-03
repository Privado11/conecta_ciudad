package com.unimagdalena.conectaCiudad.services.citizen.voting;

import com.unimagdalena.conectaCiudad.Dto.voting.VoteResponseDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VoteSaveDto;


public interface CitizenVotingService {

    VoteResponseDto submitVote(VoteSaveDto voteSaveDto, Long voterId, Long accessId);

    boolean hasUserVoted(Long projectId, Long voterId);

    VoteResponseDto getUserVoteForProject(Long projectId, Long voterId);
}
