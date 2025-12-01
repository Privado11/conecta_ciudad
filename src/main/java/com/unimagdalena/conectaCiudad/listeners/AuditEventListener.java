package com.unimagdalena.conectaCiudad.listeners;


import com.unimagdalena.conectaCiudad.Dto.action.ActionLogRequest;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.events.*;
import com.unimagdalena.conectaCiudad.services.action.ActionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {
    
    private final ActionService actionService;
    private final HttpServletRequest request;
    
    @Async("auditExecutor")
    @EventListener
    public void handleBaseAuditEvent(BaseAuditEvent event) {
        try {
            Long accessId = getCurrentAccessId();
            if (accessId == null && event instanceof UserLoggedInEvent) {
                accessId = ((UserLoggedInEvent) event).getAccess().getId();
            }
            
            ActionLogRequest logRequest = ActionLogRequest.builder()
                .actionType(event.getActionType())
                .description(event.getDescription())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .result(ActionResult.SUCCESS)
                .userId(event.getUser() != null ? event.getUser().getId() : null)
                .accessId(accessId)
                .metadata(convertMetadataToJson(event.getMetadata()))
                .ipAddress(event instanceof UserLoggedInEvent ? ((UserLoggedInEvent) event).getAccess().getIpAddress() : null)
                .userAgent(event instanceof UserLoggedInEvent ? ((UserLoggedInEvent) event).getAccess().getUserAgent() : null)
                .build();
            
            actionService.logActionWithDetails(logRequest);
            log.debug("Logged audit event: {} for entity {} with id {}", 
                     event.getActionType(), event.getEntityType(), event.getEntityId());
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", event.getActionType(), e);
        }
    }
    
    @Async("auditExecutor")
    @EventListener
    public void handleActionFailed(ActionFailedEvent event) {
        try {
            Long accessId = getCurrentAccessId();
            
            ActionLogRequest logRequest = ActionLogRequest.builder()
                .actionType(event.getActionType())
                .description(event.getDescription() + " - Error: " + event.getException().getMessage())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .result(ActionResult.FAILED)
                .userId(event.getUser() != null ? event.getUser().getId() : null)
                .accessId(accessId)
                .metadata(convertMetadataToJson(event.getMetadata()))
                .build();
            
            actionService.logActionWithDetails(logRequest);
            log.debug("Logged failed action: {} for entity {} with id {}", 
                     event.getActionType(), event.getEntityType(), event.getEntityId());
        } catch (Exception e) {
            log.error("Failed to log action failure event", e);
        }
    }
    
    private Long getCurrentAccessId() {
        try {
            Object accessId = request.getAttribute("currentAccessId");
            return accessId != null ? (Long) accessId : null;
        } catch (Exception e) {
            log.debug("Could not retrieve accessId from request: {}", e.getMessage());
            return null;
        }
    }
    
    private Map<String, Object> convertMetadataToJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        return metadata;
    }
}
