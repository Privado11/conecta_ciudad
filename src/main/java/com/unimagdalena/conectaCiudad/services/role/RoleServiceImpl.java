package com.unimagdalena.conectaCiudad.services.role;

import com.unimagdalena.conectaCiudad.Dto.permission.PermissionDto;
import com.unimagdalena.conectaCiudad.Dto.role.RoleDto;
import com.unimagdalena.conectaCiudad.Dto.role.RoleMapper;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.entities.Permission;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.enums.RoleActionType;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.RoleRepository;
import com.unimagdalena.conectaCiudad.repositories.PermissionRepository;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final AuditHelper auditHelper;


    @Override
    public List<RoleDto> findAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toDto)
                .toList();
    }


    @Override
    public List<PermissionDto> findAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(p -> new PermissionDto(p.getId(), p.getCode(), p.getDescription(), p.isCritical()))
                .toList();
    }


    @Override
    public RoleDto findById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return roleMapper.toDto(role);
    }

    @Override
    public RoleDto findByName(String name) {
        Role role = roleRepository.findByNameContainingIgnoreCase(name);
        if (role == null) {
            throw new ResourceNotFoundException("Role", "name", name);
        }
        return roleMapper.toDto(role);
    }

    @Override
    @Transactional
    public RoleDto updateRolePermissions(Long roleId, Set<String> permissionCodes) {
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

            List<Permission> newPermissions = permissionRepository.findAll().stream()
                    .filter(p -> permissionCodes.contains(p.getCode()))
                    .toList();

            if (newPermissions.isEmpty()) {
                throw new BadRequestException(ErrorCode.NO_VALID_PERMISSIONS);
            }

            Set<Permission> oldPermissions = new HashSet<>(role.getPermissions());
            role.setPermissions(new HashSet<>(newPermissions));
            Role updatedRole = roleRepository.save(role);

            if (!oldPermissions.equals(updatedRole.getPermissions())) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("roleName", role.getName());
                metadata.put("oldPermissions", oldPermissions.stream().map(Permission::getCode).toList());
                metadata.put("newPermissions", newPermissions.stream().map(Permission::getCode).toList());

                auditHelper.logComplete(
                        RoleActionType.ROLE_PERMISSIONS_UPDATED.name(),
                        "Role permissions for '" + role.getName() + "' updated",
                        EntityType.ROLE,
                        roleId,
                        ActionResult.SUCCESS,
                        metadata
                );
            }

            return roleMapper.toDto(updatedRole);

        } catch (Exception e) {
            auditHelper.logFailure(
                    RoleActionType.ROLE_PERMISSIONS_UPDATED.name(),
                    "Error updating permissions for role " + roleId,
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    @Transactional
    public RoleDto addPermissionToRole(Long roleId, String permissionCode) {
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

            Permission permission = permissionRepository.findByCode(permissionCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission", "code", permissionCode));

            boolean added = role.getPermissions().add(permission);
            if (!added) {
                throw new BadRequestException(
                        ErrorCode.PERMISSION_ALREADY_ASSIGNED,
                        Map.of("permissionCode", permissionCode)
                );
            }

            Role savedRole = roleRepository.save(role);

            auditHelper.logComplete(
                    RoleActionType.ROLE_PERMISSION_ADDED.name(),
                    "Permission '" + permissionCode + "' added to role '" + role.getName() + "'",
                    EntityType.ROLE,
                    roleId,
                    ActionResult.SUCCESS,
                    Map.of(
                            "roleName", role.getName(),
                            "permissionAdded", permissionCode
                    )
            );

            return roleMapper.toDto(savedRole);

        } catch (Exception e) {
            auditHelper.logFailure(
                    RoleActionType.ROLE_PERMISSION_ADDED.name(),
                    "Error adding permission to role " + roleId,
                    e.getMessage()
            );
            throw e;
        }
    }

    @Override
    @Transactional
    public RoleDto removePermissionFromRole(Long roleId, String permissionCode) {
        try {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

            Permission permission = permissionRepository.findByCode(permissionCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission", "code", permissionCode));

            boolean removed = role.getPermissions().remove(permission);
            if (!removed) {
                throw new BadRequestException(
                        ErrorCode.PERMISSION_NOT_ASSIGNED,
                        Map.of("permissionCode", permissionCode)
                );
            }

            Role savedRole = roleRepository.save(role);

            auditHelper.logComplete(
                    RoleActionType.ROLE_PERMISSION_REMOVED.name(),
                    "Permission '" + permissionCode + "' removed from role '" + role.getName() + "'",
                    EntityType.ROLE,
                    roleId,
                    ActionResult.SUCCESS,
                    Map.of(
                            "roleName", role.getName(),
                            "permissionRemoved", permissionCode
                    )
            );

            return roleMapper.toDto(savedRole);

        } catch (Exception e) {
            auditHelper.logFailure(
                    RoleActionType.ROLE_PERMISSION_REMOVED.name(),
                    "Error removing permission from role " + roleId,
                    e.getMessage()
            );
            throw e;
        }
    }
}
