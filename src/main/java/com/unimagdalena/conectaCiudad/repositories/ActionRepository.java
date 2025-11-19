package com.unimagdalena.conectaCiudad.repositories;

import com.unimagdalena.conectaCiudad.entities.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long>, 
                                          JpaSpecificationExecutor<Action> { 
    @Query("SELECT MAX(a.actionAt) FROM Action a WHERE a.user.id = :userId")
    OffsetDateTime findLastActionDateByUserId(@Param("userId") Long userId);
    
    @Query("SELECT a FROM Action a ORDER BY a.actionAt DESC")
    org.springframework.data.domain.Page<Action> findRecentActions(org.springframework.data.domain.Pageable pageable);
}