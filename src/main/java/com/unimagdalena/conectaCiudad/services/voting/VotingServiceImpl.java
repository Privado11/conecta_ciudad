package com.unimagdalena.conectaCiudad.services.voting;

import com.unimagdalena.conectaCiudad.Dto.voting.UserVoteHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.voting.UserVotingStatsDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingStatsDto;
import com.unimagdalena.conectaCiudad.services.citizen.voting.CitizenVotingHistoryService;
import com.unimagdalena.conectaCiudad.services.admin.voting.VotingProjectService;
import com.unimagdalena.conectaCiudad.services.admin.voting.VotingStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VotingServiceImpl implements VotingService {

    private final VotingProjectService votingProjectService;
    private final VotingStatisticsService votingStatisticsService;
    private final CitizenVotingHistoryService citizenVotingHistoryService;


    @Override
    public List<VotingProjectDto> getAllVotingProjects(String token) {
        return votingProjectService.getAllVotingProjects(token);
    }

    @Override
    public List<VotingProjectDto> getOpenVotingProjects(String token) {
        return votingProjectService.getOpenVotingProjects(token);
    }

    @Override
    public List<VotingProjectDto> getClosedVotingProjects(String token) {
        return votingProjectService.getClosedVotingProjects(token);
    }

    @Override
    public VotingStatsDto getVotingStatistics(String token) {
        return votingStatisticsService.getVotingStatistics(token);
    }

    @Override
    public UserVotingStatsDto getUserVotingStats(String token) {
        return votingStatisticsService.getUserVotingStats(token);
    }

    @Override
    public List<UserVoteHistoryDto> getUserVotingHistory(String token) {
        return citizenVotingHistoryService.getUserVotingHistory(token);
    }
}

