package com.unimagdalena.conectaCiudad.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.HashMap;

import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.security.filters.JwtAuthenticationFilter;
import com.unimagdalena.conectaCiudad.security.filters.JwtAuthorizationFilter;
import com.unimagdalena.conectaCiudad.services.access.AccessService;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import java.util.Map;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final UserRepository userRepository;
    private final AccessService accessService;
    private final ApplicationEventPublisher eventPublisher;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    JwtAuthenticationFilter jwtAuthFilter = 
        new JwtAuthenticationFilter(authenticationManager(), userRepository, accessService, eventPublisher);
    jwtAuthFilter.setFilterProcessesUrl("/auth/login");
    
    
    jwtAuthFilter.setAuthenticationFailureHandler((request, response, exception) -> {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", java.time.LocalDateTime.now().toString());
        error.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        error.put("error", "INVALID_CREDENTIALS");
        error.put("errorCode", "INVALID_CREDENTIALS");
        error.put("path", "/auth/login");
        response.getWriter().write(new ObjectMapper().writeValueAsString(error));
    });

    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(management -> 
            management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/auth/**").permitAll() 
            .requestMatchers("/", "/health").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .anyRequest().authenticated() 
        )
        .anonymous(anonymous -> anonymous.disable()) 
        .addFilter(jwtAuthFilter)
        .addFilterBefore(new JwtAuthorizationFilter(userRepository), UsernamePasswordAuthenticationFilter.class)
        .build();
}

    @Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

   
    configuration.setAllowedOrigins(Arrays.asList(
        "https://participacion-ciudadana-aze8f3ezf0ene3g2.eastus2-01.azurewebsites.net", 
        "http://localhost:5173", "https://conecta-ciudad.netlify.app"
    ));

    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    ));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setExposedHeaders(Arrays.asList("Authorization")); 

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}

}
