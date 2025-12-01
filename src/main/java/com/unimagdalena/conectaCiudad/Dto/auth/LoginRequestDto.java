package com.unimagdalena.conectaCiudad.Dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoginRequestDto(
    @NotBlank
    @Email
    String email,
    
    @NotBlank
    String password
) {}
