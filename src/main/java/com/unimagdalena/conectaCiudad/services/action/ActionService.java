package com.unimagdalena.conectaCiudad.services.action;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.ActionSaveDto;

public interface ActionService {
    ActionDto save(ActionSaveDto actionSaveDto);
}
