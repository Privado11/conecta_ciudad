package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class RoleCreatedEvent extends BaseAuditEvent {
    
    private final Role role;
    
    public RoleCreatedEvent(Object source, Role role, User creator) {
        super(source, "ROLE_CREATE",
              "Created role: " + role.getName(),
              EntityType.ROLE,
              role.getId(),
              creator,
              buildMetadata(role));
        this.role = role;
    }
    
    private static Map<String, Object> buildMetadata(Role role) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("roleName", role.getName());
        return metadata;
    }
}
