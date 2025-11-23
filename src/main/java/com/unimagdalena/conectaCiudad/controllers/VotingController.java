package com.unimagdalena.conectaCiudad.controllers;

import com.unimagdalena.conectaCiudad.Dto.voting.VotingProjectDto;
import com.unimagdalena.conectaCiudad.Dto.voting.VotingStatsDto;
import com.unimagdalena.conectaCiudad.services.voting.VotingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/voting")
@RequiredArgsConstructor
@Tag(
        name = "Gestión de Votaciones (Admin)",
        description = "Endpoints para administradores para monitorear y gestionar votaciones de proyectos"
)
public class VotingController {

    private final VotingService votingService;


    @GetMapping("/projects")
    @Operation(
            summary = "Obtener todos los proyectos en votación",
            description = """
                    Retorna todos los proyectos que están o estuvieron en proceso de votación,
                    incluyendo tanto votaciones abiertas como cerradas.
                    
                    **Información incluida:**
                    - Datos básicos del proyecto (nombre, descripción, presupuesto, etc.)
                    - Estado de votación (OPEN o CLOSED)
                    - Conteo de votos (a favor, en contra, total)
                    - Tasa de participación
                    - Para votaciones abiertas: días/horas restantes, nivel de urgencia
                    - Para votaciones cerradas: resultado final (APROBADO/RECHAZADO), porcentaje de aprobación
                    
                    **Permisos requeridos:** ADMIN_VIEW o DASHBOARD_VIEW
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de proyectos en votación obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VotingProjectDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado - Se requiere rol de administrador",
                    content = @Content
            )
    })
    public ResponseEntity<List<VotingProjectDto>> getAllVotingProjects(HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(votingService.getAllVotingProjects(token));
    }

   
    @GetMapping("/statistics")
    @Operation(
            summary = "Obtener estadísticas agregadas de votaciones",
            description = """
                    Retorna estadísticas globales sobre todas las votaciones del sistema.
                    
                    **Estadísticas incluidas:**
                    - Total de votaciones (abiertas + cerradas)
                    - Cantidad de votaciones abiertas
                    - Cantidad de votaciones cerradas
                    - Total de votos emitidos en todas las votaciones
                    - Tasa promedio de participación
                    - Tasa de aprobación (% de proyectos aprobados)
                    - Tasa de rechazo (% de proyectos rechazados)
                    - Promedio de votos por proyecto
                    
                    **Permisos requeridos:** ADMIN_VIEW o DASHBOARD_VIEW
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Estadísticas de votación obtenidas exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VotingStatsDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado - Se requiere rol de administrador",
                    content = @Content
            )
    })
    public ResponseEntity<VotingStatsDto> getVotingStatistics(HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(votingService.getVotingStatistics(token));
    }

  
    @GetMapping("/projects/open")
    @Operation(
            summary = "Obtener solo proyectos con votación abierta",
            description = """
                    Retorna únicamente los proyectos que actualmente están en período de votación activa.
                    
                    **Información incluida:**
                    - Datos del proyecto
                    - Conteo actual de votos
                    - Días y horas restantes para votar
                    - Nivel de urgencia (CRITICAL, HIGH, NORMAL)
                    - Tasa de participación actual
                    
                    **Nivel de urgencia:**
                    - CRITICAL: ≤ 2 días restantes
                    - HIGH: ≤ 7 días restantes
                    - NORMAL: > 7 días restantes
                    
                    **Permisos requeridos:** ADMIN_VIEW o DASHBOARD_VIEW
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de votaciones abiertas obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VotingProjectDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado - Se requiere rol de administrador",
                    content = @Content
            )
    })
    public ResponseEntity<List<VotingProjectDto>> getOpenVotingProjects(HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(votingService.getOpenVotingProjects(token));
    }

   
    @GetMapping("/projects/closed")
    @Operation(
            summary = "Obtener solo proyectos con votación cerrada",
            description = """
                    Retorna únicamente los proyectos cuyo período de votación ya finalizó.
                    
                    **Información incluida:**
                    - Datos del proyecto
                    - Conteo final de votos (a favor, en contra, total)
                    - Resultado final (APPROVED o REJECTED)
                    - Porcentaje de aprobación
                    - Fecha de cierre de votación
                    - Tasa de participación final
                    
                    **Criterio de aprobación:**
                    - APPROVED: > 50% de votos a favor
                    - REJECTED: ≤ 50% de votos a favor
                    
                    **Permisos requeridos:** ADMIN_VIEW o DASHBOARD_VIEW
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de votaciones cerradas obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VotingProjectDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado - Se requiere rol de administrador",
                    content = @Content
            )
    })
    public ResponseEntity<List<VotingProjectDto>> getClosedVotingProjects(HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(votingService.getClosedVotingProjects(token));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
