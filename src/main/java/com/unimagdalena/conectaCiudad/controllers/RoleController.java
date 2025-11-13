package com.unimagdalena.conectaCiudad.controllers;

import com.unimagdalena.conectaCiudad.Dto.permission.PermissionDto;
import com.unimagdalena.conectaCiudad.Dto.role.RoleDto;
import com.unimagdalena.conectaCiudad.services.role.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(
    name = "Gestión de Roles y Permisos",
    description = """
        API para administrar roles del sistema y los permisos asociados a cada uno.
        Permite listar roles, ver permisos disponibles y modificar dinámicamente 
        los permisos de cada rol (solo administradores).
        """
)
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(
        summary = "Listar todos los roles",
        description = "Devuelve la lista completa de roles registrados en el sistema."
    )
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        return ResponseEntity.ok(roleService.findAllRoles());
    }

    @GetMapping("/permissions")
    @Operation(
        summary = "Listar todos los permisos disponibles",
        description = "Devuelve todos los permisos que pueden asignarse a los roles del sistema."
    )
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        return ResponseEntity.ok(roleService.findAllPermissions());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obtener un rol por ID",
        description = "Retorna la información completa de un rol, incluyendo sus permisos."
    )
    public ResponseEntity<RoleDto> getRoleById(
        @Parameter(description = "ID del rol a consultar") 
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(roleService.findById(id));
    }


    @PreAuthorize("hasAuthority('ROLE_PERMISSION_UPDATE')")
    @PutMapping("/{id}/permissions")
    @Operation(
        summary = "Actualizar permisos de un rol",
        description = """
            Reemplaza completamente los permisos actuales del rol con los proporcionados.
            Esta acción queda registrada en la auditoría del sistema.
            """
    )
    public ResponseEntity<RoleDto> updateRolePermissions(
        @Parameter(description = "ID del rol al que se le actualizarán los permisos") 
        @PathVariable Long id,

        @Parameter(description = "Lista de códigos de permisos a asignar (reemplazan los existentes)")
        @RequestBody Set<String> permissionCodes
    ) {
        RoleDto updatedRole = roleService.updateRolePermissions(id, permissionCodes);
        return ResponseEntity.ok(updatedRole);
    }

    @PreAuthorize("hasAuthority('ROLE_PERMISSION_ADD')")
    @PostMapping("/{id}/permissions/{permissionCode}")
    @Operation(
        summary = "Agregar un permiso a un rol",
        description = """
            Agrega un nuevo permiso al rol especificado. 
            Si el rol ya tiene ese permiso, se lanza una excepción.
            Esta acción se registra en la auditoría.
            """
    )
    public ResponseEntity<RoleDto> addPermissionToRole(
        @Parameter(description = "ID del rol al que se agregará el permiso") 
        @PathVariable Long id,

        @Parameter(description = "Código del permiso a agregar") 
        @PathVariable String permissionCode
    ) {
        RoleDto updatedRole = roleService.addPermissionToRole(id, permissionCode);
        return ResponseEntity.ok(updatedRole);
    }

    @PreAuthorize("hasAuthority('ROLE_PERMISSION_REMOVE')")
    @DeleteMapping("/{id}/permissions/{permissionCode}")
    @Operation(
        summary = "Remover un permiso de un rol",
        description = """
            Elimina un permiso específico del rol indicado.
            Esta acción se registra en la auditoría.
            """
    )
    public ResponseEntity<RoleDto> removePermissionFromRole(
        @Parameter(description = "ID del rol al que se le quitará el permiso") 
        @PathVariable Long id,

        @Parameter(description = "Código del permiso a remover") 
        @PathVariable String permissionCode
    ) {
        RoleDto updatedRole = roleService.removePermissionFromRole(id, permissionCode);
        return ResponseEntity.ok(updatedRole);
    }
}
