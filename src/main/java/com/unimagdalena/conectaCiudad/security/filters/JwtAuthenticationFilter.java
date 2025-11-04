package com.unimagdalena.conectaCiudad.security.filters;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimagdalena.conectaCiudad.Dto.access.AccessDto;
import com.unimagdalena.conectaCiudad.Dto.access.AccessSaveDto;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.access.AccessService;

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
    private AccessService accessService;
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        
        User user=null;
        String username=null;
        String password=null;

        try{
            user=new ObjectMapper().readValue(request.getInputStream(), User.class);

            username=user.getEmail();
            password=user.getPassword();

        }catch(Exception e){
            e.printStackTrace();
        }

        UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(username, password);

        return authenticationManager.authenticate(authenticationToken);
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        org.springframework.security.core.userdetails.User springUser = (org.springframework.security.core.userdetails.User) authResult.getPrincipal();

        User userEntity = userRepository.findByEmail(springUser.getUsername());

    

        if (userEntity == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuario no encontrado");
            return;
        }

        String ipAddress = getIp(request);
        String userAgent = request.getHeader("User-Agent");
        String location = request.getHeader("Location");
        

         AccessDto accessDto = accessService.save(
            new AccessSaveDto(userEntity, ipAddress, userAgent, location, true)
        );

        List<String> roleNames = springUser.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .toList();

        String token=Jwts.builder()
            .subject(springUser.getUsername())
            .claims(Map.of("roles", roleNames, "id", userEntity.getId(), "access_id", accessDto.id()))
            .signWith(SECRET_KEY)
            .expiration(new Date(System.currentTimeMillis() + 3600000))
            .issuedAt(new Date())
            .compact();

        
        response.addHeader(HEADER_AUTHORIZATION, PREFIX_TOKEN + token);

        Map<String, String> json=new HashMap<>();
        json.put("token", token);
        json.put("id", userEntity.getId().toString());
        json.put("username", springUser.getUsername());
        json.put("message", "Bienvenido " + springUser.getUsername() + ", has iniciado sesión correctamente");

        response.getWriter().write(new ObjectMapper().writeValueAsString(json));
        response.setContentType(CONTENT_TYPE);
        response.setStatus(200);



    }


    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        Map<String, String> json=new HashMap<>();
        
        json.put("message", "Error en la autenticación: username o password incorrectos");
        json.put("error", failed.getMessage());

        response.getWriter().write(new ObjectMapper().writeValueAsString(json));
        response.setContentType(CONTENT_TYPE);
        response.setStatus(401);

        
 
    try {
        String ipAddress = getIp(request);
        String userAgent = request.getHeader("User-Agent");
        
     
        String body = request.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
        User user = new ObjectMapper().readValue(body, User.class);
        User userEntity = userRepository.findByEmail(user.getEmail());
        
        if (userEntity != null) {
            accessService.save(new AccessSaveDto(
                userEntity, 
                ipAddress, 
                userAgent, 
                null, 
                false 
            ));
        }
    } catch (Exception e) {
        
    }
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
       
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

}
