package com.unimagdalena.conectaCiudad.Dto.access;

import com.unimagdalena.conectaCiudad.entities.User;

public record AccessSaveDto(User user, String ipAddress, String userAgent, String location, Boolean success) {

}
