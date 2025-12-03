package com.unimagdalena.conectaCiudad.services.citizen.voting;

import com.unimagdalena.conectaCiudad.Dto.voting.UserVoteHistoryDto;

import java.util.List;

public interface CitizenVotingHistoryService {

    List<UserVoteHistoryDto> getUserVotingHistory(String token);
}
