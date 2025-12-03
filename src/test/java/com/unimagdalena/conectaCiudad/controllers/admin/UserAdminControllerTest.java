package com.unimagdalena.conectaCiudad.controllers.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.services.admin.AdminService;
import com.unimagdalena.conectaCiudad.services.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(UserAdminController.class)
@DisplayName("UserAdminController Integration Tests")
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private UserService userService;

    private UserDto userDto;
    private UserSaveDto userSaveDto;

    @BeforeEach
    void setUp() {
        userDto = new UserDto(
                1L,
                "Walter Jiménez",
                "1234567",
                "walter@conectaciudad.com",
                "3120234324",
                OffsetDateTime.now(),
                List.of("CITIZEN"),
                true,
                OffsetDateTime.now()
        );

        userSaveDto = new UserSaveDto(
                "Walter Jiménez",
                "1234567",
                "walter@conectaciudad.com",
                "password123",
                "3120234324",
                true,
                List.of("CITIZEN")
        );
    }

    @Test
    @WithMockUser(authorities = "USER_VIEW")
    @DisplayName("Should return users list when authenticated")
    void shouldReturnUsersListWhenAuthenticated() throws Exception {
        // Given
        PagedResponse<UserDto> response = new PagedResponse<>(
                new PageImpl<>(List.of(userDto)),
                null
        );
        when(adminService.findAllExceptCurrent(anyLong(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(response);

        // When/Then
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.content[0].name").value("Walter Jiménez"))
                .andExpect(jsonPath("$.page.content[0].email").value("walter@conectaciudad.com"));

        verify(adminService).findAllExceptCurrent(anyLong(), eq(0), eq(10), eq("name"), eq("asc"));
    }

    @Test
    @WithMockUser(authorities = "USER_CREATE")
    @DisplayName("Should create user with valid data")
    void shouldCreateUserWithValidData() throws Exception {
        // Given
        when(adminService.createUser(any(UserSaveDto.class))).thenReturn(userDto);

        // When/Then
        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userSaveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Walter Jiménez"))
                .andExpect(jsonPath("$.email").value("walter@conectaciudad.com"));

        verify(adminService).createUser(any(UserSaveDto.class));
    }

    @Test
    @WithMockUser(authorities = "USER_CREATE")
    @DisplayName("Should return 400 when creating user with invalid data")
    void shouldReturn400WhenCreatingUserWithInvalidData() throws Exception {
        // Given
        UserSaveDto invalidDto = new UserSaveDto(
                "", 
                "invalid-email", 
                "123",
                "pass",
                "3120234324",
                true,
                List.of("CITIZEN")
        );

        // When/Then
        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(adminService, never()).createUser(any(UserSaveDto.class));
    }

    @Test
    @DisplayName("Should return 403 when accessing without authentication")
    void shouldReturn403WhenAccessingWithoutAuthentication() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(adminService, never()).findAllExceptCurrent(anyLong(), anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    @WithMockUser(authorities = "USER_UPDATE")
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() throws Exception {
        // Given
        when(adminService.updateUser(eq(1L), any(UserSaveDto.class))).thenReturn(userDto);

        // When/Then
        mockMvc.perform(put("/api/v1/admin/users/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userSaveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Walter Jiménez"));

        verify(adminService).updateUser(eq(1L), any(UserSaveDto.class));
    }

    @Test
    @WithMockUser(authorities = "USER_DELETE")
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() throws Exception {
        // Given
        doNothing().when(adminService).deleteUser(1L);

        // When/Then
        mockMvc.perform(delete("/api/v1/admin/users/1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(adminService).deleteUser(1L);
    }

    @Test
    @WithMockUser(authorities = "USER_TOGGLE_STATUS")
    @DisplayName("Should toggle user status successfully")
    void shouldToggleUserStatusSuccessfully() throws Exception {
        // Given
        UserDto inactiveUser = new UserDto(
                1L,
                "Walter Jiménez",
                "1234567",
                "walter@conectaciudad.com",
                "3120234324",
                OffsetDateTime.now(),
                List.of("CITIZEN"),
                false,
                OffsetDateTime.now()
               
        );
        when(adminService.toggleUserStatus(1L)).thenReturn(inactiveUser);

        // When/Then
        mockMvc.perform(patch("/api/v1/admin/users/1/toggle-status")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(adminService).toggleUserStatus(1L);
    }

    @Test
    @WithMockUser(authorities = "USER_ROLE_ASSIGN")
    @DisplayName("Should assign role to user successfully")
    void shouldAssignRoleToUserSuccessfully() throws Exception {
        // Given
        UserDto userWithNewRole = new UserDto(
                1L,
                "Walter Jiménez",
                "1234567",
                "walter@conectaciudad.com",
                "3120234324",
                OffsetDateTime.now(),
                List.of("ADMIN"),
                true,
                OffsetDateTime.now()
        );
        when(adminService.assignRole(1L, "ADMIN")).thenReturn(userWithNewRole);

        // When/Then
        mockMvc.perform(post("/api/v1/admin/users/1/roles/ADMIN")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());

        verify(adminService).assignRole(1L, "ADMIN");
    }
}
