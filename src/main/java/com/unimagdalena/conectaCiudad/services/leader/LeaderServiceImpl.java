package com.unimagdalena.conectaCiudad.services.leader;

import com.unimagdalena.conectaCiudad.Dto.leader.ProjectVotingResultDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.services.leader.project.LeaderProjectService;
import com.unimagdalena.conectaCiudad.services.leader.voting.LeaderVotingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class LeaderServiceImpl implements LeaderService {

    private final LeaderProjectService leaderProjectService;
    private final LeaderVotingService leaderVotingService;



    @Override
    public ProjectDto createProject(ProjectSaveDto projectSaveDto, Long creatorId, Long accessId) {
        return leaderProjectService.createProject(projectSaveDto, creatorId, accessId);
    }

    @Override
    public ProjectDto updateProject(Long id, ProjectSaveDto projectSaveDto, Long creatorId, Long accessId) {
        return leaderProjectService.updateProject(id, projectSaveDto, creatorId, accessId);
    }

    @Override
    public void deleteProject(Long id) {
        leaderProjectService.deleteProject(id);
    }

    @Override
    public ProjectDto submitForReview(Long projectId, Long creatorId, Long accessId) {
        return leaderProjectService.submitForReview(projectId, creatorId, accessId);
    }

    @Override
    public List<ProjectDto> getMyProjects(Long creatorId) {
        return leaderProjectService.getMyProjects(creatorId);
    }


    @Override
    public List<ProjectVotingResultDto> getMyClosedVotingResults(Long creatorId, String token) {
        return leaderVotingService.getMyClosedVotingResults(creatorId, token);
    }
}
