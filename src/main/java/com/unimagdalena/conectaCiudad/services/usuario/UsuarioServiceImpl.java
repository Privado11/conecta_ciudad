package com.unimagdalena.conectaCiudad.services.usuario;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.UsuarioDto;
import com.unimagdalena.conectaCiudad.Dto.UsuarioGuardarDto;
import com.unimagdalena.conectaCiudad.entities.Rol;
import com.unimagdalena.conectaCiudad.entities.Usuario;
import com.unimagdalena.conectaCiudad.repositories.RolRepository;
import com.unimagdalena.conectaCiudad.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UsuarioDto findByCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo);
        return new UsuarioDto(usuario.getId(),
                              usuario.getNombre(),
                              usuario.getCc(),
                              usuario.getCorreo(),
                              usuario.getCelular());
    }

    @Override
    public UsuarioDto findByCc(String cc) {
        Usuario usuario = usuarioRepository.findByCc(cc);
        return new UsuarioDto(usuario.getId(),
                              usuario.getNombre(),
                              usuario.getCc(),
                              usuario.getCorreo(),
                              usuario.getCelular());
    }

    @Override
    public List<UsuarioDto> findByNombreContainingIgnoreCase(String nombre) {
        List<Usuario> usuarios = usuarioRepository.findByNombreContainingIgnoreCase(nombre);
        return usuarios.stream()
                       .map(usuario -> new UsuarioDto(usuario.getId(),
                                                      usuario.getNombre(),
                                                      usuario.getCc(),
                                                      usuario.getCorreo(),
                                                      usuario.getCelular()))
                       .toList();
    }

    @Override
    public UsuarioDto saveUsuario(UsuarioGuardarDto usuario) {
        List<Rol> roles=usuario.roles().stream()
                        .map(rol->rolRepository.findByNombreContainingIgnoreCase(rol))
                        .toList();

        Usuario usuarioGuardar=Usuario.builder()
                                    .nombre(usuario.nombre())
                                    .cc(usuario.cc())
                                    .correo(usuario.correo())
                                    .password(passwordEncoder.encode(usuario.password()))
                                    .celular(usuario.celular())
                                    .roles(roles)
                                    .build();
        
        Usuario usuarioGuardado=usuarioRepository.save(usuarioGuardar);

        return new UsuarioDto(usuarioGuardado.getId(),
                               usuarioGuardado.getNombre(),
                               usuarioGuardado.getCc(),
                               usuarioGuardado.getCorreo(),
                               usuarioGuardado.getCelular());
    }
}
