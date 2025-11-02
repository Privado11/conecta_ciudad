package com.unimagdalena.conectaCiudad.services.audit;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.ActionMapper;
import com.unimagdalena.conectaCiudad.Dto.action.CitizenActionRequest;
import com.unimagdalena.conectaCiudad.entities.Action;
import com.unimagdalena.conectaCiudad.entities.User;
import com.unimagdalena.conectaCiudad.enums.CitizenActionType;
import com.unimagdalena.conectaCiudad.exceptions.BadRequestException;
import com.unimagdalena.conectaCiudad.exceptions.ResourceNotFoundException;
import com.unimagdalena.conectaCiudad.repositories.ActionRepository;
import com.unimagdalena.conectaCiudad.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final ActionRepository actionRepository;
    private final ActionMapper actionMapper;
    private final UserRepository userRepository;

    @Override
    public List<ActionDto> findAllActions() {
        log.info("Fetching all actions...");
        return actionRepository.findAll(Sort.by(Sort.Direction.DESC, "actionAt"))
                .stream()
                .map(actionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActionDto> findActionsByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("User ID must be a positive number");
        }

        log.info("Fetching actions for user ID: {}", userId);
        var actions = actionRepository.findByUserId(userId);

        if (actions.isEmpty()) {
            throw new ResourceNotFoundException("Action", "userId", userId);
        }

        return actions.stream()
                .map(actionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActionDto> findActionsByNameContaining(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Action name cannot be empty or null");
        }

        log.info("Fetching actions containing name: {}", name);
        var actions = actionRepository.findByNameContainingIgnoreCase(name);

        if (actions.isEmpty()) {
            throw new ResourceNotFoundException("Action", "name", name);
        }

        return actions.stream()
                .map(actionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActionDto> findRecentActions(int limit) {
        if (limit <= 0) {
            throw new BadRequestException("Limit must be greater than zero");
        }

        log.info("Fetching {} most recent actions", limit);
        return actionRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "actionAt"))
            )
            .getContent()
            .stream()
            .map(actionMapper::toDto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ActionDto registerCitizenAction(CitizenActionRequest request, Long userId) {
        if (request == null) {
            throw new BadRequestException("Action request cannot be null");
        }

        if (userId == null || userId <= 0) {
            throw new BadRequestException("User ID must be valid");
        }

        if (request.getProjectId() == null || request.getProjectId() <= 0) {
            throw new BadRequestException("Project ID must be valid");
        }

        if (request.getActionType() == null) {
            throw new BadRequestException("Action type cannot be null");
        }

        log.info("Registering action for user ID: {}", userId);

        ActionDto actionDto = logAction(request.getActionType(), request.getDescription() + " - Proyecto " + request.getProjectId(), userId);

        return actionDto;
    }

     private ActionDto logAction(CitizenActionType actionType, String description, Long userId) {
        if (userId == null) return null;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        Action action = Action.builder()
            .name(actionType.name())
            .description(description)
            .user(user)
            .build();
        actionRepository.save(action);
        return actionMapper.toDto(action);
    }
}
