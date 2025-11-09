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

    @Query("SELECT u FROM User u WHERE u.email IN :emails OR u.nationalId IN :nationalIds")
    List<User> findByEmailInOrNationalIdIn(
        @Param("emails") List<String> emails, 
        @Param("nationalIds") List<String> nationalIds
    );

    long countByActive(Boolean active);
    
    @Query("SELECT COUNT(u) FROM User u WHERE :currentUserId IS NULL OR u.id != :currentUserId")
    long countExcludingUser(@Param("currentUserId") Long currentUserId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = :active AND (:currentUserId IS NULL OR u.id != :currentUserId)")
    long countByActiveExcludingUser(@Param("active") Boolean active, @Param("currentUserId") Long currentUserId);
    
    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.roles r " +
           "WHERE (:roleName IS NULL OR :roleName = '' OR UPPER(r.name) = UPPER(:roleName)) " +
           "AND (:currentUserId IS NULL OR u.id != :currentUserId)")
    long countByRoleAndCurrentUser(@Param("roleName") String roleName, 
                                    @Param("currentUserId") Long currentUserId);
    
 
    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.roles r " +
           "WHERE (:roleName IS NULL OR :roleName = '' OR UPPER(r.name) = UPPER(:roleName)) " +
           "AND u.active = :active " +
           "AND (:currentUserId IS NULL OR u.id != :currentUserId)")
    long countByRoleAndActiveAndCurrentUser(@Param("roleName") String roleName,
                                             @Param("active") Boolean active,
                                             @Param("currentUserId") Long currentUserId);
    
   
    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.roles r " +
           "WHERE (:name IS NULL OR UPPER(u.name) LIKE UPPER(CONCAT('%', :name, '%'))) " +
           "AND (:roleName IS NULL OR :roleName = '' OR UPPER(r.name) = UPPER(:roleName)) " +
           "AND (:currentUserId IS NULL OR u.id != :currentUserId)")
    long countByNameAndRoleAndCurrentUser(@Param("name") String name,
                                          @Param("roleName") String roleName,
                                          @Param("currentUserId") Long currentUserId);
    
  
    @Query("SELECT COUNT(u) FROM User u LEFT JOIN u.roles r " +
           "WHERE (:name IS NULL OR UPPER(u.name) LIKE UPPER(CONCAT('%', :name, '%'))) " +
           "AND (:roleName IS NULL OR :roleName = '' OR UPPER(r.name) = UPPER(:roleName)) " +
           "AND u.active = :active " +
           "AND (:currentUserId IS NULL OR u.id != :currentUserId)")
    long countByNameAndRoleAndActiveAndCurrentUser(@Param("name") String name,
                                                     @Param("roleName") String roleName,
                                                     @Param("active") Boolean active,
                                                     @Param("currentUserId") Long currentUserId);
    
   
    @Query("SELECT COUNT(u) FROM User u " +
           "WHERE UPPER(u.name) LIKE UPPER(CONCAT('%', :name, '%')) " +
           "AND (:currentUserId IS NULL OR u.id != :currentUserId)")
    long countByNameAndCurrentUser(@Param("name") String name, 
                                    @Param("currentUserId") Long currentUserId);

    @Query("SELECT COUNT(u) FROM User u " +
           "WHERE UPPER(u.name) LIKE UPPER(CONCAT('%', :name, '%')) " +
           "AND u.active = :active " +
           "AND (:currentUserId IS NULL OR u.id != :currentUserId)")
    long countByNameAndActiveAndCurrentUser(@Param("name") String name,
                                            @Param("active") Boolean active,
                                            @Param("currentUserId") Long currentUserId);

}