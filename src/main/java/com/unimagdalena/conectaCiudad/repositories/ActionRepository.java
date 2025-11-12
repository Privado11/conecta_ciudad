package com.unimagdalena.conectaCiudad.repositories;

import com.unimagdalena.conectaCiudad.entities.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long>, 
                                          JpaSpecificationExecutor<Action> { 
    @Query("SELECT MAX(a.actionAt) FROM Action a WHERE a.user.id = :userId")
    LocalDateTime findLastActionDateByUserId(@Param("userId") Long userId);
}