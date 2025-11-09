package com.unimagdalena.conectaCiudad.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByNameContainingIgnoreCase(String name);
    List<Project> findByBeneficiaryPopulationsContainingIgnoreCase(String beneficiaryPopulations);
    List<Project> findByStatus(ProjectStatus status);
    List<Project> findByCreatorId(Long creatorId);
    List<Project> findByStartAt(LocalDateTime startAt);
    List<Project> findByEndAt(LocalDateTime endAt);
    long countByCreatorId(Long creatorId);
    @Query("SELECT COUNT(p) FROM Project p WHERE p.creator.id = :creatorId AND p.status = :status")
    long countByCreatorIdAndStatus(
        @Param("creatorId") Long creatorId, 
        @Param("status") ProjectStatus status
    );
    
    @Query("SELECT p FROM Project p WHERE p.creator.id = :creatorId AND p.status <> :status")
    List<Project> findByCreatorIdAndStatusNot(
        @Param("creatorId") Long creatorId, 
        @Param("status") ProjectStatus status
    );
}


