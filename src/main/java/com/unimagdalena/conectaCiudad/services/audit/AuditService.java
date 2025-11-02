package com.unimagdalena.conectaCiudad.services.audit;

import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.CitizenActionRequest;

public interface AuditService {
    List<ActionDto> findAllActions();
    List<ActionDto> findActionsByUserId(Long userId);
    List<ActionDto> findActionsByNameContaining(String name);
    List<ActionDto> findRecentActions(int limit);
    ActionDto registerCitizenAction(CitizenActionRequest request, Long userId);
}
