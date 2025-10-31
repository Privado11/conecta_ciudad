package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.services.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Operaciones para gestión de usuarios")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Obtener usuario por ID",
        description = "Obtiene los detalles de un usuario específico por su ID"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "userResponse",
                    value = """
                    {
                        "id": 1,
                        "name": "Walter Jiménez",
                        "email": "walter@example.com",
                        "nationalId": "1234567890",
                        "phone": "+573001234567",
                        "roles": ["LIDER_COMUNITARIO"]
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content
        )
    })
    public ResponseEntity<UserDto> getById(
            @Parameter(description = "ID del usuario a buscar", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    
    @GetMapping
    @Operation(
        summary = "Buscar/Listar usuarios",
        description = "Filtra usuarios por nombre, email o número de identificación. Si no se especifican filtros, retorna todos los usuarios."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de usuarios que coinciden con los criterios de búsqueda",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto[].class),
                examples = @ExampleObject(
                    name = "usersList",
                    value = """
                    [
                        {
                             "id": 1,
                        "name": "Walter Jiménez",
                        "email": "walter@example.com",
                        "nationalId": "1234567890",
                        "phone": "+573001234567",
                        "roles": ["LIDER_COMUNITARIO"]
                        },
                        {
                            "id": 2,
                            "name": "Nicole Hernández",
                            "email": "nicole@example.com",
                            "nationalId": "1234567890",
                            "phone": "+573001234567",
                            "roles": ["CURATOR"]
                        }
                    ]
                    """
                )
            )
        )
    })
    public ResponseEntity<?> searchUsers(
            @Parameter(description = "Filtrar por nombre (búsqueda parcial)", example = "Walter")
            @RequestParam(required = false) String name,
            
            @Parameter(description = "Filtrar por email exacto", example = "walter@example.com")
            @RequestParam(required = false) String email,
            
            @Parameter(description = "Filtrar por número de identificación", example = "1234567890")
            @RequestParam(required = false) String nationalId) {

        if (email != null) {
            return ResponseEntity.ok(userService.findByEmail(email));
        } else if (name != null) {
            return ResponseEntity.ok(userService.findByNameContainingIgnoreCase(name));
        } else if (nationalId != null) {
            return ResponseEntity.ok(userService.findByNationalId(nationalId));
        } else {
            return ResponseEntity.ok(userService.findAll());
        }
    }


    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar usuario",
        description = "Actualiza los datos de un usuario existente"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content
        )
    })
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "ID del usuario a actualizar", example = "1")
            @PathVariable Long id, 
            @Valid @RequestBody UserSaveDto userDto) {
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

  
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar usuario",
        description = "Elimina un usuario por su ID. Solo usuarios con rol ADMIN pueden eliminar usuarios."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario eliminado exitosamente",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject("User deleted successfully")
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content
        )
    })
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "ID del usuario a eliminar", example = "1")
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    
    @PostMapping("/{id}/roles/{role}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Agregar rol a usuario",
        description = "Agrega un rol a un usuario existente. Solo usuarios con rol ADMIN pueden realizar esta acción."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Rol agregado exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Rol no válido o el usuario ya tiene el rol asignado",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content
        )
    })
    public ResponseEntity<UserDto> addRole(
            @Parameter(description = "ID del usuario", example = "1")
            @PathVariable Long id, 
            
            @Parameter(
                description = "Rol a agregar (valores posibles: ADMIN, CURATOR, LIDER_COMUNITARIO)", 
                example = "CURATOR"
            )
            @PathVariable String role) {
        return ResponseEntity.ok(userService.addRole(id, role));
    }

    @DeleteMapping("/{id}/roles/{role}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Eliminar rol de usuario",
        description = "Elimina un rol de un usuario existente. Solo usuarios con rol ADMIN pueden realizar esta acción."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Rol eliminado exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Rol no válido o el usuario no tiene el rol asignado",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado",
            content = @Content
        )
    })
    public ResponseEntity<UserDto> removeRole(
            @Parameter(description = "ID del usuario", example = "1")
            @PathVariable Long id, 
            
            @Parameter(
                description = "Rol a eliminar (valores posibles: ADMIN, CURATOR, LIDER_COMUNITARIO)", 
                example = "CURATOR"
            )
            @PathVariable String role) {
        return ResponseEntity.ok(userService.removeRole(id, role));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Crear usuario (Admin)",
        description = "Crea un nuevo usuario con roles específicos. Solo usuarios con rol ADMIN pueden realizar esta acción.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del usuario a crear",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserSaveDto.class),
                examples = @ExampleObject(
                    name = "userRequest",
                    value = """
                    {
                        "name": "Nuevo Usuario",
                        "email": "nuevo@example.com",
                        "password": "contraseñaSegura123",
                        "nationalId": "9876543210",
                        "phone": "+573001234567",
                        "roles": ["LIDER_COMUNITARIO"]
                    }
                    """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario creado exitosamente",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos o email ya registrado",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN",
            content = @Content
        )
    })
    public ResponseEntity<UserDto> createUserAdmin(
            @Valid @RequestBody UserSaveDto userDto) {
        return ResponseEntity.ok(userService.createUserWithRoles(userDto, userDto.roles()));
    }
}
