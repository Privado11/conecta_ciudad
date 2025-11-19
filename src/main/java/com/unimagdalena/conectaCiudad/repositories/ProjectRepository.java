package com.unimagdalena.conectaCiudad.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    long countByStatus(ProjectStatus status);
    
    @Query("SELECT TO_CHAR(p.createdAt, 'YYYY-MM') as month, COUNT(p) as count " +
           "FROM Project p " +
           "WHERE p.createdAt >= :startDate " +
           "GROUP BY TO_CHAR(p.createdAt, 'YYYY-MM') " +
           "ORDER BY month DESC")
    List<Object[]> countProjectsByMonth(@Param("startDate") java.time.OffsetDateTime startDate);
    
    @Query("SELECT COUNT(p) FROM Project p " +
           "WHERE p.votingStartAt <= CURRENT_DATE " +
           "AND p.votingEndAt >= CURRENT_DATE " +
           "AND p.status = 'PUBLISHED'")
    long countActiveVotations();
    
    @Query("SELECT p FROM Project p " +
           "WHERE p.votingStartAt <= CURRENT_DATE " +
           "AND p.votingEndAt >= CURRENT_DATE " +
           "AND p.status = 'PUBLISHED' " +
           "ORDER BY p.votingEndAt ASC")
    List<Project> findActiveVotations();


    @Query("""
SELECT p FROM Project p 
LEFT JOIN FETCH p.creator
WHERE p.status = 'READY_TO_PUBLISH' 
AND (p.votingStartAt IS NULL OR p.votingStartAt > :currentDate)
ORDER BY p.votingStartAt ASC NULLS LAST, p.createdAt DESC
""")
    List<Project> findReadyToPublishNotOpenForVoting(@Param("currentDate") LocalDate currentDate);


    @Query("""
SELECT p FROM Project p 
LEFT JOIN FETCH p.creator
WHERE (p.status = 'PUBLISHED' OR 
      (p.status = 'READY_TO_PUBLISH' AND p.votingStartAt <= :currentDate))
AND p.votingStartAt <= :currentDate 
AND p.votingEndAt >= :currentDate
ORDER BY p.votingEndAt ASC
""")
    List<Project> findOpenForVoting(@Param("currentDate") LocalDate currentDate);
    @Query("""
    SELECT p FROM Project p
    WHERE p.votingEndAt < :today
    AND p.status = 'READY_TO_PUBLISH'
""")
    List<Project> findExpiredVoting(@Param("today") LocalDate today);

}