package com.unimagdalena.conectaCiudad.Dto.action;

import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Builder;

import java.util.Map;

@Builder
public record ActionLogRequest(
    String actionType,
    String description,
    EntityType entityType,
    Long entityId,
    ActionResult result,
    Map<String, Object> metadata,
    Long userId,
    Long accessId
) {}