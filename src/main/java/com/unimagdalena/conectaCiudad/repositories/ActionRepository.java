package com.unimagdalena.conectaCiudad.repositories;

import com.unimagdalena.conectaCiudad.entities.Action;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {

    @Query("SELECT MAX(a.actionAt) FROM Action a WHERE a.user.id = :userId")
    LocalDateTime findLastActionDateByUserId(@Param("userId") Long userId);

    Page<Action> findByUserIdOrderByActionAtDesc(Long userId, Pageable pageable);
    Page<Action> findByEntityTypeAndEntityIdOrderByActionAtDesc(EntityType entityType, Long entityId, Pageable pageable);
    Page<Action> findByActionTypeOrderByActionAtDesc(String actionType, Pageable pageable);
    Page<Action> findByActionAtBetweenOrderByActionAtDesc(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<Action> findByResultOrderByActionAtDesc(ActionResult result, Pageable pageable);

    long countByUserId(Long userId);
    long countByUserIdAndResult(Long userId, ActionResult result);
    long countByUserIdAndActionAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    long countByActionType(String actionType);
    long countByActionTypeAndResult(String actionType, ActionResult result);
    long countByActionTypeAndActionAtBetween(String actionType, LocalDateTime start, LocalDateTime end);

    long countByEntityTypeAndEntityId(EntityType entityType, Long entityId);
    long countByEntityTypeAndEntityIdAndResult(EntityType entityType, Long entityId, ActionResult result);
    long countByEntityTypeAndEntityIdAndActionAtBetween(EntityType entityType, Long entityId, LocalDateTime start, LocalDateTime end);

    long countByResult(ActionResult result);
    long countByActionAtBetween(LocalDateTime start, LocalDateTime end);
}
