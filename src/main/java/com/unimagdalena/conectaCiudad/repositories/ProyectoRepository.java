package com.unimagdalena.conectaCiudad.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Proyecto;
import java.time.LocalDateTime;


public interface ProyectoRepository extends JpaRepository<Proyecto, Long>{
    List<Proyecto> findByNombreContainingIgnoreCase(String nombre);
    List<Proyecto> findByPoblacionesBeneficiadasContainingIgnoreCase(String poblacionesBeneficiadas);
    List<Proyecto> findByEstadoId(Long estadoId);
    List<Proyecto> findByCreadorId(Long creadorId);
    List<Proyecto> findByFechaInicio(LocalDateTime fechaInicio);
    List<Proyecto> findByFechaFinalizacion(LocalDateTime fechaFin);
}
