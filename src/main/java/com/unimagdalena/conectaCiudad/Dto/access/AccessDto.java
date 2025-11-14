package com.unimagdalena.conectaCiudad.Dto.access;

import java.time.OffsetDateTime;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;



public record AccessDto(Long id, OffsetDateTime accessAt, UserDto user, String ipAddress, String userAgent, String location, Boolean success) {
    
}
