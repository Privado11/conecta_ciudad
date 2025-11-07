package com.unimagdalena.conectaCiudad.Dto.user;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.entities.Role;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    User toEntity(UserDto user);
    User toUserSaveDtoToEntity(UserSaveDto userSaveDto);
    UserDto toDto(User user);
    

   
    default Role map(String roleName) {
        if (roleName == null) return null;
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

    default String map(Role role) {
        return role == null ? null : role.getName();
    }
}
