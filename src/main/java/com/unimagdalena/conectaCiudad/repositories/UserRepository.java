package com.unimagdalena.conectaCiudad.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import com.unimagdalena.conectaCiudad.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByNationalId(String nationalId);
    Optional<User> findByEmailOrNationalId(String email, String nationalId);
    List<User> findByNameContainingIgnoreCase(String name);
    List<User> findByRoles_NameIgnoreCase(String roleName);
    @Query("SELECT u FROM User u WHERE u.id <> :userId")
    Page<User> findAllExceptUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE CONCAT('%', LOWER(:name), '%') AND u.id <> :userId")
    Page<User> findByNameContainingIgnoreCaseAndIdNot(@Param("name") String name, @Param("userId") Long userId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE CONCAT('%', LOWER(:name), '%')")
    Page<User> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    
}


