package com.unimagdalena.conectaCiudad.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.unimagdalena.conectaCiudad.entities.Action;

public interface ActionRepository extends JpaRepository<Action, Long> {
    List<Action> findByUserId(Long userId);
    List<Action> findByNameContainingIgnoreCase(String name);
    @Query("SELECT a.access.accessAt FROM Action a WHERE a.user.id = :userId ORDER BY a.actionAt DESC LIMIT 1")
    LocalDateTime findLastActionDateByUserId(@Param("userId") Long userId);
}


