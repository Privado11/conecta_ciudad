package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Endpoints de autenticación JWT")
public class LoginDocumentationController {

    @PostMapping("/login")
    @Operation(
    summary = "Iniciar sesión y obtener token JWT",
    description = """
        Autentica a un usuario existente y genera un token JWT para acceder a recursos protegidos.
        
        **IMPORTANTE:** Este endpoint es manejado automáticamente por el filtro JwtAuthenticationFilter.
        No es un endpoint REST tradicional, sino un filtro de Spring Security.
        
        **Flujo de autenticación:**
        1. El usuario envía email y contraseña en el body
        2. El filtro JwtAuthenticationFilter intercepta la petición automáticamente
        3. Spring Security valida las credenciales contra la base de datos
        4. Si son correctas, genera un token JWT con los roles del usuario
        5. Retorna el token en el header Authorization y en el body JSON
        
        **Token JWT generado:**
        - Contiene: email (subject), roles (claims), fecha de expiración
        - Duración: 1 hora (3600000 ms)
        - Algoritmo: HS256
        - Se incluye en header: Authorization: Bearer {token}
        - También se retorna en el body JSON
        
        **Cómo usar el token en futuras peticiones:**
        Todas las peticiones a endpoints protegidos deben incluir el header:
        Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        
        **Seguridad:**
        - Las contraseñas se verifican usando BCrypt (hash seguro)
        - El token expira automáticamente después de 1 hora
        - El token debe guardarse de forma segura en el cliente (localStorage o sessionStorage)
        - Si el token es robado, solo es válido hasta su expiración
        
        **Cierre de sesión:**
        En un sistema JWT stateless, el logout se hace en el frontend eliminando el token.
        No es necesario llamar a ningún endpoint de logout en el backend.
        
        **Registro de acceso:**
        Cada login exitoso se registra automáticamente en la tabla access con timestamp.
        
        **Permisos por rol:**
        El token incluye los roles del usuario. Los endpoints protegidos verifican estos roles:
        - **CIUDADANO**: Acceso básico, puede visualizar proyectos públicos
        - **LIDER_COMUNITARIO**: Puede crear y editar sus propios proyectos
        - **CURATOR**: Puede revisar, aprobar y agregar observaciones a proyectos asignados
        - **ADMIN**: Acceso completo al sistema, gestión de usuarios y proyectos
        
        **Nota:** Los usuarios registrados obtienen rol CIUDADANO por defecto.
        Para obtener otros roles, deben ser asignados por un administrador.
        """
)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login exitoso - Token JWT generado correctamente y registrado en tabla access",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "loginCiudadano",
                        summary = "Login exitoso - Usuario CIUDADANO",
                        value = """
                        {
                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjYXJsb3Mucm9kcmlndWV6QGV4YW1wbGUuY29tIiwicm9sZXMiOlsiQ0lVREFEQU5PIl0sImlhdCI6MTYzMDQ1MDgwMCwiZXhwIjoxNjMwNDU0NDAwfQ.Xm3K8F2vN9pQ7rL5sW1tY6uZ8jH4kD2nM0oP3qR7sT9",
                            "username": "carlos.rodriguez@example.com",
                            "message": "Bienvenido carlos.rodriguez@example.com, has iniciado sesión correctamente"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "loginLider",
                        summary = "Login exitoso - Usuario LIDER_COMUNITARIO",
                        value = """
                        {
                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ3YWx0ZXIuamltZW5lekBleGFtcGxlLmNvbSIsInJvbGVzIjpbIkxJREVSX0NPTVVOSVRBULPIL0sImlhdCI6MTYzMDQ1MDgwMCwiZXhwIjoxNjMwNDU0NDAwfQ.Xm3K8F2vN9pQ7rL5sW1tY6uZ8jH4kD2nM0oP3qR7sT9",
                            "username": "walter.jimenez@example.com",
                            "message": "Bienvenido walter.jimenez@example.com, has iniciado sesión correctamente"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "loginAdmin",
                        summary = "Login exitoso - Usuario ADMIN",
                        value = """
                        {
                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbkBjb25lY3RhY2l1ZGFkLmNvbSIsInJvbGVzIjpbIkFETUlOIl0sImlhdCI6MTYzMDQ1MDgwMCwiZXhwIjoxNjMwNDU0NDAwfQ.Xm3K8F2vN9pQ7rL5sW1tY6uZ8jH4kD2nM0oP3qR7sT9",
                            "username": "admin@conectaciudad.com",
                            "message": "Bienvenido admin@conectaciudad.com, has iniciado sesión correctamente"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas - Email o contraseña incorrectos",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "loginFailedResponse",
                    value = """
                    {
                        "message": "Error en la autenticación: username o password incorrectos",
                        "error": "Bad credentials"
                    }
                    """
                )
            )
        )
    })
    public void login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = """
                    Credenciales de acceso del usuario.
                    
                    **Campos requeridos:**
                    - email: Correo electrónico registrado en el sistema (usado como username)
                    - password: Contraseña de la cuenta
                    
                    **Nota:** Las credenciales son case-sensitive.
                    El email no distingue mayúsculas, pero la contraseña sí.
                    
                    **IMPORTANTE:** El body debe ser un JSON con la estructura de User entity
                    (el filtro lo deserializa así), aunque solo necesita email y password.
                    """,
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "login",
                            summary = "Login",
                            value = """
                            {
                                "email": "walter.jimenez@example.com",
                                "password": "MiContraseñaSegura123!"
                            }
                            """
                        )
                    }
                )
            )
            @RequestBody Object loginRequest) {
        
    }
}