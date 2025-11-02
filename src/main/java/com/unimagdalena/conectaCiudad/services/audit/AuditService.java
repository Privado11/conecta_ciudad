package com.unimagdalena.conectaCiudad.services.audit;

import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;

public interface AuditService {
    List<ActionDto> findAllActions();
    List<ActionDto> findActionsByUserId(Long userId);
    List<ActionDto> findActionsByNameContaining(String name);
    List<ActionDto> findRecentActions(int limit);
}
