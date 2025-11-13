package com.unimagdalena.conectaCiudad.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        User user = userRepository.findByEmail(username);
        
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        }

        List<GrantedAuthority> authorities = user.getRoles().stream()
    .flatMap(rol -> {
        List<SimpleGrantedAuthority> roleAuthorities = rol.getPermissions().stream()
            .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
            .collect(Collectors.toList());

        roleAuthorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getName()));
        return roleAuthorities.stream();
    })
    .collect(Collectors.toList());


        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .authorities(authorities)
            .build();
    }

    
}
