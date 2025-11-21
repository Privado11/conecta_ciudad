package com.unimagdalena.conectaCiudad.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CitizenActionType {

    CITIZEN_VOTE("CITIZEN_VOTE", "Voto ciudadano en proyecto"),
    CITIZEN_COMMENT("CITIZEN_COMMENT", "Comentario ciudadano en proyecto");

    private final String code;
    private final String description;

    public String getActionCode() {
        return this.code;
    }

    public static boolean isValidCitizenAction(String code) {
        if (code == null) return false;
        for (CitizenActionType type : values()) {
            if (type.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static CitizenActionType fromCode(String code) {
        for (CitizenActionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid citizen action code: " + code);
    }
}