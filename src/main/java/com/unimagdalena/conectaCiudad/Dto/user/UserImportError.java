package com.unimagdalena.conectaCiudad.Dto.user;

public record UserImportError(int rowNumber,
                               String email,
                               String nationalId,
                               String role,
                               String errorMessage) {
    
}
