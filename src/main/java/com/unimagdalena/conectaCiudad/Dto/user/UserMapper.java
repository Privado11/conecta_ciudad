package com.unimagdalena.conectaCiudad.Dto.user;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.unimagdalena.conectaCiudad.entities.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    User toEntity(UserDto user);
    User toUserSaveDtoToEntity(UserSaveDto userSaveDto);
    UserDto toDto(User user);
}
