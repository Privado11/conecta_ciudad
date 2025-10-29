package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.project.ProjectSaveDto;
import com.unimagdalena.conectaCiudad.services.project.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    
    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<?> createProject(
            @RequestBody ProjectSaveDto projectSaveDto) {
        return ResponseEntity.ok(projectService.saveProject(projectSaveDto));
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }
}
