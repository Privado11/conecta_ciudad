package com.unimagdalena.conectaCiudad.controllers.citizen;

import com.unimagdalena.conectaCiudad.services.citizen.CitizenActionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.CitizenActionRequest;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/citizen-actions")
@RequiredArgsConstructor
@Tag(
        name = "Acciones de Participación Ciudadana",
        description = """
        API para el registro de acciones de participación ciudadana como votos, comentarios 
        y otras interacciones con los proyectos de la plataforma.
        """
)
public class CitizenActionController {


    private final CitizenActionService citizenActionService;

    @PostMapping
    @Operation(
            summary = "Registrar una acción de participación ciudadana",
            description = """
            Permite registrar acciones de ciudadanos relacionadas con la participación en proyectos.
            
            **Tipos de acciones CIUDADANAS soportadas:**
            - CITIZEN_VOTE: Cuando un ciudadano vota en un proyecto
            - CITIZEN_COMMENT: Cuando un ciudadano comenta en un proyecto
           
            **Nota Importante:** Todos los tipos de acción tienen el prefijo 'CITIZEN_' 
            para identificar claramente que son acciones exclusivas de ciudadanos.

            **Flujo de registro:**
            1. El ciudadano realiza una acción en la interfaz (votar, comentar)
            2. El frontend envía CitizenActionRequest con: actionType, description, projectId
            3. El sistema obtiene automáticamente el usuario desde el token JWT
            4. Se valida que el usuario tenga rol CIUDADANO o LIDER_COMUNITARIO
            5. Se registra la acción en el sistema de auditoría
            6. Se retorna ActionDto con la información completa de la acción registrada

            **Para el otro grupo:**
            Este es el ÚNICO endpoint que deben usar para registrar acciones ciudadanas.
            Cualquier otra acción del sistema debe usar endpoints diferentes.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Acción ciudadana registrada exitosamente. Retorna ActionDto con los datos completos de la acción",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ActionDto.class),
                            examples = @ExampleObject(
                                    name = "citizenActionResponse",
                                    summary = "Respuesta exitosa con ActionDto",
                                    description = "El sistema retorna un ActionDto con toda la información de la acción registrada",
                                    value = """
                    {
                        "id": 350,
                        "actionType": "CITIZEN_VOTE",
                        "description": "Voto a favor del Proyecto de Renovación del Parque Central - Proyecto: 'Renovación del Parque Central' (ID: 15) - Ciudadano: Walter Jiménez (walter.jimenez@example.com)",
                        "actionAt": "2024-11-21T14:30:25",
                        "result": "SUCCESS",
                        "entityType": "PROJECT",
                        "entityId": 15,
                        "ipAddress": "192.168.1.100",
                        "user": {
                            "id": 15,
                            "name": "Walter Jiménez",
                            "email": "walter.jimenez@example.com"
                        }
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos - descripción vacía, projectId nulo, o actionType inválido",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Description is required",
                        "timestamp": "2024-11-21T14:30:25"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado - Token JWT inválido, expirado o ausente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token JWT inválido o expirado. Por favor inicie sesión nuevamente"
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado - El usuario NO tiene rol CIUDADANO o LIDER_COMUNITARIO",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "status": 403,
                        "error": "Forbidden",
                        "message": "Acceso denegado. Este endpoint es EXCLUSIVO para usuarios con rol CIUDADANO o LIDER_COMUNITARIO."
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Proyecto no encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                    {
                        "status": 404,
                        "error": "Not Found",
                        "message": "Project not found with id: 999"
                    }
                    """
                            )
                    )
            )
    })
    public ResponseEntity<ActionDto> registerAction(
            @Parameter(description = "Información de la acción ciudadana a registrar. SOLO para acciones de ciudadanos.")
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = """
                Request para registrar ÚNICAMENTE acciones de participación CIUDADANA.
                
                **Campos requeridos:**
                - actionType: Tipo de acción CIUDADANA (CITIZEN_VOTE o CITIZEN_COMMENT)
                - description: Descripción detallada de la acción realizada por el ciudadano (máximo 250 caracteres)
                - projectId: ID del proyecto sobre el cual el ciudadano realiza la acción
                
                **Tipos de acción disponibles (SOLO CIUDADANAS):**
                - CITIZEN_VOTE: Para registrar votos ciudadanos
                - CITIZEN_COMMENT: Para registrar comentarios ciudadanos
                - NO usar para acciones administrativas o de otros roles
                
                **Nota crítica:** El usuario que realiza la acción se obtiene automáticamente 
                del token JWT. El sistema valida que sea un ciudadano o líder comunitario.
                
                **Para el otro grupo:** 
                Solo enviar acciones con prefijo CITIZEN_ en este endpoint.
                """,
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CitizenActionRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "voteExample",
                                            summary = "Correcto: Voto ciudadano",
                                            description = "Ejemplo válido de registro de voto ciudadano",
                                            value = """
                        {
                            "actionType": "CITIZEN_VOTE",
                            "description": "Voto a favor del proyecto. Considero que es una excelente iniciativa para la comunidad",
                            "projectId": 15
                        }
                        """
                                    ),
                                    @ExampleObject(
                                            name = "commentExample",
                                            summary = "Correcto: Comentario ciudadano",
                                            description = "Ejemplo válido de registro de comentario ciudadano",
                                            value = """
                        {
                            "actionType": "CITIZEN_COMMENT",
                            "description": "Me parece una iniciativa muy necesaria. Sugiero incluir también áreas verdes adicionales",
                            "projectId": 15
                        }
                        """
                                    )
                            }
                    )
            )
            @Valid @RequestBody CitizenActionRequest request) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();


        ActionDto actionDto = citizenActionService.registerCitizenAction(request, userEmail);

        return ResponseEntity.ok(actionDto);
    }
}