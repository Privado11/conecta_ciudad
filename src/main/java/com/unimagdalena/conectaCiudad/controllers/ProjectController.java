package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.services.project.ProjectService;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.entities.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    
    private final ProjectService projectService;
    private final UserRepository userRepository;

    @PreAuthorize("hasAuthority('LIDER_COMUNITARIO')")
    @PostMapping
    public ResponseEntity<?> createProject(@Valid
            @RequestBody ProjectSaveDto projectSaveDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User creator = userRepository.findByEmail(email);
        return ResponseEntity.ok(projectService.saveProject(projectSaveDto, creator.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @GetMapping
    public ResponseEntity<?> searchProjects(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long creatorId) {

        if (name != null) {
            return ResponseEntity.ok(projectService.findByNameContainingIgnoreCase(name));
        } else if (creatorId != null) {
            return ResponseEntity.ok(projectService.findByCreatorId(creatorId));
        } else {
            return ResponseEntity.ok(projectService.findAll());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody ProjectSaveDto projectDto) {
        return ResponseEntity.ok(projectService.updateProject(id, projectDto));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}/curator")
    public ResponseEntity<?> reassignCurator(@PathVariable Long id, @RequestParam Long curatorId) {
        return ResponseEntity.ok(projectService.reassignCurator(id, curatorId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }
}
