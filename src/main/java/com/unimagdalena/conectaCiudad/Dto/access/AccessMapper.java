package com.unimagdalena.conectaCiudad.Dto.access;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.Dto.user.UserMapper;

/**
 * Mapper for Access entity and DTOs.
 * Handles conversion between Access entity and its DTO representations.
 */
@Mapper(componentModel = "spring", uses = { UserMapper.class })
public interface AccessMapper {

    AccessMapper INSTANCE = Mappers.getMapper(AccessMapper.class);

    /**
     * Maps AccessSaveDto to Access entity.
     * The userId field is mapped to the user entity by MapStruct.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accessAt", ignore = true)
    @Mapping(source = "userId", target = "user.id")
    Access toEntity(AccessSaveDto accessSaveDto);
    
    AccessDto toDto(Access access);
    Access toEntity(AccessDto accessDto);
 
}