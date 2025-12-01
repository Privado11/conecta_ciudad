package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class BaseAuditEvent extends ApplicationEvent {
    
    private final String actionType;
    private final String description;
    private final EntityType entityType;
    private final Long entityId;
    private final User user;
    private final Map<String, Object> metadata;
    
    protected BaseAuditEvent(Object source, String actionType, String description,
                            EntityType entityType, Long entityId, User user) {
        super(source);
        this.actionType = actionType;
        this.description = description;
        this.entityType = entityType;
        this.entityId = entityId;
        this.user = user;
        this.metadata = new HashMap<>();
    }
    
    protected BaseAuditEvent(Object source, String actionType, String description,
                            EntityType entityType, Long entityId, User user,
                            Map<String, Object> metadata) {
        super(source);
        this.actionType = actionType;
        this.description = description;
        this.entityType = entityType;
        this.entityId = entityId;
        this.user = user;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}
