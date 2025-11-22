package com.unimagdalena.conectaCiudad.services.project;

import java.time.LocalDate;
import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.project.*;
import com.unimagdalena.conectaCiudad.Dto.voting.VoteDto;
import com.unimagdalena.conectaCiudad.clients.VotingClient;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ProjectReadyMapper projectReadyMapper;
    private final ProjectVotingMapper projectVotingMapper;
    private final VotingClient votingClient;


    public ProjectDto findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return projectMapper.toDto(project);
    }

    @Override
    public List<ProjectDto> findReadyToPublish() {
        return projectRepository.findByStatus(ProjectStatus.READY_TO_PUBLISH, Pageable.unpaged())
            .stream()
            .map(projectMapper::toDto)
            .toList();
    }

     @Override
    public List<ProjectReadyDto> findReadyToPublishNotOpen() {
        LocalDate today = LocalDate.now();
        return projectRepository.findReadyToPublishNotOpenForVoting(today)
            .stream()
            .map(projectReadyMapper::toDto)
            .toList();
    }

    @Override
    public List<ProjectVotingDto> findOpenForVoting(Long citizenId, String token) {
        LocalDate today = LocalDate.now();
        return projectRepository.findOpenForVoting(today)
                .stream()
                .map(project -> projectVotingMapper.toDto(project, citizenId, token))
                .toList();
    }


}
