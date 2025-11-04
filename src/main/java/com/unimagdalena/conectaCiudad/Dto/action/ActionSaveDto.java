package com.unimagdalena.conectaCiudad.Dto.action;

import com.unimagdalena.conectaCiudad.entities.Access;

public record ActionSaveDto(String name, String description, Long userId, Access access) {
}
