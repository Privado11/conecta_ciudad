package com.unimagdalena.conectaCiudad.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Access;

public interface AccessRepository extends JpaRepository<Access, Long> {
    List<Access> findByUserId(Long userId);
}


