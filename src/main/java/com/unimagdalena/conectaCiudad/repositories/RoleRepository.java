package com.unimagdalena.conectaCiudad.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unimagdalena.conectaCiudad.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByNameContainingIgnoreCase(String roleName);
}


