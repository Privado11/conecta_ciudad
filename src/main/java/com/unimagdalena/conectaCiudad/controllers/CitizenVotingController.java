package com.unimagdalena.conectaCiudad.controllers;

import com.unimagdalena.conectaCiudad.Dto.voting.UserVoteHistoryDto;
import com.unimagdalena.conectaCiudad.Dto.voting.UserVotingStatsDto;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/citizen/voting")
@RequiredArgsConstructor
@Tag(
        name = "Votaciones (Ciudadano)",
        description = "Endpoints para ciudadanos para consultar su historial y estadísticas de votación"
)
public class CitizenVotingController {

    private final VotingService votingService;

    @GetMapping("/history")
    @Operation(
            summary = "Obtener historial de votaciones del usuario",
            description = """
                    Retorna el historial completo de votaciones del usuario autenticado.
                    
                    **Información incluida:**
                    - Para votaciones ABIERTAS: información del proyecto y tiempo restante
                    - Para votaciones CERRADAS: información del proyecto y resultados completos
                    
                    **Ordenamiento:** Los votos más recientes aparecen primero
                    
                    **Permisos requeridos:** Usuario autenticado (cualquier rol)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Historial de votaciones obtenido exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserVoteHistoryDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado - Se requiere token de autenticación",
                    content = @Content
            )
    })
    public ResponseEntity<List<UserVoteHistoryDto>> getUserVotingHistory(HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(votingService.getUserVotingHistory(token));
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Obtener estadísticas de votación del usuario",
            description = """
                    Retorna las estadísticas de votación del usuario autenticado.
                    Calculadas únicamente sobre proyectos con votación CERRADA.
                    
                    **Estadísticas incluidas:**
                    - Total de votos emitidos
                    - Votos a favor
                    - Votos en contra
                    - Tasa de participación (votos del usuario / total de proyectos cerrados)
                    
                    **Permisos requeridos:** Usuario autenticado (cualquier rol)
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Estadísticas obtenidas exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserVotingStatsDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado - Se requiere token de autenticación",
                    content = @Content
            )
    })
    public ResponseEntity<UserVotingStatsDto> getUserVotingStats(HttpServletRequest request) {
        String token = extractToken(request);
        return ResponseEntity.ok(votingService.getUserVotingStats(token));
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
