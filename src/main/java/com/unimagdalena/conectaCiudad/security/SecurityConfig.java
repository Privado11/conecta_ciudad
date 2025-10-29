package com.unimagdalena.conectaCiudad.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.unimagdalena.conectaCiudad.repositories.AccessRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.security.filters.JwtAuthenticationFilter;

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
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository userRepository, AccessRepository accessRepository) throws Exception {
        return http
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

        .addFilter(new JwtAuthenticationFilter(authenticationManager(), userRepository, accessRepository))

        .csrf(csrf -> csrf.disable())

        .cors(cors-> cors.disable())

        .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        .build();
    }
}