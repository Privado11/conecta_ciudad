package com.unimagdalena.conectaCiudad.controllers.admin;

import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.user.CuratorInfoDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.services.admin.AdminService;
import com.unimagdalena.conectaCiudad.services.user.UserService;
import com.unimagdalena.conectaCiudad.validation.OnCreate;
import com.unimagdalena.conectaCiudad.validation.OnUpdate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Administration", description = "User management operations for administrators")
public class UserAdminController {

    private final AdminService adminService;
    private final UserService userService;

    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping
    @Operation(summary = "Search and list users with filters and pagination")
    public ResponseEntity<?> searchUsers(
            @Parameter(description = "Filter by name (partial search)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter by email (exact search, no pagination)")
            @RequestParam(required = false) String email,
            @Parameter(description = "Filter by national ID (exact search, no pagination)")
            @RequestParam(required = false) String nationalId,
            @Parameter(description = "Filter by role")
            @RequestParam(required = false) String role,
            @Parameter(description = "Filter by status: true (active) or false (inactive)")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number (starts at 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction: 'asc' or 'desc'")
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = (Long) auth.getDetails();

        if (email != null) {
            return ResponseEntity.ok(userService.findByEmail(email));
        }

        if (nationalId != null) {
            return ResponseEntity.ok(userService.findByNationalId(nationalId));
        }

        PagedResponse<UserDto> response;

        if (name != null) {
            response = adminService.findByNameAndFilters(
                    name, role, active, currentUserId,
                    page, size, sortBy, sortDirection
            );
        } else if (role != null || active != null) {
            response = adminService.findByFilters(
                    role, active, currentUserId,
                    page, size, sortBy, sortDirection
            );
        } else {
            response = adminService.findAllExceptCurrent(
                    currentUserId, page, size, sortBy, sortDirection
            );
        }

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('USER_CREATE')")
    @PostMapping
    @Operation(summary = "Create new user with assigned roles")
    public ResponseEntity<UserDto> createUser(
            @Validated(OnCreate.class) @RequestBody UserSaveDto userDto) {
        return ResponseEntity.ok(adminService.createUser(userDto));
    }

    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @PutMapping("/{id}")
    @Operation(summary = "Update existing user data")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody UserSaveDto userDto) {
        return ResponseEntity.ok(adminService.updateUser(id, userDto));
    }

    @PreAuthorize("hasAuthority('USER_DELETE')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user from system")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PreAuthorize("hasAuthority('USER_TOGGLE_STATUS')")
    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle user status (activate/deactivate)")
    public ResponseEntity<UserDto> toggleUserStatus(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(adminService.toggleUserStatus(id));
    }

    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    @PostMapping("/{id}/roles/{role}")
    @Operation(summary = "Assign role to user")
    public ResponseEntity<UserDto> addRole(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Role name (ADMIN, CURATOR, LIDER_COMUNITARIO)", required = true)
            @PathVariable String role) {
        return ResponseEntity.ok(adminService.assignRole(id, role));
    }

    @PreAuthorize("hasAuthority('USER_ROLE_REMOVE')")
    @DeleteMapping("/{id}/roles/{role}")
    @Operation(summary = "Remove role from user")
    public ResponseEntity<UserDto> removeRole(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Role name", required = true)
            @PathVariable String role) {
        return ResponseEntity.ok(adminService.removeRole(id, role));
    }

    @PreAuthorize("hasAuthority('USER_VIEW')")
    @GetMapping("/curators/{projectId}")
    @Operation(summary = "Get curators with statistics for project assignment")
    public ResponseEntity<CuratorInfoDto> getCuratorsWithStats(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(adminService.findAllCuratorsWithStats(projectId));
    }
}
