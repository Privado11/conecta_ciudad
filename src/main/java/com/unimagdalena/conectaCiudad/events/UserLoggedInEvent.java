package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.UserActionType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class UserLoggedInEvent extends BaseAuditEvent {

    private final Access access;

    public UserLoggedInEvent(Object source, User user, Access access) {
        super(source,
                UserActionType.USER_LOGIN.name(),
                "Successful login",
                EntityType.ACCESS,
                access.getId(),
                user,
                createMetadata(access, user));
        this.access = access;
    }

    private static Map<String, Object> createMetadata(Access access, User user) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ipAddress", access.getIpAddress());
        metadata.put("location", access.getLocation());
        metadata.put("userAgent", access.getUserAgent());
        metadata.put("userId", user.getId());
        metadata.put("userEmail", user.getEmail());
        return metadata;
    }
}
