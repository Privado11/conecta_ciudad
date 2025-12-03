package com.unimagdalena.conectaCiudad.controllers.admin;

import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.enums.ProjectStatus;
import com.unimagdalena.conectaCiudad.services.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/admin/projects")
@RequiredArgsConstructor
@Tag(name = "Project Administration", description = "Project management operations for administrators")
public class ProjectAdminController {

    private final AdminService adminService;

    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @GetMapping("/search")
    @Operation(summary = "Advanced project search with filters")
    public ResponseEntity<Page<ProjectDto>> searchProjects(
            @Parameter(description = "Search term (name or description)")
            @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) ProjectStatus status,
            @Parameter(description = "Filter by creator ID")
            @RequestParam(required = false) Long creatorId,
            @Parameter(description = "Filter by curator ID")
            @RequestParam(required = false) Long curatorId,
            @Parameter(description = "Project start date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectStartFrom,
            @Parameter(description = "Project start date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectStartTo,
            @Parameter(description = "Project end date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectEndFrom,
            @Parameter(description = "Project end date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate projectEndTo,
            @Parameter(description = "Voting start date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate votingStartFrom,
            @Parameter(description = "Voting start date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate votingStartTo,
            @Parameter(description = "Voting end date from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate votingEndFrom,
            @Parameter(description = "Voting end date to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate votingEndTo,
            @Parameter(description = "Created from")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdFrom,
            @Parameter(description = "Created to")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdTo,
            @Parameter(description = "Page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(adminService.findWithFilters(
                searchTerm, status, creatorId, curatorId,
                projectStartFrom, projectStartTo,
                projectEndFrom, projectEndTo,
                votingStartFrom, votingStartTo,
                votingEndFrom, votingEndTo,
                createdFrom, createdTo,
                pageable
        ));
    }

    @PreAuthorize("hasAuthority('PROJECT_ASSIGN_CURATOR')")
    @PutMapping("/{id}/curator")
    @Operation(summary = "Reassign curator to project")
    public ResponseEntity<ProjectDto> reassignCurator(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "New curator ID", required = true)
            @RequestParam Long curatorId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long adminId = (Long) auth.getDetails();
        Long accessId = null;

        return ResponseEntity.ok(adminService.reassignCurator(id, curatorId, adminId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @GetMapping("/statistics")
    @Operation(summary = "Get global project statistics")
    public ResponseEntity<Statistics<ProjectDto>> getProjectStatistics() {
        return ResponseEntity.ok(adminService.getGlobalStatistics());
    }
}
