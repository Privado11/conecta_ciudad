package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.Dto.project.ReviewNotesDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.services.project.ProjectService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "API para gestión completa del ciclo de vida de proyectos comunitarios")
public class ProjectController {
    
    private final ProjectService projectService;


    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @PostMapping
    @Operation(
        summary = "Crear nuevo proyecto comunitario",
        description = """
            Permite a un líder comunitario crear un nuevo proyecto. El proyecto se crea en estado PENDIENTE 
            y queda asociado automáticamente al usuario autenticado como creador.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol LIDER_COMUNITARIO pueden crear proyectos
            - El creador se asigna automáticamente desde el token de autenticación JWT
            
            **Flujo completo del proyecto:**
            1. Creación → PENDIENTE (Pendiente de revisión)
            2. Asignación de curador → EN_REVISION (En revisión)
               - Si hay curadores disponibles, se asignará uno automáticamente
               - Si no hay curadores disponibles, el proyecto permanecerá en estado PENDIENTE hasta que se asigne un curador manualmente
            3. Si requiere cambios → OBSERVACIONES (Devuelto con observaciones)
            4. Preparación → LISTO_PARA_PUBLICAR (Listo para publicar)
            5. Publicación → PUBLICADO (Publicado)
            
            También puede ser RECHAZADO en cualquier punto del proceso.
            
            **Nota importante sobre curadores:**
            - Al crear un proyecto, el sistema intentará asignar automáticamente un curador disponible.
            - La asignación automática se basa en la carga de trabajo actual de los curadores.
            - Si no hay curadores disponibles, el administrador deberá asignar uno manualmente para que el proceso de revisión pueda continuar.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del proyecto a crear. Todos los campos son requeridos.",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectSaveDto.class),
                examples = @ExampleObject(
                    name = "projectRequest",
                    value = """
                    {
                        "name": "Parque Comunal La Esperanza",
                        "objectives": "Construir un parque para la comunidad con áreas verdes y juegos infantiles",
                        "beneficiaryPopulations": "Niños, jóvenes y adultos mayores del barrio La Esperanza",
                        "budgets": "Presupuesto aprobado por la alcaldía: $150,000,000 COP",
                        "startAt": "2024-01-15T08:00:00",
                        "endAt": "2024-06-30T17:00:00"
                    }
                    """
                )
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Proyecto creado exitosamente con estado PENDIENTE",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto.class),
                examples = @ExampleObject(
                    name = "projectResponse",
                    value = """
                    {
                        "id": 1,
                        "name": "Parque Comunal La Esperanza",
                        "objectives": "Construir un parque para la comunidad con áreas verdes y juegos infantiles",
                        "beneficiaryPopulations": "Niños, jóvenes y adultos mayores del barrio La Esperanza",
                        "budgets": "Presupuesto aprobado por la alcaldía: $150,000,000 COP",
                        "startAt": "2024-01-15T08:00:00",
                        "endAt": "2024-06-30T17:00:00",
                        "status": "PENDIENTE",
                        "creator": {
                            "id": 5,
                            "name": "María González"
                        },
                        "curator": null,
                        "reviewNotes": null,
                        "reviewDueAt": null,
                        "reviewedAt": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos (campos requeridos vacíos, formato incorrecto de fechas, etc.)",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 400,
                        "error": "Bad Request",
                        "message": "El campo 'name' no puede estar vacío"
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
            description = "No autorizado - El usuario no tiene rol LIDER_COMUNITARIO",
            content = @Content
        )
    })
    public ResponseEntity<ProjectDto> createProject(
            @Valid @RequestBody ProjectSaveDto projectSaveDto, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(projectService.saveProject(projectSaveDto, creatorId, accessId));
    }


    @GetMapping("/{id}")
    @Operation(
        summary = "Consultar proyecto por su identificador único",
        description = """
            Obtiene los detalles completos de un proyecto específico mediante su ID numérico.
            
            **Información retornada:**
            - Datos básicos del proyecto (nombre, objetivos, presupuesto, población beneficiaria)
            - Estado actual del proyecto (PENDIENTE, EN_REVISION, OBSERVACIONES, APROBADO, etc.)
            - Información del creador (ID y nombre del líder comunitario)
            - Información del curador asignado (ID y nombre, si ya fue asignado)
            - Notas de revisión del curador (si las hay)
            - Fechas relevantes (creación, inicio, fin, revisión, aprobación)
            
            **Acceso:** Endpoint público (cualquier usuario autenticado puede consultar)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Proyecto encontrado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto.class),
                examples = @ExampleObject(
                    name = "projectResponse",
                    value = """
                    {
                        "id": 1,
                        "name": "Parque Comunal La Esperanza",
                        "objectives": "Construir un parque para la comunidad con áreas verdes y juegos infantiles",
                        "beneficiaryPopulations": "Niños, jóvenes y adultos mayores del barrio La Esperanza",
                        "budgets": "Presupuesto aprobado por la alcaldía: $150,000,000 COP",
                        "startAt": "2024-01-15T08:00:00",
                        "endAt": "2024-06-30T17:00:00",
                        "status": "EN_REVISION",
                        "creator": {
                            "id": 5,
                            "name": "Walter Jiménez"
                        },
                        "curator": {
                            "id": 2,
                            "name": "Nicole Hernández"
                        },
                        "reviewNotes": "Se requiere ajustar el presupuesto para incluir más áreas verdes",
                        "reviewDueAt": "2024-01-30T23:59:59",
                        "reviewedAt": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No existe un proyecto con el ID especificado en la base de datos",
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                        "status": 404,
                        "error": "Not Found",
                        "message": "No se encontró el proyecto con ID: 999"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<ProjectDto> getById(
            @Parameter(
                description = "Identificador único del proyecto en la base de datos (número entero positivo)",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

@PreAuthorize("hasAuthority('PROJECT_VIEW_ALL')")
@GetMapping
@Operation(
    summary = "Listar todos los proyectos del sistema (Solo ADMIN)",
    description = """
        Permite a los administradores ver todos los proyectos del sistema sin restricciones.
        
        **Restricciones de seguridad:**
        - Solo usuarios con rol ADMIN pueden acceder a este endpoint
        - Retorna absolutamente todos los proyectos registrados en el sistema
        - No aplica ningún filtro por usuario o estado
        
        **Información retornada:**
        - Todos los proyectos de todos los líderes comunitarios
        - Todos los estados: PENDIENTE, EN_REVISION, OBSERVACIONES, APROBADO, LISTO_PARA_PUBLICAR, PUBLICADO, RECHAZADO
        - Información completa: creadores, curadores, observaciones y fechas de revisión
        
        **Uso típico:**
        - Panel de control administrativo
        - Monitoreo general del sistema
        - Generación de reportes y estadísticas
        - Auditorías y seguimiento global
        - Gestión y supervisión de todos los proyectos
        
        **Ordenamiento:**
        Los proyectos se retornan en el orden en que fueron creados (por defecto)
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista completa de todos los proyectos del sistema (puede estar vacía si no hay proyectos registrados)",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ProjectDto[].class),
            examples = @ExampleObject(
                name = "allProjectsList",
                value = """
                [
                    {
                        "id": 1,
                        "name": "Parque Comunal La Esperanza",
                        "objectives": "Construir un parque para la comunidad con áreas verdes y juegos infantiles",
                        "beneficiaryPopulations": "Niños, jóvenes y adultos mayores del barrio La Esperanza",
                        "budgets": "Presupuesto aprobado por la alcaldía: $150,000,000 COP",
                        "startAt": "2024-01-15T08:00:00",
                        "endAt": "2024-06-30T17:00:00",
                        "status": "EN_REVISION",
                        "creator": {
                            "id": 5,
                            "name": "Walter Jiménez",
                            "email": "walter.jimenez@example.com"
                        },
                        "curator": {
                            "id": 2,
                            "name": "Nicole Hernández",
                            "email": "nicole.hernandez@example.com"
                        },
                        "reviewNotes": "Se requiere ajustar el presupuesto para incluir más áreas verdes",
                        "reviewDueAt": "2024-01-30T23:59:59",
                        "reviewedAt": null
                    },
                    {
                        "id": 2,
                        "name": "Taller de reciclaje comunitario",
                        "objectives": "Capacitar a la comunidad en prácticas de reciclaje",
                        "beneficiaryPopulations": "Adultos y jóvenes del barrio",
                        "budgets": "$80,000,000 COP",
                        "startAt": "2024-02-01T09:00:00",
                        "endAt": "2024-11-30T18:00:00",
                        "status": "LISTO_PARA_PUBLICAR",
                        "creator": {
                            "id": 5,
                            "name": "Walter Jiménez",
                            "email": "walter.jimenez@example.com"
                        },
                        "curator": {
                            "id": 3,
                            "name": "Carlos Rodríguez",
                            "email": "carlos.rodriguez@example.com"
                        },
                        "reviewNotes": "Proyecto aprobado sin observaciones",
                        "reviewDueAt": "2024-01-30T23:59:59",
                        "reviewedAt": "2024-01-28T10:15:00"
                    },
                    {
                        "id": 3,
                        "name": "Centro de capacitación digital",
                        "objectives": "Establecer un centro comunitario para formación en competencias digitales",
                        "beneficiaryPopulations": "Jóvenes y adultos sin acceso a formación tecnológica",
                        "budgets": "Presupuesto mixto: $200,000,000 COP",
                        "startAt": "2024-03-01T08:00:00",
                        "endAt": "2024-12-31T18:00:00",
                        "status": "PENDIENTE",
                        "creator": {
                            "id": 8,
                            "name": "Ana Martínez",
                            "email": "ana.martinez@example.com"
                        },
                        "curator": null,
                        "reviewNotes": null,
                        "reviewDueAt": null,
                        "reviewedAt": null
                    },
                    {
                        "id": 4,
                        "name": "Huerta comunitaria El Progreso",
                        "objectives": "Crear una huerta urbana para producción de alimentos orgánicos",
                        "beneficiaryPopulations": "Familias de bajos recursos del sector",
                        "budgets": "$50,000,000 COP",
                        "startAt": "2024-04-01T07:00:00",
                        "endAt": "2024-10-31T18:00:00",
                        "status": "OBSERVACIONES",
                        "creator": {
                            "id": 10,
                            "name": "Luis Pérez",
                            "email": "luis.perez@example.com"
                        },
                        "curator": {
                            "id": 2,
                            "name": "Nicole Hernández",
                            "email": "nicole.hernandez@example.com"
                        },
                        "reviewNotes": "Falta detallar el plan de sostenibilidad a largo plazo y especificar las especies a cultivar",
                        "reviewDueAt": "2024-03-20T23:59:59",
                        "reviewedAt": "2024-03-15T14:30:00"
                    },
                    {
                        "id": 5,
                        "name": "Biblioteca móvil comunitaria",
                        "objectives": "Implementar servicio de biblioteca móvil para zonas rurales",
                        "beneficiaryPopulations": "Niños y jóvenes de veredas alejadas",
                        "budgets": "$120,000,000 COP",
                        "startAt": "2024-05-01T08:00:00",
                        "endAt": "2024-12-31T17:00:00",
                        "status": "PUBLICADO",
                        "creator": {
                            "id": 12,
                            "name": "Carmen Ruiz",
                            "email": "carmen.ruiz@example.com"
                        },
                        "curator": {
                            "id": 3,
                            "name": "Carlos Rodríguez",
                            "email": "carlos.rodriguez@example.com"
                        },
                        "reviewNotes": "Excelente iniciativa, aprobado sin observaciones",
                        "reviewDueAt": "2024-04-15T23:59:59",
                        "reviewedAt": "2024-04-10T11:20:00"
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
        description = "No autorizado - El usuario autenticado no tiene rol ADMIN",
        content = @Content(
            examples = @ExampleObject(
                value = """
                {
                    "status": 403,
                    "error": "Forbidden",
                    "message": "Acceso denegado. Se requiere rol ADMIN para acceder a este recurso"
                }
                """
            )
        )
    )
})
public ResponseEntity<List<ProjectDto>> getAllProjects() {
    return ResponseEntity.ok(projectService.findAll());
}

@PreAuthorize("hasAuthority('PROJECT_VIEW')")
@GetMapping("/my-projects")
@Operation(
    summary = "Listar mis proyectos creados (Solo LIDER_COMUNITARIO)",
    description = """
        Permite al líder comunitario autenticado ver únicamente los proyectos que él ha creado.
        Este endpoint muestra el portafolio personal de proyectos del líder.
        
        **Restricciones de seguridad:**
        - Solo usuarios con rol LIDER_COMUNITARIO pueden acceder
        - Solo retorna proyectos donde el creador (creator_id) es el usuario autenticado
        - No puede ver proyectos de otros líderes comunitarios
        - El filtrado se hace automáticamente usando el JWT token
        
        **Información retornada:**
        - Todos los proyectos creados por el líder autenticado
        - Todos los estados de sus proyectos (PENDIENTE, EN_REVISION, OBSERVACIONES, APROBADO, PUBLICADO, RECHAZADO)
        - Información del curador asignado (si ya tiene uno)
        - Observaciones y notas de revisión del curador
        - Fechas importantes: creación, inicio, fin, revisión, aprobación
        
        **Casos de uso:**
        - Ver el progreso de todos mis proyectos
        - Revisar qué proyectos tienen observaciones pendientes
        - Verificar estados actuales de mis iniciativas
        - Consultar mi historial completo de proyectos
        - Identificar proyectos que necesitan actualización
        
        **Ordenamiento:**
        Los proyectos se retornan en el orden en que fueron creados (más antiguos primero)
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de proyectos creados por el líder comunitario autenticado (array vacío si no ha creado proyectos aún)",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ProjectDto[].class),
            examples = @ExampleObject(
                name = "myProjectsList",
                value = """
                [
                    {
                        "id": 1,
                        "name": "Parque Comunal La Esperanza",
                        "objectives": "Construir un parque para la comunidad con áreas verdes y juegos infantiles",
                        "beneficiaryPopulations": "Niños, jóvenes y adultos mayores del barrio La Esperanza",
                        "budgets": "Presupuesto aprobado por la alcaldía: $150,000,000 COP",
                        "startAt": "2024-01-15T08:00:00",
                        "endAt": "2024-06-30T17:00:00",
                        "status": "EN_REVISION",
                        "creator": {
                            "id": 5,
                            "name": "Walter Jiménez",
                            "email": "walter.jimenez@example.com"
                        },
                        "curator": {
                            "id": 2,
                            "name": "Nicole Hernández",
                            "email": "nicole.hernandez@example.com"
                        },
                        "reviewNotes": "Se requiere ajustar el presupuesto para incluir más áreas verdes y especificar cronograma de construcción",
                        "reviewDueAt": "2024-01-30T23:59:59",
                        "reviewedAt": null
                    },
                    {
                        "id": 2,
                        "name": "Taller de reciclaje comunitario",
                        "objectives": "Capacitar a la comunidad en prácticas de reciclaje y manejo de residuos sólidos",
                        "beneficiaryPopulations": "Adultos y jóvenes del barrio San José",
                        "budgets": "Presupuesto de $80,000,000 COP financiado por ONG ambiental",
                        "startAt": "2024-02-01T09:00:00",
                        "endAt": "2024-11-30T18:00:00",
                        "status": "LISTO_PARA_PUBLICAR",
                        "creator": {
                            "id": 5,
                            "name": "Walter Jiménez",
                            "email": "walter.jimenez@example.com"
                        },
                        "curator": {
                            "id": 3,
                            "name": "Carlos Rodríguez",
                            "email": "carlos.rodriguez@example.com"
                        },
                        "reviewNotes": "Proyecto aprobado sin observaciones. Excelente planificación y presupuesto bien detallado",
                        "reviewDueAt": "2024-01-30T23:59:59",
                        "reviewedAt": "2024-01-28T10:15:00"
                    },
                    {
                        "id": 7,
                        "name": "Jornadas de salud preventiva",
                        "objectives": "Realizar jornadas mensuales de consulta médica y medicina preventiva",
                        "beneficiaryPopulations": "Población vulnerable sin acceso a servicios de salud",
                        "budgets": "$100,000,000 COP - Convenio con Secretaría de Salud",
                        "startAt": "2024-03-15T07:00:00",
                        "endAt": "2024-12-15T16:00:00",
                        "status": "PENDIENTE",
                        "creator": {
                            "id": 5,
                            "name": "Walter Jiménez",
                            "email": "walter.jimenez@example.com"
                        },
                        "curator": null,
                        "reviewNotes": null,
                        "reviewDueAt": null,
                        "reviewedAt": null
                    },
                    {
                        "id": 12,
                        "name": "Escuela de fútbol infantil",
                        "objectives": "Crear espacios deportivos y formativos para niños de 6 a 14 años",
                        "beneficiaryPopulations": "Niños y adolescentes del barrio",
                        "budgets": "$60,000,000 COP - Patrocinio empresarial",
                        "startAt": "2024-04-01T14:00:00",
                        "endAt": "2024-12-20T18:00:00",
                        "status": "OBSERVACIONES",
                        "creator": {
                            "id": 5,
                            "name": "Walter Jiménez",
                            "email": "walter.jimenez@example.com"
                        },
                        "curator": {
                            "id": 2,
                            "name": "Nicole Hernández",
                            "email": "nicole.hernandez@example.com"
                        },
                        "reviewNotes": "Falta incluir plan de seguridad deportiva, certificación de entrenadores y seguro de accidentes para los menores",
                        "reviewDueAt": "2024-03-25T23:59:59",
                        "reviewedAt": "2024-03-20T15:45:00"
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
                    "message": "Token JWT inválido o expirado. Por favor inicie sesión nuevamente"
                }
                """
            )
        )
    ),
    @ApiResponse(
        responseCode = "403",
        description = "No autorizado - El usuario no tiene rol LIDER_COMUNITARIO",
        content = @Content(
            examples = @ExampleObject(
                value = """
                {
                    "status": 403,
                    "error": "Forbidden",
                    "message": "Acceso denegado. Se requiere rol LIDER_COMUNITARIO para acceder a este recurso"
                }
                """
            )
        )
    )
})
public ResponseEntity<List<ProjectDto>> getMyProjects() {
   
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Long creatorId = (Long) auth.getDetails();
    
    return ResponseEntity.ok(projectService.findByCreatorId(creatorId));
}

@PreAuthorize("hasAuthority('PROJECT_SEARCH')")
@GetMapping("/search")
@Operation(
    summary = "Buscar proyectos por nombre en todo el sistema (Solo ADMIN)",
    description = """
        Permite a los administradores buscar proyectos por nombre utilizando coincidencia parcial.
        La búsqueda es case-insensitive (no distingue mayúsculas/minúsculas) y busca en todo el sistema.
        
        **Restricciones de seguridad:**
        - Solo usuarios con rol ADMIN pueden acceder a este endpoint
        - Busca en todos los proyectos del sistema sin importar el creador
        - No aplica ningún filtro adicional por estado o curador
        
        **Funcionalidad de búsqueda:**
        - **Búsqueda parcial:** No necesita coincidir exactamente, busca el término dentro del nombre
        - **Case-insensitive:** "PARQUE", "parque", "Parque" dan los mismos resultados
        - **Coincidencias múltiples:** Retorna todos los proyectos que contengan el término buscado
        
        **Ejemplos de búsqueda:**
        - `name=parque` → encuentra: "Parque Comunal", "Remodelación de parques", "Parque infantil"
        - `name=salud` → encuentra: "Centro de salud", "Jornadas de salud", "Salud preventiva"
        - `name=eco` → encuentra: "Proyecto ecológico", "Eco-turismo", "Economía circular"
        - `name=2024` → encuentra todos los proyectos que tengan "2024" en el nombre
        
        **Casos de uso:**
        - Buscar proyectos relacionados con un tema específico
        - Encontrar proyectos por palabras clave
        - Identificar iniciativas similares en el sistema
        - Generar reportes temáticos
        - Auditorías y análisis de proyectos por categoría
        
        **Nota:** Si el término de búsqueda está vacío o no se encuentra ninguna coincidencia, 
        retorna un array vacío (no un error 404).
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Lista de proyectos que coinciden con el criterio de búsqueda (array vacío si no hay coincidencias)",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ProjectDto[].class),
            examples = @ExampleObject(
                name = "searchResults",
                value = """
                [
                    {
                        "id": 1,
                        "name": "Parque Comunal La Esperanza",
                        "objectives": "Construir un parque para la comunidad con áreas verdes y juegos infantiles",
                        "beneficiaryPopulations": "Niños, jóvenes y adultos mayores del barrio La Esperanza",
                        "budgets": "Presupuesto aprobado por la alcaldía: $150,000,000 COP",
                        "startAt": "2024-01-15T08:00:00",
                        "endAt": "2024-06-30T17:00:00",
                        "status": "EN_REVISION",
                        "creator": {
                            "id": 5,
                            "name": "Walter Jiménez",
                            "email": "walter.jimenez@example.com"
                        },
                        "curator": {
                            "id": 2,
                            "name": "Nicole Hernández",
                            "email": "nicole.hernandez@example.com"
                        },
                        "reviewNotes": "Se requiere ajustar el presupuesto",
                        "reviewDueAt": "2024-01-30T23:59:59",
                        "reviewedAt": null
                    },
                    {
                        "id": 8,
                        "name": "Remodelación de parques infantiles",
                        "objectives": "Renovar infraestructura de juegos infantiles en 5 parques del municipio",
                        "beneficiaryPopulations": "Niños de 3 a 12 años de diferentes sectores",
                        "budgets": "$180,000,000 COP - Presupuesto municipal",
                        "startAt": "2024-05-01T08:00:00",
                        "endAt": "2024-10-31T17:00:00",
                        "status": "APROBADO",
                        "creator": {
                            "id": 12,
                            "name": "Carmen Ruiz",
                            "email": "carmen.ruiz@example.com"
                        },
                        "curator": {
                            "id": 3,
                            "name": "Carlos Rodríguez",
                            "email": "carlos.rodriguez@example.com"
                        },
                        "reviewNotes": "Proyecto bien estructurado, aprobado para ejecución",
                        "reviewDueAt": "2024-04-20T23:59:59",
                        "reviewedAt": "2024-04-15T09:30:00"
                    },
                    {
                        "id": 15,
                        "name": "Parque Ecológico del Río",
                        "objectives": "Crear un parque ecológico con senderos naturales y zona de conservación",
                        "beneficiaryPopulations": "Comunidad en general, turistas y estudiantes",
                        "budgets": "$250,000,000 COP - Cofinanciación público-privada",
                        "startAt": "2024-07-01T07:00:00",
                        "endAt": "2025-01-31T18:00:00",
                        "status": "PENDIENTE",
                        "creator": {
                            "id": 18,
                            "name": "Jorge Mendoza",
                            "email": "jorge.mendoza@example.com"
                        },
                        "curator": null,
                        "reviewNotes": null,
                        "reviewDueAt": null,
                        "reviewedAt": null
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
                    "message": "Acceso denegado. Se requiere rol ADMIN para buscar proyectos en todo el sistema"
                }
                """
            )
        )
    )
})
public ResponseEntity<List<ProjectDto>> searchProjectsByName(
        @Parameter(
            description = """
                Término de búsqueda para filtrar proyectos por nombre.
                La búsqueda es parcial y case-insensitive (no distingue mayúsculas/minúsculas).
                
                Ejemplos válidos:
                - "parque" → Busca cualquier proyecto que contenga "parque" en su nombre
                - "salud" → Busca proyectos relacionados con salud
                - "2024" → Busca proyectos con "2024" en el nombre
                
                Mínimo: 1 carácter
                Máximo: 200 caracteres
                """,
            example = "parque",
            required = true
        )
        @RequestParam String name) {
    
   
    return ResponseEntity.ok(projectService.findByNameContainingIgnoreCase(name));
}

    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar datos de un proyecto existente",
        description = """
            Permite modificar los datos básicos de un proyecto (nombre, objetivos, presupuesto, fechas).
            
            **Restricciones importantes:**
            - Solo el creador del proyecto puede actualizarlo
            - No se puede actualizar el estado directamente (usar endpoints específicos: /approve, /observations, etc.)
            - No se puede cambiar el creador o curador asignado (usar /curator para cambiar curador)
            - El proyecto debe existir en la base de datos
            
            **Campos actualizables:**
            - name: Nombre del proyecto
            - objectives: Objetivos del proyecto
            - beneficiaryPopulations: Descripción de la población beneficiaria
            - budgets: Información del presupuesto
            - startAt: Fecha y hora de inicio (formato ISO 8601)
            - endAt: Fecha y hora de finalización (formato ISO 8601)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Proyecto actualizado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "No autorizado - Solo el creador del proyecto puede actualizarlo"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Proyecto no encontrado con el ID especificado"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos (fechas incorrectas, campos vacíos, etc.)"
        )
    })
    public ResponseEntity<?> updateProject(
            @Parameter(
                description = "ID único del proyecto a actualizar (debe existir en la base de datos)",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            @Valid @RequestBody ProjectSaveDto projectDto, HttpServletRequest request) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long creatorId = (Long) auth.getDetails();
            Long accessId = (Long) request.getAttribute("currentAccessId");
        return ResponseEntity.ok(projectService.updateProject(id, projectDto, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_ASSIGN_CURATOR')")
    @PutMapping("/{id}/curator")
    @Operation(
        summary = "Asignar o reasignar curador a un proyecto",
        description = """
            Permite a un administrador asignar o cambiar el curador responsable de revisar un proyecto.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol ADMIN pueden asignar/reasignar curadores
            - El usuario asignado (curatorId) debe existir en la base de datos
            - El usuario asignado debe tener rol CURATOR activo
            
            **Casos de uso:**
            - **Asignación inicial:** Cuando un proyecto pasa de PENDIENTE a EN_REVISION
            - **Reasignación por carga:** Balanceo de carga de trabajo entre curadores
            - **Reasignación por ausencia:** Cuando el curador original no está disponible
            - **Cambio por conflicto de interés:** Cuando el curador tiene algún vínculo con el proyecto
            
            **Efecto automático:** 
            - Si el proyecto está en estado PENDIENTE, cambia automáticamente a EN_REVISION
            - Se registra la fecha de asignación
            - Se notifica al nuevo curador (si el sistema tiene notificaciones)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Curador asignado/reasignado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "No autorizado - Se requiere rol ADMIN para esta operación"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Proyecto no encontrado O usuario curador no encontrado en la base de datos"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "El usuario especificado no tiene rol CURATOR activo"
        )
    })
    public ResponseEntity<?> reassignCurator(
            @Parameter(
                description = "ID único del proyecto al que se asignará/reasignará el curador",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            
            @Parameter(
                description = "ID único del usuario curador que se asignará al proyecto. Debe ser un usuario existente con rol CURATOR.",
                example = "2",
                required = true
            )
            @RequestParam Long curatorId, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");
        return ResponseEntity.ok(projectService.reassignCurator(id, curatorId, adminId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_ADD_OBSERVATIONS')")
    @PutMapping("/{id}/observations")
    @Operation(
        summary = "Agregar observaciones de revisión al proyecto",
        description = """
            Permite al curador asignado añadir notas, comentarios y observaciones durante el proceso de evaluación del proyecto.
            
            **Restricciones de seguridad:**
            - Solo el curador asignado específicamente a este proyecto puede agregar observaciones
            - Requiere rol CURATOR activo
            - El proyecto debe estar en estado EN_REVISION
            
            **Casos de uso típicos:**
            - Solicitar correcciones o aclaraciones al líder comunitario
            - Documentar hallazgos durante la revisión técnica
            - Registrar recomendaciones antes de aprobar el proyecto
            - Explicar por qué se devuelve el proyecto con estado OBSERVACIONES
            - Dejar registro de puntos débiles que deben mejorarse
            
            **Efecto en el flujo:**
            - Las observaciones son visibles inmediatamente para el creador del proyecto
            - Generalmente cambia el estado del proyecto a OBSERVACIONES
            - El líder comunitario puede actualizar el proyecto basándose en estas notas
            - Se mantiene un historial de todas las observaciones agregadas
            
            **Nota:** Las observaciones quedan registradas permanentemente en el historial del proyecto.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Observaciones agregadas exitosamente al proyecto",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "No autorizado - Solo el curador asignado a este proyecto puede agregar observaciones"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Proyecto no encontrado con el ID especificado"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Las notas no pueden estar vacías O el proyecto no está en un estado que permita agregar observaciones"
        )
    })
    public ResponseEntity<?> addObservations(
            @Parameter(
                description = "ID único del proyecto al que se agregarán las observaciones de revisión",
                example = "1",
                required = true
            )
            @PathVariable Long id,
            @Valid @RequestBody ReviewNotesDto body, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");
        return ResponseEntity.ok(projectService.addObservations(id, curatorId, body.notes(), accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_APPROVE')")
    @PutMapping("/{id}/approve")
    @Operation(
        summary = "Aprobar proyecto tras completar la revisión",
        description = """
            Permite al curador aprobar un proyecto después de haber completado exitosamente su proceso de revisión.
            
            **Restricciones de seguridad:**
            - Solo el curador asignado específicamente a este proyecto puede aprobarlo
            - Requiere rol CURATOR activo
            - El proyecto debe estar en estado EN_REVISION
            - No puede aprobar proyectos en estado PENDIENTE, OBSERVACIONES, RECHAZADO, etc.
            
            **Flujo de aprobación:**
            - Estado actual: EN_REVISION (En revisión)
            - Estado resultante: APROBADO (Aprobado) o LISTO_PARA_PUBLICAR (según lógica de negocio)
            - Se registra automáticamente la fecha y hora de aprobación
            - Se asocia el curador que aprobó el proyecto
            
            **Efecto posterior:**
            - El proyecto aprobado queda disponible para que un ADMIN lo publique
            - El líder comunitario recibe notificación de aprobación (si hay sistema de notificaciones)
            - El proyecto avanza al siguiente paso del flujo: publicación
            
            **Nota:** Una vez aprobado, solo un ADMIN puede cambiar el estado del proyecto (publicar o rechazar por razones administrativas).
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Proyecto aprobado exitosamente. Estado cambiado a APROBADO o LISTO_PARA_PUBLICAR.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "No autorizado - Solo el curador asignado a este proyecto puede aprobarlo"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Proyecto no encontrado con el ID especificado"
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "El proyecto no está en estado EN_REVISION, por lo que no puede ser aprobado en este momento"
        )
    })
    public ResponseEntity<?> approveProject(
            @Parameter(
                description = "ID único del proyecto a aprobar (debe estar en estado EN_REVISION)",
                example = "1",
                required = true
            )
            @PathVariable Long id, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");
        return ResponseEntity.ok(projectService.approveProject(id, curatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
    @GetMapping("/my")
    @Operation(
        summary = "Listar proyectos asignados al curador autenticado",
        description = """
            Retorna todos los proyectos que han sido asignados específicamente al curador que realiza la petición.
            Opcionalmente permite filtrar por estado del proyecto para facilitar la gestión de la carga de trabajo.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol CURATOR pueden acceder a este endpoint
            - Solo retorna proyectos donde el curador autenticado es el curator_id asignado
            - No muestra proyectos de otros curadores
            
            **Filtros disponibles:**
            - **Sin filtro (status=null):** Retorna todos los proyectos asignados al curador, sin importar su estado
            - **Con status:** Filtra solo proyectos en el estado específico solicitado
            
            **Estados posibles para filtrar:**
            - PENDIENTE: Proyectos recién creados esperando revisión
            - EN_REVISION: Proyectos actualmente en proceso de revisión
            - OBSERVACIONES: Proyectos devueltos que requieren correcciones
            - APROBADO: Proyectos que el curador ya aprobó
            - RECHAZADO: Proyectos que el curador rechazó
            - LISTO_PARA_PUBLICAR: Proyectos listos para publicación
            - PUBLICADO: Proyectos ya publicados
            
            **Casos de uso:**
            - Ver todos los proyectos pendientes de revisión: `?status=EN_REVISION`
            - Listar proyectos con observaciones pendientes: `?status=OBSERVACIONES`
            - Ver historial completo de proyectos asignados: sin parámetro status
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de proyectos asignados al curador (puede ser vacía si no tiene proyectos asignados)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto[].class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol CURATOR para acceder a este endpoint"
        )
    })
    public ResponseEntity<?> listMyCuratedProjects(
            @Parameter(
                description = """
                    Estado del proyecto para filtrar. Valores posibles:
                    PENDIENTE, EN_REVISION, OBSERVACIONES, APROBADO, RECHAZADO, LISTO_PARA_PUBLICAR, PUBLICADO.
                    Si no se especifica, retorna proyectos en todos los estados.
                    """,
                example = "EN_REVISION"
            )
            @RequestParam(required = false) ProjectStatus status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        return ResponseEntity.ok(projectService.findByCurator(curatorId, status));
    }

    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar proyecto del sistema",
        description = """
            Elimina permanentemente un proyecto de la base de datos.
            
            **Restricciones:**
            - Solo el creador del proyecto puede eliminarlo (dependiendo de la lógica de negocio)
            - Alternativamente, solo usuarios ADMIN pueden eliminar proyectos (configurar según requerimientos)
            - No se puede eliminar proyectos en estado PUBLICADO (según reglas de negocio)
            
            **Advertencia:** Esta es una operación destructiva e irreversible. 
            Considerar implementar "soft delete" (marcar como eliminado sin borrar físicamente) para mantener historial.
            
            **Efecto:**
            - Elimina el registro del proyecto de la tabla projects
            - Puede eliminar relaciones asociadas (dependiendo de configuración de CASCADE)
            - Se pierde toda la información y el historial del proyecto
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Proyecto eliminado exitosamente",
            content = @Content(
                examples = @ExampleObject(value = "\"Project deleted successfully\"")
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - No tiene permisos para eliminar este proyecto"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Proyecto no encontrado con el ID especificado"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "No se puede eliminar el proyecto en su estado actual (ej: ya publicado)"
        )
    })
    public ResponseEntity<?> deleteProject(
            @Parameter(
                description = "ID único del proyecto a eliminar (operación irreversible)",
                example = "1",
                required = true
            )
            @PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_READY_TO_PUBLISH')")
    @GetMapping("/ready-to-publish")
    @Operation(
        summary = "Listar proyectos listos para publicar",
        description = """
            Lista todos los proyectos que se encuentran en estado LISTO_PARA_PUBLICAR.
            Estos proyectos ya fueron aprobados por un curador y están esperando la publicación final por parte de un administrador.
            
            **Restricciones de seguridad:**
            - Este es el paso previo a la publicación oficial del proyecto
            
            **Flujo del proyecto hasta este punto:**
            1. PENDIENTE → Proyecto creado por líder comunitario
            2. EN_REVISION → Curador asignado, revisando el proyecto
            3. APROBADO → Curador aprueba el proyecto
            4. LISTO_PARA_PUBLICAR → Estado actual, esperando publicación administrativa
            5. PUBLICADO → Siguiente paso (requiere acción del ADMIN)
            
            **Propósito:**
            Este endpoint permite ver qué proyectos están esperando ser publicados,
            facilitando la gestión y priorización de publicaciones pendientes.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de proyectos con estado LISTO_PARA_PUBLICAR (puede ser vacía si no hay proyectos pendientes de publicación)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto[].class),
                examples = @ExampleObject(
                    name = "readyToPublishList",
                    value = """
                    [
                        {
                            "id": 3,
                            "name": "Parque Comunal La Esperanza",
                            "objectives": "Construir un parque para la comunidad con áreas verdes y juegos infantiles",
                            "beneficiaryPopulations": "Niños, jóvenes y adultos mayores del barrio La Esperanza",
                            "budgets": "Presupuesto aprobado por la alcaldía: $150,000,000 COP",
                            "startAt": "2024-01-15T08:00:00",
                            "endAt": "2024-06-30T17:00:00",
                            "status": "LISTO_PARA_PUBLICAR",
                            "creator": {
                                "id": 5,
                                "name": "Walter Jiménez"
                            },
                            "curator": {
                                "id": 2,
                                "name": "Nicole Hernández"
                            },
                            "reviewNotes": "Proyecto aprobado, cumple con todos los requisitos",
                            "reviewDueAt": "2024-01-30T23:59:59",
                            "reviewedAt": "2024-01-25T14:30:00"
                        },
                        {
                            "id": 7,
                            "name": "Centro de capacitación digital",
                            "objectives": "Establecer un centro comunitario para formación en competencias digitales",
                            "beneficiaryPopulations": "Jóvenes y adultos sin acceso a formación tecnológica",
                            "budgets": "Presupuesto mixto: $200,000,000 COP",
                            "startAt": "2024-03-01T08:00:00",
                            "endAt": "2024-12-31T18:00:00",
                            "status": "LISTO_PARA_PUBLICAR",
                            "creator": {
                                "id": 8,
                                "name": "Ana Martínez"
                            },
                            "curator": {
                                "id": 2,
                                "name": "Nicole Hernández"
                            },
                            "reviewNotes": "Excelente propuesta, aprobado para publicación inmediata",
                            "reviewDueAt": "2024-02-15T23:59:59",
                            "reviewedAt": "2024-02-10T16:45:00"
                        }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No autorizado - Se requiere rol ADMIN para acceder a este endpoint",
            content = @Content
        )
    })
    public ResponseEntity<List<ProjectDto>> getReadyToPublishProjects() {
        return ResponseEntity.ok(projectService.findReadyToPublish());
    }
}