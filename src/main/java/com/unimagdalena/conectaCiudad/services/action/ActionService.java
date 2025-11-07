package com.unimagdalena.conectaCiudad.services.action;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.ActionSaveDto;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;
import java.time.LocalDateTime;

public interface ActionService {
    ActionDto save(ActionSaveDto actionSaveDto);
    LocalDateTime getLastActionDateByUserId(Long userId);
    void logAction(String actionType, String message, User user, Access access);
}
