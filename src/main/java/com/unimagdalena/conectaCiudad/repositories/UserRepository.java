package com.unimagdalena.conectaCiudad.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.unimagdalena.conectaCiudad.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByNationalId(String nationalId);
    Optional<User> findByEmailOrNationalId(String email, String nationalId);
    List<User> findByNameContainingIgnoreCase(String name);
    List<User> findByRoles_NameIgnoreCase(String roleName);
}


