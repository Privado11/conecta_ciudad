package com.unimagdalena.conectaCiudad.services.action;

import com.unimagdalena.conectaCiudad.Dto.action.*;
import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;

public interface ActionService {
    
    ActionDto save(ActionSaveDto actionSaveDto);
    
    void logAction(String actionType, String description, User user, Access access);
    
    ActionDto logActionWithDetails(ActionLogRequest request);

    LocalDateTime getLastActionDateByUserId(Long userId);

    PagedResponse<ActionDto> searchWithFilters(
        String actionType, 
        ActionResult result, 
        EntityType entityType, 
        String searchTerm,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    Map<String, Object> getActionDetails(Long actionId);
}
