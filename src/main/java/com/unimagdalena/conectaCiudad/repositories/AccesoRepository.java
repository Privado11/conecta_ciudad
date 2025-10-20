package com.unimagdalena.conectaCiudad.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Acceso;

public interface AccesoRepository extends JpaRepository<Acceso, Long>{
    List<Acceso> findByUsuarioId(Long usuarioId);
}
