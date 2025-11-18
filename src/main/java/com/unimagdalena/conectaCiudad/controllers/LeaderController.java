package com.unimagdalena.conectaCiudad.controllers;


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

@RestController
@RequestMapping("/api/v1/leader")
@RequiredArgsConstructor
@Tag(
        name = "Líder Comunitario",
        description = "Endpoints para la creación, edición y envío a revisión de proyectos por parte de líderes comunitarios."
)

public class LeaderController {
    private final LeaderService leaderService;

    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @PostMapping("/projects")
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

        return ResponseEntity.ok(leaderService.createProject(projectSaveDto, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @PutMapping("/project/{id}")
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

        return ResponseEntity.ok(leaderService.updateProject(id, projectDto, creatorId, accessId));
    }


    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @PutMapping("/project/{id}/submit")
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

        return ResponseEntity.ok(leaderService.submitForReview(id, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    @DeleteMapping("/project/{id}")
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
        leaderService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }


}
