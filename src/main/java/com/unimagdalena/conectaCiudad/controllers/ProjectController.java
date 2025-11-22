package com.unimagdalena.conectaCiudad.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectReadyDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectVotingDto;
import com.unimagdalena.conectaCiudad.services.project.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "API para gestión completa del ciclo de vida de proyectos comunitarios")
public class ProjectController {
    
    private final ProjectService projectService;

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

    @PreAuthorize("hasAuthority('PROJECT_VIEW_READY_TO_PUBLISH')")
    @GetMapping("/ready-to-publish")
    @Operation(
        summary = "Listar proyectos listos para publicar",
        description = "Lista todos los proyectos con estado READY_TO_PUBLISH."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de proyectos listos para publicar"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<List<ProjectDto>> getReadyToPublishProjects() {
        return ResponseEntity.ok(projectService.findReadyToPublish());
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW_READY_TO_PUBLISH')")
    @GetMapping("/ready-not-open")
    @Operation(
        summary = "Listar proyectos listos pero no abiertos a votación",
        description = """
            Lista proyectos con estado READY_TO_PUBLISH que aún no están abiertos a votación.
            Incluye:
            - Proyectos sin fechas de votación definidas
            - Proyectos con votación programada a futuro
            
            Información adicional:
            - Días hasta el inicio de la votación
            - Duración planificada de la votación
            - Estado del cronograma
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de proyectos listos no abiertos"),
        @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<List<ProjectReadyDto>> getReadyToPublishNotOpen() {
        return ResponseEntity.ok(projectService.findReadyToPublishNotOpen());
    }

    @GetMapping("/open-for-voting")
    @Operation(
        summary = "Listar proyectos abiertos a votación",
        description = """
            Lista proyectos con estado PUBLISHED que están actualmente abiertos a votación ciudadana.
            
            Información detallada incluida:
            - Días y horas restantes para votar
            - Nivel de urgencia (CRITICAL ≤1 día, HIGH ≤3 días, NORMAL >3 días)
            - Porcentaje de progreso de la votación
            - Duración total del período de votación
            - Indicador si está próximo a vencer
            - Mensaje de estado descriptivo
            
            Los proyectos se ordenan por fecha de cierre (más próximos a vencer primero).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de proyectos abiertos a votación"),
        @ApiResponse(responseCode = "204", description = "No hay proyectos abiertos actualmente")
    })
    public ResponseEntity<List<ProjectVotingDto>> getOpenForVoting(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = extractToken(request);
        Long citizenId = (Long) auth.getDetails();
        List<ProjectVotingDto> projects = projectService.findOpenForVoting(citizenId, token);

        if (projects.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(projects);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

}