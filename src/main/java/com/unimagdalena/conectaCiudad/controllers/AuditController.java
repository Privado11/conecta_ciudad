package com.unimagdalena.conectaCiudad.controllers;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.services.action.ActionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(
    name = "Auditoría y Trazabilidad",
    description = "API unificada para consultar acciones, filtros y estadísticas del sistema."
)
public class AuditController {

    private final ActionService actionService;
    private static final int MAX_PAGE_SIZE = 100;

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/search")
    @Operation(
        summary = "Búsqueda avanzada de acciones de auditoría",
        description = """
            Permite filtrar las acciones del sistema con múltiples criterios combinados:
            - tipo de acción (`actionType`)
            - resultado (`SUCCESS` o `FAILED`)
            - tipo de entidad (`entityType`)
            - término de búsqueda (`searchTerm`)
            - rango de fechas (`startDate`, `endDate`)
            Incluye paginación y estadísticas agregadas.
            """
    )
    public ResponseEntity<PagedResponse<ActionDto>> searchActions(
        @Parameter(description = "Tipo de acción (por ejemplo, USER_CREATED, PROJECT_UPDATED)")
        @RequestParam(required = false) String actionType,

        @Parameter(description = "Resultado de la acción (SUCCESS o FAILED)")
        @RequestParam(required = false) ActionResult result,

        @Parameter(description = "Tipo de entidad afectada (USER, PROJECT, ROLE, ACCESS, etc.)")
        @RequestParam(required = false) EntityType entityType,

        @Parameter(description = "Término de búsqueda (en nombre, email o descripción)")
        @RequestParam(required = false) String searchTerm,

        @Parameter(description = "Fecha de inicio del rango (formato ISO: yyyy-MM-dd'T'HH:mm:ss)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startDate,

        @Parameter(description = "Fecha de fin del rango (formato ISO: yyyy-MM-dd'T'HH:mm:ss)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endDate,

        @Parameter(description = "Número de página (inicia en 0)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Tamaño de página (máximo 100)")
        @RequestParam(defaultValue = "20") int size
    ) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);

        PagedResponse<ActionDto> response = actionService.searchWithFilters(
            actionType,
            result,
            entityType,
            searchTerm,
            startDate,
            endDate,
            pageable
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}/details")
    @Operation(
        summary = "Obtener detalles completos de una acción de auditoría",
        description = """
            Devuelve información enriquecida de una acción específica,
            incluyendo:
            - datos básicos (tipo, descripción, resultado, usuario, IP, fecha)
            - entidad afectada (USER, PROJECT, etc.)
            - metadatos registrados
            - cambios (valores antiguos/nuevos)
            """
    )
    public ResponseEntity<Map<String, Object>> getActionDetails(
        @Parameter(description = "ID de la acción de auditoría") @PathVariable Long id
    ) {
        Map<String, Object> details = actionService.getActionDetails(id);
        return ResponseEntity.ok(details);
    }
}
