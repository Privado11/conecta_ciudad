package com.unimagdalena.conectaCiudad.services.action;

import com.unimagdalena.conectaCiudad.Dto.action.*;
import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
public interface ActionService {
    
    ActionDto save(ActionSaveDto actionSaveDto);
    
    void logAction(String actionType, String description, User user, Access access);
    
    ActionDto logActionWithDetails(ActionLogRequest request);
    
    LocalDateTime getLastActionDateByUserId(Long userId);
    
    PagedResponse<ActionDto> findByUserId(Long userId, Pageable pageable);
    
    PagedResponse<ActionDto> findByEntityTypeAndId(EntityType entityType, Long entityId, Pageable pageable);
    
    PagedResponse<ActionDto> findByActionType(String actionType, Pageable pageable);
    
    PagedResponse<ActionDto> findByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    PagedResponse<ActionDto> findByResult(ActionResult result, Pageable pageable);

}