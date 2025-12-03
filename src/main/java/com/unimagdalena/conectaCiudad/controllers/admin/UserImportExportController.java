package com.unimagdalena.conectaCiudad.controllers.admin;

import com.unimagdalena.conectaCiudad.Dto.user.BulkUserImportResult;
import com.unimagdalena.conectaCiudad.Dto.user.UserDto;
import com.unimagdalena.conectaCiudad.services.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Import/Export", description = "CSV import and export operations for users")
public class UserImportExportController {

    private final AdminService adminService;

    @PreAuthorize("hasAuthority('USER_IMPORT')")
    @PostMapping("/import")
    @Operation(summary = "Import multiple users from CSV file")
    public ResponseEntity<BulkUserImportResult> importUsers(
            @Parameter(description = "CSV file with user data", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(adminService.importUsersFromCSV(file));
    }

    @PreAuthorize("hasAuthority('USER_EXPORT')")
    @GetMapping("/export")
    @Operation(summary = "Export users to CSV with filters")
    public ResponseEntity<byte[]> exportUsers(
            @Parameter(description = "Filter by name")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter by role")
            @RequestParam(required = false) String role,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "1000") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "asc") String sortDirection) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = (Long) auth.getDetails();

        List<UserDto> users;
        if (name != null) {
            users = adminService.findByNameAndFilters(
                    name, role, active, currentUserId,
                    page, size, sortBy, sortDirection
            ).page().getContent();
        } else if (role != null || active != null) {
            users = adminService.findByFilters(
                    role, active, currentUserId,
                    page, size, sortBy, sortDirection
            ).page().getContent();
        } else {
            users = adminService.findAllExceptCurrent(
                    currentUserId, page, size, sortBy, sortDirection
            ).page().getContent();
        }

        byte[] csvData = adminService.exportUsersToCSV(users);
        String filename = "users_export_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @PreAuthorize("hasAuthority('USER_EXPORT')")
    @GetMapping("/export/all")
    @Operation(summary = "Export all users to CSV")
    public ResponseEntity<byte[]> exportAllUsers() throws IOException {
        byte[] csvData = adminService.exportAllUsersToCSV();
        String filename = "all_users_export_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
