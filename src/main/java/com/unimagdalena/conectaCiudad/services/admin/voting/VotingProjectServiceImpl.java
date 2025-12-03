package com.unimagdalena.conectaCiudad.services.admin.voting;

import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.services.voting.mapper.VotingDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VotingProjectServiceImpl implements VotingProjectService {

    private final ProjectRepository projectRepository;
    private final VotingDtoMapper votingDtoMapper;

    @Override
    public List<VotingProjectDto> getAllVotingProjects(String token) {
        log.debug("Fetching all voting projects");
        
        List<Project> openProjects = projectRepository.findByStatus(ProjectStatus.OPEN_FOR_VOTING, 
                Pageable.unpaged()).getContent();
        List<Project> closedProjects = projectRepository.findByStatus(ProjectStatus.VOTING_CLOSED, 
                Pageable.unpaged()).getContent();

        List<VotingProjectDto> allProjects = openProjects.stream()
                .sorted((p1, p2) -> {
                    if (p1.getVotingEndAt() == null) return 1;
                    if (p2.getVotingEndAt() == null) return -1;
                    return p1.getVotingEndAt().compareTo(p2.getVotingEndAt());
                })
                .map(p -> votingDtoMapper.mapToVotingProjectDto(p, "OPEN", token))
                .collect(Collectors.toList());

        allProjects.addAll(closedProjects.stream()
                .sorted((p1, p2) -> {
                    if (p1.getVotingEndAt() == null) return 1;
                    if (p2.getVotingEndAt() == null) return -1;
                    return p2.getVotingEndAt().compareTo(p1.getVotingEndAt());
                })
                .map(p -> votingDtoMapper.mapToVotingProjectDto(p, "CLOSED", token))
                .collect(Collectors.toList()));

        log.debug("Found {} total voting projects", allProjects.size());
        return allProjects;
    }

    @Override
    public List<VotingProjectDto> getOpenVotingProjects(String token) {
        log.debug("Fetching open voting projects");
        
        List<Project> openProjects = projectRepository.findByStatus(ProjectStatus.OPEN_FOR_VOTING, 
                Pageable.unpaged()).getContent();
        
        return openProjects.stream()
                .sorted((p1, p2) -> {
                    if (p1.getVotingEndAt() == null) return 1;
                    if (p2.getVotingEndAt() == null) return -1;
                    return p1.getVotingEndAt().compareTo(p2.getVotingEndAt());
                })
                .map(p -> votingDtoMapper.mapToVotingProjectDto(p, "OPEN", token))
                .collect(Collectors.toList());
    }

    @Override
    public List<VotingProjectDto> getClosedVotingProjects(String token) {
        log.debug("Fetching closed voting projects");
        
        List<Project> closedProjects = projectRepository.findByStatus(ProjectStatus.VOTING_CLOSED, 
                Pageable.unpaged()).getContent();
        
        return closedProjects.stream()
                .sorted((p1, p2) -> {
                    if (p1.getVotingEndAt() == null) return 1;
                    if (p2.getVotingEndAt() == null) return -1;
                    return p2.getVotingEndAt().compareTo(p1.getVotingEndAt());
                })
                .map(p -> votingDtoMapper.mapToVotingProjectDto(p, "CLOSED", token))
                .collect(Collectors.toList());
    }
}
