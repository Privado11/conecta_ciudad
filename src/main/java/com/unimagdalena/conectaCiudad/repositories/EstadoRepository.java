package com.unimagdalena.conectaCiudad.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Estado;

public interface EstadoRepository extends JpaRepository<Estado, Long>{
    List<Estado> findByNombreContainingIgnoreCase(String nombre);
}
