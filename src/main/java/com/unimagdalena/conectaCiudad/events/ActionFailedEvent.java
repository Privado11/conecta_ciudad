package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ActionFailedEvent extends ApplicationEvent {
    
    private final String actionType;
    private final String description;
    private final Exception exception;
    private final EntityType entityType;
    private final Long entityId;
    private final User user;
    
    public ActionFailedEvent(Object source, String actionType, String description,
                            Exception exception, EntityType entityType, Long entityId, User user) {
        super(source);
        this.actionType = actionType;
        this.description = description;
        this.exception = exception;
        this.entityType = entityType;
        this.entityId = entityId;
        this.user = user;
    }
    
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("error", exception.getMessage());
        metadata.put("exceptionType", exception.getClass().getSimpleName());
        metadata.put("timestamp", System.currentTimeMillis());
        return metadata;
    }
}
