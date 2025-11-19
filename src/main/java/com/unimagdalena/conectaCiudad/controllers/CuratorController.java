package com.unimagdalena.conectaCiudad.controllers;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectApprovalDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ReviewNotesDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewDto;
import com.unimagdalena.conectaCiudad.Dto.review.PendingReviewQueueDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.review.ReviewHistoryFilterDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.services.curator.CuratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/curator")
@RequiredArgsConstructor
@Tag(
        name = "Gestión de Curadores",
        description = "Endpoints para acciones del curador: observaciones, aprobación, proyectos asignados y cola de revisiones."
)
public class CuratorController {

    private final CuratorService curatorService;

    @PreAuthorize("hasAuthority('PROJECT_ADD_OBSERVATIONS')")
    @PutMapping("/project/{id}/observations")
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

        return ResponseEntity.ok(curatorService.addObservations(id, curatorId, body.notes(), accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_APPROVE')")
    @PutMapping("/project/{id}/approve")
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
                    "votingStartAt": "2025-12-01",
                    "votingEndAt": "2025-12-15"
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
                curatorService.approveProject(
                        id,
                        curatorId,
                        approvalDto.votingStartAt(),
                        approvalDto.votingEndAt(),
                        accessId
                )
        );
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
    @GetMapping("/projects/my-curated")
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

        return ResponseEntity.ok(curatorService.findByCurator(curatorId, status));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
    @GetMapping("/reviews/pending-queue")
    @Operation(
            summary = "Obtener cola de revisiones pendientes",
            description = """
            Obtiene todas las revisiones pendientes del curador con información detallada:
            - Información completa del proyecto (nombre, objetivos, presupuesto, fechas)
            - Datos del creador del proyecto
            - Indicadores de vencimiento y prioridad
            - Días hasta vencimiento o días de retraso
            - Identificación de reenvíos
            - Estadísticas generales de la cola
            
            Los resultados se ordenan automáticamente:
            1. Revisiones vencidas primero
            2. Luego por proximidad de vencimiento (más urgentes primero)
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cola de revisiones pendientes con estadísticas",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PendingReviewQueueDto.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                        "reviews": [
                            {
                                "projectId": 1,
                                "projectName": "Proyecto Educativo",
                                "objectives": "Mejorar educación",
                                "budget": 50000.00,
                                "projectStartAt": "2025-12-01",
                                "projectEndAt": "2026-06-30",
                                "creatorName": "Juan Pérez",
                                "creatorEmail": "juan@example.com",
                                "assignedAt": "2025-11-10T10:00:00Z",
                                "dueAt": "2025-11-15T10:00:00Z",
                                "daysUntilDue": -3,
                                "isOverdue": true,
                                "isDueSoon": false,
                                "daysInReview": 8,
                                "isResubmission": false,
                                "priorityLevel": "CRÍTICA"
                            }
                        ],
                        "statistics": {
                            "total": 5,
                            "overdue": 1,
                            "dueSoon": 2,
                            "criticalPriority": 1,
                            "highPriority": 2,
                            "resubmissions": 1
                        }
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Requiere permiso de curador")
    })
    public ResponseEntity<PendingReviewQueueDto> getPendingReviewQueue() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();

        return ResponseEntity.ok(curatorService.getPendingReviewQueue(curatorId));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
    @GetMapping("/reviews/pending/{projectId}")
    @Operation(
            summary = "Obtener detalles de una revisión pendiente específica",
            description = """
            Obtiene información detallada de una revisión pendiente específica.
            Solo el curador asignado puede acceder a estos detalles.
            Incluye todos los datos del proyecto, indicadores de vencimiento y prioridad.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalles de la revisión pendiente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PendingReviewDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "La revisión ya fue completada"),
            @ApiResponse(responseCode = "403", description = "Solo el curador asignado puede ver estos detalles"),
            @ApiResponse(responseCode = "404", description = "Revisión no encontrada")
    })
    public ResponseEntity<PendingReviewDto> getPendingReviewDetails(
            @Parameter(description = "ID del proyecto a revisar")
            @PathVariable Long projectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long curatorId = (Long) auth.getDetails();

        PendingReviewDto reviewDetails = curatorService.getPendingReviewDetails(projectId, curatorId);
        return ResponseEntity.ok(reviewDetails);
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_ASSIGNED')")
@GetMapping("/reviews/history")
@Operation(
        summary = "Obtener historial paginado de revisiones",
        description = """
        Obtiene el historial completo de revisiones del curador con:
        - Paginación estándar de Spring Data
        - Filtros avanzados (búsqueda, estado, outcome, fechas)
        - Estadísticas globales en el objeto statistics
        
        Parámetros de paginación:
        - page: número de página (0-indexed)
        - size: elementos por página
        - sort: campo,dirección (ej: reviewedAt,desc)
        
        Estructura de respuesta:
        {
            "page": { ... página de Spring Data ... },
            "statistics": {
                "total": 50,
                "metrics": {
                    "approved": 35,
                    "returned": 12,
                    "rejected": 3,
                    "resubmissions": 5,
                    "averageDaysToComplete": 4.2,
                    "completedOnTime": 45,
                    "completedOverdue": 5,
                    "approvalRate": 70.0,
                    "returnRate": 24.0,
                    "onTimeRate": 90.0
                }
            }
        }
        """
)
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Historial paginado con estadísticas",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = PagedResponse.class)
                )
        )
})
public ResponseEntity<PagedResponse<ReviewHistoryDto>> getReviewHistory(
        @Parameter(description = "Búsqueda por nombre de proyecto o creador")
        @RequestParam(required = false) String searchTerm,
        
        @Parameter(description = "Filtrar por estado del proyecto")
        @RequestParam(required = false) ProjectStatus status,
        
        @Parameter(description = "Filtrar por resultado: APROBADO, DEVUELTO, RECHAZADO, all")
        @RequestParam(required = false, defaultValue = "all") String outcome,
        
        @Parameter(description = "Filtrar por revisiones completadas tarde")
        @RequestParam(required = false) Boolean wasOverdue,
        
        @Parameter(description = "Filtrar por reenvíos")
        @RequestParam(required = false) Boolean isResubmission,
        
        @Parameter(description = "Fecha de revisión desde (ISO 8601)")
        @RequestParam(required = false) OffsetDateTime reviewedFrom,
        
        @Parameter(description = "Fecha de revisión hasta (ISO 8601)")
        @RequestParam(required = false) OffsetDateTime reviewedTo,
        
        @Parameter(description = "Número de página (0-indexed)")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Elementos por página")
        @RequestParam(defaultValue = "10") int size,
        
        @Parameter(description = "Campo de ordenamiento")
        @RequestParam(defaultValue = "reviewedAt") String sortBy,
        
        @Parameter(description = "Dirección de ordenamiento (asc/desc)")
        @RequestParam(defaultValue = "desc") String sortDir) {
    
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Long curatorId = (Long) auth.getDetails();
    
    ReviewHistoryFilterDto filters = new ReviewHistoryFilterDto(
        searchTerm,
        status,
        outcome,
        wasOverdue,
        isResubmission,
        reviewedFrom,
        reviewedTo
    );
    
    Sort sort = sortDir.equalsIgnoreCase("asc") 
        ? Sort.by(sortBy).ascending() 
        : Sort.by(sortBy).descending();
    
    Pageable pageable = PageRequest.of(page, size, sort);
    
    return ResponseEntity.ok(curatorService.getReviewHistory(curatorId, filters, pageable));
}
}