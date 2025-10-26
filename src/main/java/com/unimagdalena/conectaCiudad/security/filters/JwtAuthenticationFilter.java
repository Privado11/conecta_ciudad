package com.unimagdalena.conectaCiudad.security.filters;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimagdalena.conectaCiudad.entities.Acceso;
import com.unimagdalena.conectaCiudad.entities.Usuario;
import com.unimagdalena.conectaCiudad.repositories.AccesoRepository;
import com.unimagdalena.conectaCiudad.repositories.UsuarioRepository;

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
    private UsuarioRepository usuarioRepository;
    private AccesoRepository accesoRepository;
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        
        Usuario usuario=null;
        String username=null;
        String password=null;

        try{
            usuario=new ObjectMapper().readValue(request.getInputStream(), Usuario.class);

            username=usuario.getCorreo();
            password=usuario.getPassword();

        }catch(Exception e){
            e.printStackTrace();
        }

        UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(username, password);

        return authenticationManager.authenticate(authenticationToken);
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        User user=(org.springframework.security.core.userdetails.User) authResult.getPrincipal();

        //generamos el token
        String token=Jwts.builder()
            .subject(user.getUsername())
            .claims(Map.of("roles",user.getAuthorities()))
            .signWith(SECRET_KEY)
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .issuedAt(new Date())
            .compact();

        //devolver el token al cliente 
        response.addHeader(HEADER_AUTHORIZATION, PREFIX_TOKEN + token);

        Map<String, String> json=new HashMap<>();
        json.put("token",token);
        json.put("username", user.getUsername());
        json.put("message", "Bienvenido " + user.getUsername() + ", has iniciado sesión correctamente");

        response.getWriter().write(new ObjectMapper().writeValueAsString(json));
        response.setContentType(CONTENT_TYPE);
        response.setStatus(200);

        //registramos el acceso
        accesoRepository.save(Acceso.builder()
        .usuario(usuarioRepository.findByCorreo(user.getUsername()))
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
