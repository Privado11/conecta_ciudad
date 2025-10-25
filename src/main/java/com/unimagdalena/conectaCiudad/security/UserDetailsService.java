package com.unimagdalena.conectaCiudad.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.entities.Usuario;
import com.unimagdalena.conectaCiudad.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        Usuario usuario=usuarioRepository.findByCorreo(username);

        List<GrantedAuthority> authorities=usuario.getRoles().stream()
            .map(rol->new SimpleGrantedAuthority(rol.getNombre())).collect(Collectors.toList());

        return User.builder()
            .username(usuario.getCorreo())
            .password(usuario.getPassword())
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .authorities(authorities)
            .build();
    }

    
}
