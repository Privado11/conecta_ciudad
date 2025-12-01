package com.unimagdalena.conectaCiudad.services.action;

import com.unimagdalena.conectaCiudad.Dto.action.*;
import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.Action;
import com.unimagdalena.conectaCiudad.entities.Role;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.enums.ErrorCode;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ActionRepository;
import com.unimagdalena.conectaCiudad.repositories.ProjectRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.specifications.ActionSpecifications;
import com.unimagdalena.conectaCiudad.repositories.AccessRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class ActionServiceImpl implements ActionService {

    private final ActionRepository actionRepository;
    private final ActionMapper actionMapper;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;
    private final UserRepository userRepository;
    private final AccessRepository accessRepository;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public ActionDto save(ActionSaveDto actionSaveDto) {
        Action action = actionMapper.toEntity(actionSaveDto);
        enrichActionWithRequestData(action);
        return actionMapper.toDto(actionRepository.save(action));
    }

    @Override
    public OffsetDateTime getLastActionDateByUserId(Long userId) {
        return actionRepository.findLastActionDateByUserId(userId);
    }

    @Override
    @Transactional
    public void logAction(String actionType, String description, User user, Access access) {
       
        ActionSaveDto dto = new ActionSaveDto(actionType, description, user, access);
        save(dto);
    }

    @Override
    @Transactional
    public ActionDto logActionWithDetails(ActionLogRequest request) {
        if (request.userId() == null) {
            throw new BadRequestException(
                    ErrorCode.REQUIRED_FIELD,
                    Map.of("field", "userId")
            );
        }

        try {

             User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.userId()));
        
        Access access = null;
        if (request.accessId() != null) {
            access = accessRepository.findById(request.accessId()).orElse(null);
        }
        
            Action action = Action.builder()
                .actionType(request.actionType())
                .description(request.description())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .result(request.result() != null ? request.result() : ActionResult.SUCCESS)
                .metadata(serializeMetadata(request.metadata()))
                .user(user)
                .access(access)
                .ipAddress(request.ipAddress())
                .userAgent(request.userAgent())
                .build();

            enrichActionWithRequestData(action);
            
            Action savedAction = actionRepository.save(action);
            return actionMapper.toDto(savedAction);
            
        } catch (Exception e) {
            throw new BadRequestException(ErrorCode.OPERATION_FAILED);
        }
    }

    @Override
    public PagedResponse<ActionDto> searchWithFilters(
            String actionType,
            ActionResult result,
            EntityType entityType,
            String searchTerm,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Pageable pageable
    ) {
        String normalizedSearchTerm = (searchTerm != null && !searchTerm.trim().isEmpty())
                ? searchTerm.trim()
                : null;

        Specification<Action> spec = ActionSpecifications.withFilters(
            actionType, 
            result, 
            entityType, 
            normalizedSearchTerm, 
            startDate, 
            endDate
        );

        Page<ActionDto> page = actionRepository
                .findAll(spec, pageable)
                .map(actionMapper::toDto);

        long totalCount = actionRepository.count(spec);

        OffsetDateTime startOfDay = OffsetDateTime.now()
            .withHour(0).withMinute(0).withSecond(0).withNano(0);

        long successfulCount = actionRepository.count(
            ActionSpecifications.withFilters(
                actionType, ActionResult.SUCCESS, entityType, 
                normalizedSearchTerm, startDate, endDate
            )
        );
        
        long failedCount = actionRepository.count(
            ActionSpecifications.withFilters(
                actionType, ActionResult.FAILED, entityType, 
                normalizedSearchTerm, startDate, endDate
            )
        );
        
        long todayCount = actionRepository.count(
            ActionSpecifications.withFilters(
                actionType, result, entityType, 
                normalizedSearchTerm, startOfDay, OffsetDateTime.now()
            )
        );

        Statistics<ActionDto> stats = new Statistics<>(
                totalCount,
                Map.of(
                        "successful", successfulCount,
                        "failed", failedCount,
                        "today", todayCount,
                        "hasFilters", (actionType != null || result != null ||
                                entityType != null || normalizedSearchTerm != null ||
                                startDate != null || endDate != null)
                )
        );

        return new PagedResponse<>(page, stats);
    }

    @Override
    public Map<String, Object> getActionDetails(Long actionId) {
        Action action = actionRepository.findById(actionId)
            .orElseThrow(() -> new ResourceNotFoundException("Action", "id", actionId));
    
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("id", action.getId());
        details.put("actionType", action.getActionType());
        details.put("description", action.getDescription());
        details.put("result", action.getResult());
        details.put("entityType", action.getEntityType());
        details.put("actionAt", action.getActionAt());
        details.put("ipAddress", action.getIpAddress());
        details.put("user", action.getUser() != null ? Map.of(
            "id", action.getUser().getId(),
            "name", action.getUser().getName(),
            "email", action.getUser().getEmail()
        ) : null);
    
        Map<String, Object> metadata = null;
        if (action.getMetadata() != null) {
            try {
                metadata = objectMapper.readValue(action.getMetadata(), Map.class);
                details.put("metadata", metadata);
            } catch (Exception e) {
                details.put("metadata", Map.of("error", "No se pudo leer metadata"));
            }
        }
    
        if (action.getEntityId() == null) {
            details.put("entityData", Map.of("info", "Esta acción no tiene entidad asociada"));
        } else {
            switch (action.getEntityType()) {
                case PROJECT -> projectRepository.findById(action.getEntityId()).ifPresent(project -> {
                    Map<String, Object> projectData = new LinkedHashMap<>();
                    projectData.put("id", project.getId());
                    projectData.put("name", project.getName());
                    projectData.put("status", project.getStatus().name());
                    projectData.put("creator", project.getCreator().getName());
                    projectData.put("budget", project.getBudget());
                    projectData.put("startAt", project.getStartAt());
                    projectData.put("endAt", project.getEndAt());
                    details.put("entityData", projectData);
                });
                case USER -> userRepository.findById(action.getEntityId()).ifPresent(user -> {
                    Map<String, Object> userData = new LinkedHashMap<>();
                    userData.put("id", user.getId());
                    userData.put("name", user.getName());
                    userData.put("email", user.getEmail());
                    userData.put("roles", user.getRoles().stream().map(Role::getName).toList());
                    userData.put("active", user.getActive());
                    details.put("entityData", userData);
                });
                case ACCESS -> accessRepository.findById(action.getEntityId()).ifPresent(access -> {
                    Map<String, Object> accessData = new LinkedHashMap<>();
                    accessData.put("ipAddress", access.getIpAddress());
                    accessData.put("location", access.getLocation());
                    accessData.put("userAgent", access.getUserAgent());
                    accessData.put("accessAt", access.getAccessAt());
                    details.put("entityData", accessData);
                });
                default -> details.put("entityData", Map.of("info", "Sin datos adicionales"));
            }
        }
    
        if (metadata != null) {
            Map<String, Object> changes = new LinkedHashMap<>();
            metadata.forEach((k, v) -> {
                if (k.toLowerCase().startsWith("old") || k.toLowerCase().startsWith("new")) {
                    changes.put(k, v);
                }
            });
            if (!changes.isEmpty()) details.put("changes", changes);
        }
    
        return details;
    }
    

    private void enrichActionWithRequestData(Action action) {
        try {
            if (action.getIpAddress() == null) {

                action.setIpAddress(extractIpAddress());
            }
            if (action.getUserAgent() == null) {
                String userAgent = request.getHeader("User-Agent");
                action.setUserAgent(userAgent != null
                        ? userAgent.substring(0, Math.min(userAgent.length(), 500))
                        : null);
            }
        } catch (Exception e) {
            
        }
    }

    private String extractIpAddress() {
        String[] headers = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR", "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) ip = ip.split(",")[0].trim();
                ip = sanitizeIp(ip);
                return ip.length() > 45 ? ip.substring(0, 45) : ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? sanitizeIp(remoteAddr) : null;
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

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
          
            return null;
        }
    }
}