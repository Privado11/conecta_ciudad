package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class UserCreatedEvent extends BaseAuditEvent {
    
    private final User createdUser;
    
    public UserCreatedEvent(Object source, User createdUser, User creator) {
        super(source, "USER_CREATE",
              "Created user: " + createdUser.getName(),
              EntityType.USER,
              createdUser.getId(),
              creator,
              buildMetadata(createdUser));
        this.createdUser = createdUser;
    }
    
    private static Map<String, Object> buildMetadata(User user) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userName", user.getName());
        metadata.put("email", user.getEmail());
        metadata.put("roles", user.getRoles().stream()
            .map(role -> role.getName())
            .collect(Collectors.toList()));
        return metadata;
    }
}
