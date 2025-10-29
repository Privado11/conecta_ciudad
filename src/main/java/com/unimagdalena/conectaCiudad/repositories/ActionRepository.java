package com.unimagdalena.conectaCiudad.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Action;

public interface ActionRepository extends JpaRepository<Action, Long> {
    List<Action> findByUserId(Long userId);
    List<Action> findByNameContainingIgnoreCase(String name);
}


