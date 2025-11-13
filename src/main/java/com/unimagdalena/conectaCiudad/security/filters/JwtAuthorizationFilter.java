package com.unimagdalena.conectaCiudad.security.filters;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import static com.unimagdalena.conectaCiudad.security.TokenJwtConfig.*;


@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public JwtAuthorizationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER_AUTHORIZATION);

        if (header == null || !header.startsWith(PREFIX_TOKEN)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = header.replace(PREFIX_TOKEN, "").trim();

            Claims claims = Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            Long accessId = claims.get("access_id", Long.class);

            if (username == null) {
                chain.doFilter(request, response);
                return;
            }

            User user = userRepository.findByEmail(username);
            
            if (user == null) {
                log.warn("Usuario no encontrado en BD: {}", username);
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuario no encontrado");
                return;
            }

            if (!user.getActive()) {
                log.warn("Usuario desactivado intentando acceder: {}", username);
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                    "Tu cuenta está desactivada. Contacta al administrador.");
                return;
            }

            Set<GrantedAuthority> authorities = buildAuthoritiesFromDatabase(user);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(user.getId());
            SecurityContextHolder.getContext().setAuthentication(auth);

            if (accessId != null) {
                request.setAttribute("currentAccessId", accessId);
            }

            log.debug("Usuario {} autenticado con {} permisos desde BD", username, authorities.size());

        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    private Set<GrantedAuthority> buildAuthoritiesFromDatabase(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        user.getRoles().forEach(role -> {
            Set<GrantedAuthority> permissions = role.getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toSet());
            authorities.addAll(permissions);
        });

        return authorities;
    }
}