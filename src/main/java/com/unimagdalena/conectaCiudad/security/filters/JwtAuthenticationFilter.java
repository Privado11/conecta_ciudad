package com.unimagdalena.conectaCiudad.security.filters;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.repositories.AccessRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

import static com.unimagdalena.conectaCiudad.security.TokenJwtConfig.*;

@AllArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter{

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private AccessRepository accessRepository;
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        
        User usuario=null;
        String username=null;
        String password=null;

        try{
            usuario=new ObjectMapper().readValue(request.getInputStream(), User.class);

            username=usuario.getEmail();
            password=usuario.getPassword();

        }catch(Exception e){
            e.printStackTrace();
        }

        UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(username, password);

        return authenticationManager.authenticate(authenticationToken);
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        org.springframework.security.core.userdetails.User springUser = (org.springframework.security.core.userdetails.User) authResult.getPrincipal();

        //generamos el token
        String token=Jwts.builder()
            .subject(springUser.getUsername())
            .claims(Map.of("roles", springUser.getAuthorities()))
            .signWith(SECRET_KEY)
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .issuedAt(new Date())
            .compact();

        //devolver el token al cliente 
        response.addHeader(HEADER_AUTHORIZATION, PREFIX_TOKEN + token);

        Map<String, String> json=new HashMap<>();
        json.put("token", token);
        json.put("username", springUser.getUsername());
        json.put("message", "Bienvenido " + springUser.getUsername() + ", has iniciado sesión correctamente");

        response.getWriter().write(new ObjectMapper().writeValueAsString(json));
        response.setContentType(CONTENT_TYPE);
        response.setStatus(200);

        //registramos el acceso
        accessRepository.save(Access.builder()
            .user(userRepository.findByEmail(springUser.getUsername()))
            .build());
    }


    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        Map<String, String> json=new HashMap<>();
        
        json.put("message", "Error en la autenticación: username o password incorrectos");
        json.put("error", failed.getMessage());

        response.getWriter().write(new ObjectMapper().writeValueAsString(json));
        response.setContentType(CONTENT_TYPE);
        response.setStatus(401);
    }

}
