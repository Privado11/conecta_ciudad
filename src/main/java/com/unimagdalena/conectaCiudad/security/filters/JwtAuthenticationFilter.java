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
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimagdalena.conectaCiudad.Dto.access.AccessDto;
import com.unimagdalena.conectaCiudad.Dto.access.AccessMapper;
import com.unimagdalena.conectaCiudad.Dto.access.AccessSaveDto;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.UserActionType;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.services.access.AccessService;
import com.unimagdalena.conectaCiudad.services.action.AuditHelper;

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
    private final AccessMapper accessMapper;
    private final AuditHelper auditHelper;

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
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuario no encontrado");
            return;
        }

        if (!userEntity.getActive()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Tu cuenta está desactivada. Contacta al administrador.");
            return;
        }

        String ipAddress = getIp(request);
        String userAgent = request.getHeader("User-Agent");
        String location = getLocationFromIp(ipAddress);

        AccessDto accessDto = accessService.save(
                new AccessSaveDto(userEntity, ipAddress, userAgent, location, true)
        );
        Access access = accessMapper.toEntity(accessDto);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ipAddress", ipAddress);
        metadata.put("location", location);
        metadata.put("userAgent", userAgent);

        try {
            auditHelper.logCompleteWithAccess(
                UserActionType.USER_LOGIN.name(),
                "Inicio de sesión exitoso",
                EntityType.USER,
                userEntity.getId(),
                ActionResult.SUCCESS,
                metadata,
                access  
            );
        } catch (Exception e) {
            log.error("Error al registrar login exitoso: {}", e.getMessage(), e);
        }

        List<String> roleNames = springUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();
        String token = Jwts.builder()
                .subject(springUser.getUsername())
                .claims(Map.of(
                        "roles", roleNames,
                        "id", userEntity.getId(),
                        "access_id", accessDto.id()
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
                "roles", roleNames
        ));
        json.put("message", "Bienvenido " + userEntity.getName() + ", has iniciado sesión correctamente");

        response.setContentType(CONTENT_TYPE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(json));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed)
            throws IOException, ServletException {

        Map<String, String> json = new HashMap<>();
        json.put("message", "Correo o contraseña incorrectos");
        json.put("error", failed.getMessage());

        response.setContentType(CONTENT_TYPE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(json));

        try {
            String ipAddress = getIp(request);
            String userAgent = request.getHeader("User-Agent");

            String body = request.getReader().lines().reduce("", (acc, line) -> acc + line);
            User user = objectMapper.readValue(body, User.class);
            User userEntity = userRepository.findByEmail(user.getEmail());

            if (userEntity != null) {
                accessService.save(new AccessSaveDto(
                        userEntity,
                        ipAddress,
                        userAgent,
                        null,
                        false
                ));

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("ipAddress", ipAddress);
                metadata.put("userAgent", userAgent);
                metadata.put("reason", failed.getMessage());

                Access failedAccess = accessMapper.toEntity(
                        accessService.save(new AccessSaveDto(userEntity, ipAddress, userAgent, null, false))
                );

                auditHelper.logCompleteWithAccess(
                        UserActionType.USER_LOGIN_FAILED.name(),
                        "Intento de inicio de sesión fallido",
                        EntityType.USER,
                        userEntity.getId(),
                        ActionResult.FAILED,
                        metadata,
                        failedAccess
                );
            }
        } catch (Exception e) {
        }
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
                return ip;
            }
        }
        return request.getRemoteAddr();
    }

    private String getLocationFromIp(String ipAddress) {
        if (ipAddress == null || ipAddress.equals("127.0.0.1") ||
            ipAddress.equals("::1") || ipAddress.startsWith("192.168.") ||
            ipAddress.startsWith("10.") || ipAddress.startsWith("172.")) {
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
