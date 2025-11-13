package com.unimagdalena.conectaCiudad.enums;

public enum RoleActionType {
    ROLE_PERMISSION_ADDED("Adición de permiso al rol"),
    ROLE_PERMISSION_REMOVED("Remoción de permiso del rol"),
    ROLE_PERMISSIONS_UPDATED("Actualización de permisos del rol");
   

    private final String description;

    RoleActionType(String description) {
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
