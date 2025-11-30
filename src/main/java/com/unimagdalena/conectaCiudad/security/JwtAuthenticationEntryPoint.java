package com.unimagdalena.conectaCiudad.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        String requestUri = request.getRequestURI();
        
       
        if (requestUri.equals("/auth/login") || requestUri.equals("/auth/register")) {
            return;
        }
        
        Map<String, Object> json = new HashMap<>();
        json.put("timestamp", java.time.LocalDateTime.now().toString());
        json.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        json.put("error", "UNAUTHORIZED");
        json.put("errorCode", "ACCESS_DENIED"); 
        json.put("path", requestUri);

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(new ObjectMapper().writeValueAsString(json));
    }
}