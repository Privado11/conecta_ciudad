package com.unimagdalena.conectaCiudad.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Usuario;
import java.util.List;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findByCorreo(String correo);
    Usuario findByCc(String cc);
    List<Usuario> findByNombreContainingIgnoreCase(String nombre);
}
