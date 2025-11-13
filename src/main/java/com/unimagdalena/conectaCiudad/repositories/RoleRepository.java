package com.unimagdalena.conectaCiudad.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNameIgnoreCase(String name);


    Role findByNameContainingIgnoreCase(String roleName);


    boolean existsByNameIgnoreCase(String name);
}
