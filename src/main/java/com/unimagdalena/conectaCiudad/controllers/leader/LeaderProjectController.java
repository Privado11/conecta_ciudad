package com.unimagdalena.conectaCiudad.controllers.leader;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectDto;
import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.services.leader.LeaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leader/projects")
@RequiredArgsConstructor
@Tag(name = "Leader Projects", description = "Project management operations for community leaders")
public class LeaderProjectController {

    private final LeaderService leaderService;

    @PreAuthorize("hasAuthority('PROJECT_VIEW')")
    @GetMapping
    @Operation(summary = "Get all my projects")
    public ResponseEntity<List<ProjectDto>> getMyProjects(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        return ResponseEntity.ok(leaderService.getMyProjects(creatorId));
    }

    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @PostMapping
    @Operation(summary = "Create new community project")
    public ResponseEntity<ProjectDto> createProject(
            @Valid @RequestBody ProjectSaveDto projectSaveDto,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(leaderService.createProject(projectSaveDto, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    @PutMapping("/{id}")
    @Operation(summary = "Update project data")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectSaveDto projectDto,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(leaderService.updateProject(id, projectDto, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    @PutMapping("/{id}/submit")
    @Operation(summary = "Submit project for review")
    public ResponseEntity<ProjectDto> submitForReview(
            @PathVariable Long id,
            HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long creatorId = (Long) auth.getDetails();
        Long accessId = (Long) request.getAttribute("currentAccessId");

        return ResponseEntity.ok(leaderService.submitForReview(id, creatorId, accessId));
    }

    @PreAuthorize("hasAuthority('PROJECT_DELETE')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project from system")
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        leaderService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }
}
