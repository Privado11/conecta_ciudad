package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping
    @Operation(
        summary = "Buscar y listar usuarios con filtros opcionales y paginación",
        description = """
        Permite buscar usuarios aplicando diferentes criterios de filtrado con soporte de paginación.
        
        **Criterios de búsqueda disponibles:**
        - **Por nombre:** Búsqueda parcial case-insensitive (ej: "walter" encuentra "Walter Jiménez")
        - **Por email:** Búsqueda exacta del correo electrónico
        - **Por cédula/ID nacional:** Búsqueda exacta del número de identificación
        - **Sin filtros:** Retorna el listado completo de usuarios registrados (paginado)
        
        **IMPORTANTE:** Cuando se usa el filtro 'name', la búsqueda se aplica sobre TODA la base de datos
        y los resultados se paginan. Esto permite buscar entre miles de usuarios eficientemente.
        
        **Parámetros de paginación:**
        - **page:** Número de página (comienza en 0, por defecto: 0)
        - **size:** Elementos por página (por defecto: 10, máximo: 100)
        - **sortBy:** Campo para ordenar (por defecto: "name")
        - **sortDirection:** Dirección del ordenamiento "asc" o "desc" (por defecto: "asc")
        
        **Campos disponibles para ordenar:**
        - name, email, nationalId, createdAt, phone, active
        
        **Comportamiento:**
        - **Con filtro 'name':** Busca en toda la BD y pagina los resultados
        - **Con filtros 'email' o 'nationalId':** Retorna objeto único sin paginación (búsqueda exacta)
        - **Sin filtros:** Retorna página de todos los usuarios
        """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de usuarios que coinciden con los criterios (puede ser UserDto único, lista, o Page<UserDto>)",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "paginatedUsers",
                        description = "Respuesta paginada cuando no se especifican filtros",
                        value = """
                        {
                            "content": [
                                {
                                    "id": 1,
                                    "name": "Walter Jiménez",
                                    "email": "walter@example.com",
                                    "nationalId": "1234567890",
                                    "phone": "+573001234567",
                                    "roles": ["LIDER_COMUNITARIO"],
                                    "active": true,
                                    "createdAt": "2024-01-15T10:30:00",
                                    "lastAction": "2024-11-07T14:20:00"
                                }
                            ],
                            "pageable": {
                                "pageNumber": 0,
                                "pageSize": 10,
                                "sort": {
                                    "sorted": true,
                                    "unsorted": false,
                                    "empty": false
                                }
                            },
                            "totalPages": 5,
                            "totalElements": 47,
                            "last": false,
                            "first": true,
                            "size": 10,
                            "number": 0,
                            "numberOfElements": 10,
                            "empty": false
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "filteredUsers",
                        description = "Respuesta cuando se busca por nombre (sin paginación)",
                        value = """
                        [
                            {
                                "id": 1,
                                "name": "Walter Jiménez",
                                "email": "walter@example.com",
                                "nationalId": "1234567890",
                                "phone": "+573001234567",
                                "roles": ["LIDER_COMUNITARIO"]
                            }
                        ]
                        """
                    )
                }
            )
        )
    })
    public ResponseEntity<?> searchUsers(
        @Parameter(description = "Filtrar por nombre (búsqueda parcial en toda la BD, con paginación)")
        @RequestParam(required = false) String name,
        
        @Parameter(description = "Filtrar por correo electrónico (búsqueda exacta, sin paginación)")
        @RequestParam(required = false) String email,
        
        @Parameter(description = "Filtrar por cédula (búsqueda exacta, sin paginación)")
        @RequestParam(required = false) String nationalId,
        
        @Parameter(description = "Número de página (comienza en 0)")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Cantidad de elementos por página (máximo 100)")
        @RequestParam(defaultValue = "10") int size,
        
        @Parameter(description = "Campo por el cual ordenar")
        @RequestParam(defaultValue = "name") String sortBy,
        
        @Parameter(description = "Dirección del ordenamiento: 'asc' o 'desc'")
        @RequestParam(defaultValue = "asc") String sortDirection) {
            
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = (Long) auth.getDetails();
    
       
        if (email != null) {
            return ResponseEntity.ok(userService.findByEmail(email));
        } else if (nationalId != null) {
            return ResponseEntity.ok(userService.findByNationalId(nationalId));
        } else if (name != null) {
            return ResponseEntity.ok(
                userService.findByNameWithPagination(name, currentUserId, page, size, sortBy, sortDirection)
            );
        } else {
            return ResponseEntity.ok(
                userService.findAllExceptCurrent(currentUserId, page, size, sortBy, sortDirection)
            );
        }
    }

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
            @Valid @RequestBody UserSaveDto userDto) {
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar usuario del sistema",
        description = """
            Elimina permanentemente un usuario de la base de datos.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol ADMIN pueden eliminar usuarios
            - No se puede eliminar el propio usuario ADMIN (evitar quedar sin acceso)
            - Considerar que eliminar un usuario puede afectar proyectos asociados
            
            **Advertencias:**
            - Esta es una operación destructiva e irreversible
            - Si el usuario tiene proyectos creados, considerar:
              * Reasignar proyectos a otro usuario antes de eliminar
              * Implementar "soft delete" (marcar como inactivo) en lugar de borrado físico
            - Si el usuario es curador, sus proyectos asignados quedarán sin curador
            
            **Recomendación:** Implementar desactivación de cuenta en lugar de eliminación física 
            para mantener integridad referencial e historial.
            
            **Efecto:**
            - Elimina el registro del usuario de la tabla users
            - Puede causar problemas de integridad referencial si hay proyectos asociados
            - Se pierden todos los datos y el historial del usuario
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario eliminado exitosamente del sistema",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(value = "\"User deleted successfully\"")
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN para eliminar usuarios",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 403,
                        "error": "Forbidden",
                        "message": "Acceso denegado. Se requiere rol ADMIN"
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
            responseCode = "400",
            description = "No se puede eliminar el usuario (ej: tiene proyectos activos, es el último ADMIN, etc.)",
            content = @Content
        )
    })
    public ResponseEntity<String> deleteUser(
            @Parameter(
                description = "ID único del usuario a eliminar (operación irreversible)",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PostMapping("/{id}/roles/{role}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Asignar rol adicional a un usuario",
        description = """
            Agrega un rol nuevo a un usuario existente. Los usuarios pueden tener múltiples roles simultáneamente.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol ADMIN pueden asignar roles
            - El usuario objetivo debe existir en la base de datos
            - El rol debe ser uno de los valores válidos del sistema
            
            **Roles disponibles en el sistema:**
            - **ADMIN:** Acceso total al sistema, gestión de usuarios y proyectos
            - **CURATOR:** Revisión y aprobación de proyectos comunitarios
            - **LIDER_COMUNITARIO:** Creación y gestión de proyectos comunitarios
            
            **Casos de uso:**
            - Promover un líder comunitario a curador: agregar rol CURATOR
            - Dar permisos administrativos temporales: agregar rol ADMIN
            - Usuario con múltiples responsabilidades: puede tener CURATOR + LIDER_COMUNITARIO
            
            **Comportamiento:**
            - Si el usuario ya tiene el rol, la operación falla con error 400
            - Los roles son acumulativos (no reemplazan roles existentes)
            - El cambio es inmediato y afecta las siguientes autenticaciones
            
            **Nota:** Para remover roles, usar el endpoint DELETE `/users/{id}/roles/{role}`
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Rol agregado exitosamente al usuario. Retorna el usuario con su lista actualizada de roles.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "userWithNewRole",
                    value = """
                    {
                        "id": 1,
                        "name": "Walter Jiménez",
                        "email": "walter@example.com",
                        "nationalId": "1234567890",
                        "phone": "+573001234567",
                        "roles": ["LIDER_COMUNITARIO", "CURATOR"]
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Rol no válido o el usuario ya tiene el rol asignado",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 400,
                        "error": "Bad Request",
                        "message": "El usuario ya tiene el rol CURATOR asignado"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN para asignar roles",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado con el ID especificado",
            content = @Content
        )
    })
    public ResponseEntity<UserDto> addRole(
            @Parameter(
                description = "ID único del usuario al que se le asignará el rol",
                example = "1",
                required = true
            )
            @PathVariable Long id, 
            
            @Parameter(
                description = """
                    Rol a agregar al usuario. Valores válidos:
                    - ADMIN: Administrador del sistema
                    - CURATOR: Curador de proyectos
                    - LIDER_COMUNITARIO: Líder comunitario
                    """,
                example = "CURATOR",
                required = true
            )
            @PathVariable String role) {
        return ResponseEntity.ok(userService.addRole(id, role));
    }

    @DeleteMapping("/{id}/roles/{role}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Remover rol de un usuario",
        description = """
            Elimina un rol específico de un usuario existente. El usuario puede mantener otros roles asignados.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol ADMIN pueden remover roles
            - El usuario objetivo debe existir en la base de datos
            - El rol debe estar actualmente asignado al usuario
            - No se puede remover el último rol de un usuario (debe tener al menos uno)
            
            **Roles que se pueden remover:**
            - ADMIN: Quita permisos administrativos
            - CURATOR: Quita capacidad de revisar proyectos
            - LIDER_COMUNITARIO: Quita capacidad de crear proyectos
            
            **Casos de uso:**
            - Degradar permisos temporales de ADMIN
            - Remover responsabilidades de curador por carga de trabajo
            - Desactivar capacidad de crear proyectos temporalmente
            - Cambiar el rol principal de un usuario
            
            **Consideraciones importantes:**
            - Si se remueve CURATOR, los proyectos asignados quedarán sin curador
            - Si se remueve LIDER_COMUNITARIO, el usuario no podrá crear nuevos proyectos
            - El cambio es inmediato y afecta las siguientes autenticaciones
            - Considerar reasignar responsabilidades antes de remover roles críticos
            
            **Restricción:** No se puede dejar un usuario sin roles (debe tener al menos uno activo)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Rol removido exitosamente. Retorna el usuario con su lista actualizada de roles.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "userWithRemovedRole",
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
            responseCode = "400",
            description = "Rol no válido, el usuario no tiene el rol asignado, o es el último rol del usuario",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 400,
                        "error": "Bad Request",
                        "message": "El usuario no tiene el rol CURATOR asignado"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN para remover roles",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado con el ID especificado",
            content = @Content
        )
    })
    public ResponseEntity<UserDto> removeRole(
            @Parameter(
                description = "ID único del usuario al que se le removerá el rol",
                example = "1",
                required = true
            )
            @PathVariable Long id, 
            
            @Parameter(
                description = """
                    Rol a eliminar del usuario. Valores válidos:
                    - ADMIN: Administrador del sistema
                    - CURATOR: Curador de proyectos
                    - LIDER_COMUNITARIO: Líder comunitario
                    
                    El rol debe estar actualmente asignado al usuario.
                    """,
                example = "CURATOR",
                required = true
            )
            @PathVariable String role) {
        return ResponseEntity.ok(userService.removeRole(id, role));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Crear nuevo usuario con roles asignados (Admin)",
        description = """
            Permite a un administrador crear un nuevo usuario en el sistema con roles específicos asignados desde el inicio.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol ADMIN pueden crear usuarios mediante este endpoint
            - Para registro público de usuarios, existe otro endpoint sin autenticación
            
            **Campos requeridos:**
            - name: Nombre completo del usuario
            - email: Correo electrónico único en el sistema
            - password: Contraseña (será encriptada automáticamente)
            - nationalId: Número de identificación nacional único
            - phone: Número telefónico de contacto
            - roles: Array con al menos un rol del sistema
            
            **Validaciones automáticas:**
            - El email debe ser único (no puede existir otro usuario con el mismo email)
            - El nationalId debe ser único en el sistema
            - La contraseña se encripta con bcrypt antes de almacenarse
            - Los roles deben ser valores válidos: ADMIN, CURATOR, LIDER_COMUNITARIO
            - El formato del email debe ser válido
            - El teléfono debe tener formato válido (según configuración)
            
            **Roles disponibles:**
            - **ADMIN:** Acceso completo al sistema
            - **CURATOR:** Puede revisar y aprobar proyectos
            - **LIDER_COMUNITARIO:** Puede crear y gestionar proyectos comunitarios
            
            **Casos de uso:**
            - Crear cuenta administrativa para nuevo empleado
            - Registrar curadores del sistema
            - Pre-registrar líderes comunitarios verificados
            - Crear usuarios con múltiples roles simultáneamente
            
            **Nota:** Para registro público de usuarios (sin autenticación), usar el endpoint de registro público.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Usuario creado exitosamente con los roles asignados. La contraseña ha sido encriptada.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "createdUser",
                    value = """
                    {
                        "id": 10,
                        "name": "Nuevo Usuario",
                        "email": "nuevo@example.com",
                        "nationalId": "9876543210",
                        "phone": "+573001234567",
                        "roles": ["LIDER_COMUNITARIO"]
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos: email duplicado, formato incorrecto, campos requeridos vacíos, o roles inválidos",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 400,
                        "error": "Bad Request",
                        "message": "El email nuevo@example.com ya está registrado en el sistema"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN para crear usuarios mediante este endpoint",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 403,
                        "error": "Forbidden",
                        "message": "Acceso denegado. Se requiere rol ADMIN"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<UserDto> createUserAdmin(
            @Valid @RequestBody UserSaveDto userDto) {
        return ResponseEntity.ok(userService.saveUser(userDto));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Cambiar el estado de un usuario (activar/desactivar)",
        description = """
            Permite a un administrador cambiar el estado de un usuario entre activo e inactivo.
            
            **Acciones posibles:**
            - Activar un usuario inactivo
            - Desactivar un usuario activo
            
            **Efectos:**
            - Un usuario desactivado no podrá iniciar sesión en el sistema
            - El cambio de estado se registra en el historial de acciones
            
            **Permisos requeridos:** Rol ADMIN
            
            **Nota:** No se puede desactivar a sí mismo el usuario que realiza la acción.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Estado del usuario actualizado correctamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acceso denegado. Se requiere rol de administrador",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 403,
                        "error": "Forbidden",
                        "message": "Acceso denegado. Se requiere rol ADMIN"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuario no encontrado con el ID especificado",
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
    public ResponseEntity<UserDto> toggleUserStatus(
            @Parameter(
                description = "ID único del usuario cuyo estado se desea cambiar",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }
}