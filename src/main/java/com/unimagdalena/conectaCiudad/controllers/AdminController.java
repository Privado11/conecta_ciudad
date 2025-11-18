package com.unimagdalena.conectaCiudad.controllers;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.CuratorInfoDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.services.admin.AdminService;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(
        name = "Administración",
        description = "Operaciones administrativas del sistema: gestión de usuarios, importación masiva, roles y reportes."
)
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private static final int MAX_PAGE_SIZE = 100;

    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping("/users")
    @Operation(
            summary = "Buscar y listar usuarios con filtros avanzados y paginación",
            description = """
        Permite buscar usuarios aplicando múltiples criterios de filtrado con soporte de paginación.
        
        **Respuesta mejorada:**
        Ahora incluye estadísticas globales que respetan los filtros aplicados:
        - **page:** Datos paginados de usuarios
        - **statistics:** 
          - total: Total de usuarios que cumplen con los filtros
          - active: Total de usuarios activos que cumplen con los filtros
          - inactive: Total de usuarios inactivos que cumplen con los filtros
        
        **Criterios de búsqueda disponibles:**
        - **Por nombre:** Búsqueda parcial case-insensitive
        - **Por email:** Búsqueda exacta del correo electrónico (SIN paginación)
        - **Por cédula/ID nacional:** Búsqueda exacta (SIN paginación)
        - **Por rol:** Filtra usuarios que tengan el rol especificado
        - **Por estado:** Filtra usuarios activos (true) o inactivos (false)
        - **Combinado:** rol + estado + nombre
        
        **Roles válidos:**
        - ADMIN, CIUDADANO, CURATOR, LIDER_COMUNITARIO
        
        **Parámetros de paginación:**
        - page, size, sortBy, sortDirection
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de usuarios con estadísticas",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "adminActivos",
                                            description = "Ejemplo: Buscar todos los admins (muestra activos e inactivos en stats)",
                                            value = """
                        {
                            "page": {
                                "content": [
                                    {
                                        "id": 1,
                                        "name": "Juan Admin",
                                        "email": "juan@example.com",
                                        "nationalId": "1234567890",
                                        "phone": "+573001234567",
                                        "roles": ["ADMIN"],
                                        "active": true
                                    }
                                ],
                                "totalElements": 8,
                                "totalPages": 1,
                                "size": 10,
                                "number": 0
                            },
                            "statistics": {
                                "total": 10,
                                "active": 8,
                                "inactive": 2
                            }
                        }
                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Rol no válido especificado en los filtros",
                    content = @Content
            )
    })
    public ResponseEntity<?> searchUsers(
            @Parameter(description = "Filtrar por nombre (búsqueda parcial)")
            @RequestParam(required = false) String name,

            @Parameter(description = "Filtrar por correo electrónico (búsqueda exacta, sin paginación)")
            @RequestParam(required = false) String email,

            @Parameter(description = "Filtrar por cédula (búsqueda exacta, sin paginación)")
            @RequestParam(required = false) String nationalId,

            @Parameter(description = "Filtrar por rol")
            @RequestParam(required = false) String role,

            @Parameter(description = "Filtrar por estado: true (activos) o false (inactivos)")
            @RequestParam(required = false) Boolean active,

            @Parameter(description = "Número de página (comienza en 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Cantidad de elementos por página")
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

        PagedResponse<UserDto> response;

        if (name != null) {
            response = adminService.findByNameAndFilters(
                    name, role, active, currentUserId,
                    page, size, sortBy, sortDirection
            );
        } else if (role != null || active != null) {
            response = adminService.findByFilters(
                    role, active, currentUserId,
                    page, size, sortBy, sortDirection
            );
        } else {
            response = adminService.findAllExceptCurrent(
                    currentUserId, page, size, sortBy, sortDirection
            );
        }

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @PutMapping("/user/{id}")
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
        return ResponseEntity.ok(adminService.updateUser(id, userDto));
    }

    @PreAuthorize("hasAuthority('USER_DELETE')")
    @DeleteMapping("/user/{id}")
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

        adminService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    @PostMapping("/user/{id}/roles/{role}")
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
        return ResponseEntity.ok(adminService.assignRole(id, role));
    }

    @PreAuthorize("hasAuthority('USER_ROLE_REMOVE')")
    @DeleteMapping("/user/{id}/roles/{role}")
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
        return ResponseEntity.ok(adminService.removeRole(id, role));
    }

    @PreAuthorize("hasAuthority('USER_CREATE')")
    @PostMapping("/users")
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
        return ResponseEntity.ok(adminService.createUser(userDto));
    }

    @PreAuthorize("hasAuthority('USER_TOGGLE_STATUS')")
    @PatchMapping("/user/{id}/toggle-status")
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
        return ResponseEntity.ok(adminService.toggleUserStatus(id));
    }

    @PreAuthorize("hasAuthority('USER_IMPORT')")
    @PostMapping("/users/import")

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

        BulkUserImportResult result = adminService.importUsersFromCSV(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/export")
    @PreAuthorize("hasAuthority('USER_EXPORT')")
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
            PagedResponse<UserDto> response = adminService.findByNameAndFilters(
                    name, role, active, currentUserId, page, size, sortBy, sortDirection
            );
            users = response.page().getContent();
        } else if (role != null || active != null) {
            PagedResponse<UserDto> response = adminService.findByFilters(
                    role, active, currentUserId, page, size, sortBy, sortDirection
            );
            users = response.page().getContent();
        } else {
            users = adminService.findAll();
        }


        byte[] csvData = adminService.exportUsersToCSV(users);


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

    @GetMapping("/users/export/all")
    @PreAuthorize("hasAuthority('USER_EXPORT_ALL')")
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
        byte[] csvData = adminService.exportAllUsersToCSV();

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

    @GetMapping("/users/curators/{projectId}")
    @Operation(summary = "Curador actual y lista de curadores con estadísticas")
    public ResponseEntity<CuratorInfoDto> getCuratorsWithStats(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(adminService.findAllCuratorsWithStats(projectId));
    }

    @PreAuthorize("hasAuthority('PROJECT_ASSIGN_CURATOR')")
    @PutMapping("project/{id}/curator")
    @Operation(
            summary = "Asignar o reasignar curador a un proyecto",
            description = "Permite a un administrador asignar o cambiar el curador de un proyecto."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Curador asignado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos ADMIN"),
            @ApiResponse(responseCode = "404", description = "Proyecto o curador no encontrado")
    })
    public ResponseEntity<ProjectDto> reassignCurator(
            @PathVariable Long id,
            @RequestParam Long curatorId,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(adminService.reassignCurator(id, curatorId, adminId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW') or hasAuthority('PROJECT_SEARCH')")
    @GetMapping("/projects/search")
    @Operation(
            summary = "Búsqueda avanzada de proyectos",
            description = """
            Endpoint unificado para búsqueda y filtrado de proyectos con múltiples criterios:
            - Búsqueda por texto (searchTerm)
            - Filtro por estado (status)
            - Filtro por creador (creatorId)
            - Filtro por curador (curatorId)
            - Rangos de fechas (inicio, fin, creación)
            Incluye paginación y estadísticas agregadas.
            """
    )
    public ResponseEntity<Page<ProjectDto>> searchProjects(
            @Parameter(description = "Término de búsqueda (nombre, objetivos, población)")
            @RequestParam(required = false) String searchTerm,

            @Parameter(description = "Estado del proyecto")
            @RequestParam(required = false) ProjectStatus status,

            @Parameter(description = "ID del creador del proyecto")
            @RequestParam(required = false) Long creatorId,

            @Parameter(description = "ID del curador asignado")
            @RequestParam(required = false) Long curatorId,

            @Parameter(description = "Fecha de inicio del proyecto desde")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate projectStartFrom,

            @Parameter(description = "Fecha de inicio del proyecto hasta")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate projectStartTo,

            @Parameter(description = "Fecha de fin del proyecto desde")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate projectEndFrom,

            @Parameter(description = "Fecha de fin del proyecto hasta")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate projectEndTo,

            @Parameter(description = "Fecha de inicio de votación desde")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate votingStartFrom,

            @Parameter(description = "Fecha de inicio de votación hasta")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate votingStartTo,

            @Parameter(description = "Fecha de fin de votación desde")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate votingEndFrom,

            @Parameter(description = "Fecha de fin de votación hasta")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate votingEndTo,

            @Parameter(description = "Fecha de creación desde")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdFrom,

            @Parameter(description = "Fecha de creación hasta")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime createdTo,

            @Parameter(description = "Número de página (inicia en 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página (máximo 100)")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Campo para ordenar")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Dirección de ordenamiento (ASC o DESC)")
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        size = Math.min(size, MAX_PAGE_SIZE);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ProjectDto> response = adminService.findWithFilters(
                searchTerm,
                status,
                creatorId,
                curatorId,
                projectStartFrom,
                projectStartTo,
                projectEndFrom,
                projectEndTo,
                votingStartFrom,
                votingStartTo,
                votingEndFrom,
                votingEndTo,
                createdFrom,
                createdTo,
                pageable
        );

        return ResponseEntity.ok(response);
    }
}
