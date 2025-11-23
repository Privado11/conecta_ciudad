package com.unimagdalena.conectaCiudad.controllers;

import com.unimagdalena.conectaCiudad.Dto.leader.ProjectVotingResultDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.services.leader.LeaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
@RequestMapping("/api/v1/leader")
@RequiredArgsConstructor
@Tag(
        name = "Líder Comunitario",
        description = "Endpoints para la creación, edición y envío a revisión de proyectos por parte de líderes comunitarios."
)

public class LeaderController {
    private final LeaderService leaderService;

    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @GetMapping("/projects")
    @Operation(
            summary = "Obtener todos mis proyectos",
            description = """
            Retorna todos los proyectos creados por el líder comunitario autenticado.
            
            **Información incluida:**
            - Datos completos del proyecto
            - Estado actual del proyecto
            - Información del creador
            - Historial de revisiones (si existen)
            
            **Permisos requeridos:** PROJECT_VIEW
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de proyectos obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<List<ProjectDto>> getMyProjects(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        return ResponseEntity.ok(leaderService.getMyProjects(creatorId));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @GetMapping("/projects/voting-results")
    @Operation(
            summary = "Obtener resultados de votación de mis proyectos cerrados",
            description = """
            Retorna los resultados de votación de todos los proyectos del líder 
            que tienen votación cerrada (estado VOTING_CLOSED).
            
            **Información incluida:**
            - Datos completos del proyecto (nombre, descripción, objetivos, beneficiarios, presupuesto)
            - Fechas del proyecto (inicio y fin)
            - Fechas de votación (inicio, fin y cierre)
            - Conteo de votos (a favor, en contra, total)
            - Porcentaje de aprobación
            - Resultado final (APPROVED o REJECTED)
            
            **Criterio de aprobación:** > 50% de votos a favor
            
            **Permisos requeridos:** PROJECT_VIEW
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultados de votación obtenidos exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProjectVotingResultDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    public ResponseEntity<List<ProjectVotingResultDto>> getMyVotingResults(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        String token = extractToken(request);
        return ResponseEntity.ok(leaderService.getMyClosedVotingResults(creatorId, token));
    }

    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @PostMapping("/projects")
    @Operation(
            summary = "Crear nuevo proyecto comunitario",
            description = """
            Permite a un líder comunitario crear un nuevo proyecto. El proyecto se crea en estado BORRADOR 
            y queda asociado automáticamente al usuario autenticado como creador.
            
            **Restricciones de seguridad:**
            - Solo usuarios con rol LIDER_COMUNITARIO pueden crear proyectos
            - El creador se asigna automáticamente desde el token de autenticación JWT
            
            **Flujo completo del proyecto:**
            1. Creación → DRAFT (Borrador - Editable por el líder)
            2. Envío → PENDING_REVIEW (Pendiente de revisión)
            3. Asignación de curador → IN_REVIEW (En revisión)
            4. Si requiere cambios → RETURNED_WITH_OBSERVATIONS (Devuelto con observaciones - Editable)
            5. Aprobación → READY_TO_PUBLISH (Listo para publicar)
            6. Publicación → PUBLISHED (Publicado y visible para votación)
            
            **Estados editables:** DRAFT, RETURNED_WITH_OBSERVATIONS
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Proyecto creado exitosamente con estado DRAFT (Borrador)",
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

        return ResponseEntity.ok(leaderService.createProject(projectSaveDto, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @PutMapping("/project/{id}")
    @Operation(
            summary = "Actualizar datos de un proyecto",
            description = """
            Permite modificar los datos básicos de un proyecto.
            
            **Restricciones:**
            - Solo el creador puede actualizar su propio proyecto
            - El proyecto debe estar en estado DRAFT (Borrador) o RETURNED_WITH_OBSERVATIONS (Devuelto con observaciones)
            - No se pueden editar proyectos que ya están en revisión o publicados
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "El proyecto no está en un estado editable"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para editar este proyecto"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectSaveDto projectDto,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(leaderService.updateProject(id, projectDto, creatorId, accessId));
    }


    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @PutMapping("/project/{id}/submit")
    @Operation(
            summary = "Enviar proyecto a revisión",
            description = """
            Permite al líder comunitario enviar su proyecto para revisión por parte de un curador.
            
            **Estados válidos para envío:**
            - DRAFT (Borrador): Proyecto recién creado o guardado como borrador
            - RETURNED_WITH_OBSERVATIONS: Proyecto devuelto por el curador con observaciones que ya fueron corregidas
            
            **Proceso automático:**
            - El sistema asigna automáticamente un curador disponible si no tiene uno asignado
            - El estado cambia a PENDING_REVIEW (Pendiente de revisión)
            
            **Restricciones:**
            - Solo el creador del proyecto puede enviarlo
            - El proyecto debe estar completo y cumplir las validaciones básicas
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
            @ApiResponse(responseCode = "400", description = "El proyecto no está en estado DRAFT o RETURNED_WITH_OBSERVATIONS"),
            @ApiResponse(responseCode = "403", description = "Solo el creador puede enviar su proyecto a revisión"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<ProjectDto> submitForReview(
            @PathVariable Long id,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(leaderService.submitForReview(id, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    @DeleteMapping("/project/{id}")
    @Operation(
            summary = "Eliminar proyecto del sistema",
            description = """
            Elimina permanentemente un proyecto del sistema.
            
            **Advertencia:** Esta operación es irreversible y eliminará todos los datos asociados al proyecto.
            
            **Restricciones:**
            - Solo el creador puede eliminar su propio proyecto
            - Se recomienda solo eliminar proyectos en estado DRAFT
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto eliminado exitosamente"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para eliminar este proyecto"),
            @ApiResponse(responseCode = "404", description = "Proyecto no encontrado")
    })
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        leaderService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

}