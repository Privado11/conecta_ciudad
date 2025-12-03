package com.unimagdalena.conectaCiudad.controllers;

import java.util.Map;
import java.util.Objects;

import com.unimagdalena.conectaCiudad.Dto.user.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.services.user.UserService;
import com.unimagdalena.conectaCiudad.validation.OnUpdate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API para gestión completa de usuarios del sistema ConectaCiudad")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Consultar usuario por su identificador único",
        description = """
            Obtiene los detalles completos de un usuario específico mediante su ID numérico.
            
            **Información retornada:**
            - Datos personales (nombre, email, teléfono)
            - Número de identificación nacional
            - Lista de roles asignados al usuario
            - Estado de la cuenta (activo/inactivo, según implementación)
            
            **Acceso:** Endpoint público (cualquier usuario autenticado puede consultar)
            
            **Nota:** La contraseña nunca se retorna por seguridad, solo información pública del perfil.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario encontrado exitosamente en la base de datos",
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
            description = "No existe un usuario con el ID especificado en la base de datos",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 404,
                        "error": "Not Found",
                        "message": "No se encontró el usuario con ID: 999"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<UserDto> getById(
            @Parameter(
                description = "Identificador único del usuario en la base de datos (número entero positivo)",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }


    @PreAuthorize("hasAnyAuthority('USER_UPDATE', 'USER_PROFILE_UPDATE')")
    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar datos de un usuario existente",
        description = """
            Permite modificar la información personal de un usuario registrado.
            
            **Campos actualizables:**
            - name: Nombre completo del usuario
            - email: Correo electrónico (debe ser único en el sistema)
            - nationalId: Número de identificación nacional
            - phone: Número telefónico de contacto
            - password: Contraseña (será encriptada automáticamente)
            
            **Restricciones:**
            - El email debe ser único; no puede coincidir con otro usuario existente
            - El nationalId debe ser único en el sistema
            - No se pueden actualizar los roles directamente (usar endpoints específicos: POST/DELETE /roles)
            - La contraseña se encripta automáticamente antes de guardarse
            
            **Permisos:**
            - Un usuario puede actualizar su propia información
            - Un ADMIN puede actualizar la información de cualquier usuario
            
            **Nota:** Para cambios de roles, usar los endpoints `/users/{id}/roles/{role}`
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario actualizado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "updatedUser",
                    value = """
                    {
                        "id": 1,
                        "name": "Walter Jiménez Actualizado",
                        "email": "walter.nuevo@example.com",
                        "nationalId": "1234567890",
                        "phone": "+573001234567",
                        "roles": ["LIDER_COMUNITARIO"]
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos (email duplicado, formato incorrecto, campos requeridos vacíos)",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 400,
                        "error": "Bad Request",
                        "message": "El email ya está en uso por otro usuario"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado con el ID especificado",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Solo puedes actualizar tu propia información o debes ser ADMIN",
            content = @Content
        )
    })
    public ResponseEntity<UserDto> updateUser(
            @Parameter(
                description = "ID único del usuario a actualizar (debe existir en la base de datos)",
                example = "1",
                required = true
            )
            @PathVariable Long id, 
            @Validated(OnUpdate.class) @RequestBody UserSaveDto userDto) {
        return ResponseEntity.ok(userService.updateOwnProfile(id, userDto));
    }





    @GetMapping("/validate")
@Operation(
    summary = "Validar existencia de email o número de identificación",
    description = """
        Verifica si un **correo electrónico** o un **número de identificación nacional (cédula)** 
        ya existen en el sistema.
        
        **Uso típico:** validación en tiempo real desde formularios de creación o registro de usuarios.

        **Comportamiento:**
        - Retorna **200 OK** si los datos están disponibles.
        - Lanza **409 Conflict** si el email o la cédula ya existen.
        - Lanza **400 Bad Request** si no se proporciona ningún parámetro.

        **Ejemplos:**
        - `/api/v1/users/validate?email=juan@example.com`
        - `/api/v1/users/validate?nationalId=1234567890`
        - `/api/v1/users/validate?email=juan@example.com&nationalId=1234567890`
    """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "El email y/o la cédula están disponibles para registro",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                {
                    "available": true,
                    "message": "El email y/o la cédula están disponibles"
                }
                """
            )
        )
    ),
    @ApiResponse(
        responseCode = "409",
        description = "El email o la cédula ya están registrados en el sistema",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                {
                    "status": 409,
                    "error": "Conflict",
                    "message": "El email 'juan@example.com' ya está registrado"
                }
                """
            )
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "Solicitud inválida: no se envió ni email ni nationalId",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                {
                    "status": 400,
                    "error": "Bad Request",
                    "message": "Debe proporcionar al menos un parámetro: email o nationalId"
                }
                """
            )
        )
    )
})
public ResponseEntity<?> validateEmailOrNationalId(
    @Parameter(description = "Correo electrónico a validar", example = "juan@example.com")
    @RequestParam(required = false) String email,

    @Parameter(description = "Número de identificación nacional (cédula)", example = "1234567890")
    @RequestParam(required = false) String nationalId
) {
    userService.validateUniqueFields(email, nationalId);

    return ResponseEntity.ok(
        Map.of("available", true, "message", "El email y/o la cédula están disponibles")
    );
}



    @PreAuthorize("hasAnyAuthority('USER_UPDATE_PASSWORD', 'USER_PROFILE_UPDATE')")
