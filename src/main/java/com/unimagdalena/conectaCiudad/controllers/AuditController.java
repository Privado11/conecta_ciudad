package com.unimagdalena.conectaCiudad.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.services.audit.AuditService;

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
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(
    name = "Auditoría", 
    description = """
        API para gestión de auditoría y trazabilidad de acciones del sistema.
        Permite rastrear todas las operaciones críticas realizadas por los usuarios,
        proporcionando un historial completo de actividad para fines de seguridad,
        cumplimiento normativo y análisis de comportamiento del sistema.
        """
)
public class AuditController {

    private final AuditService auditService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    @Operation(
        summary = "Obtener el historial completo de acciones del sistema (Solo ADMIN)",
        description = """
            Retorna todas las acciones registradas en el sistema sin aplicar ningún filtro.
            Este endpoint proporciona una vista completa del historial de auditoría.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol ADMIN pueden acceder a este endpoint
            - Retorna todas las acciones de todos los usuarios del sistema
            - No aplica ningún filtro por fecha, usuario o tipo de acción
            
            **Tipos de acciones registradas:**
            
            **Acciones de Proyecto:**
            - **PROJECT_CREATED**: Cuando un líder comunitario crea un nuevo proyecto
            - **PROJECT_UPDATED**: Cuando se actualiza la información de un proyecto
            - **PROJECT_DELETED**: Cuando se elimina un proyecto del sistema
            - **CURATOR_ASSIGNED**: Cuando se asigna un curador a un proyecto por primera vez
            - **CURATOR_REASSIGNED**: Cuando se cambia el curador asignado a un proyecto
            - **PROJECT_OBSERVATIONS_ADDED**: Cuando un curador registra observaciones
            - **PROJECT_APPROVED**: Cuando un curador aprueba un proyecto
            
            **Acciones de Usuario:**
            - **USER_CREATED**: Cuando se crea un nuevo usuario en el sistema
            - **USER_UPDATED**: Cuando se actualiza la información de un usuario
            - **USER_DELETED**: Cuando se elimina un usuario del sistema
            - **USER_ROLE_ADDED**: Cuando se asigna un nuevo rol a un usuario
            - **USER_ROLE_REMOVED**: Cuando se remueve un rol de un usuario
            - **USER_LOGIN**: Cuando un usuario inicia sesión
            
            **Acciones de Ciudadano:**
            - **CITIZEN_VOTE**: Cuando un ciudadano vota por un proyecto
            - **CITIZEN_COMMENT**: Cuando un ciudadano realiza un comentario en un proyecto


            **Información incluida en cada acción:**
            - ID único de la acción
            - Nombre/tipo de la acción
            - Descripción detallada de lo ocurrido
            - Fecha y hora exacta (timestamp)
            - Usuario que realizó la acción (ID, nombre, email, roles)
            
            **Casos de uso:**
            - Auditorías de seguridad y cumplimiento normativo
            - Investigación de incidentes o errores
            - Análisis de actividad del sistema
            - Generación de reportes de uso
            - Identificación de patrones de comportamiento
            - Verificación de operaciones críticas
            
            **Nota importante:** Este endpoint puede retornar grandes volúmenes de datos.
            Para consultas específicas, considere usar los endpoints filtrados:
            `/user/{userId}`, `/search?name=X`, o `/recent?limit=N`
            
            **Ordenamiento:** Las acciones se retornan ordenadas por fecha, de más reciente a más antigua.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista completa de todas las acciones registradas en el sistema (puede ser muy extensa)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ActionDto[].class),
                examples = @ExampleObject(
                    name = "allActionsResponse",
                    value = """
                    [
                        {
                            "id": 245,
                            "name": "PROJECT_APPROVED",
                            "description": "Proyecto 15 aprobado (listo para publicar)",
                            "actionAt": "2024-11-01T14:30:25",
                            "access": {
                                "id": 145,
                                "accessAt": "2024-11-01T14:25:00",
                                "user": {
                                    "id": 2,
                                    "name": "Nicole Hernández",
                                    "email": "nicole.hernandez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.100",
                                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                                "location": "Santa Marta, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 244,
                            "name": "PROJECT_OBSERVATIONS_ADDED",
                            "description": "Observaciones registradas para proyecto 12",
                            "actionAt": "2024-11-01T11:15:40",
                            "access": {
                                "id": 144,
                                "accessAt": "2024-11-01T11:10:30",
                                "user": {
                                    "id": 3,
                                    "name": "Carlos Rodríguez",
                                    "email": "carlos.rodriguez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.105",
                                "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
                                "location": "Barranquilla, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 243,
                            "name": "CURATOR_REASSIGNED",
                            "description": "Curador reasignado a proyecto 8 -> usuario 2",
                            "actionAt": "2024-11-01T09:45:12",
                            "access": {
                                "id": 143,
                                "accessAt": "2024-11-01T09:40:00",
                                "user": {
                                    "id": 1,
                                    "name": "Admin Principal",
                                    "email": "admin@conectaciudad.com",
                                    "roles": ["ADMIN"]
                                },
                                "ipAddress": "192.168.1.50",
                                "userAgent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
                                "location": "Cartagena, Colombia",
                                "success": true
                            }
                        }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado - Token JWT inválido, expirado o ausente",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token JWT inválido o expirado"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - El usuario no tiene rol ADMIN",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 403,
                        "error": "Forbidden",
                        "message": "Acceso denegado. Se requiere rol ADMIN para acceder a los registros de auditoría"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<List<ActionDto>> getAllActions() {
        return ResponseEntity.ok(auditService.findAllActions());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Obtener historial de acciones de un usuario específico (Solo ADMIN)",
        description = """
            Retorna todas las acciones realizadas por un usuario específico del sistema.
            Útil para auditorías individuales, investigaciones de seguridad o análisis de comportamiento de usuario.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol ADMIN pueden acceder a este endpoint
            - Retorna únicamente las acciones donde el user_id coincide con el parámetro proporcionado
            - No muestra acciones de otros usuarios
            
            **Casos de uso:**
            - Auditoría de actividad de un usuario específico
            - Investigar comportamiento sospechoso o irregular
            - Verificar qué acciones realizó un usuario en un período
            - Análisis de productividad de curadores
            - Rastrear modificaciones hechas por un líder comunitario específico
            - Revisar historial de un administrador para auditorías internas
            
            **Información retornada:**
            - Todas las acciones realizadas por el usuario (creación, actualización, aprobación, etc.)
            - Fechas y horas exactas de cada acción
            - Descripciones detalladas de las operaciones realizadas
            - Contexto completo (sobre qué proyectos u objetos actuó)
            
            **Ordenamiento:** Las acciones se retornan de más reciente a más antigua.
            
            **Nota:** Si el userId no existe en la base de datos o el usuario no ha realizado ninguna acción,
            se retorna un array vacío (no un error 404).
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de acciones realizadas por el usuario especificado (array vacío si el usuario no ha realizado acciones)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ActionDto[].class),
                examples = @ExampleObject(
                    name = "userActionsResponse",
                    value = """
                    [
                        {
                            "id": 245,
                            "name": "PROJECT_APPROVED",
                            "description": "Proyecto 15 aprobado (listo para publicar)",
                            "actionAt": "2024-11-01T14:30:25",
                            "access": {
                                "id": 145,
                                "accessAt": "2024-11-01T14:25:00",
                                "user": {
                                    "id": 2,
                                    "name": "Nicole Hernández",
                                    "email": "nicole.hernandez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.100",
                                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                "location": "Santa Marta, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 238,
                            "name": "PROJECT_OBSERVATIONS_ADDED",
                            "description": "Observaciones registradas para proyecto 18",
                            "actionAt": "2024-10-30T16:45:10",
                            "access": {
                                "id": 138,
                                "accessAt": "2024-10-30T16:40:00",
                                "user": {
                                    "id": 2,
                                    "name": "Nicole Hernández",
                                    "email": "nicole.hernandez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.100",
                                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                "location": "Santa Marta, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 235,
                            "name": "CURATOR_ASSIGNED",
                            "description": "Curator asignado al proyecto 18",
                            "actionAt": "2024-10-29T09:20:33",
                            "access": {
                                "id": 135,
                                "accessAt": "2024-10-29T09:15:20",
                                "user": {
                                    "id": 2,
                                    "name": "Nicole Hernández",
                                    "email": "nicole.hernandez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.100",
                                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                "location": "Santa Marta, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 230,
                            "name": "PROJECT_APPROVED",
                            "description": "Proyecto 12 aprobado (listo para publicar)",
                            "actionAt": "2024-10-28T11:15:00",
                            "access": {
                                "id": 130,
                                "accessAt": "2024-10-28T11:10:45",
                                "user": {
                                    "id": 2,
                                    "name": "Nicole Hernández",
                                    "email": "nicole.hernandez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.100",
                                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                "location": "Santa Marta, Colombia",
                                "success": true
                            }
                        }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado - Token JWT inválido, expirado o ausente",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN",
            content = @Content
        )
    })
    public ResponseEntity<List<ActionDto>> getActionsByUser(
            @Parameter(
                description = """
                    ID único del usuario del cual se desea consultar el historial de acciones.
                    Debe ser un identificador numérico válido correspondiente a un usuario existente en la tabla users.
                    
                    Ejemplos de usuarios típicos:
                    - ID de un líder comunitario para ver sus creaciones/actualizaciones de proyectos
                    - ID de un curador para ver sus revisiones y aprobaciones
                    - ID de un administrador para auditar sus acciones administrativas
                    """,
                example = "2",
                required = true
            )
            @PathVariable Long userId) {
        return ResponseEntity.ok(auditService.findActionsByUserId(userId));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/search")
    @Operation(
        summary = "Buscar acciones por nombre/tipo de acción (Solo ADMIN)",
        description = """
            Permite buscar acciones en el sistema filtrando por el nombre o tipo de acción.
            La búsqueda es parcial y case-insensitive (no distingue mayúsculas/minúsculas).
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol ADMIN pueden acceder a este endpoint
            - Busca en todas las acciones del sistema sin importar el usuario
            
            **Funcionalidad de búsqueda:**
            - **Búsqueda parcial:** No necesita coincidir exactamente con el nombre de la acción
            - **Case-insensitive:** "PROJECT", "project", "Project" dan los mismos resultados
            - **Coincidencias múltiples:** Retorna todas las acciones que contengan el término buscado
            
            **Tipos de acciones que puedes buscar:**
            - `PROJECT` → Encuentra todas las acciones relacionadas con proyectos
            - `CREATED` → Encuentra todas las creaciones (PROJECT_CREATED, USER_CREATED, etc.)
            - `APPROVED` → Encuentra todas las aprobaciones
            - `CURATOR` → Encuentra asignaciones y reasignaciones de curadores
            - `OBSERVATIONS` → Encuentra registros de observaciones
            - `UPDATED` → Encuentra actualizaciones
            - `DELETED` → Encuentra eliminaciones
            
            **Ejemplos de búsqueda:**
            - `name=PROJECT_CREATED` → Solo creaciones de proyectos
            - `name=CURATOR` → Todas las acciones relacionadas con curadores
            - `name=APPROVED` → Todas las aprobaciones del sistema
            - `name=PROJECT` → Todas las acciones relacionadas con proyectos
            
            **Casos de uso:**
            - Filtrar por tipo específico de acción para análisis
            - Generar reportes de operaciones específicas (ej: solo aprobaciones)
            - Identificar patrones de comportamiento por tipo de acción
            - Auditorías temáticas (ej: todas las reasignaciones de curadores)
            - Investigar incidentes específicos (ej: eliminaciones)
            
            **Ordenamiento:** Las acciones se retornan de más reciente a más antigua.
            
            **Nota:** Si no se encuentran coincidencias, retorna un array vacío (no un error 404).
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de acciones que coinciden con el criterio de búsqueda (array vacío si no hay coincidencias)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ActionDto[].class),
                examples = @ExampleObject(
                    name = "searchActionsResponse",
                    value = """
                    [
                         {
                            "id": 245,
                            "name": "PROJECT_APPROVED",
                            "description": "Proyecto 15 aprobado (listo para publicar)",
                            "actionAt": "2024-11-01T14:30:25",
                            "access": {
                                "id": 145,
                                "accessAt": "2024-11-01T14:25:00",
                                "user": {
                                    "id": 2,
                                    "name": "Nicole Hernández",
                                    "email": "nicole.hernandez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.100",
                                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                "location": "Santa Marta, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 230,
                            "name": "PROJECT_APPROVED",
                            "description": "Proyecto 12 aprobado (listo para publicar)",
                            "actionAt": "2024-10-28T11:15:00",
                            "access": {
                                "id": 130,
                                "accessAt": "2024-10-28T11:10:45",
                                "user": {
                                    "id": 2,
                                    "name": "Nicole Hernández",
                                    "email": "nicole.hernandez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.100",
                                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                "location": "Santa Marta, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 218,
                            "name": "PROJECT_APPROVED",
                            "description": "Proyecto 8 aprobado (listo para publicar)",
                            "actionAt": "2024-10-25T09:40:15",
                            "access": {
                                "id": 118,
                                "accessAt": "2024-10-25T09:35:00",
                                "user": {
                                    "id": 3,
                                    "name": "Carlos Rodríguez",
                                    "email": "carlos.rodriguez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.105",
                                "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
                                "location": "Barranquilla, Colombia",
                                "success": true
                            }
                        }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetro de búsqueda inválido o vacío",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 400,
                        "error": "Bad Request",
                        "message": "El parámetro 'name' es requerido y no puede estar vacío"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado - Token JWT inválido, expirado o ausente",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN",
            content = @Content
        )
    })
    public ResponseEntity<List<ActionDto>> searchActions(
            @Parameter(
                description = """
                    Término de búsqueda para filtrar acciones por nombre/tipo.
                    La búsqueda es parcial y case-insensitive.
                    
                    Valores comunes para buscar:
                    - "PROJECT_CREATED" → Creaciones de proyectos
                    - "PROJECT_APPROVED" → Aprobaciones de proyectos
                    - "CURATOR_ASSIGNED" → Asignaciones de curadores
                    - "CURATOR_REASSIGNED" → Reasignaciones de curadores
                    - "PROJECT_OBSERVATIONS_ADDED" → Observaciones agregadas
                    - "PROJECT_UPDATED" → Actualizaciones de proyectos
                    - "PROJECT_DELETED" → Eliminaciones de proyectos
                    
                    También puedes buscar parcialmente:
                    - "PROJECT" → Todas las acciones de proyectos
                    - "CURATOR" → Todas las acciones de curadores
                    - "APPROVED" → Todas las aprobaciones
                    """,
                example = "PROJECT_APPROVED",
                required = true
            )
            @RequestParam String name) {
        return ResponseEntity.ok(auditService.findActionsByNameContaining(name));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/recent")
    @Operation(
        summary = "Obtener las acciones más recientes del sistema (Solo ADMIN)",
        description = """
            Retorna las acciones más recientes registradas en el sistema, limitadas a un número específico.
            Ideal para dashboards administrativos, monitoreo en tiempo real y vista rápida de actividad reciente.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol ADMIN pueden acceder a este endpoint
            - Retorna acciones de todos los usuarios del sistema
            
            **Funcionalidad:**
            - Retorna las N acciones más recientes ordenadas por fecha descendente (más reciente primero)
            - Permite especificar cuántas acciones retornar mediante el parámetro `limit`
            - Si no se especifica el `limit`, retorna las últimas 10 acciones por defecto
            
            **Parámetro limit:**
            - **Valor por defecto:** 10 acciones
            - **Mínimo recomendado:** 1
            - **Máximo recomendado:** 100 (para evitar respuestas muy pesadas)
            - **Uso típico:** Entre 10 y 50 acciones para dashboards
            
            **Casos de uso:**
            - **Dashboard administrativo:** Ver últimas 20 acciones en tiempo real
            - **Monitoreo de actividad:** Verificar qué está pasando en el sistema ahora
            - **Feed de actividad:** Mostrar actividad reciente a administradores
            - **Detección rápida de problemas:** Identificar errores o comportamientos anómalos
            - **Notificaciones:** Base para sistema de notificaciones en tiempo real
            - **Vista rápida:** Entender el estado actual del sistema sin filtros complejos
            
            **Ejemplos de uso:**
            - `/recent` → Últimas 10 acciones (por defecto)
            - `/recent?limit=5` → Últimas 5 acciones
            - `/recent?limit=50` → Últimas 50 acciones
            - `/recent?limit=1` → Solo la acción más reciente
            
            **Ordenamiento:** Siempre de más reciente a más antigua (por actionAt DESC).
            
            **Rendimiento:** Este endpoint está optimizado para consultas rápidas.
            Para análisis históricos extensos, use `/api/v1/audit` con paginación.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de las acciones más recientes del sistema, limitada al número especificado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ActionDto[].class),
                examples = @ExampleObject(
                    name = "recentActionsResponse",
                    value = """
                    [
                        {
                            "id": 248,
                            "name": "PROJECT_CREATED",
                            "description": "Proyecto creado con id 22",
                            "actionAt": "2024-11-01T16:45:30",
                            "access": {
                                "id": 148,
                                "accessAt": "2024-11-01T16:40:15",
                                "user": {
                                    "id": 8,
                                    "name": "Ana Martínez",
                                    "email": "ana.martinez@example.com",
                                    "roles": ["LIDER_COMUNITARIO"]
                                },
                                "ipAddress": "192.168.1.120",
                                "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0)",
                                "location": "Bogotá, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 247,
                            "name": "CURATOR_ASSIGNED",
                            "description": "Curator asignado al proyecto 22",
                            "actionAt": "2024-11-01T16:45:31",
                            "access": {
                                "id": 147,
                                "accessAt": "2024-11-01T16:40:20",
                                "user": {
                                    "id": 3,
                                    "name": "Carlos Rodríguez",
                                    "email": "carlos.rodriguez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.105",
                                "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
                                "location": "Barranquilla, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 246,
                            "name": "PROJECT_UPDATED",
                            "description": "Proyecto actualizado con id 15",
                            "actionAt": "2024-11-01T15:20:18",
                            "access": {
                                "id": 146,
                                "accessAt": "2024-11-01T15:15:00",
                                "user": {
                                    "id": 5,
                                    "name": "Walter Jiménez",
                                    "email": "walter.jimenez@example.com",
                                    "roles": ["LIDER_COMUNITARIO"]
                                },
                                "ipAddress": "192.168.1.110",
                                "userAgent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64)",
                                "location": "Medellín, Colombia",
                                "success": true
                            }
                        },
                        {
                            "id": 245,
                            "name": "PROJECT_APPROVED",
                            "description": "Proyecto 15 aprobado (listo para publicar)",
                            "actionAt": "2024-11-01T14:30:25",
                            "access": {
                                "id": 145,
                                "accessAt": "2024-11-01T14:25:00",
                                "user": {
                                    "id": 2,
                                    "name": "Nicole Hernández",
                                    "email": "nicole.hernandez@example.com",
                                    "roles": ["CURATOR"]
                                },
                                "ipAddress": "192.168.1.100",
                                "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                "location": "Santa Marta, Colombia",
                                "success": true
                            }
                        }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetro limit inválido (debe ser un número positivo mayor a 0)",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 400,
                        "error": "Bad Request",
                        "message": "El parámetro 'limit' debe ser un número entero positivo mayor a 0"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado - Token JWT inválido, expirado o ausente",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN",
            content = @Content
        )
    })
    public ResponseEntity<List<ActionDto>> getRecentActions(
            @Parameter(
                description = """
                    Número máximo de acciones recientes a retornar.
                    
                    - **Valor por defecto:** 10 (si no se especifica)
                    - **Mínimo:** 1
                    - **Máximo recomendado:** 100
                    - **Valores comunes:**
                      - 5: Vista muy resumida para widgets pequeños
                      - 10: Ideal para dashboards estándar
                      - 20: Vista extendida de actividad reciente
                      - 50: Análisis más profundo de actividad reciente
                    
                    Las acciones siempre se retornan ordenadas de más reciente a más antigua.
                    """,
                example = "10"
            )
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(auditService.findRecentActions(limit));
    }
}