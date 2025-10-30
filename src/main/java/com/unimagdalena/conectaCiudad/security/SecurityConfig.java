package com.unimagdalena.conectaCiudad.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.unimagdalena.conectaCiudad.repositories.AccesoRepository;
import com.unimagdalena.conectaCiudad.repositories.UsuarioRepository;
import com.unimagdalena.conectaCiudad.security.filters.JwtAuthenticationFilter;
import com.unimagdalena.conectaCiudad.security.filters.JwtValidationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarioRepository, AccesoRepository accesoRepository) throws Exception {
        return http

        .csrf(csrf -> csrf.disable())

        .cors(cors-> cors.disable())

        .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/usuarios/*").permitAll().anyRequest().authenticated())

        .addFilter(new JwtAuthenticationFilter(authenticationManager(), usuarioRepository, accesoRepository))

        .addFilterBefore(new JwtValidationFilter(authenticationManager()), JwtAuthenticationFilter.class)

        .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        .build();
    }
}
