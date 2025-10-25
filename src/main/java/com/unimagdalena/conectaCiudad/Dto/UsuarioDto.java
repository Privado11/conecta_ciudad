package com.unimagdalena.conectaCiudad.Dto;

public record UsuarioDto(Long id, 
                        String nombre,
                        String cc,
                        String correo,
                        String celular) {}
