package com.unimagdalena.conectaCiudad.Dto;

import java.util.List;

public record UsuarioGuardarDto(String nombre, 
                                String cc,
                                String correo,
                                String password,
                                String celular,
                                List<String> roles) {}
