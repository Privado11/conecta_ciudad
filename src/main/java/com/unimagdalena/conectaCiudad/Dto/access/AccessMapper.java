package com.unimagdalena.conectaCiudad.Dto.access;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;

@Mapper(componentModel = "spring", uses = { UserMapper.class })
public interface AccessMapper {

    AccessMapper INSTANCE = Mappers.getMapper(AccessMapper.class);

    Access toEntity(AccessSaveDto accessSaveDto);
    AccessDto toDto(Access access);
    Access toEntity(AccessDto accessDto);
 
}