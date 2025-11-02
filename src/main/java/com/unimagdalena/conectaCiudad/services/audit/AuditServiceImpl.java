package com.unimagdalena.conectaCiudad.services.audit;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.unimagdalena.conectaCiudad.Dto.action.ActionDto;
import com.unimagdalena.conectaCiudad.Dto.action.ActionMapper;
import com.unimagdalena.conectaCiudad.repositories.ActionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final ActionRepository actionRepository;
    private final ActionMapper actionMapper;

    @Override
    public List<ActionDto> findAllActions() {
        return actionRepository.findAll(Sort.by(Sort.Direction.DESC, "actionAt"))
                .stream()
                .map(actionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActionDto> findActionsByUserId(Long userId) {
        return actionRepository.findByUserId(userId)
                .stream()
                .map(actionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActionDto> findActionsByNameContaining(String name) {
        return actionRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(actionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActionDto> findRecentActions(int limit) {
        return actionRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "actionAt"))
            )
            .getContent()
            .stream()
            .map(actionMapper::toDto)
            .collect(Collectors.toList());
    }
}
