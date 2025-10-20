package com.unimagdalena.conectaCiudad.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Accion;

public interface AccionRepository extends JpaRepository<Accion, Long>{
    List<Accion> findByUsuarioId(Long usuarioId);
    List<Accion> findByNombreContainingIgnoreCase(String nombre);
}
