package com.unimagdalena.conectaCiudad.services.action;

import com.unimagdalena.conectaCiudad.Dto.action.*;
import com.unimagdalena.conectaCiudad.Dto.page.PagedResponse;
import com.unimagdalena.conectaCiudad.Dto.page.Statistics;
import com.unimagdalena.conectaCiudad.entities.Access;
import com.unimagdalena.conectaCiudad.entities.Action;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.ActionResult;
import com.unimagdalena.conectaCiudad.enums.EntityType;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ActionRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;
import com.unimagdalena.conectaCiudad.repositories.AccessRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class ActionServiceImpl implements ActionService {

    private final ActionRepository actionRepository;
    private final ActionMapper actionMapper;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;
    private final UserRepository userRepository;
    private final AccessRepository accessRepository;

    @Override
    @Transactional
    public ActionDto save(ActionSaveDto actionSaveDto) {
        Action action = actionMapper.toEntity(actionSaveDto);
        enrichActionWithRequestData(action);
        return actionMapper.toDto(actionRepository.save(action));
    }

    @Override
    public LocalDateTime getLastActionDateByUserId(Long userId) {
        return actionRepository.findLastActionDateByUserId(userId);
    }

    @Override
    @Transactional
    public void logAction(String actionType, String description, User user, Access access) {
        if (user == null) {
            log.warn("Intento de log con usuario null para acción: {}", actionType);
            return;
        }
        
        try {
            ActionSaveDto dto = new ActionSaveDto(actionType, description, user, access);
            save(dto);
        } catch (Exception e) {
            log.error("Error al registrar acción {}: {}", actionType, e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ActionDto logActionWithDetails(ActionLogRequest request) {
        if (request.userId() == null) {
            log.error("No se puede registrar acción sin userId");
            throw new IllegalArgumentException("userId es requerido");
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
                .build();

            enrichActionWithRequestData(action);
            
            Action savedAction = actionRepository.save(action);
            return actionMapper.toDto(savedAction);
            
        } catch (Exception e) {
            log.error("Error al registrar acción detallada: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar acción", e);
        }
    }

    @Override
    public PagedResponse<ActionDto> findByUserId(Long userId, Pageable pageable) {
        Page<ActionDto> page = actionRepository
        .findByUserIdOrderByActionAtDesc(userId, pageable)
        .map(actionMapper::toDto);

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        Statistics<ActionDto> stats = new Statistics<>(
            actionRepository.countByUserId(userId),
            Map.of(
                "successful", actionRepository.countByUserIdAndResult(userId, ActionResult.SUCCESS),
                "failed", actionRepository.countByUserIdAndResult(userId, ActionResult.FAILED),
                "today", actionRepository.countByUserIdAndActionAtBetween(userId, startOfDay, LocalDateTime.now())
            )
        );

        return new PagedResponse<>(page, stats);
    }

    @Override
    public PagedResponse<ActionDto> findByEntityTypeAndId(EntityType entityType, Long entityId, Pageable pageable) {
        Page<ActionDto> page = actionRepository
            .findByEntityTypeAndEntityIdOrderByActionAtDesc(entityType, entityId, pageable)
            .map(actionMapper::toDto);
    
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
    
        Statistics<ActionDto> stats = new Statistics<>(
            actionRepository.countByEntityTypeAndEntityId(entityType, entityId),
            Map.of(
                "successful", actionRepository.countByEntityTypeAndEntityIdAndResult(entityType, entityId, ActionResult.SUCCESS),
                "failed", actionRepository.countByEntityTypeAndEntityIdAndResult(entityType, entityId, ActionResult.FAILED),
                "today", actionRepository.countByEntityTypeAndEntityIdAndActionAtBetween(entityType, entityId, startOfDay, LocalDateTime.now())
            )
        );
    
        return new PagedResponse<>(page, stats);
    }

    @Override
    public PagedResponse<ActionDto> findByActionType(String actionType, Pageable pageable) {
        Page<ActionDto> page = actionRepository
            .findByActionTypeOrderByActionAtDesc(actionType, pageable)
            .map(actionMapper::toDto);
    
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
    
        Statistics<ActionDto> stats = new Statistics<>(
            actionRepository.countByActionType(actionType),
            Map.of(
                "successful", actionRepository.countByActionTypeAndResult(actionType, ActionResult.SUCCESS),
                "failed", actionRepository.countByActionTypeAndResult(actionType, ActionResult.FAILED),
                "today", actionRepository.countByActionTypeAndActionAtBetween(actionType, startOfDay, LocalDateTime.now())
            )
        );
    
        return new PagedResponse<>(page, stats);
    }

    @Override
    public PagedResponse<ActionDto> findByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Page<ActionDto> page = actionRepository
            .findByActionAtBetweenOrderByActionAtDesc(start, end, pageable)
            .map(actionMapper::toDto);

        Statistics<ActionDto> stats = new Statistics<>(
            actionRepository.countByActionAtBetween(start, end),
            Map.of(
                "successful", actionRepository.countByResult(ActionResult.SUCCESS),
                "failed", actionRepository.countByResult(ActionResult.FAILED)
            )
        );

        return new PagedResponse<>(page, stats);
    }

    @Override
    public PagedResponse<ActionDto> findByResult(ActionResult result, Pageable pageable) {
        Page<ActionDto> page = actionRepository
            .findByResultOrderByActionAtDesc(result, pageable)
            .map(actionMapper::toDto);

        Statistics<ActionDto> stats = new Statistics<>(
            actionRepository.countByResult(result),
            Map.of(
                "resultType", result.name()
            )
        );

        return new PagedResponse<>(page, stats);
    }


    private void enrichActionWithRequestData(Action action) {
        try {
            String ipAddress = extractIpAddress();
            String userAgent = request.getHeader("User-Agent");
            
            action.setIpAddress(ipAddress);
            action.setUserAgent(userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 500)) : null);
            
        } catch (Exception e) {
            log.warn("No se pudo extraer información del request: {}", e.getMessage());
        }
    }

    private String extractIpAddress() {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip.length() > 45 ? ip.substring(0, 45) : ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null && remoteAddr.length() <= 45 ? remoteAddr : null;
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar metadata: {}", e.getMessage());
            return null;
        }
    }
}