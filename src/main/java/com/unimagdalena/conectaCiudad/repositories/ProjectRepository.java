package com.unimagdalena.conectaCiudad.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

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

    @EntityGraph(attributePaths = {"creator", "reviews", "reviews.curator"})
    Page<Project> findAll(Specification<Project> spec, Pageable pageable);

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

}