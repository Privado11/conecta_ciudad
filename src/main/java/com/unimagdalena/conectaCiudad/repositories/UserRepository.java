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

    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN u.roles r " +
           "WHERE (:roleName = '' OR UPPER(r.name) = UPPER(:roleName)) " +
           "AND (:active IS NULL OR u.active = :active) " +
           "AND (:currentUserId IS NULL OR u.id <> :currentUserId)")
    Page<User> findByRoleAndActiveStatus(
        @Param("roleName") String roleName,
        @Param("active") Boolean active,
        @Param("currentUserId") Long currentUserId,
        Pageable pageable
    );
    
  
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN u.roles r " +
           "WHERE LOWER(u.name) LIKE CONCAT('%', LOWER(:name), '%') " +
           "AND (:roleName = '' OR UPPER(r.name) = UPPER(:roleName)) " +
           "AND (:active IS NULL OR u.active = :active) " +
           "AND (:currentUserId IS NULL OR u.id <> :currentUserId)")
    Page<User> findByNameAndRoleAndActiveStatus(
        @Param("name") String name,
        @Param("roleName") String roleName,
        @Param("active") Boolean active,
        @Param("currentUserId") Long currentUserId,
        Pageable pageable
    );
}