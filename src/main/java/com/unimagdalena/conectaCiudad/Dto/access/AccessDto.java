package com.unimagdalena.conectaCiudad.Dto.access;

import java.time.LocalDateTime;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;



public record AccessDto(Long id, LocalDateTime accessAt, UserDto user, String ipAddress, String userAgent, String location, Boolean success) {
    
}
