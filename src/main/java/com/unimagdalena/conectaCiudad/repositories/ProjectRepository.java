package com.unimagdalena.conectaCiudad.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByNameContainingIgnoreCase(String name);
    List<Project> findByBeneficiaryPopulationsContainingIgnoreCase(String beneficiaryPopulations);
    List<Project> findByStatus(ProjectStatus status);
    List<Project> findByCreatorId(Long creatorId);
    List<Project> findByStartAt(LocalDateTime startAt);
    List<Project> findByEndAt(LocalDateTime endAt);
}


