package com.unimagdalena.conectaCiudad.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Status;

public interface StatusRepository extends JpaRepository<Status, Long> {
    List<Status> findByNameContainingIgnoreCase(String name);
}


