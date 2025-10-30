package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.unimagdalena.conectaCiudad.Dto.user.UserSaveDto;
import com.unimagdalena.conectaCiudad.services.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserSaveDto userSaveDto) {
        return ResponseEntity.ok(userService.saveUser(userSaveDto));
    }

    
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
}
