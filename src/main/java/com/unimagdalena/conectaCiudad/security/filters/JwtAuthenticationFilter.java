package com.unimagdalena.conectaCiudad.security.filters;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
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
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimagdalena.conectaCiudad.Dto.access.AccessDto;
import com.unimagdalena.conectaCiudad.Dto.access.AccessMapper;
import com.unimagdalena.conectaCiudad.Dto.access.AccessSaveDto;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.access.AccessService;
import com.unimagdalena.conectaCiudad.events.UserLoggedInEvent;
import org.springframework.context.ApplicationEventPublisher;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.unimagdalena.conectaCiudad.security.TokenJwtConfig.*;

@Slf4j
@AllArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AccessService accessService;
    private final ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        try {
            User user = objectMapper.readValue(request.getInputStream(), User.class);
            String username = user.getEmail();
            String password = user.getPassword();

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            throw new RuntimeException("Error al leer credenciales: " + e.getMessage(), e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {

        org.springframework.security.core.userdetails.User springUser =
                (org.springframework.security.core.userdetails.User) authResult.getPrincipal();

        User userEntity = userRepository.findByEmail(springUser.getUsername());
        if (userEntity == null) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.USER_NOT_FOUND);
            return;
        }

        if (!userEntity.getActive()) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.USER_INACTIVE);
            return;
        }

        String ipAddress = getIp(request);
        String userAgent = request.getHeader("User-Agent");
        String location = getLocationFromIp(ipAddress);

        if (location == null) {
            location = "Unknown";
        }

        AccessDto accessDto = accessService.save(
                new AccessSaveDto(userEntity.getId(), ipAddress, userAgent, location, true)
        );
        Access access = accessService.findById(accessDto.id());

        
        eventPublisher.publishEvent(new UserLoggedInEvent(this, userEntity, access));

        List<String> roles = userEntity.getRoles()
                .stream()
                .map(r -> r.getName())
                .toList();

        List<String> authorities = springUser.getAuthorities().stream()
        .map(a -> a.getAuthority())
        .toList();
        String token = Jwts.builder()
        .subject(springUser.getUsername())
        .claims(Map.of(
            "roles", roles,
            "authorities", authorities,  
            "access_id", accessDto.id(),
            "user_id", userEntity.getId()
        ))
        .signWith(SECRET_KEY)
        .expiration(new Date(System.currentTimeMillis() + 3600000))
        .issuedAt(new Date())
        .compact();

        response.addHeader(HEADER_AUTHORIZATION, PREFIX_TOKEN + token);

        Map<String, Object> json = new HashMap<>();
        json.put("token", token);
        json.put("user", Map.of(
                "id", userEntity.getId(),
                "name", userEntity.getName(),
                "email", userEntity.getEmail(),
                "roles", roles, 
                "authorities", authorities 
        ));
        // message removed for i18n compliance, frontend should handle success feedback

        response.setContentType(CONTENT_TYPE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(json));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed)
            throws IOException, ServletException {

        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS);

        try {
           
        } catch (Exception e) {
            
        }
    }

    private void writeErrorResponse(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("status", status);
        errorResponse.put("error", errorCode.name()); 
        errorResponse.put("errorCode", errorCode.getCode());
        errorResponse.put("path", "/auth/login"); 
        
        response.setContentType(CONTENT_TYPE);
        response.setStatus(status);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }


    private String getIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR"
        };
    
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                ip = sanitizeIp(ip);
                return ip;
            }
        }
        
        String remoteAddr = request.getRemoteAddr();
        return sanitizeIp(remoteAddr);
    }
    
    private String sanitizeIp(String ip) {
        if (ip == null) return null;
        
        ip = ip.trim();
        
        if (ip.startsWith("[") && ip.contains("]")) {
            int closing = ip.indexOf("]");
            return ip.substring(1, closing);
        }
        
        if (ip.contains(":") && ip.chars().filter(ch -> ch == '.').count() == 3) {
            return ip.substring(0, ip.indexOf(":"));
        }
        
        return ip;
    }
    private String getLocationFromIp(String ipAddress) {
        if (ipAddress == null ||
            ipAddress.equals("127.0.0.1") ||
            ipAddress.equals("::1") ||
            ipAddress.equals("0:0:0:0:0:0:0:1") ||  
            ipAddress.startsWith("192.168.") ||
            ipAddress.startsWith("10.") ||
            ipAddress.startsWith("172.")) {
            return "Local";
        }
    
        try {
            String apiUrl = "http://ip-api.com/json/" + ipAddress;
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(apiUrl, String.class);
    
            JsonNode json = objectMapper.readTree(response);
            if ("success".equals(json.get("status").asText())) {
                String city = json.get("city").asText();
                String country = json.get("country").asText();
                return city + ", " + country;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
}
