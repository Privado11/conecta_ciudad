package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class UserUpdatedEvent extends BaseAuditEvent {
    
    private final User updatedUser;
    
    public UserUpdatedEvent(Object source, User updatedUser, User updater) {
        super(source, "USER_UPDATE",
              "Updated user: " + updatedUser.getName(),
              EntityType.USER,
              updatedUser.getId(),
              updater,
              buildMetadata(updatedUser));
        this.updatedUser = updatedUser;
    }
    
    private static Map<String, Object> buildMetadata(User user) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userName", user.getName());
        metadata.put("email", user.getEmail());
        return metadata;
    }
}
