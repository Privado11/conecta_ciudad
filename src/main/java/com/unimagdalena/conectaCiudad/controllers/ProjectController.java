package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectApprovalDto;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
    private static final int MAX_PAGE_SIZE = 100;

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
            3. Si requiere cambios → OBSERVACIONES (Devuelto con observaciones)
            4. Preparación → LISTO_PARA_PUBLICAR (Listo para publicar)
            5. Publicación → PUBLICADO (Publicado)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Proyecto creado exitosamente con estado PENDIENTE",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<ProjectDto> createProject(
            @Valid @RequestBody ProjectSaveDto projectSaveDto, 
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(projectService.saveProject(projectSaveDto, creatorId, accessId));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Consultar proyecto por ID",
        description = "Obtiene los detalles completos de un proyecto específico mediante su ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proyecto encontrado"),
        @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<ProjectDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW') or hasAuthority('PROJECT_SEARCH')")
    @GetMapping("/search")
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
    
    Page<ProjectDto> response = projectService.findWithFilters(
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

    @PreAuthorize("hasAuthority('PROJECT_VIEW') or hasAuthority('PROJECT_SEARCH')")
    @GetMapping("/statistics")
    @Operation(
    summary = "Obtener estadísticas globales del sistema de proyectos",
    description = """
        Retorna métricas agregadas del sistema:
        - Total de proyectos
        - Proyectos por estado
        - Proyectos creados este mes
        - Últimos proyectos creados
        - Últimos proyectos actualizados
        Incluye un resumen general util para dashboards.
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200", 
        description = "Estadísticas generadas correctamente"
    ),
    @ApiResponse(responseCode = "403", description = "Sin permisos para ver estadísticas")
})
public ResponseEntity<?> getGlobalStatistics() {
    return ResponseEntity.ok(projectService.getGlobalStatistics());
}




    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar datos de un proyecto",
        description = """
            Permite modificar los datos básicos de un proyecto.
            Solo el creador puede actualizar su propio proyecto.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proyecto actualizado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos"),
        @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectSaveDto projectDto, 
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");
        
        return ResponseEntity.ok(projectService.updateProject(id, projectDto, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_ASSIGN_CURATOR')")
    @PutMapping("/{id}/curator")
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
        
        return ResponseEntity.ok(projectService.reassignCurator(id, curatorId, adminId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_ADD_OBSERVATIONS')")
    @PutMapping("/{id}/observations")
    @Operation(
        summary = "Agregar observaciones de revisión",
        description = "Permite al curador agregar notas y observaciones durante la revisión."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Observaciones agregadas"),
        @ApiResponse(responseCode = "403", description = "Solo el curador asignado puede agregar observaciones"),
        @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<ProjectDto> addObservations(
            @PathVariable Long id,
            @Valid @RequestBody ReviewNotesDto body, 
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");
        
        return ResponseEntity.ok(projectService.addObservations(id, curatorId, body.notes(), accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_APPROVE')")
@PutMapping("/{id}/approve")
@Operation(
    summary = "Aprobar proyecto tras revisión",
    description = """
        Permite al curador aprobar un proyecto que está en revisión.
        Se deben especificar las fechas de inicio y fin de votación.
        La votación debe finalizar ANTES de que inicie el proyecto.
        """
)
@io.swagger.v3.oas.annotations.parameters.RequestBody(
    description = "Fechas de votación",
    required = true,
    content = @Content(
        schema = @Schema(implementation = ProjectApprovalDto.class),
        examples = @ExampleObject(
            value = """
                {
                    "votingStartAt": "2025-12-01T00:00:00",
                    "votingEndAt": "2025-12-15T23:59:59"
                }
                """
        )
    )
)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proyecto aprobado"),
        @ApiResponse(responseCode = "400", description = "Fechas de votación inválidas"),
        @ApiResponse(responseCode = "403", description = "Solo el curador asignado puede aprobar"),
        @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<ProjectDto> approveProject(
        @PathVariable Long id,
        @Valid @RequestBody ProjectApprovalDto approvalDto,
        HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Long curatorId = (Long) auth.getDetails();
    Long accessId = (Long) request.getAttribute("currentAccessId");
    
    return ResponseEntity.ok(
        projectService.approveProject(
            id, 
            curatorId, 
            approvalDto.votingStartAt(), 
            approvalDto.votingEndAt(), 
            accessId
        )
    );
}

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
    @GetMapping("/my-curated")
    @Operation(
        summary = "Listar proyectos asignados al curador autenticado",
        description = """
            Retorna proyectos asignados al curador que hace la petición.
            Permite filtrar por estado opcionalmente.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de proyectos asignados"),
        @ApiResponse(responseCode = "403", description = "Requiere rol CURATOR")
    })
    public ResponseEntity<List<ProjectDto>> listMyCuratedProjects(
            @Parameter(description = "Estado para filtrar (opcional)")
            @RequestParam(required = false) ProjectStatus status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();
        
        return ResponseEntity.ok(projectService.findByCurator(curatorId, status));
    }

    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @PutMapping("/{id}/submit")
    @Operation(
        summary = "Enviar proyecto a revisión",
        description = """
            Permite al líder comunitario enviar su proyecto para revisión.
            El proyecto debe estar en estado BORRADOR o OBSERVACIONES.
        Se asigna automáticamente un curador si no tiene uno asignado.
        """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Proyecto enviado a revisión exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "El proyecto no está en un estado válido para enviar"),
        @ApiResponse(responseCode = "403", description = "Solo el creador puede enviar su proyecto"),
        @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<ProjectDto> submitForReview(
            @PathVariable Long id, 
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");
            
        return ResponseEntity.ok(projectService.submitForReview(id, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_READY_TO_PUBLISH')")
    @GetMapping("/ready-to-publish")
    @Operation(
        summary = "Listar proyectos listos para publicar",
        description = "Lista todos los proyectos con estado LISTO_PARA_PUBLICAR."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de proyectos listos para publicar"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<List<ProjectDto>> getReadyToPublishProjects() {
        return ResponseEntity.ok(projectService.findReadyToPublish());
    }

    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar proyecto del sistema",
        description = "Elimina permanentemente un proyecto. Operación irreversible."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proyecto eliminado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos"),
        @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }
}