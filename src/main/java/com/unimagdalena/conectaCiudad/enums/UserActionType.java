package com.unimagdalena.conectaCiudad.enums;

public enum UserActionType {
    USER_CREATED("Creación de usuario"),
    USER_PROFILE_UPDATED("Actualización de perfil de usuario"),
    USER_UPDATED("Actualización de usuario"),
    USER_PASSWORD_CHANGED("Cambio de contraseña"),
    USER_DELETED("Eliminación de usuario"),
    USER_ACTIVATED("Activación de usuario"),
    USER_DEACTIVATED("Desactivación de usuario"),
    USER_ROLE_ADDED("Adición de rol al usuario"),
    USER_ROLE_REMOVED("Remoción de rol del usuario"),
    USER_LOGIN("Inicio de sesión"),
    USER_LOGIN_FAILED("Intento fallido de inicio de sesión"),
    USER_BULK_IMPORT("Importación masiva de usuarios"),
    USER_EXPORT("Exportación de usuarios");

    private final String description;

    UserActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
