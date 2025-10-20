package com.unimagdalena.conectaCiudad.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {
    List<Rol> findByNombreContainingIgnoreCase(String nombreRol);
}
