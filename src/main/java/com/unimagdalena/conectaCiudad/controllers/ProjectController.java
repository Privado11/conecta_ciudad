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
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.entities.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Operaciones para gestión de proyectos")
public class ProjectController {
    
    private final ProjectService projectService;
    private final UserRepository userRepository;

    @PreAuthorize("hasAuthority('LIDER_COMUNITARIO')")
    @PostMapping
    @Operation(
        summary = "Crear proyecto",
        description = "Crea un nuevo proyecto asociado al usuario autenticado (rol LIDER_COMUNITARIO)",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del proyecto a crear",
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
            description = "Proyecto creado exitosamente",
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
                        "status": "PENDING_REVIEW",
                        "creator": {
                            "id": 5,
                            "name": "María González"
                        },
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
            description = "Datos de entrada inválidos",
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
            responseCode = "403",
            description = "No autorizado - Se requiere rol LIDER_COMUNITARIO",
            content = @Content
        )
    })
    public ResponseEntity<ProjectDto> createProject(
            @Valid @RequestBody ProjectSaveDto projectSaveDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User creator = userRepository.findByEmail(email);
        return ResponseEntity.ok(projectService.saveProject(projectSaveDto, creator.getId()));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obtener proyecto por ID",
        description = "Obtiene los detalles completos de un proyecto específico por su ID"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Proyecto encontrado",
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
            description = "Proyecto no encontrado",
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
            @Parameter(description = "ID del proyecto a buscar", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @GetMapping
    @Operation(
        summary = "Buscar/Listar proyectos",
        description = "Filtra proyectos por nombre o por ID de creador. Si no se especifican filtros, retorna todos los proyectos."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de proyectos que coinciden con los criterios de búsqueda",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProjectDto[].class),
                examples = @ExampleObject(
                    name = "projectsList",
                    value = """
                    [
                        {
                            "id": 1,
                            "name": "Parque Comunal La Esperanza",
                            "objectives": "Construir un parque para la comunidad",
                            "status": "IN_REVIEW",
                            "startAt": "2024-01-15T08:00:00",
                            "endAt": "2024-06-30T17:00:00",
                            "status": "PENDIENTE",
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
                        },
                        {
                            "id": 2,
                            "name": "Taller de reciclaje comunitario",
                            "objectives": "Capacitar a la comunidad en prácticas de reciclaje",
                            "status": "APPROVED",
                            "startAt": "2024-02-01T09:00:00",
                            "endAt": "2024-11-30T18:00:00",
                            "status": "PENDIENTE",
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
                    ]
                    """
                )
            )
        )
    })
    public ResponseEntity<List<ProjectDto>> searchProjects(
            @Parameter(
                description = "Filtrar por nombre del proyecto (búsqueda por coincidencia parcial, case insensitive)",
                example = "parque"
            )
            @RequestParam(required = false) String name,
            
            @Parameter(
                description = "Filtrar por ID del creador del proyecto",
                example = "5"
            )
            @RequestParam(required = false) Long creatorId) {

        if (name != null) {
            return ResponseEntity.ok(projectService.findByNameContainingIgnoreCase(name));
        } else if (creatorId != null) {
            return ResponseEntity.ok(projectService.findByCreatorId(creatorId));
        } else {
            return ResponseEntity.ok(projectService.findAll());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar proyecto")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody ProjectSaveDto projectDto) {
        return ResponseEntity.ok(projectService.updateProject(id, projectDto));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}/curator")
    @Operation(summary = "Reasignar curador", description = "Permite a un ADMIN reasignar el curador de un proyecto")
    public ResponseEntity<?> reassignCurator(@PathVariable Long id, @RequestParam Long curatorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User admin = userRepository.findByEmail(email);
        return ResponseEntity.ok(projectService.reassignCurator(id, curatorId, admin.getId()));
    }

    @PreAuthorize("hasAuthority('CURATOR')")
    @PutMapping("/{id}/observations")
    @Operation(summary = "Agregar observaciones", description = "Permite al curador añadir notas de revisión al proyecto")
    public ResponseEntity<?> addObservations(@PathVariable Long id, @Valid @RequestBody ReviewNotesDto body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User curator = userRepository.findByEmail(email);
        return ResponseEntity.ok(projectService.addObservations(id, curator.getId(), body.notes()));
    }

    @PreAuthorize("hasAuthority('CURATOR')")
    @PutMapping("/{id}/approve")
    @Operation(summary = "Aprobar proyecto", description = "Permite al curador aprobar un proyecto")
    public ResponseEntity<?> approveProject(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User curator = userRepository.findByEmail(email);
        return ResponseEntity.ok(projectService.approveProject(id, curator.getId()));
    }

    @PreAuthorize("hasAuthority('CURATOR')")
    @GetMapping("/my")
    @Operation(summary = "Listar mis proyectos curados", description = "Lista los proyectos asignados al curador autenticado; opcionalmente filtra por estado")
    public ResponseEntity<?> listMyCuratedProjects(@RequestParam(required = false) ProjectStatus status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User curator = userRepository.findByEmail(email);
        return ResponseEntity.ok(projectService.findByCurator(curator.getId(), status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar proyecto")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }
}
