package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class RoleDeletedEvent extends BaseAuditEvent {
    
    private final String roleName;
    
    public RoleDeletedEvent(Object source, Long roleId, String roleName, User deleter) {
        super(source, "ROLE_DELETE",
              "Deleted role: " + roleName,
              EntityType.ROLE,
              roleId,
              deleter,
              buildMetadata(roleName));
        this.roleName = roleName;
    }
    
    private static Map<String, Object> buildMetadata(String roleName) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("roleName", roleName);
        return metadata;
    }
}
