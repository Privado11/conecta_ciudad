package com.unimagdalena.conectaCiudad.events;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.UserActionType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class UserDeletedEvent extends BaseAuditEvent {

    private final Long deletedUserId;
    private final String deletedUserName;
    private final String deletedUserEmail;

    public UserDeletedEvent(Object source, Long deletedUserId, String deletedUserName, String deletedUserEmail, User deleter) {
        super(source,
                UserActionType.USER_DELETED.name(),
                "User deleted: " + deletedUserEmail,
                EntityType.USER,
                deletedUserId,
                deleter,
                new HashMap<>(Map.of("deletedUserName", deletedUserName, "deletedUserEmail", deletedUserEmail)));
        this.deletedUserId = deletedUserId;
        this.deletedUserName = deletedUserName;
        this.deletedUserEmail = deletedUserEmail;
    }
}
