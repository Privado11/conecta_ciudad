package com.unimagdalena.conectaCiudad.enums;

public enum ActionResult {
    SUCCESS("Acción exitosa"),
    FAILED("Acción fallida"),
    PARTIAL("Acción parcialmente exitosa");

    private final String description;

    ActionResult(String description) {
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