@PatchMapping("/{id}/change-password")
@Operation(
    summary = "Cambiar la contraseña de un usuario",
    description = """
        Permite a un usuario autenticado cambiar su contraseña proporcionando la 
        **contraseña actual** y la **nueva contraseña**.

        **Flujo completo:**
        1. El usuario envía su contraseña actual
        2. El sistema valida que la contraseña sea correcta
        3. La nueva contraseña debe cumplir con las políticas mínimas (longitud, etc.)
        4. La contraseña se encripta con BCrypt antes de guardarse
        5. Se registra el evento en el sistema de auditoría

        **Permisos:**
        - El usuario solo puede cambiar *su propia* contraseña
        - Un ADMIN puede cambiar la contraseña de cualquier usuario

        **Validaciones importantes:**
        - oldPassword es obligatoria
        - newPassword debe tener al menos 6 caracteres (o la que defina tu política)
        - La nueva contraseña no puede ser igual a la anterior
        - La contraseña nunca se retorna en la respuesta
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Contraseña actualizada correctamente",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = UserDto.class),
            examples = @ExampleObject(
                value = """
                {
                    "id": 1,
                    "name": "Walter Jiménez",
                    "email": "walter@example.com",
                    "nationalId": "1234567890",
                    "phone": "+573001234567",
                    "roles": ["CIUDADANO"],
                    "active": true
                }
                """
            )
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "Validación fallida: contraseña actual incorrecta o nueva contraseña inválida",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                {
                    "status": 400,
                    "error": "Bad Request",
                    "message": "La contraseña actual es incorrecta"
                }
                """
            )
        )
    ),
    @ApiResponse(
        responseCode = "403",
        description = "No autorizado: solo puedes modificar tu propia contraseña",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                value = """
                {
                    "status": 403,
                    "error": "Forbidden",
                    "message": "No puedes cambiar la contraseña de otro usuario"
                }
                """
            )
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Usuario no encontrado con el ID proporcionado",
        content = @Content
    )
})
public ResponseEntity<UserDto> changePassword(
        @Parameter(
            description = "ID del usuario cuya contraseña se desea cambiar",
            example = "1",
            required = true
        )
        @PathVariable Long id,

        @Parameter(
            description = """
                Objeto con la contraseña actual y la nueva contraseña.
                
                - oldPassword: Contraseña actual del usuario
                - newPassword: Nueva contraseña que reemplazará a la anterior
                """,
            required = true
        )
        @RequestBody ChangePasswordDto dto
) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Long currentUserId = (Long) auth.getDetails();


    if (!auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("USER_UPDATE_PASSWORD"))
        && !Objects.equals(currentUserId, id)) {

        return ResponseEntity.status(403).body(null);
    }

    UserDto updated = userService.changePassword(id, dto.oldPassword(), dto.newPassword());
    return ResponseEntity.ok(updated);
}






}