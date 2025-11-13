package com.unimagdalena.conectaCiudad.repositories;

import com.unimagdalena.conectaCiudad.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
    List<Permission> findAll();
}
