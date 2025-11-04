package com.unimagdalena.conectaCiudad.services.action;

import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.ActionMapper;
import com.unimagdalena.conectaCiudad.Dto.action.ActionSaveDto;
import com.unimagdalena.conectaCiudad.entities.Action;
import com.unimagdalena.conectaCiudad.repositories.ActionRepository;


import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ActionServiceimpl implements ActionService {

    private final ActionRepository actionRepository;
    private final ActionMapper actionMapper;

    @Override
    public ActionDto save(ActionSaveDto actionSaveDto) {
        Action action = actionMapper.toEntity(actionSaveDto);
        return actionMapper.toDto(actionRepository.save(action));
    }
    
}
