package com.unimagdalena.conectaCiudad.repositories;

import com.unimagdalena.conectaCiudad.entities.Vote;
import com.unimagdalena.conectaCiudad.enums.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByProjectIdAndVoterId(Long projectId, Long voterId);

    boolean existsByProjectIdAndVoterId(Long projectId, Long voterId);

    long countByProjectIdAndVoteType(Long projectId, VoteType voteType);

    long countByProjectId(Long projectId);

    List<Vote> findByVoterId(Long voterId);

    List<Vote> findByProjectId(Long projectId);

    long countByVoterIdAndVoteType(Long voterId, VoteType voteType);

    @Query("SELECT v.voteType, COUNT(v) FROM Vote v WHERE v.project.id = :projectId GROUP BY v.voteType")
    List<Object[]> getVotingResultsByProjectId(@Param("projectId") Long projectId);
}
