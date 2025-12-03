package com.unimagdalena.conectaCiudad.services.admin.user;

import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.exceptions.DuplicateResourceException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.RoleRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserManagementServiceImpl
 * Uses Mockito to mock dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementService Unit Tests")
class UserManagementServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    private UserSaveDto userSaveDto;
    private User user;
    private Role citizenRole;

    @BeforeEach
    void setUp() {
        userSaveDto = new UserSaveDto(
                "Walter Jiménez",
                "123456789",
                "walter.jimenez@conectaciudad.com",
                "password123",
                "300000000",
                true,
                List.of("CITIZEN")
        );

        citizenRole = Role.builder()
                .id(1L)
                .name("CITIZEN")
                .build();

        user = User.builder()
                .id(1L)
                .name("Juan Pérez")
                .email("juan.perez@example.com")
                .nationalId("123456789")
                .password("encodedPassword")
                .active(true)
                .roles(List.of(citizenRole))
                .build();
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDto result = userManagementService.createUser(userSaveDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Juan Pérez");
        assertThat(result.email()).isEqualTo("juan.perez@example.com");
        
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

   

   
    @Test
    @DisplayName("Should toggle user status from active to inactive")
    void shouldToggleUserStatusFromActiveToInactive() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDto result = userManagementService.toggleUserStatus(1L);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(argThat(u -> !u.getActive()));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userManagementService.toggleUserStatus(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("USER_NOT_FOUND");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should assign role to user successfully")
    void shouldAssignRoleToUser() {
        // Given
        Role adminRole = Role.builder()
                .id(2L)
                .name("ADMIN")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDto result = userManagementService.assignRole(1L, "ADMIN");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(argThat(u -> u.getRoles().size() == 2));
    }

    @Test
    @DisplayName("Should remove role from user successfully")
    void shouldRemoveRoleFromUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        UserDto result = userManagementService.removeRole(1L, "CIUDADANO");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(1L);
        verify(userRepository).save(argThat(u -> u.getRoles().isEmpty()));
    }
}
