package com.unimagdalena.conectaCiudad.services.role;

import com.unimagdalena.conectaCiudad.Dto.role.RoleDto;
import com.unimagdalena.conectaCiudad.Dto.permission.PermissionDto;

import java.util.List;
import java.util.Set;

public interface RoleService {
    List<RoleDto> findAllRoles();
    List<PermissionDto> findAllPermissions();
    RoleDto findById(Long id);
    RoleDto findByName(String name);
    RoleDto updateRolePermissions(Long roleId, Set<String> permissionCodes);
    RoleDto addPermissionToRole(Long roleId, String permissionCode);
    RoleDto removePermissionFromRole(Long roleId, String permissionCode);
}
