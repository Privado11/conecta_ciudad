package com.unimagdalena.conectaCiudad.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.services.user.UserService;
import com.unimagdalena.conectaCiudad.validation.OnCreate;
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

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    summary = "Buscar y listar usuarios con filtros avanzados y paginación",
    description = """
    Permite buscar usuarios aplicando múltiples criterios de filtrado con soporte de paginación.
    
    **Criterios de búsqueda disponibles:**
    - **Por nombre:** Búsqueda parcial case-insensitive (ej: "walter" encuentra "Walter Jiménez")
    - **Por email:** Búsqueda exacta del correo electrónico (SIN paginación)
    - **Por cédula/ID nacional:** Búsqueda exacta del número de identificación (SIN paginación)
    - **Por rol:** Filtra usuarios que tengan el rol especificado
    - **Por estado:** Filtra usuarios activos (true) o inactivos (false)
    - **Combinado:** Puedes combinar rol + estado (ej: admins activos, curadores inactivos)
    - **Sin filtros:** Retorna el listado completo de usuarios registrados
    
    **Filtros combinables con paginación:**
    1. **Solo por rol:** `?role=ADMIN` → Todos los admins (activos e inactivos)
    2. **Solo por estado:** `?active=true` → Todos los usuarios activos (cualquier rol)
    3. **Rol + Estado:** `?role=CURATOR&active=false` → Curadores inactivos
    4. **Nombre + Rol:** `?name=Juan&role=LIDER_COMUNITARIO` → Líderes llamados Juan
    5. **Nombre + Estado:** `?name=Maria&active=true` → Usuarios activas llamadas María
    6. **Nombre + Rol + Estado:** `?name=Pedro&role=ADMIN&active=true` → Admins activos llamados Pedro
    
    **Roles válidos:**
    - ADMIN, CIUDADANO, CURATOR, LIDER_COMUNITARIO
    
    **Parámetros de paginación:**
    - **page:** Número de página (comienza en 0, por defecto: 0)
    - **size:** Elementos por página (por defecto: 10, máximo: 100)
    - **sortBy:** Campo para ordenar (por defecto: "name")
    - **sortDirection:** Dirección del ordenamiento "asc" o "desc" (por defecto: "asc")
    
    **Campos disponibles para ordenar:**
    - name, email, nationalId, createdAt, phone, active
    
    **Comportamiento especial:**
    - **Con 'email' o 'nationalId':** Retorna objeto único sin paginación (búsqueda exacta)
    - **Con cualquier otro filtro:** Retorna Page<UserDto> con paginación
    """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de usuarios que coinciden con los criterios",
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "adminActivos",
                    description = "Ejemplo: Buscar todos los admins activos",
                    value = """
                    {
                        "content": [
                            {
                                "id": 1,
                                "name": "Juan Admin",
                                "email": "juan@example.com",
                                "nationalId": "1234567890",
                                "phone": "+573001234567",
                                "roles": ["ADMIN"],
                                "active": true,
                                "createdAt": "2024-01-15T10:30:00",
                                "lastAction": "2024-11-07T14:20:00"
                            }
                        ],
                        "totalPages": 2,
                        "totalElements": 15,
                        "size": 10,
                        "number": 0
                    }
                    """
                ),
                @ExampleObject(
                    name = "usuariosInactivos",
                    description = "Ejemplo: Buscar todos los usuarios inactivos (cualquier rol)",
                    value = """
                    {
                        "content": [
                            {
                                "id": 5,
                                "name": "María García",
                                "email": "maria@example.com",
                                "nationalId": "9876543210",
                                "phone": "+573009876543",
                                "roles": ["LIDER_COMUNITARIO"],
                                "active": false,
                                "createdAt": "2024-02-20T09:15:00",
                                "lastAction": "2024-10-30T11:45:00"
                            }
                        ],
                        "totalPages": 1,
                        "totalElements": 8,
                        "size": 10,
                        "number": 0
                    }
                    """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "Rol no válido especificado en los filtros",
        content = @Content(
            examples = @ExampleObject(
                value = """
                {
                    "status": 400,
                    "error": "Bad Request",
                    "message": "Rol no válido: SUPERUSER. Roles permitidos: ADMIN, CIUDADANO, CURATOR, LIDER_COMUNITARIO"
                }
                """
            )
        )
    )
})
public ResponseEntity<?> searchUsers(
    @Parameter(description = "Filtrar por nombre (búsqueda parcial)")
    @RequestParam(required = false) String name,
    
    @Parameter(description = "Filtrar por correo electrónico (búsqueda exacta, sin paginación)")
    @RequestParam(required = false) String email,
    
    @Parameter(description = "Filtrar por cédula (búsqueda exacta, sin paginación)")
    @RequestParam(required = false) String nationalId,
    
    @Parameter(
        description = "Filtrar por rol. Valores: ADMIN, CIUDADANO, CURATOR, LIDER_COMUNITARIO",
        example = "ADMIN"
    )
    @RequestParam(required = false) String role,
    
    @Parameter(
        description = "Filtrar por estado: true (activos) o false (inactivos)",
        example = "true"
    )
    @RequestParam(required = false) Boolean active,
    
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
    } 
    
    if (nationalId != null) {
        return ResponseEntity.ok(userService.findByNationalId(nationalId));
    }
    
    if (name != null) {
        return ResponseEntity.ok(
            userService.findByNameAndFilters(name, role, active, currentUserId, 
                                             page, size, sortBy, sortDirection)
        );
    }
    
    if (role != null || active != null) {
        return ResponseEntity.ok(
            userService.findByFilters(role, active, currentUserId, 
                                      page, size, sortBy, sortDirection)
        );
    }
    
    return ResponseEntity.ok(
        userService.findAllExceptCurrent(currentUserId, page, size, sortBy, sortDirection)
    );
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
            @Validated(OnUpdate.class) @RequestBody UserSaveDto userDto) {
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
            @Validated(OnCreate.class) @RequestBody UserSaveDto userDto) {
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

@PostMapping("/import")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Importar múltiples usuarios desde archivo CSV",
        description = """
            Permite a un administrador importar múltiples usuarios al sistema mediante un archivo CSV.
            
            **Formato del archivo CSV requerido:**
            
            ```csv
            name,email,nationalId,phone,password,role
            Juan Pérez,juan@example.com,1234567890,+573001234567,Password123,ADMIN
            María López,maria@example.com,9876543210,+573009876543,Password456,CIUDADANO
            Pedro García,pedro@example.com,5555555555,+573005555555,Password789,CURATOR
            ```
            
            **Campos del CSV:**
            - **name** (obligatorio): Nombre completo del usuario
            - **email** (obligatorio): Correo electrónico único
            - **nationalId** (obligatorio): Número de identificación nacional único
            - **phone** (obligatorio): Número telefónico
            - **password** (obligatorio): Contraseña en texto plano (se encriptará automáticamente)
            - **role** (opcional): Rol del usuario. Si no se especifica, se asigna "CIUDADANO"
            
            **Roles válidos:**
            - ADMIN, CIUDADANO, CURATOR, LIDER_COMUNITARIO
            
            **Validaciones automáticas:**
            1. **Duplicados dentro del CSV:** Detecta emails o cédulas duplicadas en el mismo archivo
            2. **Duplicados en BD:** Verifica que emails y cédulas no existan en la base de datos
            3. **Campos obligatorios:** Valida que todos los campos requeridos estén presentes
            4. **Roles válidos:** Verifica que los roles especificados sean correctos
            5. **Formato de datos:** Valida que el formato de emails y teléfonos sea correcto
            
            **Procesamiento:**
            - Se procesan todos los registros válidos
            - Los registros con errores se reportan detalladamente (número de fila y motivo)
            - La operación es **transaccional**: si hay un error crítico, se hace rollback completo
            - Los registros exitosos se guardan en lote para optimizar rendimiento
            
            **Respuesta:**
            La respuesta incluye:
            - Total de registros procesados
            - Cantidad de importaciones exitosas
            - Cantidad de importaciones fallidas
            - Lista detallada de errores (fila, email, cédula, rol, mensaje de error)
            - Lista de usuarios importados exitosamente
            
            **Ejemplo de respuesta:**
            ```json
            {
                "totalProcessed": 100,
                "successfulImports": 95,
                "failedImports": 5,
                "errors": [
                    {
                        "rowNumber": 3,
                        "email": "duplicado@example.com",
                        "nationalId": "1234567890",
                        "role": "ADMIN",
                        "errorMessage": "El email ya existe en la base de datos"
                    }
                ],
                "importedUsers": [...]
            }
            ```
            
            **Notas importantes:**
            - El archivo debe estar en formato CSV con encoding UTF-8
            - El tamaño máximo del archivo depende de la configuración del servidor
            - Se recomienda no superar 1000 usuarios por archivo para mejor rendimiento
            - La primera fila puede ser un header (se detecta automáticamente)
            - Los valores con comas deben estar entre comillas: "Pérez, Juan"
            
            **Permisos:** Solo usuarios con rol ADMIN pueden importar usuarios
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Importación completada (puede incluir errores parciales). Revisa el detalle de la respuesta.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BulkUserImportResult.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Archivo inválido, formato incorrecto, o error crítico durante el procesamiento",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN para importar usuarios",
            content = @Content
        )
    })
    public ResponseEntity<BulkUserImportResult> importUsers(
            @Parameter(
                description = "Archivo CSV con los datos de los usuarios a importar",
                required = true
            )
            @RequestParam("file") MultipartFile file) throws IOException {
        
        BulkUserImportResult result = userService.importUsersFromCSV(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Exportar usuarios a archivo CSV",
        description = """
            Permite a un administrador exportar usuarios del sistema a un archivo CSV.
            
            **Parámetros de filtrado:**
            - **name:** Filtrar por nombre (búsqueda parcial)
            - **role:** Filtrar por rol específico
            - **active:** Filtrar por estado (true/false)
            - **Sin parámetros:** Exporta TODOS los usuarios del sistema
            
            **Formato del archivo CSV generado:**
            ```csv
            name,email,nationalId,phone,role,active,createdAt
            Juan Pérez,juan@example.com,1234567890,+573001234567,ADMIN,true,2024-01-15T10:30:00
            ```
            
            **Características del archivo:**
            - Encoding UTF-8 (compatible con Excel y Google Sheets)
            - Valores con comas automáticamente escapados entre comillas
            - Formato de fecha: ISO 8601
            - Header descriptivo en la primera fila
            
            **Ejemplos de uso:**
            - `/api/v1/users/export` → Exporta todos los usuarios
            - `/api/v1/users/export?role=ADMIN` → Solo administradores
            - `/api/v1/users/export?active=false` → Solo usuarios inactivos
            
            **Permisos:** Solo usuarios con rol ADMIN pueden exportar usuarios
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Archivo CSV generado exitosamente",
            content = @Content(mediaType = "text/csv")
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetros de filtrado inválidos",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN",
            content = @Content
        )
    })
    public ResponseEntity<byte[]> exportUsers(
            @Parameter(description = "Filtrar por nombre (búsqueda parcial)")
            @RequestParam(required = false) String name,
            
            @Parameter(description = "Filtrar por rol")
            @RequestParam(required = false) String role,
            
            @Parameter(description = "Filtrar por estado")
            @RequestParam(required = false) Boolean active,
            
            @Parameter(description = "Número de página")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Elementos por página")
            @RequestParam(defaultValue = "1000") int size,
            
            @Parameter(description = "Campo para ordenar")
            @RequestParam(defaultValue = "name") String sortBy,
            
            @Parameter(description = "Dirección del ordenamiento")
            @RequestParam(defaultValue = "asc") String sortDirection) throws IOException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = (Long) auth.getDetails();
        

        List<UserDto> users;
        
        if (name != null) {
            Page<UserDto> pagedUsers = userService.findByNameAndFilters(
                name, role, active, currentUserId, page, size, sortBy, sortDirection
            );
            users = pagedUsers.getContent();
        } else if (role != null || active != null) {
            Page<UserDto> pagedUsers = userService.findByFilters(
                role, active, currentUserId, page, size, sortBy, sortDirection
            );
            users = pagedUsers.getContent();
        } else {
            users = userService.findAll();
        }
        

        byte[] csvData = userService.exportUsersToCSV(users);
        

        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );
        String filename = "usuarios_" + timestamp + ".csv";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/export/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Exportar TODOS los usuarios del sistema a CSV",
        description = """
            Exporta todos los usuarios del sistema sin aplicar ningún filtro.
            
            **Uso recomendado:**
            - Backup completo de usuarios
            - Auditoría general del sistema
            - Migración completa de datos
            
            **Formato del archivo:** Mismo formato CSV que el endpoint /export
            
            **Permisos:** Solo usuarios con rol ADMIN
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Archivo CSV con todos los usuarios generado exitosamente",
            content = @Content(mediaType = "text/csv")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN",
            content = @Content
        )
    })
    public ResponseEntity<byte[]> exportAllUsers() throws IOException {
        byte[] csvData = userService.exportAllUsersToCSV();
        
        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );
        String filename = "usuarios_completo_" + timestamp + ".csv";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

}