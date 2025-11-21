package com.unimagdalena.conectaCiudad.services.citizen;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.ActionLogRequest;
import com.unimagdalena.conectaCiudad.Dto.action.CitizenActionRequest;
import com.unimagdalena.conectaCiudad.entities.Project;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.action.ActionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenActionServiceImpl implements CitizenActionService {

    private final ActionService actionService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final HttpServletRequest request;

    @Override
    @Transactional
    public ActionDto registerCitizenAction(CitizenActionRequest citizenRequest, String userEmail) {

        log.debug("Iniciando registro de acción ciudadana: {} para usuario: {}",
                citizenRequest.getActionType(), userEmail);
        
        validateRequest(citizenRequest);

        User user = findUserByEmail(userEmail);
        Project project = findProjectById(citizenRequest.getProjectId());
        Long accessId = getCurrentAccessId();

        Map<String, Object> metadata = buildMetadata(citizenRequest, project, user);
        String fullDescription = buildDescription(citizenRequest, project, user);

        String actionCode = citizenRequest.getActionType().getActionCode();

        ActionLogRequest logRequest = ActionLogRequest.builder()
                .actionType(actionCode)
                .description(fullDescription)
                .entityType(EntityType.PROJECT)
                .entityId(project.getId())
                .result(ActionResult.SUCCESS)
                .metadata(metadata)
                .userId(user.getId())
                .accessId(accessId)
                .build();

        ActionDto actionDto = actionService.logActionWithDetails(logRequest);

        log.info("Acción ciudadana registrada exitosamente: {} - Usuario: {} - Proyecto: {} (ID: {})",
                citizenRequest.getActionType(),
                user.getEmail(),
                project.getName(),
                project.getId());

        return actionDto;
    }

    private void validateRequest(CitizenActionRequest request) {

        if (request.getActionType() == null) {
            throw new BadRequestException("El tipo de acción es requerido");
        }

        if (request.getProjectId() == null) {
            throw new BadRequestException("El ID del proyecto es requerido");
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new BadRequestException("La descripción es requerida");
        }
    }


    private User findUserByEmail(String email) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new ResourceNotFoundException("Usuario no encontrado con email: " + email);
        }

        return user;
    }

    private Project findProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.error("Proyecto no encontrado con ID: {}", projectId);
                    return new ResourceNotFoundException("Project", "id", projectId);
                });
    }


    private Map<String, Object> buildMetadata(CitizenActionRequest request,
                                              Project project,
                                              User user) {

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("projectName", project.getName());
        metadata.put("projectStatus", project.getStatus().name());
        metadata.put("projectBudget", project.getBudget());

        metadata.put("citizenId", user.getId());
        metadata.put("citizenName", user.getName());
        metadata.put("citizenEmail", user.getEmail());

        addActionSpecificMetadata(metadata, request);

        metadata.put("registeredAt", System.currentTimeMillis());

        return metadata;
    }

    private void addActionSpecificMetadata(Map<String, Object> metadata,
                                           CitizenActionRequest request) {

        switch (request.getActionType()) {

            case CITIZEN_VOTE:
                metadata.put("actionCategory", "VOTING");
                metadata.put("voteType", "SUPPORT");
                metadata.put("votingSystem", "SIMPLE");
                break;

            case CITIZEN_COMMENT:
                metadata.put("actionCategory", "ENGAGEMENT");
                metadata.put("commentLength", request.getDescription().length());
                metadata.put("hasLongComment", request.getDescription().length() > 100);
                metadata.put("commentType", "FEEDBACK");
                break;

            default:
                log.warn("Tipo de acción no reconocido: {}", request.getActionType());
        }
    }

    private String buildDescription(CitizenActionRequest request,
                                    Project project,
                                    User user) {

        return String.format(
                "%s - Proyecto: '%s' (ID: %d) - Ciudadano: %s (%s)",
                request.getDescription(),
                project.getName(),
                project.getId(),
                user.getName(),
                user.getEmail()
        );
    }


    private Long getCurrentAccessId() {
        try {
            Object accessId = request.getAttribute("currentAccessId");

            if (accessId != null) {
                log.debug("AccessId obtenido del request: {}", accessId);
                return (Long) accessId;
            }

            log.debug("No hay accessId en el request");
            return null;

        } catch (Exception e) {
            log.debug("Error al obtener accessId del request: {}", e.getMessage());
            return null;
        }
    }
}
