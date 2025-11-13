package com.unimagdalena.conectaCiudad.Dto.role;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.Dto.permission.PermissionDto;
import com.unimagdalena.conectaCiudad.entities.Permission;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleMapper INSTANCE = Mappers.getMapper(RoleMapper.class);

    @Mapping(target = "permissions", expression = "java(mapPermissions(role.getPermissions()))")
    RoleDto toDto(Role role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    Role toEntity(RoleSaveDto roleDto);

    default List<PermissionDto> mapPermissions(Set<Permission> permissions) {
        if (permissions == null) return List.of();
        return permissions.stream()
                .map(p -> new PermissionDto(p.getId(), p.getCode(), p.getDescription(), p.isCritical()))
                .toList();
    }
}
