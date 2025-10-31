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
@Tag(name = "Authentication", description = "Endpoints para registro y autenticación de usuarios")
public class AuthController {

    private final UserService userService;
    
    @PostMapping("/register")
    @Operation(
        summary = "Registrar nuevo usuario",
        description = "Crea una nueva cuenta de usuario con el rol de LIDER_COMUNITARIO por defecto."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario registrado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "registerResponse",
                    value = """
                    {
                        "id": 1,
                        "name": "Juan Pérez",
                        "email": "juan.perez@example.com",
                        "nationalId": "1234567890",
                        "phone": "+573001234567",
                        "roles": ["LIDER_COMUNITARIO"],
                        "active": true
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos o email o número de identificación ya registrado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "errorResponse",
                    value = """
                    {
                        "status": 400,
                        "error": "Bad Request",
                        "message": "El correo electrónico o número de identificación ya está en uso"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<UserDto> register(
            @Parameter(
                description = "Datos del nuevo usuario",
                required = true
            )
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Datos del usuario a registrar",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserSaveDto.class),
                    examples = @ExampleObject(
                        name = "registerRequest",
                        value = """
                        {
                            "name": "Walter Jiménez",
                            "email": "walter.jimenez@example.com",
                            "password": "contraseñaSegura123",
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
