package com.unimagdalena.conectaCiudad.services.action;

import com.unimagdalena.conectaCiudad.Dto.action.ActionLogRequest;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.services.access.AccessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuditHelper {

    private final ActionService actionService;
    private final AccessService accessService;
    private final HttpServletRequest request;

    public void log(String actionType, String description) {
        try {
            Long accessId = getCurrentAccessId();
            if (accessId == null) {
                log.warn("No se puede registrar acción {} - No hay accessId en el contexto", actionType);
                return;
            }

            Access access = accessService.findById(accessId);
            if (access == null || access.getUser() == null) {
                log.warn("No se puede registrar acción {} - Access o User no encontrado", actionType);
                return;
            }

            actionService.logAction(actionType, description, access.getUser(), access);
        } catch (Exception e) {
            log.error("Error al registrar acción {}: {}", actionType, e.getMessage(), e);
        }
    }


    public void logEntity(String actionType, String description, EntityType entityType, Long entityId) {
        try {
            Long accessId = getCurrentAccessId();
            User user = getCurrentUser();
            
            if (user == null) {
                log.warn("No se puede registrar acción {} - Usuario no encontrado", actionType);
                return;
            }

            ActionLogRequest logRequest = ActionLogRequest.builder()
                .actionType(actionType)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .result(ActionResult.SUCCESS)
                .userId(user.getId())
                .accessId(accessId)
                .build();

            actionService.logActionWithDetails(logRequest);
        } catch (Exception e) {
            log.error("Error al registrar acción {} para entidad {}: {}", 
                     actionType, entityType, e.getMessage(), e);
        }
    }

    public void logWithResult(String actionType, String description, ActionResult result) {
        try {
            Long accessId = getCurrentAccessId();
            User user = getCurrentUser();
            
            if (user == null) {
                log.warn("No se puede registrar acción {} - Usuario no encontrado", actionType);
                return;
            }

            ActionLogRequest logRequest = ActionLogRequest.builder()
                .actionType(actionType)
                .description(description)
                .result(result)
                .userId(user.getId())
                .accessId(accessId)
                .build();

            actionService.logActionWithDetails(logRequest);
        } catch (Exception e) {
            log.error("Error al registrar acción {}: {}", actionType, e.getMessage(), e);
        }
    }

    public void logWithMetadata(String actionType, String description, Map<String, Object> metadata) {
        try {
            Long accessId = getCurrentAccessId();
            User user = getCurrentUser();
            
            if (user == null) {
                log.warn("No se puede registrar acción {} - Usuario no encontrado", actionType);
                return;
            }

            ActionLogRequest logRequest = ActionLogRequest.builder()
                .actionType(actionType)
                .description(description)
                .metadata(metadata)
                .result(ActionResult.SUCCESS)
                .userId(user.getId())
                .accessId(accessId)
                .build();

            actionService.logActionWithDetails(logRequest);
        } catch (Exception e) {
            log.error("Error al registrar acción {}: {}", actionType, e.getMessage(), e);
        }
    }

    public void logComplete(String actionType, String description, 
                           EntityType entityType, Long entityId,
                           ActionResult result, Map<String, Object> metadata) {
        try {
            Long accessId = getCurrentAccessId();
            User user = getCurrentUser();
            
            if (user == null) {
                log.warn("No se puede registrar acción {} - Usuario no encontrado", actionType);
                return;
            }

            ActionLogRequest logRequest = ActionLogRequest.builder()
                .actionType(actionType)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .result(result)
                .metadata(metadata)
                .userId(user.getId())
                .accessId(accessId)
                .build();

            actionService.logActionWithDetails(logRequest);
        } catch (Exception e) {
            log.error("Error al registrar acción completa {}: {}", actionType, e.getMessage(), e);
        }
    }

    
    public void logCompleteWithAccess(String actionType, String description, 
                                    EntityType entityType, Long entityId,
                                    ActionResult result, Map<String, Object> metadata,
                                    Access access) {
        try {
            if (access == null || access.getUser() == null) {
                log.warn("No se puede registrar acción {} - Access o User no encontrado", actionType);
                return;
            }

            ActionLogRequest logRequest = ActionLogRequest.builder()
                .actionType(actionType)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .result(result)
                .metadata(metadata)
                .userId(access.getUser().getId())
                .accessId(access.getId())
                .build();

            actionService.logActionWithDetails(logRequest);
        } catch (Exception e) {
            log.error("Error al registrar acción completa {}: {}", actionType, e.getMessage(), e);
        }
    }

    public void logFailure(String actionType, String description, String errorMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("error", errorMessage);
        metadata.put("timestamp", System.currentTimeMillis());
        
        logWithResult(actionType, description + " - Error: " + errorMessage, ActionResult.FAILED);
    }

    private Long getCurrentAccessId() {
        try {
            Object accessId = request.getAttribute("currentAccessId");
            return accessId != null ? (Long) accessId : null;
        } catch (Exception e) {
            log.debug("No se pudo obtener accessId del request: {}", e.getMessage());
            return null;
        }
    }

    private User getCurrentUser() {
        try {
            Long accessId = getCurrentAccessId();
            if (accessId == null) return null;
            
            Access access = accessService.findById(accessId);
            return access != null ? access.getUser() : null;
        } catch (Exception e) {
            log.debug("No se pudo obtener usuario actual: {}", e.getMessage());
            return null;
        }
    }
}