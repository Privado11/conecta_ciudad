package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
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


}