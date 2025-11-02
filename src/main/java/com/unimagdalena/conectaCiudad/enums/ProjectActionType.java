package com.unimagdalena.conectaCiudad.enums;

public enum ProjectActionType {
    CURATOR_ASSIGNED("Asignación de curador"),
    PROJECT_CREATED("Creación de proyecto"),
    PROJECT_UPDATED("Actualización de proyecto"),
    PROJECT_OBSERVATIONS_ADDED("Adición de observaciones"),
    PROJECT_APPROVED("Aprobación de proyecto"),
    CURATOR_REASSIGNED("Reasignación de curador");

    private final String description;

    ProjectActionType(String description) {
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
