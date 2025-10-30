package com.unimagdalena.conectaCiudad.Dto.user;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserSaveDto(
    
    @NotBlank(message = "Name is required")
    String name,
    
    @NotBlank(message = "National ID is required")
    String nationalId,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    String password,
    
    String phone,

    List<String> roles
) {}
