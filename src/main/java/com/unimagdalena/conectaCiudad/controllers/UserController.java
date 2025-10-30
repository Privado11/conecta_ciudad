package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.services.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    
    @GetMapping
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nationalId) {

        if (email != null) {
            return ResponseEntity.ok(userService.findByEmail(email));
        } else if (name != null) {
            return ResponseEntity.ok(userService.findByNameContainingIgnoreCase(name));
        } else if (nationalId != null) {
            return ResponseEntity.ok(userService.findByNationalId(nationalId));
        } else {
            return ResponseEntity.ok(userService.findAll());
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserSaveDto userDto) {
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

  
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    
    @PostMapping("/{id}/roles/{role}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> addRole(@PathVariable Long id, @PathVariable String role) {
        return ResponseEntity.ok(userService.addRole(id, role));
    }

    @DeleteMapping("/{id}/roles/{role}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> removeRole(@PathVariable Long id, @PathVariable String role) {
        return ResponseEntity.ok(userService.removeRole(id, role));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createUserAdmin(@RequestBody UserSaveDto userDto) {
        return ResponseEntity.ok(userService.createUserWithRoles(userDto, userDto.roles()));
    }
}
