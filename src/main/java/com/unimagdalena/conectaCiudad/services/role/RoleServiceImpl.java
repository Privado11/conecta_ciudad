package com.unimagdalena.conectaCiudad.services.role;

import com.unimagdalena.conectaCiudad.Dto.permission.PermissionDto;
import com.unimagdalena.conectaCiudad.Dto.role.RoleDto;
import com.unimagdalena.conectaCiudad.Dto.role.RoleMapper;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.entities.Permission;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.events.RoleUpdatedEvent;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.RoleRepository;
import com.unimagdalena.conectaCiudad.repositories.PermissionRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final RoleMapper roleMapper;
    private final ApplicationEventPublisher eventPublisher;


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
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        List<Permission> newPermissions = permissionRepository.findAll().stream()
                .filter(p -> permissionCodes.contains(p.getCode()))
                .toList();

        if (newPermissions.isEmpty()) {
            throw new BadRequestException(ErrorCode.NO_VALID_PERMISSIONS);
        }

        role.setPermissions(new HashSet<>(newPermissions));
        Role updatedRole = roleRepository.save(role);

        User currentUser = getCurrentUser();
        eventPublisher.publishEvent(new RoleUpdatedEvent(this, updatedRole, currentUser));

        return roleMapper.toDto(updatedRole);
    }

    @Override
    @Transactional
    public RoleDto addPermissionToRole(Long roleId, String permissionCode) {
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
        User currentUser = getCurrentUser();
        eventPublisher.publishEvent(new RoleUpdatedEvent(this, savedRole, currentUser));

        return roleMapper.toDto(savedRole);
    }

    @Override
    @Transactional
    public RoleDto removePermissionFromRole(Long roleId, String permissionCode) {
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
        User currentUser = getCurrentUser();
        eventPublisher.publishEvent(new RoleUpdatedEvent(this, savedRole, currentUser));

        return roleMapper.toDto(savedRole);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String nationalId = authentication.getName();
        return userRepository.findByNationalId(nationalId);
    }
}
