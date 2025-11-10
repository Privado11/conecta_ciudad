package com.unimagdalena.conectaCiudad.enums;

public enum EntityType {
    USER("Entidad de usuario"),
    PROJECT("Entidad de proyecto"),
    REVIEW("Entidad de reseña"),
    ROLE("Entidad de rol"),
    ACCESS("Entidad de acceso"),
    SYSTEM("Entidad del sistema");

    private final String description;

    EntityType(String description) {
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