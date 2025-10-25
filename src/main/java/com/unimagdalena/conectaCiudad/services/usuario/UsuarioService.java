package com.unimagdalena.conectaCiudad.services.usuario;

import java.util.List;

import com.unimagdalena.conectaCiudad.Dto.UsuarioDto;
import com.unimagdalena.conectaCiudad.Dto.UsuarioGuardarDto;

public interface UsuarioService {
    UsuarioDto findByCorreo(String correo);
    UsuarioDto findByCc(String cc);
    List<UsuarioDto> findByNombreContainingIgnoreCase(String nombre);
    UsuarioDto saveUsuario(UsuarioGuardarDto usuario);
}
