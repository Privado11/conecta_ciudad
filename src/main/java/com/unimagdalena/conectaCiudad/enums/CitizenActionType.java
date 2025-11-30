package com.unimagdalena.conectaCiudad.enums;

public enum CitizenActionType {

    CITIZEN_VOTE,
    CITIZEN_COMMENT;

    public String getActionCode() {
        return this.name();
    }

    public static boolean isValidCitizenAction(String code) {
        if (code == null) return false;
        for (CitizenActionType type : values()) {
            if (type.name().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static CitizenActionType fromCode(String code) {
        for (CitizenActionType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException(code);
    }
}