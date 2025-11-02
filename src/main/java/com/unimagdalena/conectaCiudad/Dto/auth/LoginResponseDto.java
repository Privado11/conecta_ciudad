package com.unimagdalena.conectaCiudad.Dto.auth;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import lombok.Builder;

@Builder
public record LoginResponseDto(
    String token,           
    String type,            
    Long expiresIn,         
    UserDto user            
) {}