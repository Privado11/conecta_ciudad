package com.unimagdalena.conectaCiudad.enums;

public enum ProjectStatus {
    PENDIENTE("Pendiente de revisión"),
    EN_REVISION("En revisión"),
    OBSERVACIONES("Devuelto con observaciones"),
    APROBADO("Aprobado"),
    LISTO_PARA_PUBLICAR("Listo para publicar"),
    PUBLICADO("Publicado"),
    RECHAZADO("Rechazado");

    private final String description;

    ProjectStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
