package com.unimagdalena.conectaCiudad.controllers;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectApprovalDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ReviewNotesDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(
        name = "Gestión de Curadores",
        description = "Endpoints para acciones del curador: observaciones, aprobación y proyectos asignados."
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
    @GetMapping("projects/my-curated")
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

}
