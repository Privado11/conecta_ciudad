package com.unimagdalena.conectaCiudad.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Rol findByNombreContainingIgnoreCase(String nombreRol);
}
