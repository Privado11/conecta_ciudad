package com.unimagdalena.conectaCiudad.enums;


public enum CitizenActionType {
    CITIZEN_VOTE("Voto de ciudadano"),
    
    CITIZEN_COMMENT("Comentario de ciudadano");

    private final String description;

    
    CitizenActionType(String description) {
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
