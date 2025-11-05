package com.unimagdalena.conectaCiudad.services.action;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.ActionSaveDto;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;

public interface ActionService {
    ActionDto save(ActionSaveDto actionSaveDto);
    void logAction(String actionType, String message, User user, Access access);
}
