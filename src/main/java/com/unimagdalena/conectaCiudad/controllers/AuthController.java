package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
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
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
    name = "Autenticación", 
    description = """
        API para gestión de autenticación y registro de usuarios.
        """
)
public class AuthController {

    private final UserService userService;
    
    @PostMapping("/register")
    @Operation(
        summary = "Registrar nuevo usuario en el sistema",
        description = """
            Crea una nueva cuenta de usuario con el rol de CIUDADANO por defecto.
            Este endpoint permite que nuevos usuarios se registren en la plataforma para participar en proyectos comunitarios.
            
            **Flujo de registro:**
            1. El usuario proporciona sus datos personales y credenciales
            2. El sistema valida que el email y cédula no estén ya registrados
            3. La contraseña se encripta usando BCrypt antes de almacenarla
            4. Se asigna automáticamente el rol CIUDADANO
            5. La cuenta se crea en estado activo (active: true)
            6. Se retorna la información del usuario creado (sin la contraseña)
            
            **Validaciones aplicadas:**
            - Email debe ser único en el sistema
            - Cédula (nationalId) debe ser única en el sistema
            - Contraseña debe tener mínimo 8 caracteres
            - Email debe tener formato válido
            - Teléfono debe tener formato válido (opcional)
            - Todos los campos requeridos deben estar presentes
            
            **Roles disponibles en el sistema:**
            - **CIUDADANO**: Rol por defecto, puede visualizar proyectos y participar en la plataforma
            - **LIDER_COMUNITARIO**: Puede crear y gestionar proyectos comunitarios (asignado por admin)
            - **CURATOR**: Revisa y aprueba proyectos (asignado por admin)
            - **ADMIN**: Gestión completa del sistema (asignado manualmente)
            
            **Nota importante:** Solo se puede registrar como CIUDADANO.
            Los roles LIDER_COMUNITARIO, CURATOR y ADMIN deben ser asignados por un administrador después del registro.
            
            **Después del registro:**
            - El usuario debe usar POST /auth/login para obtener su token JWT
            - Con el token JWT puede acceder a los endpoints protegidos según su rol
            
            **Escalamiento de permisos:**
            Si un ciudadano desea convertirse en líder comunitario para crear proyectos,
            debe contactar a un administrador del sistema para solicitar el cambio de rol.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario registrado exitosamente con rol CIUDADANO",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "registerSuccessResponse",
                    value = """
                    {
                        "id": 25,
                        "name": "Carlos Rodríguez",
                        "email": "carlos.rodriguez@example.com",
                        "nationalId": "1234567890",
                        "phone": "+573001234567",
                        "roles": ["CIUDADANO"],
                        "active": true,
                        "createdAt": "2024-11-01T10:30:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos, email ya registrado o cédula duplicada",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "emailDuplicado",
                        value = """
                        {
                            "status": 400,
                            "error": "Bad Request",
                            "message": "El correo electrónico carlos.rodriguez@example.com ya está registrado en el sistema",
                            "timestamp": "2024-11-01T10:30:00"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "cedulaDuplicada",
                        value = """
                        {
                            "status": 400,
                            "error": "Bad Request",
                            "message": "El número de identificación 1234567890 ya está registrado en el sistema",
                            "timestamp": "2024-11-01T10:30:00"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "validationError",
                        value = """
                        {
                            "status": 400,
                            "error": "Bad Request",
                            "message": "Errores de validación",
                            "errors": {
                                "email": "El email debe tener un formato válido",
                                "password": "La contraseña debe tener al menos 8 caracteres",
                                "name": "El nombre es requerido"
                            },
                            "timestamp": "2024-11-01T10:30:00"
                        }
                        """
                    )
                }
            )
        )
    })
    public ResponseEntity<UserDto> register(
            @Parameter(
                description = "Datos del nuevo usuario a registrar en el sistema"
            )
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = """
                    Información completa del usuario para crear la cuenta.
                    
                    **Campos requeridos:**
                    - name: Nombre completo del usuario
                    - email: Correo electrónico único (será el username para login)
                    - password: Contraseña segura (mínimo 8 caracteres, se recomienda incluir mayúsculas, minúsculas y números)
                    - nationalId: Número de cédula o identificación nacional (único)
                    
                    **Campos opcionales:**
                    - phone: Número de teléfono de contacto (formato internacional recomendado)
                    
                    **Rol asignado:**
                    El sistema asigna automáticamente el rol CIUDADANO.
                    Para obtener roles de LIDER_COMUNITARIO, CURATOR o ADMIN,
                    debe contactar a un administrador del sistema.
                    """,
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserSaveDto.class),
                    examples = @ExampleObject(
                        name = "registerRequest",
                        value = """
                        {
                            "name": "Carlos Rodríguez",
                            "email": "carlos.rodriguez@example.com",
                            "password": "MiContraseñaSegura123!",
                            "nationalId": "1234567890",
                            "phone": "+573001234567"
                        }
                        """
                    )
                )
            )
            @Valid @RequestBody UserSaveDto userSaveDto) {
        return ResponseEntity.ok(userService.saveUser(userSaveDto));
    }
}