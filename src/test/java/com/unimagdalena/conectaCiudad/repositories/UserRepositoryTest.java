package com.unimagdalena.conectaCiudad.repositories;

import com.unimagdalena.conectaCiudad.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Walter Jiménez")
                .email("walter.jimenez@conectaciudad.com")
                .nationalId("123456789")
                .password("encodedPassword")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        User found = userRepository.findByEmail("walter.jimenez@conectaciudad.com");

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Walter Jiménez");
        assertThat(found.getEmail()).isEqualTo("walter.jimenez@conectaciudad.com");
    }

    @Test
    @DisplayName("Should return empty when user email not found")
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        User found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("Should find user by national ID")
    void shouldFindUserByNationalId() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        User found = userRepository.findByNationalId("123456789");

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getNationalId()).isEqualTo("123456789");
    }

}
