package com.unimagdalena.conectaCiudad.Dto.action;

import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;

public record ActionSaveDto(String name, String description, User user, Access access) {
}
