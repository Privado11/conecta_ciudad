package com.unimagdalena.conectaCiudad.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unimagdalena.conectaCiudad.Dto.UsuarioGuardarDto;
import com.unimagdalena.conectaCiudad.services.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping("/register")
    public ResponseEntity<?> registrar(@RequestBody UsuarioGuardarDto usuarioGuardarDto) {
        return ResponseEntity.ok().body(usuarioService.saveUsuario(usuarioGuardarDto));
    }

    @GetMapping("/{nombreUsuario}")
    public ResponseEntity<?> obtenerPorNombreUsuario(@PathVariable String nombreUsuario) {
        return ResponseEntity.ok().body(usuarioService.findByNombreContainingIgnoreCase(nombreUsuario));
    }
}
