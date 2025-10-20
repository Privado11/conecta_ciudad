package com.unimagdalena.conectaCiudad.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Revision;

public interface RevisionRepository extends JpaRepository<Revision, Long> {
    List<Revision> findByFechaInicio(LocalDateTime fechaInicio);
    List<Revision> findByFechaLimite(LocalDateTime fechaLimite);
    List<Revision> findByFechaRevision(LocalDateTime fechaRevision);
    List<Revision> findByCuradorId(Long curadorId);
    List<Revision> findByProyectoId(Long proyectoId);
}
