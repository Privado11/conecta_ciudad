package com.unimagdalena.conectaCiudad.Dto.action;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;
import com.unimagdalena.conectaCiudad.entities.Action;

@Mapper(componentModel = "spring", uses = { UserMapper.class })
public interface ActionMapper {

    ActionMapper INSTANCE = Mappers.getMapper(ActionMapper.class);

    ActionDto toDto(Action action);
    Action toEntity(ActionSaveDto actionDto);
 
}